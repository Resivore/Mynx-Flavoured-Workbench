package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * State carriers for any HugeMushroom-style external material.  The six booleans intentionally
 * mean "render exterior on this boundary", exactly like vanilla HugeMushroomBlock.  A neighbor
 * of the same physical derived role turns the shared boundary false; removing it leaves false,
 * exposing the cut surface rather than regenerating an exterior cap.
 */
final class HugeMushroomSurface {
    // Reuse vanilla's exact property identities so canonical projection can transfer state.
    static final BooleanProperty UP = BlockStateProperties.UP;
    static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    static final BooleanProperty EAST = BlockStateProperties.EAST;
    static final BooleanProperty WEST = BlockStateProperties.WEST;
    private static final BooleanProperty[] PROPERTIES = {UP, DOWN, NORTH, SOUTH, EAST, WEST};

    private HugeMushroomSurface() {}

    static void add(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PROPERTIES); }
    // WallBlock consumes all five visible directional names for connection topology. Keep its
    // material surface state separate so a low/tall connection can never be misread as a cap.
    static final BooleanProperty WALL_UP = BooleanProperty.create("mushroom_up");
    static final BooleanProperty WALL_DOWN = BooleanProperty.create("mushroom_down");
    static final BooleanProperty WALL_NORTH = BooleanProperty.create("mushroom_north");
    static final BooleanProperty WALL_SOUTH = BooleanProperty.create("mushroom_south");
    static final BooleanProperty WALL_EAST = BooleanProperty.create("mushroom_east");
    static final BooleanProperty WALL_WEST = BooleanProperty.create("mushroom_west");
    private static final BooleanProperty[] WALL_PROPERTIES = {
            WALL_UP, WALL_DOWN, WALL_NORTH, WALL_SOUTH, WALL_EAST, WALL_WEST};
    static void addWall(StateDefinition.Builder<Block, BlockState> builder) { builder.add(WALL_PROPERTIES); }
    static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case UP -> UP; case DOWN -> DOWN; case NORTH -> NORTH;
            case SOUTH -> SOUTH; case EAST -> EAST; case WEST -> WEST;
        };
    }
    static BlockState exterior(BlockState state) {
        for (BooleanProperty property : PROPERTIES) state = state.setValue(property, true);
        return state;
    }
    static BlockState update(BlockState state, Direction direction, BlockState neighbor) {
        // This mirrors HugeMushroomBlock: only a touching equal material member suppresses the
        // exterior.  False is intentionally sticky after a joined member is broken.
        return neighbor.is(state.getBlock()) ? state.setValue(property(direction), false) : state;
    }
    static BlockState exteriorWall(BlockState state) {
        for (BooleanProperty property : WALL_PROPERTIES) state = state.setValue(property, true);
        return state;
    }
    static BlockState updateWall(BlockState state, Direction direction, BlockState neighbor) {
        return neighbor.is(state.getBlock()) ? state.setValue(wallProperty(direction), false) : state;
    }
    private static BooleanProperty wallProperty(Direction direction) {
        return switch (direction) {
            case UP -> WALL_UP; case DOWN -> WALL_DOWN; case NORTH -> WALL_NORTH;
            case SOUTH -> WALL_SOUTH; case EAST -> WALL_EAST; case WEST -> WALL_WEST;
        };
    }
    static BooleanProperty[] properties() { return PROPERTIES.clone(); }
}

final class HugeMushroomSlabBlock extends SlabBlock {
    HugeMushroomSlabBlock(Properties properties) { super(properties); registerDefaultState(HugeMushroomSurface.exterior(defaultBlockState())); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); HugeMushroomSurface.add(builder); }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) { return HugeMushroomSurface.update(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), direction, neighbor); }
}
final class HugeMushroomStairsBlock extends StairBlock {
    HugeMushroomStairsBlock(BlockState source, Properties properties) { super(source, properties); registerDefaultState(HugeMushroomSurface.exterior(defaultBlockState())); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); HugeMushroomSurface.add(builder); }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) { return HugeMushroomSurface.update(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), direction, neighbor); }
}
final class HugeMushroomWallBlock extends WallBlock {
    HugeMushroomWallBlock(Properties properties) { super(properties); registerDefaultState(HugeMushroomSurface.exteriorWall(defaultBlockState())); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); HugeMushroomSurface.addWall(builder); }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) { return HugeMushroomSurface.updateWall(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), direction, neighbor); }
}
final class HugeMushroomVerticalSlabBlock extends VerticalSlabBlock {
    HugeMushroomVerticalSlabBlock(Properties properties) { super(properties); registerDefaultState(HugeMushroomSurface.exterior(defaultBlockState())); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); HugeMushroomSurface.add(builder); }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) { return HugeMushroomSurface.update(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), direction, neighbor); }
}
final class HugeMushroomStepBlock extends StepBlock {
    HugeMushroomStepBlock(Properties properties) { super(properties); registerDefaultState(HugeMushroomSurface.exterior(defaultBlockState())); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); HugeMushroomSurface.add(builder); }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) { return HugeMushroomSurface.update(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), direction, neighbor); }
}
final class HugeMushroomLayerBlock extends BgeLayerBlock {
    HugeMushroomLayerBlock(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); registerDefaultState(HugeMushroomSurface.exterior(defaultBlockState())); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); HugeMushroomSurface.add(builder); }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) { return HugeMushroomSurface.update(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), direction, neighbor); }
}
final class HugeMushroomCornerBlock extends BgeCornerBlock {
    HugeMushroomCornerBlock(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); registerDefaultState(HugeMushroomSurface.exterior(defaultBlockState())); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); HugeMushroomSurface.add(builder); }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) { return HugeMushroomSurface.update(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), direction, neighbor); }
}
final class HugeMushroomColumnBlock extends BgeColumnBlock {
    HugeMushroomColumnBlock(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); registerDefaultState(HugeMushroomSurface.exterior(defaultBlockState())); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); HugeMushroomSurface.add(builder); }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) { return HugeMushroomSurface.update(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), direction, neighbor); }
}

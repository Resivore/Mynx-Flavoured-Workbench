package games.twinhead.moreslabsstairsandwalls.block.concretepowder;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.entity.FallingSlabBlockEntity;
import games.twinhead.moreslabsstairsandwalls.block.falling.FallingSlab;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderSlab extends FallingSlab {

    private final ModBlocks hardenedBlock;

    public ConcretePowderSlab(ModBlocks modBlocks, ModBlocks hardenedBlock, Properties settings) {
        super(modBlocks, settings);
        this.hardenedBlock = hardenedBlock;
    }

    public void onLanding(Level world, BlockPos pos, BlockState fallingBlockState, BlockState currentStateInPos, FallingSlabBlockEntity fallingBlockEntity) {
        if (ConcretePowderSemantics.shouldHarden(world, pos, currentStateInPos)) {
            world.setBlock(pos, ConcretePowderSemantics.hardenedState(fallingBlockEntity.getBlockState(), this.hardenedBlock.getBlock(ModBlocks.BlockType.SLAB)), 3);
        }
        super.onLanding(world, pos, fallingBlockState, currentStateInPos, fallingBlockEntity);
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return ConcretePowderSemantics.shouldHarden(world, pos, state) ? ConcretePowderSemantics.hardenedState(state, this.hardenedBlock.getBlock(ModBlocks.BlockType.SLAB)) : super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    private static boolean hardensIn(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    private static boolean hardensOnAnySide(BlockGetter world, BlockPos pos) {
        boolean bl = false;
        BlockPos.MutableBlockPos mutable = pos.mutable();
        Direction[] var4 = Direction.values();
        int var5 = var4.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Direction direction = var4[var6];
            BlockState blockState = world.getBlockState(mutable);
            if (direction != Direction.DOWN || hardensIn(blockState)) {
                mutable.setWithOffset(pos, direction);
                blockState = world.getBlockState(mutable);
                if (hardensIn(blockState) && !blockState.isFaceSturdy(world, pos, direction.getOpposite())) {
                    bl = true;
                    break;
                }
            }
        }

        return bl;
    }


    private static boolean shouldHarden(BlockGetter world, BlockPos pos, BlockState state) {
        return hardensIn(state) || hardensOnAnySide(world, pos);
    }

    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (ConcretePowderSemantics.shouldHarden(world, pos, state)) { world.setBlock(pos, ConcretePowderSemantics.hardenedState(state, this.hardenedBlock.getBlock(ModBlocks.BlockType.SLAB)), 3); return; }
        super.tick(state, world, pos, random);
    }
}

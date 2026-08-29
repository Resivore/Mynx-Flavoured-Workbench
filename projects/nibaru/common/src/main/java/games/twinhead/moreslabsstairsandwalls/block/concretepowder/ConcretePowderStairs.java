package games.twinhead.moreslabsstairsandwalls.block.concretepowder;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.falling.FallingStairs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderStairs extends FallingStairs {

    private final ModBlocks hardenedBlock;

    public ConcretePowderStairs(ModBlocks modBlocks, BlockState defaultState, ModBlocks hardenedBlock, Properties settings) {
        super(modBlocks, defaultState, settings);
        this.hardenedBlock = hardenedBlock;
    }

    public void onLand(Level world, BlockPos pos, BlockState fallingBlockState, BlockState currentStateInPos, FallingBlockEntity fallingBlockEntity) {
        if (ConcretePowderSemantics.shouldHarden(world, pos, currentStateInPos)) {
            world.setBlock(pos, ConcretePowderSemantics.hardenedState(fallingBlockEntity.getBlockState(), this.hardenedBlock.getBlock(ModBlocks.BlockType.STAIRS)), 3);
        }
        super.onLand(world, pos, fallingBlockState, currentStateInPos, fallingBlockEntity);
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return ConcretePowderSemantics.shouldHarden(world, pos, state) ? ConcretePowderSemantics.hardenedState(state, this.hardenedBlock.getBlock(ModBlocks.BlockType.STAIRS)) : super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
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
        if (ConcretePowderSemantics.shouldHarden(world, pos, state)) { world.setBlock(pos, ConcretePowderSemantics.hardenedState(state, this.hardenedBlock.getBlock(ModBlocks.BlockType.STAIRS)), 3); return; }
        super.tick(state, world, pos, random);
    }
}

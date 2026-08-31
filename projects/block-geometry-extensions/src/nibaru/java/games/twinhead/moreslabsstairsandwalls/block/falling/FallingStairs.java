package games.twinhead.moreslabsstairsandwalls.block.falling;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseStairs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
@SuppressWarnings("deprecation")
public class FallingStairs extends BaseStairs implements SimpleWaterloggedBlock, Fallable {

    public FallingStairs(ModBlocks modBlocks, BlockState baseBlockState, Properties settings) {
        super(modBlocks,baseBlockState, settings);
    }

    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        FallingSemantics.schedule(world, pos, this);
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        FallingSemantics.schedule(tickAccess, pos, this);
        return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (FallingSemantics.shouldFall(world, pos)) {
            FallingBlockEntity.fall(world, pos, state);
        }
    }

    protected int getFallDelay() {
        return FallingSemantics.FALL_DELAY;
    }

    public static boolean canFallThrough(BlockState state) {
        return FallingSemantics.canFallThrough(state);
    }

    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        FallingSemantics.animateDust(state, world, pos, random);
    }
}

package games.twinhead.moreslabsstairsandwalls.block.coral;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseWall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class CoralWall extends BaseWall {

    private final ModBlocks deadCoralBlock;

    public CoralWall(ModBlocks modBlocks, ModBlocks deadCoralBlock, Properties settings) {
        super(modBlocks,settings);
        this.deadCoralBlock = deadCoralBlock;
    }


    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!CoralSemantics.survives(state, world, pos)) {
            world.setBlock(pos, CoralSemantics.deadState(state, this.deadCoralBlock.getBlock(ModBlocks.BlockType.WALL)), Block.UPDATE_CLIENTS);
        }
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        CoralSemantics.scheduleIfDry(world, tickAccess, pos, this, random);

        return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        CoralSemantics.scheduleIfDry(ctx.getLevel(), ctx.getLevel(), ctx.getClickedPos(), this, ctx.getLevel().getRandom());

        return super.getStateForPlacement(ctx);
    }


}

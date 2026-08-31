package games.twinhead.moreslabsstairsandwalls.block.translucent;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseSlab;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

@SuppressWarnings("deprecation")
public class TranslucentSlab extends BaseSlab {

    public TranslucentSlab(ModBlocks modBlocks, Properties settings) {
        super(modBlocks, settings);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        IceGeometryBehavior.randomTick(getModBlock(), state, level, pos, random);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        IceGeometryBehavior.afterPlayerDestroy(getModBlock(), level, player, pos, tool);
    }


    @Override
    @Environment(EnvType.CLIENT)
    public boolean skipRendering(BlockState state, BlockState state2, Direction dir) {
        if (state2.getBlock() == this.getModBlock().parentBlock) return true;

        if (state2.getBlock() instanceof TranslucentSlab slab){
            if (slab.getModBlock() == getModBlock())
                if (isInvisibleToGlassSlab(state, state2, dir)) return true;
        }

        if (state2.getBlock() instanceof TranslucentStairs stairs){
            if (stairs.getModBlock() == getModBlock())
                if (isInvisibleToGlassStairs(state, state2, dir)) return true;
        }

        return super.skipRendering(state, state2, dir);
    }

    private boolean isInvisibleToGlassSlab(BlockState state, BlockState state2, Direction dir) {
        SlabType type1 = state.getValue(SlabBlock.TYPE);
        SlabType type2 = state2.getValue(SlabBlock.TYPE);

        if (type2 == SlabType.DOUBLE) return true;

        switch (dir) {
            case UP, DOWN -> {
                if (type1 != type2) return true;
            }
            case NORTH, EAST, SOUTH, WEST -> {
                if (type1 == type2) return true;
            }
        }
        return false;
    }

    private boolean isInvisibleToGlassStairs(BlockState state, BlockState state2, Direction dir) {
        SlabType type1 = state.getValue(SlabBlock.TYPE);
        Half half2 = state2.getValue(StairBlock.HALF);
        Direction facing2 = state2.getValue(StairBlock.FACING);

        // up
        if( dir == Direction.UP && half2 == Half.BOTTOM) return true;

        // down
        if(dir == Direction.DOWN && half2 == Half.TOP) return true;

        // other stairs rear
        if(facing2 == dir.getOpposite()) return true;

        // sides
        if(dir.get2DDataValue() != -1) {
            if(type1 == SlabType.BOTTOM && half2 == Half.BOTTOM) return true;
            return type1 == SlabType.TOP && half2 == Half.TOP;
        }
        return false;
    }
}

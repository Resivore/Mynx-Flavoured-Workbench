package games.twinhead.moreslabsstairsandwalls.block.translucent;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseWall;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;

@SuppressWarnings("deprecation")
public class TranslucentWall extends BaseWall {

    public TranslucentWall(ModBlocks modBlock, Properties settings) {
        super(modBlock, settings);
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

        if (dir.equals(Direction.DOWN)){

            if (state2.getBlock() instanceof TranslucentSlab slab){
                if (slab.getModBlock() == getModBlock())
                    if (state2.getValue(BlockStateProperties.SLAB_TYPE).equals(SlabType.TOP)) return true;
            }

            if (state2.getBlock() instanceof TranslucentWall wall){
                {
                    if (wall.getModBlock() == getModBlock())
                        return isMatchingBelow(state, state2);
                }
            }
        } else if (dir.equals(Direction.UP)){
            if (state2.getBlock() instanceof TranslucentSlab slab){
                if (slab.getModBlock() == getModBlock())
                    if (state2.getValue(BlockStateProperties.SLAB_TYPE).equals(SlabType.BOTTOM)) return true;
            }

            if (state2.getBlock() instanceof TranslucentWall wall){
                {
                    if (wall.getModBlock() == getModBlock())
                        return isMatchingBelow(state2, state);
                }
            }

        } else if (state2.getBlock() instanceof TranslucentWall wall){
            if (wall.getModBlock() == getModBlock())
                return true;
        }

        return super.skipRendering(state, state2, dir);
    }


    private boolean isMatchingBelow(BlockState state, BlockState state2){
        if (state.getValue(WallBlock.EAST).equals(WallSide.LOW) && state2.getValue(WallBlock.EAST).equals(WallSide.TALL)) state = state.setValue(WallBlock.EAST, WallSide.TALL);
        if (state.getValue(WallBlock.WEST).equals(WallSide.LOW) && state2.getValue(WallBlock.WEST).equals(WallSide.TALL)) state = state.setValue(WallBlock.WEST, WallSide.TALL);
        if (state.getValue(WallBlock.NORTH).equals(WallSide.LOW) && state2.getValue(WallBlock.NORTH).equals(WallSide.TALL)) state = state.setValue(WallBlock.NORTH, WallSide.TALL);
        if (state.getValue(WallBlock.SOUTH).equals(WallSide.LOW) && state2.getValue(WallBlock.SOUTH).equals(WallSide.TALL)) state = state.setValue(WallBlock.SOUTH, WallSide.TALL);

        return getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty())
                .equals(getShape(state2, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()));
    }
}

package com.starfish_studios.bbb.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The retained BBB rope keeps ChainBlock's state, shape, and ordinary placement
 * behavior while adding an intentionally state-aware vertical rope interaction.
 */
public final class RopeBlock extends ChainBlock {
    public RopeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static boolean isVerticalRope(BlockState state) {
        return state.getBlock() instanceof RopeBlock && state.getValue(AXIS) == Direction.Axis.Y;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isVerticalRope(state) || hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()
                || !stack.is(asItem())) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            tryExtend(level, pos, player, stack);
        }
        // Handle both success and obstruction so this interaction cannot fall through to an offhand use.
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!isVerticalRope(state) || player.isShiftKeyDown()
                || !player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            tryRetract(level, pos, player);
        }
        // As above, a blocked/protected interaction is still fully handled.
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private boolean tryExtend(Level level, BlockPos clickedPos, Player player, ItemStack stack) {
        BlockPos target = bottomOfColumn(level, clickedPos).below();
        if (!level.isInWorldBounds(target) || !level.getWorldBorder().isWithinBounds(target)
                || !player.mayBuild() || !level.mayInteract(player, target)) {
            return false;
        }

        FluidState fluid = level.getFluidState(target);
        if (!fluid.isEmpty() && !fluid.is(Fluids.WATER)) {
            return false;
        }

        // BlockItem.place retains vanilla replacement, collision, survival, sound, game-event,
        // advancement, and Creative-consumption behavior. An upward target face forces axis=Y.
        BlockHitResult targetHit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, targetHit);
        if (!level.getBlockState(target).canBeReplaced(context) || !(asItem() instanceof BlockItem ropeItem)) {
            return false;
        }
        return ropeItem.place(context).consumesAction();
    }

    private boolean tryRetract(Level level, BlockPos clickedPos, Player player) {
        BlockPos bottom = bottomOfColumn(level, clickedPos);
        if (!player.mayBuild() || !level.mayInteract(player, bottom)) {
            return false;
        }

        BlockState removed = level.getBlockState(bottom);
        BlockState replacement = removed.getValue(WATERLOGGED)
                ? Blocks.WATER.defaultBlockState()
                : Blocks.AIR.defaultBlockState();
        if (!level.setBlock(bottom, replacement, Block.UPDATE_ALL)) {
            return false;
        }

        level.playSound(player, bottom, removed.getSoundType().getBreakSound(), SoundSource.BLOCKS,
                removed.getSoundType().getVolume(), removed.getSoundType().getPitch());
        level.gameEvent(GameEvent.BLOCK_DESTROY, bottom, GameEvent.Context.of(player, removed));
        if (!player.isCreative()) {
            ItemStack returned = new ItemStack(asItem());
            if (!player.addItem(returned)) {
                player.drop(returned, false);
            }
        }
        return true;
    }

    private static BlockPos bottomOfColumn(Level level, BlockPos start) {
        BlockPos bottom = start;
        while (bottom.getY() > level.getMinY()) {
            BlockPos below = bottom.below();
            if (!isVerticalRope(level.getBlockState(below))) {
                break;
            }
            bottom = below;
        }
        return bottom;
    }
}

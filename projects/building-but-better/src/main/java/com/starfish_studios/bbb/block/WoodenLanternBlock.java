package com.starfish_studios.bbb.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WoodenLanternBlock extends LanternBlock implements HammerableBlock {
    protected static final VoxelShape AABB = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13),
            Block.box(4, 2, 4, 12, 11, 12),
            Block.box(3, 11, 3, 13, 13, 13));
    protected static final VoxelShape HANGING_AABB = Shapes.or(
            AABB, Block.box(6, 13, 6, 10, 16, 10));

    public WoodenLanternBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BlockStateProperties.HANGING) ? HANGING_AABB : AABB;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResult onHammerUse(BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        boolean hanging = !state.getValue(BlockStateProperties.HANGING);
        BlockState updated = state.setValue(BlockStateProperties.HANGING, hanging);
        level.setBlockAndUpdate(pos, updated);
        level.playSound(player, pos,
                hanging ? updated.getSoundType().getPlaceSound() : updated.getSoundType().getBreakSound(),
                player.getSoundSource(), 1.0F, 1.0F);
        if (!hanging && !level.getBlockState(pos.below()).isSolid()) {
            level.destroyBlock(pos, true);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}

package com.mozko.doublebarrels.mixin;

import com.mozko.doublebarrels.DoubleBarrelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("TAIL"))
    private void doublebarrels$onPlace(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BarrelBlock)) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BarrelBlockEntity thisBarrel)) return;

        DoubleBarrelAccess thisAccess = (DoubleBarrelAccess) thisBarrel;
        if (thisAccess.isConnected()) return;

        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            BlockPos clickedPos = pos.relative(context.getClickedFace().getOpposite());
            if (!(level.getBlockEntity(clickedPos) instanceof BarrelBlockEntity)) return;
        }

        Direction myFacing = state.getValue(BarrelBlock.FACING);
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
            if (!(neighborEntity instanceof BarrelBlockEntity neighborBarrel)) continue;

            DoubleBarrelAccess neighborAccess = (DoubleBarrelAccess) neighborBarrel;
            BlockState neighborState = level.getBlockState(neighborPos);
            Direction neighborFacing = neighborState.getValue(BarrelBlock.FACING);
            if (neighborAccess.isConnected()
                    || neighborFacing != myFacing
                    || (myFacing == Direction.DOWN && direction.getAxis() != Direction.Axis.Y)) {
                continue;
            }

            thisAccess.connectTo(neighborBarrel);
            return;
        }
    }
}

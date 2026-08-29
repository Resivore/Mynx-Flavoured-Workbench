package com.mozko.doublebarrels.mixin;

import com.mozko.doublebarrels.DoubleBarrelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
    private void doublebarrels$preRemoveSideEffects(
            BlockPos pos,
            BlockState state,
            CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof BarrelBlockEntity barrel)) return;
        if (!(self instanceof DoubleBarrelAccess access)) return;
        if (!access.isConnected()) return;

        access.disconnect();
        Level level = barrel.getLevel();
        if (level != null) {
            NonNullList<ItemStack> items = access.doublebarrels$getItems();
            Containers.dropContents(level, pos, items);
            items.clear();
        }
    }
}

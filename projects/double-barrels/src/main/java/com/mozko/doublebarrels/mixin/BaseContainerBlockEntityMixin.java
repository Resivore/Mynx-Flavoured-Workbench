package com.mozko.doublebarrels.mixin;

import com.mozko.doublebarrels.DoubleBarrelAccess;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityMixin {
    @Inject(method = "clearContent", at = @At("HEAD"), cancellable = true)
    private void doublebarrels$clear(CallbackInfo ci) {
        if (!((Object) this instanceof BarrelBlockEntity barrel)) return;
        DoubleBarrelAccess access = (DoubleBarrelAccess) barrel;
        if (!access.isConnected()) return;
        Container combined = access.getCombinedInventory();
        if (combined != null) {
            combined.clearContent();
            ci.cancel();
        }
    }
}

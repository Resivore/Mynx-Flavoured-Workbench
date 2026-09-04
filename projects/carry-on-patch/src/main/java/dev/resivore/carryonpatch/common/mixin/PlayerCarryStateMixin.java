package dev.resivore.carryonpatch.common.mixin;

import dev.resivore.carryonpatch.common.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.chermew.grabandgo.duck.GrabCarrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, priority = 1100, remap = false)
public abstract class PlayerCarryStateMixin implements CarryStateAccess, PlayerCarryStateMixinBridge {
    @Unique private CarryState carryOnPatch$preservation;
    @Unique private boolean carryOnPatch$restoring;

    public CarryState carryOnPatch$state() {
        if (carryOnPatch$preservation == null) carryOnPatch$preservation = new CarryState();
        return carryOnPatch$preservation;
    }

    public void carryOnPatch$restore(CompoundTag data, boolean carrying) {
        carryOnPatch$restoring = true;
        try {
            ((GrabCarrier) this).grabandgo$setCarriedData(data);
            ((GrabCarrier) this).grabandgo$setCarrying(carrying);
        } finally { carryOnPatch$restoring = false; }
    }

    @Inject(method = {"grabandgo$clearCarried", "grabandgo$setCarriedData", "grabandgo$setCarrying"},
            at = @At("HEAD"), cancellable = true, require = 3)
    private void carryOnPatch$protect(CallbackInfo ci) {
        if (!carryOnPatch$restoring && carryOnPatch$state().protectedState()) ci.cancel();
    }
}

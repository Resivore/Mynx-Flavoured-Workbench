package com.yungnickyoung.minecraft.ribbits.mixin.mixins.chute;

import com.yungnickyoung.minecraft.ribbits.chute.ChutePlayerAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds one synchronized Boolean; it is intentionally absent from save/respawn copy code. */
@Mixin(Player.class)
public abstract class PlayerChuteDataMixin implements ChutePlayerAccess {
    @Unique
    private static final EntityDataAccessor<Boolean> RIBBITS_CHUTE_DEPLOYED =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void ribbits$defineChuteDeployment(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(RIBBITS_CHUTE_DEPLOYED, false);
    }

    @Override
    public boolean ribbits$isChuteDeployed() {
        return ((Player) (Object) this).getEntityData().get(RIBBITS_CHUTE_DEPLOYED);
    }

    @Override
    public void ribbits$setChuteDeployed(boolean deployed) {
        Player player = (Player) (Object) this;
        if (player.getEntityData().get(RIBBITS_CHUTE_DEPLOYED) != deployed) {
            player.getEntityData().set(RIBBITS_CHUTE_DEPLOYED, deployed);
        }
    }
}

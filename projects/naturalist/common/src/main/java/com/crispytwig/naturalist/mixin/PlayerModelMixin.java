package com.crispytwig.naturalist.mixin;

import com.crispytwig.naturalist.server.entity.base.IKMount;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {
    @Unique
    private static final float naturalist$waistY = 12.0F;
    @Unique
    private static final float naturalist$pitchSign = -1.0F;
    @Unique
    private static final float naturalist$rollSign = -1.0F;

    public PlayerModelMixin() {
        super(null);
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("RETURN"))
    private void naturalist$counterLean(AvatarRenderState state, CallbackInfo ci) {
        IKMount mount = naturalist$leanMount(state);
        if (mount == null) {
            return;
        }
        float pitch = mount.getRenderPitch() * naturalist$pitchSign;
        float roll = mount.getRenderRoll() * naturalist$rollSign;
        if (pitch == 0.0F && roll == 0.0F) {
            return;
        }
        float cosP = (float) Math.cos(pitch), sinP = (float) Math.sin(pitch);
        float cosR = (float) Math.cos(roll), sinR = (float) Math.sin(roll);
        naturalist$rotateWaist(this.head, pitch, roll, cosP, sinP, cosR, sinR);
        naturalist$rotateWaist(this.body, pitch, roll, cosP, sinP, cosR, sinR);
        naturalist$rotateWaist(this.leftArm, pitch, roll, cosP, sinP, cosR, sinR);
        naturalist$rotateWaist(this.rightArm, pitch, roll, cosP, sinP, cosR, sinR);
        // In 26.2 the hat, jacket, and sleeves are children of these parts and inherit the lean.
    }

    @Unique
    private static IKMount naturalist$leanMount(AvatarRenderState state) {
        Entity entity = NaturalistRenderEntityLookup.source(state);
        if (entity instanceof Player && entity.getVehicle() instanceof IKMount mount) {
            return mount;
        }
        return null;
    }

    @Unique
    private static void naturalist$rotateWaist(ModelPart part, float pitch, float roll, float cosP, float sinP, float cosR, float sinR) {
        float dx = part.x;
        float dy = part.y - naturalist$waistY;
        float dz = part.z;

        float y1 = dy * cosP - dz * sinP;

        part.x = dx * cosR - y1 * sinR;
        part.y = naturalist$waistY + dx * sinR + y1 * cosR;
        part.z = dy * sinP + dz * cosP;
        part.xRot += pitch;
        part.zRot += roll;
    }
}

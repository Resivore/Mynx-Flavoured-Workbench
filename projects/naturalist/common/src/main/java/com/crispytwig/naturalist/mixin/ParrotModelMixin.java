package com.crispytwig.naturalist.mixin;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParrotModel.class)
public abstract class ParrotModelMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/ParrotRenderState;)V",
            at = @At("HEAD"))
    private void naturalist$flyingShoulderAnim(ParrotRenderState state, CallbackInfo ci) {
        if (!NaturalistParrotRenderStateLookup.isFlyingShoulder(state)
                || state.pose != ParrotModel.Pose.ON_SHOULDER) {
            return;
        }
        state.pose = ParrotModel.Pose.FLYING;
        state.flapAngle = Mth.sin(state.ageInTicks * 1.5F) + 1.0F;
    }
}

/**
 * Tracks the ephemeral shoulder-parrot states that need Naturalist's flight pose. Model setup is
 * deferred in 26.2, so the decision must follow the queued state rather than a frame-global flag.
 */
final class NaturalistParrotRenderStateLookup {
    private static final Set<ParrotRenderState> FLYING_SHOULDERS =
            Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));

    private NaturalistParrotRenderStateLookup() {
    }

    static void markFlyingShoulder(ParrotRenderState state) {
        FLYING_SHOULDERS.add(state);
    }

    static boolean isFlyingShoulder(ParrotRenderState state) {
        return FLYING_SHOULDERS.contains(state);
    }
}

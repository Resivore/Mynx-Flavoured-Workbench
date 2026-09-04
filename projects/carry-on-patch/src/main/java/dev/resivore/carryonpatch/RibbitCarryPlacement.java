package dev.resivore.carryonpatch;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Placement only at GrabAndGo's carried-entity submit calls; never ordinary world rendering. */
public final class RibbitCarryPlacement {
    // Post-GrabAndGo, pre-entity-renderer units (blocks, not model pixels).
    // First person: T(0,-.45,-.55) S(.3,.3,.3) Ry(180) T(0,.5,.4).
    // Thus +.15 camera-up and -.12 camera-Z (away); preserve upstream scale/rotation.
    public static final float FIRST_PERSON_LOCAL_UP = 0.50F;
    public static final float FIRST_PERSON_LOCAL_FORWARD = 0.40F;
    // Third person: player-model T(0,.9,-.45) S(.5,-.5,-.5) T(0,.6,.2).
    // Thus -.30 player-model Y (up) and -.10 player-model Z (forward).
    public static final float THIRD_PERSON_LOCAL_UP = 0.60F;
    public static final float THIRD_PERSON_LOCAL_FORWARD = 0.20F;

    private RibbitCarryPlacement() {}

    /** Exact registry IDs cover professions and the separate Wandering Ribbit; no GeckoLib link. */
    public static boolean targets(Identifier id) {
        return id != null && id.getNamespace().equals("ribbits")
                && (id.getPath().equals("ribbit") || id.getPath().equals("wandering_ribbit"));
    }

    public static void translate(PoseStack poses, boolean firstPerson) {
        poses.translate(0.0F,
                firstPerson ? FIRST_PERSON_LOCAL_UP : THIRD_PERSON_LOCAL_UP,
                firstPerson ? FIRST_PERSON_LOCAL_FORWARD : THIRD_PERSON_LOCAL_FORWARD);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void submit(EntityRenderer renderer, EntityRenderState state, PoseStack poses,
            SubmitNodeCollector collector, CameraRenderState camera, boolean firstPerson) {
        if (state.entityType == null
                || !targets(BuiltInRegistries.ENTITY_TYPE.getKey(state.entityType))) {
            renderer.submit(state, poses, collector, camera);
            return;
        }
        poses.pushPose();
        try {
            translate(poses, firstPerson);
            renderer.submit(state, poses, collector, camera);
        } finally {
            poses.popPose();
        }
    }
}

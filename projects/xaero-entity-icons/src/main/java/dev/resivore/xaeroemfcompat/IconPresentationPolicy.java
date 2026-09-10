package dev.resivore.xaeroemfcompat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;

/** Per-request Xaero raster framing; it never changes an entity model. */
public final class IconPresentationPolicy {
    private static final ThreadLocal<Float> REQUEST_SCALE = new ThreadLocal<>();

    private IconPresentationPolicy() { }

    public static void requestStarted(Entity entity, boolean canPrerender) {
        requestStarted(entity == null ? null : net.minecraft.world.entity.EntityType
                .getKey(entity.getType()).toString(), canPrerender);
    }

    static void requestStarted(String entityId, boolean canPrerender) {
        float scale = canPrerender ? IconTargetPolicy.presentationScale(entityId) : 1.0F;
        if (scale != 1.0F) REQUEST_SCALE.set(scale); else REQUEST_SCALE.remove();
    }

    /** Xaero honors its immutable request scale only below one. */
    public static RadarIconCreator.Parameters scaleForCurrentRequest(RadarIconCreator.Parameters parameters) {
        Float scale = REQUEST_SCALE.get();
        if (scale == null || scale >= 1.0F) return parameters;
        return new RadarIconCreator.Parameters(
                parameters.variant, parameters.defaultModelConfig, parameters.form,
                parameters.scale * scale, parameters.debug);
    }

    /** Applies an enlargement exactly once at Xaero's model-form presentation stage. */
    public static void applyUpscaleForCurrentRequest(PoseStack pose) {
        Float scale = REQUEST_SCALE.get();
        if (scale != null && scale > 1.0F) pose.scale(scale, scale, scale);
    }

    public static void requestFinished() { REQUEST_SCALE.remove(); }
}

package dev.resivore.xaeroemfcompat;

import net.minecraft.world.entity.Entity;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;

/** Per-request Xaero raster framing; it never changes an entity model. */
public final class IconPresentationPolicy {
    private static final ThreadLocal<Float> REQUEST_SCALE = new ThreadLocal<>();

    private IconPresentationPolicy() { }

    public static void requestStarted(Entity entity, boolean canPrerender) {
        float scale = canPrerender ? IconTargetPolicy.presentationScale(entity) : 1.0F;
        if (scale != 1.0F) REQUEST_SCALE.set(scale); else REQUEST_SCALE.remove();
    }

    public static RadarIconCreator.Parameters scaleForCurrentRequest(RadarIconCreator.Parameters parameters) {
        Float scale = REQUEST_SCALE.get();
        if (scale == null) return parameters;
        return new RadarIconCreator.Parameters(
                parameters.variant, parameters.defaultModelConfig, parameters.form,
                parameters.scale * scale, parameters.debug);
    }

    public static void requestFinished() { REQUEST_SCALE.remove(); }
}

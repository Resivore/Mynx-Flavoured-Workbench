package dev.resivore.naturalistxaeroicons;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;
import xaero.hud.minimap.radar.icon.definition.form.sprite.RadarIconSpriteForm;

/** Applies the C14 retune only to the evidenced native Brown Bear sprite request. */
public final class BrownBearSpritePresentation {
    static final float SCALE = 0.42F;
    private static final ThreadLocal<Boolean> BROWN_BEAR_REQUEST = ThreadLocal.withInitial(() -> false);

    private BrownBearSpritePresentation() {}

    public static void requestStarted(Entity entity, boolean canPrerender) {
        BROWN_BEAR_REQUEST.set(canPrerender && entity != null
                && "naturalist:bear".equals(EntityType.getKey(entity.getType()).toString()));
    }

    public static RadarIconCreator.Parameters scaleForCurrentRequest(RadarIconCreator.Parameters parameters) {
        if (!Boolean.TRUE.equals(BROWN_BEAR_REQUEST.get()) || !(parameters.form instanceof RadarIconSpriteForm)) {
            return parameters;
        }
        float scale = Math.min(parameters.scale, SCALE);
        return new RadarIconCreator.Parameters(
                parameters.variant, parameters.defaultModelConfig, parameters.form, scale, parameters.debug);
    }

    public static void requestFinished() {
        BROWN_BEAR_REQUEST.remove();
    }
}

package games.twinhead.moreslabsstairsandwalls.block.slime;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Geometry-neutral Slime callbacks shared by native and provider-bound geometries. */
public final class SlimeSemantics {
    private SlimeSemantics() {}

    public static boolean handlesFall(Entity entity) {
        return !entity.isSuppressingBounce();
    }

    public static void suppressFallDamage(Level level, Entity entity, double fallDistance) {
        if (!level.isClientSide())
            entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
    }

    public static void modifyHorizontalMovement(Entity entity) {
        double verticalSpeed = Math.abs(entity.getDeltaMovement().y);
        if (verticalSpeed < 0.1 && !entity.isSteppingCarefully()) {
            double factor = 0.4 + verticalSpeed * 0.2;
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(factor, 1.0, factor));
        }
    }
}

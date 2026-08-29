package dev.resivore.sweetberryhorseimmunity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;

public final class SweetBerryHorseImmunity {
    private SweetBerryHorseImmunity() {
    }

    public static boolean shouldBypassSweetBerryEffects(EntityType<?> entityType) {
        return entityType == EntityTypes.HORSE;
    }

    public static boolean shouldBypassSweetBerryEffects(
        EntityType<?> entityType,
        boolean isPlayer,
        EntityType<?> vehicleType
    ) {
        return shouldBypassSweetBerryEffects(entityType)
            || isPlayer
            && shouldBypassSweetBerryEffects(vehicleType);
    }

    public static boolean shouldBypassPowderSnowEffects(EntityType<?> entityType) {
        return shouldBypassSweetBerryEffects(entityType);
    }

    public static boolean shouldBypassPowderSnowEffects(
        EntityType<?> entityType,
        boolean isPlayer,
        EntityType<?> vehicleType
    ) {
        return shouldBypassSweetBerryEffects(entityType, isPlayer, vehicleType);
    }

    public static boolean shouldBypassPowderSnowEffects(Entity entity) {
        Entity vehicle = entity.getVehicle();
        return shouldBypassPowderSnowEffects(
            entity.getType(),
            entity instanceof Player,
            vehicle == null ? null : vehicle.getType()
        );
    }
}

package dev.resivore.sweetberryhorseimmunity;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowderSnowHorseImmunityTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void ordinaryHorseReceivesPowderSnowImmunity() {
        assertAll(
            () -> assertTrue(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(EntityTypes.HORSE)),
            () -> assertTrue(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.HORSE,
                false,
                null
            ))
        );
    }

    @Test
    void playerRidingExactOrdinaryHorseReceivesPowderSnowImmunity() {
        assertTrue(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
            EntityTypes.PLAYER,
            true,
            EntityTypes.HORSE
        ));
    }

    @Test
    void dismountedOrMerelyNearbyPlayerRemainsVanilla() {
        assertAll(
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.PLAYER,
                true,
                null
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.PLAYER,
                false,
                EntityTypes.HORSE
            ))
        );
    }

    @Test
    void otherEquinesAndTheirRidersRemainVanilla() {
        assertAll(
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(EntityTypes.DONKEY)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(EntityTypes.MULE)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(EntityTypes.SKELETON_HORSE)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(EntityTypes.ZOMBIE_HORSE)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.DONKEY
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.MULE
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.SKELETON_HORSE
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.ZOMBIE_HORSE
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.LLAMA
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.TRADER_LLAMA
            ))
        );
    }

    @Test
    void unrelatedLivingEntitiesAndPassengersRemainVanilla() {
        assertAll(
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(EntityTypes.COW)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(EntityTypes.PLAYER)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.COW,
                false,
                EntityTypes.HORSE
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.CAMEL
            ))
        );
    }
}

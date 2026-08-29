package dev.resivore.sweetberryhorseimmunity;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SweetBerryHorseImmunityTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void ordinaryHorseKeepsTheCanary1BypassForEveryVehicleState() {
        assertAll(
            () -> assertTrue(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.HORSE)),
            () -> assertTrue(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.HORSE,
                false,
                null
            )),
            () -> assertTrue(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.HORSE,
                false,
                EntityTypes.DONKEY
            ))
        );
    }

    @Test
    void playerRidingExactOrdinaryHorseBypassesSweetBerryEffects() {
        assertTrue(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
            EntityTypes.PLAYER,
            true,
            EntityTypes.HORSE
        ));
    }

    @Test
    void unmountedPlayerRemainsVanilla() {
        assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
            EntityTypes.PLAYER,
            true,
            null
        ));
    }

    @Test
    void playerRidingDonkeyRemainsVanilla() {
        assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
            EntityTypes.PLAYER,
            true,
            EntityTypes.DONKEY
        ));
    }

    @Test
    void playerRidingMuleRemainsVanilla() {
        assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
            EntityTypes.PLAYER,
            true,
            EntityTypes.MULE
        ));
    }

    @Test
    void playerRidingOtherVehiclesRemainsVanilla() {
        assertAll(
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.SKELETON_HORSE
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.ZOMBIE_HORSE
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.LLAMA
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.CAMEL
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.PIG
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.STRIDER
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.OAK_BOAT
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.PLAYER,
                true,
                EntityTypes.MINECART
            ))
        );
    }

    @Test
    void otherEquinesAndRideableEntitiesRemainVanilla() {
        assertAll(
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.DONKEY)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.MULE)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.SKELETON_HORSE)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.ZOMBIE_HORSE)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.LLAMA)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.TRADER_LLAMA)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.CAMEL))
        );
    }

    @Test
    void ordinaryNonHorseLivingEntitiesRemainVanilla() {
        assertAll(
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.COW)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.PLAYER)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.COW,
                false,
                EntityTypes.HORSE
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.PLAYER,
                false,
                EntityTypes.HORSE
            ))
        );
    }

    @Test
    void vanillaFoxAndBeeExceptionsRemainVanillaOwned() {
        assertAll(
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.FOX)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(EntityTypes.BEE)),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.FOX,
                false,
                EntityTypes.HORSE
            )),
            () -> assertFalse(SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
                EntityTypes.BEE,
                false,
                EntityTypes.HORSE
            ))
        );
    }
}

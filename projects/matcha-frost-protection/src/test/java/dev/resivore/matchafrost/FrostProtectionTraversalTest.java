package dev.resivore.matchafrost;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

class FrostProtectionTraversalTest {
    @Test
    void scansExactlyTheFourEquippedArmorSlots() {
        assertEquals(
                List.of(
                        EquipmentSlot.HEAD,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.FEET),
                FrostProtectionTraversal.ARMOR_SLOTS);
    }

    @Test
    void everyArmorSlotQualifiesIndependentlyAtEverySupportedLevel() {
        for (int level = 1; level <= 3; level++) {
            int currentLevel = level;
            assertAll(
                    "Frost Protection " + level,
                    () -> assertTrue(qualifiesOnly(EquipmentSlot.HEAD, currentLevel),
                            "helmet must qualify"),
                    () -> assertTrue(qualifiesOnly(EquipmentSlot.CHEST, currentLevel),
                            "chestplate must qualify"),
                    () -> assertTrue(qualifiesOnly(EquipmentSlot.LEGS, currentLevel),
                            "leggings must qualify"),
                    () -> assertTrue(qualifiesOnly(EquipmentSlot.FEET, currentLevel),
                            "boots must qualify"));
        }
    }

    @Test
    void noFrostProtectionDoesNotQualify() {
        assertFalse(FrostProtectionTraversal.hasQualifyingArmor(slot -> 0));
    }

    @Test
    void removingTheOnlyQualifyingLevelRestoresTheNegativeDecision() {
        assertTrue(qualifiesOnly(EquipmentSlot.LEGS, 1));
        assertFalse(FrostProtectionTraversal.hasQualifyingArmor(slot -> 0));
    }

    private static boolean qualifiesOnly(EquipmentSlot equippedSlot, int level) {
        return FrostProtectionTraversal.hasQualifyingArmor(
                slot -> slot == equippedSlot ? level : 0);
    }
}

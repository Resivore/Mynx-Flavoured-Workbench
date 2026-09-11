package dev.resivore.xaeroemfcompat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IconTargetPolicyTest {
    @Test
    void exactAllowlistHasNoNamespaceOrEntityWildcards() {
        for (String id : List.of("minecraft:fox", "minecraft:goat", "minecraft:frog",
                "minecraft:bogged", "minecraft:witch")) {
            assertTrue(IconTargetPolicy.select(id, null).isCompositionTarget(), id);
        }
        for (String profession : IconTargetPolicy.targetedVillagerProfessions()) {
            var selection = IconTargetPolicy.select("minecraft:villager", profession);
            assertEquals(IconTargetPolicy.Composition.VILLAGER, selection.composition());
            assertTrue(selection.expectedHat().startsWith("ribbits_"));
        }
        for (String profession : List.of("mynx_flora_trades:florist", "minecraft:farmer",
                "minecraft:cleric", "minecraft:mason")) {
            assertTrue(IconTargetPolicy.select("minecraft:villager", profession)
                    .suppressProfessionHat(), profession);
        }
        assertFalse(IconTargetPolicy.select("minecraft:villager", "minecraft:butcher")
                .suppressProfessionHat());
        for (String id : List.of(
                "minecraft:allay", "minecraft:vex", "minecraft:axolotl", "minecraft:sniffer",
                "minecraft:iron_golem", "minecraft:wolf", "minecraft:bat", "minecraft:parrot",
                "minecraft:ravager", "minecraft:sheep", "minecraft:horse", "minecraft:turtle",
                "minecraft:creeper", "minecraft:ghast", "minecraft:happy_ghast",
                "minecraft:bee", "minecraft:rabbit", "minecraft:camel",
                "naturalist:bear", "ribbits:ribbit", "other:fox", "minecraft:fox_variant")) {
            assertFalse(IconTargetPolicy.select(id, "minecraft:farmer").isCompositionTarget(), id);
        }
        for (String profession : List.of("minecraft:librarian", "minecraft:armorer",
                "minecraft:none", "ribbits:farmer", "mynx_flora_trades:other")) {
            assertFalse(IconTargetPolicy.select("minecraft:villager", profession).isCompositionTarget(), profession);
        }
    }

    @Test
    void exactRasterScalePolicyCannotAffectUnlistedIds() {
        assertEquals(1.50F, IconTargetPolicy.presentationScale("minecraft:ghast"));
        assertEquals(1.50F, IconTargetPolicy.presentationScale("minecraft:happy_ghast"));
        assertEquals(0.75F, IconTargetPolicy.presentationScale("minecraft:bee"));
        assertEquals(0.75F, IconTargetPolicy.presentationScale("minecraft:rabbit"));
        for (String id : List.of("minecraft:fox", "minecraft:allay", "naturalist:bear", "ribbits:ribbit")) {
            assertEquals(1.0F, IconTargetPolicy.presentationScale(id), id);
        }
    }
}

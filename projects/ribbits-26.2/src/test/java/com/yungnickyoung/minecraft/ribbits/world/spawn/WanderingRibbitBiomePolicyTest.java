package com.yungnickyoung.minecraft.ribbits.world.spawn;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderingRibbitBiomePolicyTest {
    private static final Set<Identifier> EXPECTED_BASELINE = Set.of(
            vanilla("swamp"), vanilla("mangrove_swamp"),
            vanilla("forest"), vanilla("flower_forest"), vanilla("birch_forest"),
            vanilla("old_growth_birch_forest"), vanilla("dark_forest"), vanilla("pale_garden"),
            vanilla("plains"), vanilla("sunflower_plains"), vanilla("meadow"), vanilla("cherry_grove"),
            vanilla("jungle"), vanilla("sparse_jungle"), vanilla("bamboo_jungle"),
            vanilla("taiga"), vanilla("old_growth_pine_taiga"), vanilla("old_growth_spruce_taiga"),
            vanilla("mushroom_fields"), vanilla("river")
    );

    @Test
    void exactVanillaBaselineIsComplete() {
        assertEquals(20, WanderingRibbitBiomePolicy.VANILLA_BASELINE.size());
        assertEquals(EXPECTED_BASELINE, WanderingRibbitBiomePolicy.VANILLA_BASELINE);
    }

    @Test
    void missingAllowTagUsesBroaderFallback() {
        for (Identifier biome : EXPECTED_BASELINE) {
            assertTrue(allows(biome, false, false, false, false), biome.toString());
        }
        assertTrue(allows(Identifier.parse("example:temperate_woods"), false, false, false, true));
        assertFalse(allows(vanilla("desert"), false, false, false, false));
        assertFalse(allows(Identifier.parse("example:unaudited"), false, false, false, false));
    }

    @Test
    void installedAllowTagIsAuthoritativeEvenWhenEmpty() {
        assertFalse(allows(vanilla("forest"), false, true, false, true));
        assertTrue(allows(Identifier.parse("example:audited_grove"), false, true, true, false));
    }

    @Test
    void denyAlwaysWinsOverEveryAdmissionPath() {
        assertFalse(allows(vanilla("forest"), true, false, false, true));
        assertFalse(allows(vanilla("meadow"), true, true, true, false));
        assertFalse(allows(Identifier.parse("example:tagged"), true, true, true, true));
    }

    @Test
    void baselineSeparatesRequestedExceptionsFromExcludedNeighbors() {
        assertTrue(allows(vanilla("meadow"), false, false, false, false));
        assertTrue(allows(vanilla("cherry_grove"), false, false, false, false));
        assertFalse(allows(vanilla("grove"), true, false, false, true));
        assertFalse(allows(vanilla("snowy_taiga"), true, false, false, true));
        assertFalse(allows(vanilla("frozen_river"), true, false, false, true));
        assertFalse(allows(vanilla("stony_peaks"), true, false, false, false));
        assertFalse(allows(vanilla("windswept_forest"), true, false, false, true));
        assertFalse(allows(vanilla("savanna"), true, false, false, false));
        assertFalse(allows(vanilla("badlands"), true, false, false, false));
        assertFalse(allows(vanilla("ocean"), true, false, false, false));
    }

    private static boolean allows(
            Identifier biome,
            boolean denied,
            boolean allowTagPresent,
            boolean allowTagMember,
            boolean familyMember
    ) {
        return WanderingRibbitBiomePolicy.allows(
                biome, denied, allowTagPresent, allowTagMember, familyMember);
    }

    private static Identifier vanilla(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }
}

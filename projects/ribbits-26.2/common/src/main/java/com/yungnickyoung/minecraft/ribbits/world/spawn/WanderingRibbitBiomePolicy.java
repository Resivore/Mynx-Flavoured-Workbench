package com.yungnickyoung.minecraft.ribbits.world.spawn;

import net.minecraft.resources.Identifier;

import java.util.Set;

/** Pure decision layer for automatic Wandering Ribbit biome admission. */
final class WanderingRibbitBiomePolicy {
    static final Set<Identifier> VANILLA_BASELINE = Set.of(
            vanilla("swamp"),
            vanilla("mangrove_swamp"),
            vanilla("forest"),
            vanilla("flower_forest"),
            vanilla("birch_forest"),
            vanilla("old_growth_birch_forest"),
            vanilla("dark_forest"),
            vanilla("pale_garden"),
            vanilla("plains"),
            vanilla("sunflower_plains"),
            vanilla("meadow"),
            vanilla("cherry_grove"),
            vanilla("jungle"),
            vanilla("sparse_jungle"),
            vanilla("bamboo_jungle"),
            vanilla("taiga"),
            vanilla("old_growth_pine_taiga"),
            vanilla("old_growth_spruce_taiga"),
            vanilla("mushroom_fields"),
            vanilla("river")
    );

    private WanderingRibbitBiomePolicy() {
    }

    /**
     * An installed allow tag is authoritative, including when it is deliberately empty. When that
     * tag is absent, the exact vanilla baseline and audited family tags provide the fallback.
     */
    static boolean allows(
            Identifier biomeId,
            boolean denied,
            boolean allowTagPresent,
            boolean allowTagMember,
            boolean auditedFamilyMember
    ) {
        if (denied) {
            return false;
        }
        if (allowTagPresent) {
            return allowTagMember;
        }
        return VANILLA_BASELINE.contains(biomeId) || auditedFamilyMember;
    }

    private static Identifier vanilla(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }
}

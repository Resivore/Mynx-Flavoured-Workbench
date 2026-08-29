package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.block.slime.StickyMaterialSemantics;
import net.minecraft.world.level.block.state.BlockState;

/** Extends provider-native sticky classification only to exact CNM runtime bindings. */
public final class ProviderStickyMaterialSemantics {
    private ProviderStickyMaterialSemantics() {}

    public static StickyMaterialSemantics.Family family(BlockState state) {
        StickyMaterialSemantics.Family nativeFamily = StickyMaterialSemantics.family(state);
        if (nativeFamily != StickyMaterialSemantics.Family.NONE) return nativeFamily;
        return NibaruProviderAdapter.runtimeBinding(state.getBlock()).map(binding -> {
            if (binding.profile().capabilities().contains(BehaviorCapability.HONEY_INTERACTION))
                return StickyMaterialSemantics.Family.HONEY;
            if (binding.profile().capabilities().contains(BehaviorCapability.SLIME_INTERACTION))
                return StickyMaterialSemantics.Family.SLIME;
            return StickyMaterialSemantics.Family.NONE;
        }).orElse(StickyMaterialSemantics.Family.NONE);
    }

    public static boolean isDerivedSticky(BlockState state) {
        return NibaruProviderAdapter.runtimeBinding(state.getBlock()).map(binding ->
                binding.profile().capabilities().contains(BehaviorCapability.HONEY_INTERACTION)
                        || binding.profile().capabilities().contains(BehaviorCapability.SLIME_INTERACTION))
                .orElse(false);
    }

    public static boolean opposed(BlockState first, BlockState second) {
        StickyMaterialSemantics.Family a = family(first);
        StickyMaterialSemantics.Family b = family(second);
        return a != StickyMaterialSemantics.Family.NONE && b != StickyMaterialSemantics.Family.NONE && a != b;
    }
}

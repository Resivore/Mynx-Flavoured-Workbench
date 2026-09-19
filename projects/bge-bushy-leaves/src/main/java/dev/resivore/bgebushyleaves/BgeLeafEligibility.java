package dev.resivore.bgebushyleaves;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Optional;

/** Leaf eligibility derives solely from BGE's binding plus canonical leaf semantics. */
public final class BgeLeafEligibility {
    private BgeLeafEligibility() {}

    public static Optional<Binding> binding(BlockState state) {
        Objects.requireNonNull(state, "state");
        return BgeMaterialBindings.fromBlock(state.getBlock()).filter(value -> isLeaf(value, state));
    }

    public static boolean isLeaf(Binding binding, BlockState physicalState) {
        Objects.requireNonNull(binding, "binding");
        return binding.canonicalState(physicalState).filter(canonical ->
                canonical.is(BlockTags.LEAVES) || binding.materialProfile().map(profile ->
                        profile.visualProfile() == VisualProfile.LEAVES_CUTOUT_TINTED
                                && profile.capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE))
                        .orElse(false)).isPresent();
    }

    public static boolean sameCanonicalLeaf(Binding source, BlockState other) {
        return binding(other).map(candidate -> candidate.canonicalMaterial()
                == source.canonicalMaterial()).orElse(other.is(source.canonicalMaterial())
                && other.is(BlockTags.LEAVES));
    }
}

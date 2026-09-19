package dev.resivore.bgeglassculling;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Optional;

/** BGE-canonical glass eligibility and mutual-culling policy, independent of geometry. */
public final class MaterialCompatibility {
    private MaterialCompatibility() {}

    public static boolean isGlassState(BlockState state) {
        return glassBinding(state).isPresent();
    }

    public static boolean mutuallyCullCompatible(BlockState source, BlockState neighbor) {
        Optional<Binding> sourceBinding = glassBinding(source);
        Optional<Binding> neighborBinding = glassBinding(neighbor);
        return sourceBinding.isPresent() && neighborBinding.isPresent()
                && sourceBinding.get().canonicalMaterial()
                        == neighborBinding.get().canonicalMaterial();
    }

    public static Optional<Binding> glassBinding(BlockState state) {
        Objects.requireNonNull(state, "state");
        return BgeMaterialBindings.fromBlock(state.getBlock()).filter(binding ->
                binding.materialProfile().map(profile ->
                        profile.visualProfile() == VisualProfile.GLASS_EDGE
                                && profile.capabilities().contains(
                                        BehaviorCapability.TRANSLUCENT_ADJACENCY))
                        .orElse(false));
    }
}

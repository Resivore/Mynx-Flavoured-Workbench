package dev.aero.cnmterraincompat;

import net.minecraft.world.level.block.state.BlockState;

/** Placement-only conversion of completed supported geometry to its canonical material block. */
public final class FullOccupancyNormalizer {
    private FullOccupancyNormalizer() {}

    /**
     * Receives the state already accepted by the block's existing placement implementation.
     * Returning the input on any incomplete, special, or invalid projection keeps placement
     * compatibility fail-closed.
     */
    public static BlockState normalize(BlockState placementState) {
        if (placementState == null) return null;
        return BgeMaterialBindings.fromBlock(placementState.getBlock())
                .filter(BgeMaterialBindings.Binding::fullOccupancyNormalizationEnabled)
                .filter(binding -> binding.topology().isFullOccupancy(placementState))
                .flatMap(binding -> binding.canonicalState(placementState))
                .orElse(placementState);
    }
}

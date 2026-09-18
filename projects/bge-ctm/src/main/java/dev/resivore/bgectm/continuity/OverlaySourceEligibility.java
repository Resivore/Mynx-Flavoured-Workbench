package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.CanonicalAppearanceResolver;
import net.minecraft.world.level.block.state.BlockState;

/** Exact, typed exception to Continuity's native full-collision source gate. */
public final class OverlaySourceEligibility {
    private OverlaySourceEligibility() {}

    /**
     * Permits only a supported BGE carrier with a canonical material projection to continue
     * through Continuity's own connectBlocks/connectTiles/predicate checks. It grants no final
     * overlay result and does not change collision, physics, or arbitrary partial blocks.
     */
    public static boolean mayReachCanonicalSemantics(BlockState physicalSource) {
        return CanonicalAppearanceResolver.inspect(physicalSource).inherited();
    }
}

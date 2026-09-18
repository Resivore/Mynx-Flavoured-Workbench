package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.CanonicalAppearanceResolver;
import dev.resivore.bgectm.SurfaceContactResolver;

/** Replaces only Continuity's full-unit geometry gate after its semantic predicates pass. */
public final class OverlayProcessingEligibility {
    private OverlayProcessingEligibility() {}

    public static boolean allows(boolean nativeUnitSquare, ContinuityQuadContext.Capture capture) {
        if (nativeUnitSquare) return true;
        return capture != null && capture.valid() && capture.receiverState() != null
                && CanonicalAppearanceResolver.inspect(capture.receiverState()).inherited()
                && SurfaceContactResolver.matchRenderedSurface(
                        capture.receiverState(), capture.surface()).isPresent();
    }
}

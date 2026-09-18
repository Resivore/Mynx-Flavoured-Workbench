package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.continuity.ContinuityQuadContext;
import dev.resivore.bgectm.continuity.OverlayProcessingEligibility;
import me.pepperbell.continuity.client.processor.overlay.OverlayProcessingPredicate;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets an authoritative BGE patch pass Continuity's otherwise full-unit receiver-quad gate. */
@Mixin(OverlayProcessingPredicate.class)
abstract class ContinuityOverlayProcessingMixin {
    @Redirect(method = "shouldProcessQuad",
            at = @At(value = "INVOKE", target = "Lme/pepperbell/continuity/client/util/QuadUtil;"
                    + "isQuadUnitSquare(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadView;)Z"),
            require = 1)
    private boolean bgeCtm$allowAuthoritativePatch(QuadView quad) {
        return OverlayProcessingEligibility.allows(
                QuadUtil.isQuadUnitSquare(quad), ContinuityQuadContext.current());
    }
}

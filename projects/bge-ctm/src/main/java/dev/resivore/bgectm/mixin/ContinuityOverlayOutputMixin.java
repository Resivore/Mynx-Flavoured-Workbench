package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.continuity.OverlayRenderCoordinator;
import me.pepperbell.continuity.client.model.CtmBlockStateModel;
import me.pepperbell.continuity.impl.client.ProcessingContextImpl;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Places receiver subdivisions before Continuity's buffered Standard Overlay quads. */
@Mixin(CtmBlockStateModel.class)
abstract class ContinuityOverlayOutputMixin {
    @Redirect(method = "emitQuads",
            at = @At(value = "INVOKE",
                    target = "Lme/pepperbell/continuity/impl/client/ProcessingContextImpl;"
                            + "outputTo(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/"
                            + "QuadEmitter;)V"),
            require = 1)
    private void bgeCtm$outputReceiverPiecesFirst(ProcessingContextImpl context,
            QuadEmitter emitter) {
        OverlayRenderCoordinator.outputBasePieces(emitter);
        context.outputTo(emitter);
    }
}

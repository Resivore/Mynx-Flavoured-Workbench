package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.continuity.OverlayEmissionController;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces only the exact Standard Overlay QuadUtil emission callsite. */
@Mixin(StandardOverlayQuadProcessor.class)
abstract class ContinuityOverlayEmissionMixin {
    @Redirect(method = "processQuadInner",
            at = @At(value = "INVOKE", target = "Lme/pepperbell/continuity/client/util/QuadUtil;"
                    + "emitOverlayQuad(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;"
                    + "Lnet/minecraft/core/Direction;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
                    + "ILnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"
                    + "Lnet/fabricmc/fabric/api/util/TriState;)V"), require = 1)
    private void bgeCtm$emitOnCapturedReceiver(QuadEmitter emitter, Direction face,
            TextureAtlasSprite sprite, int tint, ChunkSectionLayer layer, TriState ao) {
        OverlayEmissionController.emit(emitter, face, sprite, tint, layer, ao);
    }
}

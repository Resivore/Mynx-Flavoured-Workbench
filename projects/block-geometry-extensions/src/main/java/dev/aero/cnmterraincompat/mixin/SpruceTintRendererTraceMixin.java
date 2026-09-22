package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.SpruceTintRenderTrace;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the real Minecraft 26.2 quad submission seam; it never changes the submitted quad. */
@Mixin(ModelBlockRenderer.class)
abstract class SpruceTintRendererTraceMixin {
    @Inject(method = "putQuadWithTint", at = @At("HEAD"), require = 1)
    private void bge$traceSpruceTintAtRendererSeam(BlockQuadOutput output, float offsetX,
            float offsetY, float offsetZ, BlockAndTintGetter level, BlockState state,
            BlockPos pos, BakedQuad quad, CallbackInfo ci) {
        SpruceTintRenderTrace.beforeQuadColorMultiply(level, state, pos, quad);
    }
}

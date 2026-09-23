package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.SodiumSpruceTerrainTrace;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional C98 hook for the exact Sodium 0.9.1 terrain BlockRenderer implementation. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
abstract class SodiumSpruceTerrainTraceMixin {
    @Shadow @Final private ChunkVertexEncoder.Vertex[] vertices;

    @Inject(method = "renderModel", at = @At("HEAD"), require = 1)
    private void bge$beginSodiumSpruceTrace(BlockStateModel model, BlockState state, BlockPos pos,
            BlockPos origin, CallbackInfo ci) {
        SodiumSpruceTerrainTrace.beginModel(state, pos);
    }

    @Inject(method = "renderModel", at = @At("RETURN"), require = 1)
    private void bge$endSodiumSpruceTrace(BlockStateModel model, BlockState state, BlockPos pos,
            BlockPos origin, CallbackInfo ci) {
        SodiumSpruceTerrainTrace.endModel();
    }

    @Redirect(method = "tintQuad",
            at = @At(value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/model/color/ColorProvider;getColors(Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos$MutableBlockPos;Ljava/lang/Object;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;[IZ)V"),
            require = 1)
    private void bge$traceSodiumResolvedTint(ColorProvider<BlockState> provider, LevelSlice slice,
            BlockPos pos, BlockPos.MutableBlockPos scratchPos, Object state, ModelQuadView quad,
            int[] vertexColors, boolean biomeBlend) {
        provider.getColors(slice, pos, scratchPos, (BlockState) state, quad, vertexColors, biomeBlend);
        MutableQuadViewImpl mutableQuad = (MutableQuadViewImpl) quad;
        int[] modelArgb = new int[] {
                mutableQuad.baseColor(0), mutableQuad.baseColor(1), mutableQuad.baseColor(2),
                mutableQuad.baseColor(3)
        };
        SodiumSpruceTerrainTrace.afterColorProvider(provider, quad.getTintIndex(), vertexColors, modelArgb);
    }

    @Inject(method = "bufferQuad", at = @At("TAIL"), require = 1)
    private void bge$traceSodiumVertexWrite(MutableQuadViewImpl quad, float[] brightnesses,
            Material material, CallbackInfo ci) {
        int[] packedAbgr = new int[] {
                vertices[0].color, vertices[1].color, vertices[2].color, vertices[3].color
        };
        SodiumSpruceTerrainTrace.afterVertexWrite(quad.getTintIndex(), packedAbgr);
    }
}

package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.BgeCtmDiagnostics;
import dev.resivore.bgectm.CanonicalAppearanceResolver;
import dev.resivore.bgectm.SurfaceContactResolver;
import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import dev.resivore.bgectm.SurfaceContactResolver.SurfaceMatch;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Narrow C6 replacement for Continuity's one Standard Overlay emission callsite. */
public final class OverlayEmissionController {
    private OverlayEmissionController() {}

    public static void emit(QuadEmitter emitter, Direction face, TextureAtlasSprite sprite, int tint,
            ChunkSectionLayer layer, TriState ao) {
        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        if (capture == null || capture.receiverState() == null || capture.receiverPos() == null) {
            // This can only occur outside BGE × CTM's exact CtmQuadTransform wrapper. It is not
            // safe to classify the receiver, so Continuity's unrelated behavior remains untouched.
            QuadUtil.emitOverlayQuad(emitter, face, sprite, tint, layer, ao);
            return;
        }

        BlockState receiver = capture.receiverState();
        if (!CanonicalAppearanceResolver.inspect(receiver).inherited()) {
            QuadUtil.emitOverlayQuad(emitter, face, sprite, tint, layer, ao);
            return;
        }

        Resolution resolution = resolve(receiver, face, capture);
        if (resolution.kind == Kind.VETO) {
            BgeCtmDiagnostics.overlayEmit(receiver, face, capture.surface(), null,
                    "VETO", resolution.reason);
            return;
        }
        if (resolution.kind == Kind.ORIGINAL) {
            BgeCtmDiagnostics.overlayEmit(receiver, face, resolution.surface, null,
                    "ORIGINAL", resolution.reason);
            QuadUtil.emitOverlayQuad(emitter, face, sprite, tint, layer, ao);
            return;
        }

        OverlayEmissionGeometry.Projection projected = resolution.projection;
        emitter.square(face, projected.left(), projected.bottom(), projected.right(), projected.top(),
                projected.depth());
        emitter.color(tint, tint, tint, tint);
        for (int vertex = 0; vertex < 4; vertex++) {
            emitter.uv(vertex, interpolate(sprite.getU0(), sprite.getU1(), projected.uvU(vertex)),
                    interpolate(sprite.getV0(), sprite.getV1(), projected.uvV(vertex)));
        }
        emitter.atlas(QuadAtlas.BLOCK);
        emitter.animated(sprite.contents().isAnimated());
        emitter.chunkLayer(layer);
        emitter.itemRenderType(layer == ChunkSectionLayer.TRANSLUCENT
                ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet());
        emitter.ambientOcclusion(ao);
        emitter.emit();
        BgeCtmDiagnostics.overlayEmit(receiver, face, resolution.surface, projected,
                "PROJECTED", resolution.reason);
    }

    private static Resolution resolve(BlockState receiver, Direction face,
            ContinuityQuadContext.Capture capture) {
        QuadSurface exact = capture.surface();
        if (exact != null) {
            if (exact.normal() != face) return Resolution.veto("SURFACE_UNSUPPORTED");
            Optional<SurfaceMatch> match = SurfaceContactResolver.matchRenderedSurface(receiver, exact);
            if (match.isEmpty()) return Resolution.veto("SURFACE_CAPTURE_UNMATCHED");
            QuadSurface presentation = match.get().presentation();
            String reason = match.get().planeRelation()
                    == dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation.TERRAIN_HEIGHT_INSET
                    ? "TERRAIN_NOMINAL_PRESENTATION" : "EMIT_MATCH_RECEIVER";
            return OverlayEmissionGeometry.originalUnitSquareMatches(presentation)
                    ? Resolution.original(presentation, reason)
                    : Resolution.projected(presentation, reason);
        }
        Optional<QuadSurface> fallback = SurfaceContactResolver.describeLocalForOverlay(receiver, face);
        if (fallback.isEmpty()) return Resolution.veto("SURFACE_CAPTURE_MISSING");
        Optional<SurfaceMatch> match = SurfaceContactResolver.matchRenderedSurface(receiver, fallback.get());
        if (match.isEmpty()) return Resolution.veto("STATE_SURFACE_UNMATCHED");
        QuadSurface presentation = match.get().presentation();
        String reason = match.get().planeRelation()
                == dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation.TERRAIN_HEIGHT_INSET
                ? "STATE_TERRAIN_NOMINAL_PRESENTATION" : "STATE_DERIVED_PROJECTED";
        return OverlayEmissionGeometry.originalUnitSquareMatches(presentation)
                ? Resolution.original(presentation, reason)
                : Resolution.projected(presentation, reason);
    }

    private static float interpolate(float min, float max, float fraction) {
        return min + (max - min) * fraction;
    }

    private enum Kind { ORIGINAL, PROJECTED, VETO }

    private static final class Resolution {
        final Kind kind;
        final QuadSurface surface;
        final OverlayEmissionGeometry.Projection projection;
        final String reason;

        private Resolution(Kind kind, QuadSurface surface,
                OverlayEmissionGeometry.Projection projection, String reason) {
            this.kind = kind;
            this.surface = surface;
            this.projection = projection;
            this.reason = reason;
        }

        static Resolution original(QuadSurface surface) {
            return original(surface, "FULL_RECEIVER_ORIGINAL");
        }

        static Resolution original(QuadSurface surface, String reason) {
            return new Resolution(Kind.ORIGINAL, surface, null, reason);
        }

        static Resolution projected(QuadSurface surface, String reason) {
            return new Resolution(Kind.PROJECTED, surface,
                    OverlayEmissionGeometry.project(surface), reason);
        }

        static Resolution veto(String reason) {
            return new Resolution(Kind.VETO, null, null, reason);
        }
    }
}

package dev.resivore.bgectm.continuity;

import dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation;
import dev.resivore.bgectm.BgeCtmDiagnostics;
import dev.resivore.bgectm.CanonicalAppearanceResolver;
import dev.resivore.bgectm.SurfaceContactResolver;
import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import dev.resivore.bgectm.SurfaceContactResolver.SurfaceMatch;
import dev.resivore.bgectm.continuity.ContinuityQuadContext.OverlaySpriteContribution;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Narrow replacement for Continuity's Standard Overlay emission callsite. */
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
        Optional<OverlaySpriteContribution> spriteContribution = capture.nextOverlaySprite();
        List<QuadSurface> contributionFootprints = spriteContribution
                .map(OverlaySpriteContribution::footprints).orElseGet(List::of);
        boolean inheritedReceiver = CanonicalAppearanceResolver.inspect(receiver).inherited();
        if (contributionFootprints.isEmpty() && !inheritedReceiver) {
            QuadUtil.emitOverlayQuad(emitter, face, sprite, tint, layer, ao);
            return;
        }
        if (spriteContribution.isPresent() && contributionFootprints.isEmpty()) {
            if (BgeCtmDiagnostics.enabled()) {
                BgeCtmDiagnostics.overlayEmit(receiver, face, capture.surface(), null,
                        "VETO", "CONTRIBUTION_PROVENANCE_MISSING");
            }
            return;
        }

        Resolution resolution = resolve(receiver, face, capture, contributionFootprints);
        if (resolution.surfaces.isEmpty()) {
            if (BgeCtmDiagnostics.enabled()) {
                BgeCtmDiagnostics.overlayEmit(receiver, face, capture.surface(), null,
                        "VETO", resolution.reason);
            }
            return;
        }
        // Native Continuity emits its extra quads after all receiver quads. Managed overlays are
        // delayed one step further so the final receiver quad and every logical overlay can be
        // subdivided at the same authoritative edges. This removes the only render-path
        // difference that made clipped coplanar triangles depth-unstable.
        capture.addPendingOverlay(face, sprite, tint, layer, ao,
                resolution.surfaces, resolution.reason);
    }

    /** Exact Continuity 3.0.1 overlay attributes, changing only geometry bounds and UV crop. */
    @Nullable
    static OverlayEmissionGeometry.Projection emitSurface(QuadEmitter emitter, Direction face,
            TextureAtlasSprite sprite, int tint, ChunkSectionLayer layer, TriState ao, QuadSurface surface) {
        if (OverlayEmissionGeometry.originalUnitSquareMatches(surface)) {
            QuadUtil.emitOverlayQuad(emitter, face, sprite, tint, layer, ao);
            return null;
        }
        OverlayEmissionGeometry.Projection projected = OverlayEmissionGeometry.project(surface);
        emitter.square(face, projected.left(), projected.bottom(), projected.right(),
                projected.top(), projected.depth());
        for (int vertex = 0; vertex < 4; vertex++) {
            emitter.uv(vertex, interpolate(sprite.getU0(), sprite.getU1(), projected.uvU(vertex)),
                    interpolate(sprite.getV0(), sprite.getV1(), projected.uvV(vertex)));
        }
        OverlayRenderState.continuity(sprite, tint, layer, ao).apply(emitter);
        emitter.emit();
        return projected;
    }

    private static Resolution resolve(BlockState receiver, Direction face,
            ContinuityQuadContext.Capture capture, List<QuadSurface> contributionFootprints) {
        QuadSurface exact = capture.surface();
        if (!contributionFootprints.isEmpty()) {
            if (exact == null || exact.normal() != face) {
                return Resolution.veto("CONTRIBUTION_SURFACE_UNSUPPORTED");
            }
            Optional<SurfaceMatch> receiverMatch =
                    SurfaceContactResolver.matchRenderedSurface(receiver, exact);
            if (receiverMatch.isEmpty()) {
                return Resolution.veto("CONTRIBUTION_RECEIVER_UNMATCHED");
            }
            SurfaceMatch matched = receiverMatch.get();
            int presentationPlane = matched.presentation().plane16();
            List<QuadSurface> presentationContributions = new ArrayList<>();
            for (QuadSurface footprint : contributionFootprints) {
                if (footprint.normal() != exact.normal()
                        || footprint.plane16() != exact.plane16()
                        || footprint.uAxis() != exact.uAxis()
                        || footprint.vAxis() != exact.vAxis()) {
                    return Resolution.veto("CONTRIBUTION_SURFACE_MISMATCH");
                }
                presentationContributions.add(
                        OverlayFootprintPlan.onPlane(footprint, presentationPlane));
            }
            List<QuadSurface> regions = OverlayFootprintPlan.partition(presentationContributions);
            String reason = diagnosticReason(matched, "CONTRIBUTION_FOOTPRINT",
                    "CONTRIBUTION_TERRAIN_NOMINAL_PRESENTATION");
            return regions.isEmpty() ? Resolution.veto("EMPTY_CONTRIBUTION_FOOTPRINT")
                    : new Resolution(regions, reason);
        }
        if (exact != null) {
            if (exact.normal() != face) return Resolution.veto("SURFACE_UNSUPPORTED");
            Optional<SurfaceMatch> match = SurfaceContactResolver.matchRenderedSurface(receiver, exact);
            if (match.isEmpty()) return Resolution.veto("SURFACE_CAPTURE_UNMATCHED");
            SurfaceMatch matched = match.get();
            QuadSurface presentation = matched.presentation();
            String reason = diagnosticReason(matched, "EMIT_MATCH_RECEIVER",
                    "TERRAIN_NOMINAL_PRESENTATION");
            return new Resolution(List.of(presentation), reason);
        }
        Optional<QuadSurface> fallback = SurfaceContactResolver.describeLocalForOverlay(receiver, face);
        if (fallback.isEmpty()) return Resolution.veto("SURFACE_CAPTURE_MISSING");
        Optional<SurfaceMatch> match = SurfaceContactResolver.matchRenderedSurface(receiver, fallback.get());
        if (match.isEmpty()) return Resolution.veto("STATE_SURFACE_UNMATCHED");
        SurfaceMatch matched = match.get();
        QuadSurface presentation = matched.presentation();
        String reason = diagnosticReason(matched, "STATE_DERIVED_PROJECTED",
                    "STATE_TERRAIN_NOMINAL_PRESENTATION");
        return new Resolution(List.of(presentation), reason);
    }

    private static float interpolate(float min, float max, float fraction) {
        return min + (max - min) * fraction;
    }

    @Nullable
    private static String diagnosticReason(SurfaceMatch match, String exact, String terrain) {
        if (!BgeCtmDiagnostics.enabled()) return null;
        return match.planeRelation() == PlaneRelation.TERRAIN_HEIGHT_INSET ? terrain : exact;
    }

    private record Resolution(List<QuadSurface> surfaces, @Nullable String reason) {
        static Resolution veto(String reason) {
            return new Resolution(List.of(), reason);
        }
    }
}

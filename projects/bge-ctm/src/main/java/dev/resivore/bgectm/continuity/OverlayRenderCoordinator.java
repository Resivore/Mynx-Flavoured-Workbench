package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.BgeCtmDiagnostics;
import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import dev.resivore.bgectm.continuity.ContinuityQuadContext.PendingOverlay;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes projected Standard Overlay geometry depth-equivalent to native Continuity geometry.
 * Native overlays and their receiver use identical full-face triangles. Cropping only the overlay
 * changed that invariant, so this coordinator delays managed overlays, derives one shared cell
 * grid, subdivides the receiver on its physical plane, and emits every logical overlay on those
 * same cells. No plane is offset and typed terrain presentation remains on its nominal plane.
 */
public final class OverlayRenderCoordinator {
    private static final ThreadLocal<MutableMesh> BASE_PIECES = new ThreadLocal<>();

    private OverlayRenderCoordinator() {}

    /** Finalizes one source quad after every Continuity processor and pass has run. */
    public static void finishQuad(MutableQuadView receiverQuad, QuadEmitter overlayEmitter,
            boolean receiverRetained, ContinuityQuadContext.Capture capture) {
        if (capture == null || capture.pendingOverlays().isEmpty()) return;

        List<PendingOverlay> pending = capture.pendingOverlays();
        Map<SurfaceKey, List<QuadSurface>> regionsByPlane = new LinkedHashMap<>();
        for (PendingOverlay overlay : pending) {
            for (QuadSurface surface : overlay.surfaces()) {
                regionsByPlane.computeIfAbsent(SurfaceKey.of(surface), ignored -> new ArrayList<>())
                        .add(surface);
            }
        }

        QuadSurface physicalReceiver = capture.surface();
        SurfaceKey receiverKey = physicalReceiver == null ? null : SurfaceKey.of(physicalReceiver);
        Map<SurfaceKey, List<QuadSurface>> cellsByPlane = new LinkedHashMap<>();
        for (Map.Entry<SurfaceKey, List<QuadSurface>> entry : regionsByPlane.entrySet()) {
            SurfaceKey key = entry.getKey();
            List<QuadSurface> cells = receiverRetained && key.equals(receiverKey)
                    ? OverlayFootprintPlan.tessellateReceiver(physicalReceiver, entry.getValue())
                    : OverlayFootprintPlan.tessellateUnion(entry.getValue());
            cellsByPlane.put(key, cells);
        }

        if (receiverRetained && receiverKey != null && cellsByPlane.containsKey(receiverKey)) {
            subdivideReceiver(receiverQuad, physicalReceiver, cellsByPlane.get(receiverKey));
        }

        for (PendingOverlay overlay : pending) {
            Map<SurfaceKey, List<QuadSurface>> contributionByPlane = new LinkedHashMap<>();
            for (QuadSurface surface : overlay.surfaces()) {
                contributionByPlane.computeIfAbsent(SurfaceKey.of(surface),
                        ignored -> new ArrayList<>()).add(surface);
            }
            for (Map.Entry<SurfaceKey, List<QuadSurface>> entry : contributionByPlane.entrySet()) {
                List<QuadSurface> cells = OverlayFootprintPlan.coveredCells(
                        cellsByPlane.getOrDefault(entry.getKey(), List.of()), entry.getValue());
                for (QuadSurface cell : cells) {
                    OverlayEmissionController.emitSurface(overlayEmitter, overlay.face(),
                            overlay.sprite(), overlay.tint(), overlay.layer(), overlay.ao(), cell);
                    boolean original = OverlayEmissionGeometry.originalUnitSquareMatches(cell);
                    BgeCtmDiagnostics.overlayEmit(capture.receiverState(), overlay.face(), cell,
                            original ? null : OverlayEmissionGeometry.project(cell),
                            original ? "ORIGINAL" : "PROJECTED",
                            overlay.reason() + "+SHARED_TESSELLATION");
                }
            }
        }
    }

    /** Outputs receiver subdivisions before Continuity's buffered overlay mesh. */
    public static void outputBasePieces(QuadEmitter output) {
        MutableMesh mesh = BASE_PIECES.get();
        if (mesh == null) return;
        try {
            mesh.outputTo(output);
        } finally {
            mesh.clear();
            BASE_PIECES.remove();
        }
    }

    private static void subdivideReceiver(MutableQuadView receiverQuad, QuadSurface receiver,
            List<QuadSurface> cells) {
        if (cells.size() <= 1) return;
        QuadTemplate template = QuadTemplate.capture(receiverQuad, receiver);
        MutableMesh mesh = BASE_PIECES.get();
        if (mesh == null) {
            mesh = Renderer.get().mutableMesh();
            BASE_PIECES.set(mesh);
        }
        QuadEmitter emitter = mesh.emitter();
        for (int index = 1; index < cells.size(); index++) {
            emitter.copyFrom(receiverQuad);
            template.apply(emitter, cells.get(index));
            emitter.emit();
        }
        template.apply(receiverQuad, cells.getFirst());
    }

    private record SurfaceKey(Direction normal, int plane16, Direction.Axis uAxis,
            Direction.Axis vAxis) {
        static SurfaceKey of(QuadSurface surface) {
            return new SurfaceKey(surface.normal(), surface.plane16(),
                    surface.uAxis(), surface.vAxis());
        }
    }

    /** Immutable final receiver attributes used to crop every generated base cell consistently. */
    private static final class QuadTemplate {
        private final QuadSurface receiver;
        private final float[] u = new float[4];
        private final float[] v = new float[4];
        private final int[] color = new int[4];
        private final int[] lightmap = new int[4];
        private final boolean[] hasNormal = new boolean[4];
        private final float[] normalX = new float[4];
        private final float[] normalY = new float[4];
        private final float[] normalZ = new float[4];

        private QuadTemplate(QuadView quad, QuadSurface receiver) {
            this.receiver = receiver;
            for (int vertex = 0; vertex < 4; vertex++) {
                u[vertex] = quad.u(vertex);
                v[vertex] = quad.v(vertex);
                color[vertex] = quad.color(vertex);
                lightmap[vertex] = quad.lightmap(vertex);
                hasNormal[vertex] = quad.hasNormal(vertex);
                if (hasNormal[vertex]) {
                    normalX[vertex] = quad.normalX(vertex);
                    normalY[vertex] = quad.normalY(vertex);
                    normalZ[vertex] = quad.normalZ(vertex);
                }
            }
        }

        static QuadTemplate capture(QuadView quad, QuadSurface receiver) {
            return new QuadTemplate(quad, receiver);
        }

        void apply(MutableQuadView quad, QuadSurface cell) {
            OverlayEmissionGeometry.Projection receiverProjection =
                    OverlayEmissionGeometry.project(receiver);
            OverlayEmissionGeometry.Projection cellProjection =
                    OverlayEmissionGeometry.project(cell);
            quad.square(cell.normal(), cellProjection.left(), cellProjection.bottom(),
                    cellProjection.right(), cellProjection.top(), cellProjection.depth());
            float baseU0 = receiverProjection.uvU(0);
            float baseU1 = receiverProjection.uvU(2);
            float baseV0 = receiverProjection.uvV(0);
            float baseV1 = receiverProjection.uvV(1);
            for (int vertex = 0; vertex < 4; vertex++) {
                float fu = fraction(cellProjection.uvU(vertex), baseU0, baseU1);
                float fv = fraction(cellProjection.uvV(vertex), baseV0, baseV1);
                quad.uv(vertex, bilerp(u, fu, fv), bilerp(v, fu, fv));
                quad.color(vertex, bilerpColor(color, fu, fv));
                quad.lightmap(vertex, bilerpPacked(lightmap, fu, fv));
                if (hasNormal[0] && hasNormal[1] && hasNormal[2] && hasNormal[3]) {
                    float nx = bilerp(normalX, fu, fv);
                    float ny = bilerp(normalY, fu, fv);
                    float nz = bilerp(normalZ, fu, fv);
                    float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (length > 0) quad.normal(vertex, nx / length, ny / length, nz / length);
                }
            }
        }

        private static float fraction(float value, float min, float max) {
            return max == min ? 0 : (value - min) / (max - min);
        }

        private static float bilerp(float[] values, float u, float v) {
            float top = lerp(values[0], values[3], u);
            float bottom = lerp(values[1], values[2], u);
            return lerp(top, bottom, v);
        }

        private static int bilerpColor(int[] values, float u, float v) {
            int result = 0;
            for (int shift = 0; shift < 32; shift += 8) {
                float[] channel = new float[4];
                for (int i = 0; i < 4; i++) channel[i] = (values[i] >>> shift) & 0xFF;
                result |= Math.round(bilerp(channel, u, v)) << shift;
            }
            return result;
        }

        private static int bilerpPacked(int[] values, float u, float v) {
            float[] low = new float[4];
            float[] high = new float[4];
            for (int i = 0; i < 4; i++) {
                low[i] = values[i] & 0xFFFF;
                high[i] = (values[i] >>> 16) & 0xFFFF;
            }
            return (Math.round(bilerp(high, u, v)) << 16)
                    | (Math.round(bilerp(low, u, v)) & 0xFFFF);
        }

        private static float lerp(float a, float b, float amount) {
            return a + (b - a) * amount;
        }
    }
}

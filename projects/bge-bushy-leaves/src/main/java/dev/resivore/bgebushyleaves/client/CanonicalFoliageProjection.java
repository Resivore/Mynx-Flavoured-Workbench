package dev.resivore.bgebushyleaves.client;

import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfacePatch;
import dev.resivore.bgebushyleaves.FoliageSurfaceResolver;
import dev.resivore.bgebushyleaves.geometry.ExteriorProjection;
import dev.resivore.bgebushyleaves.geometry.PatchFrame;
import dev.resivore.bgebushyleaves.geometry.PatchMerger;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/** Copies active canonical model metadata while changing only foliage-quad positions. */
final class CanonicalFoliageProjection {
    private CanonicalFoliageProjection() {}

    static void emit(MutableMesh canonicalMesh, QuadEmitter output, Binding binding,
            BlockState physical, BlockAndTintGetter level, BlockPos pos) {
        List<PatchFrame> patches = PatchMerger.mergeSurfacePatches(binding.surfaceModel(physical).patches());
        canonicalMesh.forEach(source -> ExteriorQuad.inspect(source).ifPresent(exterior -> {
            for (PatchFrame patch : patches) {
                List<ExteriorProjection.Projection> projections = ExteriorProjection.plan(exterior.source(), patch,
                        FoliageSurfaceResolver.hiddenRegions(binding, physical, level, pos, patch));
                for (ExteriorProjection.Projection projection : projections) emitProjection(source, output, exterior, projection);
            }
        }));
    }

    private static void emitProjection(QuadView source, QuadEmitter output, ExteriorQuad exterior,
            ExteriorProjection.Projection projection) {
        output.copyFrom(source);
        for (int uCorner = 0; uCorner < 2; uCorner++) for (int vCorner = 0; vCorner < 2; vCorner++) {
            int vertex = exterior.vertex(uCorner, vCorner);
            int targetU = uCorner == 0 ? projection.bounds().uMin() : projection.bounds().uMax();
            int targetV = vCorner == 0 ? projection.bounds().vMin() : projection.bounds().vMax();
            int canonicalU = projection.canonicalU(targetU), canonicalV = projection.canonicalV(targetV);
            float uFraction = fraction(canonicalU, exterior.uMin16, exterior.uMax16);
            float vFraction = fraction(canonicalV, exterior.vMin16, exterior.vMax16);
            setPosition(output, vertex, projection, targetU, targetV);
            output.uv(vertex, bilinear(source, exterior, Attribute.U, uFraction, vFraction),
                    bilinear(source, exterior, Attribute.V, uFraction, vFraction));
            output.color(vertex, bilinearInt(source, exterior, Attribute.COLOR, uFraction, vFraction));
            output.lightmap(vertex, bilinearInt(source, exterior, Attribute.LIGHT, uFraction, vFraction));
            setNormal(source, output, exterior, vertex, uFraction, vFraction);
        }
        // Projected foliage is a visual shell, never a physical/cull face in BGE's blockspace.
        output.cullFace(null);
        output.emit();
    }

    private static void setPosition(QuadEmitter output, int vertex, ExteriorProjection.Projection projection,
            int targetU, int targetV) {
        PatchFrame target = projection.target();
        float normal = projection.plane16() / 16.0F, u = targetU / 16.0F, v = targetV / 16.0F;
        output.pos(vertex, coordinate(Direction.Axis.X, target, normal, u, v),
                coordinate(Direction.Axis.Y, target, normal, u, v), coordinate(Direction.Axis.Z, target, normal, u, v));
    }
    private static float coordinate(Direction.Axis axis, PatchFrame frame, float normal, float u, float v) {
        if (axis == frame.normal().getAxis()) return normal;
        if (axis == frame.uAxis()) return u;
        if (axis == frame.vAxis()) return v;
        throw new IllegalArgumentException("Axis is not in foliage frame");
    }
    private static float fraction(int value, int min, int max) { return (float) (value - min) / (float) (max - min); }
    private static float bilinear(QuadView quad, ExteriorQuad frame, Attribute attribute, float u, float v) {
        float a = attribute.floatValue(quad, frame.vertex(0, 0));
        float b = attribute.floatValue(quad, frame.vertex(1, 0));
        float c = attribute.floatValue(quad, frame.vertex(0, 1));
        float d = attribute.floatValue(quad, frame.vertex(1, 1));
        return a * (1 - u) * (1 - v) + b * u * (1 - v) + c * (1 - u) * v + d * u * v;
    }
    private static int bilinearInt(QuadView quad, ExteriorQuad frame, Attribute attribute, float u, float v) {
        int a = attribute.intValue(quad, frame.vertex(0, 0)); int b = attribute.intValue(quad, frame.vertex(1, 0));
        int c = attribute.intValue(quad, frame.vertex(0, 1)); int d = attribute.intValue(quad, frame.vertex(1, 1));
        return Math.round(a * (1 - u) * (1 - v) + b * u * (1 - v) + c * (1 - u) * v + d * u * v);
    }
    private static void setNormal(QuadView source, QuadEmitter output, ExteriorQuad frame, int vertex, float u, float v) {
        for (int ui = 0; ui < 2; ui++) for (int vi = 0; vi < 2; vi++) if (!source.hasNormal(frame.vertex(ui, vi))) return;
        float x = bilinear(source, frame, Attribute.NORMAL_X, u, v), y = bilinear(source, frame, Attribute.NORMAL_Y, u, v), z = bilinear(source, frame, Attribute.NORMAL_Z, u, v);
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length > 0.0F) output.normal(vertex, x / length, y / length, z / length);
    }

    private enum Attribute {
        U { float floatValue(QuadView q, int v) { return q.u(v); } }, V { float floatValue(QuadView q, int v) { return q.v(v); } },
        NORMAL_X { float floatValue(QuadView q, int v) { return q.normalX(v); } }, NORMAL_Y { float floatValue(QuadView q, int v) { return q.normalY(v); } }, NORMAL_Z { float floatValue(QuadView q, int v) { return q.normalZ(v); } },
        COLOR { int intValue(QuadView q, int v) { return q.color(v); } }, LIGHT { int intValue(QuadView q, int v) { return q.lightmap(v); } };
        float floatValue(QuadView q, int v) { throw new UnsupportedOperationException(); }
        int intValue(QuadView q, int v) { throw new UnsupportedOperationException(); }
    }

    /** Exact-grid, face-parallel exterior model quad; anything else is deliberately ignored. */
    private record ExteriorQuad(Direction normal, int plane16, Direction.Axis uAxis, Direction.Axis vAxis,
            int uMin16, int uMax16, int vMin16, int vMax16, int[][] corners) {
        static Optional<ExteriorQuad> inspect(QuadView quad) {
            Direction normal = quad.lightFace(); if (normal == null) return Optional.empty();
            Direction.Axis[] axes = inPlaneAxes(normal.getAxis()); int[] ns = new int[4], us = new int[4], vs = new int[4];
            try { for (int i = 0; i < 4; i++) { ns[i] = units(coordinate(quad, i, normal.getAxis())); us[i] = units(coordinate(quad, i, axes[0])); vs[i] = units(coordinate(quad, i, axes[1])); } }
            catch (IllegalArgumentException ignored) { return Optional.empty(); }
            int plane = ns[0]; for (int n : ns) if (n != plane) return Optional.empty();
            boolean exterior = normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? plane > 16 : plane < 0;
            int uMin = min(us), uMax = max(us), vMin = min(vs), vMax = max(vs); if (!exterior || uMin >= uMax || vMin >= vMax) return Optional.empty();
            int[][] corners = {{-1, -1}, {-1, -1}};
            for (int i = 0; i < 4; i++) { int uc = us[i] == uMin ? 0 : us[i] == uMax ? 1 : -1, vc = vs[i] == vMin ? 0 : vs[i] == vMax ? 1 : -1; if (uc < 0 || vc < 0 || corners[uc][vc] >= 0) return Optional.empty(); corners[uc][vc] = i; }
            for (int[] row : corners) for (int vertex : row) if (vertex < 0) return Optional.empty();
            return Optional.of(new ExteriorQuad(normal, plane, axes[0], axes[1], uMin, uMax, vMin, vMax, corners));
        }
        ExteriorProjection.Source source() { return new ExteriorProjection.Source(normal, plane16, uMin16, uMax16, vMin16, vMax16); }
        int vertex(int u, int v) { return corners[u][v]; }
        private static int units(float value) { int units = Math.round(value * 16); if (Float.compare(value, units / 16.0F) != 0) throw new IllegalArgumentException("non-grid"); return units; }
        private static float coordinate(QuadView quad, int vertex, Direction.Axis axis) { return switch (axis) { case X -> quad.x(vertex); case Y -> quad.y(vertex); case Z -> quad.z(vertex); }; }
        private static int min(int[] values) { int min = Integer.MAX_VALUE; for (int value : values) min = Math.min(min, value); return min; }
        private static int max(int[] values) { int max = Integer.MIN_VALUE; for (int value : values) max = Math.max(max, value); return max; }
        private static Direction.Axis[] inPlaneAxes(Direction.Axis normal) { return switch (normal) { case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z}; case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}; case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y}; }; }
    }
}

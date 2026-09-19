package dev.resivore.bgeglassculling.client;

import dev.resivore.bgeglassculling.SurfaceOverlapResolver;
import dev.resivore.bgeglassculling.geometry.Rect16;
import dev.resivore.bgeglassculling.geometry.RectSubtraction;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Clips final renderer quads while retaining every non-geometric quad attribute. */
final class GlassQuadClipper {
    private GlassQuadClipper() {}

    static void emit(MutableQuadView sourceQuad, QuadEmitter output,
            BlockAndTintGetter level, BlockPos pos, BlockState state) {
        var inspected = AxisAlignedQuad.inspect(sourceQuad);
        if (inspected.isEmpty()) {
            output.copyFrom(sourceQuad).emit();
            return;
        }
        AxisAlignedQuad quad = inspected.get();
        BlockState neighbor = level.getBlockState(pos.relative(quad.normal()));
        List<Rect16> cullRegions = SurfaceOverlapResolver.cullRegions(state, neighbor,
                quad.normal(), quad.plane16(), quad.uAxis(), quad.vAxis(), quad.bounds());
        emitWithCullRegions(sourceQuad, output, quad, cullRegions);
    }

    static void emitWithCullRegions(MutableQuadView sourceQuad, QuadEmitter output,
            List<Rect16> cullRegions) {
        var inspected = AxisAlignedQuad.inspect(sourceQuad);
        if (inspected.isEmpty()) {
            output.copyFrom(sourceQuad).emit();
            return;
        }
        emitWithCullRegions(sourceQuad, output, inspected.get(), cullRegions);
    }

    private static void emitWithCullRegions(MutableQuadView sourceQuad, QuadEmitter output,
            AxisAlignedQuad quad, List<Rect16> cullRegions) {
        List<Rect16> visible = RectSubtraction.subtract(quad.bounds(), cullRegions);
        if (visible.size() == 1 && visible.getFirst().equals(quad.bounds())) {
            output.copyFrom(sourceQuad).emit();
            return;
        }
        for (Rect16 fragment : visible) emitFragment(sourceQuad, output, quad, fragment);
    }

    private static void emitFragment(MutableQuadView source, QuadEmitter output,
            AxisAlignedQuad geometry, Rect16 fragment) {
        output.copyFrom(source);
        for (int uCorner = 0; uCorner < 2; uCorner++) {
            for (int vCorner = 0; vCorner < 2; vCorner++) {
                int vertex = geometry.vertex(uCorner, vCorner);
                int u16 = uCorner == 0 ? fragment.uMin() : fragment.uMax();
                int v16 = vCorner == 0 ? fragment.vMin() : fragment.vMax();
                float uFraction = fraction(u16, geometry.bounds().uMin(), geometry.bounds().uMax());
                float vFraction = fraction(v16, geometry.bounds().vMin(), geometry.bounds().vMax());
                setPosition(output, vertex, geometry, u16, v16);
                output.uv(vertex,
                        interpolate(source, geometry, Attribute.U, uFraction, vFraction),
                        interpolate(source, geometry, Attribute.V, uFraction, vFraction));
                output.color(vertex, QuadInterpolation.packedBytes(
                        source.color(geometry.vertex(0, 0)),
                        source.color(geometry.vertex(1, 0)),
                        source.color(geometry.vertex(0, 1)),
                        source.color(geometry.vertex(1, 1)), uFraction, vFraction));
                output.lightmap(vertex, QuadInterpolation.packedShorts(
                        source.lightmap(geometry.vertex(0, 0)),
                        source.lightmap(geometry.vertex(1, 0)),
                        source.lightmap(geometry.vertex(0, 1)),
                        source.lightmap(geometry.vertex(1, 1)), uFraction, vFraction));
                interpolateNormal(source, output, geometry, vertex, uFraction, vFraction);
            }
        }
        output.emit();
    }

    private static void setPosition(QuadEmitter output, int vertex, AxisAlignedQuad geometry,
            int u16, int v16) {
        float normal = geometry.plane16() / 16.0F;
        float u = u16 / 16.0F;
        float v = v16 / 16.0F;
        float x = coordinate(Direction.Axis.X, geometry, normal, u, v);
        float y = coordinate(Direction.Axis.Y, geometry, normal, u, v);
        float z = coordinate(Direction.Axis.Z, geometry, normal, u, v);
        output.pos(vertex, x, y, z);
    }

    private static float coordinate(Direction.Axis axis, AxisAlignedQuad geometry,
            float normal, float u, float v) {
        if (axis == geometry.normal().getAxis()) return normal;
        if (axis == geometry.uAxis()) return u;
        if (axis == geometry.vAxis()) return v;
        throw new IllegalArgumentException("Axis is outside the quad frame");
    }

    private static float interpolate(MutableQuadView source, AxisAlignedQuad geometry,
            Attribute attribute, float u, float v) {
        return QuadInterpolation.bilinear(
                attribute.get(source, geometry.vertex(0, 0)),
                attribute.get(source, geometry.vertex(1, 0)),
                attribute.get(source, geometry.vertex(0, 1)),
                attribute.get(source, geometry.vertex(1, 1)), u, v);
    }

    private static void interpolateNormal(MutableQuadView source, QuadEmitter output,
            AxisAlignedQuad geometry, int vertex, float u, float v) {
        for (int uCorner = 0; uCorner < 2; uCorner++) {
            for (int vCorner = 0; vCorner < 2; vCorner++) {
                if (!source.hasNormal(geometry.vertex(uCorner, vCorner))) return;
            }
        }
        float x = interpolate(source, geometry, Attribute.NORMAL_X, u, v);
        float y = interpolate(source, geometry, Attribute.NORMAL_Y, u, v);
        float z = interpolate(source, geometry, Attribute.NORMAL_Z, u, v);
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length > 0.0F) output.normal(vertex, x / length, y / length, z / length);
    }

    private static float fraction(int value, int min, int max) {
        return (float) (value - min) / (float) (max - min);
    }

    private enum Attribute {
        U { @Override float get(MutableQuadView quad, int vertex) { return quad.u(vertex); } },
        V { @Override float get(MutableQuadView quad, int vertex) { return quad.v(vertex); } },
        NORMAL_X { @Override float get(MutableQuadView quad, int vertex) {
            return quad.normalX(vertex); } },
        NORMAL_Y { @Override float get(MutableQuadView quad, int vertex) {
            return quad.normalY(vertex); } },
        NORMAL_Z { @Override float get(MutableQuadView quad, int vertex) {
            return quad.normalZ(vertex); } };

        abstract float get(MutableQuadView quad, int vertex);
    }
}

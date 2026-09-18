package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import net.minecraft.core.Direction;

import java.util.Objects;

/**
 * Exact block-local projection from a captured receiver surface into Continuity's canonical
 * {@code QuadEmitter.square} and UV convention. The resulting UV rectangle is a crop of the
 * normal full-block overlay, never a rescaled texture.
 */
public final class OverlayEmissionGeometry {
    private static final float UNITS = 16.0F;

    private OverlayEmissionGeometry() {}

    public static Projection project(QuadSurface surface) {
        Objects.requireNonNull(surface, "surface");
        float plane = fraction(surface.plane16());
        float uMin = fraction(surface.uMin16());
        float uMax = fraction(surface.uMax16());
        float vMin = fraction(surface.vMin16());
        float vMax = fraction(surface.vMax16());
        return switch (surface.normal()) {
            // QuadEmitter.square's UP convention flips its Z coordinate before assigning the
            // canonical overlay UVs; these parameters retain Continuity's original orientation.
            case UP -> new Projection(surface, uMin, 1 - vMax, uMax, 1 - vMin, 1 - plane);
            case DOWN -> new Projection(surface, uMin, vMin, uMax, vMax, plane);
            case WEST -> new Projection(surface, vMin, uMin, vMax, uMax, plane);
            case EAST -> new Projection(surface, 1 - vMax, uMin, 1 - vMin, uMax, 1 - plane);
            case NORTH -> new Projection(surface, 1 - uMax, vMin, 1 - uMin, vMax, plane);
            case SOUTH -> new Projection(surface, uMin, vMin, uMax, vMax, 1 - plane);
        };
    }

    /** True only when Continuity's original unit-square call is exactly geometry-equivalent. */
    public static boolean originalUnitSquareMatches(QuadSurface surface) {
        return switch (surface.normal()) {
            case UP -> full(surface, 16);
            case DOWN -> full(surface, 0);
            case NORTH, WEST -> full(surface, 0);
            case SOUTH, EAST -> full(surface, 16);
        };
    }

    private static boolean full(QuadSurface surface, int plane16) {
        return surface.plane16() == plane16 && surface.uMin16() == 0 && surface.uMax16() == 16
                && surface.vMin16() == 0 && surface.vMax16() == 16;
    }

    private static float fraction(int sixteenths) {
        return sixteenths / UNITS;
    }

    /**
     * Parameters for {@link net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter#square}.
     * UV fractions retain the vertex order used by Continuity's QuadUtil emitter: 0/1/2/3 are
     * left-top, left-bottom, right-bottom, right-top in square parameter space.
     */
    public record Projection(QuadSurface surface, float left, float bottom, float right, float top,
            float depth) {
        public Projection {
            Objects.requireNonNull(surface, "surface");
        }

        public float uvU(int vertex) {
            return switch (vertex) {
                case 0, 1 -> left;
                case 2, 3 -> right;
                default -> throw new IllegalArgumentException("Invalid quad vertex: " + vertex);
            };
        }

        public float uvV(int vertex) {
            return switch (vertex) {
                case 0, 3 -> 1.0F - top;
                case 1, 2 -> 1.0F - bottom;
                default -> throw new IllegalArgumentException("Invalid quad vertex: " + vertex);
            };
        }

        /** Exact positions produced by Fabric's documented {@code QuadEmitter.square} helper. */
        public Position vertex(int vertex) {
            if (vertex < 0 || vertex > 3) throw new IllegalArgumentException("Invalid quad vertex: " + vertex);
            return switch (surface.normal()) {
                case UP -> new Position(vertex < 2 ? left : right, 1 - depth,
                        vertex == 0 || vertex == 3 ? 1 - top : 1 - bottom);
                case DOWN -> new Position(vertex < 2 ? left : right, depth,
                        vertex == 0 || vertex == 3 ? top : bottom);
                case WEST -> new Position(depth, vertex == 0 || vertex == 3 ? top : bottom,
                        vertex < 2 ? left : right);
                case EAST -> new Position(1 - depth, vertex == 0 || vertex == 3 ? top : bottom,
                        vertex < 2 ? 1 - left : 1 - right);
                case NORTH -> new Position(vertex < 2 ? 1 - left : 1 - right,
                        vertex == 0 || vertex == 3 ? top : bottom, depth);
                case SOUTH -> new Position(vertex < 2 ? left : right,
                        vertex == 0 || vertex == 3 ? top : bottom, 1 - depth);
            };
        }
    }

    public record Position(float x, float y, float z) {}
}

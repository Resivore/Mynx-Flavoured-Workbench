package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Thread-confined snapshot of the exact quad currently being processed by Continuity. */
public final class ContinuityQuadContext {
    private static final int VERTEX_COUNT = 4;
    private static final float SIXTEENTH_EPSILON = 0.001F;
    private static final ThreadLocal<Capture> CURRENT = new ThreadLocal<>();

    private ContinuityQuadContext() {}

    public static Scope push(MutableQuadView quad, BlockState receiverState, BlockPos receiverPos) {
        Capture previous = CURRENT.get();
        Capture next;
        try {
            next = new Capture(snapshot(quad), receiverState, receiverPos.immutable());
        } catch (IllegalArgumentException exception) {
            next = new Capture(null, receiverState, receiverPos.immutable());
        }
        CURRENT.set(next);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    @Nullable
    public static Capture current() {
        return CURRENT.get();
    }

    private static QuadSurface snapshot(MutableQuadView quad) {
        Direction face = quad.lightFace();
        if (face == null) throw new IllegalArgumentException("Quad has no light face");
        Direction.Axis normal = face.getAxis();
        Direction.Axis[] inPlane = inPlaneAxes(normal);

        float normalMin = Float.POSITIVE_INFINITY;
        float normalMax = Float.NEGATIVE_INFINITY;
        float uMin = Float.POSITIVE_INFINITY;
        float uMax = Float.NEGATIVE_INFINITY;
        float vMin = Float.POSITIVE_INFINITY;
        float vMax = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < VERTEX_COUNT; vertex++) {
            float normalCoordinate = coordinate(quad, vertex, normal);
            float uCoordinate = coordinate(quad, vertex, inPlane[0]);
            float vCoordinate = coordinate(quad, vertex, inPlane[1]);
            normalMin = Math.min(normalMin, normalCoordinate);
            normalMax = Math.max(normalMax, normalCoordinate);
            uMin = Math.min(uMin, uCoordinate);
            uMax = Math.max(uMax, uCoordinate);
            vMin = Math.min(vMin, vCoordinate);
            vMax = Math.max(vMax, vCoordinate);
        }
        if (normalMax - normalMin > SIXTEENTH_EPSILON) {
            throw new IllegalArgumentException("Quad is not planar on its light-face axis");
        }
        return new QuadSurface(face, sixteenths(normalMin),
                inPlane[0], sixteenths(uMin), sixteenths(uMax),
                inPlane[1], sixteenths(vMin), sixteenths(vMax));
    }

    private static int sixteenths(float coordinate) {
        float scaled = coordinate * 16.0F;
        int rounded = Math.round(scaled);
        if (Math.abs(scaled - rounded) > SIXTEENTH_EPSILON) {
            throw new IllegalArgumentException("Quad coordinate is not an exact sixteenth");
        }
        return rounded;
    }

    private static float coordinate(MutableQuadView quad, int vertex, Direction.Axis axis) {
        return switch (axis) {
            case X -> quad.x(vertex);
            case Y -> quad.y(vertex);
            case Z -> quad.z(vertex);
        };
    }

    private static Direction.Axis[] inPlaneAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    public record Capture(@Nullable QuadSurface surface, @Nullable BlockState receiverState,
            @Nullable BlockPos receiverPos) {
        public Capture(@Nullable QuadSurface surface) {
            this(surface, null, null);
        }

        public boolean valid() {
            return surface != null;
        }
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}

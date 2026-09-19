package dev.resivore.bgeglassculling.client;

import dev.resivore.bgeglassculling.geometry.Rect16;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.core.Direction;

import java.util.Optional;

/** Exact axis-aligned rectangular view of a final Fabric-renderer quad. */
record AxisAlignedQuad(Direction normal, int plane16, Direction.Axis uAxis,
        Direction.Axis vAxis, Rect16 bounds, int[][] cornerVertices) {
    private static final int VERTEX_COUNT = 4;

    static Optional<AxisAlignedQuad> inspect(QuadView quad) {
        Direction normal = quad.lightFace();
        if (normal == null) return Optional.empty();
        Direction.Axis[] axes = inPlaneAxes(normal.getAxis());
        int[] normalCoordinates = new int[VERTEX_COUNT];
        int[] uCoordinates = new int[VERTEX_COUNT];
        int[] vCoordinates = new int[VERTEX_COUNT];
        try {
            for (int vertex = 0; vertex < VERTEX_COUNT; vertex++) {
                normalCoordinates[vertex] = exactSixteenths(coordinate(quad, vertex, normal.getAxis()));
                uCoordinates[vertex] = exactSixteenths(coordinate(quad, vertex, axes[0]));
                vCoordinates[vertex] = exactSixteenths(coordinate(quad, vertex, axes[1]));
            }
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        int plane = normalCoordinates[0];
        for (int coordinate : normalCoordinates) if (coordinate != plane) return Optional.empty();
        int uMin = min(uCoordinates);
        int uMax = max(uCoordinates);
        int vMin = min(vCoordinates);
        int vMax = max(vCoordinates);
        if (plane < 0 || plane > 16 || uMin < 0 || uMax > 16 || vMin < 0 || vMax > 16
                || uMin >= uMax || vMin >= vMax) {
            return Optional.empty();
        }

        int[][] corners = {{-1, -1}, {-1, -1}};
        for (int vertex = 0; vertex < VERTEX_COUNT; vertex++) {
            int uCorner = corner(uCoordinates[vertex], uMin, uMax);
            int vCorner = corner(vCoordinates[vertex], vMin, vMax);
            if (uCorner < 0 || vCorner < 0 || corners[uCorner][vCorner] >= 0) {
                return Optional.empty();
            }
            corners[uCorner][vCorner] = vertex;
        }
        for (int[] row : corners) for (int vertex : row) if (vertex < 0) return Optional.empty();
        return Optional.of(new AxisAlignedQuad(normal, plane, axes[0], axes[1],
                new Rect16(uMin, uMax, vMin, vMax), corners));
    }

    int vertex(int uCorner, int vCorner) {
        return cornerVertices[uCorner][vCorner];
    }

    private static int exactSixteenths(float coordinate) {
        int units = Math.round(coordinate * 16.0F);
        if (Float.compare(coordinate, units / 16.0F) != 0) {
            throw new IllegalArgumentException("Coordinate is not an exact sixteenth");
        }
        return units;
    }

    private static float coordinate(QuadView quad, int vertex, Direction.Axis axis) {
        return switch (axis) {
            case X -> quad.x(vertex);
            case Y -> quad.y(vertex);
            case Z -> quad.z(vertex);
        };
    }

    private static int min(int[] values) {
        int result = Integer.MAX_VALUE;
        for (int value : values) result = Math.min(result, value);
        return result;
    }

    private static int max(int[] values) {
        int result = Integer.MIN_VALUE;
        for (int value : values) result = Math.max(result, value);
        return result;
    }

    private static int corner(int value, int min, int max) {
        if (value == min) return 0;
        if (value == max) return 1;
        return -1;
    }

    private static Direction.Axis[] inPlaneAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }
}

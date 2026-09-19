package dev.resivore.bgeglassculling;

import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfaceModel;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfacePatch;
import dev.resivore.bgeglassculling.geometry.Rect16;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Generic exact contact intersection over BGE C78 surface patches. */
public final class SurfaceOverlapResolver {
    private static final int BLOCK_UNITS = 16;

    private SurfaceOverlapResolver() {}

    public static boolean canEvaluateBoundary(BlockState source, BlockState neighbor,
            Direction outwardNormal) {
        if (!MaterialCompatibility.mutuallyCullCompatible(source, neighbor)) return false;
        SurfaceModel sourceModel = model(source);
        SurfaceModel neighborModel = model(neighbor);
        if (!sourceModel.supported() || !neighborModel.supported()) return false;
        int boundaryPlane = outwardNormal.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? BLOCK_UNITS : 0;
        return sourceModel.patches(outwardNormal).stream()
                .anyMatch(patch -> patch.plane16() == boundaryPlane);
    }

    /**
     * Returns source-local rectangles covered by the directly adjacent compatible state. The
     * adjacent state is one block in {@code outwardNormal}; physical planes must coincide exactly.
     */
    public static List<Rect16> cullRegions(BlockState source, BlockState neighbor,
            Direction outwardNormal, int sourcePlane16, Direction.Axis uAxis,
            Direction.Axis vAxis, Rect16 renderedBounds) {
        Objects.requireNonNull(outwardNormal, "outwardNormal");
        Objects.requireNonNull(uAxis, "uAxis");
        Objects.requireNonNull(vAxis, "vAxis");
        Objects.requireNonNull(renderedBounds, "renderedBounds");
        if (!MaterialCompatibility.mutuallyCullCompatible(source, neighbor)) return List.of();

        SurfaceModel sourceModel = model(source);
        SurfaceModel neighborModel = model(neighbor);
        if (!sourceModel.supported() || !neighborModel.supported()) return List.of();

        Direction opposing = outwardNormal.getOpposite();
        int neighborOrigin = outwardNormal.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? BLOCK_UNITS : -BLOCK_UNITS;
        List<Rect16> result = new ArrayList<>();
        for (SurfacePatch sourcePatch : sourceModel.patches(outwardNormal)) {
            if (!matchesFrame(sourcePatch, sourcePlane16, uAxis, vAxis)) continue;
            Rect16 sourceBounds = bounds(sourcePatch);
            var renderedSource = sourceBounds.intersection(renderedBounds);
            if (renderedSource.isEmpty()) continue;
            for (SurfacePatch neighborPatch : neighborModel.patches(opposing)) {
                if (neighborPatch.uAxis() != uAxis || neighborPatch.vAxis() != vAxis
                        || sourcePatch.plane16()
                                != neighborOrigin + neighborPatch.plane16()) {
                    continue;
                }
                renderedSource.get().intersection(bounds(neighborPatch)).ifPresent(result::add);
            }
        }
        return result.stream().distinct().toList();
    }

    private static SurfaceModel model(BlockState state) {
        Binding binding = MaterialCompatibility.glassBinding(state).orElseThrow(
                () -> new IllegalArgumentException("State is not a BGE-canonical glass surface"));
        return binding.surfaceModel(state);
    }

    private static boolean matchesFrame(SurfacePatch patch, int plane16,
            Direction.Axis uAxis, Direction.Axis vAxis) {
        return patch.plane16() == plane16
                && patch.uAxis() == uAxis && patch.vAxis() == vAxis;
    }

    private static Rect16 bounds(SurfacePatch patch) {
        return new Rect16(patch.uMin16(), patch.uMax16(),
                patch.vMin16(), patch.vMax16());
    }
}

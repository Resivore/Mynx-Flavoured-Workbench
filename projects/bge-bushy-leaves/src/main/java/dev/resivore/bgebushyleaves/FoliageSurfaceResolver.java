package dev.resivore.bgebushyleaves;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfaceModel;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfacePatch;
import dev.resivore.bgebushyleaves.geometry.PatchFrame;
import dev.resivore.bgebushyleaves.geometry.Rect16;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Exact same-canonical-leaf contact masking over BGE's exposed-surface contract. */
public final class FoliageSurfaceResolver {
    private FoliageSurfaceResolver() {}

    public static List<Rect16> hiddenRegions(Binding sourceBinding, BlockState source,
            BlockAndTintGetter level, BlockPos pos, PatchFrame patch) {
        int boundary = patch.normal().getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 : 0;
        if (patch.plane16() != boundary) return List.of();
        BlockState neighbor = level.getBlockState(pos.relative(patch.normal()));
        if (!BgeLeafEligibility.sameCanonicalLeaf(sourceBinding, neighbor)) return List.of();
        List<SurfacePatch> patches = neighborPatches(sourceBinding, neighbor, patch.normal().getOpposite());
        int neighborPlane = patch.normal().getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : 16;
        List<Rect16> result = new ArrayList<>();
        for (SurfacePatch other : patches) {
            if (other.plane16() != neighborPlane || other.uAxis() != patch.uAxis() || other.vAxis() != patch.vAxis()) continue;
            new Rect16(other.uMin16(), other.uMax16(), other.vMin16(), other.vMax16())
                    .intersection(patch.bounds()).ifPresent(result::add);
        }
        return result.stream().distinct().toList();
    }

    private static List<SurfacePatch> neighborPatches(Binding source, BlockState neighbor, Direction face) {
        return BgeMaterialBindings.fromBlock(neighbor.getBlock()).filter(candidate ->
                BgeLeafEligibility.isLeaf(candidate, neighbor) && candidate.canonicalMaterial() == source.canonicalMaterial())
                .map(candidate -> candidate.surfaceModel(neighbor)).filter(SurfaceModel::supported)
                .map(model -> model.patches(face)).orElseGet(() -> fullCanonicalPatch(source, neighbor, face));
    }

    private static List<SurfacePatch> fullCanonicalPatch(Binding source, BlockState state, Direction face) {
        if (!state.is(source.canonicalMaterial()) || !state.is(net.minecraft.tags.BlockTags.LEAVES)) return List.of();
        Direction.Axis[] axes = switch (face.getAxis()) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
        int plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 : 0;
        return List.of(new SurfacePatch(face, plane, axes[0], 0, 16, axes[1], 0, 16, face,
                dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation.EXACT));
    }
}

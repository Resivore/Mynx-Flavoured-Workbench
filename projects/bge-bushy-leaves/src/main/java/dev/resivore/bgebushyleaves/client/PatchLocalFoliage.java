package dev.resivore.bgebushyleaves.client;

import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfaceModel;
import dev.resivore.bgebushyleaves.geometry.PatchFoliagePlan;
import dev.resivore.bgebushyleaves.geometry.PatchFrame;
import dev.resivore.bgebushyleaves.geometry.PatchMerger;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/** Appends authored foliage cards to BGE's authoritative exposed SurfacePatch collection. */
final class PatchLocalFoliage {
    private static final float OUTWARD_EPSILON = 0.01F / 16.0F;

    private PatchLocalFoliage() {}

    static long canonicalSeed(BlockPos pos, BlockState canonical) {
        return mix(pos.asLong() ^ (((long) canonical.hashCode()) << 32));
    }

    static void emit(MeshView canonicalMesh, QuadEmitter output, Binding binding, BlockState canonical,
            BlockState physical, BlockPos pos, Predicate<@Nullable Direction> cullTest) {
        SurfaceModel model = binding.surfaceModel(physical);
        if (!model.supported()) return;
        CanonicalFoliageAppearance.sample(canonicalMesh).ifPresent(appearance -> {
            List<PatchFrame> patches = PatchMerger.mergeSurfacePatches(model.patches());
            for (PatchFrame patch : patches) {
                // Only a complete boundary face has an unambiguous, renderer-supplied full-face
                // cull answer. Partial surfaces retain their foliage rather than guessed masking.
                if (reliablyBuried(patch, cullTest)) continue;
                long patchSeed = mix(canonicalSeed(pos, canonical) ^ ((long) physical.hashCode() << 1)
                        ^ patchHash(patch));
                for (PatchFoliagePlan.Card card : PatchFoliagePlan.plan(patch, patchSeed)) {
                    CanonicalFoliageAppearance.Snapshot appearanceSample = appearance.choose(
                            mix(patchSeed ^ card.ordinal()));
                    emitCard(appearanceSample, output, patch, card, false);
                    emitCard(appearanceSample, output, patch, card, true);
                }
            }
        });
    }

    private static boolean reliablyBuried(PatchFrame patch, Predicate<@Nullable Direction> cullTest) {
        return (patch.plane16() == 0 || patch.plane16() == 16)
                && patch.bounds().uMin() == 0 && patch.bounds().uMax() == 16
                && patch.bounds().vMin() == 0 && patch.bounds().vMax() == 16
                && cullTest.test(patch.normal());
    }

    /** Emits one card from an owned snapshot; no transient source quad is available here. */
    static void emitCard(CanonicalFoliageAppearance.Snapshot source, QuadEmitter output, PatchFrame frame,
            PatchFoliagePlan.Card card, boolean reverse) {
        copyAppearance(source, output);
        PatchFoliagePlan.Vertex[] vertices = card.vertices();
        int[] winding = reverse ? new int[] {0, 3, 2, 1} : new int[] {0, 1, 2, 3};
        float[][] positions = new float[4][3];
        for (int target = 0; target < 4; target++) {
            PatchFoliagePlan.Vertex vertex = vertices[winding[target]];
            positions[target] = position(frame, vertex);
            output.pos(target, positions[target][0], positions[target][1], positions[target][2]);
            int sampled = winding[target];
            output.uv(target, source.u(sampled), source.v(sampled));
            output.color(target, source.color(sampled));
            output.lightmap(target, source.lightmap(sampled));
        }
        float[] normal = normal(positions);
        for (int vertex = 0; vertex < 4; vertex++) output.normal(vertex, normal[0], normal[1], normal[2]);
        output.nominalFace(frame.normal());
        output.cullFace(null);
        output.emit();
    }

    /** Copies the owned public appearance data into a renderer-owned output quad. */
    private static void copyAppearance(CanonicalFoliageAppearance.Snapshot source, QuadEmitter output) {
        // This BlockStateModel-to-world-chunk path always uses the public block atlas. The source
        // QuadView atlas backing is deliberately never queried.
        output.atlas(QuadAtlas.BLOCK);
        output.chunkLayer(source.chunkLayer());
        output.emissive(source.emissive());
        output.diffuseShade(source.diffuseShade());
        output.ambientOcclusion(source.ambientOcclusion());
        output.foilType(source.foilType());
        output.shadeMode(source.shadeMode());
        output.animated(source.animated());
        output.tintIndex(source.tintIndex());
        output.tag(source.tag());
    }

    private static float[] position(PatchFrame frame, PatchFoliagePlan.Vertex vertex) {
        float normal = frame.plane16() / 16.0F
                + frame.normal().getAxisDirection().getStep() * (vertex.outward16() / 16.0F + OUTWARD_EPSILON);
        float u = vertex.u16() / 16.0F, v = vertex.v16() / 16.0F;
        return new float[] {coordinate(Direction.Axis.X, frame, normal, u, v),
                coordinate(Direction.Axis.Y, frame, normal, u, v),
                coordinate(Direction.Axis.Z, frame, normal, u, v)};
    }

    private static float coordinate(Direction.Axis axis, PatchFrame frame, float normal, float u, float v) {
        if (axis == frame.normal().getAxis()) return normal;
        if (axis == frame.uAxis()) return u;
        if (axis == frame.vAxis()) return v;
        throw new IllegalArgumentException("Axis is not in a BGE surface frame");
    }

    private static float[] normal(float[][] p) {
        float ax = p[1][0] - p[0][0], ay = p[1][1] - p[0][1], az = p[1][2] - p[0][2];
        float bx = p[3][0] - p[0][0], by = p[3][1] - p[0][1], bz = p[3][2] - p[0][2];
        float x = ay * bz - az * by, y = az * bx - ax * bz, z = ax * by - ay * bx;
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        return length == 0.0F ? new float[] {0.0F, 1.0F, 0.0F}
                : new float[] {x / length, y / length, z / length};
    }

    private static long patchHash(PatchFrame patch) {
        long value = patch.normal().ordinal();
        value = value * 31 + patch.plane16();
        value = value * 31 + patch.uAxis().ordinal();
        value = value * 31 + patch.vAxis().ordinal();
        value = value * 31 + patch.bounds().uMin();
        value = value * 31 + patch.bounds().uMax();
        value = value * 31 + patch.bounds().vMin();
        return value * 31 + patch.bounds().vMax();
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return value;
    }
}

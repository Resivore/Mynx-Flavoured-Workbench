package dev.resivore.bgebushyleaves.client;

import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfaceModel;
import dev.resivore.bgebushyleaves.geometry.BlockSpaceFoliagePlan;
import dev.resivore.bgebushyleaves.geometry.BlockSpaceOccupancy;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Appends world-axis foliage planes composed from BGE's complete occupied block-space volume. */
final class BlockSpaceFoliage {
    private BlockSpaceFoliage() {}

    static long canonicalSeed(BlockPos pos, BlockState canonical) {
        return mix(pos.asLong() ^ (((long) canonical.hashCode()) << 32));
    }

    static void emit(MeshView canonicalMesh, QuadEmitter output, Binding binding, BlockState canonical,
            BlockState physical, BlockPos pos) {
        SurfaceModel model = binding.surfaceModel(physical);
        if (!model.supported()) return;
        BlockSpaceOccupancy.fromSurfacePatches(model.patches()).ifPresent(occupancy ->
                CanonicalFoliageAppearance.sample(canonicalMesh).ifPresent(appearance -> {
                    long seed = mix(canonicalSeed(pos, canonical) ^ ((long) physical.hashCode() << 1));
                    for (BlockSpaceFoliagePlan.Card card : BlockSpaceFoliagePlan.plan(occupancy, seed)) {
                        CanonicalFoliageAppearance.Snapshot sample = appearance.choose(mix(seed ^ card.ordinal()));
                        emitCard(sample, output, card, false);
                        emitCard(sample, output, card, true);
                    }
                }));
    }

    /** Emits one card from an owned snapshot; no transient source quad is available here. */
    static void emitCard(CanonicalFoliageAppearance.Snapshot source, QuadEmitter output,
            BlockSpaceFoliagePlan.Card card, boolean reverse) {
        copyAppearance(source, output);
        BlockSpaceFoliagePlan.Vertex[] vertices = card.vertices();
        int[] winding = reverse ? new int[] {0, 3, 2, 1} : new int[] {0, 1, 2, 3};
        float[][] positions = new float[4][3];
        for (int target = 0; target < 4; target++) {
            BlockSpaceFoliagePlan.Vertex vertex = vertices[winding[target]];
            positions[target] = new float[] {vertex.x16() / 16.0F, vertex.y16() / 16.0F, vertex.z16() / 16.0F};
            output.pos(target, positions[target][0], positions[target][1], positions[target][2]);
            int sampled = winding[target];
            output.uv(target, source.u(sampled), source.v(sampled));
            output.color(target, source.color(sampled));
            output.lightmap(target, source.lightmap(sampled));
        }
        float[] normal = normal(positions);
        for (int vertex = 0; vertex < 4; vertex++) output.normal(vertex, normal[0], normal[1], normal[2]);
        output.nominalFace(nominalFace(normal));
        output.cullFace(null);
        output.emit();
    }

    /** Copies owned public appearance data, including canonical tint metadata, to a world quad. */
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

    private static Direction nominalFace(float[] normal) {
        Direction result = Direction.UP;
        float best = Float.NEGATIVE_INFINITY;
        for (Direction direction : Direction.values()) {
            float dot = normal[0] * direction.getStepX() + normal[1] * direction.getStepY()
                    + normal[2] * direction.getStepZ();
            if (dot > best) { best = dot; result = direction; }
        }
        return result;
    }

    private static float[] normal(float[][] p) {
        float ax = p[1][0] - p[0][0], ay = p[1][1] - p[0][1], az = p[1][2] - p[0][2];
        float bx = p[3][0] - p[0][0], by = p[3][1] - p[0][1], bz = p[3][2] - p[0][2];
        float x = ay * bz - az * by, y = az * bx - ax * bz, z = ax * by - ay * bx;
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        return length == 0.0F ? new float[] {0.0F, 1.0F, 0.0F}
                : new float[] {x / length, y / length, z / length};
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return value;
    }
}

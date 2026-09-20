package dev.resivore.bgebushyleaves.client;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Value-only appearance samples captured from the final canonical baked mesh. Renderer-owned
 * {@link QuadView} instances are callback-lifetime views and are never retained here.
 */
final class CanonicalFoliageAppearance {
    private final List<Snapshot> samples;
    private final int fallbackTintIndex;

    private CanonicalFoliageAppearance(List<Snapshot> samples, int fallbackTintIndex) {
        this.samples = List.copyOf(samples);
        this.fallbackTintIndex = fallbackTintIndex;
    }

    static Optional<CanonicalFoliageAppearance> sample(MeshView mesh) {
        List<AppearanceSelection.Candidate<Snapshot>> candidates = new ArrayList<>();
        mesh.forEach(quad -> {
            // Both preference and every value used later must be read while this callback owns the
            // renderer's transient view. No emitted card can call back into this QuadView.
            boolean decorative = quad.cullFace() == null;
            candidates.add(new AppearanceSelection.Candidate<>(Snapshot.capture(quad), decorative));
        });
        List<Snapshot> selected = AppearanceSelection.preferDecorative(candidates);
        // Some bushy providers put their texture-bearing non-cull cards beside a standard tinted
        // leaf shell. The decorative card itself can legitimately be untinted; inherit only the
        // sampled canonical tint metadata in that case. If no sampled quad is tinted, -1 remains
        // authoritative for plain/untinted leaves.
        int fallbackTint = candidates.stream().map(AppearanceSelection.Candidate::value)
                .mapToInt(Snapshot::tintIndex).filter(index -> index >= 0).findFirst().orElse(-1);
        return selected.isEmpty() ? Optional.empty() : Optional.of(new CanonicalFoliageAppearance(selected, fallbackTint));
    }

    Snapshot choose(long stableSeed) {
        Snapshot selected = samples.get(Math.floorMod((int) (stableSeed ^ (stableSeed >>> 32)), samples.size()));
        return selected.tintIndex() >= 0 || fallbackTintIndex < 0 ? selected : selected.withTintIndex(fallbackTintIndex);
    }

    /** Immutable copied FRAPI data required by the authored world-block foliage cards. */
    static final class Snapshot {
        private final float[] u;
        private final float[] v;
        private final int[] color;
        private final int[] lightmap;
        private final ChunkSectionLayer chunkLayer;
        private final boolean emissive;
        private final boolean diffuseShade;
        private final TriState ambientOcclusion;
        private final FoilType foilType;
        private final ShadeMode shadeMode;
        private final boolean animated;
        private final int tintIndex;
        private final int tag;

        private Snapshot(float[] u, float[] v, int[] color, int[] lightmap,
                ChunkSectionLayer chunkLayer, boolean emissive, boolean diffuseShade,
                TriState ambientOcclusion, FoilType foilType, ShadeMode shadeMode, boolean animated,
                int tintIndex, int tag) {
            this.u = Arrays.copyOf(u, 4);
            this.v = Arrays.copyOf(v, 4);
            this.color = Arrays.copyOf(color, 4);
            this.lightmap = Arrays.copyOf(lightmap, 4);
            this.chunkLayer = chunkLayer;
            this.emissive = emissive;
            this.diffuseShade = diffuseShade;
            this.ambientOcclusion = ambientOcclusion;
            this.foilType = foilType;
            this.shadeMode = shadeMode;
            this.animated = animated;
            this.tintIndex = tintIndex;
            this.tag = tag;
        }

        static Snapshot capture(QuadView source) {
            float[] u = new float[4];
            float[] v = new float[4];
            int[] color = new int[4];
            int[] lightmap = new int[4];
            for (int vertex = 0; vertex < 4; vertex++) {
                u[vertex] = source.u(vertex);
                v[vertex] = source.v(vertex);
                color[vertex] = source.color(vertex);
                lightmap[vertex] = source.lightmap(vertex);
            }
            return new Snapshot(u, v, color, lightmap, source.chunkLayer(), source.emissive(),
                    source.diffuseShade(), source.ambientOcclusion(), source.foilType(),
                    source.shadeMode(), source.animated(), source.tintIndex(), source.tag());
        }

        float u(int vertex) { return u[vertex]; }
        float v(int vertex) { return v[vertex]; }
        int color(int vertex) { return color[vertex]; }
        int lightmap(int vertex) { return lightmap[vertex]; }
        ChunkSectionLayer chunkLayer() { return chunkLayer; }
        boolean emissive() { return emissive; }
        boolean diffuseShade() { return diffuseShade; }
        TriState ambientOcclusion() { return ambientOcclusion; }
        FoilType foilType() { return foilType; }
        ShadeMode shadeMode() { return shadeMode; }
        boolean animated() { return animated; }
        int tintIndex() { return tintIndex; }
        int tag() { return tag; }
        Snapshot withTintIndex(int replacement) {
            return new Snapshot(u, v, color, lightmap, chunkLayer, emissive, diffuseShade,
                    ambientOcclusion, foilType, shadeMode, animated, replacement, tag);
        }
    }
}

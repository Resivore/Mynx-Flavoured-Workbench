package dev.resivore.bgebushyleaves.client;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Live appearance samples from the final canonical baked mesh. A selected quad is copied verbatim
 * before only its authored-card positions and normals are changed, retaining atlas, tint, layer,
 * emissive, UV, and material policy without any provider resource-path convention.
 */
final class CanonicalFoliageAppearance {
    private final List<QuadView> samples;

    private CanonicalFoliageAppearance(List<QuadView> samples) { this.samples = List.copyOf(samples); }

    static Optional<CanonicalFoliageAppearance> sample(MeshView mesh) {
        List<AppearanceSelection.Candidate<QuadView>> candidates = new ArrayList<>();
        mesh.forEach(quad -> candidates.add(new AppearanceSelection.Candidate<>(quad,
                quad.cullFace() == null)));
        List<QuadView> selected = AppearanceSelection.preferDecorative(candidates);
        return selected.isEmpty() ? Optional.empty() : Optional.of(new CanonicalFoliageAppearance(selected));
    }

    QuadView choose(long stableSeed) {
        return samples.get(Math.floorMod((int) (stableSeed ^ (stableSeed >>> 32)), samples.size()));
    }
}

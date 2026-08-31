package dev.aero.cnmterraincompat.client;

import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Objects;

/** One ordered client-resource pass for every BGE-owned generated geometry. */
public final class BgeGeneratedResources {
    private BgeGeneratedResources() {}

    public static GenerationSummary generate(ResourceManager manager) {
        Objects.requireNonNull(manager, "manager");
        LayerGeneratedResources.GenerationSummary layer =
                LayerGeneratedResources.generate(manager, false);
        QuarterGeometryGeneratedResources.GenerationSummary quarter =
                QuarterGeometryGeneratedResources.generate(manager);
        return new GenerationSummary(layer, quarter);
    }

    public record GenerationSummary(LayerGeneratedResources.GenerationSummary layer,
            QuarterGeometryGeneratedResources.GenerationSummary quarter) {}
}

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
        ExternalMaterialGeneratedResources.GenerationSummary external =
                ExternalMaterialGeneratedResources.generate(manager);
        PrivateBeamGeneratedResources.generate(manager);
        // Run last so C93's persistent preview wrappers are actively unwrapped after every normal
        // provider/BGE item definition exists. The pass changes only item definitions plus the two
        // inventory-only Beam models; placed blockstates and their model selections stay untouched.
        CatalogItemGeneratedResources.generate(manager);
        return new GenerationSummary(layer, quarter, external);
    }

    public record GenerationSummary(LayerGeneratedResources.GenerationSummary layer,
            QuarterGeometryGeneratedResources.GenerationSummary quarter,
            ExternalMaterialGeneratedResources.GenerationSummary external) {}
}

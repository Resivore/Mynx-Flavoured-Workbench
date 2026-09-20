package dev.resivore.bgebushyleaves.client;

import dev.resivore.bgebushyleaves.BgeLeafEligibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.resources.Identifier;

/** Installs the appearance capture and the world-only BGE foliage wrapper in final bake order. */
public final class BgeBushyLeavesClient implements ClientModInitializer {
    private static final Identifier CANONICAL_APPEARANCE_CAPTURE_PHASE = Identifier.fromNamespaceAndPath(
            "bge_bushy_leaves", "canonical_leaf_appearance_capture");
    private static final Identifier PATCH_LOCAL_FOLIAGE_PHASE = Identifier.fromNamespaceAndPath(
            "bge_bushy_leaves", "patch_local_foliage");

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(context -> {
            CanonicalLeafModels.clearForReload();
            var event = context.modifyBlockModelAfterBake();
            // The capture deliberately sees every public WRAP_LAST modifier before we install our
            // own derived-model wrapper. This keeps resource-pack/model-modifier appearance live.
            event.addPhaseOrdering(ModelModifier.WRAP_LAST_PHASE, CANONICAL_APPEARANCE_CAPTURE_PHASE);
            event.addPhaseOrdering(CANONICAL_APPEARANCE_CAPTURE_PHASE, PATCH_LOCAL_FOLIAGE_PHASE);
            event.register(CANONICAL_APPEARANCE_CAPTURE_PHASE, (model, bakeContext) -> {
                if (bakeContext.state() != null && BgeLeafEligibility.binding(bakeContext.state())
                        .filter(binding -> binding.topology() == dev.aero.cnmterraincompat.BgeMaterialBindings.Topology.CANONICAL_ROOT)
                        .isPresent()) CanonicalLeafModels.capture(bakeContext.state(), model);
                return model;
            });
            event.register(PATCH_LOCAL_FOLIAGE_PHASE, (model, bakeContext) -> bakeContext.state() != null
                    && BgeLeafEligibility.binding(bakeContext.state()).filter(binding ->
                            binding.topology() != dev.aero.cnmterraincompat.BgeMaterialBindings.Topology.CANONICAL_ROOT).isPresent()
                    && !(model instanceof BushyLeafBlockStateModel)
                    ? new BushyLeafBlockStateModel(model) : model);
        });
    }
}

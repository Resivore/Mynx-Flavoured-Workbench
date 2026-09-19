package dev.resivore.bgebushyleaves.client;

import dev.resivore.bgebushyleaves.BgeLeafEligibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.resources.Identifier;

/** Installs a final client-only wrapper after normal resource-pack and model transformations. */
public final class BgeBushyLeavesClient implements ClientModInitializer {
    private static final Identifier FINAL_PROJECTION_PHASE = Identifier.fromNamespaceAndPath(
            "bge_bushy_leaves", "canonical_foliage_projection");

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(context -> {
            CanonicalLeafModels.clearForReload();
            var event = context.modifyBlockModelAfterBake();
            event.register(ModelModifier.DEFAULT_PHASE, (model, bakeContext) -> {
                if (bakeContext.state() != null && BgeLeafEligibility.binding(bakeContext.state())
                        .filter(binding -> binding.topology() == dev.aero.cnmterraincompat.BgeMaterialBindings.Topology.CANONICAL_ROOT)
                        .isPresent()) CanonicalLeafModels.capture(bakeContext.state(), model);
                return model;
            });
            event.addPhaseOrdering(ModelModifier.WRAP_LAST_PHASE, FINAL_PROJECTION_PHASE);
            event.register(FINAL_PROJECTION_PHASE, (model, bakeContext) -> bakeContext.state() != null
                    && BgeLeafEligibility.binding(bakeContext.state()).filter(binding ->
                            binding.topology() != dev.aero.cnmterraincompat.BgeMaterialBindings.Topology.CANONICAL_ROOT).isPresent()
                    && !(model instanceof BushyLeafBlockStateModel)
                    ? new BushyLeafBlockStateModel(model) : model);
        });
    }
}

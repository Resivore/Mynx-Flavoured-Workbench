package dev.resivore.slabdecorations.client;

import dev.resivore.slabdecorations.PlantFamilyEligibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

public final class SlabDecorationsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(pluginContext ->
                pluginContext.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, bakeContext) ->
                        PlantFamilyEligibility.isEligible(bakeContext.state())
                                ? new SurfaceOffsetModel(model)
                                : model));
    }
}

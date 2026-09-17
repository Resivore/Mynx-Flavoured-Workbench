package dev.resivore.slabdecorations.client;

import dev.resivore.slabdecorations.PlantFamilyEligibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

public final class SlabDecorationsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(pluginContext ->
                // Cave-vine berries select their own lit blockstate model.  Register last so any
                // later wrapper that specializes that luminous variant is itself wrapped and both
                // variants retain the identical world-relative surface translation.
                pluginContext.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, bakeContext) ->
                        PlantFamilyEligibility.isEligible(bakeContext.state())
                                ? new SurfaceOffsetModel(model)
                                : model));
    }
}

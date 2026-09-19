package dev.resivore.dragonbound.client;

import dev.resivore.dragonbound.DragonboundContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

/** Client-only dynamic material rendering registration. */
public final class DragonboundWaystoneClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(context -> {
            // Model plugins run for every model reload; do not retain atlas sprites from the
            // previous resource-pack generation.
            MaterializedWaystoneModels.clearCache();
            context.modifyBlockModelAfterBake().register((model, bakeContext) ->
                    bakeContext.state().is(DragonboundContent.WAYSTONE)
                            ? new MaterializedWaystoneBlockStateModel(model)
                            : model);
            context.modifyItemModelAfterBake().register((model, bakeContext) ->
                    bakeContext.itemId().equals(DragonboundContent.WAYSTONE_ID)
                            ? new MaterializedWaystoneItemModel(model)
                            : model);
        });
    }
}

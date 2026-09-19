package dev.resivore.bgeglassculling.client;

import dev.resivore.bgeglassculling.MaterialCompatibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.Identifier;

/** Installs one late model wrapper after default-phase texture/model systems such as Continuity. */
public final class BgeGlassFaceCullingClient implements ClientModInitializer {
    private static final Identifier FINAL_CULLING_PHASE = Identifier.fromNamespaceAndPath(
            "bge_glass_face_culling", "final_culling");

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(context -> {
            var event = context.modifyBlockModelAfterBake();
            event.addPhaseOrdering(Event.DEFAULT_PHASE, FINAL_CULLING_PHASE);
            event.register(FINAL_CULLING_PHASE, (model, modifierContext) ->
                    MaterialCompatibility.isGlassState(modifierContext.state())
                            && !(model instanceof GlassCullingBlockStateModel)
                                    ? new GlassCullingBlockStateModel(model) : model);
        });
    }
}

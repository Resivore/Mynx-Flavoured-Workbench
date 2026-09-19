package dev.resivore.bgeglassculling.client;

import dev.resivore.bgeglassculling.MaterialCompatibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.resources.Identifier;

/** Installs one outermost model wrapper after texture/model and late geometry wrappers. */
public final class BgeGlassFaceCullingClient implements ClientModInitializer {
    private static final Identifier FINAL_CULLING_PHASE = Identifier.fromNamespaceAndPath(
            "bge_glass_face_culling", "final_culling");

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(context -> {
            var event = context.modifyBlockModelAfterBake();
            // WRAP_LAST is the last Fabric built-in model-wrapper phase.  Ordering C2 after it
            // makes this wrapper receive geometry already emitted by wrappers such as Slab
            // Decorations rather than clipping their pre-transform input.
            event.addPhaseOrdering(ModelModifier.WRAP_LAST_PHASE, FINAL_CULLING_PHASE);
            event.register(FINAL_CULLING_PHASE, (model, modifierContext) ->
                    MaterialCompatibility.isGlassState(modifierContext.state())
                            && !(model instanceof GlassCullingBlockStateModel)
                                    ? new GlassCullingBlockStateModel(model) : model);
        });
    }
}

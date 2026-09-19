package dev.resivore.dragonbound.client;

import dev.resivore.dragonbound.mixin.client.ItemStackRenderStateAccessor;
import dev.resivore.dragonbound.mixin.client.LayerRenderStateAccessor;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBakedItemModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Item renderer counterpart of the placed-model wrapper.
 *
 * <p>The vanilla model first constructs its complete render state. Only then are its immutable
 * baked quads retextured, retaining its display transforms, extents, foil/tint state, and every
 * other layer property.</p>
 */
final class MaterializedWaystoneItemModel extends WrapperBakedItemModel {
    MaterializedWaystoneItemModel(ItemModel wrapped) {
        super(wrapped);
    }

    @Override
    public void update(
            ItemStackRenderState state,
            ItemStack stack,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            ClientLevel level,
            ItemOwner owner,
            int seed) {
        Optional<MaterializedWaystoneModels.DirectionalMaterial> material = MaterializedWaystoneModels.resolve(stack);
        ItemStackRenderStateAccessor renderState = (ItemStackRenderStateAccessor) state;
        int firstLayer = renderState.dragonboundWaystone$activeLayerCount();
        wrapped.update(state, stack, resolver, displayContext, level, owner, seed);
        if (material.isEmpty()) {
            return;
        }

        ItemStackRenderState.LayerRenderState[] layers = renderState.dragonboundWaystone$layers();
        for (int layerIndex = firstLayer;
             layerIndex < renderState.dragonboundWaystone$activeLayerCount();
             layerIndex++) {
            LayerRenderStateAccessor layer = (LayerRenderStateAccessor) layers[layerIndex];
            layer.dragonboundWaystone$quads().replaceAll(quad ->
                    MaterializedWaystoneModels.retarget(quad, material.get()));
        }
        state.appendModelIdentityElement(material.get().blockId());
    }
}

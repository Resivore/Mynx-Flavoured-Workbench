package dev.resivore.dragonbound.client;

import dev.resivore.dragonbound.mixin.client.CuboidItemModelWrapperAccessor;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBakedItemModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Item renderer counterpart of the placed-model wrapper. */
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
        if (material.isEmpty() || !(wrapped instanceof CuboidItemModelWrapper)) {
            wrapped.update(state, stack, resolver, displayContext, level, owner, seed);
            return;
        }

        CuboidItemModelWrapperAccessor source = (CuboidItemModelWrapperAccessor) wrapped;
        state.clear();
        ItemStackRenderState.LayerRenderState layer = state.newLayer();
        List<BakedQuad> output = layer.prepareQuadList();
        for (BakedQuad quad : source.dragonboundWaystone$quads().getAll()) {
            output.add(MaterializedWaystoneModels.retarget(quad, material.get()));
        }
        source.dragonboundWaystone$properties().applyToLayer(layer, displayContext);
        state.appendModelIdentityElement(material.get().blockId());
    }
}

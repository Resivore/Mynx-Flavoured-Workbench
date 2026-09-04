package com.yungnickyoung.minecraft.ribbits.client.render;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.world.loot.RibbitVillageExplorerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBakedItemModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Chain-preserving filled-map wrapper. It claims only the exact Ribbits success marker and
 * delegates every other stack, so independently installed wrappers such as MME compose in
 * either normal registration order.
 */
public final class RibbitVillageMapItemModel extends WrapperBakedItemModel {
    private static final Identifier FILLED_MAP = Identifier.parse("minecraft:filled_map");
    private static final Identifier RIBBIT_VILLAGE_MODEL =
            RibbitsCommon.id("ribbit_village_explorer_map");

    private ItemModel resolvedModel;
    private boolean unavailable;

    private RibbitVillageMapItemModel(ItemModel wrapped) {
        super(wrapped);
    }

    public static void register() {
        ModelLoadingPlugin.register(context ->
                context.modifyItemModelAfterBake().register((model, modifierContext) ->
                        FILLED_MAP.equals(modifierContext.itemId())
                                ? new RibbitVillageMapItemModel(model)
                                : model));
    }

    @Override
    public void update(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            ClientLevel level,
            ItemOwner owner,
            int seed) {
        if (!RibbitVillageExplorerMap.isSuccessfulMap(stack)) {
            super.update(renderState, stack, resolver, displayContext, level, owner, seed);
            return;
        }

        ItemModel model = resolve();
        if (model == null) {
            super.update(renderState, stack, resolver, displayContext, level, owner, seed);
            return;
        }
        model.update(renderState, stack, resolver, displayContext, level, owner, seed);
    }

    private ItemModel resolve() {
        if (resolvedModel != null || unavailable) {
            return resolvedModel;
        }

        ItemModel model = Minecraft.getInstance().getModelManager()
                .getItemModel(RIBBIT_VILLAGE_MODEL);
        if (model instanceof MissingItemModel) {
            unavailable = true;
            return null;
        }
        resolvedModel = model;
        return model;
    }
}

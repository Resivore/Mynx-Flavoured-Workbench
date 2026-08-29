package com.crispytwig.naturalist.client.model.item;

import com.crispytwig.naturalist.server.item.NaturalistBucketItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Preserves Naturalist's registry-driven bucket/caught-mob model selection on the 26.2 item model pipeline.
 */
@Environment(EnvType.CLIENT)
public final class VariantAwareItemModel implements ItemModel {
    private final ItemModel parent;
    private final NaturalistBucketItem item;
    private final Function<Identifier, @Nullable ItemModel> modelGetter;

    public VariantAwareItemModel(ItemModel parent, NaturalistBucketItem item,
                                 Function<Identifier, @Nullable ItemModel> modelGetter) {
        this.parent = parent;
        this.item = item;
        this.modelGetter = modelGetter;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
        renderState.appendModelIdentityElement(this);
        ItemModel selected = VariantItemModels.resolveModelId(this.item, stack, level)
                .map(this.modelGetter)
                .orElse(null);
        (selected != null ? selected : this.parent)
                .update(renderState, stack, resolver, displayContext, level, owner, seed);
    }
}

package com.crispytwig.naturalist.client.model.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/** Evaluates Naturalist's preserved numeric override tables on the 26.2 ItemModel API. */
@Environment(EnvType.CLIENT)
public final class PropertyAwareItemModel implements ItemModel {
    private final ItemModel parent;
    private final Item item;
    private final ItemModelOverrides overrides;
    private final Function<Identifier, @Nullable ItemModel> modelGetter;

    public PropertyAwareItemModel(ItemModel parent, Item item, ItemModelOverrides overrides,
                                  Function<Identifier, @Nullable ItemModel> modelGetter) {
        this.parent = parent;
        this.item = item;
        this.overrides = overrides;
        this.modelGetter = modelGetter;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
        renderState.appendModelIdentityElement(this);
        ItemModel selected = this.overrides.resolve(id -> NaturalistItemModelProperties.get(
                        this.item, id, stack, level, owner, seed))
                .map(this.modelGetter)
                .orElse(null);
        (selected != null ? selected : this.parent)
                .update(renderState, stack, resolver, displayContext, level, owner, seed);
    }
}

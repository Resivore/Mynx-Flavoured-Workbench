package com.crispytwig.naturalist.fabric.client;

import com.crispytwig.naturalist.NaturalistClient;
import com.crispytwig.naturalist.NaturalistClientConfig;
import com.crispytwig.naturalist.fabric.config.FabricNaturalistClientConfig;
import com.crispytwig.naturalist.client.model.item.ItemModelOverrides;
import com.crispytwig.naturalist.client.model.item.LegacyItemModelResources;
import com.crispytwig.naturalist.client.model.item.NaturalistItemModelProperties;
import com.crispytwig.naturalist.client.model.item.PropertyAwareItemModel;
import com.crispytwig.naturalist.client.model.item.VariantAwareItemModel;
import com.crispytwig.naturalist.client.model.item.VariantItemModels;
import com.crispytwig.naturalist.client.particle.CaptureNetSwingParticle;
import com.crispytwig.naturalist.registry.NaturalistParticleTypes;
import com.crispytwig.naturalist.server.item.NaturalistBucketItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class NaturalistFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricNaturalistClientConfig.load();
        NaturalistClientConfig.setGlowGoopTooltip(FabricNaturalistClientConfig::isGlowGoopTooltipEnabled);

        NaturalistClient.registerLayerDefinitions((location, definition) ->
                ModelLayerRegistry.registerModelLayer(location, definition::get));
        NaturalistClient.registerRenderers(EntityRendererRegistry::register);

        NaturalistClient.registerItemProperties();
        NaturalistClient.registerMenuScreens(MenuScreens::register);

        registerVariantItemModels();

        ParticleProviderRegistry.getInstance().register(NaturalistParticleTypes.CAPTURE_NET_SWING.get(), CaptureNetSwingParticle.Provider::new);
    }

    private static void registerVariantItemModels() {
        Map<Identifier, NaturalistBucketItem> variantItems = VariantItemModels.collectVariantItems();
        Map<Identifier, Item> propertyItems = new HashMap<>();
        for (Item item : NaturalistItemModelProperties.snapshot().keySet()) {
            propertyItems.put(BuiltInRegistries.ITEM.getKey(item), item);
        }

        PreparableModelLoadingPlugin.register(
                (sharedState, executor) -> CompletableFuture.supplyAsync(() -> new PreparedItemModels(
                        VariantItemModels.scanExtraModels(sharedState.resourceManager()),
                        LegacyItemModelResources.scanOverrides(sharedState.resourceManager(), propertyItems.keySet())), executor),
                (prepared, context) -> {
                    for (Identifier modelId : prepared.allModelIds()) {
                        ExtraModelKey<ResolvedModel> key = ExtraModelKey.create(
                                () -> "Naturalist legacy item model " + modelId);
                        context.addModel(key, new SimpleUnbakedExtraModel<>(
                                modelId, (resolvedModel, baker) -> resolvedModel));
                    }

                    context.modifyItemModelAfterBake().register((model, ctx) -> {
                        Identifier itemId = ctx.itemId();
                        NaturalistBucketItem variantItem = variantItems.get(itemId);
                        Item propertyItem = propertyItems.get(itemId);
                        if (variantItem == null && propertyItem == null) {
                            return model;
                        }

                        Set<Identifier> requiredModels = new LinkedHashSet<>();
                        if (variantItem != null) {
                            requiredModels.addAll(prepared.variantModels());
                        }
                        ItemModelOverrides overrides = prepared.propertyOverrides()
                                .getOrDefault(itemId, ItemModelOverrides.EMPTY);
                        if (propertyItem != null) {
                            overrides.entries().forEach(entry -> requiredModels.add(entry.model()));
                        }
                        Map<Identifier, ItemModel> bakedModels = bakeModels(requiredModels, ctx);
                        ItemModel missingModel = ctx.bakingContext().missingItemModel(ctx.transformation());

                        ItemModel wrapped = model;
                        if (propertyItem != null && !overrides.entries().isEmpty()) {
                            wrapped = new PropertyAwareItemModel(wrapped, propertyItem, overrides,
                                    modelId -> bakedModels.getOrDefault(modelId, missingModel));
                        }
                        if (variantItem != null) {
                            wrapped = new VariantAwareItemModel(wrapped, variantItem,
                                    modelId -> bakedModels.getOrDefault(modelId, missingModel));
                        }
                        return wrapped;
                    });
                });
    }

    private static Map<Identifier, ItemModel> bakeModels(
            Collection<Identifier> modelIds,
            net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBakeItem.Context context) {
        Map<Identifier, ItemModel> result = new LinkedHashMap<>();
        for (Identifier modelId : modelIds) {
            ItemModel model = new CuboidItemModelWrapper.Unbaked(modelId, Optional.empty(), List.of())
                    .bake(context.bakingContext(), context.transformation());
            result.put(modelId, model);
        }
        return Map.copyOf(result);
    }

    private record PreparedItemModels(
            List<Identifier> variantModels,
            Map<Identifier, ItemModelOverrides> propertyOverrides) {
        private Set<Identifier> allModelIds() {
            Set<Identifier> result = new LinkedHashSet<>(this.variantModels);
            this.propertyOverrides.values().forEach(overrides ->
                    overrides.entries().forEach(entry -> result.add(entry.model())));
            return result;
        }
    }
}

package com.crispytwig.naturalist.fabric;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.NaturalistConfig;
import com.crispytwig.naturalist.fabric.config.FabricNaturalistConfig;
import com.crispytwig.naturalist.port.loot.NaturalistDropPolicy;
import com.crispytwig.naturalist.registry.NaturalistTags;
import com.crispytwig.naturalist.server.level.NaturalistSpawns;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FabricPotionBrewingBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.List;

public class NaturalistFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricNaturalistConfig.load();
        Naturalist.bootstrap();
        NaturalistDropPolicy.initializeUpstreamBaseline();

        Naturalist.createAttributes(FabricDefaultAttributeRegistry::register);

        Naturalist.registerSpawnPlacements(SpawnPlacements::register);

        Naturalist.registerDispenserBehaviors();

        FabricPotionBrewingBuilder.BUILD.register(builder ->
                Naturalist.registerPotionMixes((input, ingredient, output) ->
                        builder.registerPotionRecipe(input, Ingredient.of(ingredient), output)));

        registerBiomeSpawns();
        registerBiomeFeatures();
    }

    private static void registerBiomeSpawns() {
        NaturalistSpawns.forEachSpawn((hasTag, blacklistTag, category, type, weight, min, max) -> {
            BiomeModifications.addSpawn(ctx -> !NaturalistConfig.isRemoved(type)
                    && ctx.hasTag(hasTag) && (blacklistTag == null || !ctx.hasTag(blacklistTag)), category, type, weight, min, max);
        });
    }

    private static void registerBiomeFeatures() {
        for (String name : List.of("ant_hill_small", "ant_hill_big")) {
            BiomeModifications.addFeature(ctx -> ctx.hasTag(NaturalistTags.Biomes.HAS_ANT_HILL), GenerationStep.Decoration.SURFACE_STRUCTURES,
                    ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Naturalist.MOD_ID, name)));
        }
    }
}

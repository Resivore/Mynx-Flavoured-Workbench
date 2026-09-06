package com.crispytwig.naturalist.fabric;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.NaturalistConfig;
import com.crispytwig.naturalist.fabric.config.FabricNaturalistConfig;
import com.crispytwig.naturalist.port.loot.NaturalistDropPolicy;
import com.crispytwig.naturalist.server.level.NaturalistSpawns;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.SpawnPlacements;

public class NaturalistFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricNaturalistConfig.load();
        Naturalist.bootstrap();
        NaturalistDropPolicy.initializeUpstreamBaseline();

        Naturalist.createAttributes(FabricDefaultAttributeRegistry::register);

        Naturalist.registerSpawnPlacements(SpawnPlacements::register);

        Naturalist.registerDispenserBehaviors();

        registerBiomeSpawns();
    }

    private static void registerBiomeSpawns() {
        NaturalistSpawns.forEachSpawn((hasTag, blacklistTag, category, type, weight, min, max) -> {
            BiomeModifications.addSpawn(ctx -> !NaturalistConfig.isRemoved(type)
                    && ctx.hasTag(hasTag) && (blacklistTag == null || !ctx.hasTag(blacklistTag)), category, type, weight, min, max);
        });
    }

}

package dev.aero.cnmterraincompat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSources;

import java.util.List;

public final class CnmTerrainCompatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockColorRegistry.register(
                List.of(BlockTintSources.grass()),
                CnmTerrainCompat.GRASS_VERTICAL_SLAB,
                CnmTerrainCompat.GRASS_SLAB);
        NibaruProviderAdapter.configureTintRegistrar((tint, block) -> BlockColorRegistry.register(List.of(switch (tint) {
            case GRASS_BIOME -> BlockTintSources.grass();
            case FOLIAGE_BIOME -> BlockTintSources.foliage();
            case FOLIAGE_SPRUCE -> BlockTintSources.constant(0x619961);
            case FOLIAGE_BIRCH -> BlockTintSources.constant(0x80A755);
            case NONE -> throw new IllegalArgumentException("NONE tint must not be registered");
        }), block));
    }
}

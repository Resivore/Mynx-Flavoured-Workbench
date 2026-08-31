package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.registry.fabric.ModRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;

import java.util.List;

public final class CnmTerrainCompatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CnmTerrainCompat.initializeNativeCatalog();
        registerNativeClientBindings();
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

    private static void registerNativeClientBindings() {
        for (ModBlocks block : ModBlocks.values()) {
            List<BlockTintSource> tintSources = nativeTintSources(block);
            if (tintSources.isEmpty()) continue;
            for (ModBlocks.BlockType type : ModBlocks.BlockType.values()) {
                if (block.hasBlock(type)) {
                    BlockColorRegistry.register(tintSources, block.getBlock(type));
                }
            }
        }
        EntityRendererRegistry.register(ModRegistry.FALLING_SLAB_BLOCK_ENTITY, FallingBlockRenderer::new);
    }

    private static List<BlockTintSource> nativeTintSources(ModBlocks block) {
        return switch (block) {
            case GRASS_BLOCK -> List.of(BlockTintSources.grassBlock());
            case OAK_LEAVES, JUNGLE_LEAVES, ACACIA_LEAVES, DARK_OAK_LEAVES, MANGROVE_LEAVES ->
                    List.of(BlockTintSources.foliage());
            case SPRUCE_LEAVES -> List.of(BlockTintSources.constant(0xFF619961));
            case BIRCH_LEAVES -> List.of(BlockTintSources.constant(0xFF80A755));
            case PALE_OAK_LEAVES -> List.of();
            default -> List.of();
        };
    }
}

package games.twinhead.moreslabsstairsandwalls.fabric;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.registry.fabric.ModRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import java.util.List;

public class MoreSlabsStairsAndWallsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        initBlockColorsLayers();

        EntityRendererRegistry.register(ModRegistry.FALLING_SLAB_BLOCK_ENTITY, FallingBlockRenderer::new);
    }

    private void initBlockColorsLayers() {
        for (ModBlocks block : ModBlocks.values()) {
            List<BlockTintSource> tintSources = tintSources(block);
            if (tintSources.isEmpty()) continue;
            for (ModBlocks.BlockType type : ModBlocks.BlockType.values()) {
                if (!block.hasBlock(type)) continue;
                BlockColorRegistry.register(tintSources, block.getBlock(type));
            }
        }
    }

    private static List<BlockTintSource> tintSources(ModBlocks block) {
        return switch (block) {
            case GRASS_BLOCK -> List.of(BlockTintSources.grassBlock());
            case OAK_LEAVES, JUNGLE_LEAVES, ACACIA_LEAVES, DARK_OAK_LEAVES, MANGROVE_LEAVES ->
                    List.of(BlockTintSources.foliage());
            case SPRUCE_LEAVES -> List.of(BlockTintSources.constant(0xFF619961));
            case BIRCH_LEAVES -> List.of(BlockTintSources.constant(0xFF80A755));
            // Vanilla Pale Oak leaves carry their final color in the canonical texture.
            case PALE_OAK_LEAVES -> List.of();
            default -> List.of();
        };
    }
}

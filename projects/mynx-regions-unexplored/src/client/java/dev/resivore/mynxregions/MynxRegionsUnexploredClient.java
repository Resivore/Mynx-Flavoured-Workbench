package dev.resivore.mynxregions;

import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSources;

public final class MynxRegionsUnexploredClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        BlockColorRegistry.register(List.of(BlockTintSources.grass()),
                MynxRegionsUnexplored.STONE_BUD, MynxRegionsUnexplored.TASSEL);
        // RU's clover model uses tint index 0 for its bed and 1 for its grayscale stem.
        BlockColorRegistry.register(List.of(BlockTintSources.grass(), BlockTintSources.grass()),
                MynxRegionsUnexplored.CLOVER);
        BlockColorRegistry.register(List.of(BlockTintSources.foliage()), MynxRegionsUnexplored.WINDSWEPT_GRASS);
    }
}

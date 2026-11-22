package dev.resivore.amc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.List;

public final class MossyOakClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BuiltInRegistries.BLOCK.getOptional(Identifier.parse("architectural_material_closure:mossy_oak_lattice"))
                .ifPresent(lattice -> BlockColorRegistry.register(
                        List.of(BlockTintSources.constant(0xFFFFFFFF), BlockTintSources.foliage()), lattice));
    }
}

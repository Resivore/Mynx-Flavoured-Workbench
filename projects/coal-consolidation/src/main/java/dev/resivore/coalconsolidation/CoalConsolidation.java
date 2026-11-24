package dev.resivore.coalconsolidation;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CoalConsolidation implements ModInitializer {
    public void onInitialize() {
        var container = FabricLoader.getInstance().getModContainer("coal_consolidation").orElseThrow();
        if (!ResourceLoader.registerBuiltinPack(Identifier.parse("coal_consolidation:coal_recipes"),
                container, Component.literal("Workbench coal recipes"), PackActivationType.ALWAYS_ENABLED)) {
            throw new IllegalStateException("Could not enable required coal recipes");
        }
    }
}

package com.starfish_studios.bbb.compat;

import com.starfish_studios.bbb.BuildingButBetter;
import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Set;

/** Explicit optional integration for Enderscape's three complete wood-like families. */
public final class EnderscapeWoodIntegration {
    public static final String PROVIDER_ID = "enderscape";
    public static final Identifier PACK_ID = Identifier.fromNamespaceAndPath(
            BuildingButBetter.MOD_ID, "enderscape_wood_families");

    private static final Identifier VEILED_PLANKS = id("veiled_planks");
    private static final Identifier STRIPPED_VEILED_LOG = id("stripped_veiled_log");
    private static final Identifier CELESTIAL_PLANKS = id("celestial_planks");
    private static final Identifier STRIPPED_CELESTIAL_STEM = id("stripped_celestial_stem");
    private static final Identifier MURUBLIGHT_PLANKS = id("murublight_planks");
    private static final Identifier STRIPPED_MURUBLIGHT_STEM = id("stripped_murublight_stem");
    private static final Set<Identifier> REQUIRED_BLOCKS = Set.of(
            VEILED_PLANKS, STRIPPED_VEILED_LOG,
            CELESTIAL_PLANKS, STRIPPED_CELESTIAL_STEM,
            MURUBLIGHT_PLANKS, STRIPPED_MURUBLIGHT_STEM
    );

    private static boolean initialized;
    private static boolean registering;
    private static boolean registered;

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded(PROVIDER_ID)) return;

        ModContainer container = FabricLoader.getInstance().getModContainer(BuildingButBetter.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Missing BBB Fabric mod container"));
        boolean packRegistered = ResourceLoader.registerBuiltinPack(
                PACK_ID,
                container,
                Component.literal("Building But Better — Enderscape wood families"),
                PackActivationType.ALWAYS_ENABLED);
        if (!packRegistered) {
            throw new IllegalStateException("Could not register BBB Enderscape built-in resource pack " + PACK_ID);
        }

        RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((rawId, id, block) -> {
            if (REQUIRED_BLOCKS.contains(id)) tryRegisterFamilies();
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> requireProviderReady());
        tryRegisterFamilies();
    }

    private static synchronized void tryRegisterFamilies() {
        if (registered || registering || !allProviderBlocksPresent()) return;
        registering = true;
        try {
            BBBContent.registerEnderscapeWoodFamilies(
                    requireBlock(VEILED_PLANKS), requireBlock(STRIPPED_VEILED_LOG),
                    requireBlock(CELESTIAL_PLANKS), requireBlock(STRIPPED_CELESTIAL_STEM),
                    requireBlock(MURUBLIGHT_PLANKS), requireBlock(STRIPPED_MURUBLIGHT_STEM));
            registered = true;
        } finally {
            registering = false;
        }
    }

    public static synchronized void requireProviderReady() {
        if (FabricLoader.getInstance().isModLoaded(PROVIDER_ID) && !registered) {
            throw new IllegalStateException("Enderscape is loaded but its explicit BBB wood sources are incomplete: "
                    + REQUIRED_BLOCKS.stream().filter(id -> !isRegistered(id)).toList());
        }
    }

    public static synchronized boolean familiesRegistered() {
        return registered;
    }

    private static boolean allProviderBlocksPresent() {
        return REQUIRED_BLOCKS.stream().allMatch(EnderscapeWoodIntegration::isRegistered);
    }

    private static boolean isRegistered(Identifier id) {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return block != null && id.equals(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static Block requireBlock(Identifier id) {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || !id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            throw new IllegalStateException("Missing exact Enderscape BBB source block " + id);
        }
        return block;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(PROVIDER_ID, path);
    }

    private EnderscapeWoodIntegration() {}
}

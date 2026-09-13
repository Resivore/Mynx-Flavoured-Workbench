package dev.resivore.mynxmatchatradetweaks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MynxMatchaTradeTweaks implements ModInitializer {
    public static final String MOD_ID = "mynx_matcha_trade_tweaks";
    public static final Identifier BUILTIN_PACK_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID);
    public static final String BUILTIN_PACK_RESOURCE_ID = BUILTIN_PACK_ID.toString();

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Missing Fabric mod container for " + MOD_ID));
        boolean registered = ResourceLoader.registerBuiltinPack(
                BUILTIN_PACK_ID,
                container,
                Component.literal("Mynx Matcha Trade Tweaks Canary 1"),
                PackActivationType.ALWAYS_ENABLED);
        if (!registered) {
            throw new IllegalStateException(
                    "Failed to register the required built-in server-data pack " + BUILTIN_PACK_ID);
        }

        LOGGER.info("Registered {} as ALWAYS_ENABLED for narrow Matcha trade overrides.", BUILTIN_PACK_ID);
    }
}

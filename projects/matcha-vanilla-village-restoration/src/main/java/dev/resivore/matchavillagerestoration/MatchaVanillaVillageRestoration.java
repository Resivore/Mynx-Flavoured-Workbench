package dev.resivore.matchavillagerestoration;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MatchaVanillaVillageRestoration implements ModInitializer {
    public static final String MOD_ID = "matcha_vanilla_village_restoration";
    public static final Identifier BUILTIN_PACK_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "vanilla_villages");
    public static final String BUILTIN_PACK_RESOURCE_ID = BUILTIN_PACK_ID.toString();

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Missing Fabric mod container for " + MOD_ID));
        boolean registered = ResourceLoader.registerBuiltinPack(
                BUILTIN_PACK_ID,
                container,
                Component.literal("Matcha Vanilla Village Restoration C2"),
                PackActivationType.ALWAYS_ENABLED);
        if (!registered) {
            throw new IllegalStateException(
                    "Failed to register the required built-in server-data pack " + BUILTIN_PACK_ID);
        }

        LOGGER.info(
                "Registered {} as ALWAYS_ENABLED; exact-pack precedence enforcement is active for its five village definitions.",
                BUILTIN_PACK_ID);
    }
}

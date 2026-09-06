package dev.resivore.matchaflavoureddata;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers the unmodified Matcha 1.12 server-data tree before world creation reloads. */
public final class MatchaFlavouredData implements ModInitializer {
    public static final String MOD_ID = "matcha_flavoured_data";
    public static final Identifier BUILTIN_PACK_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "matcha_flavoured_1_12");

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Missing Fabric mod container for " + MOD_ID));
        boolean registered = ResourceLoader.registerBuiltinPack(
                BUILTIN_PACK_ID,
                container,
                Component.literal("Matcha Flavoured 1.12 server data"),
                PackActivationType.ALWAYS_ENABLED);
        if (!registered) {
            throw new IllegalStateException("Failed to register required Matcha data pack " + BUILTIN_PACK_ID);
        }
        LOGGER.info("Registered {} as ALWAYS_ENABLED for create-world and server-data reloads.", BUILTIN_PACK_ID);
    }
}

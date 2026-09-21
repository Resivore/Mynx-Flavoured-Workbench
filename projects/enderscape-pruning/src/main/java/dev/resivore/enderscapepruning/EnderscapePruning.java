package dev.resivore.enderscapepruning;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers C3's always-enabled, narrowly generated Enderscape data overlay. */
public final class EnderscapePruning implements ModInitializer {
    public static final String MOD_ID = "enderscape_pruning";
    public static final Identifier BUILTIN_PACK_ID = Identifier.fromNamespaceAndPath(MOD_ID, "enderscape_pruning");
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        EnderscapePruningRecipes.register();
        ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Missing Enderscape Pruning mod container"));
        boolean registered = ResourceLoader.registerBuiltinPack(
                BUILTIN_PACK_ID,
                container,
                Component.literal("Enderscape Pruning C3"),
                PackActivationType.ALWAYS_ENABLED);
        if (!registered) {
            throw new IllegalStateException("Could not register required Enderscape Pruning data overlay");
        }
        LOGGER.info("Registered Enderscape Pruning C3 overlay; exact upstream input SHA-256={}",
                PruningContract.ENDERSCAPE_SHA256);
    }
}

package dev.resivore.matchajei.client;

import dev.resivore.matchajei.network.MatchaJeiDataPayload;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.function.Consumer;

/**
 * Exposes the synchronized exact Matcha catalog in the vanilla Creative Search
 * tab. This client-only path intentionally has no JEI API dependency.
 */
final class MatchaCreativeCatalog {
    private static final Consumer<MatchaJeiDataPayload> DATA_LISTENER = payload -> rebuildSearchTab();
    private static boolean initialized;

    private MatchaCreativeCatalog() {
    }

    static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SEARCH).register(output ->
                MatchaClientData.current().catalog().forEach(stack -> output.accept(stack.copyWithCount(1)))
        );
        MatchaClientData.addListener(DATA_LISTENER);
    }

    private static void rebuildSearchTab() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
                client.level.enabledFeatures(),
                client.player.canUseGameMasterBlocks(),
                client.level.registryAccess()
        );
        if (!CreativeModeTabs.tryRebuildTabContents(
                parameters.enabledFeatures(), parameters.hasPermissions(), parameters.holders())) {
            // Vanilla's cached all-tabs rebuild is intentionally a no-op when
            // its parameters are unchanged. Payload revisions are additional
            // data, so rebuild Search directly to replace stale exact stacks.
            CreativeModeTabs.searchTab().buildContents(parameters);
        }
    }
}

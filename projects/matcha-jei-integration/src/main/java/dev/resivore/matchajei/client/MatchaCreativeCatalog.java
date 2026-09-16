package dev.resivore.matchajei.client;

import dev.resivore.matchajei.network.MatchaJeiDataPayload;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Exposes the synchronized exact Matcha catalog in the vanilla Creative Search
 * tab. This client-only path intentionally has no JEI API dependency.
 */
final class MatchaCreativeCatalog {
    private static final Set<ItemStack> OWNED_SEARCH_ENTRIES =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Consumer<MatchaJeiDataPayload> DATA_LISTENER = payload ->
            replaceActiveSearchEntries(payload);
    private static boolean initialized;

    private MatchaCreativeCatalog() {
    }

    static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
            // Search is derived from every ordinary tab's search-only entries.
            // Contributing through Ingredients lets future vanilla/Fabric tab
            // rebuilds include Matcha without displaying the stacks there.
            MatchaCreativeSearchEntries.retainActiveOwnership(
                    CreativeModeTabs.searchTab().getDisplayItems(),
                    OWNED_SEARCH_ENTRIES
            );
            MatchaClientData.current().catalog().forEach(stack -> {
                ItemStack contribution = stack.copyWithCount(1);
                MatchaCreativeSearchEntries.rememberOwned(contribution, OWNED_SEARCH_ENTRIES);
                output.accept(contribution, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
            });
        });
        MatchaClientData.addListener(DATA_LISTENER);
    }

    private static void replaceActiveSearchEntries(MatchaJeiDataPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        MatchaCreativeSearchEntries.replaceOwned(
                CreativeModeTabs.searchTab().getDisplayItems(),
                OWNED_SEARCH_ENTRIES,
                payload.catalog()
        );
    }
}

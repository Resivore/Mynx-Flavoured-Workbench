package dev.resivore.matchajei.client;

import dev.resivore.matchajei.network.MatchaJeiDataPayload;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
    private static final Set<ItemStack> SUPPRESSED_DEFAULT_SEARCH_ENTRIES =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Consumer<MatchaJeiDataPayload> DATA_LISTENER = payload -> pendingPayload = payload;
    private static MatchaJeiDataPayload pendingPayload;
    private static boolean initialized;

    private MatchaCreativeCatalog() {
    }

    static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        // Vanilla constructs the global Search tab from each category's
        // search-only entries. Removing a default only from the already-built
        // global collection is therefore temporary: the next tab rebuild puts
        // it back. Filter the actual category sources before vanilla indexes
        // them, while leaving ordinary category display contents untouched.
        CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, output) -> {
            MatchaCreativeSearchEntries.suppressCanonicalDefaults(
                    output.getSearchTabStacks(), MatchaClientData.current().catalog());
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
            // Search is derived from every ordinary tab's search-only entries.
            // Contributing through Ingredients lets future vanilla/Fabric tab
            // rebuilds include Matcha without displaying the stacks there.
            MatchaCreativeSearchEntries.retainActiveOwnership(
                    CreativeModeTabs.searchTab().getDisplayItems(),
                    OWNED_SEARCH_ENTRIES
            );
            MatchaCreativeSearchEntries.retainActiveOwnership(
                    CreativeModeTabs.searchTab().getDisplayItems(),
                    SUPPRESSED_DEFAULT_SEARCH_ENTRIES
            );
            MatchaClientData.current().catalog().forEach(stack -> {
                ItemStack contribution = stack.copyWithCount(1);
                MatchaCreativeSearchEntries.rememberOwned(contribution, OWNED_SEARCH_ENTRIES);
                output.accept(contribution, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
            });
        });
        MatchaClientData.addListener(DATA_LISTENER);
        ClientTickEvents.END_CLIENT_TICK.register(MatchaCreativeCatalog::applyPendingSearchReplacement);
    }

    /**
     * Custom payloads may arrive before the play client has installed its
     * level and player. C6 returned at that point and never retried, leaving
     * the pre-indexed vanilla default in Search while a later tab build added
     * the Matcha stack. Apply once at the first safe client tick instead.
     */
    private static void applyPendingSearchReplacement(Minecraft client) {
        MatchaJeiDataPayload payload = pendingPayload;
        if (payload == null) {
            return;
        }
        if (client.level == null || client.player == null) {
            return;
        }
        MatchaCreativeSearchEntries.replaceOwned(
                CreativeModeTabs.searchTab().getDisplayItems(),
                OWNED_SEARCH_ENTRIES,
                SUPPRESSED_DEFAULT_SEARCH_ENTRIES,
                payload.catalog()
        );
        pendingPayload = null;
    }
}

package dev.resivore.enderscapepruning.client;

import dev.resivore.enderscapepruning.PruningContract;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.ItemStack;

/** Removes only C1-hidden exact stacks from tab output and the source Search lists. */
public final class EnderscapePruningClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, output) -> {
            output.getDisplayStacks().removeIf(EnderscapePruningClient::hiddenFromDiscovery);
            output.getSearchTabStacks().removeIf(EnderscapePruningClient::hiddenFromDiscovery);
        });
    }

    private static boolean hiddenFromDiscovery(ItemStack stack) {
        return PruningContract.isSuppressedItem(stack) || PruningContract.hasSuppressedStoredEnchantment(stack);
    }
}

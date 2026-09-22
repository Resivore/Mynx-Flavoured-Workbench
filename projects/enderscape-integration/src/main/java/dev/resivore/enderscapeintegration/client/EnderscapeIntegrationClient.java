package dev.resivore.enderscapeintegration.client;

import dev.resivore.enderscapeintegration.IntegrationContract;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.ItemStack;

/** Removes only C1-hidden exact stacks from tab output and the source Search lists. */
public final class EnderscapeIntegrationClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, output) -> {
            output.getDisplayStacks().removeIf(EnderscapeIntegrationClient::hiddenFromDiscovery);
            output.getSearchTabStacks().removeIf(EnderscapeIntegrationClient::hiddenFromDiscovery);
        });
    }

    private static boolean hiddenFromDiscovery(ItemStack stack) {
        return IntegrationContract.isSuppressedItem(stack) || IntegrationContract.hasSuppressedStoredEnchantment(stack);
    }
}

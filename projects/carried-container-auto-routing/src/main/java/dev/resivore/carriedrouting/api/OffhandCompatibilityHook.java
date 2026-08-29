package dev.resivore.carriedrouting.api;

import dev.resivore.carriedrouting.RoutingContext;
import dev.resivore.carriedrouting.RoutingService;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class OffhandCompatibilityHook {
    private OffhandCompatibilityHook() {}

    public static boolean routeIncoming(Player player, ItemStack incoming) {
        return routeIncoming(player, incoming, -1, false);
    }

    public static boolean routeIncoming(Player player, ItemStack incoming,
                                        int excludedInventorySlot, boolean excludeOffhand) {
        int before = incoming.getCount();
        RoutingService.routeIncomingStack(player, incoming, RoutingContext.QUICK_MOVE,
                excludedInventorySlot, excludeOffhand);
        return incoming.getCount() != before;
    }

    public static boolean routePlayerOrigin(Player player, ItemStack incoming,
                                            int excludedInventorySlot, boolean excludeOffhand) {
        return RoutingService.routePlayerOriginSpecialDestinations(
                player, incoming, excludedInventorySlot, excludeOffhand
        );
    }

    public static boolean isDelegatedToOffhand() {
        return FabricLoader.getInstance().isModLoaded("offhand_shift_click_qol");
    }
}

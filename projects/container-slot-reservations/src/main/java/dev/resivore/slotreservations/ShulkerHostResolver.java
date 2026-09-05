package dev.resivore.slotreservations;

import dev.resivore.slotreservations.network.ShulkerHostLocator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Shared exact-slot proof used by panel, reservation, selection, and contextual actions. */
public final class ShulkerHostResolver {
    private ShulkerHostResolver() {}

    public static Optional<ResolvedHost> resolve(ServerPlayer player, int menuId,
                                                 ShulkerHostLocator locator, String fingerprint) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!usable(player, menu, menuId) || locator == null || fingerprint == null) return Optional.empty();
        Slot slot;
        int menuSlot;
        if (locator.kind() == ShulkerHostLocator.Kind.MENU_SLOT) {
            menuSlot = locator.visibleMenuSlot();
            if (menuSlot < 0 || menuSlot >= menu.slots.size()) return Optional.empty();
            slot = menu.slots.get(menuSlot);
        } else {
            if (menu != player.inventoryMenu || locator.physicalPlayerSlot() < 0
                    || locator.physicalPlayerSlot() >= player.getInventory().getContainerSize()) return Optional.empty();
            slot = null;
            menuSlot = -1;
            for (int index = 0; index < menu.slots.size(); index++) {
                Slot candidate = menu.slots.get(index);
                if (candidate.container == player.getInventory()
                        && candidate.getContainerSlot() == locator.physicalPlayerSlot()) {
                    if (slot != null) return Optional.empty();
                    slot = candidate;
                    menuSlot = index;
                }
            }
            if (slot == null) return Optional.empty();
        }
        return resolveMenuSlot(player, menu, menuSlot, slot)
                .filter(host -> ShulkerHostFingerprint.of(host.stack(), player.registryAccess()).equals(fingerprint));
    }

    public static Optional<ResolvedHost> resolveMenuSlot(Player player, AbstractContainerMenu menu, Slot slot) {
        return resolveMenuSlot(player, menu, menu.slots.indexOf(slot), slot);
    }

    private static Optional<ResolvedHost> resolveMenuSlot(Player player, AbstractContainerMenu menu,
                                                          int menuSlot, Slot slot) {
        if (player == null || menu == null || slot == null || menuSlot < 0 || menuSlot >= menu.slots.size()
                || menu.slots.get(menuSlot) != slot || !slot.isActive() || slot.isFake()
                || !slot.container.stillValid(player)) return Optional.empty();
        ItemStack stack = slot.getItem();
        if (stack.getCount() != 1 || !SupportedContainerResolver.isSupportedShulkerItem(stack)
                || !slot.mayPickup(player) || !slot.mayPlace(stack) || !slot.allowModification(player)) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedHost(menu, slot, menuSlot, stack));
    }

    public static boolean writableTarget(Player player, Slot slot, ItemStack incoming) {
        return player != null && slot != null && !incoming.isEmpty() && slot.isActive() && !slot.isFake()
                && slot.container.stillValid(player) && slot.mayPlace(incoming)
                && slot.allowModification(player);
    }

    public static boolean removableSource(Player player, Slot slot) {
        return player != null && slot != null && slot.hasItem() && slot.isActive() && !slot.isFake()
                && slot.container.stillValid(player) && slot.mayPickup(player)
                && slot.allowModification(player);
    }

    private static boolean usable(ServerPlayer player, AbstractContainerMenu menu, int menuId) {
        return player.isAlive() && !player.isSpectator() && menu.containerId == menuId
                && menu.stillValid(player);
    }

    public record ResolvedHost(AbstractContainerMenu menu, Slot slot, int menuSlot, ItemStack stack) {}
}

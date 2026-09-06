package dev.resivore.slotreservations;

import dev.resivore.slotreservations.network.ShulkerHostLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Per-player ephemeral selection; no item component or save data is touched. */
public final class ShulkerSelectionTracker {
    private static final Map<Player, Selection> SELECTIONS = new WeakHashMap<>();

    private ShulkerSelectionTracker() {}

    public static synchronized boolean select(Player player, ShulkerHostResolver.ResolvedHost host,
                                              ShulkerHostLocator locator, int internalSlot, String fingerprint) {
        if (internalSlot < 0 || internalSlot >= ReservationData.SLOT_COUNT
                || ShulkerContents.copy(host.stack()).get(internalSlot).isEmpty()) return false;
        SELECTIONS.put(player, new Selection(HostKind.MENU_SLOT, host.menu(), host.slot(), locator,
                fingerprint, internalSlot));
        return true;
    }

    public static synchronized Optional<Selection> get(Player player) {
        return Optional.ofNullable(SELECTIONS.get(player));
    }

    public static synchronized void clear(Player player) {
        SELECTIONS.remove(player);
    }

    public static synchronized void clearForMenu(Player player, AbstractContainerMenu menu) {
        Selection selection = SELECTIONS.get(player);
        if (selection != null && selection.menu() == menu) SELECTIONS.remove(player);
    }

    public static synchronized Selection validate(Player player) {
        Selection selection = SELECTIONS.get(player);
        if (selection == null) return null;
        ItemStack host;
        if (selection.kind() == HostKind.CARRIED_CURSOR) {
            host = selection.menu().getCarried();
        } else {
            if (selection.menu() != player.containerMenu || selection.slot() == null
                    || selection.menu().slots.indexOf(selection.slot()) < 0) {
                SELECTIONS.remove(player); return null;
            }
            host = selection.slot().getItem();
        }
        if (host.getCount() != 1 || !SupportedContainerResolver.isSupportedShulkerItem(host)
                || !ShulkerHostFingerprint.of(host, player.registryAccess()).equals(selection.fingerprint())) {
            SELECTIONS.remove(player); return null;
        }
        if (ShulkerContents.copy(host).get(selection.internalSlot()).isEmpty()) {
            int next = ShulkerContents.nextOccupied(ShulkerContents.copy(host), selection.internalSlot());
            if (next < 0) { SELECTIONS.remove(player); return null; }
            selection = selection.withIndex(next);
            SELECTIONS.put(player, selection);
        }
        return selection;
    }

    public static synchronized void rebind(Player player, ShulkerHostResolver.ResolvedHost host,
                                           String newFingerprint, int selectedSlot) {
        Selection current = SELECTIONS.get(player);
        if (current != null && current.kind() == HostKind.MENU_SLOT && current.slot() == host.slot()) {
            if (selectedSlot < 0) SELECTIONS.remove(player);
            else SELECTIONS.put(player, new Selection(HostKind.MENU_SLOT, host.menu(), host.slot(),
                    current.locator(), newFingerprint, selectedSlot));
        }
    }

    public static synchronized void rebindCarried(Player player, AbstractContainerMenu menu,
                                                  String newFingerprint, int selectedSlot) {
        Selection current = SELECTIONS.get(player);
        if (current != null && current.kind() == HostKind.CARRIED_CURSOR && current.menu() == menu) {
            if (selectedSlot < 0) SELECTIONS.remove(player);
            else SELECTIONS.put(player, new Selection(HostKind.CARRIED_CURSOR, menu, null,
                    current.locator(), newFingerprint, selectedSlot));
        }
    }

    /**
     * Resolves the server-authoritative carried selection at the native-click seam.
     *
     * A client selection packet may arrive adjacent to the pickup packet that moved the
     * host to the cursor.  Preserve a matching pre-migration host selection when it is
     * still present; otherwise use CSR's existing deterministic first-occupied default.
     * The cursor stack and its complete fingerprint remain the authority in both cases.
     */
    public static synchronized Selection ensureCarriedSelection(Player player, AbstractContainerMenu menu,
                                                                ItemStack carried) {
        if (carried.getCount() != 1 || !SupportedContainerResolver.isSupportedShulkerItem(carried)) return null;
        String fingerprint = ShulkerHostFingerprint.of(carried, player.registryAccess());
        var contents = ShulkerContents.copy(carried);
        Selection current = SELECTIONS.get(player);
        int selected = -1;
        ShulkerHostLocator locator = ShulkerHostLocator.menuSlot(-1);
        if (current != null && current.menu() == menu && current.fingerprint().equals(fingerprint)
                && current.internalSlot() >= 0 && current.internalSlot() < ReservationData.SLOT_COUNT
                && !contents.get(current.internalSlot()).isEmpty()) {
            selected = current.internalSlot();
            locator = current.locator();
        } else {
            selected = ShulkerContents.firstOccupied(contents);
        }
        if (selected < 0) {
            if (current != null && current.menu() == menu) SELECTIONS.remove(player);
            return null;
        }
        Selection carriedSelection = new Selection(HostKind.CARRIED_CURSOR, menu, null, locator,
                fingerprint, selected);
        SELECTIONS.put(player, carriedSelection);
        return carriedSelection;
    }

    public static synchronized ClickMigration beforeClick(Player player, AbstractContainerMenu menu,
                                                          int slotId, int button, ContainerInput input) {
        Selection selection = validate(player);
        if (selection == null || selection.kind() != HostKind.MENU_SLOT || selection.menu() != menu
                || input != ContainerInput.PICKUP || button != 0 || slotId < 0
                || slotId >= menu.slots.size() || menu.slots.get(slotId) != selection.slot()
                || !menu.getCarried().isEmpty()) return null;
        return new ClickMigration(selection, selection.slot().getItem().copy());
    }

    public static synchronized void afterClick(Player player, ClickMigration migration) {
        if (migration == null || SELECTIONS.get(player) != migration.selection()) return;
        ItemStack carried = migration.selection().menu().getCarried();
        if (carried.getCount() == 1 && SupportedContainerResolver.isSupportedShulkerItem(carried)
                && ShulkerHostFingerprint.of(carried, player.registryAccess())
                .equals(migration.selection().fingerprint())
                && !ItemStack.matches(migration.selection().slot().getItem(), migration.before())) {
            SELECTIONS.put(player, new Selection(HostKind.CARRIED_CURSOR, migration.selection().menu(), null,
                    migration.selection().locator(), migration.selection().fingerprint(),
                    migration.selection().internalSlot()));
        } else {
            SELECTIONS.remove(player);
        }
    }

    public enum HostKind { MENU_SLOT, CARRIED_CURSOR }

    public record Selection(HostKind kind, AbstractContainerMenu menu, Slot slot,
                            ShulkerHostLocator locator, String fingerprint, int internalSlot) {
        Selection withIndex(int index) {
            return new Selection(kind, menu, slot, locator, fingerprint, index);
        }
    }

    public record ClickMigration(Selection selection, ItemStack before) {}
}

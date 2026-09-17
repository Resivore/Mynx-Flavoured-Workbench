package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.ShulkerContents;
import dev.resivore.slotreservations.ShulkerHostFingerprint;
import dev.resivore.slotreservations.ShulkerHostResolver;
import dev.resivore.slotreservations.ShulkerSelectionTracker;
import dev.resivore.slotreservations.ShulkerTransferPlanner;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.network.CarriedShulkerInventoryActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Client coordinator for Mouse Tweaks 2.31's native-slot RMB entry callbacks.
 * Only expected predecessor fingerprints are projected locally; the server
 * resolves, plans, and commits every inventory-to-shulker transaction.
 */
public final class CarriedShulkerMouseTweaks {
    private static final int ORDINARY_PLAYER_SLOT_COUNT = 36;
    private static final CarriedShulkerRmbGesture GESTURE = new CarriedShulkerRmbGesture();

    private static AbstractContainerScreen<?> screen;
    private static AbstractContainerMenu menu;
    private static int authoritativeMenuId = -1;
    private static boolean creativeFacade;
    private static ItemStack expectedNativeShulker = ItemStack.EMPTY;
    private static ItemStack expectedNativeSlot = ItemStack.EMPTY;
    private static Slot nativeOutboundSlot;
    private static int selectedInternalSlot = -1;

    private CarriedShulkerMouseTweaks() {}

    /**
     * Begins on an ordinary native press. An occupied origin is consumed and sent through
     * CSR authority immediately; an empty origin remains on Mouse Tweaks' proven native path.
     */
    public static boolean begin(AbstractContainerScreen<?> candidateScreen, Slot target) {
        reset();
        Minecraft client = Minecraft.getInstance();
        if (!MouseTweaksCompatibility.ownsRightDrag() || client.player == null || candidateScreen == null
                || !ClientPlayNetworking.canSend(CarriedShulkerInventoryActionPayload.TYPE)) return false;
        AbstractContainerMenu candidateMenu = candidateScreen.getMenu();
        boolean candidateCreative = candidateScreen instanceof CreativeModeInventoryScreen;
        if (!candidateCreative && candidateMenu != client.player.containerMenu) return false;
        CarriedShulkerRmbGesture.SlotKey key = playerSlotKey(
                client.player, candidateMenu, target, candidateCreative,
                candidateScreen instanceof CreativeModeInventoryScreen creative
                        && creative.isInventoryOpen());
        ItemStack carried = candidateMenu.getCarried();
        if (key == null || carried.getCount() != 1
                || !SupportedContainerResolver.isSupportedShulkerItem(carried)) return false;

        String fingerprint = ShulkerHostFingerprint.of(carried, client.player.registryAccess());
        CarriedShulkerRmbGesture.Mode mode = CarriedShulkerRmbGesture.selectMode(target.hasItem());
        // Mouse Tweaks itself replays an empty origin exactly once when the pointer first
        // leaves it. Inbound cannot rely on that replay because its compatibility gate
        // rejects the occupied ordinary source before invoking a click.
        GESTURE.begin(mode, mode == CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER ? key : null,
                carried, fingerprint);
        screen = candidateScreen;
        menu = candidateMenu;
        creativeFacade = candidateCreative;
        authoritativeMenuId = candidateCreative
                ? client.player.inventoryMenu.containerId : candidateMenu.containerId;
        if (mode == CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER) {
            dispatchInventoryToShulker(client.player, target, key);
            return true;
        }
        // The accepted outbound path performs its first extraction in the native screen click
        // that follows this HEAD hook. Predict and verify that synchronous F0 -> F1 transition
        // before Mouse Tweaks begins replaying/entering further slots.
        nativeOutboundSlot = target;
        planExpectedNativeResult(client.player, target, carried);
        return false;
    }

    /**
     * Runs at HEAD of Mouse Tweaks' exact rmbTweakMaybeClickSlot helper.
     * Returning true cancels that helper invocation.
     */
    public static boolean beforeMouseTweaksSlot(Slot target) {
        if (!GESTURE.isActive()) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !validContext(client) || !validLiveCursor(client.player)) {
            reset();
            return true;
        }

        CarriedShulkerRmbGesture.SlotKey key = playerSlotKey(
                client.player, menu, target, creativeFacade, creativeInventoryTab());
        if (key == null) {
            // The latched gesture owns RMB until release, but non-player and special slots
            // are outside its target surface and must never receive a native fallback click.
            GESTURE.enter(otherSlotKey(target));
            return true;
        }
        if (!GESTURE.enter(key)) return true;

        if (GESTURE.mode() == CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER) {
            if (target.hasItem()) dispatchInventoryToShulker(client.player, target, key);
            return true;
        }

        // Preserve the accepted native outbound behavior only for empty ordinary slots.
        if (target.hasItem() || !ItemStack.matches(menu.getCarried(), GESTURE.projectedShulker())) {
            return true;
        }
        nativeOutboundSlot = target;
        planExpectedNativeResult(client.player, target, menu.getCarried());
        return false;
    }

    /** Runs at RETURN of the exact helper and advances only a proven native outbound result. */
    public static void afterMouseTweaksSlot(Slot target) {
        if (nativeOutboundSlot == null) return;
        if (target != nativeOutboundSlot) {
            reset();
            return;
        }
        finishNativeOutbound(target);
    }

    /** Verifies the native empty-origin click that immediately follows {@link #begin}. */
    public static void afterInitialNativePress() {
        if (nativeOutboundSlot != null) finishNativeOutbound(nativeOutboundSlot);
    }

    private static void finishNativeOutbound(Slot target) {
        nativeOutboundSlot = null;
        if (!GESTURE.isActive() || !acceptExpectedNativeResult(target)) {
            reset();
            return;
        }
        acceptLiveCarried();
        clearExpectedNativeResult();
    }

    /**
     * Runs after Mouse Tweaks' full drag callback so its null-slot transition is visible even
     * when rmbTweakMaybeClickSlot was never called. This preserves A -> blank -> A re-entry.
     */
    public static void observeMouseTweaksDrag(Slot selectedSlot) {
        if (!GESTURE.isActive()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !validContext(client) || !validLiveCursor(client.player)) {
            reset();
            return;
        }
        CarriedShulkerRmbGesture.SlotKey playerSlot = playerSlotKey(
                client.player, menu, selectedSlot, creativeFacade, creativeInventoryTab());
        GESTURE.observe(playerSlot != null ? playerSlot : otherSlotKey(selectedSlot));
    }

    public static void maintain(Minecraft client) {
        if (!GESTURE.isActive()) return;
        if (!validContext(client) || client.player == null || !validLiveCursor(client.player)
                || GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_RELEASE) {
            reset();
        }
    }

    public static void reset() {
        GESTURE.reset();
        screen = null;
        menu = null;
        authoritativeMenuId = -1;
        creativeFacade = false;
        nativeOutboundSlot = null;
        selectedInternalSlot = -1;
        clearExpectedNativeResult();
    }

    private static void dispatchInventoryToShulker(Player player, Slot source,
                                                    CarriedShulkerRmbGesture.SlotKey key) {
        ItemStack before = GESTURE.projectedSource(key, source.getItem());
        if (before.isEmpty()) return;
        ItemStack projected = GESTURE.projectedShulker();
        ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planInsertion(projected, before);
        if (plan.moved() == 0) return;
        String sourceFingerprint = ShulkerHostFingerprint.of(before, player.registryAccess());
        String predecessor = GESTURE.projectedFingerprint();
        if (predecessor == null) {
            reset();
            return;
        }
        ClientPlayNetworking.send(new CarriedShulkerInventoryActionPayload(
                authoritativeMenuId, key.menuSlot(), key.physicalPlayerSlot(), predecessor, sourceFingerprint));
        String successor = ShulkerHostFingerprint.of(plan.shulker(), player.registryAccess());
        GESTURE.advance(plan.shulker(), successor);
        GESTURE.advanceSource(key, plan.remainder());
    }

    private static void planExpectedNativeResult(Player player, Slot target, ItemStack carried) {
        expectedNativeShulker = carried.copy();
        expectedNativeSlot = target == null ? ItemStack.EMPTY : target.getItem().copy();
        if (target == null) return;
        ShulkerSelectionTracker.Selection selection = ShulkerSelectionTracker.ensureCarriedSelection(
                player, menu, carried);
        selectedInternalSlot = selection == null ? -1 : selection.internalSlot();
        if (selectedInternalSlot < 0 || target.hasItem()) return;
        NonNullList<ItemStack> contents = ShulkerContents.copy(carried);
        ItemStack physical = contents.get(selectedInternalSlot);
        if (physical.isEmpty() || !ShulkerHostResolver.writableTarget(player, target, physical)
                || SupportedContainerResolver.resolve(target.container, target.getContainerSlot())
                .filter(resolved -> !ReservationStore.reservationAllows(resolved, physical)).isPresent()) return;
        int capacity = Math.min(target.getMaxStackSize(physical), physical.getMaxStackSize());
        ShulkerTransferPlanner.Extraction plan = ShulkerTransferPlanner.planExtraction(
                carried, selectedInternalSlot, false, capacity);
        if (plan.moved() == 0) return;
        expectedNativeShulker = plan.shulker();
        expectedNativeSlot = plan.extracted();
        selectedInternalSlot = plan.contents().get(selectedInternalSlot).isEmpty()
                ? ShulkerContents.previousOccupied(plan.contents(), selectedInternalSlot)
                : selectedInternalSlot;
    }

    private static boolean acceptExpectedNativeResult(Slot target) {
        if (menu == null || target == null || expectedNativeShulker.isEmpty()) return false;
        return ItemStack.matches(target.getItem(), expectedNativeSlot)
                && ItemStack.matches(menu.getCarried(), expectedNativeShulker);
    }

    private static void acceptLiveCarried() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || menu == null) {
            reset();
            return;
        }
        ItemStack carried = menu.getCarried();
        String fingerprint = ShulkerHostFingerprint.of(carried, client.player.registryAccess());
        GESTURE.advance(carried, fingerprint);
    }

    private static boolean validContext(Minecraft client) {
        if (screen == null || menu == null || client.player == null || authoritativeMenuId < 0) return false;
        if (client.gui.screen() != screen || screen.getMenu() != menu) return false;
        if (creativeFacade) {
            return screen instanceof CreativeModeInventoryScreen
                    && client.player.inventoryMenu.containerId == authoritativeMenuId;
        }
        return client.player.containerMenu == menu && menu.containerId == authoritativeMenuId;
    }

    private static boolean validLiveCursor(Player player) {
        if (menu == null) return false;
        ItemStack carried = menu.getCarried();
        if (carried.getCount() != 1 || !SupportedContainerResolver.isSupportedShulkerItem(carried)) return false;
        return GESTURE.acceptsLiveFingerprint(ShulkerHostFingerprint.of(carried, player.registryAccess()));
    }

    private static CarriedShulkerRmbGesture.SlotKey playerSlotKey(
            Player player, AbstractContainerMenu candidateMenu, Slot target, boolean creative,
            boolean creativeInventoryTab) {
        if (player == null || candidateMenu == null || target == null || target.isFake()
                || !target.isActive() || target.container != player.getInventory()
                || !target.container.stillValid(player)) return null;
        int physical = target.getContainerSlot();
        if (creative) {
            physical = CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(
                    physical, creativeInventoryTab);
        }
        if (physical < 0 || physical >= ORDINARY_PLAYER_SLOT_COUNT) return null;
        int visible = candidateMenu.slots.indexOf(target);
        if (visible < 0) return null;
        return new CarriedShulkerRmbGesture.SlotKey(creative ? -1 : visible, physical);
    }

    private static boolean creativeInventoryTab() {
        return screen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen();
    }

    private static CarriedShulkerRmbGesture.SlotKey otherSlotKey(Slot target) {
        if (menu == null || target == null) return null;
        return new CarriedShulkerRmbGesture.SlotKey(menu.slots.indexOf(target), -1);
    }

    private static void clearExpectedNativeResult() {
        expectedNativeShulker = ItemStack.EMPTY;
        expectedNativeSlot = ItemStack.EMPTY;
    }
}

package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ShulkerHostFingerprint;
import dev.resivore.slotreservations.ShulkerTransferPlanner;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.mixin.client.ContainerScreenMouseAccess;
import dev.resivore.slotreservations.network.CarriedShulkerInventoryActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * CSR-owned traversal for an occupied-origin, cursor-held shulker RMB drag.
 * It deliberately does not use Mouse Tweaks' compatible-stack helper: the
 * shulker and each arbitrary source stack are expected to be incompatible.
 */
public final class CarriedShulkerRmbCollector {
    private static final int ORDINARY_PLAYER_SLOT_COUNT = 36;
    private static final CarriedShulkerRmbGesture GESTURE = new CarriedShulkerRmbGesture();

    private static AbstractContainerScreen<?> screen;
    private static AbstractContainerMenu menu;
    private static int authoritativeMenuId = -1;
    private static boolean creativeFacade;

    private CarriedShulkerRmbCollector() {}

    /**
     * Claims only an occupied ordinary player source. An empty origin returns
     * false without retaining state so outbound remains the native path.
     */
    public static boolean begin(AbstractContainerScreen<?> candidateScreen, Slot target) {
        reset();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || candidateScreen == null
                || !ClientPlayNetworking.canSend(CarriedShulkerInventoryActionPayload.TYPE)) return false;
        AbstractContainerMenu candidateMenu = candidateScreen.getMenu();
        boolean candidateCreative = candidateScreen instanceof CreativeModeInventoryScreen;
        if (!candidateCreative && candidateMenu != client.player.containerMenu) return false;
        CarriedShulkerRmbGesture.SlotKey key = playerSlotKey(client.player, candidateMenu, target,
                candidateCreative, candidateScreen instanceof CreativeModeInventoryScreen creative
                        && creative.isInventoryOpen());
        ItemStack carried = candidateMenu.getCarried();
        if (key == null || !target.hasItem() || carried.getCount() != 1
                || !SupportedContainerResolver.isSupportedShulkerItem(carried)) return false;

        screen = candidateScreen;
        menu = candidateMenu;
        creativeFacade = candidateCreative;
        authoritativeMenuId = candidateCreative
                ? client.player.inventoryMenu.containerId : candidateMenu.containerId;
        GESTURE.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER, key, carried,
                ShulkerHostFingerprint.of(carried, client.player.registryAccess()));
        dispatchInventoryToShulker(client.player, target, key);
        return true;
    }

    /** Handles the native screen-level coordinates after every RMB drag callback. */
    public static boolean drag(AbstractContainerScreen<?> candidateScreen, double mouseX, double mouseY) {
        if (!GESTURE.isActive()) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || candidateScreen != screen || !validContext(client)
                || !validLiveCursor(client.player)) {
            reset();
            return false;
        }
        Slot target = ((ContainerScreenMouseAccess) candidateScreen)
                .containerSlotReservations$slotAt(mouseX, mouseY);
        CarriedShulkerRmbGesture.SlotKey key = playerSlotKey(client.player, menu, target, creativeFacade,
                creativeInventoryTab());
        if (key == null) {
            GESTURE.leaveSlotSurface();
            return true;
        }
        if (!GESTURE.enter(key)) return true;
        if (target.hasItem()) dispatchInventoryToShulker(client.player, target, key);
        return true;
    }

    public static void maintain(Minecraft client) {
        if (!GESTURE.isActive()) return;
        if (!validContext(client) || client.player == null || !validLiveCursor(client.player)
                || GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_RELEASE) reset();
    }

    public static void reset() {
        GESTURE.reset();
        screen = null;
        menu = null;
        authoritativeMenuId = -1;
        creativeFacade = false;
    }

    private static void dispatchInventoryToShulker(Player player, Slot source,
                                                    CarriedShulkerRmbGesture.SlotKey key) {
        ItemStack before = GESTURE.projectedSource(key, source.getItem());
        if (before.isEmpty()) return;
        ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planInsertion(
                GESTURE.projectedShulker(), before);
        if (plan.moved() == 0) return;
        String predecessor = GESTURE.projectedFingerprint();
        if (predecessor == null) {
            reset();
            return;
        }
        ClientPlayNetworking.send(new CarriedShulkerInventoryActionPayload(authoritativeMenuId,
                key.menuSlot(), key.physicalPlayerSlot(), predecessor,
                ShulkerHostFingerprint.of(before, player.registryAccess())));
        GESTURE.advance(plan.shulker(), ShulkerHostFingerprint.of(plan.shulker(), player.registryAccess()));
        GESTURE.advanceSource(key, plan.remainder());
    }

    private static boolean validContext(Minecraft client) {
        if (screen == null || menu == null || client.player == null || authoritativeMenuId < 0) return false;
        if (client.gui.screen() != screen || screen.getMenu() != menu) return false;
        return creativeFacade
                ? screen instanceof CreativeModeInventoryScreen
                        && client.player.inventoryMenu.containerId == authoritativeMenuId
                : client.player.containerMenu == menu && menu.containerId == authoritativeMenuId;
    }

    private static boolean validLiveCursor(Player player) {
        if (menu == null) return false;
        ItemStack carried = menu.getCarried();
        return carried.getCount() == 1 && SupportedContainerResolver.isSupportedShulkerItem(carried)
                && GESTURE.acceptsLiveFingerprint(
                        ShulkerHostFingerprint.of(carried, player.registryAccess()));
    }

    private static CarriedShulkerRmbGesture.SlotKey playerSlotKey(
            Player player, AbstractContainerMenu candidateMenu, Slot target, boolean creative,
            boolean creativeInventoryTab) {
        if (player == null || candidateMenu == null || target == null || target.isFake()
                || !target.isActive() || target.container != player.getInventory()
                || !target.container.stillValid(player)) return null;
        int physical = target.getContainerSlot();
        if (creative) physical = CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(
                physical, creativeInventoryTab);
        if (physical < 0 || physical >= ORDINARY_PLAYER_SLOT_COUNT) return null;
        int visible = candidateMenu.slots.indexOf(target);
        return visible < 0 ? null : new CarriedShulkerRmbGesture.SlotKey(creative ? -1 : visible, physical);
    }

    private static boolean creativeInventoryTab() {
        return screen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen();
    }
}

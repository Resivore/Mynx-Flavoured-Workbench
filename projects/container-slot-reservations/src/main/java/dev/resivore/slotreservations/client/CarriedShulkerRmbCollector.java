package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ShulkerHostFingerprint;
import dev.resivore.slotreservations.ShulkerTransferPlanner;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.mixin.client.ContainerScreenMouseAccess;
import dev.resivore.slotreservations.network.CarriedShulkerInventoryActionPayload;
import dev.resivore.slotreservations.network.CreativeCarriedShulkerSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Owns only occupied-origin RMB traversal from ordinary inventory slots into a carried shulker. */
public final class CarriedShulkerRmbCollector {
    private static final CarriedShulkerRmbGesture GESTURE = new CarriedShulkerRmbGesture();

    private static AbstractContainerScreen<?> screen;
    private static AbstractContainerMenu menu;
    private static int authoritativeMenuId = -1;
    private static boolean creativeFacade;
    private static boolean ownsPhysicalRmb;

    private CarriedShulkerRmbCollector() {}

    /** C27 keeps the established Fabric seam without C26's default diagnostic output. */
    public static void observePhysicalPress(AbstractContainerScreen<?> candidateScreen,
                                            double mouseX, double mouseY, int button,
                                            boolean shift, boolean control, boolean alt) {
    }

    /** Retained mixin seam; C27 intentionally emits no default input trace. */
    public static void observeVanillaScreenClick(AbstractContainerScreen<?> candidateScreen,
                                                 double mouseX, double mouseY) {
    }

    /** Empty origins return false, preserving the established shulker-to-inventory RMB direction. */
    public static boolean begin(AbstractContainerScreen<?> candidateScreen, Slot target) {
        reset();
        Minecraft client = Minecraft.getInstance();
        if (candidateScreen == null || client.player == null
                || !ClientPlayNetworking.canSend(CarriedShulkerInventoryActionPayload.TYPE)) return false;

        AbstractContainerMenu candidateMenu = candidateScreen.getMenu();
        boolean candidateCreative = candidateScreen instanceof CreativeModeInventoryScreen;
        if (!candidateCreative && candidateMenu != client.player.containerMenu) return false;
        CarriedShulkerSourceSlot.Resolution source = CarriedShulkerSourceSlot.resolve(
                client.player, candidateMenu, target, candidateCreative,
                candidateScreen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen());
        ItemStack carried = candidateMenu.getCarried();
        if (source == null || target == null || !target.hasItem()
                || carried.getCount() != 1 || !SupportedContainerResolver.isSupportedShulkerItem(carried)) {
            return false;
        }

        screen = candidateScreen;
        menu = candidateMenu;
        creativeFacade = candidateCreative;
        ownsPhysicalRmb = true;
        authoritativeMenuId = candidateCreative ? client.player.inventoryMenu.containerId : candidateMenu.containerId;
        GESTURE.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER, source.hovered(), carried,
                ShulkerHostFingerprint.of(carried, client.player.registryAccess()));
        dispatchInventoryToShulker(client.player, target, source.key());
        return true;
    }

    /** Handles each later native screen-level drag coordinate. */
    public static boolean drag(AbstractContainerScreen<?> candidateScreen, double mouseX, double mouseY) {
        if (!ownsPhysicalRmb) return false;
        if (!GESTURE.isActive()) return true;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || candidateScreen != screen || !validContext(client)
                || !validLiveCursor(client.player)) {
            GESTURE.reset();
            return true;
        }
        Slot target = ((ContainerScreenMouseAccess) candidateScreen)
                .containerSlotReservations$slotAt(mouseX, mouseY);
        CarriedShulkerSourceSlot.Resolution source = CarriedShulkerSourceSlot.resolve(
                client.player, menu, target, creativeFacade, creativeInventoryTab());
        if (source == null || !GESTURE.enter(source.hovered())) return true;
        if (target.hasItem()) dispatchInventoryToShulker(client.player, target, source.key());
        return true;
    }

    public static void maintain(Minecraft client) {
        if (!ownsPhysicalRmb) return;
        if (GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_RELEASE) {
            reset();
        } else if (GESTURE.isActive()
                && (!validContext(client) || client.player == null || !validLiveCursor(client.player))) {
            GESTURE.reset();
        }
    }

    /** Ends input ownership and reports whether CSR consumed this physical RMB hold. */
    public static boolean release() {
        boolean owned = ownsPhysicalRmb;
        reset();
        return owned;
    }

    /** Used only by the exact Mouse Tweaks pre-screen guard. */
    public static boolean ownsInboundRmb() {
        return ownsPhysicalRmb;
    }

    public static void reset() {
        GESTURE.reset();
        screen = null;
        menu = null;
        authoritativeMenuId = -1;
        creativeFacade = false;
        ownsPhysicalRmb = false;
    }

    private static void dispatchInventoryToShulker(Player player, Slot source,
                                                    CarriedShulkerRmbGesture.SlotKey key) {
        ItemStack before = source.getItem().copy();
        if (before.isEmpty()) return;
        ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planInsertion(
                GESTURE.projectedShulker(), before);
        if (plan.moved() == 0) return;
        String predecessor = GESTURE.projectedFingerprint();
        if (predecessor == null) {
            GESTURE.reset();
            return;
        }
        ClientPlayNetworking.send(new CarriedShulkerInventoryActionPayload(authoritativeMenuId,
                key.menuSlot(), key.physicalPlayerSlot(), predecessor,
                ShulkerHostFingerprint.of(before, player.registryAccess()),
                creativeFacade ? menu.getCarried().copy() : ItemStack.EMPTY));
        GESTURE.advance(plan.shulker(), ShulkerHostFingerprint.of(plan.shulker(), player.registryAccess()));
        // Creative's visible cursor is client-only; project it locally while the server validates F0 -> F1 -> F2.
        if (creativeFacade) menu.setCarried(plan.shulker());
    }

    /** Applies only a current, count-one supported shulker response to the Creative facade. */
    public static void acceptCreativeCursorSync(Minecraft client, CreativeCarriedShulkerSyncPayload payload) {
        if (!creativeFacade || client.player == null || screen == null || menu == null
                || client.gui.screen() != screen || !(screen instanceof CreativeModeInventoryScreen)
                || client.player.inventoryMenu != menu || menu.containerId != payload.menuId()) return;
        ItemStack returned = payload.carried();
        if (returned.isEmpty() || returned.getCount() != 1
                || !SupportedContainerResolver.isSupportedShulkerItem(returned)) return;
        ItemStack visible = menu.getCarried();
        if (visible.isEmpty()) return;
        String visibleFingerprint = ShulkerHostFingerprint.of(visible, client.player.registryAccess());
        String returnedFingerprint = ShulkerHostFingerprint.of(returned, client.player.registryAccess());
        if (visibleFingerprint.equals(payload.predecessorFingerprint()) || visibleFingerprint.equals(returnedFingerprint)) {
            menu.setCarried(returned.copy());
        }
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

    static boolean supportsOccupiedOrigin(ItemStack carried, Slot target) {
        return target != null && target.hasItem() && carried.getCount() == 1
                && SupportedContainerResolver.isSupportedShulkerItem(carried);
    }

    private static boolean creativeInventoryTab() {
        return screen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen();
    }
}

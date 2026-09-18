package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ShulkerHostFingerprint;
import dev.resivore.slotreservations.ShulkerTransferPlanner;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.CarriedShulkerRmbTrace;
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
    private static final CarriedShulkerRmbGesture GESTURE = new CarriedShulkerRmbGesture();

    private static AbstractContainerScreen<?> screen;
    private static AbstractContainerMenu menu;
    private static int authoritativeMenuId = -1;
    private static boolean creativeFacade;
    private static boolean ownsPhysicalRmb;
    private static int payloadsSent;
    private static long pendingPhysicalGesture;

    private CarriedShulkerRmbCollector() {}

    /** Runs at Fabric allowMouseClick entry, before CSR applies any eligibility decision. */
    public static void observePhysicalPress(AbstractContainerScreen<?> candidateScreen,
                                            double mouseX, double mouseY, int button,
                                            boolean shift, boolean control, boolean alt) {
        if (button != 1 || candidateScreen == null) return;
        ItemStack carried = candidateScreen.getMenu().getCarried();
        Slot target = ((ContainerScreenMouseAccess) candidateScreen)
                .containerSlotReservations$slotAt(mouseX, mouseY);
        boolean relevant = SupportedContainerResolver.isSupportedShulkerItem(carried);
        if (!relevant && (target == null || !target.hasItem())) return;
        long gesture = CarriedShulkerRmbTrace.nextGesture();
        pendingPhysicalGesture = gesture;
        CarriedShulkerRmbTrace.client(gesture, "PHYSICAL_RMB_PRESS_SEEN", "screen="
                + candidateScreen.getClass().getName() + " x=" + mouseX + " y=" + mouseY
                + " button=" + button + " shift=" + shift + " control=" + control + " alt=" + alt
                + " menu=" + candidateScreen.getMenu().getClass().getName() + " menuId="
                + candidateScreen.getMenu().containerId + " carried=" + CarriedShulkerRmbTrace.stack(carried)
                + " supportedShulker=" + relevant);
        CarriedShulkerRmbTrace.client(gesture, "FABRIC_ALLOW_CLICK_ENTER", "phase=csr:carried_shulker_input");
        if (shift || control || alt) {
            CarriedShulkerRmbTrace.client(gesture, "ORIGIN_DECISION", "reason=MODIFIER_REJECTED");
        }
    }

    /** Documents the vanilla seam only when upstream did not consume the press first. */
    public static void observeVanillaScreenClick(AbstractContainerScreen<?> candidateScreen,
                                                 double mouseX, double mouseY) {
        ItemStack carried = candidateScreen.getMenu().getCarried();
        Slot target = ((ContainerScreenMouseAccess) candidateScreen)
                .containerSlotReservations$slotAt(mouseX, mouseY);
        if (!SupportedContainerResolver.isSupportedShulkerItem(carried)
                && (target == null || !target.hasItem())) return;
        CarriedShulkerRmbTrace.client(0, "VANILLA_SCREEN_CLICK_ENTER", "screen="
                + candidateScreen.getClass().getName() + " "
                + CarriedShulkerRmbTrace.slot(target, candidateScreen.getMenu().slots.indexOf(target)));
    }

    /**
     * Claims only an occupied ordinary player source. An empty origin returns
     * false without retaining state so outbound remains the native path.
     */
    public static boolean begin(AbstractContainerScreen<?> candidateScreen, Slot target) {
        reset();
        Minecraft client = Minecraft.getInstance();
        long traceGesture = pendingPhysicalGesture == 0 ? CarriedShulkerRmbTrace.nextGesture() : pendingPhysicalGesture;
        pendingPhysicalGesture = 0;
        if (candidateScreen == null || client.player == null) {
            CarriedShulkerRmbTrace.client(traceGesture, "ORIGIN_DECISION", "reason=NO_PLAYER_OR_SCREEN");
            return false;
        }
        AbstractContainerMenu candidateMenu = candidateScreen.getMenu();
        boolean candidateCreative = candidateScreen instanceof CreativeModeInventoryScreen;
        ItemStack carried = candidateMenu.getCarried();
        CarriedShulkerRmbTrace.client(traceGesture, "SCREEN_CONTEXT", "screen="
                + candidateScreen.getClass().getName() + " menu=" + candidateMenu.getClass().getName()
                + " menuId=" + candidateMenu.containerId + " playerPresent=true menuMatches="
                + (candidateCreative || candidateMenu == client.player.containerMenu));
        CarriedShulkerRmbTrace.client(traceGesture, "HOVERED_SLOT", CarriedShulkerRmbTrace.slot(target,
                candidateMenu.slots.indexOf(target)) + " canDragTo=" + (target != null && candidateMenu.canDragTo(target)));
        CarriedShulkerRmbTrace.client(traceGesture, "CARRIED_SHULKER_CLASSIFICATION", CarriedShulkerRmbTrace.stack(carried)
                + " supported=" + SupportedContainerResolver.isSupportedShulkerItem(carried));
        CarriedShulkerRmbTrace.client(traceGesture, "CAN_SEND", "payload="
                + CarriedShulkerInventoryActionPayload.TYPE.id() + " canSend="
                + ClientPlayNetworking.canSend(CarriedShulkerInventoryActionPayload.TYPE));
        if (!ClientPlayNetworking.canSend(CarriedShulkerInventoryActionPayload.TYPE)) {
            CarriedShulkerRmbTrace.client(traceGesture, "ORIGIN_DECISION", "reason=NETWORK_UNAVAILABLE");
            return false;
        }
        if (!candidateCreative && candidateMenu != client.player.containerMenu) {
            CarriedShulkerRmbTrace.client(traceGesture, "ORIGIN_DECISION", "reason=MENU_MISMATCH");
            return false;
        }
        CarriedShulkerSourceSlot.Resolution source = CarriedShulkerSourceSlot.resolve(
                client.player, candidateMenu, target,
                candidateCreative, candidateScreen instanceof CreativeModeInventoryScreen creative
                        && creative.isInventoryOpen());
        if (source == null) {
            CarriedShulkerRmbTrace.client(traceGesture, "ORDINARY_PLAYER_SLOT_CLASSIFICATION",
                    "eligible=false reason=NO_HOVERED_SLOT_OR_NON_PLAYER_SLOT");
            CarriedShulkerRmbTrace.client(traceGesture, "ORIGIN_DECISION", "reason=NON_PLAYER_SLOT");
            return false;
        }
        CarriedShulkerRmbTrace.client(traceGesture, "ORDINARY_PLAYER_SLOT_CLASSIFICATION", "eligible=true backing="
                + CarriedShulkerRmbTrace.slot(source.backing(), candidateMenu.slots.indexOf(source.backing())));
        CarriedShulkerRmbTrace.client(traceGesture, "CREATIVE_WRAPPER", "creative=" + candidateCreative
                + " inventoryTab=" + creativeInventoryTab() + " hovered="
                + CarriedShulkerRmbTrace.slot(source.hovered(), candidateMenu.slots.indexOf(source.hovered()))
                + " backing=" + CarriedShulkerRmbTrace.slot(source.backing(), candidateMenu.slots.indexOf(source.backing())));
        if (target == null || !target.hasItem()) {
            CarriedShulkerRmbTrace.client(traceGesture, "ORIGIN_DECISION", "reason=EMPTY_ORIGIN_PASSTHROUGH");
            return false;
        }
        if (carried.getCount() != 1 || !SupportedContainerResolver.isSupportedShulkerItem(carried)) {
            CarriedShulkerRmbTrace.client(traceGesture, "ORIGIN_DECISION", "reason=UNSUPPORTED_CURSOR");
            return false;
        }

        screen = candidateScreen;
        menu = candidateMenu;
        creativeFacade = candidateCreative;
        ownsPhysicalRmb = true;
        authoritativeMenuId = candidateCreative
                ? client.player.inventoryMenu.containerId : candidateMenu.containerId;
        GESTURE.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                source.hovered(), carried,
                ShulkerHostFingerprint.of(carried, client.player.registryAccess()));
        GESTURE.traceGesture(traceGesture);
        CarriedShulkerRmbTrace.client(traceGesture, "ORIGIN_DECISION", "result=INBOUND_CLAIMED");
        CarriedShulkerRmbTrace.client(traceGesture, "GESTURE_BEGIN", "menuId=" + authoritativeMenuId
                + " menuSlot=" + source.key().menuSlot() + " physicalSlot=" + source.key().physicalPlayerSlot()
                + " source=" + CarriedShulkerRmbTrace.stack(target.getItem()) + " cursorFingerprint="
                + CarriedShulkerRmbTrace.shortFingerprint(GESTURE.projectedFingerprint()));
        dispatchInventoryToShulker(client.player, target, source.key());
        return true;
    }

    /** Handles the native screen-level coordinates after every RMB drag callback. */
    public static boolean drag(AbstractContainerScreen<?> candidateScreen, double mouseX, double mouseY) {
        if (!ownsPhysicalRmb) return false;
        if (!GESTURE.isActive()) return true;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || candidateScreen != screen || !validContext(client)
                || !validLiveCursor(client.player)) {
            trace("RMB_RESET", "reason=INVALID_CONTEXT_OR_CURSOR");
            GESTURE.reset();
            return true;
        }
        Slot target = ((ContainerScreenMouseAccess) candidateScreen)
                .containerSlotReservations$slotAt(mouseX, mouseY);
        CarriedShulkerSourceSlot.Resolution source = CarriedShulkerSourceSlot.resolve(
                client.player, menu, target, creativeFacade, creativeInventoryTab());
        if (source == null || !GESTURE.enter(source.hovered())) {
            trace("DRAG_SLOT_ENTER", source == null ? "slot=null_or_ineligible" : "firstVisit=false alreadyVisited=true");
            return true;
        }
        trace("DRAG_SLOT_ENTER", CarriedShulkerRmbTrace.slot(target, menu.slots.indexOf(target))
                + " eligible=true firstVisit=true alreadyVisited=false");
        if (!target.hasItem()) {
            trace("DRAG_EMPTY_NOOP", "physicalSlot=" + source.key().physicalPlayerSlot());
        }
        if (target.hasItem()) dispatchInventoryToShulker(client.player, target, source.key());
        return true;
    }

    public static void maintain(Minecraft client) {
        if (!ownsPhysicalRmb) return;
        if (GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_RELEASE) {
            trace("RMB_RELEASE", "reason=PHYSICAL_RELEASE visited=" + GESTURE.visitedCount()
                    + " payloadsSent=" + payloadsSent);
            reset();
            return;
        }
        if (GESTURE.isActive()
                && (!validContext(client) || client.player == null || !validLiveCursor(client.player))) {
            GESTURE.reset();
        }
    }

    /** Ends input ownership and reports whether CSR consumed this physical RMB hold. */
    public static boolean release() {
        boolean owned = ownsPhysicalRmb;
        if (owned) trace("RMB_RELEASE", "reason=EVENT_RELEASE visited=" + GESTURE.visitedCount()
                + " payloadsSent=" + payloadsSent);
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
        payloadsSent = 0;
    }

    private static void dispatchInventoryToShulker(Player player, Slot source,
                                                    CarriedShulkerRmbGesture.SlotKey key) {
        ItemStack before = source.getItem().copy();
        if (before.isEmpty()) {
            trace("CLIENT_PLAN", "moved=0 reason=EMPTY_SOURCE");
            return;
        }
        ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planInsertion(
                GESTURE.projectedShulker(), before);
        trace("CLIENT_PLAN", "source=" + CarriedShulkerRmbTrace.stack(before) + " moved=" + plan.moved()
                + " remainder=" + plan.remainder().getCount() + " predecessor="
                + CarriedShulkerRmbTrace.shortFingerprint(GESTURE.projectedFingerprint()));
        if (plan.moved() == 0) return;
        String predecessor = GESTURE.projectedFingerprint();
        if (predecessor == null) {
            trace("RMB_RESET", "reason=MISSING_PROJECTED_FINGERPRINT");
            GESTURE.reset();
            return;
        }
        CarriedShulkerRmbTrace.client(GESTURE.traceGesture(), "PAYLOAD_SEND", "menuId=" + authoritativeMenuId
                + " menuSlot=" + key.menuSlot() + " physicalSlot=" + key.physicalPlayerSlot()
                + " predecessor=" + CarriedShulkerRmbTrace.shortFingerprint(predecessor)
                + " source=" + CarriedShulkerRmbTrace.shortFingerprint(ShulkerHostFingerprint.of(before, player.registryAccess())));
        ClientPlayNetworking.send(new CarriedShulkerInventoryActionPayload(authoritativeMenuId,
                key.menuSlot(), key.physicalPlayerSlot(), predecessor,
                ShulkerHostFingerprint.of(before, player.registryAccess())));
        payloadsSent++;
        CarriedShulkerRmbTrace.client(GESTURE.traceGesture(), "PAYLOAD_SEND_RETURNED", "payloadsSent=" + payloadsSent);
        GESTURE.advance(plan.shulker(), ShulkerHostFingerprint.of(plan.shulker(), player.registryAccess()));
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

    private static void trace(String stage, String details) {
        CarriedShulkerRmbTrace.client(GESTURE.traceGesture(), stage, details);
    }
}

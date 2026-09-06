package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.ReservationTemplateEligibility;
import dev.resivore.slotreservations.ShulkerContents;
import dev.resivore.slotreservations.ShulkerHostFingerprint;
import dev.resivore.slotreservations.ShulkerHostResolver;
import dev.resivore.slotreservations.ShulkerSelectionTracker;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.network.ReservationActionPayload;
import dev.resivore.slotreservations.network.ShulkerHostLocator;
import dev.resivore.slotreservations.network.ShulkerPanelContentActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelReservationActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelSyncPayload;
import dev.resivore.slotreservations.network.ShulkerSelectionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** CSR-owned pinned panel state machine, rendering, hit testing, and input ownership. */
public final class ShulkerPanel {
    private static final Identifier SHULKER_TEXTURE = Identifier.withDefaultNamespace(
            "textures/gui/container/shulker_box.png");
    private static final Identifier HIGHLIGHT_BACK = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier HIGHLIGHT_FRONT = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static Binding binding;
    private static ShulkerPanelGeometry geometry;
    private static int hoveredCell = -1;
    private static final ShulkerPanelState STATE = new ShulkerPanelState();
    private static String expectedFingerprint;
    private static int lastSentSelection = Integer.MIN_VALUE;

    private ShulkerPanel() {}

    public static void updateAndRender(AbstractContainerScreen<?> screen, Slot hoveredSlot,
                                       GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       int leftPos, int topPos, int imageWidth) {
        Minecraft client = Minecraft.getInstance();
        Candidate candidate = eligible(screen, hoveredSlot, leftPos, topPos);
        boolean insideOwnedArea = binding != null && geometry != null
                && (geometry.bounds().contains(mouseX, mouseY)
                || binding.hostBounds().contains(mouseX, mouseY)
                || geometry.corridorContains(binding.hostBounds(), mouseX, mouseY));

        if (binding == null) {
            if (candidate == null) return;
            open(candidate);
        } else if (candidate != null && candidate.slot() != binding.slot() && !insideOwnedArea) {
            open(candidate);
        } else if (!refreshLiveBinding(client, screen)) {
            close(true); return;
        }

        geometry = ShulkerPanelGeometry.place(graphics.guiWidth(), graphics.guiHeight(),
                leftPos, imageWidth, binding.hostBounds());
        boolean retained = binding.hostBounds().contains(mouseX, mouseY)
                || geometry.bounds().contains(mouseX, mouseY)
                || geometry.corridorContains(binding.hostBounds(), mouseX, mouseY);
        if (!STATE.retain(retained)) { close(false); return; }

        hoveredCell = geometry.slot(mouseX, mouseY);
        NonNullList<ItemStack> contents = ShulkerContents.copy(binding.slot().getItem());
        if (hoveredCell >= 0 && !contents.get(hoveredCell).isEmpty()) setSelection(hoveredCell);
        else ensureSelection(contents);
        render(graphics, contents);
    }

    private static Candidate eligible(AbstractContainerScreen<?> screen, Slot slot, int leftPos, int topPos) {
        Minecraft client = Minecraft.getInstance();
        if (slot == null || client.player == null) return null;
        AbstractContainerMenu menu = screen.getMenu();
        var resolved = ShulkerHostResolver.resolveMenuSlot(client.player, menu, slot);
        if (resolved.isEmpty()) return null;
        int menuIndex = menu.slots.indexOf(slot);
        ShulkerHostLocator locator;
        if (screen instanceof CreativeModeInventoryScreen creative) {
            if (!creative.isInventoryOpen() || slot.container != client.player.getInventory()) return null;
            locator = ShulkerHostLocator.playerInventory(menuIndex, slot.getContainerSlot());
        } else {
            locator = ShulkerHostLocator.menuSlot(menuIndex);
        }
        ItemStack stack = slot.getItem();
        String fingerprint = ShulkerHostFingerprint.of(stack, client.player.registryAccess());
        return new Candidate(screen, menu, slot, menuIndex, locator, fingerprint,
                new ShulkerPanelGeometry.Rect(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18));
    }

    private static void open(Candidate candidate) {
        if (binding != null && binding.slot() != candidate.slot()) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) ShulkerSelectionTracker.clear(client.player);
        }
        binding = new Binding(candidate.screen(), candidate.menu(), candidate.menu().containerId,
                candidate.menuSlot(), candidate.slot(), candidate.slot().getContainerSlot(), candidate.locator(),
                candidate.fingerprint(), candidate.hostBounds());
        geometry = null;
        hoveredCell = -1;
        STATE.open();
        expectedFingerprint = null;
        lastSentSelection = Integer.MIN_VALUE;
        ensureSelection(ShulkerContents.copy(candidate.slot().getItem()));
    }

    private static boolean refreshLiveBinding(Minecraft client, AbstractContainerScreen<?> screen) {
        if (client.player == null || client.gui.screen() != binding.screen() || screen != binding.screen()
                || screen.getMenu() != binding.menu() || binding.menu().containerId != binding.menuId()
                || binding.menuSlot() < 0 || binding.menuSlot() >= binding.menu().slots.size()
                || binding.menu().slots.get(binding.menuSlot()) != binding.slot()
                || binding.slot().getContainerSlot() != binding.containerSlot()
                || !binding.slot().isActive() || binding.slot().isFake()) return false;
        ItemStack current = binding.slot().getItem();
        if (current.getCount() != 1 || !SupportedContainerResolver.isSupportedShulkerItem(current)) return false;
        String currentFingerprint = ShulkerHostFingerprint.of(current, client.player.registryAccess());
        if (!currentFingerprint.equals(binding.fingerprint())) {
            if (!currentFingerprint.equals(expectedFingerprint)) return false;
            binding = binding.withFingerprint(currentFingerprint);
            expectedFingerprint = null;
            ShulkerHostResolver.resolveMenuSlot(client.player, binding.menu(), binding.slot())
                    .ifPresent(host -> ShulkerSelectionTracker.rebind(
                            client.player, host, currentFingerprint, lastSentSelection));
        }
        return true;
    }

    private static void ensureSelection(List<ItemStack> contents) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || binding == null) return;
        ShulkerSelectionTracker.Selection current = ShulkerSelectionTracker.get(client.player).orElse(null);
        if (current != null && current.kind() == ShulkerSelectionTracker.HostKind.MENU_SLOT
                && current.slot() == binding.slot() && current.internalSlot() >= 0
                && !contents.get(current.internalSlot()).isEmpty()) return;
        setSelection(ShulkerContents.lastOccupied(contents));
    }

    private static void setSelection(int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || binding == null || slot == lastSentSelection) return;
        if (slot < 0) ShulkerSelectionTracker.clear(client.player);
        else {
            var host = ShulkerHostResolver.resolveMenuSlot(client.player, binding.menu(), binding.slot());
            if (host.isEmpty() || !ShulkerSelectionTracker.select(client.player, host.orElseThrow(),
                    binding.locator(), slot, binding.fingerprint())) return;
        }
        lastSentSelection = slot;
        if (ClientPlayNetworking.canSend(ShulkerSelectionPayload.TYPE)) {
            ClientPlayNetworking.send(new ShulkerSelectionPayload(binding.menuId(), binding.locator(),
                    slot, binding.fingerprint()));
        }
    }

    private static void render(GuiGraphicsExtractor graphics, NonNullList<ItemStack> contents) {
        graphics.nextStratum();
        graphics.blit(RenderPipelines.GUI_TEXTURED, SHULKER_TEXTURE, geometry.x(), geometry.y(),
                0, 0, ShulkerPanelGeometry.WIDTH, ShulkerPanelGeometry.MAIN_HEIGHT, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SHULKER_TEXTURE, geometry.x(),
                geometry.y() + ShulkerPanelGeometry.MAIN_HEIGHT, 0,
                ShulkerPanelGeometry.BOTTOM_FRAME_SOURCE_Y, ShulkerPanelGeometry.WIDTH,
                ShulkerPanelGeometry.BOTTOM_FRAME_HEIGHT, 256, 256);
        graphics.text(Minecraft.getInstance().font, binding.slot().getItem().getHoverName(),
                geometry.x() + 8, geometry.y() + 6, 0x404040, false);
        if (hoveredCell >= 0) highlight(graphics, HIGHLIGHT_BACK, hoveredCell);
        List<ShulkerPanelOverlay.SlotOverlay> overlays = ShulkerPanelOverlay.plan(binding.slot().getItem(), contents);
        for (ShulkerPanelOverlay.SlotOverlay overlay : overlays) {
            int x = geometry.itemX(overlay.slot()), y = geometry.itemY(overlay.slot());
            if (!overlay.physical().isEmpty()) {
                graphics.item(overlay.physical(), x, y, x + y * ShulkerPanelGeometry.WIDTH);
                graphics.itemDecorations(Minecraft.getInstance().font, overlay.physical(), x, y);
            }
            ReservationVisualRenderer.extract(graphics, Minecraft.getInstance().font,
                    overlay.physical(), overlay.reservation(), x, y,
                    x + y * ShulkerPanelGeometry.WIDTH);
        }
        if (hoveredCell >= 0) highlight(graphics, HIGHLIGHT_FRONT, hoveredCell);
    }

    private static void highlight(GuiGraphicsExtractor graphics, Identifier sprite, int slot) {
        int itemX = geometry.itemX(slot), itemY = geometry.itemY(slot);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, itemX - 4, itemY - 4, 24, 24);
    }

    public static void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (binding == null || geometry == null || hoveredCell < 0) return;
        ItemStack physical = ShulkerContents.copy(binding.slot().getItem()).get(hoveredCell);
        if (!physical.isEmpty()) {
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, physical, mouseX, mouseY);
            return;
        }
        Optional<ItemStack> reservation = ReservationStore.getData(binding.slot().getItem()).get(hoveredCell);
        reservation.ifPresent(template -> graphics.setComponentTooltipForNextFrame(
                Minecraft.getInstance().font,
                List.of(template.getHoverName(),
                        Component.translatable("tooltip.container_slot_reservations.reserved", template.getHoverName()),
                        Component.translatable("tooltip.container_slot_reservations.empty")), mouseX, mouseY));
    }

    public static boolean suppressOuterTooltip(Slot hovered, int mouseX, int mouseY) {
        return binding != null && geometry != null
                && (hovered == binding.slot() || geometry.bounds().contains(mouseX, mouseY));
    }

    public static boolean click(double mouseX, double mouseY, int button, boolean standardClick) {
        if (binding == null || geometry == null || !geometry.bounds().contains(mouseX, mouseY)) return false;
        STATE.capturePointer();
        int slot = geometry.slot(mouseX, mouseY);
        if (standardClick && slot >= 0 && (button == 0 || button == 1)
                && ClientPlayNetworking.canSend(ShulkerPanelContentActionPayload.TYPE)) {
            ClientPlayNetworking.send(new ShulkerPanelContentActionPayload(binding.menuId(), binding.locator(), slot,
                    button == 0 ? ShulkerPanelContentActionPayload.Click.PRIMARY
                            : ShulkerPanelContentActionPayload.Click.SECONDARY,
                    binding.fingerprint()));
        }
        return true;
    }

    public static boolean ownsHoveredCell() {
        return binding != null && geometry != null && hoveredCell >= 0;
    }

    public static boolean drag(double mouseX, double mouseY) {
        return STATE.ownsDrag(binding != null && geometry != null && geometry.bounds().contains(mouseX, mouseY));
    }

    public static boolean release(double mouseX, double mouseY) {
        return STATE.releasePointer(binding != null && geometry != null
                && geometry.bounds().contains(mouseX, mouseY));
    }

    public static boolean scroll(double mouseX, double mouseY, double vertical) {
        if (binding == null || geometry == null || !(geometry.bounds().contains(mouseX, mouseY)
                || binding.hostBounds().contains(mouseX, mouseY)
                || geometry.corridorContains(binding.hostBounds(), mouseX, mouseY))) return false;
        NonNullList<ItemStack> contents = ShulkerContents.copy(binding.slot().getItem());
        int selected = selectedIndex();
        if (selected < 0) selected = ShulkerContents.lastOccupied(contents);
        else selected = vertical > 0 ? ShulkerContents.previousOccupied(contents, selected)
                : ShulkerContents.nextOccupied(contents, selected);
        setSelection(selected);
        return true;
    }

    /** null means the pointer is outside the panel grid; false is a cell-owned no-op. */
    public static Boolean reservationKey() {
        if (binding == null || geometry == null || hoveredCell < 0) return null;
        ItemStack host = binding.slot().getItem();
        NonNullList<ItemStack> contents = ShulkerContents.copy(host);
        ItemStack physical = contents.get(hoveredCell), carried = binding.menu().getCarried();
        ReservationActionPayload.Source source;
        if (!physical.isEmpty()) {
            if (!ReservationStore.getData(host).matches(hoveredCell, physical)
                    && !ReservationTemplateEligibility.allows(physical)) return false;
            source = ReservationActionPayload.Source.SLOT_STACK;
        } else if (!carried.isEmpty()) {
            if (!carried.getItem().canFitInsideContainerItems() || !ReservationTemplateEligibility.allows(carried)) return false;
            source = ReservationActionPayload.Source.CARRIED_STACK;
        } else if (ReservationStore.getData(host).get(hoveredCell).isPresent()) {
            source = ReservationActionPayload.Source.CLEAR_EMPTY;
        } else return false;
        if (!ClientPlayNetworking.canSend(ShulkerPanelReservationActionPayload.TYPE)) return false;
        ClientPlayNetworking.send(new ShulkerPanelReservationActionPayload(binding.menuId(), binding.locator(),
                hoveredCell, source, binding.fingerprint()));
        return true;
    }

    public static void acceptSync(ShulkerPanelSyncPayload payload) {
        if (binding != null && payload.menuId() == binding.menuId() && payload.host().equals(binding.locator())) {
            expectedFingerprint = payload.hostFingerprint();
            lastSentSelection = payload.selectedSlot();
        }
    }

    public static void closeScreen(AbstractContainerScreen<?> screen) {
        if (binding != null && binding.screen() == screen) {
            close(true);
        }
    }

    private static int selectedIndex() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || binding == null) return -1;
        ShulkerSelectionTracker.Selection selection = ShulkerSelectionTracker.get(client.player).orElse(null);
        return selection != null && selection.kind() == ShulkerSelectionTracker.HostKind.MENU_SLOT
                && selection.slot() == binding.slot() ? selection.internalSlot() : -1;
    }

    private static void close(boolean clearSelection) {
        if (clearSelection) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) ShulkerSelectionTracker.clear(client.player);
        }
        binding = null; geometry = null; hoveredCell = -1;
        STATE.close(); expectedFingerprint = null; lastSentSelection = Integer.MIN_VALUE;
    }

    private record Candidate(AbstractContainerScreen<?> screen, AbstractContainerMenu menu, Slot slot,
                             int menuSlot, ShulkerHostLocator locator, String fingerprint,
                             ShulkerPanelGeometry.Rect hostBounds) {}
    private record Binding(AbstractContainerScreen<?> screen, AbstractContainerMenu menu, int menuId,
                           int menuSlot, Slot slot, int containerSlot, ShulkerHostLocator locator,
                           String fingerprint, ShulkerPanelGeometry.Rect hostBounds) {
        Binding withFingerprint(String changed) {
            return new Binding(screen, menu, menuId, menuSlot, slot, containerSlot, locator, changed, hostBounds);
        }
    }
}

package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.ReservationTemplateEligibility;
import dev.resivore.slotreservations.ShulkerContents;
import dev.resivore.slotreservations.ShulkerHostFingerprint;
import dev.resivore.slotreservations.ShulkerHostResolver;
import dev.resivore.slotreservations.ShulkerSelectionTracker;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.api.client.ShulkerPanelHeaderDecorations;
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
    private static final Identifier SHULKER_TEXTURE = ShulkerPanelTextureLayout.SHULKER_TEXTURE;
    private static final Identifier HIGHLIGHT_BACK = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier HIGHLIGHT_FRONT = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static Binding binding;
    private static ShulkerPanelGeometry geometry;
    private static int hoveredCell = -1;
    private static final ShulkerPanelState STATE = new ShulkerPanelState();
    private static final SecondaryDrag SECONDARY_DRAG = new SecondaryDrag();
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
                || binding.hostBounds().contains(mouseX, mouseY));

        if (binding == null) {
            if (candidate == null) return;
            open(candidate);
        } else if (candidate != null && candidate.slot() != binding.slot() && !insideOwnedArea) {
            open(candidate);
        } else if (!refreshLiveBinding(client, screen)) {
            close(true); return;
        }

        geometry = ShulkerPanelGeometry.place(graphics.guiWidth(), graphics.guiHeight(), binding.hostBounds());
        boolean retained = binding.hostBounds().contains(mouseX, mouseY)
                || geometry.bounds().contains(mouseX, mouseY);
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
            // The exact screen/menu/Slot/container location remains live above. A server menu
            // sync (including CCAR's lock component) may legitimately replace that one host.
            // A pending CSR action still has to resolve to its exact server-issued fingerprint.
            if (expectedFingerprint != null && !currentFingerprint.equals(expectedFingerprint)) return false;
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
        ShulkerPanelTextureLayout.bottomFrameSourceY().ifPresent(sourceY ->
                graphics.blit(RenderPipelines.GUI_TEXTURED, SHULKER_TEXTURE, geometry.x(),
                        geometry.y() + ShulkerPanelGeometry.MAIN_HEIGHT, 0, sourceY,
                        ShulkerPanelGeometry.WIDTH, ShulkerPanelGeometry.BOTTOM_FRAME_HEIGHT, 256, 256));
        renderHeader(graphics);
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

    private static void renderHeader(GuiGraphicsExtractor graphics) {
        ItemStack stack = binding.slot().getItem();
        Optional<ShulkerPanelHeaderDecorations.ResolvedDecoration> resolved =
                ShulkerPanelHeaderDecorations.resolve(stack);
        Optional<ShulkerPanelHeaderDecorations.Decoration> decoration =
                resolved.map(ShulkerPanelHeaderDecorations.ResolvedDecoration::decoration);
        int decorationWidth = decoration.map(ShulkerPanelHeaderDecorations.Decoration::width).orElse(0);
        Component title = truncateTitle(stack.getHoverName(), geometry.titleWidth(decorationWidth));
        // Minecraft 26.2 GuiGraphicsExtractor text expects ARGB, including opaque alpha.
        graphics.text(Minecraft.getInstance().font, title,
                geometry.x() + ShulkerPanelGeometry.TITLE_X,
                geometry.y() + ShulkerPanelGeometry.TITLE_Y, 0xFF404040, false);
        decoration.ifPresent(value -> value.renderer().render(graphics,
                geometry.headerDecorationX(value.width()), geometry.headerDecorationY(value.height())));
    }

    private static Component truncateTitle(Component title, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(title) <= width) return title;
        String ellipsis = "…";
        int textWidth = Math.max(0, width - font.width(ellipsis));
        return Component.literal(font.plainSubstrByWidth(title.getString(), textWidth) + ellipsis)
                .withStyle(title.getStyle());
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

    public static boolean click(double mouseX, double mouseY, int button, boolean standardClick,
                                boolean shiftPrimary) {
        if (binding == null || geometry == null || !geometry.bounds().contains(mouseX, mouseY)) return false;
        if (button == 0 && standardClick && clickHeaderDecoration(mouseX, mouseY)) return true;
        int slot = geometry.slot(mouseX, mouseY);
        if (slot >= 0) {
            if (shiftPrimary) sendContent(slot, ShulkerPanelContentActionPayload.Click.QUICK_MOVE);
            else if (standardClick && (button == 0 || button == 1)) {
                if (button == 1) SECONDARY_DRAG.begin(slot);
                sendContent(slot, button == 0 ? ShulkerPanelContentActionPayload.Click.PRIMARY
                        : ShulkerPanelContentActionPayload.Click.SECONDARY);
            }
        } else if (button == 1) {
            SECONDARY_DRAG.reset();
        }
        return true;
    }

    private static boolean clickHeaderDecoration(double mouseX, double mouseY) {
        ItemStack stack = binding.slot().getItem();
        Optional<ShulkerPanelHeaderDecorations.ResolvedDecoration> resolved =
                ShulkerPanelHeaderDecorations.resolve(stack);
        if (resolved.isEmpty()) return false;
        ShulkerPanelHeaderDecorations.Decoration decoration = resolved.orElseThrow().decoration();
        // Renderers receive a header-aligned origin. The supplied CCAR art starts one pixel below it.
        ShulkerPanelGeometry.Rect bounds = new ShulkerPanelGeometry.Rect(
                geometry.headerDecorationX(decoration.width()),
                geometry.headerDecorationY(decoration.height()) + 1,
                decoration.width(), decoration.height());
        if (!bounds.contains(mouseX, mouseY)) return false;
        return resolved.orElseThrow().interaction().map(interaction -> interaction.handler().click(
                new ShulkerPanelHeaderDecorations.ClickContext(binding.menuId(), binding.menuSlot())
        )).orElse(false);
    }

    private static void sendContent(int slot, ShulkerPanelContentActionPayload.Click click) {
        if (ClientPlayNetworking.canSend(ShulkerPanelContentActionPayload.TYPE)) {
            ClientPlayNetworking.send(new ShulkerPanelContentActionPayload(binding.menuId(), binding.locator(), slot,
                    click, binding.fingerprint()));
        }
    }

    public static boolean ownsHoveredCell() {
        return binding != null && geometry != null && hoveredCell >= 0;
    }

    public static boolean drag(double mouseX, double mouseY, int button) {
        boolean insidePanel = binding != null && geometry != null && geometry.bounds().contains(mouseX, mouseY);
        if (!STATE.ownsDrag(insidePanel)) return false;
        if (button == 1) {
            int slot = geometry.slot(mouseX, mouseY);
            if (SECONDARY_DRAG.enter(slot)) sendContent(slot, ShulkerPanelContentActionPayload.Click.SECONDARY);
        }
        return true;
    }

    public static boolean release(double mouseX, double mouseY) {
        SECONDARY_DRAG.reset();
        return STATE.releasePointer(binding != null && geometry != null && geometry.bounds().contains(mouseX, mouseY));
    }

    public static boolean scroll(double mouseX, double mouseY, double vertical) {
        if (binding == null || geometry == null || !(geometry.bounds().contains(mouseX, mouseY)
                || binding.hostBounds().contains(mouseX, mouseY))) return false;
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
            // The native host-slot update may have arrived before this optional CSR metadata.
            // Treat an already-current fingerprint as acknowledged rather than leaving it as a
            // stale expectation for the next legitimate same-slot menu synchronization.
            expectedFingerprint = binding.fingerprint().equals(payload.hostFingerprint())
                    ? null : payload.hostFingerprint();
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
        binding = null; geometry = null; hoveredCell = -1; SECONDARY_DRAG.reset();
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

    /** Emits once when a right-button drag enters a cell, never once per mouse-drag event. */
    static final class SecondaryDrag {
        private int currentSlot = -1;

        void begin(int slot) { currentSlot = slot; }

        boolean enter(int slot) {
            if (slot < 0) {
                currentSlot = -1;
                return false;
            }
            if (slot == currentSlot) return false;
            currentSlot = slot;
            return true;
        }

        void reset() { currentSlot = -1; }
    }
}

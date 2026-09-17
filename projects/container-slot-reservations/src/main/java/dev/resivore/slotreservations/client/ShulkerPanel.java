package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.MouseTweaksTrace;
import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.ReservationTemplateEligibility;
import dev.resivore.slotreservations.ShulkerContents;
import dev.resivore.slotreservations.ShulkerHostFingerprint;
import dev.resivore.slotreservations.ShulkerHostResolver;
import dev.resivore.slotreservations.ShulkerSelectionTracker;
import dev.resivore.slotreservations.ShulkerTransferPlanner;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.api.client.ShulkerPanelHeaderDecorations;
import dev.resivore.slotreservations.network.ReservationActionPayload;
import dev.resivore.slotreservations.network.ShulkerHostLocator;
import dev.resivore.slotreservations.network.ShulkerPanelContentActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelMenuQuickMovePayload;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
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
    /** Client-only slots exposed through Mouse Tweaks' extension API; never menu ownership. */
    private static final SimpleContainer MOUSE_TWEAKS_VIRTUAL_CONTAINER =
            new SimpleContainer(ReservationData.SLOT_COUNT);
    private static VirtualSlot[] mouseTweaksSlots = new VirtualSlot[ReservationData.SLOT_COUNT];
    private static int mouseTweaksLeft = Integer.MIN_VALUE;
    private static int mouseTweaksTop = Integer.MIN_VALUE;
    private static String expectedFingerprint;
    private static int lastSentSelection = Integer.MIN_VALUE;
    /** Press-time Mouse Tweaks semantics plus copy-only projected content/cursor state. */
    private static final MouseTweaksRmbGesture MOUSE_TWEAKS_RMB_GESTURE = new MouseTweaksRmbGesture();
    private static ItemStack mouseTweaksShadowHost = ItemStack.EMPTY;
    private static ItemStack mouseTweaksShadowCarried = ItemStack.EMPTY;
    private static String mouseTweaksShadowFingerprint;
    /** One pre-press bridge, armed only when a carried cursor opens this panel. */
    private static boolean mouseTweaksPreGestureRetention;

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

        // A cancellable Fabric release callback may be short-circuited by an earlier
        // listener. The physical button state is the final release/cancel backstop.
        if (MOUSE_TWEAKS_RMB_GESTURE.isActive()
                && GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_RELEASE) {
            endMouseTweaksRightGesture();
        }
        geometry = ShulkerPanelGeometry.place(graphics.guiWidth(), graphics.guiHeight(), binding.hostBounds());
        positionMouseTweaksSlots(leftPos, topPos);
        // Keep one carried-cursor approach from the newly opened host to an ordinary
        // press origin, then retain only the active gesture. Release/cancel restores the
        // normal immediate host-or-panel lifetime even if items remain on the cursor.
        boolean carrying = !binding.menu().getCarried().isEmpty();
        if (!carrying) mouseTweaksPreGestureRetention = false;
        boolean mouseTweaksDepositBridge = MouseTweaksCompatibility.ownsRightDrag()
                && MOUSE_TWEAKS_RMB_GESTURE.retainPanel(mouseTweaksPreGestureRetention, carrying);
        boolean retained = binding.hostBounds().contains(mouseX, mouseY)
                || geometry.bounds().contains(mouseX, mouseY) || mouseTweaksDepositBridge;
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
        resetMouseTweaksRightGesture();
        mouseTweaksPreGestureRetention = MouseTweaksCompatibility.ownsRightDrag()
                && !candidate.menu().getCarried().isEmpty();
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

    /**
     * Observes (but never consumes) the initial RMB press for every menu coordinate.
     * Mouse Tweaks' Fabric event has already captured the same press; this records the
     * CSR-side mode before the screen body can alter a cursor or virtual-cell state.
     */
    public static void beginMouseTweaksRightGesture(AbstractContainerScreen<?> screen, Slot nativeTarget,
                                                     double mouseX, double mouseY) {
        resetMouseTweaksRightGesture();
        if (!MouseTweaksCompatibility.ownsRightDrag() || binding == null || geometry == null
                || binding.screen() != screen || binding.menu() != screen.getMenu()) {
            MouseTweaksTrace.event(2, "CSR compatibility hook declined",
                    "enabled=" + MouseTweaksCompatibility.ownsRightDrag()
                            + ", bound=" + (binding != null) + ", geometry=" + (geometry != null));
            return;
        }
        MouseTweaksTrace.event(2, "CSR compatibility hook observed", "active binding confirmed");

        Slot panelTarget = mouseTweaksSlotAt(mouseX, mouseY);
        Slot target = panelTarget != null ? panelTarget : nativeTarget;
        if (target == null || !target.isActive() || target.isFake()) {
            MouseTweaksTrace.event(3, "Initial slot unresolved",
                    target == null ? "no slot" : "inactive-or-fake slot");
            return;
        }
        ItemStack carried = binding.menu().getCarried();
        MouseTweaksTrace.event(3, "Initial slot resolved",
                "region=" + (target instanceof VirtualSlot ? "panel" : "menu")
                        + ", occupied=" + !target.getItem().isEmpty());
        MouseTweaksRmbGesture.Mode mode = MouseTweaksRmbGesture.selectMode(
                !target.getItem().isEmpty(), !carried.isEmpty());
        MouseTweaksTrace.event(4, "Gesture mode selected", mode.name());
        MouseTweaksTrace.event(5, "Initial carried stack",
                "empty=" + carried.isEmpty() + ", item=" + carried.getItem() + ", count=" + carried.getCount());
        if (mode == MouseTweaksRmbGesture.Mode.INACTIVE) return;

        boolean panelOrigin = target instanceof VirtualSlot;
        int panelCell = panelOrigin ? ((VirtualSlot) target).cell() : -1;
        MOUSE_TWEAKS_RMB_GESTURE.begin(mode,
                panelOrigin ? MouseTweaksRmbGesture.OriginRegion.PANEL
                        : MouseTweaksRmbGesture.OriginRegion.MENU,
                panelCell);
        mouseTweaksPreGestureRetention = false;
        mouseTweaksShadowHost = binding.slot().getItem().copy();
        mouseTweaksShadowCarried = carried.copy();
        mouseTweaksShadowFingerprint = binding.fingerprint();
    }

    public static boolean click(double mouseX, double mouseY, int button, boolean standardClick,
                                boolean shiftPrimary) {
        if (binding == null || geometry == null || !geometry.bounds().contains(mouseX, mouseY)) return false;
        if (button == 0 && standardClick && clickHeaderDecoration(mouseX, mouseY)) return true;
        int slot = geometry.slot(mouseX, mouseY);
        if (slot >= 0) {
            if (shiftPrimary) sendContent(slot, ShulkerPanelContentActionPayload.Click.QUICK_MOVE);
            else if (standardClick && (button == 0 || button == 1)) {
                if (button == 1) {
                    SECONDARY_DRAG.begin(slot);
                    // Replace vanilla's deferred first RMB placement when this was the
                    // press-time virtual origin. Mouse Tweaks will revisit it when leaving;
                    // the latched gesture de-duplicates that one replay by cell identity.
                    if (MOUSE_TWEAKS_RMB_GESTURE.isActive()
                            && MOUSE_TWEAKS_RMB_GESTURE.originRegion()
                            == MouseTweaksRmbGesture.OriginRegion.PANEL
                            && MOUSE_TWEAKS_RMB_GESTURE.originPanelCell() == slot) {
                        dispatchMouseTweaksSecondary(slot, "panel-press");
                        return true;
                    }
                }
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

    private static boolean sendContent(int slot, ShulkerPanelContentActionPayload.Click click) {
        return binding != null && ClientPlayNetworking.canSend(ShulkerPanelContentActionPayload.TYPE)
                && sendContent(slot, click, binding.fingerprint());
    }

    private static boolean sendContent(int slot, ShulkerPanelContentActionPayload.Click click,
                                       String fingerprint) {
        if (binding == null || fingerprint == null
                || !ClientPlayNetworking.canSend(ShulkerPanelContentActionPayload.TYPE)) return false;
        MouseTweaksTrace.event(12, "Content payload sent",
                "cell=" + slot + ", click=" + click + ", fingerprint=" + fingerprint);
        ClientPlayNetworking.send(new ShulkerPanelContentActionPayload(binding.menuId(), binding.locator(), slot,
                click, fingerprint));
        return true;
    }

    /**
     * Projects a secondary virtual-cell action on copies only, then sends the
     * pre-action fingerprint from that projection. Consecutive packets therefore
     * carry F0, F1, F2 rather than C20's stale F0 burst, while the server still
     * independently resolves, plans, and commits every real mutation.
     */
    private static void dispatchMouseTweaksSecondary(int slot, String route) {
        if (!MOUSE_TWEAKS_RMB_GESTURE.enterPanelCell(slot) || binding == null
                || mouseTweaksShadowFingerprint == null) return;
        MouseTweaksTrace.event(8, "CSR virtual cell claimed",
                "cell=" + slot + ", route=" + route);

        if (MOUSE_TWEAKS_RMB_GESTURE.takeShadowNeedsLiveCarried()) {
            // Before the first ordinary-menu -> panel transition, use the latest live
            // host and cursor after Mouse Tweaks' native origin action. Once any panel
            // payload has been sent, native clicks are integrated only as relative deltas
            // by afterMouseTweaksNativeClick and this absolute sample is never armed again.
            mouseTweaksShadowHost = binding.slot().getItem().copy();
            mouseTweaksShadowFingerprint = binding.fingerprint();
            mouseTweaksShadowCarried = binding.menu().getCarried().copy();
        }

        ItemStack changedHost;
        ItemStack changedCarried;
        ShulkerPanelContentActionPayload.Click click;
        if (MOUSE_TWEAKS_RMB_GESTURE.mode() == MouseTweaksRmbGesture.Mode.DEPOSIT) {
            // Deposit is explicit: an exhausted cursor must not turn a later occupied
            // cell into a collection/extraction just because it is currently occupied.
            if (mouseTweaksShadowCarried.isEmpty()) return;
            ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planExactInsertion(
                    mouseTweaksShadowHost, mouseTweaksShadowCarried, slot, true);
            if (plan.moved() == 0) return;
            changedHost = plan.shulker();
            changedCarried = plan.remainder();
            click = ShulkerPanelContentActionPayload.Click.SECONDARY_DEPOSIT;
        } else if (mouseTweaksShadowCarried.isEmpty()) {
            ShulkerTransferPlanner.Extraction plan = ShulkerTransferPlanner.planExtraction(
                    mouseTweaksShadowHost, slot, true, Integer.MAX_VALUE);
            if (plan.moved() == 0) return;
            changedHost = plan.shulker();
            changedCarried = plan.extracted();
            click = ShulkerPanelContentActionPayload.Click.SECONDARY;
        } else {
            // Preserve C20's generic collection/source path once it has a carried
            // stack; no later occupied/empty hover is allowed to alter the latch.
            ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planExactInsertion(
                    mouseTweaksShadowHost, mouseTweaksShadowCarried, slot, true);
            if (plan.moved() == 0) return;
            changedHost = plan.shulker();
            changedCarried = plan.remainder();
            click = ShulkerPanelContentActionPayload.Click.SECONDARY;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        MouseTweaksTrace.event(11, "Content transaction created",
                "cell=" + slot + ", mode=" + MOUSE_TWEAKS_RMB_GESTURE.mode()
                        + ", click=" + click + ", cursor=" + mouseTweaksShadowCarried.getCount()
                        + "->" + changedCarried.getCount());
        String nextFingerprint = ShulkerHostFingerprint.of(changedHost, client.player.registryAccess());
        if (!sendContent(slot, click, mouseTweaksShadowFingerprint)) return;
        mouseTweaksShadowHost = changedHost;
        mouseTweaksShadowCarried = changedCarried;
        mouseTweaksShadowFingerprint = nextFingerprint;
        MOUSE_TWEAKS_RMB_GESTURE.markPanelActionDispatched();
    }

    private static void resetMouseTweaksRightGesture() {
        MOUSE_TWEAKS_RMB_GESTURE.reset();
        mouseTweaksShadowHost = ItemStack.EMPTY;
        mouseTweaksShadowCarried = ItemStack.EMPTY;
        mouseTweaksShadowFingerprint = null;
    }

    private static void blockMouseTweaksRightGesture() {
        MOUSE_TWEAKS_RMB_GESTURE.blockUntilRelease();
        mouseTweaksShadowHost = ItemStack.EMPTY;
        mouseTweaksShadowCarried = ItemStack.EMPTY;
        mouseTweaksShadowFingerprint = null;
    }

    public static boolean ownsHoveredCell() {
        return binding != null && geometry != null && hoveredCell >= 0;
    }

    public static boolean drag(double mouseX, double mouseY, int button) {
        boolean insidePanel = binding != null && geometry != null && geometry.bounds().contains(mouseX, mouseY);
        if (button == 1 && !insidePanel) {
            MOUSE_TWEAKS_RMB_GESTURE.leavePanelCell();
            SECONDARY_DRAG.enter(-1);
        }
        if (!STATE.ownsDrag(insidePanel)) return false;
        if (button != 1) return true;
        int slot = geometry.slot(mouseX, mouseY);
        if (slot < 0) {
            MOUSE_TWEAKS_RMB_GESTURE.leavePanelCell();
            SECONDARY_DRAG.enter(-1);
            return true;
        }
        if (MOUSE_TWEAKS_RMB_GESTURE.isActive()) {
            // Mouse Tweaks 2.31 runs before this screen method. If its provider already
            // delivered MT_clickSlot, the gesture's cell-identity latch makes this a no-op.
            // If it did not, this is the one server-authoritative fallback action C21 lacked.
            dispatchMouseTweaksSecondary(slot, "post-upstream-screen-fallback");
        } else if (SECONDARY_DRAG.enter(slot)) {
            sendContent(slot, ShulkerPanelContentActionPayload.Click.SECONDARY);
        }
        return true;
    }

    public static boolean release(double mouseX, double mouseY, int button) {
        if (button == 1) {
            SECONDARY_DRAG.reset();
            endMouseTweaksRightGesture();
        }
        return STATE.releasePointer(binding != null && geometry != null && geometry.bounds().contains(mouseX, mouseY));
    }

    /** Clears the optional handoff from the screen/observer release paths. */
    public static void endMouseTweaksRightGesture() {
        resetMouseTweaksRightGesture();
        mouseTweaksPreGestureRetention = false;
        MouseTweaksTrace.event(4, "Gesture reset", "RMB release/cancel observed");
    }

    public static boolean scroll(double mouseX, double mouseY, double vertical) {
        if (binding == null || geometry == null || !(geometry.bounds().contains(mouseX, mouseY)
                || binding.hostBounds().contains(mouseX, mouseY))) return false;
        // Fabric invokes Mouse Tweaks' own scroll listener after the screen returns. Ask it
        // first here so a consumed transfer wins; cancelling then prevents a duplicate call.
        if (geometry.bounds().contains(mouseX, mouseY)
                && MouseTweaksCompatibility.consumeWheel(binding.screen(), mouseX, mouseY, vertical)) return true;
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
        boolean acceptedBinding = binding != null && payload.menuId() == binding.menuId()
                && payload.host().equals(binding.locator());
        MouseTweaksTrace.event(18, "Authoritative host/cursor sync received",
                "menu=" + payload.menuId() + ", fingerprint=" + payload.hostFingerprint()
                        + ", acceptedBinding=" + acceptedBinding
                        + ", carried=" + (binding == null ? "unbound"
                        : binding.menu().getCarried().getCount()));
        if (acceptedBinding) {
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
        resetMouseTweaksRightGesture();
        mouseTweaksPreGestureRetention = false;
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

    /** Returns native menu slots plus the live transient panel region for Mouse Tweaks only. */
    public static List<Slot> mouseTweaksSlots(List<Slot> menuSlots) {
        if (binding == null || geometry == null) return menuSlots;
        List<Slot> slots = new ArrayList<>(menuSlots.size() + mouseTweaksSlots.length);
        slots.addAll(menuSlots);
        for (VirtualSlot slot : mouseTweaksSlots) slots.add(slot);
        return List.copyOf(slots);
    }

    /** Gives Mouse Tweaks priority over the covered panel grid but never over ordinary menu slots. */
    public static Slot mouseTweaksSlotAt(double mouseX, double mouseY) {
        if (binding == null || geometry == null) return null;
        int slot = geometry.slot(mouseX, mouseY);
        return slot < 0 ? null : mouseTweaksSlots[slot];
    }

    /** Returns the CSR cell for a transient provider slot, or -1 for a native/null slot. */
    public static int mouseTweaksPanelCell(Slot slot) {
        return slot instanceof VirtualSlot virtual ? virtual.cell() : -1;
    }

    /** Routes Mouse Tweaks' supported API click back into existing fingerprint-bound CSR actions. */
    public static boolean mouseTweaksClick(Slot target, int button, ContainerInput input) {
        if (!(target instanceof VirtualSlot virtual) || binding == null) return false;
        int slot = virtual.cell();
        MouseTweaksTrace.event(10, "CSR received provider invocation",
                "cell=" + slot + ", button=" + button + ", input=" + input);
        if (input == ContainerInput.QUICK_MOVE) {
            sendContent(slot, ShulkerPanelContentActionPayload.Click.QUICK_MOVE);
            return true;
        }
        if (input != ContainerInput.PICKUP || (button != 0 && button != 1)) return true;
        if (button == 1 && MOUSE_TWEAKS_RMB_GESTURE.isActive()) {
            dispatchMouseTweaksSecondary(slot, "mouse-tweaks-provider");
            return true;
        }
        sendContent(slot, button == 0 ? ShulkerPanelContentActionPayload.Click.PRIMARY
                : ShulkerPanelContentActionPayload.Click.SECONDARY);
        return true;
    }

    /** Opaque two-phase plan used only around Mouse Tweaks' synchronous native invoker. */
    public static final class NativeClickPlan {
        private final boolean invoke;
        private final boolean project;
        private final AbstractContainerMenu menu;
        private final ItemStack liveBefore;
        private final ItemStack shadowBefore;

        private NativeClickPlan(boolean invoke, boolean project, AbstractContainerMenu menu,
                                ItemStack liveBefore, ItemStack shadowBefore) {
            this.invoke = invoke;
            this.project = project;
            this.menu = menu;
            this.liveBefore = liveBefore;
            this.shadowBefore = shadowBefore;
        }

        public boolean invoke() { return invoke; }
    }

    /**
     * Validates a native Mouse Tweaks action against the latched mode before it reaches
     * the menu. After the first panel payload, only a provable place-one action may run.
     */
    public static NativeClickPlan beforeMouseTweaksNativeClick(
            Slot target, int button, ContainerInput input) {
        NativeClickPlan passThrough = new NativeClickPlan(true, false, null, null, null);
        if (target == null || target instanceof VirtualSlot || button != 1
                || input != ContainerInput.PICKUP) return passThrough;
        MOUSE_TWEAKS_RMB_GESTURE.leavePanelCell();
        if (!MOUSE_TWEAKS_RMB_GESTURE.isActive() || binding == null) return passThrough;
        if (MOUSE_TWEAKS_RMB_GESTURE.isBlockedUntilRelease()) {
            MouseTweaksTrace.event(10, "Native RMB action suppressed",
                    "gesture is blocked until release");
            return new NativeClickPlan(false, false, null, null, null);
        }

        boolean boundHost = target == binding.slot()
                || (target.container == binding.slot().container
                && target.getContainerSlot() == binding.containerSlot());
        if (boundHost) {
            boolean pendingProjection = MOUSE_TWEAKS_RMB_GESTURE.hasDispatchedPanelAction();
            blockMouseTweaksRightGesture();
            MouseTweaksTrace.event(10, pendingProjection ? "Projected host click suppressed"
                    : "Native host click blocked gesture until release", "slot=" + target.index);
            return new NativeClickPlan(!pendingProjection, false, null, null, null);
        }

        // Before the first panel action, Mouse Tweaks' native origin remains authoritative
        // client prediction. The first panel entry samples its resulting live cursor once.
        if (!MOUSE_TWEAKS_RMB_GESTURE.hasDispatchedPanelAction()) {
            MOUSE_TWEAKS_RMB_GESTURE.markUnprojectedNativeBoundary();
            return passThrough;
        }

        ItemStack liveBefore = binding.menu().getCarried().copy();
        if (mouseTweaksShadowCarried.isEmpty()) {
            MouseTweaksTrace.event(10, "Native RMB action suppressed",
                    "projected cursor exhausted; live=" + liveBefore.getCount());
            return new NativeClickPlan(false, false, null, null, null);
        }
        if (!MouseTweaksRmbGesture.canProjectNativePlaceOne(
                mouseTweaksShadowCarried, liveBefore, target)) {
            int projectedBefore = mouseTweaksShadowCarried.getCount();
            boolean identityUnsafe = liveBefore.isEmpty()
                    || liveBefore.getItem() instanceof BundleItem
                    || !ItemStack.isSameItemSameComponents(mouseTweaksShadowCarried, liveBefore)
                    || liveBefore.getCount() < mouseTweaksShadowCarried.getCount();
            if (identityUnsafe) blockMouseTweaksRightGesture();
            MouseTweaksTrace.event(10, "Native RMB action suppressed",
                    "unsafe projection; slot=" + target.index + ", live=" + liveBefore.getCount()
                            + ", projected=" + projectedBefore + ", blocked=" + identityUnsafe);
            return new NativeClickPlan(false, false, null, null, null);
        }
        MouseTweaksTrace.event(10, "Native RMB place-one allowed",
                "slot=" + target.index + ", live=" + liveBefore.getCount()
                        + ", projected=" + mouseTweaksShadowCarried.getCount());
        return new NativeClickPlan(true, true, binding.menu(), liveBefore,
                mouseTweaksShadowCarried.copy());
    }

    /** Applies a successful native click's relative cursor delta without absolute rebasing. */
    public static void afterMouseTweaksNativeClick(NativeClickPlan plan, boolean completed) {
        if (plan == null || !plan.project) return;
        boolean current = completed && binding != null && binding.menu() == plan.menu
                && MOUSE_TWEAKS_RMB_GESTURE.isActive()
                && ItemStack.matches(mouseTweaksShadowCarried, plan.shadowBefore);
        ItemStack liveAfter = current ? plan.menu.getCarried().copy() : ItemStack.EMPTY;
        if (!current || !MouseTweaksRmbGesture.applyNativeCursorDelta(
                mouseTweaksShadowCarried, plan.liveBefore, liveAfter)) {
            MouseTweaksTrace.event(11, "Native cursor delta rejected",
                    "completed=" + completed + ", current=" + current);
            blockMouseTweaksRightGesture();
            return;
        }
        MouseTweaksTrace.event(11, "Native cursor delta projected",
                "live=" + plan.liveBefore.getCount() + "->" + liveAfter.getCount()
                        + ", projected=" + plan.shadowBefore.getCount() + "->"
                        + mouseTweaksShadowCarried.getCount());
    }

    /**
     * Lets a Mouse Tweaks shift-drag from a real player-inventory slot target the virtual
     * region. Vanilla quick move cannot see this panel, so the server performs the exact
     * reservation-aware insertion after resolving the live menu source and host fingerprint.
     */
    public static boolean mouseTweaksQuickMoveFromMenuSlot(Slot source) {
        if (binding == null || source == null || source == binding.slot() || source.isFake()
                || !source.isActive() || binding.menu().slots.indexOf(source) != source.index) return false;
        if (!ClientPlayNetworking.canSend(ShulkerPanelMenuQuickMovePayload.TYPE)) return false;
        ClientPlayNetworking.send(new ShulkerPanelMenuQuickMovePayload(binding.menuId(), binding.locator(),
                source.index, binding.fingerprint()));
        return true;
    }

    private static void positionMouseTweaksSlots(int leftPos, int topPos) {
        if (mouseTweaksLeft == leftPos && mouseTweaksTop == topPos) return;
        VirtualSlot[] positioned = new VirtualSlot[ReservationData.SLOT_COUNT];
        for (int cell = 0; cell < positioned.length; cell++) {
            positioned[cell] = new VirtualSlot(cell, geometry.itemX(cell) - leftPos,
                    geometry.itemY(cell) - topPos);
        }
        mouseTweaksSlots = positioned;
        mouseTweaksLeft = leftPos;
        mouseTweaksTop = topPos;
    }

    /** Transient read-only view; all mutations continue through CSR server payloads. */
    private static final class VirtualSlot extends Slot {
        private final int cell;

        private VirtualSlot(int cell, int x, int y) {
            super(MOUSE_TWEAKS_VIRTUAL_CONTAINER, -(cell + 1), x, y);
            this.cell = cell;
        }

        private int cell() { return cell; }

        @Override public ItemStack getItem() {
            if (binding == null) return ItemStack.EMPTY;
            return ShulkerContents.copy(binding.slot().getItem()).get(cell).copy();
        }

        @Override public boolean mayPlace(ItemStack stack) {
            if (binding == null || stack.isEmpty() || !stack.getItem().canFitInsideContainerItems()) return false;
            return switch (dev.resivore.slotreservations.api.ContainerSlotReservationsApi
                    .classify(binding.slot().getItem(), cell, stack)) {
                case OCCUPIED_COMPATIBLE, RESERVED_MATCH, UNRESERVED_EMPTY -> true;
                default -> false;
            };
        }

        @Override public int getMaxStackSize(ItemStack stack) {
            return stack.getMaxStackSize();
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

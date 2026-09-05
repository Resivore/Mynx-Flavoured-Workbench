package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.*;
import dev.resivore.slotreservations.network.NestedReservationActionPayload;
import dev.resivore.slotreservations.network.ReservationActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** One visible-frame hit record, bound to one exact menu slot; no inventory equality search. */
public final class NestedTooltipEditor {
    private static long frame;
    private static Hit hit, previous;
    private static Binding renderingHost;
    private static ItemStack expectedReply;
    private static long sentFrame;
    private NestedTooltipEditor() {}

    public record Binding(AbstractContainerScreen<?> screen, AbstractContainerMenu menu, int menuId,
                          int menuSlot, Slot slot, int menuState, ItemStack identity,
                          int anchorX, int anchorY, long frame) {
        public boolean live() {
            var client = Minecraft.getInstance();
            return client.gui.screen() == screen && client.player != null
                    && client.player.containerMenu == menu && screen.getMenu() == menu
                    && menu.containerId == menuId && menu.getStateId() == menuState
                    && menuSlot >= 0 && menuSlot < menu.slots.size() && slot.index == menuSlot
                    && menu.slots.get(menuSlot) == slot && slot.isActive() && !slot.isFake()
                    && slot.getItem().getCount() == 1
                    && SupportedContainerResolver.isSupportedShulkerItem(slot.getItem())
                    && ItemStack.isSameItemSameComponents(slot.getItem(), identity);
        }
        Binding refresh(ItemStack stack) {
            return new Binding(screen, menu, menuId, menuSlot, slot, menu.getStateId(), stack.copy(),
                    anchorX, anchorY, NestedTooltipEditor.frame);
        }
    }
    private record Hit(long frame, Object tooltip, Binding host, TooltipGrid grid,
                       double mouseX, double mouseY, int width, int height, int menuState) {
        boolean current(Object activeTooltip) {
            var client = Minecraft.getInstance();
            return new TooltipHitState(frame, tooltip, host, host.screen(), mouseX, mouseY).matches(
                    NestedTooltipEditor.frame, activeTooltip, host, client.gui.screen(),
                    client.mouseHandler.getScaledXPos(client.getWindow()), client.mouseHandler.getScaledYPos(client.getWindow()))
                    && host.live();
        }
    }

    public static void beginFrame() {
        previous = hit;
        frame++;
        if (previous != null && expectedReply != null) {
            Binding old = previous.host();
            if (ItemStack.matches(old.slot().getItem(), expectedReply)) {
                Binding refreshed = old.refresh(expectedReply);
                previous = new Hit(previous.frame(), previous.tooltip(), refreshed, previous.grid(),
                        previous.mouseX(), previous.mouseY(), previous.width(), previous.height(), old.screen().getMenu().getStateId());
                expectedReply = null;
            } else if (frame - sentFrame > 20) expectedReply = null;
        }
        hit = null;
        renderingHost = null;
    }
    public static void endFrame() {
        renderingHost = null;
        if (hit == null) { previous = null; expectedReply = null; }
    }
    /** Scope only the exact outer slot scheduling this tooltip, including repeated image queries. */
    public static void schedule(AbstractContainerScreen<?> screen, Slot slot, int x, int y, Runnable nativeSchedule) {
        Binding host = null;
        if (slot != null) {
            var menu = screen.getMenu();
            host = new Binding(screen, menu, menu.containerId, slot.index, slot, menu.getStateId(),
                    slot.getItem().copy(), x, y, frame);
            if (!host.live()) host = null;
        }
        withHost(host, nativeSchedule);
    }
    private static void withHost(Binding host, Runnable nativeSchedule) {
        Binding outer = renderingHost;
        renderingHost = host;
        try { nativeSchedule.run(); }
        finally { renderingHost = outer; }
    }
    public static Binding capture(ItemStack stack) {
        return renderingHost != null && renderingHost.frame() == frame && renderingHost.live()
                && ItemStack.matches(renderingHost.identity(), stack) ? renderingHost : null;
    }
    public static void show(Object tooltip, Binding host, int gridX, int gridY, GuiGraphicsExtractor graphics) {
        if (host == null || host.frame() != frame || !host.live()) return;
        var client = Minecraft.getInstance();
        hit = new Hit(frame, tooltip, host, new TooltipGrid(gridX, gridY),
                client.mouseHandler.getScaledXPos(client.getWindow()), client.mouseHandler.getScaledYPos(client.getWindow()),
                graphics.guiWidth(), graphics.guiHeight(), host.screen().getMenu().getStateId());
    }
    public static int hovered(Object tooltip) {
        return hit != null && hit.current(tooltip)
                ? hit.grid().slot(hit.mouseX(), hit.mouseY()) : -1;
    }
    /** Retain the native preview while crossing its border or editing inside it. */
    public static boolean retainPreview(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                                        int mouseX, int mouseY) {
        if (previous == null || previous.frame() != frame - 1 || previous.host().screen() != screen
                || !previous.host().live() || previous.width() != graphics.guiWidth()
                || previous.height() != graphics.guiHeight()) return false;
        Binding host = previous.host();
        TooltipGrid grid = previous.grid();
        Slot outer = ((ReservationScreenAccess) screen).containerSlotReservations$getHoveredSlot();
        if (outer != null && outer != host.slot() && grid.slot(mouseX, mouseY) < 0) return false;
        // Include the small native gap from the host hover point to its tooltip border.
        int left = Math.min(host.anchorX() - 2, grid.x() - 7);
        int right = Math.max(host.anchorX() + 2, grid.x() + 169);
        int top = Math.min(host.anchorY() - 2, grid.y() - 7);
        int bottom = Math.max(host.anchorY() + 2, grid.y() + 61);
        if (mouseX < left || mouseX >= right || mouseY < top || mouseY >= bottom) return false;
        withHost(host.refresh(host.slot().getItem()), () -> graphics.setTooltipForNextFrame(
                Minecraft.getInstance().font, host.slot().getItem(), host.anchorX(), host.anchorY()));
        return true;
    }
    /** null = outside the current valid grid; false = valid grid with no action. */
    public static Boolean send() {
        var client = Minecraft.getInstance();
        Hit current = hit;
        if (current == null || !current.current(current.tooltip())) return null;
        int nested = current.grid().slot(current.mouseX(), current.mouseY());
        if (nested < 0) return null;
        if (current.menuState() != current.host().screen().getMenu().getStateId()) return false;
        if (expectedReply != null || !ClientPlayNetworking.canSend(NestedReservationActionPayload.TYPE)) return false;
        var menu = current.host().screen().getMenu();
        ItemStack host = current.host().slot().getItem();
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        host.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        ItemStack physical = contents.get(nested), carried = menu.getCarried();
        ReservationActionPayload.Source source;
        if (!physical.isEmpty()) {
            if (!ReservationStore.getData(host).matches(nested, physical)
                    && !ReservationTemplateEligibility.allows(physical)) return false;
            source = ReservationActionPayload.Source.SLOT_STACK;
        } else if (!carried.isEmpty()) {
            if (!carried.getItem().canFitInsideContainerItems() || !ReservationTemplateEligibility.allows(carried)) return false;
            source = ReservationActionPayload.Source.CARRIED_STACK;
        } else if (ReservationStore.getData(host).get(nested).isPresent()) {
            source = ReservationActionPayload.Source.CLEAR_EMPTY;
        } else return false;
        ClientPlayNetworking.send(new NestedReservationActionPayload(menu.containerId, menu.getStateId(),
                current.host().slot().index, nested, source, ShulkerHostFingerprint.of(host, client.player.registryAccess())));
        // Predict only the expected acknowledgement identity, never mutate the client-owned stack.
        var data = ReservationStore.getData(host);
        data = source == ReservationActionPayload.Source.CLEAR_EMPTY
                || source == ReservationActionPayload.Source.SLOT_STACK && data.matches(nested, physical)
                ? data.without(nested) : data.with(nested, physical.isEmpty() ? carried : physical);
        expectedReply = host.copy();
        ReservationStore.setData(expectedReply, data);
        sentFrame = frame;
        return true;
    }
}

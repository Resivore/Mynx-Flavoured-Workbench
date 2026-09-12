package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.ClientReservationState;
import dev.resivore.slotreservations.client.ReservationVisualRenderer;
import dev.resivore.slotreservations.client.ReservationScreenAccess;
import dev.resivore.slotreservations.client.ShulkerPanel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin implements ReservationScreenAccess {
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected Slot hoveredSlot;
    @Shadow @Final protected int imageWidth;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Override
    public Slot containerSlotReservations$getHoveredSlot() {
        return hoveredSlot;
    }

    @Inject(
            method = "extractCarriedItem(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            at = @At("HEAD"),
            require = 1
    )
    private void containerSlotReservations$extractPinnedShulkerPanel(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo callbackInfo) {
        ShulkerPanel.updateAndRender((AbstractContainerScreen<?>) (Object) this, hoveredSlot,
                graphics, mouseX, mouseY, leftPos, topPos, imageWidth);
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void containerSlotReservations$extractReservationVisual(
            GuiGraphicsExtractor graphics,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo callbackInfo
    ) {
        Optional<ItemStack> reservation = ClientReservationState.template(menu, slot);
        if (reservation.isEmpty()) return;
        ReservationVisualRenderer.extract(
                graphics,
                Minecraft.getInstance().font,
                slot.getItem(),
                reservation,
                slot.x,
                slot.y,
                slot.x + slot.y * imageWidth
        );
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void containerSlotReservations$extractEmptyReservationTooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CallbackInfo callbackInfo
    ) {
        ShulkerPanel.extractTooltip(graphics, mouseX, mouseY);
        if (ShulkerPanel.suppressOuterTooltip(hoveredSlot, mouseX, mouseY)) {
            callbackInfo.cancel(); return;
        }
        if (hoveredSlot == null || !hoveredSlot.getItem().isEmpty()) return;
        Optional<ItemStack> reservation = ClientReservationState.template(menu, hoveredSlot);
        if (reservation.isEmpty()) return;

        graphics.setComponentTooltipForNextFrame(
                Minecraft.getInstance().font,
                List.of(
                        Component.translatable(
                                "tooltip.container_slot_reservations.reserved",
                                reservation.orElseThrow().getHoverName()).withStyle(ChatFormatting.AQUA),
                        Component.translatable("tooltip.container_slot_reservations.empty")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                ),
                mouseX,
                mouseY
        );
        callbackInfo.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void containerSlotReservations$panelClick(MouseButtonEvent event, boolean doubleClick,
                                                       CallbackInfoReturnable<Boolean> callbackInfo) {
        boolean standardClick = !doubleClick && !event.hasShiftDown()
                && !event.hasControlDown() && !event.hasAltDown();
        boolean shiftPrimary = !doubleClick && event.hasShiftDown() && !event.hasControlDown()
                && !event.hasAltDown() && event.button() == 0;
        if (ShulkerPanel.click(event.x(), event.y(), event.button(), standardClick, shiftPrimary)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "checkHotbarKeyPressed", at = @At("HEAD"), cancellable = true)
    private void containerSlotReservations$blockPanelHotbarSwap(
            KeyEvent event, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (ShulkerPanel.ownsHoveredCell() && event.getDigit() >= 0) callbackInfo.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void containerSlotReservations$panelDrag(MouseButtonEvent event, double dragX, double dragY,
                                                      CallbackInfoReturnable<Boolean> callbackInfo) {
        if (ShulkerPanel.drag(event.x(), event.y(), event.button())) callbackInfo.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void containerSlotReservations$panelRelease(MouseButtonEvent event,
                                                         CallbackInfoReturnable<Boolean> callbackInfo) {
        if (ShulkerPanel.release(event.x(), event.y())) callbackInfo.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void containerSlotReservations$panelScroll(double mouseX, double mouseY,
                                                        double horizontal, double vertical,
                                                        CallbackInfoReturnable<Boolean> callbackInfo) {
        if (ShulkerPanel.scroll(mouseX, mouseY, vertical)) callbackInfo.setReturnValue(true);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void containerSlotReservations$closePanel(CallbackInfo callbackInfo) {
        ShulkerPanel.closeScreen((AbstractContainerScreen<?>) (Object) this);
    }
}

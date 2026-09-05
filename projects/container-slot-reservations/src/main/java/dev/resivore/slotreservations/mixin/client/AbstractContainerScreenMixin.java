package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.ClientReservationState;
import dev.resivore.slotreservations.client.ReservationVisualRenderer;
import dev.resivore.slotreservations.client.ReservationScreenAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

import java.util.List;
import java.util.Optional;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin implements ReservationScreenAccess {
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected Slot hoveredSlot;
    @Shadow @Final protected int imageWidth;

    @Override
    public Slot containerSlotReservations$getHoveredSlot() {
        return hoveredSlot;
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
        if (dev.resivore.slotreservations.client.NestedTooltipEditor.retainPreview(
                (AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY)) {
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
}

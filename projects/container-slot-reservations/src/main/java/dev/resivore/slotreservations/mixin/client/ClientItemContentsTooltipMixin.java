package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.ReservationVisualRenderer;
import dev.resivore.slotreservations.client.ShulkerTooltipOverlay;
import dev.resivore.slotreservations.client.TooltipSourceAccess;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/** Slot-local overlay for Item Interactions 26.2.2; absent targets are safely ignored. */
@Pseudo
@Mixin(
        targets = "fuzs.iteminteractions.common.api.v2.client.gui.screens.inventory.tooltip.ClientItemContentsTooltip",
        remap = false,
        priority = 900
)
abstract class ClientItemContentsTooltipMixin {
    @Shadow @Final private NonNullList<ItemStack> itemList;
    @Shadow @Final private int gridWidth;
    @Shadow @Final private int gridHeight;

    @Unique
    private ItemStack containerSlotReservations$sourceStack;

    @Inject(
            method = "<init>(Lfuzs/iteminteractions/common/api/v2/world/inventory/tooltip/ItemContentsTooltip;)V",
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private void containerSlotReservations$captureSource(
            @Coerce Object originalTooltip,
            CallbackInfo callbackInfo
    ) {
        if (originalTooltip instanceof TooltipSourceAccess access) {
            ItemStack sourceStack = access.containerSlotReservations$getSourceStack();
            if (!sourceStack.isEmpty()) {
                containerSlotReservations$sourceStack = sourceStack.copy();
            }
        }
    }

    @Inject(
            method = "extractSlot(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphicsExtractor;III)V",
            at = @At("TAIL"),
            require = 1,
            remap = false
    )
    private void containerSlotReservations$extractReservationOverlay(
            Font font,
            GuiGraphicsExtractor graphics,
            int slotX,
            int slotY,
            int physicalSlot,
            CallbackInfo callbackInfo
    ) {
        if (containerSlotReservations$sourceStack == null
                || gridWidth != ShulkerTooltipOverlay.GRID_WIDTH
                || gridHeight != ShulkerTooltipOverlay.GRID_HEIGHT) {
            return;
        }

        ShulkerTooltipOverlay.at(
                containerSlotReservations$sourceStack,
                itemList,
                physicalSlot
        ).ifPresent(overlay -> ReservationVisualRenderer.extract(
                graphics,
                font,
                itemList.get(physicalSlot),
                Optional.of(overlay.template()),
                slotX + 1,
                slotY + 1,
                physicalSlot
        ));
    }
}

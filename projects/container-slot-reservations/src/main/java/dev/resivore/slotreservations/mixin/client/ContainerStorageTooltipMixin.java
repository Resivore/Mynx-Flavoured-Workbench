package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.EasyShulkerTooltipCompat;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Captures Item Interactions' source stack at its exact tooltip-component factory seam. */
@Pseudo
@Mixin(
        targets = "fuzs.iteminteractions.common.api.v2.world.item.storage.ContainerStorage",
        remap = false,
        priority = 900
)
abstract class ContainerStorageTooltipMixin {
    @Inject(
            method = "createTooltipImageComponent(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/NonNullList;)Lnet/minecraft/world/inventory/tooltip/TooltipComponent;",
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private void containerSlotReservations$captureTooltipSource(
            ItemStack sourceStack,
            Player player,
            NonNullList<ItemStack> physicalSlots,
            CallbackInfoReturnable<TooltipComponent> callbackInfo
    ) {
        EasyShulkerTooltipCompat.captureSource(
                sourceStack,
                Optional.ofNullable(callbackInfo.getReturnValue())
        );
    }
}

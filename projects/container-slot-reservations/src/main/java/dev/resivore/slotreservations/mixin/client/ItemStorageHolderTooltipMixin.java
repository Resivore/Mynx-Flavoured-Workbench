package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.EasyShulkerTooltipCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only the tooltip's physical-content gate changes; bars and interactions keep hasContents. */
@Pseudo
@Mixin(targets = "fuzs.iteminteractions.common.api.v2.world.item.storage.ItemStorageHolder", remap = false)
abstract class ItemStorageHolderTooltipMixin {
    @Shadow public abstract boolean hasContents(ItemStack stack, Player player);

    @Redirect(
            method = "getTooltipImage(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;",
            at = @At(value = "INVOKE", target = "Lfuzs/iteminteractions/common/api/v2/world/item/storage/ItemStorageHolder;hasContents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z"),
            require = 1, allow = 1, remap = false)
    private boolean containerSlotReservations$includeReservedEmpty(
            @Coerce Object holder, ItemStack stack, Player player) {
        return hasContents(stack, player)
                || EasyShulkerTooltipCompat.hasEmptyReservationTooltip(holder, stack);
    }
}

package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.CarriedShulkerInsertionPolicy;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reservation-aware destination filtering for Item Interactions' carried-container writer. */
@Pseudo
@Mixin(
        targets = "fuzs.iteminteractions.common.impl.world.item.container.ItemStackingContext",
        remap = false,
        priority = 900
)
abstract class ItemStackingContextMixin {
    @Unique
    private ItemStack containerSlotReservations$sourceShulker = ItemStack.EMPTY;

    @Inject(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I",
            at = @At("HEAD"),
            require = 1,
            remap = false
    )
    private void containerSlotReservations$captureSource(
            ItemStack sourceShulker,
            ItemStack incoming,
            int prioritizedSlot,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        containerSlotReservations$sourceShulker = sourceShulker;
    }

    @Redirect(
            method = "moveItemToOccupiedSlotsWithSameType(Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;I)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/ints/IntSet;toIntArray()[I"
            ),
            require = 1,
            expect = 1,
            allow = 1,
            remap = false
    )
    private int[] containerSlotReservations$filterOccupiedCandidates(
            IntSet candidates,
            Container liveContents,
            ItemStack incoming,
            int prioritizedSlot
    ) {
        return CarriedShulkerInsertionPolicy.occupiedCandidates(
                containerSlotReservations$sourceShulker,
                liveContents,
                incoming,
                candidates.toIntArray()
        );
    }

    @Redirect(
            method = "moveItemToEmptySlots(Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;I)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/ints/IntSet;toIntArray()[I"
            ),
            require = 1,
            expect = 1,
            allow = 1,
            remap = false
    )
    private int[] containerSlotReservations$orderEmptyCandidates(
            IntSet candidates,
            Container liveContents,
            ItemStack incoming,
            int prioritizedSlot
    ) {
        return CarriedShulkerInsertionPolicy.emptyCandidates(
                containerSlotReservations$sourceShulker,
                liveContents,
                incoming,
                candidates.toIntArray()
        );
    }

    @Inject(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I",
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private void containerSlotReservations$clearSource(
            ItemStack sourceShulker,
            ItemStack incoming,
            int prioritizedSlot,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        containerSlotReservations$sourceShulker = ItemStack.EMPTY;
    }
}

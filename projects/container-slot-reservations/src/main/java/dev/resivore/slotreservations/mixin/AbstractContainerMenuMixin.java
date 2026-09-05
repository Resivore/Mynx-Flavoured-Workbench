package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.ShulkerContextualTransfers;
import dev.resivore.slotreservations.ShulkerSelectionTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractContainerMenu.class, priority = 2000)
abstract class AbstractContainerMenuMixin {
    @Unique private ShulkerSelectionTracker.ClickMigration containerSlotReservations$migration;

    @Inject(method = "tryItemClickBehaviourOverride", at = @At("HEAD"), cancellable = true)
    private void containerSlotReservations$ownRecognizedShulkerSecondaryClick(
            Player player, ClickAction action, Slot slot, ItemStack slotStack, ItemStack carried,
            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (ShulkerContextualTransfers.handle(player, (AbstractContainerMenu) (Object) this,
                action, slot, slotStack, carried)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "clicked", at = @At("HEAD"))
    private void containerSlotReservations$beginSelectionMigration(
            int slotId, int button, ContainerInput input, Player player, CallbackInfo callbackInfo) {
        containerSlotReservations$migration = ShulkerSelectionTracker.beforeClick(
                player, (AbstractContainerMenu) (Object) this, slotId, button, input);
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void containerSlotReservations$finishSelectionMigration(
            int slotId, int button, ContainerInput input, Player player, CallbackInfo callbackInfo) {
        ShulkerSelectionTracker.afterClick(player, containerSlotReservations$migration);
        containerSlotReservations$migration = null;
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void containerSlotReservations$clearSelection(Player player, CallbackInfo callbackInfo) {
        ShulkerSelectionTracker.clearForMenu(player, (AbstractContainerMenu) (Object) this);
    }
}

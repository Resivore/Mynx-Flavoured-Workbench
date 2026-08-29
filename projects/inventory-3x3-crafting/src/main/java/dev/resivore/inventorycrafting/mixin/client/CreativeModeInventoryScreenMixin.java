package dev.resivore.inventorycrafting.mixin.client;

import dev.resivore.inventorycrafting.InventoryCraftingLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true, require = 1)
    private void inventory3x3$rejectDirectCraftingWrapperMutation(
            Slot slot,
            int slotId,
            int button,
            ContainerInput input,
            CallbackInfo ci
    ) {
        if (inventory3x3$isCraftContainerView(slot)) {
            ci.cancel();
        }
    }

    @ModifyArgs(
            method = "selectTab",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"
            ),
            require = 1
    )
    private void inventory3x3$hideCraftingWrappersByIdentity(Args args) {
        Slot target = args.get(0);
        if (inventory3x3$isCraftingInput(target)) {
            args.set(2, -2000);
            args.set(3, -2000);
        }
    }

    @Redirect(
            method = "slotClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;set(Lnet/minecraft/world/item/ItemStack;)V"
            ),
            require = 1
    )
    private void inventory3x3$excludeCraftInputsFromBulkClear(Slot slot, ItemStack replacement) {
        if (!inventory3x3$isCraftingInput(slot)) {
            slot.set(replacement);
        }
    }

    @Redirect(
            method = "slotClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleCreativeModeItemAdd(Lnet/minecraft/world/item/ItemStack;I)V"
            ),
            require = 3,
            expect = 3
    )
    private void inventory3x3$excludeCraftInputsFromCreativeWrites(
            MultiPlayerGameMode gameMode,
            ItemStack stack,
            int slotIndex
    ) {
        if (!inventory3x3$isCraftingMenuIndex(slotIndex)) {
            gameMode.handleCreativeModeItemAdd(stack, slotIndex);
        }
    }

    @Unique
    private static boolean inventory3x3$isCraftingInput(Slot slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        InventoryMenu menu = minecraft.player.inventoryMenu;
        return InventoryCraftingLayout.isCraftingInput(menu, slot);
    }

    @Unique
    private static boolean inventory3x3$isCraftContainerView(Slot slot) {
        Minecraft minecraft = Minecraft.getInstance();
        return slot != null
                && minecraft.player != null
                && slot.container == minecraft.player.inventoryMenu.getCraftSlots();
    }

    @Unique
    private static boolean inventory3x3$isCraftingMenuIndex(int slotIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        InventoryMenu menu = minecraft.player.inventoryMenu;
        return slotIndex >= 0
                && slotIndex < menu.slots.size()
                && InventoryCraftingLayout.isCraftingInput(menu, menu.slots.get(slotIndex));
    }
}

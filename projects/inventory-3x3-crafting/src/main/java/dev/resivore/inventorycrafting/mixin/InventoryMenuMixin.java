package dev.resivore.inventorycrafting.mixin;

import dev.resivore.inventorycrafting.Inventory3x3Crafting;
import dev.resivore.inventorycrafting.InventoryCraftingLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

/**
 * Priority 250 is intentionally between Simple Trash Slot (1) and Trinkets
 * (500). The callback fails closed unless Trash has already produced menu 73
 * and Trinkets has not yet claimed its dynamic suffix.
 */
@Mixin(value = InventoryMenu.class, priority = 250)
abstract class InventoryMenuMixin extends AbstractCraftingMenu {
    protected InventoryMenuMixin() {
        super(null, 0, InventoryCraftingLayout.GRID_WIDTH, InventoryCraftingLayout.GRID_HEIGHT);
    }

    @ModifyConstant(
            method = "<init>",
            constant = @Constant(intValue = 2),
            require = 2,
            expect = 2
    )
    private static int inventory3x3$constructNineCellContainer(int originalDimension) {
        return InventoryCraftingLayout.GRID_WIDTH;
    }

    @ModifyArgs(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/InventoryMenu;addResultSlot(Lnet/minecraft/world/entity/player/Player;II)Lnet/minecraft/world/inventory/Slot;"
            ),
            require = 1
    )
    private void inventory3x3$positionResult(Args args) {
        args.set(1, InventoryCraftingLayout.RESULT_X);
        args.set(2, InventoryCraftingLayout.RESULT_Y);
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/InventoryMenu;addCraftingGridSlots(II)V"
            ),
            require = 1
    )
    private void inventory3x3$emitLegacyViews(InventoryMenu menu, int ignoredLeft, int ignoredTop) {
        for (int logicalCell : InventoryCraftingLayout.LEGACY_LOGICAL_CELLS) {
            this.addSlot(new Slot(
                    this.craftSlots,
                    logicalCell,
                    InventoryCraftingLayout.gridX(logicalCell),
                    InventoryCraftingLayout.gridY(logicalCell)
            ));
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void inventory3x3$appendViewsAfterTrashBeforeTrinkets(
            Inventory inventory,
            boolean active,
            Player owner,
            CallbackInfo ci
    ) {
        InventoryMenu menu = (InventoryMenu) (Object) this;
        InventoryCraftingLayout.verifyPreAppend(menu, inventory);
        inventory3x3$verifySnapshotSizes(menu.slots.size());

        for (int logicalCell : InventoryCraftingLayout.APPENDED_LOGICAL_CELLS) {
            this.addSlot(new Slot(
                    this.craftSlots,
                    logicalCell,
                    InventoryCraftingLayout.gridX(logicalCell),
                    InventoryCraftingLayout.gridY(logicalCell)
            ));
        }

        InventoryCraftingLayout.verifyPostAppend(menu);
        inventory3x3$verifySnapshotSizes(menu.slots.size());
        Inventory3x3Crafting.LOGGER.debug(
                "Constructed audited InventoryMenu prefix: 79 slots, row-major crafting IDs {}",
                InventoryCraftingLayout.EXPECTED_ROW_MAJOR_MENU_IDS
        );
    }

    @Inject(method = "getInputGridSlots", at = @At("HEAD"), cancellable = true, require = 1)
    private void inventory3x3$returnRowMajorViews(CallbackInfoReturnable<List<Slot>> cir) {
        cir.setReturnValue(InventoryCraftingLayout.orderedCraftingSlots((InventoryMenu) (Object) this));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, require = 1)
    private void inventory3x3$routeEveryCraftingSourceToOrdinaryInventory(
            Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        InventoryMenu menu = (InventoryMenu) (Object) this;
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.slots.get(slotIndex);
        if (!InventoryCraftingLayout.isCraftingInput(menu, slot)) {
            return;
        }

        ItemStack clicked = ItemStack.EMPTY;
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            int ordinaryEnd = InventoryCraftingLayout.ORDINARY_MENU_START
                    + player.getInventory().getNonEquipmentItems().size();
            if (!this.moveItemStackTo(
                    stack,
                    InventoryCraftingLayout.ORDINARY_MENU_START,
                    ordinaryEnd,
                    false
            )) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, clicked);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }

            slot.onTake(player, stack);
        }

        cir.setReturnValue(clicked);
    }

    private void inventory3x3$verifySnapshotSizes(int expected) {
        AbstractContainerMenuSnapshotAccessor snapshots = (AbstractContainerMenuSnapshotAccessor) this;
        int lastSize = snapshots.inventory3x3$getLastSlots().size();
        int remoteSize = snapshots.inventory3x3$getRemoteSlots().size();
        if (lastSize != expected || remoteSize != expected) {
            throw InventoryCraftingLayout.layoutFailure(
                    "menu/snapshot sizes diverged: slots=" + expected
                            + ", lastSlots=" + lastSize
                            + ", remoteSlots=" + remoteSize
            );
        }
    }
}

package dev.resivore.offhandqol.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.resivore.offhandqol.OffhandRoutingService;
import dev.resivore.offhandqol.compat.OffhandCarriedCompatibility;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {
    @Unique private Player offhandQol$player;
    @Unique private ItemStack offhandQol$sourceStack;
    @Unique private int offhandQol$excludedInventorySlot = -1;
    @Unique private boolean offhandQol$excludeOffhand;
    @Unique private boolean offhandQol$sourceIsPlayerOwned;
    @Unique private boolean offhandQol$routed;

    @WrapOperation(
            method = "doClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack offhandQol$trackQuickMove(
            AbstractContainerMenu menu,
            Player player,
            int slotIndex,
            Operation<ItemStack> original
    ) {
        Player previousPlayer = this.offhandQol$player;
        ItemStack previousSourceStack = this.offhandQol$sourceStack;
        int previousExcludedSlot = this.offhandQol$excludedInventorySlot;
        boolean previousExcludeOffhand = this.offhandQol$excludeOffhand;
        boolean previousPlayerOwned = this.offhandQol$sourceIsPlayerOwned;
        boolean previousRouted = this.offhandQol$routed;

        this.offhandQol$player = player;
        this.offhandQol$sourceStack = ItemStack.EMPTY;
        this.offhandQol$excludedInventorySlot = -1;
        this.offhandQol$excludeOffhand = false;
        this.offhandQol$sourceIsPlayerOwned = false;
        this.offhandQol$routed = false;

        List<Slot> slots = menu.slots;
        if (slotIndex >= 0 && slotIndex < slots.size()) {
            Slot source = slots.get(slotIndex);
            Inventory inventory = player.getInventory();
            this.offhandQol$sourceStack = source.getItem();
            this.offhandQol$sourceIsPlayerOwned = source.container == inventory;
            int containerSlot = source.getContainerSlot();
            if (source.container == inventory
                    && containerSlot >= 0
                    && containerSlot < inventory.getNonEquipmentItems().size()) {
                this.offhandQol$excludedInventorySlot = containerSlot;
            }
            this.offhandQol$excludeOffhand = source.container == inventory
                    && containerSlot == Inventory.SLOT_OFFHAND;
        }

        try {
            return original.call(menu, player, slotIndex);
        } finally {
            this.offhandQol$player = previousPlayer;
            this.offhandQol$sourceStack = previousSourceStack;
            this.offhandQol$excludedInventorySlot = previousExcludedSlot;
            this.offhandQol$excludeOffhand = previousExcludeOffhand;
            this.offhandQol$sourceIsPlayerOwned = previousPlayerOwned;
            this.offhandQol$routed = previousRouted;
        }
    }

    @WrapMethod(method = "moveItemStackTo")
    private boolean offhandQol$routeQuickMove(
            ItemStack incoming,
            int startSlot,
            int endSlot,
            boolean backwards,
            Operation<Boolean> original
    ) {
        Player player = this.offhandQol$player;
        if (player == null
                || this.offhandQol$routed
                || incoming.isEmpty()
                || incoming != this.offhandQol$sourceStack) {
            return original.call(incoming, startSlot, endSlot, backwards);
        }

        boolean targetContainsPlayerInventory = offhandQol$containsOrdinaryPlayerSlot(
                startSlot, endSlot, player.getInventory());
        if (!this.offhandQol$sourceIsPlayerOwned && !targetContainsPlayerInventory) {
            return original.call(incoming, startSlot, endSlot, backwards);
        }

        this.offhandQol$routed = true;
        if (this.offhandQol$sourceIsPlayerOwned) {
            boolean changed;
            if (OffhandCarriedCompatibility.isPlayerOriginAvailable()) {
                changed = OffhandCarriedCompatibility.routePlayerOrigin(
                        player,
                        incoming,
                        this.offhandQol$excludedInventorySlot,
                        this.offhandQol$excludeOffhand
                );
            } else {
                changed = OffhandRoutingService.routePlayerOriginToMatchingOffhand(
                        player,
                        incoming,
                        this.offhandQol$excludeOffhand
                );
            }
            if (!incoming.isEmpty()) {
                changed |= original.call(incoming, startSlot, endSlot, backwards);
            }
            return changed;
        }

        boolean changed;
        if (OffhandCarriedCompatibility.isAvailable()) {
            changed = OffhandCarriedCompatibility.routeIncoming(
                    player,
                    incoming,
                    this.offhandQol$excludedInventorySlot,
                    this.offhandQol$excludeOffhand
            );
        } else {
            changed = OffhandRoutingService.routeIncoming(
                    player,
                    incoming,
                    this.offhandQol$excludedInventorySlot,
                    this.offhandQol$excludeOffhand
            );
        }

        // External-source moves are fully owned by the incoming hierarchy.
        return changed;
    }

    @Unique
    private boolean offhandQol$containsOrdinaryPlayerSlot(int start, int end, Inventory inventory) {
        List<Slot> slots = ((AbstractContainerMenu) (Object) this).slots;
        int size = inventory.getNonEquipmentItems().size();
        for (int i = Math.max(0, start); i < Math.min(end, slots.size()); i++) {
            Slot slot = slots.get(i);
            int containerSlot = slot.getContainerSlot();
            if (slot.container == inventory && containerSlot >= 0 && containerSlot < size) return true;
        }
        return false;
    }
}

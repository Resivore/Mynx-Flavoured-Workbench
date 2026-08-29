package dev.resivore.carriedrouting.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.resivore.carriedrouting.RoutingContext;
import dev.resivore.carriedrouting.RoutingService;
import dev.resivore.carriedrouting.api.OffhandCompatibilityHook;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {
    @Unique private Player carriedRouting$player;
    @Unique private ItemStack carriedRouting$sourceStack;
    @Unique private int carriedRouting$excludedInventorySlot = -1;
    @Unique private boolean carriedRouting$excludeOffhand;
    @Unique private boolean carriedRouting$sourceIsPlayerOwned;
    @Unique private boolean carriedRouting$routed;

    /**
     * Some expanded player-menu compositions retain one more live Slot than
     * AbstractContainerMenu's comparison/remote snapshots. Native bundle and
     * ItemInteractions shulker mutations both call slotsChanged immediately,
     * so that mismatch aborts the click before it can be synchronized. Grow
     * only missing snapshot entries at the synchronization boundary; existing
     * entries and the live ItemStacks remain authoritative.
     */
    @Inject(
            method = {"broadcastChanges", "broadcastFullState", "sendAllDataToRemote"},
            at = @At("HEAD")
    )
    private void carriedRouting$alignMenuSnapshotsBeforeSynchronization(CallbackInfo ci) {
        carriedRouting$growMissingMenuSnapshots((AbstractContainerMenu) (Object) this);
    }

    @Inject(method = "transferState", at = @At("HEAD"))
    private void carriedRouting$alignTransferredMenuSnapshots(
            AbstractContainerMenu otherMenu,
            CallbackInfo ci
    ) {
        carriedRouting$growMissingMenuSnapshots((AbstractContainerMenu) (Object) this);
        carriedRouting$growMissingMenuSnapshots(otherMenu);
    }

    @Unique
    private static void carriedRouting$growMissingMenuSnapshots(AbstractContainerMenu menu) {
        AbstractContainerMenuSnapshotAccessor snapshots =
                (AbstractContainerMenuSnapshotAccessor) menu;
        int requiredSize = menu.slots.size();
        while (snapshots.carriedRouting$getLastSlots().size() < requiredSize) {
            snapshots.carriedRouting$getLastSlots().add(ItemStack.EMPTY);
        }

        ContainerSynchronizer synchronizer = snapshots.carriedRouting$getSynchronizer();
        while (snapshots.carriedRouting$getRemoteSlots().size() < requiredSize) {
            snapshots.carriedRouting$getRemoteSlots().add(
                    synchronizer == null ? RemoteSlot.PLACEHOLDER : synchronizer.createSlot()
            );
        }
    }

    @WrapOperation(
            method = "doClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack carriedRouting$trackQuickMove(
            AbstractContainerMenu menu,
            Player player,
            int slotIndex,
            Operation<ItemStack> original
    ) {
        Player previousPlayer = this.carriedRouting$player;
        ItemStack previousSourceStack = this.carriedRouting$sourceStack;
        int previousExcludedSlot = this.carriedRouting$excludedInventorySlot;
        boolean previousExcludeOffhand = this.carriedRouting$excludeOffhand;
        boolean previousPlayerOwned = this.carriedRouting$sourceIsPlayerOwned;
        boolean previousRouted = this.carriedRouting$routed;

        this.carriedRouting$player = player;
        this.carriedRouting$sourceStack = ItemStack.EMPTY;
        this.carriedRouting$excludedInventorySlot = -1;
        this.carriedRouting$excludeOffhand = false;
        this.carriedRouting$sourceIsPlayerOwned = false;
        this.carriedRouting$routed = false;

        List<Slot> slots = menu.slots;
        if (slotIndex >= 0 && slotIndex < slots.size()) {
            Slot source = slots.get(slotIndex);
            Inventory inventory = player.getInventory();
            this.carriedRouting$sourceStack = source.getItem();
            this.carriedRouting$sourceIsPlayerOwned = source.container == inventory;
            int containerSlot = source.getContainerSlot();
            if (source.container == inventory
                    && containerSlot >= 0
                    && containerSlot < inventory.getNonEquipmentItems().size()) {
                this.carriedRouting$excludedInventorySlot = containerSlot;
            }
            this.carriedRouting$excludeOffhand = source.container == inventory
                    && containerSlot == Inventory.SLOT_OFFHAND;
        }

        try {
            return original.call(menu, player, slotIndex);
        } finally {
            this.carriedRouting$player = previousPlayer;
            this.carriedRouting$sourceStack = previousSourceStack;
            this.carriedRouting$excludedInventorySlot = previousExcludedSlot;
            this.carriedRouting$excludeOffhand = previousExcludeOffhand;
            this.carriedRouting$sourceIsPlayerOwned = previousPlayerOwned;
            this.carriedRouting$routed = previousRouted;
        }
    }

    @WrapMethod(method = "moveItemStackTo")
    private boolean carriedRouting$routeQuickMove(
            ItemStack incoming,
            int startSlot,
            int endSlot,
            boolean backwards,
            Operation<Boolean> original
    ) {
        Player player = this.carriedRouting$player;
        if (player == null
                || this.carriedRouting$routed
                || incoming.isEmpty()
                || incoming != this.carriedRouting$sourceStack
                || OffhandCompatibilityHook.isDelegatedToOffhand()) {
            return original.call(incoming, startSlot, endSlot, backwards);
        }

        boolean targetContainsPlayerInventory = carriedRouting$containsOrdinaryPlayerSlot(
                startSlot, endSlot, player.getInventory());
        if (!this.carriedRouting$sourceIsPlayerOwned && !targetContainsPlayerInventory) {
            return original.call(incoming, startSlot, endSlot, backwards);
        }

        this.carriedRouting$routed = true;
        if (this.carriedRouting$sourceIsPlayerOwned) {
            boolean changed = RoutingService.routePlayerOriginSpecialDestinations(
                    player,
                    incoming,
                    this.carriedRouting$excludedInventorySlot,
                    this.carriedRouting$excludeOffhand
            );
            if (!incoming.isEmpty()) {
                changed |= original.call(incoming, startSlot, endSlot, backwards);
            }
            return changed;
        }

        int before = incoming.getCount();
        RoutingService.routeIncomingStack(
                player,
                incoming,
                RoutingContext.QUICK_MOVE,
                this.carriedRouting$excludedInventorySlot,
                this.carriedRouting$excludeOffhand
        );
        boolean changed = incoming.getCount() != before;

        // External-source moves are fully owned by the incoming hierarchy.
        return changed;
    }

    @Unique
    private boolean carriedRouting$containsOrdinaryPlayerSlot(int start, int end, Inventory inventory) {
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

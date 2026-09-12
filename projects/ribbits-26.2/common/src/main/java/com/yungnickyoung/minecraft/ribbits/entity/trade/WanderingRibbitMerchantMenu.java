package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.yungnickyoung.minecraft.ribbits.entity.WanderingRibbitEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * The normal merchant menu with a server-side preflight for Wandering Ribbit results.
 *
 * <p>Minecraft moves a result out of {@link MerchantResultSlot} before
 * {@link MerchantResultSlot#onTake(Player, ItemStack)} asks the offer to take payment.  A guard in
 * {@link MerchantOffer#take(ItemStack, ItemStack)} is consequently too late to prevent a stale
 * result from reaching the player.  This menu replaces only the Wandering Ribbit result slot and
 * checks the entity's persisted provider identity before any ordinary click path can remove the
 * result.  Quick-move also checks on every invocation because vanilla may invoke
 * {@link #quickMoveStack(Player, int)} repeatedly for a single shift-click.</p>
 */
public final class WanderingRibbitMerchantMenu extends MerchantMenu {
    public WanderingRibbitMerchantMenu(
            int containerId, Inventory inventory, WanderingRibbitEntity merchant
    ) {
        this(containerId, inventory, merchant, merchant::mayTakeMerchantResult);
    }

    WanderingRibbitMerchantMenu(
            int containerId,
            Inventory inventory,
            Merchant merchant,
            Predicate<MerchantOffer> mayTakeResult
    ) {
        super(containerId, inventory, merchant);
        Objects.requireNonNull(mayTakeResult, "mayTakeResult");

        Slot vanillaResult = this.getSlot(RESULT_SLOT);
        MerchantContainer container = (MerchantContainer) vanillaResult.container;
        Slot guardedResult = new GuardedResultSlot(
                inventory.player,
                merchant,
                container,
                vanillaResult.getContainerSlot(),
                vanillaResult.x,
                vanillaResult.y,
                mayTakeResult
        );
        guardedResult.index = vanillaResult.index;
        this.slots.set(RESULT_SLOT, guardedResult);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == RESULT_SLOT && !this.getSlot(RESULT_SLOT).mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    private static final class GuardedResultSlot extends MerchantResultSlot {
        private final MerchantContainer container;
        private final Predicate<MerchantOffer> mayTakeResult;

        private GuardedResultSlot(
                Player player,
                Merchant merchant,
                MerchantContainer container,
                int slot,
                int x,
                int y,
                Predicate<MerchantOffer> mayTakeResult
        ) {
            super(player, merchant, container, slot, x, y);
            this.container = container;
            this.mayTakeResult = mayTakeResult;
        }

        @Override
        public boolean mayPickup(Player player) {
            return super.mayPickup(player) && mayTakeResult.test(container.getActiveOffer());
        }
    }
}

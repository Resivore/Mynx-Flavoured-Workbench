package dev.resivore.carriedrouting;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassivePickupPriorityTest {
    private static DataComponentMap stackComponents;

    private static final class TestInventory extends Inventory {
        private final NonNullList<ItemStack> ordinary;

        private TestInventory(int liveOrdinarySize) {
            super(null, new EntityEquipment());
            this.ordinary = NonNullList.withSize(liveOrdinarySize, ItemStack.EMPTY);
        }

        @Override
        public NonNullList<ItemStack> getNonEquipmentItems() {
            return ordinary;
        }

        @Override
        public ItemStack getItem(int slot) {
            return ordinary.get(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            ordinary.set(slot, stack);
        }
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();
        stackComponents = DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 64)
                .build();
    }

    private static Inventory inventory(int liveOrdinarySize) {
        Inventory inventory = new TestInventory(liveOrdinarySize);
        assertEquals(liveOrdinarySize, inventory.getNonEquipmentItems().size());
        return inventory;
    }

    private static ItemStack stone(int count) {
        return stack(Items.STONE, count);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(Holder.direct(item, stackComponents), count);
    }

    private static ItemStack namedStone(String name, int count) {
        ItemStack stack = stone(count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static int total(Inventory inventory, ItemStack incoming) {
        return incoming.getCount() + inventory.getNonEquipmentItems().stream()
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static void routePassiveFallbacks(Inventory inventory, ItemStack incoming) {
        routeFallbacks(inventory, incoming, RoutingContext.WORLD_PICKUP);
    }

    private static void routeExternalQuickMoveFallbacks(Inventory inventory, ItemStack incoming) {
        routeFallbacks(inventory, incoming, RoutingContext.QUICK_MOVE);
    }

    private static void routeFallbacks(
            Inventory inventory,
            ItemStack incoming,
            RoutingContext context
    ) {
        RoutingService.routeOrdinaryInventoryFallbacks(
                inventory,
                incoming,
                context,
                -1
        );
    }

    @Test
    void inventoryExtendedStoragePartialsAtIndices9_36And62PrecedeEmptyHotbar() {
        for (int index : List.of(9, 36, 62)) {
            Inventory inventory = inventory(63);
            inventory.getNonEquipmentItems().set(index, stone(60));
            ItemStack incoming = stone(4);
            int totalBefore = total(inventory, incoming);

            routePassiveFallbacks(inventory, incoming);

            assertEquals(64, inventory.getItem(index).getCount());
            assertTrue(inventory.getItem(0).isEmpty());
            assertTrue(incoming.isEmpty());
            assertEquals(totalBefore, total(inventory, incoming));
        }
    }

    @Test
    void storagePartialTakesTenBeforeRemainderOpensHotbarStack() {
        Inventory inventory = inventory(63);
        inventory.getNonEquipmentItems().set(36, stone(54));
        ItemStack incoming = stone(20);
        int totalBefore = total(inventory, incoming);

        routePassiveFallbacks(inventory, incoming);

        assertEquals(64, inventory.getItem(36).getCount());
        assertEquals(10, inventory.getItem(0).getCount());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void allStoragePartialsFillInStableOrderBeforeHotbarFallback() {
        Inventory inventory = inventory(63);
        inventory.getNonEquipmentItems().set(9, stone(59));
        inventory.getNonEquipmentItems().set(62, stone(56));
        ItemStack incoming = stone(14);
        int totalBefore = total(inventory, incoming);

        routePassiveFallbacks(inventory, incoming);

        assertEquals(64, inventory.getItem(9).getCount());
        assertEquals(64, inventory.getItem(62).getCount());
        assertEquals(1, inventory.getItem(0).getCount());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void productionHotbarMergeThenStorageMergeBothPrecedeEmptyHotbar() {
        Inventory inventory = inventory(63);
        inventory.getNonEquipmentItems().set(2, stone(62));
        inventory.getNonEquipmentItems().set(7, stone(61));
        inventory.getNonEquipmentItems().set(36, stone(60));
        ItemStack incoming = stone(11);
        int totalBefore = total(inventory, incoming);

        RoutingService.mergeInventoryRange(inventory, incoming, 0, 9, -1, -1);
        routePassiveFallbacks(inventory, incoming);

        assertEquals(64, inventory.getItem(2).getCount());
        assertEquals(64, inventory.getItem(7).getCount());
        assertEquals(64, inventory.getItem(36).getCount());
        assertEquals(2, inventory.getItem(0).getCount());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void noCompatiblePartialStillUsesEmptyHotbarBeforeEmptyStorage() {
        Inventory inventory = inventory(63);
        ItemStack incoming = stone(12);
        int totalBefore = total(inventory, incoming);

        routePassiveFallbacks(inventory, incoming);

        assertEquals(12, inventory.getItem(0).getCount());
        assertTrue(inventory.getItem(9).isEmpty());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void fullHotbarStillAllowsStoragePartialToAcceptPickup() {
        Inventory inventory = inventory(63);
        for (int index = 0; index < 9; index++) {
            inventory.getNonEquipmentItems().set(index, stack(Items.DIRT, 64));
        }
        inventory.getNonEquipmentItems().set(62, stone(58));
        ItemStack incoming = stone(6);
        int totalBefore = total(inventory, incoming);

        routePassiveFallbacks(inventory, incoming);

        assertEquals(64, inventory.getItem(62).getCount());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void vanillaLiveInventoryIncludesFinalStorageIndex35() {
        Inventory inventory = inventory(36);
        inventory.getNonEquipmentItems().set(35, stone(60));
        ItemStack incoming = stone(4);
        int totalBefore = total(inventory, incoming);

        routePassiveFallbacks(inventory, incoming);

        assertEquals(36, inventory.getNonEquipmentItems().size());
        assertEquals(64, inventory.getItem(35).getCount());
        assertTrue(inventory.getItem(0).isEmpty());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void componentDistinctStorageStackDoesNotBlockEmptyHotbarPlacement() {
        Inventory inventory = inventory(63);
        ItemStack storage = namedStone("A", 60);
        ItemStack incoming = namedStone("B", 4);
        inventory.getNonEquipmentItems().set(36, storage);
        int totalBefore = total(inventory, incoming);

        assertFalse(ItemStack.isSameItemSameComponents(storage, incoming));
        routePassiveFallbacks(inventory, incoming);

        assertEquals(60, inventory.getItem(36).getCount());
        assertEquals(4, inventory.getItem(0).getCount());
        assertTrue(ItemStack.isSameItemSameComponents(
                inventory.getItem(0),
                namedStone("B", 1)
        ));
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void fullEarlierStorageDestinationCannotConsumeOrHideLaterPartial() {
        Inventory inventory = inventory(63);
        inventory.getNonEquipmentItems().set(9, stone(64));
        inventory.getNonEquipmentItems().set(62, stone(62));
        ItemStack incoming = stone(3);
        int totalBefore = total(inventory, incoming);

        routePassiveFallbacks(inventory, incoming);

        assertEquals(64, inventory.getItem(9).getCount());
        assertEquals(64, inventory.getItem(62).getCount());
        assertEquals(1, inventory.getItem(0).getCount());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void externalQuickMoveStoragePartialsAtIndices9_36And62PrecedeEmptyHotbar() {
        for (int index : List.of(9, 36, 62)) {
            Inventory inventory = inventory(63);
            inventory.getNonEquipmentItems().set(index, stone(40));
            ItemStack incoming = stone(10);
            int totalBefore = total(inventory, incoming);

            routeExternalQuickMoveFallbacks(inventory, incoming);

            assertEquals(50, inventory.getItem(index).getCount());
            assertTrue(inventory.getItem(0).isEmpty());
            assertTrue(incoming.isEmpty());
            assertEquals(totalBefore, total(inventory, incoming));
        }
    }

    @Test
    void externalQuickMoveUsesVanillaFinalStoragePartialBeforeEmptyHotbar() {
        Inventory inventory = inventory(36);
        inventory.getNonEquipmentItems().set(35, stone(60));
        ItemStack incoming = stone(4);
        int totalBefore = total(inventory, incoming);

        routeExternalQuickMoveFallbacks(inventory, incoming);

        assertEquals(64, inventory.getItem(35).getCount());
        assertTrue(inventory.getItem(0).isEmpty());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void externalQuickMoveFillsStorageThenPlacesExactRemainderInHotbar() {
        Inventory inventory = inventory(63);
        inventory.getNonEquipmentItems().set(36, stone(54));
        ItemStack incoming = stone(20);
        int totalBefore = total(inventory, incoming);

        routeExternalQuickMoveFallbacks(inventory, incoming);

        assertEquals(64, inventory.getItem(36).getCount());
        assertEquals(10, inventory.getItem(0).getCount());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void externalQuickMoveExhaustsMultipleStoragePartialsInStableOrder() {
        Inventory orderedInventory = inventory(63);
        orderedInventory.getNonEquipmentItems().set(9, stone(59));
        orderedInventory.getNonEquipmentItems().set(62, stone(56));
        ItemStack orderedIncoming = stone(10);
        int orderedTotalBefore = total(orderedInventory, orderedIncoming);

        routeExternalQuickMoveFallbacks(orderedInventory, orderedIncoming);

        assertEquals(64, orderedInventory.getItem(9).getCount());
        assertEquals(61, orderedInventory.getItem(62).getCount());
        assertTrue(orderedInventory.getItem(0).isEmpty());
        assertTrue(orderedIncoming.isEmpty());
        assertEquals(orderedTotalBefore, total(orderedInventory, orderedIncoming));

        Inventory exhaustedInventory = inventory(63);
        exhaustedInventory.getNonEquipmentItems().set(9, stone(59));
        exhaustedInventory.getNonEquipmentItems().set(62, stone(56));
        ItemStack exhaustedIncoming = stone(14);
        int exhaustedTotalBefore = total(exhaustedInventory, exhaustedIncoming);

        routeExternalQuickMoveFallbacks(exhaustedInventory, exhaustedIncoming);

        assertEquals(64, exhaustedInventory.getItem(9).getCount());
        assertEquals(64, exhaustedInventory.getItem(62).getCount());
        assertEquals(1, exhaustedInventory.getItem(0).getCount());
        assertTrue(exhaustedIncoming.isEmpty());
        assertEquals(exhaustedTotalBefore, total(exhaustedInventory, exhaustedIncoming));
    }

    @Test
    void externalQuickMoveKeepsHotbarMergeAheadOfStorageAndEmptyHotbar() {
        Inventory inventory = inventory(63);
        inventory.getNonEquipmentItems().set(2, stone(62));
        inventory.getNonEquipmentItems().set(36, stone(60));
        ItemStack incoming = stone(7);
        int totalBefore = total(inventory, incoming);

        RoutingService.mergeInventoryRange(inventory, incoming, 0, 9, -1, -1);
        routeExternalQuickMoveFallbacks(inventory, incoming);

        assertEquals(64, inventory.getItem(2).getCount());
        assertEquals(64, inventory.getItem(36).getCount());
        assertEquals(1, inventory.getItem(0).getCount());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void externalQuickMoveWithoutPartialStillUsesEmptyHotbarBeforeStorage() {
        Inventory inventory = inventory(63);
        ItemStack incoming = stone(12);
        int totalBefore = total(inventory, incoming);

        routeExternalQuickMoveFallbacks(inventory, incoming);

        assertEquals(12, inventory.getItem(0).getCount());
        assertTrue(inventory.getItem(9).isEmpty());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void externalQuickMoveDoesNotMergeComponentDistinctStorageStack() {
        Inventory inventory = inventory(63);
        ItemStack storage = namedStone("A", 40);
        ItemStack incoming = namedStone("B", 10);
        inventory.getNonEquipmentItems().set(36, storage);
        int totalBefore = total(inventory, incoming);

        assertFalse(ItemStack.isSameItemSameComponents(storage, incoming));
        routeExternalQuickMoveFallbacks(inventory, incoming);

        assertEquals(40, inventory.getItem(36).getCount());
        assertEquals(10, inventory.getItem(0).getCount());
        assertTrue(ItemStack.isSameItemSameComponents(
                inventory.getItem(0),
                namedStone("B", 1)
        ));
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }

    @Test
    void compatibilityTransferRetainsItsExistingEmptyHotbarFallback() {
        Inventory inventory = inventory(63);
        inventory.getNonEquipmentItems().set(36, stone(60));
        ItemStack incoming = stone(4);
        int totalBefore = total(inventory, incoming);

        routeFallbacks(inventory, incoming, RoutingContext.COMPAT_QUICK_TRANSFER);

        assertEquals(60, inventory.getItem(36).getCount());
        assertEquals(4, inventory.getItem(0).getCount());
        assertTrue(incoming.isEmpty());
        assertEquals(totalBefore, total(inventory, incoming));
    }
}

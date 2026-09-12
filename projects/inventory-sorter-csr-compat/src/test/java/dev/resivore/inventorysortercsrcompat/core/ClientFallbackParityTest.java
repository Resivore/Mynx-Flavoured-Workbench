package dev.resivore.inventorysortercsrcompat.core;

import dev.resivore.slotreservations.ModComponents;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.kyrptonaught.inventorysorter.client.sort.ClientSortScope;
import net.kyrptonaught.inventorysorter.client.sort.plan.ClientSortClickPlanner;
import net.kyrptonaught.inventorysorter.client.sort.plan.PlannedContainerClick;
import net.kyrptonaught.inventorysorter.sort.SortType;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientFallbackParityTest {
    @BeforeAll
    static void bootstrapMinecraftAndCsr() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bind(Items.COBBLESTONE);
        bind(Items.DIRT);
        bind(Items.OAK_PLANKS);
        initializeCsrComponents();
    }

    @Test
    void fallbackClicksProduceTheSameFillThenMaskedSortAsTheAuthoritativePath() {
        BarrelBlockEntity authoritative = barrel();
        BarrelBlockEntity fallback = barrel();
        initializeFixture(authoritative);
        initializeFixture(fallback);

        assertTrue(MaskedServerSort.sortIfNeeded(
                authoritative, 0, 9, SortType.NAME, "en_us", List.of(), false));

        List<ClientSortScope.ScopedSlot> scopedSlots = new ArrayList<>();
        Map<Integer, Slot> menuSlots = new HashMap<>();
        for (int index = 0; index < 9; index++) {
            Slot slot = new Slot(fallback, index, 0, 0);
            scopedSlots.add(new ClientSortScope.ScopedSlot(index, slot));
            menuSlots.put(index, slot);
        }
        ClientSortScope scope = new ClientSortScope(7, scopedSlots, List.of(), List.of());
        Optional<List<PlannedContainerClick>> planned = MaskedClientFallbackSort.planIfNeeded(
                scope, new ClientSortClickPlanner(), SortType.NAME, "en_us", List.of());
        assertNotNull(planned);
        List<PlannedContainerClick> clicks = planned.orElseThrow();

        // Three deterministic fill transfers produce 7 clicks before upstream plans the
        // unreserved layout. No later click may address either reserved menu slot.
        assertEquals(List.of(7, 1, 7, 0, 4, 8, 4),
                clicks.subList(0, 7).stream().map(PlannedContainerClick::slotIndex).toList());
        assertFalse(clicks.subList(7, clicks.size()).stream()
                .anyMatch(click -> click.slotIndex() == 1 || click.slotIndex() == 4));

        applyPickupClicks(menuSlots, clicks);
        for (int index = 0; index < 9; index++) {
            assertTrue(ItemStack.matches(authoritative.getItem(index), fallback.getItem(index)),
                    "Fallback diverged from authoritative final state at physical slot " + index);
        }
        assertEquals(96, count(fallback, Items.COBBLESTONE));
        assertEquals(8, count(fallback, Items.DIRT));
        assertEquals(2, count(fallback, Items.OAK_PLANKS));
    }

    private static void initializeFixture(BarrelBlockEntity barrel) {
        barrel.setItem(0, stack(Items.DIRT, 5));
        barrel.setItem(1, stack(Items.COBBLESTONE, 32));
        reserve(barrel, 1, stack(Items.COBBLESTONE, 1));
        reserve(barrel, 4, stack(Items.DIRT, 1));
        barrel.setItem(6, stack(Items.OAK_PLANKS, 2));
        barrel.setItem(7, stack(Items.COBBLESTONE, 64));
        barrel.setItem(8, stack(Items.DIRT, 3));
    }

    private static void applyPickupClicks(Map<Integer, Slot> menuSlots, List<PlannedContainerClick> clicks) {
        ItemStack carried = ItemStack.EMPTY;
        for (PlannedContainerClick click : clicks) {
            assertEquals(ContainerInput.PICKUP, click.input());
            assertEquals(0, click.button());
            Slot slot = menuSlots.get(click.slotIndex());
            assertNotNull(slot, "Fallback clicked outside its scope");
            ItemStack physical = slot.getItem();
            if (carried.isEmpty()) {
                carried = physical.copy();
                slot.set(ItemStack.EMPTY);
            } else if (physical.isEmpty()) {
                int moved = Math.min(carried.getCount(), slot.getMaxStackSize(carried));
                ItemStack placed = carried.copy();
                placed.setCount(moved);
                slot.set(placed);
                carried.shrink(moved);
            } else if (ItemStack.isSameItemSameComponents(physical, carried)) {
                int moved = Math.min(carried.getCount(),
                        Math.max(0, slot.getMaxStackSize(carried) - physical.getCount()));
                physical.grow(moved);
                carried.shrink(moved);
                slot.setChanged();
            } else {
                ItemStack replacement = carried;
                carried = physical.copy();
                slot.set(replacement);
            }
        }
        assertTrue(carried.isEmpty(), "Fallback left an ItemStack on the cursor");
    }

    private static BarrelBlockEntity barrel() {
        return new BarrelBlockEntity(BlockPos.ZERO, Blocks.BARREL.defaultBlockState());
    }

    private static void reserve(BarrelBlockEntity barrel, int slot, ItemStack template) {
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, slot).orElseThrow(), template);
    }

    private static ItemStack stack(Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(count);
        return stack;
    }

    private static int count(BarrelBlockEntity barrel, Item item) {
        int total = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = barrel.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void bind(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }

    private static void initializeCsrComponents() throws Exception {
        Field frozen = MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true);
        boolean wasFrozen = frozen.getBoolean(BuiltInRegistries.DATA_COMPONENT_TYPE);
        frozen.setBoolean(BuiltInRegistries.DATA_COMPONENT_TYPE, false);
        try {
            ModComponents.initialize();
        } finally {
            frozen.setBoolean(BuiltInRegistries.DATA_COMPONENT_TYPE, wasFrozen);
        }
    }
}

package dev.resivore.slotreservations;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TemplateEligibilityTest {
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        var registry = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE;
        var frozen = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true); boolean wasFrozen = frozen.getBoolean(registry); frozen.setBoolean(registry, false);
        try { ModComponents.initialize(); } finally { frozen.setBoolean(registry, wasFrozen); }
        List<Item> items = new ArrayList<>(List.of(Items.STONE, Blocks.SHULKER_BOX.asItem()));
        Blocks.DYED_SHULKER_BOX.asList().forEach(b -> items.add(b.asItem()));
        for (Item item : items) if (!item.builtInRegistryHolder().areComponentsBound())
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
    @Test void allColorsAndMultiCountEmptyIdentityAreAllowed() {
        var blocks = new ArrayList<>(Blocks.DYED_SHULKER_BOX.asList()); blocks.add(Blocks.SHULKER_BOX);
        for (var block : blocks) {
            var empty = new ItemStack(block, 16);
            empty.set(DataComponents.CUSTOM_NAME, Component.literal("Named empty"));
            empty.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(Component.literal("Kept lore"))));
            assertTrue(ReservationTemplateEligibility.allows(empty));
            var result = ReservationTransition.fromCursor(ReservationData.EMPTY, 0, empty);
            assertEquals(1, result.data().get(0).orElseThrow().getCount());
            assertTrue(result.data().matches(0, empty));
            var different = empty.copy(); different.set(DataComponents.CUSTOM_NAME, Component.literal("Other"));
            assertFalse(result.data().matches(0, different));
        }
    }
    @Test void nestedReservationDataStillRejectsGenericTemplatesWhileFilledShulkersBecomeSpecific() {
        for (int kind = 1; kind <= 3; kind++) {
            var invalid = new ItemStack(Blocks.SHULKER_BOX);
            if ((kind & 1) != 0) invalid.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE))));
            if ((kind & 2) != 0) ReservationStore.setData(invalid, ReservationData.EMPTY.with(26, new ItemStack(Items.STONE)));
            var existing = ReservationData.EMPTY.with(0, new ItemStack(Items.STONE));
            if ((kind & 1) != 0) {
                var occupied = ReservationTransition.fromOccupied(existing, 0, invalid);
                var cursor = ReservationTransition.fromCursor(existing, 0, invalid);
                assertEquals(ReservationTransition.Outcome.SET, occupied.outcome());
                assertEquals(ReservationTransition.Outcome.SET, cursor.outcome());
                assertTrue(occupied.data().getEntry(0).orElseThrow().specific());
            } else {
                assertFalse(ReservationTemplateEligibility.allows(invalid));
                assertEquals(ReservationTransition.Outcome.REJECTED, ReservationTransition.fromOccupied(existing, 0, invalid).outcome());
                assertEquals(ReservationTransition.Outcome.REJECTED, ReservationTransition.fromCursor(existing, 0, invalid).outcome());
                assertSame(existing, ReservationTransition.fromCursor(existing, 0, invalid).data());
            }
        }
    }
    @Test void historicalInvalidIdentityRemainsVisibleAndToggleClearable() {
        var invalid = new ItemStack(Blocks.SHULKER_BOX);
        ReservationStore.setData(invalid, ReservationData.EMPTY.with(26, new ItemStack(Items.STONE)));
        var historical = ReservationData.EMPTY.with(0, invalid);
        assertTrue(historical.matches(0, invalid)); assertTrue(historical.get(0).isPresent());
        assertEquals(ReservationTransition.Outcome.CLEARED, ReservationTransition.fromOccupied(historical, 0, invalid).outcome());
        assertTrue(ReservationTransition.clear(historical, 0).data().isEmpty());
        assertFalse(historical.isEmpty());
    }
    @Test void emptyReservationRejectsItsLaterFilledAndReservedForms() {
        var empty = new ItemStack(Blocks.SHULKER_BOX);
        var data = ReservationTransition.fromOccupied(ReservationData.EMPTY, 0, empty).data();
        var filled = empty.copy(); filled.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE))));
        var reserved = empty.copy(); ReservationStore.setData(reserved, ReservationData.EMPTY.with(1, new ItemStack(Items.STONE)));
        assertFalse(data.matches(0, filled)); assertFalse(data.matches(0, reserved));
    }
    @Test void nonShulkerAndNoOpBehaviorIsUnchanged() {
        var stone = new ItemStack(Items.STONE); assertTrue(ReservationTemplateEligibility.allows(stone));
        assertFalse(ReservationTemplateEligibility.allows(ItemStack.EMPTY));
        var set = ReservationTransition.fromOccupied(ReservationData.EMPTY, 0, stone);
        assertTrue(set.changed()); assertFalse(ReservationTransition.fromCursor(set.data(), 0, stone).changed());
        assertTrue(ReservationTransition.fromOccupied(set.data(), 0, stone).data().isEmpty());
    }
}

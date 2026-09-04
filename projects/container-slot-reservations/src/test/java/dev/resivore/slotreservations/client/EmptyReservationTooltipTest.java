package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class EmptyReservationTooltipTest {
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Standalone JUnit has no Fabric registration phase; temporarily open this fixture registry only.
        var registry = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE;
        var frozen = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true);
        boolean wasFrozen = frozen.getBoolean(registry);
        frozen.setBoolean(registry, false);
        try { dev.resivore.slotreservations.ModComponents.initialize(); }
        finally { frozen.setBoolean(registry, wasFrozen); }
        bind(Items.STONE);
        bind(Blocks.SHULKER_BOX.asItem());
        for (var block : Blocks.DYED_SHULKER_BOX.asList()) bind(block.asItem());
    }
    private static void bind(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound())
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
    private static ItemStack carrier(int... slots) {
        ItemStack stack = new ItemStack(Blocks.SHULKER_BOX);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Reserved empty"));
        ReservationData data = ReservationData.EMPTY;
        for (int slot : slots) data = data.with(slot, new ItemStack(Items.STONE));
        ReservationStore.setData(stack, data);
        return stack;
    }
    @Test void singleAndMultipleReservationsEnableOnlyThePhysicalEmptinessException() {
        for (int[] slots : List.of(new int[]{26}, new int[]{0,8,9,17,18,26})) {
            ItemStack stack = carrier(slots);
            var before = stack.getComponents();
            assertTrue(EasyShulkerTooltipCompat.hasEmptyReservations(stack));
            var physical = NonNullList.withSize(27, ItemStack.EMPTY);
            stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(physical);
            assertTrue(physical.stream().allMatch(s -> s == ItemStack.EMPTY));
            assertEquals(Arrays.stream(slots).boxed().toList(),
                    ShulkerTooltipOverlay.plan(stack, physical).stream().map(ShulkerTooltipOverlay.SlotOverlay::slot).toList());
            assertEquals(before, stack.getComponents());
            assertNull(stack.get(DataComponents.CONTAINER));
        }
    }
    @Test void unreservedAndClearedFinalReservationKeepNativeNoPreview() {
        ItemStack stack = carrier();
        assertFalse(EasyShulkerTooltipCompat.hasEmptyReservations(stack));
        ReservationStore.setData(stack, ReservationData.EMPTY.with(8, new ItemStack(Items.STONE)));
        assertTrue(EasyShulkerTooltipCompat.hasEmptyReservations(stack));
        ReservationStore.setData(stack, ReservationData.EMPTY);
        assertFalse(EasyShulkerTooltipCompat.hasEmptyReservations(stack));
    }
    @Test void physicalContentsAndStackCountKeepTheExistingPath() {
        ItemStack stack = carrier(8);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE))));
        var original = stack.get(DataComponents.CONTAINER);
        assertFalse(EasyShulkerTooltipCompat.hasEmptyReservations(stack));
        assertSame(original, stack.get(DataComponents.CONTAINER));
        stack.remove(DataComponents.CONTAINER);
        stack.setCount(2);
        assertFalse(EasyShulkerTooltipCompat.hasEmptyReservations(stack));
    }
    @Test void everyVanillaShulkerColorIsEligibleAndOtherItemsAreNot() {
        var items = new ArrayList<Item>();
        items.add(Blocks.SHULKER_BOX.asItem());
        Blocks.DYED_SHULKER_BOX.asList().forEach(block -> items.add(block.asItem()));
        assertEquals(17, items.size());
        for (Item item : items) {
            ItemStack stack = new ItemStack(item);
            ReservationStore.setData(stack, ReservationData.EMPTY.with(0, new ItemStack(Items.STONE)));
            assertTrue(EasyShulkerTooltipCompat.hasEmptyReservations(stack));
        }
        ItemStack other = new ItemStack(Items.STONE);
        ReservationStore.setData(other, ReservationData.EMPTY.with(0, other));
        assertFalse(EasyShulkerTooltipCompat.hasEmptyReservations(other));
    }
    @Test void exactNativeComponentRetains27EmptySlotsAndNativeGeometry() throws Exception {
        byte[] bytes = null;
        try (ZipFile outer = new ZipFile(System.getProperty("easyShulkerBoxesJar"))) {
            byte[] nested = outer.getInputStream(outer.getEntry("META-INF/jars/iteminteractions-fabric-26.2.2.jar")).readAllBytes();
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(nested))) {
                for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                    if (entry.getName().equals("fuzs/iteminteractions/common/api/v2/world/inventory/tooltip/ItemContentsTooltip.class"))
                        bytes = zip.readAllBytes();
                }
            }
        }
        assertNotNull(bytes);
        class NativeLoader extends ClassLoader {
            NativeLoader() { super(EmptyReservationTooltipTest.class.getClassLoader()); }
            Class<?> define(byte[] code) { return defineClass(null, code, 0, code.length); }
        }
        Class<?> nativeType = new NativeLoader().define(bytes);
        var physical = NonNullList.withSize(27, ItemStack.EMPTY);
        Object tooltip = nativeType.getConstructor(NonNullList.class, int.class, int.class, int.class, int.class)
                .newInstance(physical, -1, 9, 3, -1);
        assertSame(physical, nativeType.getMethod("itemList").invoke(tooltip));
        assertEquals(9, nativeType.getMethod("gridWidth").invoke(tooltip));
        assertEquals(3, nativeType.getMethod("gridHeight").invoke(tooltip));
        assertEquals(-1, nativeType.getMethod("selectedItem").invoke(tooltip));
        assertEquals(0, physical.stream().filter(s -> !s.isEmpty()).count());
    }
    @Test void emptyReservationSourceIsCapturedWithoutChangingAnyCarrierComponent() {
        class Source implements net.minecraft.world.inventory.tooltip.TooltipComponent, TooltipSourceAccess {
            ItemStack source = ItemStack.EMPTY;
            public ItemStack containerSlotReservations$getSourceStack() { return source; }
            public void containerSlotReservations$setSourceStack(ItemStack stack) { source = stack; }
        }
        ItemStack carrier = carrier(0, 8, 9, 17, 18, 26);
        var before = carrier.copy();
        Source component = new Source();
        EasyShulkerTooltipCompat.captureSource(true, carrier, Optional.of(component));
        assertTrue(ItemStack.matches(carrier, component.source));
        assertNotSame(carrier, component.source);
        assertTrue(ItemStack.matches(before, carrier));
        Source disabled = new Source();
        EasyShulkerTooltipCompat.captureSource(false, carrier, Optional.of(disabled));
        assertTrue(disabled.source.isEmpty());
    }
}

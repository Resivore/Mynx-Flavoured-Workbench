package dev.resivore.carriedrouting;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ShulkerReservationRoutingTest {
    private static boolean realProvider;
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        for (Item item : List.of(Items.STONE, Items.DIRT, Items.SADDLE, Blocks.SHULKER_BOX.asItem())) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, item == Blocks.SHULKER_BOX.asItem() ? 1 : 64).build());
        }
        realProvider = ShulkerReservationRoutingTest.class.getClassLoader()
                .getResource("dev/resivore/slotreservations/api/ContainerSlotReservationsApi.class") != null;
        if (realProvider) {
            var registry = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE;
            var frozen = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
            frozen.setAccessible(true);
            boolean before = frozen.getBoolean(registry);
            frozen.setBoolean(registry, false);
            try { Class.forName("dev.resivore.slotreservations.ModComponents").getMethod("initialize").invoke(null); }
            finally { frozen.setBoolean(registry, before); }
        }
    }
    private static ItemStack stone(int count) { return new ItemStack(Items.STONE, count); }
    private static ItemStack named(String name) {
        ItemStack stack = stone(1); stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)); return stack;
    }
    private static final class Fixture {
        final ItemStack carrier = new ItemStack(Blocks.SHULKER_BOX);
        final Map<Integer, ItemStack> reservations = new TreeMap<>();
        Fixture reserve(int slot, ItemStack item) { reservations.put(slot, item.copyWithCount(1)); return this; }
        Fixture physical(int slot, ItemStack item) {
            var items = contents(); items.set(slot, item);
            carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items)); return this;
        }
        NonNullList<ItemStack> contents() {
            var items = NonNullList.withSize(27, ItemStack.EMPTY);
            carrier.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items); return items;
        }
        ReservationAdmission admission() {
            if (realProvider) {
                try {
                    // Test setup only: production references exclusively the public read-only API.
                    Class<?> dataType = Class.forName("dev.resivore.slotreservations.ReservationData");
                    Object data = dataType.getField("EMPTY").get(null);
                    for (var entry : reservations.entrySet())
                        data = dataType.getMethod("with", int.class, ItemStack.class).invoke(data, entry.getKey(), entry.getValue());
                    Class.forName("dev.resivore.slotreservations.ReservationStore")
                            .getMethod("setData", ItemStack.class, dataType).invoke(null, carrier, data);
                    return new CsrReservationAdmission();
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            }
            return new ReservationAdmission() {
                public Slot classify(ItemStack carrier, int slot, ItemStack incoming, ItemStack physical) {
                    if (!permitsAffinity(carrier, slot, incoming)) return Slot.RESERVED_OTHER;
                    Slot nativeSlot = ABSENT.classify(carrier, slot, incoming, physical);
                    return nativeSlot == Slot.UNRESERVED_EMPTY && reservations.containsKey(slot)
                            ? Slot.RESERVED_MATCH : nativeSlot;
                }
                public boolean permitsAffinity(ItemStack carrier, int slot, ItemStack incoming) {
                    return !reservations.containsKey(slot)
                            || ItemStack.isSameItemSameComponents(reservations.get(slot), incoming);
                }
            };
        }
        ShulkerDestination destination() { return new ShulkerDestination(carrier, admission()); }
        int count(int slot) { return contents().get(slot).getCount(); }
        int total() { return contents().stream().mapToInt(ItemStack::getCount).sum(); }
    }
    @Test void emptyReservationCreatesAffinityAtItsPhysicalIndex() {
        var f = new Fixture().reserve(26, stone(1)); var incoming = stone(12);
        var d = f.destination(); var before = f.carrier.copy();
        assertTrue(d.qualifies(incoming)); assertTrue(ItemStack.matches(before, f.carrier));
        assertEquals(12, d.insert(incoming)); assertEquals(12, f.count(26)); assertEquals(0, f.count(0));
    }
    @Test void allMatchingReservationsPrecedeEarlierUnreservedSlots() {
        var f = new Fixture().reserve(8, stone(1)).reserve(17, stone(1)).reserve(26, stone(1));
        assertEquals(200, f.destination().insert(stone(200)));
        assertEquals(64, f.count(8)); assertEquals(64, f.count(17)); assertEquals(64, f.count(26));
        assertEquals(8, f.count(0)); assertEquals(200, f.total());
    }
    @Test void componentDistinctAndUnrelatedReservationsCannotClaimAnItemType() {
        for (ItemStack reserved : List.of(named("different"), new ItemStack(Items.DIRT))) {
            var f = new Fixture().reserve(5, reserved);
            assertFalse(f.destination().qualifies(stone(4))); assertEquals(0, f.destination().insert(stone(4)));
            assertEquals(0, f.total());
        }
    }
    @Test void emptyUnreservedShulkerRemainsIneligible() {
        assertFalse(new Fixture().destination().qualifies(stone(1)));
    }
    @Test void mismatchSkipsReservedEmptyAndUsesPermittedFallback() {
        var f = new Fixture().physical(26, stone(64)).reserve(0, new ItemStack(Items.DIRT));
        assertEquals(4, f.destination().insert(stone(4))); assertEquals(0, f.count(0)); assertEquals(4, f.count(1));
    }
    @Test void noFallbackMismatchDeniesWithoutMutation() {
        var f = new Fixture().physical(26, stone(64));
        for (int i = 0; i < 26; i++) f.reserve(i, new ItemStack(Items.DIRT));
        var d = f.destination(); var before = f.carrier.copy(); var incoming = stone(4);
        assertFalse(d.qualifies(incoming)); assertEquals(0, d.insert(incoming));
        assertEquals(4, incoming.getCount()); assertTrue(ItemStack.matches(before, f.carrier));
    }
    @Test void occupiedThenReservedThenOrdinaryAndExactRemainder() {
        var f = new Fixture().physical(20, stone(60)).reserve(26, stone(1));
        assertEquals(70, f.destination().insert(stone(70)));
        assertEquals(64, f.count(20)); assertEquals(64, f.count(26)); assertEquals(2, f.count(0));
    }
    @Test void fullPhysicalMatchStillEstablishesAffinity() {
        var f = new Fixture().physical(26, stone(64));
        assertTrue(f.destination().qualifies(stone(1)));
        assertEquals(1, f.destination().insert(stone(1))); assertEquals(1, f.count(0));
    }
    @Test void conflictingOccupiedReservationNeitherGrowsNorSeedsAffinity() {
        var f = new Fixture().physical(3, stone(20)).reserve(3, new ItemStack(Items.DIRT));
        assertEquals(0, f.destination().insert(stone(4))); assertEquals(20, f.count(3));
        f.reserve(8, stone(1));
        assertEquals(4, f.destination().insert(stone(4))); assertEquals(20, f.count(3)); assertEquals(4, f.count(8));
    }
    @Test void lockAndNestingDeny() {
        var f = new Fixture().reserve(8, stone(1));
        RoutingLock.setLocked(f.carrier, true); assertEquals(0, f.destination().insert(stone(1)));
        RoutingLock.setLocked(f.carrier, false); assertEquals(1, f.destination().insert(stone(1)));
        f.reserve(9, new ItemStack(Blocks.SHULKER_BOX));
        assertEquals(0, f.destination().insert(new ItemStack(Blocks.SHULKER_BOX)));
    }
    @Test void componentsAndReservationSurviveInsertionAndRemoval() {
        var f = new Fixture().reserve(8, stone(1));
        f.carrier.set(DataComponents.CUSTOM_NAME, Component.literal("Carrier"));
        f.carrier.set(DataComponents.DAMAGE, 7);
        RoutingLock.setLocked(f.carrier, false);
        var d = f.destination();
        var components = f.carrier.getComponents();
        d.insert(stone(3));
        for (var component : components)
            assertEquals(component.value(), f.carrier.get(component.type()));
        f.physical(8, ItemStack.EMPTY);
        assertTrue(d.qualifies(stone(1))); assertEquals(1, d.insert(stone(1))); assertEquals(1, f.count(8));
    }
    @Test void countsBelowAtAndAboveCapacityRemainExact() {
        for (int count : List.of(1, 63, 64, 65, 128, 2000)) {
            var f = new Fixture().reserve(26, stone(1)); var incoming = stone(count);
            int inserted = f.destination().insert(incoming);
            assertEquals(Math.min(count, 1728), inserted);
            assertEquals(count, f.total() + incoming.getCount());
        }
    }
    @Test void effectiveNonstackableHolderMaximumIsRespectedWithoutExplicitPatch() {
        ItemStack saddle = new ItemStack(Items.SADDLE, 70);
        assertEquals(64, saddle.getMaxStackSize());
        assertFalse(saddle.getComponentsPatch().entrySet().stream().anyMatch(e -> e.getKey() == DataComponents.MAX_STACK_SIZE));
        var f = new Fixture().reserve(26, saddle);
        assertEquals(70, f.destination().insert(saddle)); assertEquals(64, f.count(26)); assertEquals(6, f.count(0));
    }
    @Test void ordinaryCarrierOrderAndRemainderOwnershipRemainStable() {
        var first = new Fixture().reserve(26, stone(1));
        for (int i = 0; i < 26; i++) first.reserve(i, new ItemStack(Items.DIRT));
        var second = new Fixture().physical(4, stone(1)); var incoming = stone(100);
        first.destination().insert(incoming); second.destination().insert(incoming);
        assertEquals(64, first.count(26)); assertEquals(37, second.count(4)); assertTrue(incoming.isEmpty());
    }
    @Test void absentProviderReproducesPhysicalAffinityWithoutResolvingCsr() {
        var f = new Fixture().reserve(8, stone(1));
        var d = new ShulkerDestination(f.carrier, ReservationAdmission.ABSENT);
        assertFalse(d.qualifies(stone(1)));
        f.physical(26, stone(64)); assertEquals(4, d.insert(stone(4))); assertEquals(4, f.count(0));
        if (!realProvider) assertInstanceOf(ReservationAdmission.class, ReservationAdmission.load());
    }
    @Test void incompatibleApiFailsClosedBeforeAnyMutation() {
        var f = new Fixture().physical(0, stone(1)); var incoming = stone(8);
        var broken = new ReservationAdmission() {
            public Slot classify(ItemStack c, int s, ItemStack i, ItemStack p) { throw new NoSuchMethodError("fixture API drift"); }
            public boolean permitsAffinity(ItemStack c, int s, ItemStack i) { return true; }
        };
        var d = new ShulkerDestination(f.carrier, broken);
        var before = f.carrier.copy();
        assertFalse(d.qualifies(incoming)); assertEquals(0, d.insert(incoming));
        assertTrue(ItemStack.matches(before, f.carrier)); assertEquals(8, incoming.getCount());
    }
    @Test void acceptedC11BytesRejectTheNewAffinityCaseAndRetainIdenticalBundleImplementation() throws Exception {
        var jar = java.nio.file.Path.of(System.getProperty("projectRoot"), "artifacts",
                "carried-container-auto-routing-0.3.6-external-quick-move-priority-fix-canary.jar");
        try (var zip = new java.util.zip.ZipFile(jar.toFile())) {
            class BaselineLoader extends ClassLoader {
                BaselineLoader() { super(ShulkerReservationRoutingTest.class.getClassLoader()); }
                @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if (!name.startsWith("dev.resivore.carriedrouting.")) return super.loadClass(name, resolve);
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        try {
                            byte[] bytes = zip.getInputStream(zip.getEntry(name.replace('.', '/') + ".class")).readAllBytes();
                            type = defineClass(name, bytes, 0, bytes.length);
                        } catch (java.io.IOException e) { throw new ClassNotFoundException(name, e); }
                    }
                    if (resolve) resolveClass(type);
                    return type;
                }
            }
            Class<?> baseline = new BaselineLoader().loadClass("dev.resivore.carriedrouting.ShulkerDestination");
            var constructor = baseline.getDeclaredConstructor(ItemStack.class); constructor.setAccessible(true);
            var qualifies = baseline.getDeclaredMethod("qualifies", ItemStack.class); qualifies.setAccessible(true);
            var f = new Fixture().reserve(26, stone(1));
            var successor = f.destination();
            assertEquals(false, qualifies.invoke(constructor.newInstance(f.carrier), stone(12)));
            assertTrue(successor.qualifies(stone(12)));
            String bundle = "dev/resivore/carriedrouting/BundleDestination.class";
            assertArrayEquals(zip.getInputStream(zip.getEntry(bundle)).readAllBytes(),
                    getClass().getClassLoader().getResourceAsStream(bundle).readAllBytes());
        }
    }
}

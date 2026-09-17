package dev.resivore.slotreservations.client;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic entry, identity, and projected-authority state for C24's inbound RMB gesture. */
final class CarriedShulkerRmbGestureTest {
    private static final CarriedShulkerRmbGesture.SlotKey SLOT_A =
            new CarriedShulkerRmbGesture.SlotKey(12, 3);
    private static final CarriedShulkerRmbGesture.SlotKey SLOT_B =
            new CarriedShulkerRmbGesture.SlotKey(13, 4);

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.SHULKER_BOX);
        bindTestComponents(Items.COBBLESTONE);
        bindTestComponents(Items.DIRT);
    }

    @Test
    void occupiedOriginActivatesOnlyInboundOwnershipAndDeduplicatesIt() {
        var gesture = new CarriedShulkerRmbGesture();
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                SLOT_A, shulker(), "cursor-0");
        assertEquals(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER, gesture.mode());
        assertFalse(gesture.enter(SLOT_A), "The directly dispatched occupied origin is already claimed");
        assertTrue(gesture.enter(SLOT_B));
        assertEquals(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER, gesture.mode(),
                "A later slot never reclassifies the press-time direction");

    }

    @Test
    void consecutiveIdentityDeduplicatesButOtherAndNullPermitReentryWithProjectedRemainder() {
        var gesture = new CarriedShulkerRmbGesture();
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                SLOT_A, shulker(), "cursor-0");

        ItemStack original = new ItemStack(Items.COBBLESTONE, 19);
        ItemStack remainder = new ItemStack(Items.COBBLESTONE, 7);
        gesture.advanceSource(SLOT_A, remainder);
        assertFalse(gesture.enter(SLOT_A));
        assertTrue(gesture.enter(SLOT_B));
        assertTrue(gesture.enter(SLOT_A), "A -> B -> A is a distinct Mouse Tweaks entry");
        assertStack(gesture.projectedSource(SLOT_A, original), Items.COBBLESTONE, 7);

        gesture.leaveSlotSurface();
        assertTrue(gesture.enter(SLOT_A), "A null boundary permits the same slot to be re-entered");
        assertStack(gesture.projectedSource(SLOT_A, original), Items.COBBLESTONE, 7);
        assertEquals(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER, gesture.mode());
    }

    @Test
    void creativeFacadeMapsOnlyOrdinaryInventoryAndHotbarCoordinates() {
        assertEquals(-1, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(5, true));
        assertEquals(-1, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(8, true));
        assertEquals(9, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(9, true));
        assertEquals(35, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(35, true));
        assertEquals(0, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(36, true));
        assertEquals(8, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(44, true));
        assertEquals(-1, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(45, true));

        assertEquals(0, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(0, false));
        assertEquals(8, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(8, false));
        assertEquals(-1, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(9, false));
        assertEquals(-1, CarriedShulkerRmbGesture.creativePhysicalPlayerSlot(44, false));
    }

    @Test
    void sourceProjectionsArePerPhysicalSlotAndReturnedAsDefensiveCopies() {
        var gesture = new CarriedShulkerRmbGesture();
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                SLOT_A, shulker(), "cursor-0");

        ItemStack liveA = new ItemStack(Items.COBBLESTONE, 11);
        ItemStack firstRead = gesture.projectedSource(SLOT_A, liveA);
        firstRead.shrink(5);
        assertStack(gesture.projectedSource(SLOT_A, liveA), Items.COBBLESTONE, 11);

        gesture.advanceSource(SLOT_A, new ItemStack(Items.COBBLESTONE, 4));
        gesture.advanceSource(SLOT_B, new ItemStack(Items.DIRT, 2));
        ItemStack projectedA = gesture.projectedSource(SLOT_A, liveA);
        projectedA.shrink(1);
        assertStack(gesture.projectedSource(SLOT_A, liveA), Items.COBBLESTONE, 4);
        assertStack(gesture.projectedSource(SLOT_B, new ItemStack(Items.DIRT, 20)), Items.DIRT, 2);

        gesture.advanceSource(SLOT_A, ItemStack.EMPTY);
        assertTrue(gesture.projectedSource(SLOT_A, liveA).isEmpty(),
                "A fully consumed source must not be resurrected from delayed live state");
    }

    @Test
    void cursorProjectionChainsFingerprintsWhileAcceptingKnownNetworkPredecessors() {
        var gesture = new CarriedShulkerRmbGesture();
        ItemStack initial = shulker();
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                SLOT_A, initial, "cursor-0");

        assertTrue(gesture.acceptsLiveFingerprint("cursor-0"));
        assertFalse(gesture.acceptsLiveFingerprint("unknown"));
        ItemStack exposed = gesture.projectedShulker();
        exposed.shrink(1);
        assertEquals(1, gesture.projectedShulker().getCount(),
                "The projected cursor is never exposed for mutation");

        ItemStack successor = shulker();
        gesture.advance(successor, "cursor-1");
        assertEquals("cursor-1", gesture.projectedFingerprint());
        assertTrue(gesture.acceptsLiveFingerprint("cursor-0"),
                "An in-flight server sync may still expose a known predecessor");
        assertTrue(gesture.acceptsLiveFingerprint("cursor-1"));
        assertFalse(gesture.acceptsLiveFingerprint(null));
    }

    @Test
    void invalidBeginAndResetClearModeCursorFingerprintsAndSourceProjections() {
        var gesture = new CarriedShulkerRmbGesture();
        gesture.begin(CarriedShulkerRmbGesture.Mode.INACTIVE, SLOT_A, shulker(), "cursor-0");
        assertFalse(gesture.isActive());
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                SLOT_A, ItemStack.EMPTY, "cursor-0");
        assertFalse(gesture.isActive());
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                SLOT_A, shulker(), null);
        assertFalse(gesture.isActive());

        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                SLOT_A, shulker(), "cursor-0");
        gesture.advance(shulker(), "cursor-1");
        gesture.advanceSource(SLOT_A, new ItemStack(Items.COBBLESTONE, 3));
        gesture.reset();

        assertEquals(CarriedShulkerRmbGesture.Mode.INACTIVE, gesture.mode());
        assertFalse(gesture.isActive());
        assertFalse(gesture.enter(SLOT_A));
        assertTrue(gesture.projectedShulker().isEmpty());
        assertEquals(null, gesture.projectedFingerprint());
        assertFalse(gesture.acceptsLiveFingerprint("cursor-0"));
        assertFalse(gesture.acceptsLiveFingerprint("cursor-1"));
        assertStack(gesture.projectedSource(SLOT_A, new ItemStack(Items.COBBLESTONE, 9)),
                Items.COBBLESTONE, 9);
    }

    private static ItemStack shulker() {
        return new ItemStack(Items.SHULKER_BOX, 1);
    }

    private static void assertStack(ItemStack stack, Item item, int count) {
        assertTrue(stack.is(item));
        assertEquals(count, stack.getCount());
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}

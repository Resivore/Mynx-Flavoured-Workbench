package dev.resivore.slotreservations.client;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic reference-style Slot identity and projected-authority state for C25. */
final class CarriedShulkerRmbGestureTest {
    private static final SimpleContainer INVENTORY = new SimpleContainer(2);
    private static final Slot HOVERED_A = new Slot(INVENTORY, 0, 0, 0);
    private static final Slot HOVERED_B = new Slot(INVENTORY, 1, 18, 0);

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.SHULKER_BOX);
        bindTestComponents(Items.COBBLESTONE);
        bindTestComponents(Items.STONE);
    }

    @Test
    void occupiedOriginActivatesOnlyInboundOwnershipAndDeduplicatesIt() {
        var gesture = new CarriedShulkerRmbGesture();
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                HOVERED_A, shulker(), "cursor-0");
        assertEquals(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER, gesture.mode());
        assertFalse(gesture.enter(HOVERED_A), "The directly dispatched occupied origin is already claimed");
        assertTrue(gesture.enter(HOVERED_B));
        assertEquals(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER, gesture.mode(),
                "A later slot never reclassifies the press-time direction");

    }

    @Test
    void activationRequiresSupportedCursorShulkerAndOccupiedOrigin() {
        INVENTORY.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
        assertTrue(CarriedShulkerRmbCollector.supportsOccupiedOrigin(shulker(), HOVERED_A));

        INVENTORY.setItem(0, ItemStack.EMPTY);
        assertFalse(CarriedShulkerRmbCollector.supportsOccupiedOrigin(shulker(), HOVERED_A),
                "Empty origin must preserve the established outbound RMB path");

        INVENTORY.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
        assertFalse(CarriedShulkerRmbCollector.supportsOccupiedOrigin(
                new ItemStack(Items.STONE, 1), HOVERED_A),
                "An ordinary carried stack must remain vanilla/Mouse Tweaks behavior");
    }

    @Test
    void everyActualHoveredSlotIdentityIsVisitedAtMostOnceUntilRelease() {
        var gesture = new CarriedShulkerRmbGesture();
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                HOVERED_A, shulker(), "cursor-0");

        assertFalse(gesture.enter(HOVERED_A));
        assertTrue(gesture.enter(HOVERED_B));
        assertFalse(gesture.enter(HOVERED_A),
                "Item Interactions keeps every visited Slot identity for the whole held drag");

        assertFalse(gesture.enter(null), "Blank space has no Slot identity to visit");
        assertFalse(gesture.enter(HOVERED_A), "Blank space does not forget an already visited Slot");
        Slot replacementView = new Slot(INVENTORY, 0, 0, 0);
        assertTrue(gesture.enter(replacementView),
                "Traversal identity is the actual hovered Slot object, not a numeric coordinate");
        assertEquals(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER, gesture.mode());
    }

    @Test
    void cursorProjectionChainsFingerprintsWhileAcceptingKnownNetworkPredecessors() {
        var gesture = new CarriedShulkerRmbGesture();
        ItemStack initial = shulker();
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                HOVERED_A, initial, "cursor-0");

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
        gesture.begin(CarriedShulkerRmbGesture.Mode.INACTIVE,
                HOVERED_A, shulker(), "cursor-0");
        assertFalse(gesture.isActive());
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                HOVERED_A, ItemStack.EMPTY, "cursor-0");
        assertFalse(gesture.isActive());
        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                HOVERED_A, shulker(), null);
        assertFalse(gesture.isActive());

        gesture.begin(CarriedShulkerRmbGesture.Mode.INVENTORY_TO_SHULKER,
                HOVERED_A, shulker(), "cursor-0");
        gesture.advance(shulker(), "cursor-1");
        gesture.reset();

        assertEquals(CarriedShulkerRmbGesture.Mode.INACTIVE, gesture.mode());
        assertFalse(gesture.isActive());
        assertFalse(gesture.enter(HOVERED_A));
        assertTrue(gesture.projectedShulker().isEmpty());
        assertEquals(null, gesture.projectedFingerprint());
        assertFalse(gesture.acceptsLiveFingerprint("cursor-0"));
        assertFalse(gesture.acceptsLiveFingerprint("cursor-1"));
    }

    private static ItemStack shulker() {
        return new ItemStack(Items.SHULKER_BOX, 1);
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}

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

/** Deterministic press-time mode, lifetime, and virtual-cell traversal contract. */
final class MouseTweaksRmbGestureTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.COBBLESTONE);
        bindTestComponents(Items.DIRT);
        bindTestComponents(Items.BUNDLE);
    }

    @Test void occupiedPressLatchesCollectionEvenAfterAnEmptyCellIsEntered() {
        var gesture = new MouseTweaksRmbGesture();
        gesture.begin(MouseTweaksRmbGesture.selectMode(true, false),
                MouseTweaksRmbGesture.OriginRegion.PANEL, 3);

        assertEquals(MouseTweaksRmbGesture.Mode.COLLECTION_SOURCE, gesture.mode());
        assertEquals(MouseTweaksRmbGesture.OriginRegion.PANEL, gesture.originRegion());
        assertEquals(3, gesture.originPanelCell());
        assertTrue(gesture.enterPanelCell(3), "The direct panel press owns the initial source cell");
        assertFalse(gesture.enterPanelCell(3), "MT's first origin replay must not duplicate the direct press");
        assertTrue(gesture.enterPanelCell(4), "Entering a later empty cell is one distinct event");
        gesture.leavePanelCell();
        gesture.markUnprojectedNativeBoundary();
        assertTrue(gesture.takeShadowNeedsLiveCarried(),
                "A native action before any panel transaction arms one live sample");
        assertTrue(gesture.enterPanelCell(5),
                "A native crossing leaves and permits a later panel re-entry");
        assertEquals(MouseTweaksRmbGesture.Mode.COLLECTION_SOURCE, gesture.mode(),
                "Later cells and ordinary-slot crossings never reclassify the gesture");
    }

    @Test void emptyPressWithCarriedStackLatchesDepositAcrossOccupiedCellsAndMenuBoundary() {
        var gesture = new MouseTweaksRmbGesture();
        gesture.begin(MouseTweaksRmbGesture.selectMode(false, true),
                MouseTweaksRmbGesture.OriginRegion.MENU, -1);

        assertEquals(MouseTweaksRmbGesture.Mode.DEPOSIT, gesture.mode());
        assertEquals(MouseTweaksRmbGesture.OriginRegion.MENU, gesture.originRegion());
        assertTrue(gesture.takeShadowNeedsLiveCarried(),
                "Ordinary-menu origins re-sample the cursor before the first panel destination");
        assertTrue(gesture.enterPanelCell(7));
        assertEquals(MouseTweaksRmbGesture.Mode.DEPOSIT, gesture.mode(),
                "An occupied destination cannot flip deposit into collection");
        gesture.leavePanelCell();
        assertTrue(gesture.enterPanelCell(8));
        assertEquals(MouseTweaksRmbGesture.Mode.DEPOSIT, gesture.mode());
    }

    @Test void nativePlaceOneUsesRelativeDeltaAndFailsClosedAtExhaustionOrUnsafeTargets() {
        SimpleContainer container = new SimpleContainer(3);
        Slot empty = new Slot(container, 0, 0, 0);
        Slot full = new Slot(container, 1, 0, 0);
        Slot incompatible = new Slot(container, 2, 0, 0);
        container.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        container.setItem(2, new ItemStack(Items.DIRT, 1));

        ItemStack projected = new ItemStack(Items.COBBLESTONE, 14);
        ItemStack liveBefore = new ItemStack(Items.COBBLESTONE, 16);
        assertTrue(MouseTweaksRmbGesture.canProjectNativePlaceOne(projected, liveBefore, empty));
        assertFalse(MouseTweaksRmbGesture.canProjectNativePlaceOne(projected, liveBefore, full));
        assertFalse(MouseTweaksRmbGesture.canProjectNativePlaceOne(projected, liveBefore, incompatible));
        ItemStack bundle = new ItemStack(Items.BUNDLE, 1);
        assertFalse(MouseTweaksRmbGesture.canProjectNativePlaceOne(bundle.copy(), bundle, empty),
                "Bundle/component-changing RMB behavior is outside the place-one projection");
        assertTrue(MouseTweaksRmbGesture.applyNativeCursorDelta(
                projected, liveBefore, new ItemStack(Items.COBBLESTONE, 15)));
        assertEquals(13, projected.getCount(),
                "A live 16->15 delta applies to projected 14 as 14->13, never as an absolute copy");
        assertTrue(MouseTweaksRmbGesture.applyNativeCursorDelta(
                projected, new ItemStack(Items.COBBLESTONE, 15),
                new ItemStack(Items.COBBLESTONE, 14)));
        assertEquals(12, projected.getCount(), "Successive native deltas accumulate relatively");
        assertFalse(MouseTweaksRmbGesture.canProjectNativePlaceOne(
                new ItemStack(Items.COBBLESTONE, 5),
                new ItemStack(Items.COBBLESTONE, 4), empty),
                "A live cursor below the projection violates the authority invariant");

        ItemStack lastProjected = new ItemStack(Items.COBBLESTONE, 1);
        assertTrue(MouseTweaksRmbGesture.applyNativeCursorDelta(
                lastProjected, new ItemStack(Items.COBBLESTONE, 2),
                new ItemStack(Items.COBBLESTONE, 1)));
        assertTrue(lastProjected.isEmpty());
        assertFalse(MouseTweaksRmbGesture.canProjectNativePlaceOne(
                lastProjected, new ItemStack(Items.COBBLESTONE, 1), empty),
                "An exhausted projection suppresses a stale-live native click");

        ItemStack unchanged = new ItemStack(Items.COBBLESTONE, 8);
        assertTrue(MouseTweaksRmbGesture.applyNativeCursorDelta(
                unchanged, new ItemStack(Items.COBBLESTONE, 10),
                new ItemStack(Items.COBBLESTONE, 10)));
        assertEquals(8, unchanged.getCount(), "A native no-op retains the projection");
        assertFalse(MouseTweaksRmbGesture.applyNativeCursorDelta(
                unchanged, new ItemStack(Items.COBBLESTONE, 10),
                new ItemStack(Items.DIRT, 1)), "A swap or component change must fail closed");
        assertFalse(MouseTweaksRmbGesture.applyNativeCursorDelta(
                unchanged, new ItemStack(Items.COBBLESTONE, 10),
                new ItemStack(Items.COBBLESTONE, 8)), "A drop larger than one must fail closed");
        assertFalse(MouseTweaksRmbGesture.applyNativeCursorDelta(
                unchanged, new ItemStack(Items.COBBLESTONE, 10),
                new ItemStack(Items.COBBLESTONE, 11)), "A cursor increase must fail closed");
        assertEquals(8, unchanged.getCount());
    }

    @Test void virtualTraversalMatchesMouseTweaksIdentityDeduplicationAndReentry() {
        var gesture = new MouseTweaksRmbGesture();
        gesture.begin(MouseTweaksRmbGesture.Mode.DEPOSIT,
                MouseTweaksRmbGesture.OriginRegion.PANEL, 1);

        assertTrue(gesture.enterPanelCell(1));
        assertFalse(gesture.enterPanelCell(1), "Stationary drag events do not spam the same cell");
        assertTrue(gesture.enterPanelCell(2));
        assertFalse(gesture.enterPanelCell(2));
        assertTrue(gesture.enterPanelCell(1), "B -> A re-entry is one new MT 2.31 action");
        gesture.leavePanelCell();
        assertTrue(gesture.enterPanelCell(1), "Leaving to an ordinary/null slot permits later re-entry");
    }

    @Test void providerAndPostUpstreamFallbackShareOneCellIdentityHandshake() {
        var gesture = new MouseTweaksRmbGesture();
        gesture.begin(MouseTweaksRmbGesture.Mode.DEPOSIT,
                MouseTweaksRmbGesture.OriginRegion.MENU, -1);

        assertTrue(gesture.enterPanelCell(6), "The provider may own the destination first");
        assertFalse(gesture.enterPanelCell(6),
                "The later screen-drag fallback must not duplicate a provider-owned action");
        assertTrue(gesture.enterPanelCell(7), "A new destination remains actionable");
    }

    @Test void approachRetentionRequiresCarriedStackButActiveGestureSurvivesExhaustion() {
        var gesture = new MouseTweaksRmbGesture();

        assertTrue(gesture.retainPanel(true, true),
                "One armed carried-cursor approach may bridge from the host to a menu origin");
        assertFalse(gesture.retainPanel(true, false),
                "A non-RMB action which empties the cursor must not leave the panel sticky");

        gesture.begin(MouseTweaksRmbGesture.Mode.DEPOSIT,
                MouseTweaksRmbGesture.OriginRegion.MENU, -1);
        assertTrue(gesture.retainPanel(false, false),
                "An active deposit remains latched after the carried stack is exhausted");
        gesture.reset();
        assertFalse(gesture.retainPanel(false, true),
                "A released gesture with no approach bridge restores normal panel lifetime");
    }

    @Test void releaseCloseOrStaleCancellationResetsEveryModeAndProjectionFlag() {
        var gesture = new MouseTweaksRmbGesture();
        gesture.begin(MouseTweaksRmbGesture.Mode.DEPOSIT,
                MouseTweaksRmbGesture.OriginRegion.PANEL, 9);
        gesture.markPanelActionDispatched();
        gesture.blockUntilRelease();

        assertTrue(gesture.isActive(),
                "Invalidation keeps ownership so held RMB cannot fall through to generic extraction");
        assertTrue(gesture.isBlockedUntilRelease());
        assertEquals(MouseTweaksRmbGesture.Mode.DEPOSIT, gesture.mode());
        assertFalse(gesture.enterPanelCell(10));
        gesture.reset();

        assertFalse(gesture.isActive());
        assertFalse(gesture.isBlockedUntilRelease());
        assertEquals(MouseTweaksRmbGesture.Mode.INACTIVE, gesture.mode());
        assertEquals(MouseTweaksRmbGesture.OriginRegion.NONE, gesture.originRegion());
        assertEquals(-1, gesture.originPanelCell());
        assertFalse(gesture.takeShadowNeedsLiveCarried());
        assertFalse(gesture.hasDispatchedPanelAction());
        assertFalse(gesture.enterPanelCell(9));
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}

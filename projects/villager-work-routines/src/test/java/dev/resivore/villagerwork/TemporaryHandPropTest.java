package dev.resivore.villagerwork;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryHandPropTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000041");
    private static final UUID ACTION = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Items.SHEARS.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 1)
                .set(DataComponents.MAX_DAMAGE, 238)
                .build());
        Items.FISHING_ROD.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 1)
                .set(DataComponents.MAX_DAMAGE, 64)
                .build());
        for (var item : new net.minecraft.world.item.Item[] { Items.EMERALD, Items.DIAMOND, Items.STICK })
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
    }

    @Test void emptyOriginalActivatesAZeroDropMarkedOverlay() {
        TemporaryHandProp prop = TemporaryHandProp.begin(
                OWNER, ACTION, Items.SHEARS, ItemStack.EMPTY, 0.085F);

        assertTrue(prop.originalState().stack().isEmpty());
        assertEquals(0.085F, prop.originalState().dropChance());
        TemporaryHandProp.Overlay overlay = prop.overlay();
        assertTrue(overlay.stack().is(Items.SHEARS));
        assertEquals(1, overlay.stack().getCount());
        assertEquals(TemporaryHandProp.SYNTHETIC_DROP_CHANCE, overlay.dropChance());
        assertTrue(prop.owns(overlay.stack()));
        TemporaryHandProp.Restore restore = prop.restore(overlay.stack().copy(), overlay.dropChance());
        assertTrue(restore.applyRestoration());
        assertTrue(restore.handState().stack().isEmpty());
        assertEquals(0.085F, restore.handState().dropChance());
    }

    @Test void occupiedOriginalIsCopiedExactlyAndRestoredWithItsDropChance() {
        ItemStack occupied = named(Items.EMERALD, 7, "legitimate work item");
        ItemStack expected = occupied.copy();
        TemporaryHandProp prop = TemporaryHandProp.begin(
                OWNER, ACTION, Items.SHEARS, occupied, 0.42F);
        occupied.setCount(1);
        occupied.set(DataComponents.CUSTOM_NAME, Component.literal("changed after capture"));

        TemporaryHandProp.Restore restore = prop.restore(prop.overlay().stack().copy(), 0.0F);

        assertTrue(restore.applyRestoration());
        assertEquals(TemporaryHandProp.RestoreKind.ORIGINAL_RESTORED, restore.kind());
        assertTrue(ItemStack.matches(expected, restore.handState().stack()));
        assertNotSame(expected, restore.handState().stack());
        assertEquals(0.42F, restore.handState().dropChance());
        assertFalse(prop.owns(restore.handState().stack()), "the synthetic marker must not leak");
    }

    @Test void copiedAndReconstructedPresentationsAreRecognizedSemantically() {
        TemporaryHandProp prop = TemporaryHandProp.begin(
                OWNER, ACTION, Items.SHEARS, ItemStack.EMPTY, 0.085F);
        ItemStack shown = prop.overlay().stack();
        ItemStack copied = shown.copy();
        ItemStack reconstructed = new ItemStack(Items.SHEARS);
        reconstructed.set(DataComponents.CUSTOM_DATA, shown.get(DataComponents.CUSTOM_DATA));

        assertNotSame(shown, copied);
        assertTrue(prop.owns(copied));
        assertTrue(prop.owns(reconstructed));
        TemporaryHandProp otherAction = TemporaryHandProp.begin(
                OWNER, UUID.fromString("00000000-0000-0000-0000-000000000043"),
                Items.SHEARS, ItemStack.EMPTY, 0.085F);
        assertFalse(otherAction.owns(copied));
        TemporaryHandProp otherOwner = TemporaryHandProp.begin(
                UUID.fromString("00000000-0000-0000-0000-000000000044"), ACTION,
                Items.SHEARS, ItemStack.EMPTY, 0.085F);
        assertFalse(otherOwner.owns(copied));
        ItemStack wrongItem = new ItemStack(Items.STICK);
        wrongItem.set(DataComponents.CUSTOM_DATA, shown.get(DataComponents.CUSTOM_DATA));
        assertFalse(prop.owns(wrongItem));
    }

    @Test void externalReplacementRebasesThenRestoresOnlyTheLatestLegitimateState() {
        ItemStack original = named(Items.EMERALD, 2, "old legitimate state");
        TemporaryHandProp prop = TemporaryHandProp.begin(
                OWNER, ACTION, Items.SHEARS, original, 0.25F);
        ItemStack replacement = named(Items.DIAMOND, 4, "new legitimate state");
        ItemStack expectedReplacement = replacement.copy();

        TemporaryHandProp.Reconcile reconcile = prop.reconcile(replacement, 0.73F);
        replacement.setCount(1);

        assertEquals(TemporaryHandProp.ReconcileKind.EXTERNAL_REPLACEMENT_REBASED, reconcile.kind());
        assertTrue(reconcile.applyOverlay());
        assertTrue(prop.owns(reconcile.overlay().stack()));
        assertEquals(TemporaryHandProp.SYNTHETIC_DROP_CHANCE, reconcile.overlay().dropChance());
        assertTrue(ItemStack.matches(expectedReplacement, prop.restorationState().stack()));
        assertEquals(0.73F, prop.restorationState().dropChance());

        TemporaryHandProp.Restore restore = prop.restore(reconcile.overlay().stack().copy(), 0.0F);
        assertEquals(TemporaryHandProp.RestoreKind.REBASED_STATE_RESTORED, restore.kind());
        assertTrue(restore.applyRestoration());
        assertTrue(ItemStack.matches(expectedReplacement, restore.handState().stack()));
        assertEquals(0.73F, restore.handState().dropChance());
        assertFalse(ItemStack.matches(original, restore.handState().stack()),
                "rebasing must not duplicate or resurrect the displaced original");
    }

    @Test void externalClearRebasesThenRestoresTheClearAndItsDropChance() {
        TemporaryHandProp prop = TemporaryHandProp.begin(
                OWNER, ACTION, Items.SHEARS, new ItemStack(Items.EMERALD, 3), 0.25F);

        TemporaryHandProp.Reconcile reconcile = prop.reconcile(ItemStack.EMPTY, 0.11F);
        assertEquals(TemporaryHandProp.ReconcileKind.EXTERNAL_CLEAR_REBASED, reconcile.kind());
        assertTrue(reconcile.applyOverlay());
        assertTrue(prop.restorationState().stack().isEmpty());
        assertEquals(0.11F, prop.restorationState().dropChance());

        TemporaryHandProp.Restore restore = prop.restore(reconcile.overlay().stack(), 0.0F);
        assertTrue(restore.applyRestoration());
        assertTrue(restore.handState().stack().isEmpty());
        assertEquals(0.11F, restore.handState().dropChance());
    }

    @Test void reconcileRecognizesAStackCopyAndRepairsOnlySyntheticDropChance() {
        TemporaryHandProp prop = TemporaryHandProp.begin(
                OWNER, ACTION, Items.SHEARS, new ItemStack(Items.EMERALD), 0.35F);
        ItemStack copiedPresentation = prop.overlay().stack().copy();

        TemporaryHandProp.Reconcile intact = prop.reconcile(copiedPresentation, 0.0F);
        assertEquals(TemporaryHandProp.ReconcileKind.PRESENTATION_INTACT, intact.kind());
        assertFalse(intact.applyOverlay());
        assertEquals(0, prop.rebaseCount());

        TemporaryHandProp.Reconcile corrected = prop.reconcile(copiedPresentation, 0.9F);
        assertEquals(TemporaryHandProp.ReconcileKind.SYNTHETIC_DROP_CHANCE_CORRECTED, corrected.kind());
        assertTrue(corrected.applyOverlay());
        assertEquals(0.0F, corrected.overlay().dropChance());
        assertEquals(0, prop.rebaseCount());
        assertEquals(0.35F, corrected.restorationState().dropChance());
    }

    @Test void cleanupDoesNotOverwriteAnUnobservedExternalChangeOrDropChance() {
        TemporaryHandProp prop = TemporaryHandProp.begin(
                OWNER, ACTION, Items.SHEARS, new ItemStack(Items.EMERALD, 2), 0.25F);
        ItemStack external = named(Items.DIAMOND, 5, "unobserved external state");

        TemporaryHandProp.Restore restore = prop.restore(external, 0.88F);

        assertEquals(TemporaryHandProp.RestoreKind.EXTERNAL_STATE_LEFT_UNTOUCHED, restore.kind());
        assertFalse(restore.applyRestoration());
        assertTrue(ItemStack.matches(external, restore.handState().stack()));
        assertEquals(0.88F, restore.handState().dropChance());
    }

    @Test void aForeignSyntheticMarkerNeverBecomesRestorationState() {
        TemporaryHandProp first = TemporaryHandProp.begin(
                OWNER, ACTION, Items.SHEARS, ItemStack.EMPTY, 0.085F);
        ItemStack foreignPresentation = first.overlay().stack();
        TemporaryHandProp second = TemporaryHandProp.begin(
                UUID.fromString("00000000-0000-0000-0000-000000000044"),
                UUID.fromString("00000000-0000-0000-0000-000000000045"),
                Items.SHEARS, new ItemStack(Items.EMERALD), 0.3F);

        TemporaryHandProp.Reconcile reconcile = second.reconcile(foreignPresentation, 0.0F);

        assertEquals(TemporaryHandProp.ReconcileKind.FOREIGN_VWR_PRESENTATION, reconcile.kind());
        assertTrue(reconcile.applyOverlay());
        assertTrue(second.owns(reconcile.overlay().stack()));
        assertTrue(second.restorationState().stack().is(Items.EMERALD));

        TemporaryHandProp.Restore directCleanup = second.restore(foreignPresentation, 0.0F);
        assertEquals(TemporaryHandProp.RestoreKind.FOREIGN_VWR_PRESENTATION_QUARANTINED,
                directCleanup.kind());
        assertTrue(directCleanup.applyRestoration());
        assertTrue(directCleanup.handState().stack().is(Items.EMERALD));
        assertEquals(0.3F, directCleanup.handState().dropChance());
    }

    @Test void switchingPropTypesStartsFromTheStateRestoredByThePriorTransaction() {
        ItemStack occupied = named(Items.EMERALD, 6, "occupied before fishing");
        TemporaryHandProp fishing = TemporaryHandProp.begin(
                OWNER, ACTION, Items.FISHING_ROD, occupied, 0.61F);
        TemporaryHandProp.Restore between = fishing.restore(fishing.overlay().stack(), 0.0F);
        assertTrue(between.applyRestoration());

        TemporaryHandProp shearing = TemporaryHandProp.begin(
                OWNER, UUID.fromString("00000000-0000-0000-0000-000000000046"),
                Items.SHEARS, between.handState().stack(), between.handState().dropChance());
        TemporaryHandProp.Restore finalRestore = shearing.restore(shearing.overlay().stack(), 0.0F);

        assertTrue(finalRestore.applyRestoration());
        assertTrue(ItemStack.matches(occupied, finalRestore.handState().stack()));
        assertEquals(0.61F, finalRestore.handState().dropChance());
    }

    @Test void slotCreatesOnceAndRetainsTheSameActionForAnIntactFishingCycle() {
        TemporaryHandProp.Slot slot = new TemporaryHandProp.Slot();
        TemporaryHandProp first = slot.beginIfAbsent(OWNER, ACTION, Items.FISHING_ROD, ItemStack.EMPTY, 0.2F);
        TemporaryHandProp retained = slot.beginIfAbsent(OWNER,
                UUID.fromString("00000000-0000-0000-0000-000000000047"), Items.FISHING_ROD,
                new ItemStack(Items.DIAMOND), 0.8F);

        assertTrue(slot.isCurrent(first));
        assertEquals(ACTION, first.actionId());
        assertTrue(first == retained, "normal ticks must retain one action transaction");
    }

    @Test void saveSuspensionRestoresTheSameActionWithoutRebasingOrSyntheticPersistence() {
        TemporaryHandProp.Slot slot = new TemporaryHandProp.Slot();
        ItemStack original = named(Items.EMERALD, 2, "real hand state");
        TemporaryHandProp prop = slot.beginIfAbsent(OWNER, ACTION, Items.FISHING_ROD, original, 0.35F);
        TemporaryHandProp.Overlay shown = prop.overlay();

        TemporaryHandProp.Restore saveCleanup = prop.restore(shown.stack(), shown.dropChance());
        assertTrue(saveCleanup.applyRestoration());
        assertTrue(ItemStack.matches(original, saveCleanup.handState().stack()));
        assertTrue(slot.suspendIfCurrent(prop));
        assertTrue(slot.isSuspended(prop));

        TemporaryHandProp.Reconcile resume = prop.resume(saveCleanup.handState().stack(),
                saveCleanup.handState().dropChance());
        assertEquals(TemporaryHandProp.ReconcileKind.SUSPENDED_PRESENTATION_REAPPLIED, resume.kind());
        assertEquals(ACTION, prop.actionId());
        assertEquals(0, prop.rebaseCount());
        assertTrue(prop.owns(resume.overlay().stack()));
        assertTrue(slot.resumeIfCurrent(prop));
        assertFalse(slot.isSuspended(prop));
    }

    @Test void suspendedExternalReplacementIsRebasedBeforeTheOriginalActionIsReapplied() {
        TemporaryHandProp prop = TemporaryHandProp.begin(OWNER, ACTION, Items.FISHING_ROD,
                new ItemStack(Items.EMERALD), 0.2F);
        ItemStack replacement = named(Items.DIAMOND, 3, "save-time replacement");

        TemporaryHandProp.Reconcile resume = prop.resume(replacement, 0.71F);

        assertEquals(TemporaryHandProp.ReconcileKind.EXTERNAL_REPLACEMENT_REBASED, resume.kind());
        assertEquals(ACTION, prop.actionId());
        assertTrue(ItemStack.matches(replacement, prop.restorationState().stack()));
        assertTrue(prop.owns(resume.overlay().stack()));
    }

    @Test void staleAndDoubleCleanupCannotClearANewerTransaction() {
        TemporaryHandProp.Slot slot = new TemporaryHandProp.Slot();
        TemporaryHandProp first = slot.beginIfAbsent(OWNER, ACTION, Items.SHEARS, ItemStack.EMPTY, 0.1F);
        assertTrue(slot.clearIfCurrent(first));
        assertFalse(slot.clearIfCurrent(first), "double cleanup is harmless");

        TemporaryHandProp newer = slot.beginIfAbsent(OWNER,
                UUID.fromString("00000000-0000-0000-0000-000000000048"), Items.FISHING_ROD,
                new ItemStack(Items.EMERALD), 0.3F);
        assertFalse(slot.clearIfCurrent(first), "a stale cleanup may not clobber a newer action");
        assertTrue(slot.isCurrent(newer));
        assertTrue(newer.owns(newer.overlay().stack()));
    }

    private static ItemStack named(net.minecraft.world.item.Item item, int count, String name) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        CompoundTag unrelated = new CompoundTag();
        unrelated.putString("fixture:legitimate", name);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(unrelated));
        return stack;
    }
}

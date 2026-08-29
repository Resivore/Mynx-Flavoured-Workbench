package dev.resivore.carriedrouting;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreativeMenuListenerAlignmentTest {
    private static final int VANILLA_PICKER_SLOTS = 54;
    private static final int EXPANDED_INVENTORY_VIEW_SLOTS = 76;

    private static final class MenuModel {
        private final List<Integer> slots = new ArrayList<>();
        private final List<Integer> lastSlots = new ArrayList<>();
        private final List<Integer> remoteSlots = new ArrayList<>();

        private MenuModel() {
            for (int i = 0; i < VANILLA_PICKER_SLOTS; i++) addTracked();
            slots.clear();
        }

        private void addDirect() {
            slots.add(slots.size());
        }

        private void addTracked() {
            slots.add(slots.size());
            lastSlots.add(0);
            remoteSlots.add(0);
        }

        private void addWithCompatibilityFix() {
            if (slots.size() >= lastSlots.size()) addTracked();
            else addDirect();
        }

        private void broadcast() {
            for (int i = 0; i < slots.size(); i++) {
                lastSlots.get(i);
                remoteSlots.get(i);
            }
        }
    }

    private static final class ContainerItemMutationModel {
        private final List<Integer> slots = new ArrayList<>();
        private final List<Integer> lastSlots = new ArrayList<>();
        private final List<Integer> remoteSlots = new ArrayList<>();
        private String liveCarrierIdentity = "carrier{contents=before}";
        private String synchronizedCarrierIdentity = liveCarrierIdentity;
        private int completedActions;

        private ContainerItemMutationModel(int liveSlotCount, int snapshotCount) {
            for (int i = 0; i < liveSlotCount; i++) slots.add(i);
            for (int i = 0; i < snapshotCount; i++) {
                lastSlots.add(0);
                remoteSlots.add(0);
            }
        }

        private void growMissingSnapshots() {
            while (lastSlots.size() < slots.size()) lastSlots.add(0);
            while (remoteSlots.size() < slots.size()) remoteSlots.add(0);
        }

        private void mutateAndSynchronize(String newIdentity) {
            liveCarrierIdentity = newIdentity;
            for (int i = 0; i < slots.size(); i++) {
                lastSlots.get(i);
                remoteSlots.get(i);
            }
            synchronizedCarrierIdentity = liveCarrierIdentity;
            completedActions++;
        }
    }

    @Test
    void directCreativeInventoryExpansionReproducesThe54Vs55Failure() {
        MenuModel broken = new MenuModel();
        for (int i = 0; i < EXPANDED_INVENTORY_VIEW_SLOTS; i++) broken.addDirect();

        IndexOutOfBoundsException failure = assertThrows(IndexOutOfBoundsException.class, broken::broadcast);
        assertTrue(failure.getMessage().contains("54"));
        assertEquals(VANILLA_PICKER_SLOTS, broken.lastSlots.size());
        assertEquals(EXPANDED_INVENTORY_VIEW_SLOTS, broken.slots.size());
    }

    @Test
    void trackedSuccessorExpansionKeepsBothSnapshotsSafe() {
        MenuModel fixed = new MenuModel();
        for (int i = 0; i < EXPANDED_INVENTORY_VIEW_SLOTS; i++) fixed.addWithCompatibilityFix();

        assertDoesNotThrow(fixed::broadcast);
        assertEquals(EXPANDED_INVENTORY_VIEW_SLOTS, fixed.lastSlots.size());
        assertEquals(EXPANDED_INVENTORY_VIEW_SLOTS, fixed.remoteSlots.size());
    }

    @Test
    void liveInventoryMenuReproducesTheDemonstrated76Vs75MutationFailure() {
        ContainerItemMutationModel broken = new ContainerItemMutationModel(76, 75);

        IndexOutOfBoundsException failure = assertThrows(
                IndexOutOfBoundsException.class,
                () -> broken.mutateAndSynchronize("shulker{contents=mutated}")
        );
        assertTrue(failure.getMessage().contains("75"));
        assertEquals("carrier{contents=before}", broken.synchronizedCarrierIdentity);
        assertEquals(0, broken.completedActions);
    }

    @Test
    void successorCompletesBundleMutationAndTheImmediatelyFollowingAction() {
        ContainerItemMutationModel fixed = new ContainerItemMutationModel(76, 75);
        fixed.growMissingSnapshots();

        assertDoesNotThrow(() -> fixed.mutateAndSynchronize("bundle{contents=after-first-click}"));
        assertDoesNotThrow(() -> fixed.mutateAndSynchronize("unrelated-slot{after-next-click}"));
        assertEquals(2, fixed.completedActions);
        assertEquals("unrelated-slot{after-next-click}", fixed.synchronizedCarrierIdentity);
    }

    @Test
    void successorKeepsMutatedShulkerIdentityAuthoritativeThroughSynchronization() {
        ContainerItemMutationModel fixed = new ContainerItemMutationModel(76, 75);
        fixed.growMissingSnapshots();

        fixed.mutateAndSynchronize("shulker{contents=exact-mutated-stack}");

        assertEquals(fixed.liveCarrierIdentity, fixed.synchronizedCarrierIdentity);
        assertEquals(1, fixed.completedActions);
    }

    @Test
    void productionMixinUsesNormalAddSlotOnlyPastTrackedCapacity() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        String mixin = Files.readString(root.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/CreativeModeInventoryScreenMixin.java"
        ));
        String tracker = Files.readString(root.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/CreativeModeItemPickerMenuMixin.java"
        ));

        assertTrue(mixin.contains("receiver != (Object) menu.slots"));
        assertTrue(mixin.contains("menuIndex >= this.carriedRouting$trackedSlotCapacity"));
        assertTrue(mixin.contains("carriedRouting$addTrackedSlot(slot)"));
        assertTrue(tracker.contains("return this.addSlot(slot)"));
        assertTrue(!tracker.contains("@Shadow"));
        assertTrue(!tracker.contains("@Accessor"));
        assertTrue(!tracker.contains("@Invoker"));
    }

    @Test
    void productionSynchronizationGuardGrowsBothExactSnapshotTypes() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        String mixin = Files.readString(root.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/AbstractContainerMenuMixin.java"
        ));
        String accessor = Files.readString(root.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/AbstractContainerMenuSnapshotAccessor.java"
        ));
        String config = Files.readString(root.resolve(
                "src/main/resources/carried_container_auto_routing.mixins.json"
        ));

        assertTrue(mixin.contains("method = {\"broadcastChanges\", \"broadcastFullState\", \"sendAllDataToRemote\"}"));
        assertTrue(mixin.contains("method = \"transferState\""));
        assertTrue(mixin.contains("getLastSlots().size() < requiredSize"));
        assertTrue(mixin.contains("getRemoteSlots().size() < requiredSize"));
        assertTrue(mixin.contains("synchronizer.createSlot()"));
        assertTrue(accessor.contains("NonNullList<ItemStack> carriedRouting$getLastSlots()"));
        assertTrue(accessor.contains("NonNullList<RemoteSlot> carriedRouting$getRemoteSlots()"));
        assertFalse(accessor.contains("@Shadow"));
        assertTrue(config.contains("AbstractContainerMenuSnapshotAccessor"));
    }

    @Test
    void runtimeBridgeIsOutsideTheDeclaredMixinPackage() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        Path unsafeBridge = root.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/CreativeMenuSlotTracker.java"
        );
        Path safeBridge = root.resolve(
                "src/main/java/dev/resivore/carriedrouting/compat/CreativeMenuSlotTracker.java"
        );
        String pickerMixin = Files.readString(root.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/CreativeModeItemPickerMenuMixin.java"
        ));
        String screenMixin = Files.readString(root.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/CreativeModeInventoryScreenMixin.java"
        ));

        assertFalse(Files.exists(unsafeBridge),
                "A normally loaded helper in the declared Mixin package crashes production startup");
        assertTrue(Files.exists(safeBridge));
        assertTrue(Files.readString(safeBridge)
                .contains("package dev.resivore.carriedrouting.compat;"));
        assertTrue(pickerMixin.contains(
                "import dev.resivore.carriedrouting.compat.CreativeMenuSlotTracker;"));
        assertTrue(screenMixin.contains(
                "import dev.resivore.carriedrouting.compat.CreativeMenuSlotTracker;"));
    }
}

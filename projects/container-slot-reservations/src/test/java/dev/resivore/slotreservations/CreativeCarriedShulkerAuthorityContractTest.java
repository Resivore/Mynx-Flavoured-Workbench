package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused C27 contract coverage for the Creative-only cursor-authority bridge. */
final class CreativeCarriedShulkerAuthorityContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void creativeVisibleCursorBridgesOnlyAnEmptyServerInventoryMenuCursor() throws IOException {
        String authority = source("CreativeCarriedShulkerAuthority.java");
        String actions = source("CarriedShulkerInventoryActions.java");

        assertTrue(authority.contains("!serverCarried.isEmpty() || !infiniteMaterials || !inventoryMenu || menuSlot != -1"));
        assertTrue(authority.contains("isExactSupported(creativeCursor, fingerprint, registries)"));
        assertTrue(actions.contains("action.creativeCursor()"));
        assertTrue(actions.contains("player.hasInfiniteMaterials(), menu == player.inventoryMenu, action.menuSlot()"));
    }

    @Test
    void creativeInboundTransferStillUsesTheSharedAuthoritativePlanAndMutation() throws IOException {
        String actions = source("CarriedShulkerInventoryActions.java");
        assertTrue(actions.contains("resolvePlayerInventorySlot("));
        assertTrue(actions.contains("ShulkerTransferPlanner.planInsertion(carried, incoming)"));
        assertTrue(actions.contains("ShulkerContextualTransfers.insertFromSlot(player, menu, source, carried)"));
        assertTrue(actions.contains("ShulkerHostResolver.removableSource(player, source)"));
    }

    @Test
    void severalCreativeTransfersPreserveTheF0ToF1ToF2FingerprintChain() throws IOException {
        String collector = source("client/CarriedShulkerRmbCollector.java");
        String authority = source("CreativeCarriedShulkerAuthority.java");
        assertTrue(collector.contains("GESTURE.advance(plan.shulker(), ShulkerHostFingerprint.of(plan.shulker()"));
        assertTrue(collector.contains("creativeFacade ? menu.getCarried().copy() : ItemStack.EMPTY"));
        assertTrue(collector.contains("if (creativeFacade) menu.setCarried(plan.shulker())"));
        assertTrue(authority.contains("isExactSupported(serverCarried, fingerprint, registries)"));
    }

    @Test
    void stalePredecessorOrSourceFingerprintFailsClosed() throws IOException {
        String actions = source("CarriedShulkerInventoryActions.java");
        assertTrue(actions.contains("if (cursor.isEmpty()) return false"));
        assertTrue(actions.contains("incoming.isEmpty()"));
        assertTrue(actions.contains("ShulkerHostFingerprint.of(incoming, player.registryAccess()).equals(action.sourceFingerprint())"));
    }

    @Test
    void acceptedCreativeMutationReturnsTheResultToTheVisibleCursor() throws IOException {
        String actions = source("CarriedShulkerInventoryActions.java");
        String client = source("client/ContainerSlotReservationsClient.java");
        String collector = source("client/CarriedShulkerRmbCollector.java");
        assertTrue(actions.contains("new CreativeCarriedShulkerSyncPayload("));
        assertTrue(client.contains("CreativeCarriedShulkerSyncPayload.TYPE"));
        assertTrue(collector.contains("acceptCreativeCursorSync"));
        assertTrue(collector.contains("menu.setCarried(returned.copy())"));
    }

    @Test
    void survivalAuthorityInventoryExtendedAndOutboundBehaviorRemainNarrow() throws IOException {
        String authority = source("CreativeCarriedShulkerAuthority.java");
        String actions = source("CarriedShulkerInventoryActions.java");
        String collector = source("client/CarriedShulkerRmbCollector.java");
        String slots = source("OrdinaryPlayerInventorySlots.java");
        assertTrue(authority.contains("return Optional.of(serverCarried)"));
        assertTrue(actions.contains("OrdinaryPlayerInventorySlots.isExactLiveSlot"));
        assertTrue(slots.contains("inventory.getNonEquipmentItems().size()"));
        assertTrue(collector.contains("target == null || !target.hasItem()"));
        assertFalse(collector.contains("SECONDARY_DEPOSIT"));
    }

    @Test
    void emptyStacksNeverReachFingerprintGenerationInTheNewBridgePaths() throws IOException {
        String authority = source("CreativeCarriedShulkerAuthority.java");
        String collector = source("client/CarriedShulkerRmbCollector.java");
        assertTrue(authority.contains("stack != null && !stack.isEmpty() && stack.getCount() == 1"));
        assertTrue(collector.contains("if (returned.isEmpty() || returned.getCount() != 1"));
        assertTrue(collector.contains("if (visible.isEmpty()) return"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/dev/resivore/slotreservations").resolve(relative));
    }
}

package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductionWiringContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path JAVA = ROOT.resolve("src/main/java/dev/resivore/slotreservations");

    @Test
    void resolverWhitelistsExactVanillaOwnersAndAllSeventeenShulkers() throws IOException {
        String resolver = source("SupportedContainerResolver.java");

        assertTrue(resolver.contains("blockEntity.getType() == BlockEntityTypes.CHEST"));
        assertTrue(resolver.contains("block == Blocks.CHEST"));
        assertTrue(resolver.contains("blockEntity.getType() == BlockEntityTypes.TRAPPED_CHEST"));
        assertTrue(resolver.contains("block == Blocks.TRAPPED_CHEST"));
        assertTrue(resolver.contains("blockEntity.getType() == BlockEntityTypes.BARREL"));
        assertTrue(resolver.contains("block == Blocks.BARREL"));
        assertTrue(resolver.contains("blockEntity.getType() == BlockEntityTypes.SHULKER_BOX"));
        assertTrue(resolver.contains("Blocks.DYED_SHULKER_BOX.asList()"));
        assertTrue(resolver.contains("blocks.add(Blocks.SHULKER_BOX)"));
        assertFalse(resolver.contains("EnderChestBlockEntity"));
        assertFalse(resolver.contains("AbstractChestBlock"),
                "Canary 1 must not silently opt arbitrary or modded chest families into persistence");
    }

    @Test
    void doubleChestMenuIndicesResolveToTheOwningPhysicalHalf() throws IOException {
        String resolver = source("SupportedContainerResolver.java");
        String accessor = source("mixin/CompoundContainerAccessor.java");

        assertTrue(resolver.contains("container instanceof CompoundContainer compound"));
        assertTrue(resolver.contains("containerSlotReservations$getFirst()"));
        assertTrue(resolver.contains("containerSlotReservations$getSecond()"));
        assertTrue(resolver.contains("slot < firstSize"));
        assertTrue(resolver.contains("resolvePhysical(first, slot)"));
        assertTrue(resolver.contains("resolvePhysical(second, slot - firstSize)"));
        assertTrue(accessor.contains("@Accessor(\"container1\")"));
        assertTrue(accessor.contains("@Accessor(\"container2\")"));
    }

    @Test
    void serverDerivesTemplatesFromItsLiveSlotOrCursorAfterContextValidation() throws IOException {
        String networking = source("ReservationNetworking.java");

        assertTrue(networking.contains("ItemStack physical = target.slot().getItem()"));
        assertTrue(networking.contains("ItemStack carried = target.menu().getCarried()"));
        assertTrue(networking.contains("case SLOT_STACK"));
        assertTrue(networking.contains("if (physical.isEmpty()) return false"));
        assertTrue(networking.contains("case CARRIED_STACK"));
        assertTrue(networking.contains("if (!physical.isEmpty() || carried.isEmpty()) return false"));
        assertTrue(networking.contains("target.resolvedSlot().blockEntity() instanceof ShulkerBoxBlockEntity"));
        assertTrue(networking.contains("!carried.getItem().canFitInsideContainerItems()"),
                "Server must reject impossible nested-container reservation templates for shulkers");
        assertTrue(networking.contains("case CLEAR_EMPTY"));
        assertTrue(networking.contains("menu.containerId == menuId"));
        assertTrue(networking.contains("menu.stillValid(player)"));
        assertTrue(networking.contains("slot.index != menuSlotIndex || !slot.isActive()"));
        assertFalse(networking.contains("payload.template()"));
        assertFalse(networking.contains("payload.stack()"));
    }

    @Test
    void menuLocalWrappersGateManualQuickMoveAndBundlePathsWithoutGlobalSlotRewrite() throws IOException {
        String chestMixin = source("mixin/ChestMenuMixin.java");
        String shulkerMixin = source("mixin/ShulkerBoxMenuMixin.java");
        String slot = source("menu/ReservationAwareSlot.java");
        String shulkerSlot = source("menu/ReservationAwareShulkerBoxSlot.java");
        String commonMixins = Files.readString(ROOT.resolve(
                "src/main/resources/container_slot_reservations.mixins.json"));

        assertTrue(chestMixin.contains("method = \"addChestGrid\""));
        assertTrue(chestMixin.contains("new ReservationAwareSlot(container, slot, x, y)"));
        assertTrue(shulkerMixin.contains("new ReservationAwareShulkerBoxSlot(container, slot, x, y)"));
        assertTrue(slot.contains("public boolean mayPlace(ItemStack incoming)"));
        assertTrue(slot.contains("super.mayPlace(incoming)"));
        assertTrue(slot.contains("ReservationStore.reservationAllows(resolved, incoming)"));
        assertTrue(shulkerSlot.contains("super.mayPlace(incoming)"),
                "The reservation gate must preserve vanilla shulker nesting restrictions");
        assertTrue(commonMixins.contains("\"ChestMenuMixin\""));
        assertTrue(commonMixins.contains("\"ShulkerBoxMenuMixin\""));
        assertFalse(Files.exists(JAVA.resolve("mixin/SlotMixin.java")),
                "Reservation admission must stay confined to supported menu slots");
    }

    @Test
    void hopperGateAndShulkerComponentRoundTripUseNarrowVanillaSeams() throws IOException {
        String containerMixin = source("mixin/RandomizableContainerBlockEntityMixin.java");
        String shulkerBlock = source("mixin/ShulkerBoxBlockMixin.java");
        String drops = source("ShulkerReservationDrops.java");
        String components = source("ModComponents.java");
        String commonMixins = Files.readString(ROOT.resolve(
                "src/main/resources/container_slot_reservations.mixins.json"));

        assertTrue(containerMixin.contains("public boolean canPlaceItem(int slot, ItemStack incoming)"));
        assertTrue(containerMixin.contains("SupportedContainerResolver.resolve(self, slot)"));
        assertTrue(containerMixin.contains("ReservationStore.reservationAllows(resolved, incoming)"));
        assertTrue(commonMixins.contains("\"RandomizableContainerBlockEntityMixin\""));
        assertTrue(commonMixins.contains("\"ShulkerBoxBlockMixin\""));
        assertTrue(shulkerBlock.contains("method = \"playerWillDestroy\""));
        assertTrue(drops.contains("drop.applyComponents(shulker.components())"));
        assertTrue(components.contains(".persistent(ReservationData.CODEC)"));
        assertTrue(components.contains(".networkSynchronized(ReservationData.STREAM_CODEC)"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(JAVA.resolve(relative));
    }
}

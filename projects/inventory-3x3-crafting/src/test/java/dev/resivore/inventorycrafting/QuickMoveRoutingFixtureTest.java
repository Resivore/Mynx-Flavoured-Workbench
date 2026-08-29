package dev.resivore.inventorycrafting;

import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.HOTBAR_END_EXCLUSIVE;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.ORDINARY_START;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.destinationMenuIds;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.syntheticSlot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Destination;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.DestinationRegion;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Owner;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.QuickMoveRouter;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Role;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Side;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.SlotDescriptor;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.TargetInventoryMenu;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.TransferPlan;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuickMoveRoutingFixtureTest {
    private static final Destination ORDINARY_DESTINATION =
            new Destination(DestinationRegion.ORDINARY, ORDINARY_START, HOTBAR_END_EXCLUSIVE);

    @Test
    void everyCraftInputUsesOnlyTheAcceptedOrdinaryRangeAsItsQuickMoveDestination() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);

        for (SlotDescriptor craftSource : menu.inputGridSlots()) {
            TransferPlan plan = QuickMoveRouter.plan(menu, craftSource, false);
            assertEquals(List.of(ORDINARY_DESTINATION), plan.destinations(),
                    "craft source " + craftSource.index() + " drifted from [9,72)");
            assertFalse(plan.reverse());
            assertFalse(plan.autoEquipAttempted());
            assertEquals(63, destinationMenuIds(plan).size());
            assertEquals(ORDINARY_START, destinationMenuIds(plan).stream().mapToInt(Integer::intValue).min().orElseThrow());
            assertEquals(HOTBAR_END_EXCLUSIVE - 1,
                    destinationMenuIds(plan).stream().mapToInt(Integer::intValue).max().orElseThrow());
        }
    }

    @Test
    void equippableIngredientsInAppendedCellsCannotFallThroughToAutoEquip() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);

        for (int menuId = 74; menuId <= 78; menuId++) {
            TransferPlan plan = QuickMoveRouter.plan(menu, menu.slot(menuId), true);
            assertEquals(List.of(ORDINARY_DESTINATION), plan.destinations(),
                    "appended equippable source " + menuId + " entered an equipment branch");
            assertFalse(plan.autoEquipAttempted());
            assertTrue(plan.destinations().stream().noneMatch(destination ->
                    destination.region() == DestinationRegion.EQUIPMENT
                            || destination.region() == DestinationRegion.OFFHAND
                            || destination.region() == DestinationRegion.TRINKETS));
        }
    }

    @Test
    void routingClassifiesCraftSourcesByContainerIdentityInsteadOfPublicOrdinalOrRole() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        SlotDescriptor craftOwnedAtUnrelatedOrdinal = syntheticSlot(
                900, menu.craftingContainer(), Owner.CRAFT, Role.HOTBAR, 8);
        SlotDescriptor playerOwnedAtAppendedOrdinal = syntheticSlot(
                74, menu.playerInventory(), Owner.PLAYER, Role.HOTBAR, 0);

        TransferPlan craftOwnedPlan = QuickMoveRouter.plan(menu, craftOwnedAtUnrelatedOrdinal, true);
        TransferPlan playerOwnedPlan = QuickMoveRouter.plan(menu, playerOwnedAtAppendedOrdinal, true);

        assertEquals(List.of(ORDINARY_DESTINATION), craftOwnedPlan.destinations());
        assertFalse(craftOwnedPlan.autoEquipAttempted());
        assertTrue(playerOwnedPlan.autoEquipAttempted(),
                "ordinal 74 alone must not confer transient-crafting ownership");
    }

    @Test
    void noMenuSourceEverOffersAnyOfTheNineCraftCellsAsAQuickMoveTarget() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        Set<Integer> craftMenuIds = Set.copyOf(
                menu.inputGridSlots().stream().map(SlotDescriptor::index).toList());

        for (SlotDescriptor source : menu.slots()) {
            for (boolean equippable : List.of(false, true)) {
                TransferPlan plan = QuickMoveRouter.plan(menu, source, equippable);
                Set<Integer> destinations = destinationMenuIds(plan);
                assertTrue(plan.destinations().stream()
                                .noneMatch(destination -> destination.region() == DestinationRegion.CRAFT),
                        "source " + source.index() + " declared a craft destination");
                assertTrue(destinations.stream().noneMatch(craftMenuIds::contains),
                        "source " + source.index() + " routed into a transient craft cell");
            }
        }
    }

    @Test
    void resultQuickMoveRetainsReverseOrdinaryInsertionWithoutExpandingTheDestination() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);

        TransferPlan plan = QuickMoveRouter.plan(menu, menu.slot(0), false);

        assertEquals(List.of(ORDINARY_DESTINATION), plan.destinations());
        assertTrue(plan.reverse());
        assertFalse(plan.autoEquipAttempted());
    }
}

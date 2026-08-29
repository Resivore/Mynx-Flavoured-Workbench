package dev.resivore.inventorycrafting;

import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.syntheticSlot;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.CreativePolicy;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.CreativeProjection;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.CreativeWrapper;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.CreativeWrapperKind;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.JeiTransferPlan;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.JeiTransferPlanner;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Owner;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Role;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Side;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.SlotDescriptor;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.TargetInventoryMenu;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CreativeAndJeiFixtureTest {
    @Test
    void creativeProjectionWrapsThePreTrinketsPrefixOnceAndHidesEveryCraftTarget() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.CLIENT, 3);
        CreativeProjection projection = CreativePolicy.project(menu);

        List<CreativeWrapper> baseWrappers = projection.wrappers().stream()
                .filter(wrapper -> wrapper.kind() == CreativeWrapperKind.BASE_VISIBLE
                        || wrapper.kind() == CreativeWrapperKind.BASE_HIDDEN)
                .toList();
        List<CreativeWrapper> trinketWrappers = projection.wrappers().stream()
                .filter(wrapper -> wrapper.kind() == CreativeWrapperKind.TRINKET)
                .toList();
        Set<Integer> hiddenTargets = Set.copyOf(projection.wrappers().stream()
                .filter(wrapper -> wrapper.kind() == CreativeWrapperKind.BASE_HIDDEN)
                .map(CreativeWrapper::targetMenuId)
                .toList());

        assertAll(
                () -> assertEquals(82, menu.slots().size()),
                () -> assertEquals(83, projection.wrappers().size(),
                        "79 base wrappers + 3 Trinket wrappers + destroy slot"),
                () -> assertEquals(79, baseWrappers.size()),
                () -> assertEquals(IntStream.range(0, 79).boxed().toList(),
                        baseWrappers.stream().map(CreativeWrapper::targetMenuId).toList()),
                () -> assertEquals(List.of(79, 80, 81),
                        trinketWrappers.stream().map(CreativeWrapper::targetMenuId).toList()),
                () -> assertEquals(
                        Set.of(0, 1, 2, 3, 4, 74, 75, 76, 77, 78),
                        hiddenTargets),
                () -> assertEquals(1, projection.wrappers().stream()
                        .filter(wrapper -> wrapper.kind() == CreativeWrapperKind.DESTROY)
                        .count())
        );

        List<Integer> realTargets = projection.wrappers().stream()
                .map(CreativeWrapper::targetMenuId)
                .filter(target -> target >= 0)
                .toList();
        assertEquals(menu.slots().size(), Set.copyOf(realTargets).size(),
                "a survival-menu target was duplicated or omitted by the Creative projection");
    }

    @Test
    void creativeVisibilityAndWritesAreGuardedByBackingContainerIdentity() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.CLIENT, 3);

        for (SlotDescriptor craftTarget : menu.inputGridSlots()) {
            assertFalse(CreativePolicy.visible(menu, craftTarget));
            assertFalse(CreativePolicy.creativeWriteAllowed(menu, craftTarget));
        }

        SlotDescriptor craftOwnedAtAnOrdinaryLookingId = syntheticSlot(
                12, menu.craftingContainer(), Owner.CRAFT, Role.ORDINARY_STORAGE, 6);
        SlotDescriptor playerOwnedAtAnAppendedLookingId = syntheticSlot(
                76, menu.playerInventory(), Owner.PLAYER, Role.ORDINARY_STORAGE, 20);

        assertAll(
                () -> assertFalse(CreativePolicy.visible(menu, craftOwnedAtAnOrdinaryLookingId)),
                () -> assertFalse(CreativePolicy.creativeWriteAllowed(menu, craftOwnedAtAnOrdinaryLookingId)),
                () -> assertTrue(CreativePolicy.visible(menu, playerOwnedAtAnAppendedLookingId)),
                () -> assertTrue(CreativePolicy.creativeWriteAllowed(menu, playerOwnedAtAnAppendedLookingId))
        );
    }

    @Test
    void creativeGuardsDoNotHideOrBlockTheSixtyThreeOrdinaryPlayerSlots() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.CLIENT, 3);
        List<SlotDescriptor> ordinary = menu.slots().stream()
                .filter(slot -> slot.role() == Role.ORDINARY_STORAGE || slot.role() == Role.HOTBAR)
                .toList();

        assertEquals(63, ordinary.size());
        assertTrue(ordinary.stream().allMatch(slot -> CreativePolicy.visible(menu, slot)));
        assertTrue(ordinary.stream().allMatch(slot -> CreativePolicy.creativeWriteAllowed(menu, slot)));
    }

    @Test
    void jeiMapsAllNineViewPositionsToTheirActualNoncontiguousMenuTargets() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        JeiTransferPlan plan = JeiTransferPlanner.forInventory(menu);
        List<Integer> expectedTargets = List.of(1, 2, 74, 3, 4, 75, 76, 77, 78);

        assertEquals(expectedTargets, plan.recipeTargetMenuIds());
        for (int viewPosition = 0; viewPosition < 9; viewPosition++) {
            assertEquals(expectedTargets.get(viewPosition), plan.actualTargetForViewPosition(viewPosition),
                    "JEI view cell " + viewPosition + " was normalized to a contiguous menu range");
            assertEquals(viewPosition,
                    menu.slot(plan.actualTargetForViewPosition(viewPosition)).containerSlot());
        }
        assertEquals(9, Set.copyOf(plan.recipeTargetMenuIds()).size());
    }

    @Test
    void jeiUsesAllAndOnlyTheSixtyThreeInventoryExtendedOrdinarySources() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        JeiTransferPlan plan = JeiTransferPlanner.forInventory(menu);

        assertAll(
                () -> assertEquals(63, plan.sourceMenuIds().size()),
                () -> assertEquals(IntStream.range(9, 72).boxed().toList(), plan.sourceMenuIds()),
                () -> assertFalse(plan.sourceMenuIds().contains(72)),
                () -> assertFalse(plan.sourceMenuIds().contains(73)),
                () -> assertTrue(plan.sourceMenuIds().stream().noneMatch(plan.recipeTargetMenuIds()::contains))
        );
        assertTrue(plan.sourceMenuIds().stream()
                .map(menu::slot)
                .allMatch(slot -> slot.container() == menu.playerInventory()
                        && slot.containerSlot() >= 0
                        && slot.containerSlot() < 63));
        assertTrue(plan.sourceMenuIds().stream()
                .map(menu::slot)
                .allMatch(slot -> slot.role() == Role.ORDINARY_STORAGE || slot.role() == Role.HOTBAR));
    }
}

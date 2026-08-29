package dev.resivore.offhandqol;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoexistenceFixtureTest {
    enum Source { GROUND, EXTERNAL_MENU, PLAYER_INVENTORY }

    record Carrier(String kind, String identity, boolean hasMatch, boolean locked,
                   boolean sourceSlot, int capacity) {}

    record AcquisitionResult(
            int mainMatch,
            int carrierMatch,
            int hotbarMatch,
            int offhandMatch,
            int emptyHotbar,
            int ordinary,
            int remainder
    ) {}

    record Result(
            int initial,
            int mainHand,
            int hotbar,
            int offhand,
            List<Integer> carriers,
            int ordinaryInventory,
            int menuDestination,
            int remainder
    ) {
        int moved() {
            return mainHand + hotbar + offhand
                    + carriers.stream().mapToInt(Integer::intValue).sum()
                    + ordinaryInventory + menuDestination;
        }
    }

    private static Result route(
            Source source,
            String identity,
            int count,
            int mainCapacity,
            int hotbarCapacity,
            int offhandCapacity,
            List<Carrier> carriers,
            int ordinaryCapacityExcludingSource,
            int menuCapacity
    ) {
        int remainder = count;
        int main = 0;
        int hotbar = 0;
        int ordinary = 0;
        List<Integer> accepted = new ArrayList<>();

        if (source != Source.PLAYER_INVENTORY) {
            main = Math.min(remainder, mainCapacity);
            remainder -= main;
        }

        int offhand = 0;
        if (source == Source.PLAYER_INVENTORY) {
            offhand = Math.min(remainder, offhandCapacity);
            remainder -= offhand;
        }

        for (Carrier carrier : carriers) {
            boolean qualifies = !carrier.locked()
                    && carrier.hasMatch()
                    && !carrier.sourceSlot()
                    && carrier.identity().equals(identity)
                    && (carrier.kind().equals("shulker") || carrier.kind().equals("bundle"));
            int moved = qualifies ? Math.min(remainder, carrier.capacity()) : 0;
            accepted.add(moved);
            remainder -= moved;
        }

        if (source != Source.PLAYER_INVENTORY) {
            hotbar = Math.min(remainder, hotbarCapacity);
            remainder -= hotbar;
            offhand = Math.min(remainder, offhandCapacity);
            remainder -= offhand;
            ordinary = Math.min(remainder, ordinaryCapacityExcludingSource);
            remainder -= ordinary;
        }
        int menu = source == Source.PLAYER_INVENTORY ? Math.min(remainder, menuCapacity) : 0;
        remainder -= menu;
        return new Result(count, main, hotbar, offhand, List.copyOf(accepted), ordinary, menu, remainder);
    }

    private static Carrier carrier(String kind, String identity, int capacity) {
        return new Carrier(kind, identity, true, false, false, capacity);
    }

    private static AcquisitionResult routeAcquisitionWithEmptyHotbar(
            int count,
            int mainMatchCapacity,
            int carrierMatchCapacity,
            int hotbarMatchCapacity,
            int offhandMatchCapacity,
            int emptyHotbarCapacity,
            int ordinaryCapacity
    ) {
        int remainder = count;
        int main = Math.min(remainder, mainMatchCapacity);
        remainder -= main;
        int carrier = Math.min(remainder, carrierMatchCapacity);
        remainder -= carrier;
        int hotbarMatch = Math.min(remainder, hotbarMatchCapacity);
        remainder -= hotbarMatch;
        int offhand = Math.min(remainder, offhandMatchCapacity);
        remainder -= offhand;
        int emptyHotbar = Math.min(remainder, emptyHotbarCapacity);
        remainder -= emptyHotbar;
        int ordinary = Math.min(remainder, ordinaryCapacity);
        remainder -= ordinary;
        return new AcquisitionResult(
                main, carrier, hotbarMatch, offhand, emptyHotbar, ordinary, remainder
        );
    }

    private static void exact(Result result) {
        assertEquals(result.initial(), result.moved() + result.remainder());
        assertTrue(result.remainder() >= 0);
    }

    @Test
    void playerInventoryShiftClickCanMergeIntoOccupiedMatchingOffhand() {
        Result result = route(
                Source.PLAYER_INVENTORY, "gold_ingot{}", 15,
                64, 64, 63, List.of(carrier("shulker", "gold_ingot{}", 64)), 64, 54
        );
        assertEquals(0, result.mainHand());
        assertEquals(0, result.hotbar());
        assertEquals(15, result.offhand());
        assertEquals(List.of(0), result.carriers());
        assertEquals(0, result.ordinaryInventory());
        assertEquals(0, result.menuDestination());
        assertEquals(0, result.remainder());
        exact(result);
    }

    @Test
    void groundPickupUsesGlobalHierarchy() {
        Result result = route(
                Source.GROUND, "stone{}", 20,
                2, 3, 4, List.of(carrier("shulker", "stone{}", 5)), 6, 0
        );
        assertEquals(2, result.mainHand());
        assertEquals(List.of(5), result.carriers());
        assertEquals(3, result.hotbar());
        assertEquals(4, result.offhand());
        assertEquals(6, result.ordinaryInventory());
        exact(result);
    }

    @Test
    void externalChestQuickMoveUsesSameHierarchy() {
        Result result = route(
                Source.EXTERNAL_MENU, "stone{}", 20,
                2, 3, 4, List.of(carrier("bundle", "stone{}", 5)), 6, 0
        );
        assertEquals(2, result.mainHand());
        assertEquals(List.of(5), result.carriers());
        assertEquals(3, result.hotbar());
        assertEquals(4, result.offhand());
        assertEquals(6, result.ordinaryInventory());
        exact(result);
    }

    @Test
    void groundPickupDoesNotUseEmptyHotbarBeforeMatchingCarrierOrOffhand() {
        AcquisitionResult result = routeAcquisitionWithEmptyHotbar(
                7, 0, 3, 0, 2, 64, 64
        );
        assertEquals(3, result.carrierMatch());
        assertEquals(2, result.offhandMatch());
        assertEquals(2, result.emptyHotbar());
        assertEquals(0, result.ordinary());
        assertEquals(0, result.remainder());
    }

    @Test
    void externalQuickMoveDoesNotUseEmptyHotbarBeforeMatchingCarrierOrOffhand() {
        AcquisitionResult result = routeAcquisitionWithEmptyHotbar(
                9, 1, 3, 2, 1, 64, 64
        );
        assertEquals(1, result.mainMatch());
        assertEquals(3, result.carrierMatch());
        assertEquals(2, result.hotbarMatch());
        assertEquals(1, result.offhandMatch());
        assertEquals(2, result.emptyHotbar());
        assertEquals(0, result.ordinary());
        assertEquals(0, result.remainder());
    }

    @Test
    void carriedContainerTierRunsBeforeHotbarAndInStableInventoryOrder() {
        Result result = route(
                Source.EXTERNAL_MENU, "stone{}", 12,
                0, 20, 4,
                List.of(carrier("bundle", "stone{}", 3), carrier("shulker", "stone{}", 5)),
                20, 0
        );
        assertEquals(List.of(3, 5), result.carriers());
        assertEquals(4, result.hotbar());
        assertEquals(0, result.offhand());
        assertEquals(0, result.ordinaryInventory());
        exact(result);
    }

    @Test
    void lockedEmptyUnrelatedAndForeignContainersAreSkipped() {
        List<Carrier> carriers = List.of(
                new Carrier("shulker", "stone{}", true, true, false, 64),
                new Carrier("bundle", "stone{}", false, false, false, 64),
                new Carrier("shulker", "dirt{}", true, false, false, 64),
                new Carrier("backpack", "stone{}", true, false, false, 64),
                carrier("bundle", "stone{}", 3)
        );
        Result result = route(Source.GROUND, "stone{}", 10, 0, 0, 0, carriers, 0, 0);
        assertEquals(List.of(0, 0, 0, 0, 3), result.carriers());
        assertEquals(7, result.remainder());
        exact(result);
    }

    @Test
    void exactRemainderCrossesEveryTierOnce() {
        Result result = route(
                Source.EXTERNAL_MENU, "stone{name=A}", 23,
                1, 2, 3,
                List.of(carrier("shulker", "stone{name=A}", 4), carrier("bundle", "stone{name=A}", 5)),
                6, 0
        );
        assertEquals(1, result.mainHand());
        assertEquals(List.of(4, 5), result.carriers());
        assertEquals(2, result.hotbar());
        assertEquals(3, result.offhand());
        assertEquals(6, result.ordinaryInventory());
        assertEquals(2, result.remainder());
        exact(result);
    }

    @Test
    void playerSourceCapacityIsExcludedFromOrdinaryDestinations() {
        Result result = route(
                Source.PLAYER_INVENTORY, "gold_ingot{}", 15,
                0, 0, 0, List.of(), 0, 64
        );
        assertEquals(0, result.ordinaryInventory());
        assertEquals(15, result.menuDestination());
        exact(result);
    }

    @Test
    void playerOriginOffersOffhandThenCarrierRemainderToNormalMenuBehavior() {
        Result result = route(
                Source.PLAYER_INVENTORY, "oak_planks{}", 15,
                64, 64, 4, List.of(carrier("bundle", "oak_planks{}", 5)), 64, 64
        );
        assertEquals(0, result.mainHand());
        assertEquals(0, result.hotbar());
        assertEquals(4, result.offhand());
        assertEquals(List.of(5), result.carriers());
        assertEquals(0, result.ordinaryInventory());
        assertEquals(6, result.menuDestination());
        assertEquals(0, result.remainder());
        exact(result);
    }

    @Test
    void playerOriginRoutesIntoMatchingCarrierAndExcludesClickedSource() {
        Result result = route(
                Source.PLAYER_INVENTORY, "oak_planks{}", 15,
                64, 64, 0,
                List.of(
                        new Carrier("bundle", "oak_planks{}", true, false, true, 64),
                        carrier("shulker", "oak_planks{}", 9)
                ),
                64, 64
        );
        assertEquals(List.of(0, 9), result.carriers());
        assertEquals(6, result.menuDestination());
        assertEquals(0, result.remainder());
        exact(result);
    }
}

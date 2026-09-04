package dev.resivore.slotreservations;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarriedShulkerInsertionPolicyTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.POISONOUS_POTATO);
        bindTestComponents(Items.RABBIT_STEW);
        bindTestComponents(Items.STONE);
        bindTestComponents(Blocks.SHULKER_BOX.asItem());
    }

    @Test
    void matchingReservedEmptiesLeadAndMismatchesAreRemovedBeforeUnreservedFallbacks() {
        ItemStack incoming = identity(Items.POISONOUS_POTATO, 9, "green_curry", "Green Curry");
        ReservationData reservations = ReservationData.EMPTY
                .with(1, identity(Items.RABBIT_STEW, 1, "ramen", "Ramen"))
                .with(5, incoming);
        SimpleContainer liveContents = new SimpleContainer(27);
        int[] upstream = {1, 0, 5, 2, 26};

        assertArrayEquals(
                new int[]{5, 0, 2, 26},
                CarriedShulkerInsertionPolicy.emptyCandidates(
                        reservations,
                        liveContents,
                        incoming,
                        upstream
                )
        );
    }

    @Test
    void componentDistinctReservationRejectsTheSameItemWhileExactComponentsRemainEligible() {
        ItemStack reserved = identity(Items.POISONOUS_POTATO, 1, "green_curry", "Green Curry");
        ItemStack componentDistinct = identity(
                Items.POISONOUS_POTATO,
                64,
                "green_curry",
                "Different Name"
        );
        ReservationData reservations = ReservationData.EMPTY.with(7, reserved);
        SimpleContainer liveContents = new SimpleContainer(27);

        assertArrayEquals(
                new int[0],
                CarriedShulkerInsertionPolicy.emptyCandidates(
                        reservations,
                        liveContents,
                        componentDistinct,
                        new int[]{7}
                )
        );
        assertArrayEquals(
                new int[]{7},
                CarriedShulkerInsertionPolicy.emptyCandidates(
                        reservations,
                        liveContents,
                        reserved.copyWithCount(32),
                        new int[]{7}
                )
        );
    }

    @Test
    void occupiedPassPreservesUpstreamOrderButDropsReservationMismatchesAndEmptySlots() {
        ItemStack incoming = identity(Items.POISONOUS_POTATO, 8, "green_curry", "Green Curry");
        ReservationData reservations = ReservationData.EMPTY
                .with(2, identity(Items.RABBIT_STEW, 1, "ramen", "Ramen"))
                .with(4, incoming);
        SimpleContainer liveContents = new SimpleContainer(27);
        liveContents.setItem(2, incoming.copyWithCount(1));
        liveContents.setItem(3, incoming.copyWithCount(2));
        liveContents.setItem(4, incoming.copyWithCount(3));

        assertArrayEquals(
                new int[]{3, 4},
                CarriedShulkerInsertionPolicy.occupiedCandidates(
                        reservations,
                        liveContents,
                        incoming,
                        new int[]{2, 0, 3, 4}
                )
        );
    }

    @Test
    void nativeShulkerNestingAndUnexpectedTemporaryViewsFailClosed() {
        ReservationData reservations = ReservationData.EMPTY.with(
                6,
                new ItemStack(Blocks.SHULKER_BOX)
        );
        ItemStack nestedShulker = new ItemStack(Blocks.SHULKER_BOX);

        assertArrayEquals(
                new int[0],
                CarriedShulkerInsertionPolicy.emptyCandidates(
                        reservations,
                        new SimpleContainer(27),
                        nestedShulker,
                        new int[]{6, 0}
                )
        );
        assertArrayEquals(
                new int[0],
                CarriedShulkerInsertionPolicy.emptyCandidates(
                        ReservationData.EMPTY.with(6, new ItemStack(Items.STONE)),
                        new SimpleContainer(26),
                        new ItemStack(Items.STONE),
                        new int[]{6, 0}
                )
        );
    }

    @Test
    void reservationFreeAndNonShulkerPathsRemainExactPassthroughs() {
        int[] upstream = {9, 0, 1, 2};
        SimpleContainer liveContents = new SimpleContainer(27);
        ItemStack incoming = new ItemStack(Items.STONE);

        assertSame(
                upstream,
                CarriedShulkerInsertionPolicy.emptyCandidates(
                        ReservationData.EMPTY,
                        liveContents,
                        incoming,
                        upstream
                )
        );
        assertSame(
                upstream,
                CarriedShulkerInsertionPolicy.occupiedCandidates(
                        new ItemStack(Items.STONE),
                        liveContents,
                        incoming,
                        upstream
                )
        );
    }

    @Test
    void candidatePlanningNeverMutatesReservationsContentsIncomingOrUpstreamOrder() {
        ItemStack incoming = identity(Items.POISONOUS_POTATO, 17, "green_curry", "Green Curry");
        ReservationData reservations = ReservationData.EMPTY.with(8, incoming);
        SimpleContainer liveContents = new SimpleContainer(27);
        liveContents.setItem(3, identity(Items.STONE, 4, "stone", "Stone"));
        ItemStack physicalBefore = liveContents.getItem(3).copy();
        ItemStack incomingBefore = incoming.copy();
        int[] upstream = {3, 8, 0};
        int[] upstreamBefore = upstream.clone();

        CarriedShulkerInsertionPolicy.occupiedCandidates(
                reservations,
                liveContents,
                incoming,
                upstream
        );
        CarriedShulkerInsertionPolicy.emptyCandidates(
                reservations,
                liveContents,
                incoming,
                upstream
        );

        assertTrue(reservations.matches(8, incomingBefore));
        assertTrue(ItemStack.matches(physicalBefore, liveContents.getItem(3)));
        assertTrue(ItemStack.matches(incomingBefore, incoming));
        assertArrayEquals(upstreamBefore, upstream);
    }

    private static ItemStack identity(Item item, int count, String model, String name) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("container_slot_reservations_fixture", model));
        stack.set(DataComponents.ITEM_NAME, Component.literal(name));
        return stack;
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}

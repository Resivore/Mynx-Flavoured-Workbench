package dev.resivore.slotreservations;

import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import dev.resivore.slotreservations.api.Reservation;
import dev.resivore.slotreservations.api.ReservationSlotClass;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContainerSlotReservationsApiTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.POISONOUS_POTATO);
        bindTestComponents(Blocks.SHULKER_BOX.asItem());
    }

    @Test
    void resolverSupportsOnlyExactVanillaPhysicalContainerTypes() {
        ChestBlockEntity chest = chest(Blocks.CHEST);
        TrappedChestBlockEntity trapped = trappedChest();
        BarrelBlockEntity barrel = new BarrelBlockEntity(BlockPos.ZERO, Blocks.BARREL.defaultBlockState());
        ShulkerBoxBlockEntity shulker = new ShulkerBoxBlockEntity(
                BlockPos.ZERO, Blocks.DYED_SHULKER_BOX.red().defaultBlockState());

        assertResolved(chest, 26);
        assertResolved(trapped, 0);
        assertResolved(barrel, 11);
        assertResolved(shulker, 5);
        assertTrue(SupportedContainerResolver.resolve(chest, -1).isEmpty());
        assertTrue(SupportedContainerResolver.resolve(chest, 27).isEmpty());

        ChestBlockEntity copperChestShape = chest(Blocks.COPPER_CHEST.asList().getFirst());
        assertTrue(SupportedContainerResolver.resolve(copperChestShape, 0).isEmpty(),
                "Canary 1 must not silently broaden ownership to copper or modded chests");
    }

    @Test
    void publicReservationValueIsCountlessDefensiveAndComponentExact() {
        ItemStack reserved = identity(Items.POISONOUS_POTATO, 13, "green_curry", "Green Curry");
        Reservation reservation = new Reservation(reserved);
        ItemStack exposed = reservation.template();

        assertEquals(1, exposed.getCount());
        assertTrue(reservation.matches(reserved.copyWithCount(64)));
        exposed.set(DataComponents.ITEM_NAME, Component.literal("mutated caller copy"));
        assertTrue(ItemStack.isSameItemSameComponents(reserved, reservation.template()));
        assertEquals(reserved.getComponentsPatch(), reservation.template().getComponentsPatch());
    }

    @Test
    void slotClassificationEnumPublishesRoutingFactsWithoutPrescribingOrder() {
        assertTrue(ReservationSlotClass.OCCUPIED_COMPATIBLE.acceptsIncoming());
        assertTrue(ReservationSlotClass.RESERVED_MATCH.acceptsIncoming());
        assertTrue(ReservationSlotClass.UNRESERVED_EMPTY.acceptsIncoming());
        assertFalse(ReservationSlotClass.RESERVED_OTHER.acceptsIncoming());
        assertFalse(ReservationSlotClass.NON_WRITABLE.acceptsIncoming());
        assertFalse(ReservationSlotClass.INELIGIBLE.acceptsIncoming());
    }

    @Test
    void nonShulkerItemsAndOutOfRangeShulkerSlotsRemainOutsideTheItemApi() {
        ItemStack ordinary = identity(Items.POISONOUS_POTATO, 1, "green_curry", "Green Curry");
        assertEquals(ReservationSlotClass.INELIGIBLE,
                ContainerSlotReservationsApi.classify(ordinary, 0, ordinary));
        assertTrue(ContainerSlotReservationsApi.getReservation(ordinary, 0).isEmpty());
        assertTrue(ContainerSlotReservationsApi.getReservation(new ItemStack(Blocks.SHULKER_BOX), 27).isEmpty());
    }

    private static void assertResolved(BlockEntity blockEntity, int slot) {
        SupportedContainerResolver.ResolvedSlot resolved = SupportedContainerResolver
                .resolve((net.minecraft.world.Container) blockEntity, slot).orElseThrow();
        assertSame(blockEntity, resolved.owner());
        assertSame(blockEntity, resolved.blockEntity());
        assertEquals(slot, resolved.localSlot());
    }

    private static ChestBlockEntity chest(Block block) {
        return new ChestBlockEntity(BlockPos.ZERO, block.defaultBlockState());
    }

    private static TrappedChestBlockEntity trappedChest() {
        return new TrappedChestBlockEntity(BlockPos.ZERO, Blocks.TRAPPED_CHEST.defaultBlockState());
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

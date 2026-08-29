package dev.resivore.slotreservations.gametest;

import dev.resivore.slotreservations.ModComponents;
import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import dev.resivore.slotreservations.api.ReservationSlotClass;
import dev.resivore.slotreservations.menu.ReservationAwareShulkerBoxSlot;
import dev.resivore.slotreservations.menu.ReservationAwareSlot;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.lang.reflect.Method;
import java.util.List;

public final class ContainerSlotReservationGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void exactChestTrappedBarrelAndShulkerBlockEntitiesResolve(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 2, 1);
        BlockPos trappedPos = new BlockPos(2, 2, 1);
        BlockPos barrelPos = new BlockPos(3, 2, 1);
        BlockPos shulkerPos = new BlockPos(4, 2, 1);
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.setBlock(trappedPos, Blocks.TRAPPED_CHEST);
        helper.setBlock(barrelPos, Blocks.BARREL);
        helper.setBlock(shulkerPos, Blocks.DYED_SHULKER_BOX.blue());

        ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        TrappedChestBlockEntity trapped = helper.getBlockEntity(trappedPos, TrappedChestBlockEntity.class);
        BarrelBlockEntity barrel = helper.getBlockEntity(barrelPos, BarrelBlockEntity.class);
        ShulkerBoxBlockEntity shulker = helper.getBlockEntity(shulkerPos, ShulkerBoxBlockEntity.class);

        assertPhysicalOwner(helper, chest, 0);
        assertPhysicalOwner(helper, trapped, 7);
        assertPhysicalOwner(helper, barrel, 13);
        assertPhysicalOwner(helper, shulker, 26);
        helper.assertTrue(SupportedContainerResolver.resolve(chest, 27).isEmpty(),
                "A physical 27-slot owner exposed an out-of-range reservation slot");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void doubleChestIndicesRemainOwnedBySeparatePhysicalHalves(GameTestHelper helper) {
        BlockPos leftPos = new BlockPos(1, 2, 1);
        BlockPos rightPos = new BlockPos(2, 2, 1);
        BlockState leftState = Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.LEFT);
        BlockState rightState = Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.RIGHT);
        helper.setBlock(rightPos, rightState);
        helper.setBlock(leftPos, leftState);

        Container logical = ChestBlock.getContainer(
                (ChestBlock) Blocks.CHEST,
                helper.getBlockState(leftPos),
                helper.getLevel(),
                helper.absolutePos(leftPos),
                true
        );
        helper.assertTrue(logical instanceof CompoundContainer,
                "The placed pair did not form an actual vanilla double-chest container");

        SupportedContainerResolver.ResolvedSlot first = SupportedContainerResolver.resolve(logical, 0)
                .orElseThrow();
        SupportedContainerResolver.ResolvedSlot second = SupportedContainerResolver.resolve(logical, 27)
                .orElseThrow();
        helper.assertTrue(first.blockEntity() != second.blockEntity()
                        && first.localSlot() == 0
                        && second.localSlot() == 0,
                "Logical double-chest indices were not mapped to separate physical slot-zero owners");

        ItemStack a = identity(Items.POISONOUS_POTATO, 8, "green_curry", "Green Curry");
        ItemStack b = identity(Items.RABBIT_STEW, 5, "ramen", "Ramen");
        ReservationStore.set(first, a);
        ReservationStore.set(second, b);
        helper.assertTrue(ReservationStore.getData(first.blockEntity()).matches(0, a)
                        && !ReservationStore.getData(first.blockEntity()).matches(0, b)
                        && ReservationStore.getData(second.blockEntity()).matches(0, b)
                        && !ReservationStore.getData(second.blockEntity()).matches(0, a),
                "A double-chest reservation leaked across its physical persistence boundary");

        Container reopenedFromOtherHalf = ChestBlock.getContainer(
                (ChestBlock) Blocks.CHEST,
                helper.getBlockState(rightPos),
                helper.getLevel(),
                helper.absolutePos(rightPos),
                true
        );
        SupportedContainerResolver.ResolvedSlot reopenedFirst = SupportedContainerResolver
                .resolve(reopenedFromOtherHalf, 0).orElseThrow();
        SupportedContainerResolver.ResolvedSlot reopenedSecond = SupportedContainerResolver
                .resolve(reopenedFromOtherHalf, 27).orElseThrow();
        helper.assertTrue(reopenedFirst.blockEntity() == first.blockEntity()
                        && reopenedSecond.blockEntity() == second.blockEntity()
                        && ContainerSlotReservationsApi.reservationMatches(reopenedFromOtherHalf, 0, a)
                        && ContainerSlotReservationsApi.reservationMatches(reopenedFromOtherHalf, 27, b),
                "Opening the other half changed logical-to-physical reservation ownership");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void manualAndQuickMoveInsertionUseReservationAwareChestSlots(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
        ItemStack reserved = identity(Items.POISONOUS_POTATO, 1, "green_curry", "Green Curry");
        ItemStack wrong = identity(Items.POISONOUS_POTATO, 5, "ramen", "Ramen");
        ReservationStore.set(SupportedContainerResolver.resolve(chest, 0).orElseThrow(), reserved);

        var player = helper.makeMockServerPlayerInLevel();
        ChestMenu menu = ChestMenu.threeRows(17, player.getInventory(), chest);
        Slot destination = menu.slots.get(0);
        helper.assertTrue(destination instanceof ReservationAwareSlot,
                "ChestMenu did not install its reservation-aware destination slot");

        menu.setCarried(wrong.copy());
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(chest.getItem(0).isEmpty()
                        && ItemStack.isSameItemSameComponents(menu.getCarried(), wrong)
                        && menu.getCarried().getCount() == wrong.getCount(),
                "Ordinary manual placement admitted a component-distinct stack");

        ItemStack manualMatch = reserved.copyWithCount(6);
        menu.setCarried(manualMatch);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(ItemStack.isSameItemSameComponents(chest.getItem(0), reserved)
                        && chest.getItem(0).getCount() == 6
                        && menu.getCarried().isEmpty(),
                "Ordinary manual placement rejected the exact reserved identity");

        chest.removeItemNoUpdate(0);
        helper.assertTrue(ContainerSlotReservationsApi.isReserved(chest, 0),
                "Removing the physical stack cleared the independent reservation");
        menu.removed(player);
        menu = ChestMenu.threeRows(18, player.getInventory(), chest);
        helper.assertTrue(menu.slots.get(0) instanceof ReservationAwareSlot
                        && ContainerSlotReservationsApi.isReserved(chest, 0),
                "Closing and reopening the same chest lost its reservation or menu wrapper");
        for (int slot = 1; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        player.getInventory().setItem(9, wrong.copy());
        menu.quickMoveStack(player, 27);
        helper.assertTrue(chest.getItem(0).isEmpty()
                        && player.getInventory().getItem(9).getCount() == wrong.getCount(),
                "QUICK_MOVE routed a component-distinct stack into the reserved empty slot");

        ItemStack quickMatch = reserved.copyWithCount(7);
        player.getInventory().setItem(9, quickMatch);
        menu.quickMoveStack(player, 27);
        helper.assertTrue(ItemStack.isSameItemSameComponents(chest.getItem(0), reserved)
                        && chest.getItem(0).getCount() == 7
                        && player.getInventory().getItem(9).isEmpty(),
                "QUICK_MOVE rejected the exact reserved identity");

        ItemStack physicalBeforeClear = chest.getItem(0).copy();
        ReservationStore.clear(SupportedContainerResolver.resolve(chest, 0).orElseThrow());
        helper.assertTrue(!ContainerSlotReservationsApi.isReserved(chest, 0)
                        && chest.getItem(0).getCount() == physicalBeforeClear.getCount()
                        && ItemStack.isSameItemSameComponents(chest.getItem(0), physicalBeforeClear)
                        && chest.getItem(0).getComponentsPatch().equals(physicalBeforeClear.getComponentsPatch()),
                "Clearing reservation metadata altered or moved the physical stack");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void shulkerMenuKeepsVanillaNestingAndReservationAdmission(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.SHULKER_BOX);
        ShulkerBoxBlockEntity shulker = helper.getBlockEntity(pos, ShulkerBoxBlockEntity.class);
        ItemStack reserved = identity(Items.POISONOUS_POTATO, 1, "green_curry", "Green Curry");
        ItemStack wrong = identity(Items.POISONOUS_POTATO, 1, "ramen", "Ramen");
        ReservationStore.set(SupportedContainerResolver.resolve(shulker, 0).orElseThrow(), reserved);

        var player = helper.makeMockServerPlayerInLevel();
        ShulkerBoxMenu menu = new ShulkerBoxMenu(23, player.getInventory(), shulker);
        Slot destination = menu.slots.get(0);
        helper.assertTrue(destination instanceof ReservationAwareShulkerBoxSlot,
                "ShulkerBoxMenu did not install its reservation-aware vanilla slot subtype");
        helper.assertTrue(destination.mayPlace(reserved.copyWithCount(2)),
                "Shulker slot rejected its exact reserved identity");
        helper.assertTrue(!destination.mayPlace(wrong),
                "Shulker slot admitted a component-distinct identity");
        helper.assertTrue(!destination.mayPlace(new ItemStack(Blocks.SHULKER_BOX)),
                "Reservation wrapping bypassed vanilla's no-container-items shulker rule");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void backingContainerAdmissionProvidesTheHopperGate(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 2, 1);
        BlockPos trappedPos = new BlockPos(2, 2, 1);
        BlockPos barrelPos = new BlockPos(3, 2, 1);
        BlockPos shulkerPos = new BlockPos(4, 2, 1);
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.setBlock(trappedPos, Blocks.TRAPPED_CHEST);
        helper.setBlock(barrelPos, Blocks.BARREL);
        helper.setBlock(shulkerPos, Blocks.SHULKER_BOX);
        List<Container> containers = List.of(
                helper.getBlockEntity(chestPos, ChestBlockEntity.class),
                helper.getBlockEntity(trappedPos, TrappedChestBlockEntity.class),
                helper.getBlockEntity(barrelPos, BarrelBlockEntity.class),
                helper.getBlockEntity(shulkerPos, ShulkerBoxBlockEntity.class)
        );
        ItemStack reserved = identity(Items.POISONOUS_POTATO, 1, "green_curry", "Green Curry");
        ItemStack wrong = identity(Items.POISONOUS_POTATO, 1, "ramen", "Ramen");

        for (Container container : containers) {
            ReservationStore.set(SupportedContainerResolver.resolve(container, 0).orElseThrow(), reserved);
            helper.assertTrue(container.canPlaceItem(0, reserved.copyWithCount(4)),
                    "Backing-container admission rejected its exact reservation: " + container.getClass());
            helper.assertTrue(!container.canPlaceItem(0, wrong),
                    "Backing-container admission accepted a distinct reservation: " + container.getClass());
            helper.assertTrue(container.canPlaceItem(1, wrong),
                    "An unreserved backing-container slot lost vanilla admission: " + container.getClass());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void publicApiClassifiesReservedUnreservedAndOccupiedDestinations(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.BARREL);
        BarrelBlockEntity barrel = helper.getBlockEntity(pos, BarrelBlockEntity.class);
        ItemStack reserved = identity(Items.POISONOUS_POTATO, 11, "green_curry", "Green Curry");
        ItemStack exact = reserved.copyWithCount(3);
        ItemStack other = identity(Items.POISONOUS_POTATO, 1, "ramen", "Ramen");
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 0).orElseThrow(), reserved);

        helper.assertTrue(ContainerSlotReservationsApi.classify(barrel, 0, exact)
                        == ReservationSlotClass.RESERVED_MATCH
                        && ContainerSlotReservationsApi.mayInsert(barrel, 0, exact),
                "Exact incoming identity was not classified as an allowed reserved match");
        helper.assertTrue(ContainerSlotReservationsApi.classify(barrel, 0, other)
                        == ReservationSlotClass.RESERVED_OTHER
                        && !ContainerSlotReservationsApi.mayInsert(barrel, 0, other),
                "Distinct incoming identity was not classified as a rejected reservation");
        helper.assertTrue(ContainerSlotReservationsApi.classify(barrel, 1, other)
                        == ReservationSlotClass.UNRESERVED_EMPTY,
                "An empty unreserved destination lost its explicit routing classification");

        barrel.setItem(0, exact.copyWithCount(3));
        helper.assertTrue(ContainerSlotReservationsApi.classify(barrel, 0, exact)
                        == ReservationSlotClass.OCCUPIED_COMPATIBLE,
                "A non-full exact physical stack was not classified as merge-compatible");
        barrel.setItem(0, exact.copyWithCount(exact.getMaxStackSize()));
        helper.assertTrue(ContainerSlotReservationsApi.classify(barrel, 0, exact)
                        == ReservationSlotClass.NON_WRITABLE,
                "A full physical stack was advertised as writable");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void chestAndBarrelFullMetadataSaveLoadRetainsExactReservations(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 2, 1);
        BlockPos barrelPos = new BlockPos(2, 2, 1);
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.setBlock(barrelPos, Blocks.BARREL);
        List<BlockEntity> originals = List.of(
                helper.getBlockEntity(chestPos, ChestBlockEntity.class),
                helper.getBlockEntity(barrelPos, BarrelBlockEntity.class)
        );
        ItemStack reserved = identity(Items.RABBIT_STEW, 9, "ramen", "Ramen");
        ItemStack physical = identity(Items.POISONOUS_POTATO, 4, "green_curry", "Green Curry");

        for (BlockEntity original : originals) {
            Container container = (Container) original;
            container.setItem(3, physical.copy());
            ReservationStore.set(SupportedContainerResolver.resolve(container, 21).orElseThrow(), reserved);
            CompoundTag serialized = original.saveWithFullMetadata(helper.getLevel().registryAccess());
            BlockEntity loaded = BlockEntity.loadStatic(
                    original.getBlockPos(),
                    original.getBlockState(),
                    serialized,
                    helper.getLevel().registryAccess()
            );
            helper.assertTrue(loaded != null
                            && loaded.getType() == original.getType()
                            && ReservationStore.getData(loaded).matches(21, reserved)
                            && ((Container) loaded).getItem(3).getCount() == physical.getCount()
                            && ItemStack.isSameItemSameComponents(((Container) loaded).getItem(3), physical),
                    "Full block-entity save/load lost type, contents, slot index, or exact reservation components: "
                            + original.getClass());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void shulkerComponentsCollectAndApplyRoundTripReservationsAndContents(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.SHULKER_BOX);
        ShulkerBoxBlockEntity original = helper.getBlockEntity(pos, ShulkerBoxBlockEntity.class);
        Component customName = Component.literal("Component round-trip fixture");
        ItemStack itemOrigin = new ItemStack(Blocks.SHULKER_BOX);
        itemOrigin.set(DataComponents.CUSTOM_NAME, customName);
        original.applyComponentsFromItemStack(itemOrigin);

        ItemStack reserved = identity(Items.RABBIT_STEW, 1, "ramen", "Ramen");
        ItemStack physical = identity(Items.POISONOUS_POTATO, 4, "green_curry", "Green Curry");
        original.setItem(4, physical.copy());
        ReservationStore.set(SupportedContainerResolver.resolve(original, 5).orElseThrow(), reserved);

        DataComponentMap collected = original.collectComponents();
        ItemStack dropped = new ItemStack(Blocks.SHULKER_BOX);
        dropped.applyComponents(collected);
        helper.assertTrue(ReservationStore.getData(dropped).matches(5, reserved)
                        && customName.equals(dropped.get(DataComponents.CUSTOM_NAME)),
                "collectComponents did not preserve the reservation and unrelated custom name on the item");

        ShulkerBoxBlockEntity restored = new ShulkerBoxBlockEntity(
                BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState());
        restored.applyComponentsFromItemStack(dropped);
        helper.assertTrue(ReservationStore.getData(restored).matches(5, reserved)
                        && ItemStack.isSameItemSameComponents(restored.getItem(4), physical)
                        && restored.getItem(4).getCount() == physical.getCount()
                        && customName.equals(restored.getCustomName()),
                "applyComponentsFromItemStack did not restore reservation, contents, and unrelated name");
        helper.assertTrue(dropped.has(ModComponents.RESERVATIONS),
                "The retained shulker item lacks the registered reservation component");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void actualShulkerLootDropRetainsReservationContentsAndName(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        Block shulkerBlock = Blocks.DYED_SHULKER_BOX.blue();
        helper.setBlock(pos, shulkerBlock);
        ShulkerBoxBlockEntity shulker = helper.getBlockEntity(pos, ShulkerBoxBlockEntity.class);
        Component customName = Component.literal("Loot survival fixture");
        ItemStack itemOrigin = new ItemStack(shulkerBlock);
        itemOrigin.set(DataComponents.CUSTOM_NAME, customName);
        itemOrigin.set(DataComponents.RARITY, Rarity.EPIC);
        shulker.applyComponentsFromItemStack(itemOrigin);
        ItemStack reserved = identity(Items.RABBIT_STEW, 1, "ramen", "Ramen");
        ItemStack physical = identity(Items.POISONOUS_POTATO, 4, "green_curry", "Green Curry");
        shulker.setItem(4, physical.copy());
        ReservationStore.set(SupportedContainerResolver.resolve(shulker, 5).orElseThrow(), reserved);

        ItemStack drop = Block.getDrops(
                        helper.getBlockState(pos),
                        helper.getLevel(),
                        helper.absolutePos(pos),
                        shulker,
                        helper.makeMockServerPlayerInLevel(),
                        ItemStack.EMPTY
                ).stream()
                .filter(stack -> stack.getItem() == shulkerBlock.asItem())
                .findFirst()
                .orElseThrow();
        helper.assertTrue(ReservationStore.getData(drop).matches(5, reserved)
                        && customName.equals(drop.get(DataComponents.CUSTOM_NAME))
                        && drop.getOrDefault(DataComponents.RARITY, Rarity.COMMON) == Rarity.EPIC,
                "Actual shulker loot did not retain reservation, name, and unrelated rarity components");

        ShulkerBoxBlockEntity restored = new ShulkerBoxBlockEntity(
                BlockPos.ZERO, shulkerBlock.defaultBlockState());
        restored.applyComponentsFromItemStack(drop);
        helper.assertTrue(ReservationStore.getData(restored).matches(5, reserved)
                        && ItemStack.isSameItemSameComponents(restored.getItem(4), physical)
                        && restored.getItem(4).getCount() == physical.getCount()
                        && customName.equals(restored.getCustomName())
                        && restored.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON) == Rarity.EPIC,
                "The actual retained shulker drop did not restore reservation, contents, name, and rarity");
        helper.succeed();
    }

    private static void assertPhysicalOwner(GameTestHelper helper, BlockEntity blockEntity, int slot) {
        SupportedContainerResolver.ResolvedSlot resolved = SupportedContainerResolver
                .resolve((Container) blockEntity, slot).orElseThrow();
        helper.assertTrue(resolved.owner() == blockEntity
                        && resolved.blockEntity() == blockEntity
                        && resolved.localSlot() == slot,
                "Resolver did not retain exact physical ownership for " + blockEntity.getClass());
    }

    private static ItemStack identity(Item item, int count, String model, String name) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("container_slot_reservations_fixture", model));
        stack.set(DataComponents.ITEM_NAME, Component.literal(name));
        return stack;
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}

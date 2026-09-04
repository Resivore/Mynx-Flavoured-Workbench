package dev.resivore.slotreservations.gametest;

import dev.resivore.slotreservations.ModComponents;
import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.ReservationNetworking;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import dev.resivore.slotreservations.api.ReservationSlotClass;
import dev.resivore.slotreservations.menu.ReservationAwareShulkerBoxSlot;
import dev.resivore.slotreservations.menu.ReservationAwareSlot;
import dev.resivore.slotreservations.network.ReservationSnapshotPayload;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.FurnaceFuelSlot;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

    @GameTest(maxTicks = 80)
    public void canaryFourExactSupportMatrixIncludesCopperMachinesAndEnderOnly(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        List<Block> supported = new ArrayList<>(List.of(
                Blocks.CHEST,
                Blocks.TRAPPED_CHEST,
                Blocks.BARREL,
                Blocks.SHULKER_BOX,
                Blocks.DISPENSER,
                Blocks.DROPPER,
                Blocks.HOPPER,
                Blocks.FURNACE,
                Blocks.BLAST_FURNACE,
                Blocks.SMOKER,
                Blocks.BREWING_STAND,
                Blocks.CRAFTER
        ));
        supported.addAll(Blocks.COPPER_CHEST.asList());
        supported.addAll(Blocks.DYED_SHULKER_BOX.asList());

        for (Block block : supported) {
            BlockEntity blockEntity = placeBlockEntity(helper, pos, block);
            helper.assertTrue(blockEntity instanceof Container,
                    "Requested support block is not a physical Container: " + block);
            Container container = (Container) blockEntity;
            int lastSlot = container.getContainerSize() - 1;
            SupportedContainerResolver.ResolvedSlot first = SupportedContainerResolver.resolve(container, 0)
                    .orElseThrow();
            SupportedContainerResolver.ResolvedSlot last = SupportedContainerResolver.resolve(container, lastSlot)
                    .orElseThrow();
            helper.assertTrue(first.owner() == blockEntity
                            && last.owner() == blockEntity
                            && first.ownerSlotCount() == container.getContainerSize()
                            && last.localSlot() == lastSlot
                            && SupportedContainerResolver.resolve(container, container.getContainerSize()).isEmpty(),
                    "Requested owner did not expose exactly its physical slot range: " + block);
        }

        var player = helper.makeMockServerPlayer(GameType.CREATIVE);
        PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
        SupportedContainerResolver.ResolvedSlot enderLast = SupportedContainerResolver.resolve(enderChest, 26)
                .orElseThrow();
        helper.assertTrue(enderLast.owner() == enderChest
                        && enderLast.blockEntity() == null
                        && enderLast.ownerSlotCount() == 27
                        && SupportedContainerResolver.resolve(enderChest, 27).isEmpty(),
                "Player Ender Chest did not resolve as one non-block 27-slot owner");

        helper.assertTrue(SupportedContainerResolver.resolve(new SimpleContainer(27), 0).isEmpty(),
                "An arbitrary Container implementation entered the exact support allowlist");
        for (Block excluded : List.of(Blocks.DECORATED_POT, Blocks.CHISELED_BOOKSHELF, Blocks.LECTERN)) {
            BlockEntity blockEntity = placeBlockEntity(helper, pos, excluded);
            if (blockEntity instanceof Container container) {
                helper.assertTrue(SupportedContainerResolver.resolve(container, 0).isEmpty(),
                        "An explicitly excluded block entered the support allowlist: " + excluded);
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void everyCopperStateRetainsDoubleChestPhysicalHalfReservations(GameTestHelper helper) {
        List<Block> copperStates = Blocks.COPPER_CHEST.asList();
        helper.assertTrue(copperStates.size() == 8,
                "Minecraft 26.2 no longer exposes the audited eight-state Copper Chest family");

        BlockPos leftPos = new BlockPos(1, 2, 1);
        BlockPos rightPos = new BlockPos(2, 2, 1);
        ChestBlock initialBlock = (ChestBlock) copperStates.getFirst();
        helper.setBlock(rightPos, copperChestState(initialBlock, ChestType.RIGHT));
        helper.setBlock(leftPos, copperChestState(initialBlock, ChestType.LEFT));
        ChestBlockEntity leftOwner = helper.getBlockEntity(leftPos, ChestBlockEntity.class);
        ChestBlockEntity rightOwner = helper.getBlockEntity(rightPos, ChestBlockEntity.class);
        ItemStack leftReservation = identity(Items.POISONOUS_POTATO, 1, "green_curry", "Green Curry");
        ItemStack rightReservation = identity(Items.RABBIT_STEW, 1, "ramen", "Ramen");
        ReservationStore.set(SupportedContainerResolver.resolve(leftOwner, 4).orElseThrow(), leftReservation);
        ReservationStore.set(SupportedContainerResolver.resolve(rightOwner, 4).orElseThrow(), rightReservation);

        for (Block copperState : copperStates) {
            ChestBlock copperChest = (ChestBlock) copperState;
            helper.getLevel().setBlock(
                    helper.absolutePos(leftPos), copperChestState(copperChest, ChestType.LEFT), 3);
            helper.assertTrue(helper.getBlockState(leftPos).getBlock() == copperState,
                    "Failed to transition the left Copper Chest half to " + copperState);
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(leftPos)) == leftOwner
                            && ReservationStore.getData(leftOwner).matches(4, leftReservation)
                            && ReservationStore.getData(rightOwner).matches(4, rightReservation),
                    "Changing one Copper Chest half replaced an owner or migrated reservation data");

            helper.getLevel().setBlock(
                    helper.absolutePos(rightPos), copperChestState(copperChest, ChestType.RIGHT), 3);
            helper.assertTrue(helper.getBlockState(rightPos).getBlock() == copperState,
                    "Failed to transition the right Copper Chest half to " + copperState);
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(rightPos)) == rightOwner,
                    "Changing the right Copper Chest state replaced its stable block entity");

            Container logical = ChestBlock.getContainer(
                    copperChest,
                    helper.getBlockState(leftPos),
                    helper.getLevel(),
                    helper.absolutePos(leftPos),
                    true
            );
            helper.assertTrue(logical instanceof CompoundContainer,
                    "Copper Chest state did not retain the vanilla double-container view: " + copperState);
            SupportedContainerResolver.ResolvedSlot first = SupportedContainerResolver.resolve(logical, 0)
                    .orElseThrow();
            SupportedContainerResolver.ResolvedSlot second = SupportedContainerResolver.resolve(logical, 27)
                    .orElseThrow();
            helper.assertTrue(first.localSlot() == 0
                            && second.localSlot() == 0
                            && first.owner() != second.owner()
                            && (first.owner() == leftOwner || first.owner() == rightOwner)
                            && (second.owner() == leftOwner || second.owner() == rightOwner)
                            && ReservationStore.getData(leftOwner).matches(4, leftReservation)
                            && ReservationStore.getData(rightOwner).matches(4, rightReservation),
                    "Copper double-chest ownership changed or crossed physical persistence halves");

            Container fromOtherHalf = ChestBlock.getContainer(
                    copperChest,
                    helper.getBlockState(rightPos),
                    helper.getLevel(),
                    helper.absolutePos(rightPos),
                    true
            );
            helper.assertTrue(SupportedContainerResolver.resolve(fromOtherHalf, 0).orElseThrow().owner()
                            == first.owner()
                            && SupportedContainerResolver.resolve(fromOtherHalf, 27).orElseThrow().owner()
                            == second.owner(),
                    "Opening the other Copper Chest half changed logical-to-physical ordering");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void trappedDoubleChestKeepsTwoPhysicalReservationOwners(GameTestHelper helper) {
        BlockPos leftPos = new BlockPos(1, 2, 1);
        BlockPos rightPos = new BlockPos(2, 2, 1);
        ChestBlock trapped = (ChestBlock) Blocks.TRAPPED_CHEST;
        helper.setBlock(rightPos, trapped.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.RIGHT));
        helper.setBlock(leftPos, trapped.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.LEFT));

        Container logical = ChestBlock.getContainer(
                trapped,
                helper.getBlockState(leftPos),
                helper.getLevel(),
                helper.absolutePos(leftPos),
                true
        );
        helper.assertTrue(logical instanceof CompoundContainer,
                "The trapped pair did not form an actual vanilla double-chest container");
        SupportedContainerResolver.ResolvedSlot first = SupportedContainerResolver.resolve(logical, 0)
                .orElseThrow();
        SupportedContainerResolver.ResolvedSlot second = SupportedContainerResolver.resolve(logical, 27)
                .orElseThrow();
        ItemStack firstReservation = identity(
                Items.POISONOUS_POTATO, 1, "trapped_first", "Trapped First");
        ItemStack secondReservation = identity(
                Items.RABBIT_STEW, 1, "trapped_second", "Trapped Second");
        ReservationStore.set(first, firstReservation);
        ReservationStore.set(second, secondReservation);

        Container reopened = ChestBlock.getContainer(
                trapped,
                helper.getBlockState(rightPos),
                helper.getLevel(),
                helper.absolutePos(rightPos),
                true
        );
        helper.assertTrue(first.owner() != second.owner()
                        && first.localSlot() == 0
                        && second.localSlot() == 0
                        && SupportedContainerResolver.resolve(reopened, 0).orElseThrow().owner()
                        == first.owner()
                        && SupportedContainerResolver.resolve(reopened, 27).orElseThrow().owner()
                        == second.owner()
                        && ContainerSlotReservationsApi.reservationMatches(reopened, 0, firstReservation)
                        && ContainerSlotReservationsApi.reservationMatches(reopened, 27, secondReservation),
                "Trapped double-chest reservations crossed or changed physical owners");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void requestedMenusSnapshotOnlyPhysicalSlotsAndKeepSpecializedClasses(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        var player = helper.makeMockServerPlayerInLevel();
        int menuId = 40;

        Container dispenser = (Container) placeBlockEntity(helper, pos, Blocks.DISPENSER);
        DispenserMenu dispenserMenu = new DispenserMenu(menuId++, player.getInventory(), dispenser);
        assertMenuSnapshot(helper, dispenserMenu, 9);
        helper.assertTrue(dispenserMenu.getSlot(0).getClass() == Slot.class,
                "Dispenser menu layout no longer uses its ordinary vanilla Slot class");
        dispenserMenu.removed(player);

        Container dropper = (Container) placeBlockEntity(helper, pos, Blocks.DROPPER);
        DispenserMenu dropperMenu = new DispenserMenu(menuId++, player.getInventory(), dropper);
        assertMenuSnapshot(helper, dropperMenu, 9);
        dropperMenu.removed(player);

        Container hopper = (Container) placeBlockEntity(helper, pos, Blocks.HOPPER);
        HopperMenu hopperMenu = new HopperMenu(menuId++, player.getInventory(), hopper);
        assertMenuSnapshot(helper, hopperMenu, 5);
        hopperMenu.removed(player);

        Container furnace = (Container) placeBlockEntity(helper, pos, Blocks.FURNACE);
        FurnaceMenu furnaceMenu = new FurnaceMenu(
                menuId++, player.getInventory(), furnace, new SimpleContainerData(4));
        assertFurnaceMenu(helper, furnaceMenu);
        furnaceMenu.removed(player);

        Container blastFurnace = (Container) placeBlockEntity(helper, pos, Blocks.BLAST_FURNACE);
        BlastFurnaceMenu blastMenu = new BlastFurnaceMenu(
                menuId++, player.getInventory(), blastFurnace, new SimpleContainerData(4));
        assertFurnaceMenu(helper, blastMenu);
        blastMenu.removed(player);

        Container smoker = (Container) placeBlockEntity(helper, pos, Blocks.SMOKER);
        SmokerMenu smokerMenu = new SmokerMenu(
                menuId++, player.getInventory(), smoker, new SimpleContainerData(4));
        assertFurnaceMenu(helper, smokerMenu);
        smokerMenu.removed(player);

        Container brewing = (Container) placeBlockEntity(helper, pos, Blocks.BREWING_STAND);
        BrewingStandMenu brewingMenu = new BrewingStandMenu(
                menuId++, player.getInventory(), brewing, new SimpleContainerData(2));
        assertMenuSnapshot(helper, brewingMenu, 5);
        helper.assertTrue(brewingMenu.getSlot(0).getClass().getSimpleName().equals("PotionSlot")
                        && brewingMenu.getSlot(1).getClass().getSimpleName().equals("PotionSlot")
                        && brewingMenu.getSlot(2).getClass().getSimpleName().equals("PotionSlot")
                        && brewingMenu.getSlot(3).getClass().getSimpleName().equals("IngredientsSlot")
                        && brewingMenu.getSlot(4).getClass().getSimpleName().equals("FuelSlot"),
                "Brewing Stand specialized slot classes were replaced or reordered");
        brewingMenu.removed(player);

        CrafterBlockEntity crafter = (CrafterBlockEntity) placeBlockEntity(helper, pos, Blocks.CRAFTER);
        CrafterMenu crafterMenu = new CrafterMenu(
                menuId++, player.getInventory(), crafter, new SimpleContainerData(10));
        assertMenuSnapshot(helper, crafterMenu, 9);
        for (int slot = 0; slot < 9; slot++) {
            helper.assertTrue(crafterMenu.getSlot(slot) instanceof CrafterSlot,
                    "Crafter grid slot lost its CrafterSlot behavior at " + slot);
        }
        helper.assertTrue(crafterMenu.getSlot(crafterMenu.slots.size() - 1).getClass().getSimpleName()
                        .equals("NonInteractiveResultSlot"),
                "Crafter result slot lost its non-interactive vanilla subtype");
        crafterMenu.removed(player);

        Container copper = (Container) placeBlockEntity(
                helper, pos, Blocks.COPPER_CHEST.asList().getFirst());
        ChestMenu copperMenu = ChestMenu.threeRows(menuId++, player.getInventory(), copper);
        assertMenuSnapshot(helper, copperMenu, 27);
        copperMenu.removed(player);

        Container shulker = (Container) placeBlockEntity(helper, pos, Blocks.SHULKER_BOX);
        ShulkerBoxMenu shulkerMenu = new ShulkerBoxMenu(menuId++, player.getInventory(), shulker);
        assertMenuSnapshot(helper, shulkerMenu, 27);
        helper.assertTrue(shulkerMenu.getSlot(0) instanceof ReservationAwareShulkerBoxSlot,
                "Shulker menu lost its reservation-aware ShulkerBoxSlot subtype");
        shulkerMenu.removed(player);

        PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
        ChestMenu enderMenu = ChestMenu.threeRows(menuId, player.getInventory(), enderChest);
        assertMenuSnapshot(helper, enderMenu, 27);
        enderMenu.removed(player);
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void nativeClassificationPreservesFurnaceOutputAndDisabledCrafterPolicy(GameTestHelper helper) {
        BlockPos furnacePos = new BlockPos(1, 2, 1);
        BlockPos crafterPos = new BlockPos(2, 2, 1);
        helper.setBlock(furnacePos, Blocks.FURNACE);
        helper.setBlock(crafterPos, Blocks.CRAFTER);
        FurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos, FurnaceBlockEntity.class);
        CrafterBlockEntity crafter = helper.getBlockEntity(crafterPos, CrafterBlockEntity.class);

        ItemStack input = identity(Items.RAW_IRON, 1, "furnace_input", "Furnace Input");
        ItemStack inputMismatch = identity(Items.RAW_IRON, 1, "other_input", "Other Input");
        ItemStack fuel = identity(Items.COAL, 1, "furnace_fuel", "Furnace Fuel");
        ItemStack output = identity(Items.IRON_INGOT, 1, "furnace_output", "Furnace Output");
        ReservationStore.set(SupportedContainerResolver.resolve(furnace, 0).orElseThrow(), input);
        ReservationStore.set(SupportedContainerResolver.resolve(furnace, 1).orElseThrow(), fuel);
        ReservationStore.set(SupportedContainerResolver.resolve(furnace, 2).orElseThrow(), output);

        helper.assertTrue(ContainerSlotReservationsApi.classify(furnace, 0, input)
                        == ReservationSlotClass.RESERVED_MATCH
                        && ContainerSlotReservationsApi.classify(furnace, 0, inputMismatch)
                        == ReservationSlotClass.RESERVED_OTHER,
                "Furnace input lost native-writable matching/mismatch classification");
        helper.assertTrue(ContainerSlotReservationsApi.classify(furnace, 1, fuel)
                        == ReservationSlotClass.RESERVED_MATCH,
                "Furnace fuel slot rejected native fuel before reservation admission");
        helper.assertTrue(ContainerSlotReservationsApi.classify(furnace, 2, output)
                        == ReservationSlotClass.NON_WRITABLE
                        && !ContainerSlotReservationsApi.mayInsert(furnace, 2, output)
                        && !furnace.canPlaceItem(2, output),
                "A matching reservation made the furnace result slot externally writable");

        ItemStack craftItem = identity(Items.COBBLESTONE, 1, "crafter", "Crafter Item");
        ItemStack craftMismatch = identity(Items.COBBLESTONE, 1, "crafter_other", "Other Crafter Item");
        ReservationStore.set(SupportedContainerResolver.resolve(crafter, 0).orElseThrow(), craftItem);
        crafter.setSlotState(0, false);
        helper.assertTrue(ContainerSlotReservationsApi.classify(crafter, 0, craftItem)
                        == ReservationSlotClass.NON_WRITABLE
                        && !crafter.canPlaceItem(0, craftItem),
                "A matching reservation bypassed a disabled Crafter slot");
        crafter.setSlotState(0, true);
        helper.assertTrue(ContainerSlotReservationsApi.classify(crafter, 0, craftItem)
                        == ReservationSlotClass.RESERVED_MATCH
                        && crafter.canPlaceItem(0, craftItem)
                        && !crafter.canPlaceItem(0, craftMismatch),
                "Re-enabled Crafter slot did not restore native AND reservation admission");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void everyMachineComposesNativeAndReservationAdmission(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        ItemStack general = identity(Items.COBBLESTONE, 1, "machine_general", "Machine General");
        ItemStack generalOther = identity(
                Items.COBBLESTONE, 1, "machine_general_other", "Machine General Other");
        for (Block block : List.of(Blocks.DISPENSER, Blocks.DROPPER, Blocks.HOPPER)) {
            Container container = (Container) placeBlockEntity(helper, pos, block);
            ReservationStore.set(
                    SupportedContainerResolver.resolve(container, 0).orElseThrow(), general);
            helper.assertTrue(container.canPlaceItem(0, general)
                            && !container.canPlaceItem(0, generalOther)
                            && ContainerSlotReservationsApi.classify(container, 0, general)
                            == ReservationSlotClass.RESERVED_MATCH
                            && ContainerSlotReservationsApi.classify(container, 0, generalOther)
                            == ReservationSlotClass.RESERVED_OTHER,
                    "Ordinary machine admission did not compose native and CSR policy: " + block);
        }

        ItemStack furnaceInput = identity(Items.RAW_IRON, 1, "machine_input", "Machine Input");
        ItemStack furnaceInputOther = identity(
                Items.RAW_IRON, 1, "machine_input_other", "Machine Input Other");
        ItemStack furnaceFuel = identity(Items.COAL, 1, "machine_fuel", "Machine Fuel");
        ItemStack furnaceFuelOther = identity(
                Items.COAL, 1, "machine_fuel_other", "Machine Fuel Other");
        ItemStack furnaceOutput = identity(
                Items.IRON_INGOT, 1, "machine_output", "Machine Output");
        for (Block block : List.of(Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER)) {
            AbstractFurnaceBlockEntity furnace =
                    (AbstractFurnaceBlockEntity) placeBlockEntity(helper, pos, block);
            ReservationStore.set(
                    SupportedContainerResolver.resolve(furnace, 0).orElseThrow(), furnaceInput);
            ReservationStore.set(
                    SupportedContainerResolver.resolve(furnace, 1).orElseThrow(), furnaceFuel);
            ReservationStore.set(
                    SupportedContainerResolver.resolve(furnace, 2).orElseThrow(), furnaceOutput);
            helper.assertTrue(furnace.canPlaceItem(0, furnaceInput)
                            && !furnace.canPlaceItem(0, furnaceInputOther)
                            && furnace.canPlaceItem(1, furnaceFuel)
                            && !furnace.canPlaceItem(1, furnaceFuelOther)
                            && !furnace.canPlaceItem(1, general)
                            && !furnace.canPlaceItem(2, furnaceOutput)
                            && furnace.canPlaceItemThroughFace(0, furnaceInput, Direction.UP)
                            && !furnace.canPlaceItemThroughFace(0, furnaceInputOther, Direction.UP),
                    "Furnace-family native, sided, or reservation admission changed: " + block);
        }

        BrewingStandBlockEntity brewing =
                (BrewingStandBlockEntity) placeBlockEntity(helper, pos, Blocks.BREWING_STAND);
        ItemStack potion = identity(Items.POTION, 1, "brew_potion", "Brew Potion");
        ItemStack potionOther = identity(Items.POTION, 1, "brew_potion_other", "Brew Potion Other");
        ItemStack ingredient = identity(Items.NETHER_WART, 1, "brew_ingredient", "Brew Ingredient");
        ItemStack ingredientOther = identity(
                Items.NETHER_WART, 1, "brew_ingredient_other", "Brew Ingredient Other");
        ItemStack brewingFuel = identity(Items.BLAZE_POWDER, 1, "brew_fuel", "Brew Fuel");
        ItemStack brewingFuelOther = identity(
                Items.BLAZE_POWDER, 1, "brew_fuel_other", "Brew Fuel Other");
        ReservationStore.set(SupportedContainerResolver.resolve(brewing, 0).orElseThrow(), potion);
        ReservationStore.set(SupportedContainerResolver.resolve(brewing, 3).orElseThrow(), ingredient);
        ReservationStore.set(SupportedContainerResolver.resolve(brewing, 4).orElseThrow(), brewingFuel);
        helper.assertTrue(brewing.canPlaceItem(0, potion)
                        && !brewing.canPlaceItem(0, potionOther)
                        && brewing.canPlaceItem(3, ingredient)
                        && !brewing.canPlaceItem(3, ingredientOther)
                        && brewing.canPlaceItem(4, brewingFuel)
                        && !brewing.canPlaceItem(4, brewingFuelOther)
                        && !brewing.canPlaceItem(4, general)
                        && brewing.canPlaceItemThroughFace(3, ingredient, Direction.UP)
                        && !brewing.canPlaceItemThroughFace(3, ingredientOther, Direction.UP),
                "Brewing Stand native slot, sided, or reservation admission changed");

        CrafterBlockEntity crafter =
                (CrafterBlockEntity) placeBlockEntity(helper, pos, Blocks.CRAFTER);
        ReservationStore.set(SupportedContainerResolver.resolve(crafter, 0).orElseThrow(), general);
        helper.assertTrue(crafter.canPlaceItem(0, general)
                        && !crafter.canPlaceItem(0, generalOther),
                "Enabled Crafter slot did not apply its matching reservation");
        crafter.setSlotState(0, false);
        helper.assertTrue(!crafter.canPlaceItem(0, general)
                        && ContainerSlotReservationsApi.classify(crafter, 0, general)
                        == ReservationSlotClass.NON_WRITABLE,
                "Disabled Crafter slot became writable through its reservation");
        crafter.setSlotState(0, true);
        helper.assertTrue(crafter.canPlaceItem(0, general),
                "Re-enabled Crafter slot did not restore matching reservation admission");
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void machineManualQuickMoveAndBackingAdmissionHonorExactReservation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.DISPENSER);
        DispenserBlockEntity dispenser = helper.getBlockEntity(pos, DispenserBlockEntity.class);
        ItemStack reserved = identity(Items.POISONOUS_POTATO, 1, "green_curry", "Green Curry");
        ItemStack wrong = identity(Items.POISONOUS_POTATO, 5, "ramen", "Ramen");
        ReservationStore.set(SupportedContainerResolver.resolve(dispenser, 0).orElseThrow(), reserved);
        helper.assertTrue(dispenser.canPlaceItem(0, reserved.copyWithCount(2))
                        && !dispenser.canPlaceItem(0, wrong),
                "Machine backing-container admission did not apply the exact reservation gate");

        var player = helper.makeMockServerPlayerInLevel();
        DispenserMenu menu = new DispenserMenu(71, player.getInventory(), dispenser);
        menu.setCarried(wrong.copy());
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(dispenser.getItem(0).isEmpty()
                        && ItemStack.isSameItemSameComponents(menu.getCarried(), wrong),
                "Manual placement admitted a component-distinct stack into a machine slot");

        menu.setCarried(reserved.copyWithCount(4));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(dispenser.getItem(0).getCount() == 4
                        && ItemStack.isSameItemSameComponents(dispenser.getItem(0), reserved),
                "Manual placement rejected a matching machine reservation");

        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(dispenser.getItem(0).isEmpty()
                        && ItemStack.isSameItemSameComponents(menu.getCarried(), reserved),
                "Reservation policy changed ordinary machine-slot extraction");
        menu.setCarried(ItemStack.EMPTY);
        for (int slot = 1; slot < dispenser.getContainerSize(); slot++) {
            dispenser.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        player.getInventory().setItem(9, wrong.copy());
        menu.quickMoveStack(player, 9);
        helper.assertTrue(dispenser.getItem(0).isEmpty()
                        && player.getInventory().getItem(9).getCount() == wrong.getCount(),
                "QUICK_MOVE admitted a mismatching machine reservation");
        player.getInventory().setItem(9, reserved.copyWithCount(7));
        menu.quickMoveStack(player, 9);
        helper.assertTrue(dispenser.getItem(0).getCount() == 7
                        && ItemStack.isSameItemSameComponents(dispenser.getItem(0), reserved)
                        && player.getInventory().getItem(9).isEmpty(),
                "QUICK_MOVE rejected a matching machine reservation");
        menu.removed(player);
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void machineComponentsPersistAndFurnaceProcessingIgnoresReservationWrites(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        List<Block> machines = List.of(
                Blocks.DISPENSER,
                Blocks.DROPPER,
                Blocks.HOPPER,
                Blocks.FURNACE,
                Blocks.BLAST_FURNACE,
                Blocks.SMOKER,
                Blocks.BREWING_STAND,
                Blocks.CRAFTER
        );
        ItemStack reservation = identity(Items.RABBIT_STEW, 1, "persistent", "Persistent Reservation");
        ItemStack physical = identity(Items.POISONOUS_POTATO, 3, "physical", "Physical Stack");
        for (Block machine : machines) {
            BlockEntity original = placeBlockEntity(helper, pos, machine);
            Container container = (Container) original;
            int reservedSlot = container.getContainerSize() - 1;
            container.setItem(0, physical.copy());
            ReservationStore.set(
                    SupportedContainerResolver.resolve(container, reservedSlot).orElseThrow(), reservation);
            CompoundTag serialized = original.saveWithFullMetadata(helper.getLevel().registryAccess());
            BlockEntity loaded = BlockEntity.loadStatic(
                    original.getBlockPos(), original.getBlockState(), serialized, helper.getLevel().registryAccess());
            helper.assertTrue(loaded instanceof Container loadedContainer
                            && ReservationStore.getData(loaded).matches(reservedSlot, reservation)
                            && loadedContainer.getItem(0).getCount() == physical.getCount()
                            && ItemStack.isSameItemSameComponents(loadedContainer.getItem(0), physical),
                    "Machine save/load lost physical contents or reservation component: " + machine);
        }

        helper.setBlock(pos, Blocks.FURNACE);
        FurnaceBlockEntity furnace = helper.getBlockEntity(pos, FurnaceBlockEntity.class);
        ItemStack outputReservation = identity(Items.DIAMOND, 1, "reserved_output", "Reserved Output");
        ReservationStore.set(SupportedContainerResolver.resolve(furnace, 2).orElseThrow(), outputReservation);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        helper.assertTrue(ContainerSlotReservationsApi.classify(furnace, 2, outputReservation)
                        == ReservationSlotClass.NON_WRITABLE,
                "Furnace output fixture was not externally non-writable before processing");
        for (int tick = 0; tick < 205; tick++) {
            AbstractFurnaceBlockEntity.serverTick(
                    helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), furnace);
        }
        helper.assertTrue(furnace.getItem(0).isEmpty()
                        && furnace.getItem(2).is(Items.IRON_INGOT)
                        && ReservationStore.getData(furnace).matches(2, outputReservation),
                "Reservation admission blocked or rewrote vanilla furnace processing/output");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void enderReservationsPersistRemainIsolatedAndFollowRespawnInventory(GameTestHelper helper) {
        ServerPlayer owner = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        ServerPlayer otherPlayer = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        PlayerEnderChestContainer ownerChest = owner.getEnderChestInventory();
        PlayerEnderChestContainer otherChest = otherPlayer.getEnderChestInventory();
        ItemStack reservation = identity(Items.RABBIT_STEW, 1, "ender", "Ender Reservation");
        ItemStack physical = identity(Items.POISONOUS_POTATO, 4, "ender_physical", "Ender Physical");
        ownerChest.setItem(6, physical.copy());
        ReservationStore.set(SupportedContainerResolver.resolve(ownerChest, 12).orElseThrow(), reservation);
        helper.assertTrue(ContainerSlotReservationsApi.reservationMatches(ownerChest, 12, reservation)
                        && !ContainerSlotReservationsApi.isReserved(otherChest, 12),
                "One player's Ender Chest reservation leaked to another player");

        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        owner.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        helper.assertTrue(saved.contains("container_slot_reservations:ender_chest_reservations"),
                "Player save omitted the namespaced Ender Chest reservation payload");

        ServerPlayer loadedPlayer = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        loadedPlayer.load(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(ContainerSlotReservationsApi.reservationMatches(
                            loadedPlayer.getEnderChestInventory(), 12, reservation)
                        && loadedPlayer.getEnderChestInventory().getItem(6).getCount() == physical.getCount()
                        && ItemStack.isSameItemSameComponents(
                            loadedPlayer.getEnderChestInventory().getItem(6), physical),
                "Player save/load lost Ender Chest reservation or physical contents");

        CompoundTag legacy = saved.copy();
        legacy.remove("container_slot_reservations:ender_chest_reservations");
        ServerPlayer legacyPlayer = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        ReservationStore.set(
                SupportedContainerResolver.resolve(legacyPlayer.getEnderChestInventory(), 12).orElseThrow(),
                reservation);
        legacyPlayer.load(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacy));
        helper.assertTrue(!ContainerSlotReservationsApi.isReserved(
                        legacyPlayer.getEnderChestInventory(), 12),
                "Old player data without the new key did not default reservations to empty");

        ServerPlayer respawned = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        respawned.restoreFrom(owner, false);
        helper.assertTrue(respawned.getEnderChestInventory() == ownerChest
                        && ContainerSlotReservationsApi.reservationMatches(
                            respawned.getEnderChestInventory(), 12, reservation)
                        && !ContainerSlotReservationsApi.isReserved(otherChest, 12),
                "Respawn did not retain the player-owned Ender inventory independently of keep-inventory");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void optionalDoubleBarrelsMapAndRetainVerifiedPhysicalHalves(GameTestHelper helper)
            throws ReflectiveOperationException {
        if (!FabricLoader.getInstance().isModLoaded("doublebarrels")) {
            helper.succeed();
            return;
        }

        BlockPos mainPos = new BlockPos(1, 2, 1);
        BlockPos partnerPos = new BlockPos(2, 2, 1);
        helper.setBlock(mainPos, Blocks.BARREL);
        helper.setBlock(partnerPos, Blocks.BARREL);
        BarrelBlockEntity main = helper.getBlockEntity(mainPos, BarrelBlockEntity.class);
        BarrelBlockEntity partner = helper.getBlockEntity(partnerPos, BarrelBlockEntity.class);
        Class<?> access = Class.forName("com.mozko.doublebarrels.DoubleBarrelAccess", false,
                main.getClass().getClassLoader());
        helper.assertTrue(access.isInstance(main) && access.isInstance(partner),
                "Loaded Double Barrels did not apply its audited BarrelBlockEntity access contract");
        Method connect = access.getMethod("connectTo", BarrelBlockEntity.class);
        Method disconnect = access.getMethod("disconnect");
        Method combined = access.getMethod("getCombinedInventory");
        connect.invoke(main, partner);

        Container openedFromMain = (Container) combined.invoke(main);
        Container openedFromPartner = (Container) combined.invoke(partner);
        helper.assertTrue(openedFromMain != null && openedFromPartner != null,
                "Connected Double Barrels did not expose their audited combined inventory");
        assertDoubleBarrelMapping(helper, openedFromMain, main, partner);
        assertDoubleBarrelMapping(helper, openedFromPartner, main, partner);

        ItemStack mainReservation = identity(Items.POISONOUS_POTATO, 1, "double_main", "Double Main");
        ItemStack partnerReservation = identity(Items.RABBIT_STEW, 1, "double_partner", "Double Partner");
        ReservationStore.set(SupportedContainerResolver.resolve(openedFromMain, 26).orElseThrow(),
                mainReservation);
        ReservationStore.set(SupportedContainerResolver.resolve(openedFromMain, 27).orElseThrow(),
                partnerReservation);
        helper.assertTrue(ReservationStore.getData(main).matches(26, mainReservation)
                        && ReservationStore.getData(partner).matches(0, partnerReservation),
                "Double Barrel wrapper stored or crossed physical-half reservation data");

        disconnect.invoke(main);
        helper.assertTrue(ContainerSlotReservationsApi.reservationMatches(main, 26, mainReservation)
                        && ContainerSlotReservationsApi.reservationMatches(partner, 0, partnerReservation),
                "Disconnect moved or deleted physical-half reservations");
        connect.invoke(main, partner);
        Container reconnected = (Container) combined.invoke(partner);
        assertDoubleBarrelMapping(helper, reconnected, main, partner);
        helper.assertTrue(ContainerSlotReservationsApi.reservationMatches(reconnected, 26, mainReservation)
                        && ContainerSlotReservationsApi.reservationMatches(reconnected, 27, partnerReservation),
                "Reconnect swapped or lost Double Barrel reservations");
        disconnect.invoke(main);
        helper.succeed();
    }

    private static BlockEntity placeBlockEntity(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block);
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (blockEntity == null) {
            throw new AssertionError("Placed block did not create a block entity: " + block);
        }
        return blockEntity;
    }

    private static BlockState copperChestState(ChestBlock block, ChestType type) {
        return block.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, type);
    }

    private static void assertMenuSnapshot(
            GameTestHelper helper,
            AbstractContainerMenu menu,
            int expectedPhysicalSlots
    ) {
        List<ReservationSnapshotPayload.Entry> entries = snapshotEntries(menu);
        helper.assertTrue(entries.size() == expectedPhysicalSlots,
                "Menu snapshot exposed " + entries.size() + " slots instead of "
                        + expectedPhysicalSlots + " for " + menu.getClass());
        for (int slot = 0; slot < expectedPhysicalSlots; slot++) {
            helper.assertTrue(entries.get(slot).menuSlotIndex() == slot,
                    "Menu snapshot omitted, duplicated, or reordered physical slot " + slot
                            + " for " + menu.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ReservationSnapshotPayload.Entry> snapshotEntries(AbstractContainerMenu menu) {
        try {
            Method method = ReservationNetworking.class.getDeclaredMethod(
                    "snapshotEntries", AbstractContainerMenu.class);
            if (!method.trySetAccessible()) {
                throw new AssertionError("Could not access the server snapshot seam");
            }
            return (List<ReservationSnapshotPayload.Entry>) method.invoke(null, menu);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Server snapshot seam changed", exception);
        }
    }

    private static void assertFurnaceMenu(GameTestHelper helper, AbstractContainerMenu menu) {
        assertMenuSnapshot(helper, menu, 3);
        helper.assertTrue(menu.getSlot(0).getClass() == Slot.class
                        && menu.getSlot(1) instanceof FurnaceFuelSlot
                        && menu.getSlot(2) instanceof FurnaceResultSlot,
                "Furnace-family input, fuel, or result specialized slot class changed: "
                        + menu.getClass());
    }

    private static void assertDoubleBarrelMapping(
            GameTestHelper helper,
            Container combined,
            BarrelBlockEntity main,
            BarrelBlockEntity partner
    ) {
        SupportedContainerResolver.ResolvedSlot slot0 = SupportedContainerResolver.resolve(combined, 0)
                .orElseThrow();
        SupportedContainerResolver.ResolvedSlot slot26 = SupportedContainerResolver.resolve(combined, 26)
                .orElseThrow();
        SupportedContainerResolver.ResolvedSlot slot27 = SupportedContainerResolver.resolve(combined, 27)
                .orElseThrow();
        SupportedContainerResolver.ResolvedSlot slot53 = SupportedContainerResolver.resolve(combined, 53)
                .orElseThrow();
        helper.assertTrue(slot0.owner() == main
                        && slot0.localSlot() == 0
                        && slot26.owner() == main
                        && slot26.localSlot() == 26
                        && slot27.owner() == partner
                        && slot27.localSlot() == 0
                        && slot53.owner() == partner
                        && slot53.localSlot() == 26
                        && slot0.ownerSlotCount() == 27
                        && slot53.ownerSlotCount() == 27
                        && SupportedContainerResolver.resolve(combined, 54).isEmpty(),
                "Double Barrel slots 0/26/27/53 did not map main-first to two physical owners");
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

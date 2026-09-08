package dev.resivore.quickstacknearbycompat.gametest;

import dev.resivore.quickstacknearbycompat.core.CsrQuickStackIntegration;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;
import tempeststudios.quickstacknearby.QuickStackService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class CsrQuickStackIntegrationGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void realDiscoveryAdmitsReservationOnlyTargetAndRejectsComponentMismatch(GameTestHelper helper) {
        requireCsr(helper);
        BarrelBlockEntity barrel = barrel(helper);
        ServerPlayer player = playerAt(helper, new BlockPos(0, 2, 1));
        ItemStack reserved = namedPotato(1, "reserved");
        ItemStack exact = reserved.copyWithCount(12);
        ItemStack distinct = namedPotato(12, "distinct");
        ItemStack distinctIdentity = distinct.copyWithCount(1);
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 5).orElseThrow(), reserved);

        player.getInventory().setItem(9, exact);
        helper.assertTrue(QuickStackMoveEngine.acceptedTypes(barrel).isEmpty(),
                "The reservation-only fixture unexpectedly had native physical affinity");
        QuickStackMoveEngine.Result result = QuickStackService.quickStack(
                player,
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        helper.assertTrue(result.itemsMoved() == 12
                        && result.sourceStacksTouched() == 1
                        && result.targetContainersTouched() == 1
                        && player.getInventory().getItem(9).isEmpty()
                        && barrel.getItem(0).isEmpty()
                        && ItemStack.isSameItemSameComponents(barrel.getItem(5), reserved)
                        && barrel.getItem(5).getCount() == 12
                        && ContainerSlotReservationsApi.reservationMatches(barrel, 5, reserved),
                "QSN did not move the exact source into its reserved physical slot with exact totals");

        barrel.removeItemNoUpdate(5);
        helper.assertTrue(ContainerSlotReservationsApi.reservationMatches(barrel, 5, reserved),
                "Extraction cleared the independent CSR reservation");

        player.getInventory().setItem(9, distinct);
        QuickStackMoveEngine.Result rejectedResult = QuickStackService.quickStack(
                player,
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        helper.assertTrue(rejectedResult.itemsMoved() == 0
                        && rejectedResult.targetContainersTouched() == 0
                        && player.getInventory().getItem(9).getCount() == 12
                        && barrel.getItem(5).isEmpty(),
                "Real QSN discovery admitted a registry-equal but component-distinct reservation");

        barrel.setItem(0, distinct.copyWithCount(distinct.getMaxStackSize()));
        QuickStackMoveEngine.Result physicalResult = QuickStackService.quickStack(
                player,
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        helper.assertTrue(physicalResult.itemsMoved() == 12
                        && player.getInventory().getItem(9).isEmpty()
                        && barrel.getItem(5).isEmpty()
                        && ItemStack.isSameItemSameComponents(barrel.getItem(1), distinctIdentity)
                        && barrel.getItem(1).getCount() == 12
                        && ContainerSlotReservationsApi.reservationMatches(barrel, 5, reserved),
                "A component-mismatched reservation accepted a normally admitted physical source"
                        + "; moved=" + physicalResult.itemsMoved()
                        + ", source=" + player.getInventory().getItem(9).getCount()
                        + ", slot0=" + barrel.getItem(0).getCount()
                        + ", slot1=" + barrel.getItem(1).getCount()
                        + ", slot5=" + barrel.getItem(5).getCount()
                        + ", reservation=" + ContainerSlotReservationsApi.reservationMatches(barrel, 5, reserved));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nativePhysicalMergePrecedesReservationAndReservationPrecedesOrdinaryEmpty(GameTestHelper helper) {
        requireCsr(helper);
        BarrelBlockEntity barrel = barrel(helper);
        ServerPlayer player = playerAt(helper, new BlockPos(0, 2, 1));
        ItemStack identity = namedPotato(1, "reserved");
        barrel.setItem(0, identity.copyWithCount(60));
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 4).orElseThrow(), identity);
        player.getInventory().setItem(9, identity.copyWithCount(70));

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(
                player,
                QuickStackMoveEngine.SourceRules.EMPTY
        );

        helper.assertTrue(result.itemsMoved() == 70
                        && result.sourceStacksTouched() == 1
                        && result.targetContainersTouched() == 1
                        && player.getInventory().getItem(9).isEmpty()
                        && barrel.getItem(0).getCount() == 64
                        && barrel.getItem(4).getCount() == 64
                        && barrel.getItem(1).getCount() == 2
                        && ContainerSlotReservationsApi.reservationMatches(barrel, 4, identity),
                "QSN did not preserve physical-merge, reserved-empty, ordinary-empty priority or exact totals");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void unrelatedReservationUnreservedAndUnsupportedEmptyTargetsGainNoAffinity(GameTestHelper helper) {
        requireCsr(helper);
        ServerPlayer player = playerAt(helper, new BlockPos(0, 2, 1));
        BarrelBlockEntity unrelatedBarrel = barrel(helper, new BlockPos(1, 2, 1));
        BarrelBlockEntity unreservedBarrel = barrel(helper, new BlockPos(2, 2, 1));
        BlockPos hopperPos = new BlockPos(3, 2, 1);
        helper.setBlock(hopperPos, Blocks.HOPPER);
        HopperBlockEntity unsupportedHopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        ItemStack sourceStack = namedPotato(8, "source");
        ItemStack unrelated = new ItemStack(Items.COBBLESTONE);
        ReservationStore.set(
                SupportedContainerResolver.resolve(unrelatedBarrel, 3).orElseThrow(),
                unrelated
        );
        player.getInventory().setItem(9, sourceStack);

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(
                player,
                QuickStackMoveEngine.SourceRules.EMPTY
        );

        helper.assertTrue(result.itemsMoved() == 0
                        && result.targetContainersTouched() == 0
                        && player.getInventory().getItem(9).getCount() == 8
                        && unrelatedBarrel.isEmpty()
                        && unreservedBarrel.isEmpty()
                        && unsupportedHopper.isEmpty()
                        && ContainerSlotReservationsApi.reservationMatches(
                                unrelatedBarrel, 3, unrelated),
                "Real discovery admitted an unrelated, unreserved, or CSR-unsupported empty target");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void mixedContainerWithZeroPhysicalReservedItemIsDiscovered(GameTestHelper helper) {
        requireCsr(helper);
        ServerPlayer player = playerAt(helper, new BlockPos(0, 2, 1));
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack reserved = namedPotato(1, "reserved");
        barrel.setItem(0, new ItemStack(Items.DIRT, 12));
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 6).orElseThrow(), reserved);
        player.getInventory().setItem(9, reserved.copyWithCount(7));

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(
                player,
                QuickStackMoveEngine.SourceRules.EMPTY
        );

        helper.assertTrue(result.itemsMoved() == 7
                        && result.targetContainersTouched() == 1
                        && player.getInventory().getItem(9).isEmpty()
                        && barrel.getItem(0).is(Items.DIRT)
                        && barrel.getItem(0).getCount() == 12
                        && ItemStack.isSameItemSameComponents(barrel.getItem(6), reserved)
                        && barrel.getItem(6).getCount() == 7,
                "A container with unrelated contents and zero physical reserved items was dropped");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void reservationOnlyTargetKeepsItsNaturalRawDiscoveryOrder(GameTestHelper helper) {
        requireCsr(helper);
        ServerPlayer player = playerAt(helper, new BlockPos(0, 2, 1));
        BarrelBlockEntity nearReservationOnly = barrel(helper, new BlockPos(1, 2, 1));
        BarrelBlockEntity fartherNative = barrel(helper, new BlockPos(2, 2, 1));
        ItemStack identity = namedPotato(1, "ordered");
        ReservationStore.set(
                SupportedContainerResolver.resolve(nearReservationOnly, 4).orElseThrow(),
                identity
        );
        fartherNative.setItem(0, identity.copyWithCount(20));
        player.getInventory().setItem(9, identity.copyWithCount(4));

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(
                player,
                QuickStackMoveEngine.SourceRules.EMPTY
        );

        helper.assertTrue(result.itemsMoved() == 4
                        && result.targetContainersTouched() == 1
                        && nearReservationOnly.getItem(4).getCount() == 4
                        && fartherNative.getItem(0).getCount() == 20,
                "Reservation-only affinity was globally appended instead of retaining QSN distance order");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void reservationDiscoveryHonorsLockedAndFullyKeptSourceRules(GameTestHelper helper) {
        requireCsr(helper);
        ServerPlayer player = playerAt(helper, new BlockPos(0, 2, 1));
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack identity = namedPotato(8, "ruled");
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 4).orElseThrow(), identity);

        player.getInventory().setItem(9, identity.copy());
        QuickStackMoveEngine.Result locked = QuickStackService.quickStack(
                player,
                new QuickStackMoveEngine.SourceRules(Map.of(
                        9, new QuickStackMoveEngine.SlotRule(true, 0)
                ))
        );
        helper.assertTrue(locked.itemsMoved() == 0
                        && player.getInventory().getItem(9).getCount() == 8
                        && barrel.getItem(4).isEmpty(),
                "A locked source created reservation-only discovery affinity");

        QuickStackMoveEngine.Result kept = QuickStackService.quickStack(
                player,
                new QuickStackMoveEngine.SourceRules(Map.of(
                        9, new QuickStackMoveEngine.SlotRule(false, 8)
                ))
        );
        helper.assertTrue(kept.itemsMoved() == 0
                        && player.getInventory().getItem(9).getCount() == 8
                        && barrel.getItem(4).isEmpty(),
                "A fully kept source created reservation-only discovery affinity");

        QuickStackMoveEngine.Result partialKeep = QuickStackService.quickStack(
                player,
                new QuickStackMoveEngine.SourceRules(Map.of(
                        9, new QuickStackMoveEngine.SlotRule(false, 3)
                ))
        );
        helper.assertTrue(partialKeep.itemsMoved() == 5
                        && partialKeep.sourceStacksTouched() == 1
                        && partialKeep.targetContainersTouched() == 1
                        && player.getInventory().getItem(9).getCount() == 3
                        && barrel.getItem(4).getCount() == 5,
                "Reservation-only discovery or insertion changed QSN's keep-count remainder");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void insertionInterceptionActivatesOnlyForReservationGovernedEmpties(GameTestHelper helper) {
        requireCsr(helper);
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack sourceIdentity = namedPotato(1, "source");
        ItemStack unreservedMoving = sourceIdentity.copyWithCount(8);

        OptionalInt unreserved = CsrQuickStackIntegration.insertIntoEmptySlots(unreservedMoving, barrel);
        helper.assertTrue(unreserved.isEmpty()
                        && unreservedMoving.getCount() == 8
                        && barrel.isEmpty(),
                "A supported but wholly unreserved target did not delegate to native QSN insertion");

        ItemStack unsupportedMoving = sourceIdentity.copyWithCount(8);
        SimpleContainer unsupported = new SimpleContainer(3);
        OptionalInt unsupportedResult = CsrQuickStackIntegration.insertIntoEmptySlots(
                unsupportedMoving,
                unsupported
        );
        helper.assertTrue(unsupportedResult.isEmpty()
                        && unsupportedMoving.getCount() == 8
                        && unsupported.isEmpty(),
                "An unsupported target did not remain outside CSR insertion integration");

        barrel.setItem(0, sourceIdentity.copyWithCount(sourceIdentity.getMaxStackSize()));
        for (int slot = 2; slot < barrel.getContainerSize(); slot++) {
            barrel.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        ItemStack mismatchedReservation = namedPotato(1, "other");
        ReservationStore.set(
                SupportedContainerResolver.resolve(barrel, 1).orElseThrow(),
                mismatchedReservation
        );
        QuickStackMoveEngine.Target physicalTarget = QuickStackMoveEngine.Target.fromCurrentContents(barrel);
        ItemStack mismatchedMoving = sourceIdentity.copyWithCount(8);
        OptionalInt mismatchResult = CsrQuickStackIntegration.insertIntoEmptySlots(
                mismatchedMoving,
                barrel
        );

        helper.assertTrue(physicalTarget.accepts(QuickStackMoveEngine.StackKey.of(sourceIdentity))
                        && mismatchResult.isPresent()
                        && mismatchResult.getAsInt() == 0
                        && mismatchedMoving.getCount() == 8
                        && barrel.getItem(1).isEmpty()
                        && ContainerSlotReservationsApi.reservationMatches(
                                barrel,
                                1,
                                mismatchedReservation
                        ),
                "A physically admitted target did not intercept and block its mismatched reserved empty slot");
        helper.succeed();
    }

    private static BarrelBlockEntity barrel(GameTestHelper helper) {
        return barrel(helper, new BlockPos(1, 2, 1));
    }

    private static BarrelBlockEntity barrel(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.BARREL);
        return helper.getBlockEntity(pos, BarrelBlockEntity.class);
    }

    private static ServerPlayer playerAt(GameTestHelper helper, BlockPos relativePos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absolutePos = helper.absolutePos(relativePos);
        player.setPos(
                absolutePos.getX() + 0.5,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5
        );
        return player;
    }

    private static boolean csrAvailable() {
        return FabricLoader.getInstance().isModLoaded("container_slot_reservations");
    }

    private static void requireCsr(GameTestHelper helper) {
        helper.assertTrue(csrAvailable(),
                "Container Slot Reservations C1 or a compatible successor is required for the C8 fixture");
    }

    private static ItemStack namedPotato(int count, String name) {
        ItemStack stack = new ItemStack(Items.POISONOUS_POTATO, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}

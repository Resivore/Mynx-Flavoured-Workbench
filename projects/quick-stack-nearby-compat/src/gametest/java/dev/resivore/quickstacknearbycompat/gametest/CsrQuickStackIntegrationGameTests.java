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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.lang.reflect.Method;
import java.util.List;
import java.util.OptionalInt;

public final class CsrQuickStackIntegrationGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void reservationOnlyTargetGainsExactAffinityAndRetainsReservation(GameTestHelper helper) {
        requireCsr(helper);
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack reserved = namedPotato(1, "reserved");
        ItemStack exact = reserved.copyWithCount(12);
        ItemStack distinct = namedPotato(12, "distinct");
        ItemStack distinctIdentity = distinct.copyWithCount(1);
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 5).orElseThrow(), reserved);

        SimpleContainer exactSource = new SimpleContainer(exact);
        QuickStackMoveEngine.Target raw = QuickStackMoveEngine.Target.fromCurrentContents(barrel);
        List<QuickStackMoveEngine.Target> augmented = CsrQuickStackIntegration.augmentTargets(
                exactSource,
                0,
                1,
                List.of(raw),
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        helper.assertTrue(!raw.accepts(QuickStackMoveEngine.StackKey.of(exact))
                        && augmented.getFirst().accepts(QuickStackMoveEngine.StackKey.of(exact)),
                "An exact empty reservation did not add the source's native QSN StackKey affinity");

        QuickStackMoveEngine.Result result = QuickStackMoveEngine.moveMatchingItems(
                exactSource,
                0,
                1,
                augmented,
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        helper.assertTrue(result.itemsMoved() == 12
                        && result.sourceStacksTouched() == 1
                        && result.targetContainersTouched() == 1
                        && exactSource.isEmpty()
                        && barrel.getItem(0).isEmpty()
                        && ItemStack.isSameItemSameComponents(barrel.getItem(5), reserved)
                        && barrel.getItem(5).getCount() == 12
                        && ContainerSlotReservationsApi.reservationMatches(barrel, 5, reserved),
                "QSN did not move the exact source into its reserved physical slot with exact totals");

        barrel.removeItemNoUpdate(5);
        helper.assertTrue(ContainerSlotReservationsApi.reservationMatches(barrel, 5, reserved),
                "Extraction cleared the independent CSR reservation");

        SimpleContainer distinctSource = new SimpleContainer(distinct);
        List<QuickStackMoveEngine.Target> rejected = CsrQuickStackIntegration.augmentTargets(
                distinctSource,
                0,
                1,
                List.of(QuickStackMoveEngine.Target.fromCurrentContents(barrel)),
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        QuickStackMoveEngine.Result rejectedResult = QuickStackMoveEngine.moveMatchingItems(
                distinctSource,
                0,
                1,
                rejected,
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        helper.assertTrue(!rejected.getFirst().accepts(QuickStackMoveEngine.StackKey.of(distinct))
                        && rejectedResult.itemsMoved() == 0
                        && distinctSource.getItem(0).getCount() == 12
                        && barrel.getItem(5).isEmpty(),
                "A registry-equal but component-distinct source gained reservation affinity or entered the slot");

        barrel.setItem(0, distinct.copyWithCount(distinct.getMaxStackSize()));
        List<QuickStackMoveEngine.Target> physicallyAdmitted = CsrQuickStackIntegration.augmentTargets(
                distinctSource,
                0,
                1,
                List.of(QuickStackMoveEngine.Target.fromCurrentContents(barrel)),
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        QuickStackMoveEngine.Result physicalResult = QuickStackMoveEngine.moveMatchingItems(
                distinctSource,
                0,
                1,
                physicallyAdmitted,
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        helper.assertTrue(physicalResult.itemsMoved() == 12
                        && distinctSource.isEmpty()
                        && barrel.getItem(5).isEmpty()
                        && ItemStack.isSameItemSameComponents(barrel.getItem(1), distinctIdentity)
                        && barrel.getItem(1).getCount() == 12
                        && ContainerSlotReservationsApi.reservationMatches(barrel, 5, reserved),
                "A component-mismatched reservation accepted a normally admitted physical source"
                        + "; moved=" + physicalResult.itemsMoved()
                        + ", source=" + distinctSource.getItem(0).getCount()
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
        ItemStack identity = namedPotato(1, "reserved");
        barrel.setItem(0, identity.copyWithCount(60));
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 4).orElseThrow(), identity);
        SimpleContainer source = new SimpleContainer(identity.copyWithCount(70));

        List<QuickStackMoveEngine.Target> targets = CsrQuickStackIntegration.augmentTargets(
                source,
                0,
                1,
                List.of(QuickStackMoveEngine.Target.fromCurrentContents(barrel)),
                QuickStackMoveEngine.SourceRules.EMPTY
        );
        QuickStackMoveEngine.Result result = QuickStackMoveEngine.moveMatchingItems(
                source,
                0,
                1,
                targets,
                QuickStackMoveEngine.SourceRules.EMPTY
        );

        helper.assertTrue(result.itemsMoved() == 70
                        && result.sourceStacksTouched() == 1
                        && result.targetContainersTouched() == 1
                        && source.isEmpty()
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
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack sourceStack = namedPotato(8, "source");
        ItemStack unrelated = new ItemStack(Items.COBBLESTONE);
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 3).orElseThrow(), unrelated);
        SimpleContainer source = new SimpleContainer(sourceStack);
        SimpleContainer unsupported = new SimpleContainer(9);
        QuickStackMoveEngine.Target barrelTarget = QuickStackMoveEngine.Target.fromCurrentContents(barrel);
        QuickStackMoveEngine.Target unsupportedTarget = QuickStackMoveEngine.Target.fromCurrentContents(unsupported);
        List<QuickStackMoveEngine.Target> original = List.of(barrelTarget, unsupportedTarget);

        List<QuickStackMoveEngine.Target> targets = CsrQuickStackIntegration.augmentTargets(
                source,
                0,
                1,
                original,
                QuickStackMoveEngine.SourceRules.EMPTY
        );

        helper.assertTrue(targets == original
                        && !barrelTarget.accepts(QuickStackMoveEngine.StackKey.of(sourceStack))
                        && !unsupportedTarget.accepts(QuickStackMoveEngine.StackKey.of(sourceStack)),
                "An unrelated, unreserved, or unsupported empty target created CSR affinity");
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
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.BARREL);
        return helper.getBlockEntity(pos, BarrelBlockEntity.class);
    }

    private static boolean csrAvailable() {
        return FabricLoader.getInstance().isModLoaded("container_slot_reservations");
    }

    private static void requireCsr(GameTestHelper helper) {
        helper.assertTrue(csrAvailable(),
                "Container Slot Reservations C1 is required for the C7 behavioral fixture");
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

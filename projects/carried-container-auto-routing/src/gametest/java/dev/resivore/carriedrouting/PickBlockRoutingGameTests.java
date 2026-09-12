package dev.resivore.carriedrouting;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/** Controlled Minecraft-level coverage for the complete Pick Block transaction. */
public class PickBlockRoutingGameTests {
    private static ServerPlayer player(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(gameType);
        player.getInventory().setSelectedSlot(0);
        for (int slot = 1; slot < Inventory.SELECTION_SIZE; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.GRANITE, 64));
        }
        return player;
    }

    private static ItemStack shulker(int slot, ItemStack stack) {
        ItemStack carrier = new ItemStack(Blocks.SHULKER_BOX);
        set(carrier, slot, stack);
        return carrier;
    }

    private static ItemStack bundle(ItemStack stack) {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        BundleContents.Mutable contents = new BundleContents.Mutable(BundleContents.EMPTY);
        contents.tryInsert(stack);
        bundle.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
        return bundle;
    }

    private static NonNullList<ItemStack> contents(ItemStack carrier) {
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        carrier.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        return contents;
    }

    private static ItemStack get(ItemStack carrier, int slot) {
        return contents(carrier).get(slot);
    }

    private static void set(ItemStack carrier, int slot, ItemStack stack) {
        NonNullList<ItemStack> contents = contents(carrier);
        contents.set(slot, stack);
        carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
    }

    private static void fillStorage(ServerPlayer player, int... exclusions) {
        outer: for (int slot = Inventory.SELECTION_SIZE;
                    slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
            for (int exclusion : exclusions) if (slot == exclusion) continue outer;
            player.getInventory().setItem(slot, new ItemStack(Items.GRANITE, 64));
        }
    }

    private static ReservationAdmission reservations(Map<Integer, ItemStack> reservations) {
        Map<Integer, ItemStack> copies = new HashMap<>();
        reservations.forEach((slot, stack) -> copies.put(slot, stack.copyWithCount(1)));
        return new ReservationAdmission() {
            @Override public Slot classify(ItemStack carrier, int slot, ItemStack incoming, ItemStack physical) {
                ItemStack reservation = copies.get(slot);
                if (reservation != null && !ItemStack.isSameItemSameComponents(reservation, incoming)) {
                    return Slot.RESERVED_OTHER;
                }
                Slot nativeResult = ReservationAdmission.ABSENT.classify(carrier, slot, incoming, physical);
                return nativeResult == Slot.UNRESERVED_EMPTY && reservation != null
                        ? Slot.RESERVED_MATCH : nativeResult;
            }

            @Override public boolean permitsAffinity(ItemStack carrier, int slot, ItemStack incoming) {
                ItemStack reservation = copies.get(slot);
                return reservation == null || ItemStack.isSameItemSameComponents(reservation, incoming);
            }
        };
    }

    private static int totalItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) total += stack.getCount();
            if (stack.getItem() == Blocks.SHULKER_BOX.asItem()) {
                total += contents(stack).stream().filter(inner -> inner.is(item)).mapToInt(ItemStack::getCount).sum();
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(item)) total += offhand.getCount();
        if (offhand.getItem() == Blocks.SHULKER_BOX.asItem()) {
            total += contents(offhand).stream().filter(inner -> inner.is(item)).mapToInt(ItemStack::getCount).sum();
        }
        return total;
    }

    private static Object reservationData(ItemStack carrier) {
        try {
            return Class.forName("dev.resivore.slotreservations.ReservationStore")
                    .getMethod("getData", ItemStack.class).invoke(null, carrier);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void reserve(ItemStack carrier, int slot, ItemStack stack) {
        try {
            Class<?> type = Class.forName("dev.resivore.slotreservations.ReservationData");
            Object data = type.getMethod("with", int.class, ItemStack.class)
                    .invoke(type.getField("EMPTY").get(null), slot, stack.copyWithCount(1));
            Class.forName("dev.resivore.slotreservations.ReservationStore")
                    .getMethod("setData", ItemStack.class, type).invoke(null, carrier, data);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    @GameTest public void vanillaOrdinaryInventoryMatchTakesPrecedence(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(0, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(10, new ItemStack(Items.STONE, 7));
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        ItemStack before = carrier.copy();
        helper.assertTrue(!PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE)),
                "CCAR ran despite vanilla's ordinary-inventory match");
        helper.assertTrue(ItemStack.matches(before, carrier)
                        && player.getInventory().getItem(0).is(Items.DIRT),
                "Vanilla-precedence guard mutated carried or selected state");
        helper.succeed();
    }

    @GameTest public void unlockedShulkerFallbackMovesTheWholeVanillaSourceStack(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(4, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE)),
                "Unlocked carried shulker was not used after vanilla miss");
        helper.assertTrue(player.getInventory().getSelectedSlot() == 0
                        && player.getInventory().getItem(0).is(Items.STONE)
                        && player.getInventory().getItem(0).getCount() == 64,
                "Pick Block did not preserve vanilla whole-source-stack movement");
        helper.assertTrue(get(player.getInventory().getItem(9), 4).isEmpty(),
                "Physical source slot was not emptied");
        helper.succeed();
    }

    @GameTest public void vanillaSuitableEmptyHotbarSlotAvoidsDisplacement(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(4, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        player.getInventory().setItem(1, ItemStack.EMPTY);
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && player.getInventory().getSelectedSlot() == 1
                        && player.getInventory().getItem(1).is(Items.STONE)
                        && player.getInventory().getItem(0).is(Items.DIRT),
                "CCAR did not preserve Inventory.pickSlot's suitable-hotbar selection semantics");
        helper.succeed();
    }

    @GameTest public void lockedShulkerIsIgnored(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(4, new ItemStack(Items.STONE, 64));
        RoutingLock.setLocked(carrier, true);
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        ItemStack before = carrier.copy();
        helper.assertTrue(!PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && ItemStack.matches(before, carrier),
                "Locked carrier was searched or mutated");
        helper.succeed();
    }

    @GameTest public void bundlesAndNestedContainerContentsAreNotSearched(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack directBundle = bundle(new ItemStack(Items.STONE, 8));
        ItemStack nestedBundleCarrier = shulker(0, bundle(new ItemStack(Items.STONE, 8)));
        player.getInventory().setItem(9, directBundle);
        player.getInventory().setItem(10, nestedBundleCarrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        helper.assertTrue(!PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && player.getInventory().getItem(0).is(Items.DIRT)
                        && ItemStack.matches(directBundle, player.getInventory().getItem(9))
                        && ItemStack.matches(nestedBundleCarrier, player.getInventory().getItem(10)),
                "Pick Block searched bundles or recursed into nested container contents");
        helper.succeed();
    }

    @GameTest public void carrierAndPhysicalSourceOrderingAreDeterministic(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack first = shulker(8, new ItemStack(Items.STONE, 8));
        set(first, 2, new ItemStack(Items.STONE, 2));
        ItemStack second = shulker(0, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, first);
        player.getInventory().setItem(10, second);
        player.getInventory().setItem(0, ItemStack.EMPTY);
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE)),
                "Ordered source was not found");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 2
                        && get(player.getInventory().getItem(9), 2).isEmpty()
                        && get(player.getInventory().getItem(9), 8).getCount() == 8
                        && get(player.getInventory().getItem(10), 0).getCount() == 64,
                "Carrier or physical-slot order drifted");
        helper.succeed();
    }

    @GameTest public void displacedStackFullyMergesIntoOrdinaryPartial(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(10, new ItemStack(Items.DIRT, 32));
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && player.getInventory().getItem(10).getCount() == 64
                        && get(player.getInventory().getItem(9), 5).isEmpty(),
                "Ordinary compatible partial did not receive the full displacement");
        helper.succeed();
    }

    @GameTest public void displacedStackFullyMergesIntoUnlockedShulkerPartial(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        set(carrier, 3, new ItemStack(Items.DIRT, 32));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && get(player.getInventory().getItem(9), 3).getCount() == 64
                        && get(player.getInventory().getItem(9), 5).isEmpty(),
                "Unlocked shulker compatible partial did not receive the displacement");
        helper.succeed();
    }

    @GameTest public void displacedStackSpansMultipleCompatiblePartials(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        set(carrier, 1, new ItemStack(Items.DIRT, 60));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(10, new ItemStack(Items.DIRT, 58));
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 10));
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && get(player.getInventory().getItem(9), 1).getCount() == 64
                        && player.getInventory().getItem(10).getCount() == 64,
                "Displacement did not continue through multiple partial destinations");
        helper.succeed();
    }

    @GameTest public void partialMergeThenEmptyOrdinarySlotCompletesRemainder(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        set(carrier, 1, new ItemStack(Items.DIRT, 60));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 12));
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && get(player.getInventory().getItem(9), 1).getCount() == 64
                        && player.getInventory().getItem(10).is(Items.DIRT)
                        && player.getInventory().getItem(10).getCount() == 8,
                "Partial merge plus first empty ordinary slot did not complete the transaction");
        helper.succeed();
    }

    @GameTest public void noMatchUsesFirstEmptyOrdinarySlot(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && player.getInventory().getItem(10).is(Items.DIRT)
                        && player.getInventory().getItem(10).getCount() == 32,
                "First vanilla-ordered empty ordinary slot was not used");
        helper.succeed();
    }

    @GameTest public void noMatchAndNoEmptyUsesExactSourceSlotSwap(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        fillStorage(player, 9);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && player.getInventory().getItem(0).is(Items.STONE)
                        && get(player.getInventory().getItem(9), 5).is(Items.DIRT)
                        && get(player.getInventory().getItem(9), 5).getCount() == 32,
                "Exact physical source slot was not used as the final swap destination");
        helper.succeed();
    }

    @GameTest public void matchingReservationPermitsExactSourceFallback(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        fillStorage(player, 9);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        ReservationAdmission admission = reservations(Map.of(5, new ItemStack(Items.DIRT)));
        PickBlockRouting.Transaction plan = PickBlockRouting.plan(
                player, new ItemStack(Items.STONE), admission).orElseThrow();
        helper.assertTrue(plan.sourceFallbackCount() == 32 && plan.commit()
                        && get(player.getInventory().getItem(9), 5).is(Items.DIRT),
                "Matching reservation did not admit the legal exact-source fallback");
        helper.succeed();
    }

    @GameTest public void matchingReservationPermitsCompatiblePartialDestination(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        set(carrier, 1, new ItemStack(Items.DIRT, 32));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        PickBlockRouting.Transaction plan = PickBlockRouting.plan(player, new ItemStack(Items.STONE),
                reservations(Map.of(1, new ItemStack(Items.DIRT)))).orElseThrow();
        helper.assertTrue(plan.merges().stream().mapToInt(PickBlockRouting.Merge::count).sum() == 32
                        && plan.commit()
                        && get(player.getInventory().getItem(9), 1).getCount() == 64,
                "Matching CSR reservation did not admit its compatible partial destination");
        helper.succeed();
    }

    @GameTest public void mismatchingReservationRejectsCompatiblePhysicalPartial(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        set(carrier, 1, new ItemStack(Items.DIRT, 32));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        PickBlockRouting.Transaction plan = PickBlockRouting.plan(player, new ItemStack(Items.STONE),
                reservations(Map.of(1, new ItemStack(Items.STONE)))).orElseThrow();
        helper.assertTrue(plan.merges().isEmpty()
                        && plan.emptyOrdinarySlot().orElse(-1) == 10
                        && plan.commit()
                        && get(player.getInventory().getItem(9), 1).getCount() == 32
                        && player.getInventory().getItem(10).getCount() == 32,
                "Conflicting CSR reservation admitted a physically compatible partial");
        helper.succeed();
    }

    @GameTest public void mismatchingReservedSourceFailsAtomically(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        fillStorage(player, 9);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        ItemStack beforeCarrier = carrier.copy();
        ItemStack beforeSelected = player.getInventory().getItem(0).copy();
        ReservationAdmission admission = reservations(Map.of(5, new ItemStack(Items.STONE)));
        helper.assertTrue(PickBlockRouting.plan(player, new ItemStack(Items.STONE), admission).isEmpty()
                        && ItemStack.matches(beforeCarrier, carrier)
                        && ItemStack.matches(beforeSelected, player.getInventory().getItem(0)),
                "Mismatching reserved source did not fail without mutation");
        helper.succeed();
    }

    @GameTest public void realCsrSourceReservationSurvivesExtractionUnchanged(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("container_slot_reservations")) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        reserve(carrier, 5, new ItemStack(Items.STONE));
        Object reservationBefore = reservationData(carrier);
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE)),
                "Reserved source was incorrectly made non-extractable");
        ItemStack liveCarrier = player.getInventory().getItem(9);
        helper.assertTrue(reservationBefore.equals(reservationData(liveCarrier))
                        && get(liveCarrier, 5).isEmpty(),
                "CSR reservation or portable identity data changed during extraction");
        helper.succeed();
    }

    @GameTest public void fullMatchingStacksDoNotCountAsMergeCapacity(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        set(carrier, 1, new ItemStack(Items.DIRT, 64));
        player.getInventory().setItem(9, carrier);
        fillStorage(player, 9);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        PickBlockRouting.Transaction plan = PickBlockRouting.plan(
                player, new ItemStack(Items.STONE), ReservationAdmission.ABSENT).orElseThrow();
        helper.assertTrue(plan.merges().isEmpty() && plan.sourceFallbackCount() == 32 && plan.commit(),
                "A full compatible stack falsely claimed merge capacity");
        helper.succeed();
    }

    @GameTest public void effectiveNon64MaximumIsRespected(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        ItemStack selected = new ItemStack(Items.DIRT, 8);
        selected.set(DataComponents.MAX_STACK_SIZE, 16);
        ItemStack shulkerPartial = selected.copyWithCount(12);
        ItemStack ordinaryPartial = selected.copyWithCount(14);
        set(carrier, 1, shulkerPartial);
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(10, ordinaryPartial);
        player.getInventory().setItem(0, selected);
        PickBlockRouting.Transaction plan = PickBlockRouting.plan(
                player, new ItemStack(Items.STONE), ReservationAdmission.ABSENT).orElseThrow();
        helper.assertTrue(plan.merges().stream().mapToInt(PickBlockRouting.Merge::count).sum() == 6
                        && plan.emptyOrdinarySlot().orElse(-1) == 11
                        && plan.commit()
                        && get(player.getInventory().getItem(9), 1).getCount() == 16
                        && player.getInventory().getItem(10).getCount() == 16
                        && player.getInventory().getItem(11).getCount() == 2,
                "Effective component maximum was treated as 64");
        helper.succeed();
    }

    @GameTest public void dynamicInventoryExtendedOrdinaryRangeIsUsed(GameTestHelper helper) {
        java.util.List<ItemStack> expanded = new java.util.ArrayList<>();
        for (int slot = 0; slot < 63; slot++) expanded.add(new ItemStack(Items.GRANITE, 64));
        expanded.set(62, ItemStack.EMPTY);
        helper.assertTrue(PickBlockRouting.firstEmptyOrdinarySlot(expanded, 0) == 62
                        && CarriedContainerOrder.hosts(expanded.size(), -1, false).get(62).inventorySlot() == 62,
                "Production ordering did not include the live Inventory Extended ordinary-storage tail");
        helper.succeed();
    }

    @GameTest public void staleSourceStateAbortsBeforeAnyTransactionWrite(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        PickBlockRouting.Transaction plan = PickBlockRouting.plan(
                player, new ItemStack(Items.STONE), ReservationAdmission.ABSENT).orElseThrow();
        set(carrier, 5, new ItemStack(Items.STONE, 63));
        helper.assertTrue(!plan.commit()
                        && player.getInventory().getItem(0).is(Items.DIRT)
                        && get(carrier, 5).getCount() == 63,
                "Stale physical source committed a partially outdated transaction");
        helper.succeed();
    }

    @GameTest public void staleSelectedHotbarStateAbortsBeforeAnyTransactionWrite(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        PickBlockRouting.Transaction plan = PickBlockRouting.plan(
                player, new ItemStack(Items.STONE), ReservationAdmission.ABSENT).orElseThrow();
        player.getInventory().getItem(0).shrink(1);
        helper.assertTrue(!plan.commit()
                        && player.getInventory().getItem(0).getCount() == 31
                        && get(carrier, 5).getCount() == 64,
                "Stale selected-hotbar state allowed source extraction");
        helper.succeed();
    }

    @GameTest public void exactConservationHoldsAcrossSplitRehoming(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        set(carrier, 1, new ItemStack(Items.DIRT, 60));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(10, new ItemStack(Items.DIRT, 62));
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 12));
        int stoneBefore = totalItem(player, Items.STONE);
        int dirtBefore = totalItem(player, Items.DIRT);
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && totalItem(player, Items.STONE) == stoneBefore
                        && totalItem(player, Items.DIRT) == dirtBefore,
                "Pick Block transaction did not conserve exact item counts");
        helper.succeed();
    }

    @GameTest public void componentIdentitySurvivesEveryMove(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack picked = new ItemStack(Items.STONE, 16);
        picked.set(DataComponents.CUSTOM_NAME, Component.literal("Picked identity"));
        ItemStack displaced = new ItemStack(Items.DIRT, 12);
        displaced.set(DataComponents.CUSTOM_NAME, Component.literal("Displaced identity"));
        ItemStack partial = displaced.copyWithCount(60);
        ItemStack carrier = shulker(5, picked);
        set(carrier, 1, partial);
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, displaced);
        ItemStack request = picked.copyWithCount(1);
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, request)
                        && Component.literal("Picked identity").equals(
                        player.getInventory().getItem(0).get(DataComponents.CUSTOM_NAME))
                        && Component.literal("Displaced identity").equals(
                        get(player.getInventory().getItem(9), 1).get(DataComponents.CUSTOM_NAME))
                        && Component.literal("Displaced identity").equals(
                        player.getInventory().getItem(10).get(DataComponents.CUSTOM_NAME)),
                "A picked or displaced stack lost its component identity");
        helper.succeed();
    }

    @GameTest public void lockComponentIsNeverRewritten(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        ItemStack source = shulker(5, new ItemStack(Items.STONE, 64));
        ItemStack destination = shulker(1, new ItemStack(Items.DIRT, 32));
        RoutingLock.setLocked(source, false);
        RoutingLock.setLocked(destination, false);
        player.getInventory().setItem(9, source);
        player.getInventory().setItem(10, destination);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        helper.assertTrue(PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && !RoutingLock.isLocked(source)
                        && !RoutingLock.isLocked(destination),
                "Pick Block rewrote a carrier routing-lock component");
        helper.succeed();
    }

    @GameTest public void creativeBehaviorRemainsVanillaOwned(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.CREATIVE);
        ItemStack carrier = shulker(5, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
        ItemStack before = carrier.copy();
        helper.assertTrue(!PickBlockRouting.tryPickFromShulkers(player, new ItemStack(Items.STONE))
                        && ItemStack.matches(before, carrier)
                        && player.getInventory().getItem(0).is(Items.DIRT),
                "CCAR changed Creative Pick Block ownership or state");
        helper.succeed();
    }
}

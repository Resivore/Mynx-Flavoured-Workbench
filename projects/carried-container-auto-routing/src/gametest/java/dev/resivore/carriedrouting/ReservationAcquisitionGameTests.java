package dev.resivore.carriedrouting;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Real pickup and menu entrypoints, with actual Fabric mod registration when CSR is supplied. */
public class ReservationAcquisitionGameTests {
    private static boolean csr() { return FabricLoader.getInstance().isModLoaded("container_slot_reservations"); }
    private static ItemStack carrier() {
        ItemStack carrier = new ItemStack(Blocks.SHULKER_BOX);
        if (csr()) {
            try {
                Class<?> type = Class.forName("dev.resivore.slotreservations.ReservationData");
                Object data = type.getMethod("with", int.class, ItemStack.class)
                        .invoke(type.getField("EMPTY").get(null), 26, new ItemStack(Items.STONE));
                Class.forName("dev.resivore.slotreservations.ReservationStore")
                        .getMethod("setData", ItemStack.class, type).invoke(null, carrier, data);
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        } else {
            var contents = NonNullList.withSize(27, ItemStack.EMPTY);
            contents.set(26, new ItemStack(Items.STONE));
            carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        }
        return carrier;
    }
    private static int count(ItemStack carrier, int slot) {
        var contents = NonNullList.withSize(27, ItemStack.EMPTY);
        carrier.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        return contents.get(slot).getCount();
    }
    private static ItemStack bundleCarrier() {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        BundleContents.Mutable contents = new BundleContents.Mutable(BundleContents.EMPTY);
        contents.tryInsert(new ItemStack(Items.STONE));
        bundle.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
        return bundle;
    }
    private static ItemStack partialCarrier() {
        ItemStack carrier = new ItemStack(Blocks.SHULKER_BOX);
        var contents = NonNullList.withSize(27, ItemStack.EMPTY);
        for (int i = 0; i < contents.size(); i++) contents.set(i, new ItemStack(Items.DIRT, 64));
        contents.set(0, new ItemStack(Items.STONE, 63));
        carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return carrier;
    }
    private static void check(GameTestHelper helper, ItemStack carrier, int acquired) {
        helper.assertTrue(count(carrier, 26) == acquired + (csr() ? 0 : 1),
                "Acquisition did not reach exact affinity slot/count");
        helper.assertTrue(count(carrier, 0) == 0, "Earlier empty slot stole insertion");
    }
    @GameTest public void realWorldPickup(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack carrier = carrier(); player.getInventory().setItem(9, carrier);
        ItemEntity entity = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(),
                new ItemStack(Items.STONE, 12));
        entity.setNoPickUpDelay();
        entity.playerTouch(player);
        check(helper, carrier, 12);
        helper.assertTrue(entity.getItem().isEmpty() || entity.isRemoved(), "Pickup retained a duplicate remainder");
        helper.succeed();
    }
    @GameTest public void realExternalQuickMoveSurvivalAndCreative(GameTestHelper helper) {
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(mode);
            ItemStack carrier = carrier(); player.getInventory().setItem(9, carrier);
            SimpleContainer chest = new SimpleContainer(27);
            chest.setItem(0, new ItemStack(Items.STONE, 12));
            ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);
            menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
            check(helper, carrier, 12);
            helper.assertTrue(chest.getItem(0).isEmpty(), "External source was not consumed exactly");
        }
        helper.succeed();
    }
    @GameTest public void realPlayerOriginQuickMove(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack carrier = carrier(); player.getInventory().setItem(10, carrier);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 12));
        SimpleContainer chest = new SimpleContainer(27);
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);
        menu.clicked(27, 0, ContainerInput.QUICK_MOVE, player);
        check(helper, carrier, 12);
        helper.assertTrue(player.getInventory().getItem(9).isEmpty() && chest.isEmpty(),
                "Player-origin source/remainder ownership drifted");
        helper.succeed();
    }
    @GameTest public void offhandAndExcludedSourceRemainIndependent(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack excluded = carrier(), offhand = carrier();
        player.getInventory().setItem(9, excluded);
        player.setItemSlot(EquipmentSlot.OFFHAND, offhand);
        ItemStack incoming = new ItemStack(Items.STONE, 12);
        RoutingService.routePlayerOriginSpecialDestinations(player, incoming, 9, false);
        check(helper, offhand, 12); check(helper, excluded, 0);
        incoming = new ItemStack(Items.STONE, 4);
        RoutingService.routePlayerOriginSpecialDestinations(player, incoming, 9, true);
        helper.assertTrue(incoming.getCount() == 4, "Offhand exclusion was ignored");
        helper.succeed();
    }
    @GameTest public void selectedHandAndStableCarrierOrder(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 60));
        ItemStack first = carrier(), second = carrier();
        player.getInventory().setItem(9, first); player.getInventory().setItem(10, second);
        ItemStack incoming = new ItemStack(Items.STONE, 12);
        RoutingService.routeIncomingStack(player, incoming, RoutingContext.WORLD_PICKUP);
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 64, "Selected hand lost priority");
        check(helper, first, 8); check(helper, second, 0);
        helper.assertTrue(incoming.isEmpty(), "Unexpected remainder");
        helper.succeed();
    }
    @GameTest public void lockedCarrierCannotClaimAndUnlockRestoresAffinity(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack carrier = carrier(); player.getInventory().setItem(9, carrier);
        RoutingLock.setLocked(carrier, true);
        ItemStack incoming = new ItemStack(Items.STONE, 12);
        RoutingService.routePlayerOriginSpecialDestinations(player, incoming, -1, false);
        helper.assertTrue(incoming.getCount() == 12, "Locked carrier claimed items");
        RoutingLock.setLocked(carrier, false);
        RoutingService.routePlayerOriginSpecialDestinations(player, incoming, -1, false);
        check(helper, carrier, 12);
        helper.succeed();
    }
    @GameTest public void worldPickupAudioAccountingOnlyMarksActualCarriedInsertion(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack first = carrier(), second = carrier();
        player.getInventory().setItem(9, first);
        player.getInventory().setItem(10, second);
        RoutingService.RoutingResult split = RoutingService.routeIncomingStackWithResult(
                player, new ItemStack(Items.STONE, 64), RoutingContext.WORLD_PICKUP, -1, false
        );
        helper.assertTrue(split.routedToCarriedContainer() && split.carriedContainerItemsMoved() == 64,
                "Multiple carriers must produce one positive carried-routing audio decision");

        ServerPlayer bundlePlayer = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        bundlePlayer.getInventory().setItem(9, bundleCarrier());
        RoutingService.RoutingResult bundle = RoutingService.routeIncomingStackWithResult(
                bundlePlayer, new ItemStack(Items.STONE, 4), RoutingContext.WORLD_PICKUP, -1, false
        );
        helper.assertTrue(bundle.routedToCarriedContainer() && bundle.carriedContainerItemsMoved() == 4,
                "A qualifying bundle insertion must request the routed-pickup cue");

        ServerPlayer lockedPlayer = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack locked = carrier(); RoutingLock.setLocked(locked, true);
        lockedPlayer.getInventory().setItem(9, locked);
        RoutingService.RoutingResult lockedResult = RoutingService.routeIncomingStackWithResult(
                lockedPlayer, new ItemStack(Items.STONE, 4), RoutingContext.WORLD_PICKUP, -1, false
        );
        helper.assertTrue(!lockedResult.routedToCarriedContainer(),
                "A locked carrier must retain normal vanilla pickup audio");

        ServerPlayer partialPlayer = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        partialPlayer.getInventory().setItem(9, partialCarrier());
        RoutingService.RoutingResult partial = RoutingService.routeIncomingStackWithResult(
                partialPlayer, new ItemStack(Items.STONE, 64), RoutingContext.WORLD_PICKUP, -1, false
        );
        helper.assertTrue(partial.routedToCarriedContainer()
                        && partial.carriedContainerItemsMoved() == 1
                        && partial.totalItemsMoved() == 64,
                "Partial carried routing plus ordinary-inventory remainder must retain one positive cue decision");
        helper.succeed();
    }
}

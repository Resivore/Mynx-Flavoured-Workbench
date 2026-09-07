package tempeststudios.quickstacknearby.gametest;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;
import tempeststudios.quickstacknearby.QuickStackService;

import java.lang.reflect.Method;

/** Exact C4 CSR provider checks for C10's public read-only integration boundary. */
public final class C10CsrGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void reservationOnlyTargetIsDiscoveredWithExactComponents(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper); ServerPlayer player = player(helper);
        ItemStack reservation = named(1, "exact");
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 5).orElseThrow(), reservation);
        player.getInventory().setItem(9, reservation.copyWithCount(12));
        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        helper.assertTrue(result.itemsMoved() == 12 && barrel.getItem(5).getCount() == 12
                && ItemStack.isSameItemSameComponents(barrel.getItem(5), reservation),
                "CSR-only destination was not admitted by direct C10 discovery");
        barrel.removeItemNoUpdate(5);
        player.getInventory().setItem(9, named(12, "different"));
        helper.assertTrue(QuickStackService.quickStack(player).itemsMoved() == 0 && barrel.getItem(5).isEmpty(),
                "component-distinct source was admitted by an exact CSR reservation");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void physicalThenReservedThenOrdinaryOrderingIsPreserved(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper); ServerPlayer player = player(helper);
        ItemStack identity = named(1, "ordered");
        barrel.setItem(0, identity.copyWithCount(60));
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 4).orElseThrow(), identity);
        player.getInventory().setItem(9, identity.copyWithCount(70));
        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        helper.assertTrue(result.itemsMoved() == 70 && barrel.getItem(0).getCount() == 64
                && barrel.getItem(4).getCount() == 64 && barrel.getItem(1).getCount() == 2,
                "C10 did not keep physical, matching-reserved, ordinary-empty insertion order");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void childOnlyNestedReservationAdmitsWithoutGivingTheParentAffinity(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper); ServerPlayer player = player(helper);
        ItemStack exact = named(1, "nested exact");
        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        ReservationStore.setData(host, ReservationData.EMPTY.with(7, exact));
        barrel.setItem(0, host);
        player.getInventory().setItem(9, exact.copyWithCount(12));

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        net.minecraft.core.NonNullList<ItemStack> after = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        host.getOrDefault(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY).copyInto(after);
        helper.assertTrue(result.itemsMoved() == 12 && barrel.getItem(0) == host && barrel.getItem(1).isEmpty()
                        && after.get(7).getCount() == 12 && ItemStack.isSameItemSameComponents(after.get(7), exact),
                "a nested CSR-only affinity did not route through its child or leaked to the physical parent");

        player.getInventory().setItem(9, named(4, "different"));
        helper.assertTrue(QuickStackService.quickStack(player).itemsMoved() == 0 && after.get(7).getCount() == 12,
                "a mismatched nested CSR reservation was not protected");
        helper.succeed();
    }


    private static BarrelBlockEntity barrel(GameTestHelper helper) { helper.setBlock(new BlockPos(1, 2, 1), Blocks.BARREL); return helper.getBlockEntity(new BlockPos(1, 2, 1), BarrelBlockEntity.class); }
    private static ServerPlayer player(GameTestHelper helper) { ServerPlayer player = helper.makeMockServerPlayerInLevel(); BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 1)); player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5); return player; }
    private static ItemStack named(int count, String name) { ItemStack result = new ItemStack(Items.POISONOUS_POTATO, count); result.set(DataComponents.CUSTOM_NAME, Component.literal(name)); return result; }
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException { method.invoke(this, helper); }
}

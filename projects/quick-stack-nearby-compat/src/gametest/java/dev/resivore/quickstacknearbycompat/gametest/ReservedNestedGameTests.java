package dev.resivore.quickstacknearbycompat.gametest;
import dev.resivore.slotreservations.*;
import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import tempeststudios.quickstacknearby.QuickStackService;
import java.lang.reflect.Method;

public class ReservedNestedGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks=40) public void reservationOnlyNestedAffinityDoesNotAdmitOuterEmpty(GameTestHelper h) {
        var chest=PhysicalNestedGameTests.chest(h,1);var p=PhysicalNestedGameTests.player(h);var host=PhysicalNestedGameTests.host(0,false);
        var data=ReservationData.EMPTY.with(8,new ItemStack(Items.STONE));ReservationStore.setData(host,data);chest.setItem(3,host);
        p.getInventory().setItem(9,new ItemStack(Items.STONE,12));var result=QuickStackService.quickStack(p);
        h.assertTrue(result.itemsMoved()==12&&result.targetContainersTouched()==1&&chest.getItem(0).isEmpty(),"nested-only key leaked to outer");
        var list=PhysicalNestedGameTests.contents(host);h.assertTrue(list.get(0).isEmpty()&&list.get(8).getCount()==12,"matching reservation lost to early empty");
        h.assertTrue(ReservationStore.getData(host).equals(data)&&host.getHoverName().getString().equals("Exact host"),"unrelated host components changed");h.succeed();
    }
    @GameTest(maxTicks=40) public void mergeThenReservationsInPhysicalOrderThenUnreservedFallback(GameTestHelper h) {
        var chest=PhysicalNestedGameTests.chest(h,1);var p=PhysicalNestedGameTests.player(h);var host=PhysicalNestedGameTests.host(60,false);
        ReservationStore.setData(host,ReservationData.EMPTY.with(8,new ItemStack(Items.STONE)).with(17,new ItemStack(Items.STONE)));
        chest.setItem(0,host);p.getInventory().setItem(9,new ItemStack(Items.STONE,140));var result=QuickStackService.quickStack(p);var list=PhysicalNestedGameTests.contents(host);
        h.assertTrue(result.itemsMoved()==140&&p.getInventory().getItem(9).isEmpty(),"nested counts wrong");
        h.assertTrue(list.get(0).getCount()==64&&list.get(8).getCount()==64&&list.get(17).getCount()==64&&list.get(1).getCount()==8,"nested three-pass order wrong");
        h.assertTrue(ContainerSlotReservationsApi.reservationMatches(host,8,new ItemStack(Items.STONE)),"reservation consumed");h.succeed();
    }
    @GameTest(maxTicks=40) public void mismatchedReservationsProtectOccupiedAndEmptySlots(GameTestHelper h) {
        var chest=PhysicalNestedGameTests.chest(h,1);var p=PhysicalNestedGameTests.player(h);var host=PhysicalNestedGameTests.host(10,false);
        ReservationStore.setData(host,ReservationData.EMPTY.with(0,new ItemStack(Items.DIRT)).with(1,new ItemStack(Items.DIRT)).with(8,new ItemStack(Items.STONE)));
        chest.setItem(0,host);p.getInventory().setItem(9,new ItemStack(Items.STONE,8));var result=QuickStackService.quickStack(p);var list=PhysicalNestedGameTests.contents(host);
        h.assertTrue(result.itemsMoved()==8&&list.get(0).getCount()==10&&list.get(1).isEmpty()&&list.get(8).getCount()==8,"conflicting occupied or empty reservation grew");h.succeed();
    }
    @GameTest(maxTicks=40) public void unrelatedAndComponentDistinctReservationsDoNotEstablishAffinity(GameTestHelper h) {
        var chest=PhysicalNestedGameTests.chest(h,1);var p=PhysicalNestedGameTests.player(h);var host=PhysicalNestedGameTests.host(0,false);
        var distinct=new ItemStack(Items.STONE);distinct.set(DataComponents.CUSTOM_NAME,Component.literal("distinct"));
        ReservationStore.setData(host,ReservationData.EMPTY.with(8,distinct).with(17,new ItemStack(Items.DIRT)));chest.setItem(0,host);
        p.getInventory().setItem(9,new ItemStack(Items.STONE,8));var result=QuickStackService.quickStack(p);
        h.assertTrue(result.itemsMoved()==0&&p.getInventory().getItem(9).getCount()==8&&PhysicalNestedGameTests.contents(host).stream().allMatch(ItemStack::isEmpty),"false reservation affinity");h.succeed();
    }
    @Override public void invokeTestMethod(GameTestHelper h,Method m)throws ReflectiveOperationException{m.invoke(this,h);}
}

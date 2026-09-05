package dev.resivore.slotreservations.gametest;

import dev.resivore.slotreservations.*;
import dev.resivore.slotreservations.network.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.lang.reflect.Method;
import java.util.List;

public class NestedReservationGameTests implements CustomTestMethodInvoker {
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        var pos = h.absolutePos(new BlockPos(1,2,1)); p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5); return p;
    }
    private static NestedReservationActionPayload action(ServerPlayer p,int host,int nested,ReservationActionPayload.Source source) {
        return new NestedReservationActionPayload(p.containerMenu.containerId,p.containerMenu.getStateId(),host,nested,source,
                ShulkerHostFingerprint.of(p.containerMenu.getSlot(host).getItem(),p.registryAccess()));
    }
    @GameTest(maxTicks=40) public void chestHostExactSlotTransitionsAndComponents(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,Blocks.CHEST);var chest=h.getBlockEntity(pos,ChestBlockEntity.class);
        var p=player(h);p.containerMenu=ChestMenu.threeRows(7,p.getInventory(),chest);
        var host=new ItemStack(Blocks.SHULKER_BOX);host.set(DataComponents.CUSTOM_NAME,Component.literal("Host"));
        var contents=ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE,12)));host.set(DataComponents.CONTAINER,contents);
        chest.setItem(0,host);chest.setItem(1,host.copy());var duplicate=chest.getItem(1).copy();
        for(int i:new int[]{0,8,9,17,18,26}) {
            if(i==0) h.assertTrue(NestedReservationActions.handle(p,action(p,0,i,ReservationActionPayload.Source.SLOT_STACK)),"occupied set rejected");
            else {p.containerMenu.setCarried(new ItemStack(Items.DIRT));h.assertTrue(NestedReservationActions.handle(p,action(p,0,i,ReservationActionPayload.Source.CARRIED_STACK)),"cursor set rejected");}
            h.assertTrue(ReservationStore.getData(host).get(i).isPresent(),"wrong physical index");
        }
        h.assertTrue(ItemStack.matches(duplicate,chest.getItem(1)),"duplicate host edited");
        h.assertTrue(host.get(DataComponents.CONTAINER)==contents,"CONTAINER changed");
        h.assertTrue(host.getHoverName().getString().equals("Host"),"name changed");
        p.containerMenu.setCarried(ItemStack.EMPTY);
        h.assertTrue(NestedReservationActions.handle(p,action(p,0,0,ReservationActionPayload.Source.SLOT_STACK)),"occupied toggle rejected");
        for(int i:new int[]{8,9,17,18,26}) h.assertTrue(NestedReservationActions.handle(p,action(p,0,i,ReservationActionPayload.Source.CLEAR_EMPTY)),"clear rejected");
        h.assertTrue(ReservationStore.getData(host).isEmpty(),"final clear retained reservation");
        h.assertTrue(!NestedReservationActions.handle(p,action(p,0,26,ReservationActionPayload.Source.CLEAR_EMPTY)),"empty no-op consumed");h.succeed();
    }
    @GameTest(maxTicks=40) public void playerHostStaleCoordinatesAndReplacementFailClosed(GameTestHelper h) {
        var p=player(h);p.containerMenu=p.inventoryMenu;p.getInventory().setItem(9,new ItemStack(Blocks.SHULKER_BOX));
        int host=9;var baseline=action(p,host,26,ReservationActionPayload.Source.CARRIED_STACK);
        p.containerMenu.setCarried(new ItemStack(Items.STONE));
        for(var invalid:List.of(
                new NestedReservationActionPayload(999,baseline.stateId(),host,26,baseline.source(),baseline.hostFingerprint()),
                new NestedReservationActionPayload(baseline.menuId(),baseline.stateId()+1,host,26,baseline.source(),baseline.hostFingerprint()),
                new NestedReservationActionPayload(baseline.menuId(),baseline.stateId(),-1,26,baseline.source(),baseline.hostFingerprint()),
                new NestedReservationActionPayload(baseline.menuId(),baseline.stateId(),host,27,baseline.source(),baseline.hostFingerprint()),
                new NestedReservationActionPayload(baseline.menuId(),baseline.stateId(),host,26,baseline.source(),"0".repeat(64))))
            h.assertTrue(!NestedReservationActions.handle(p,invalid),"stale coordinates admitted");
        var replacement=new ItemStack(Blocks.SHULKER_BOX);replacement.set(DataComponents.CUSTOM_NAME,Component.literal("replacement"));
        p.getInventory().setItem(9,replacement);h.assertTrue(!NestedReservationActions.handle(p,baseline),"replacement admitted");
        h.assertTrue(ReservationStore.getData(replacement).isEmpty(),"replacement changed");
        replacement.setCount(2);h.assertTrue(!NestedReservationActions.handle(p,action(p,host,26,baseline.source())),"multicount admitted");
        replacement.setCount(1);h.assertTrue(NestedReservationActions.handle(p,action(p,host,26,baseline.source())),"inventory host rejected");
        h.assertTrue(ReservationStore.getData(replacement).matches(26,new ItemStack(Items.STONE)),"inventory reservation missing");h.succeed();
    }
    @GameTest(maxTicks=40) public void sharedViewerAndSourceGuards(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,Blocks.CHEST);var chest=h.getBlockEntity(pos,ChestBlockEntity.class);
        var p=player(h);var viewer=player(h);
        p.containerMenu=ChestMenu.threeRows(7,p.getInventory(),chest);
        viewer.containerMenu=ChestMenu.threeRows(8,viewer.getInventory(),chest);
        var host=new ItemStack(Blocks.SHULKER_BOX);chest.setItem(0,host);
        var observed=new java.util.concurrent.atomic.AtomicReference<ItemStack>(ItemStack.EMPTY);
        viewer.containerMenu.addSlotListener(new net.minecraft.world.inventory.ContainerListener() {
            public void slotChanged(net.minecraft.world.inventory.AbstractContainerMenu menu,int slot,ItemStack stack) {
                if(slot==0) observed.set(stack.copy());
            }
            public void dataChanged(net.minecraft.world.inventory.AbstractContainerMenu menu,int slot,int value) {}
        });
        h.assertTrue(!NestedReservationActions.handle(p,action(p,0,0,ReservationActionPayload.Source.SLOT_STACK)),"empty physical source admitted");
        h.assertTrue(!NestedReservationActions.handle(p,action(p,0,0,ReservationActionPayload.Source.CARRIED_STACK)),"empty cursor admitted");
        p.containerMenu.setCarried(new ItemStack(Items.STONE));
        h.assertTrue(!NestedReservationActions.handle(p,action(p,0,0,ReservationActionPayload.Source.CLEAR_EMPTY)),"clear with cursor admitted");
        h.assertTrue(NestedReservationActions.handle(p,action(p,0,0,ReservationActionPayload.Source.CARRIED_STACK)),"live carried rejected");
        h.assertTrue(ReservationStore.getData(observed.get()).matches(0,new ItemStack(Items.STONE)),"second viewer not broadcast");
        p.containerMenu.setCarried(new ItemStack(Blocks.SHULKER_BOX));
        h.assertTrue(!NestedReservationActions.handle(p,action(p,0,1,ReservationActionPayload.Source.CARRIED_STACK)),"shulker nesting admitted");
        var saved=action(p,0,2,ReservationActionPayload.Source.CARRIED_STACK);
        p.setPos(p.getX()+100,p.getY(),p.getZ());
        h.assertTrue(!NestedReservationActions.handle(p,saved),"out of reach host admitted");h.succeed();
    }
    @Override public void invokeTestMethod(GameTestHelper h,Method m)throws ReflectiveOperationException{m.invoke(this,h);}
}

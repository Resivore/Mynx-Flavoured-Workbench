package dev.resivore.quickstacknearbycompat.gametest;

import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import tempeststudios.quickstacknearby.*;
import java.lang.reflect.Method;
import java.util.*;

public class PhysicalNestedGameTests implements CustomTestMethodInvoker {
    static ServerPlayer player(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();var pos=h.absolutePos(new BlockPos(0,2,1));
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);return p;
    }
    static ChestBlockEntity chest(GameTestHelper h,int x) {
        var pos=new BlockPos(x,2,1);h.setBlock(pos,Blocks.CHEST);return h.getBlockEntity(pos,ChestBlockEntity.class);
    }
    static ItemStack host(int matching, boolean full) {
        var contents=NonNullList.withSize(27,ItemStack.EMPTY);
        if(full)for(int i=0;i<27;i++)contents.set(i,new ItemStack(Items.DIRT,64));
        if(matching>0)contents.set(0,new ItemStack(Items.STONE,matching));
        var host=new ItemStack(Blocks.SHULKER_BOX);host.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));
        host.set(DataComponents.CUSTOM_NAME,Component.literal("Exact host"));return host;
    }
    static NonNullList<ItemStack> contents(ItemStack host) {
        var list=NonNullList.withSize(27,ItemStack.EMPTY);host.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(list);return list;
    }
    @GameTest(maxTicks=40) public void realScanNestedOnlyPhysicalAffinityAndSourceAccounting(GameTestHelper h) {
        var chest=chest(h,1);var p=player(h);var host=host(60,false);chest.setItem(8,host);p.getInventory().setItem(9,new ItemStack(Items.STONE,70));
        var result=QuickStackService.quickStack(p);var list=contents(host);
        h.assertTrue(result.itemsMoved()==70&&result.sourceStacksTouched()==1&&result.targetContainersTouched()==1,"QSN feedback counts wrong");
        h.assertTrue(p.getInventory().getItem(9).isEmpty()&&chest.getItem(0).isEmpty(),"source or outer nested-only leak");
        h.assertTrue(list.get(0).getCount()==64&&list.get(1).getCount()==64&&list.get(2).getCount()==2,"merge/empty order or total wrong");
        h.assertTrue(host.getHoverName().getString().equals("Exact host"),"host name changed");h.succeed();
    }
    @GameTest(maxTicks=40) public void nativeParentThenChildrenThenNextParent(GameTestHelper h) {
        var first=chest(h,1);var second=chest(h,3);var p=player(h);
        for(int i=0;i<27;i++)first.setItem(i,new ItemStack(Items.DIRT,64));
        first.setItem(0,new ItemStack(Items.STONE,63));ItemStack a=host(63,true),b=host(63,true);
        first.setItem(1,a);first.setItem(2,b);second.setItem(0,new ItemStack(Items.STONE,63));
        p.getInventory().setItem(9,new ItemStack(Items.STONE,3));var result=QuickStackService.quickStack(p);
        h.assertTrue(result.itemsMoved()==3&&result.targetContainersTouched()==3,"parent/children counts wrong");
        h.assertTrue(first.getItem(0).getCount()==64&&contents(a).get(0).getCount()==64&&contents(b).get(0).getCount()==64
                &&second.getItem(0).getCount()==63,"native parent-first or child physical order lost");h.succeed();
    }
    @GameTest(maxTicks=40) public void emptyMultiCountMismatchAndMachineOutputAreExcluded(GameTestHelper h) {
        var chest=chest(h,1);var p=player(h);chest.setItem(0,host(0,false));var multi=host(1,false);chest.setItem(1,multi);multi.setCount(2);
        var distinct=host(0,false);var named=new ItemStack(Items.STONE);named.set(DataComponents.CUSTOM_NAME,Component.literal("distinct"));
        distinct.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(named)));chest.setItem(2,distinct);
        var pos=new BlockPos(2,2,1);h.setBlock(pos,Blocks.FURNACE);var furnace=h.getBlockEntity(pos,FurnaceBlockEntity.class);var output=host(1,false);furnace.setItem(2,output);
        p.getInventory().setItem(9,new ItemStack(Items.STONE,12));var result=QuickStackService.quickStack(p);
        h.assertTrue(result.itemsMoved()==0&&p.getInventory().getItem(9).getCount()==12&&contents(output).get(0).getCount()==1,"unsafe host was admitted");h.succeed();
    }
    @GameTest(maxTicks=40) public void fullPhysicalMatchKeepsAffinityAndRulesKeepExactRemainder(GameTestHelper h) {
        var chest=chest(h,1);var p=player(h);var host=host(64,false);chest.setItem(0,host);
        p.getInventory().setItem(9,new ItemStack(Items.STONE,12));var result=QuickStackService.quickStack(p);
        h.assertTrue(result.itemsMoved()==12&&contents(host).get(0).getCount()==64&&contents(host).get(1).getCount()==12,"full-match affinity lost");h.succeed();
    }
    @GameTest(maxTicks=40) public void vanillaAndCopperDoubleChestHostsAreUnique(GameTestHelper h) {
        for(ChestBlock block:List.of((ChestBlock)Blocks.CHEST,(ChestBlock)Blocks.COPPER_CHEST.asList().getFirst())) {
            var left=new BlockPos(1,2,1);var right=new BlockPos(2,2,1);
            h.setBlock(left,block.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.LEFT));
            h.setBlock(right,block.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.RIGHT));
            var a=h.getBlockEntity(left,ChestBlockEntity.class);var b=h.getBlockEntity(right,ChestBlockEntity.class);
            ItemStack first=host(63,true),second=host(63,true);a.setItem(0,first);b.setItem(0,second);
            var p=player(h);p.getInventory().setItem(9,new ItemStack(Items.STONE,3));var result=QuickStackService.quickStack(p);
            h.assertTrue(result.itemsMoved()==2&&result.targetContainersTouched()==2&&p.getInventory().getItem(9).getCount()==1,"double host duplicated or count wrong");
            h.assertTrue(contents(first).get(0).getCount()==64&&contents(second).get(0).getCount()==64,"double half writeback wrong");
            h.setBlock(left,Blocks.AIR);h.setBlock(right,Blocks.AIR);
        }h.succeed();
    }
    @GameTest(maxTicks=40) public void connectedDoubleBarrelHostsPersistExactlyOnce(GameTestHelper h) throws Exception {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("doublebarrels")) { h.succeed(); return; }
        var left=new BlockPos(1,2,1);var right=new BlockPos(2,2,1);
        h.setBlock(left,Blocks.BARREL);h.setBlock(right,Blocks.BARREL);
        var a=h.getBlockEntity(left,BarrelBlockEntity.class);var b=h.getBlockEntity(right,BarrelBlockEntity.class);
        var api=Class.forName("com.mozko.doublebarrels.DoubleBarrelAccess");
        api.getMethod("connectTo",BarrelBlockEntity.class).invoke(a,b);
        var combined=(Container)api.getMethod("getCombinedInventory").invoke(a);
        ItemStack first=host(63,true), second=host(63,true);
        combined.setItem(0,first);combined.setItem(27,second);
        var p=player(h);p.getInventory().setItem(9,new ItemStack(Items.STONE,3));
        var result=QuickStackService.quickStack(p);
        h.assertTrue(result.itemsMoved()==2&&result.targetContainersTouched()==2&&p.getInventory().getItem(9).getCount()==1,"double barrel accounting");
        h.assertTrue(contents(first).get(0).getCount()==64&&contents(second).get(0).getCount()==64,"double barrel owner writeback");
        h.assertTrue(combined.getItem(0)==first&&combined.getItem(27)==second,"double barrel host replaced");h.succeed();
    }
    @GameTest(maxTicks=40) public void nestedDiscoveryPreservesSourceLockAndKeepRules(GameTestHelper h) {
        var chest=chest(h,1);var p=player(h);var host=host(1,false);chest.setItem(0,host);
        p.getInventory().setItem(9,new ItemStack(Items.STONE,12));
        var locked=new QuickStackMoveEngine.SourceRules(Map.of(9,new QuickStackMoveEngine.SlotRule(true,0)));
        h.assertTrue(QuickStackService.quickStack(p,locked).itemsMoved()==0,"locked nested source moved");
        var kept=new QuickStackMoveEngine.SourceRules(Map.of(9,new QuickStackMoveEngine.SlotRule(false,12)));
        h.assertTrue(QuickStackService.quickStack(p,kept).itemsMoved()==0,"fully kept nested source moved");
        var partial=new QuickStackMoveEngine.SourceRules(Map.of(9,new QuickStackMoveEngine.SlotRule(false,3)));
        h.assertTrue(QuickStackService.quickStack(p,partial).itemsMoved()==9&&p.getInventory().getItem(9).getCount()==3
                &&contents(host).get(0).getCount()==10,"nested keep remainder wrong");h.succeed();
    }
    @Override public void invokeTestMethod(GameTestHelper h,Method m)throws ReflectiveOperationException{m.invoke(this,h);}
}

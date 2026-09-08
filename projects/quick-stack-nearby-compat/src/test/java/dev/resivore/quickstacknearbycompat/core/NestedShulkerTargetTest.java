package dev.resivore.quickstacknearbycompat.core;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class NestedShulkerTargetTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        for(var item:List.of(Items.STONE,Items.DIRT,Blocks.SHULKER_BOX.asItem()))
            if(!item.builtInRegistryHolder().areComponentsBound()) item.builtInRegistryHolder().bindComponents(
                    DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE,64).build());
    }
    private static ItemStack host(int count) {
        ItemStack host=new ItemStack(Blocks.SHULKER_BOX);
        host.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE,count))));
        host.set(DataComponents.CUSTOM_NAME,Component.literal("named"));return host;
    }
    @Test void physicalPartialThenEmptyAndExactComponentsAndCounts() {
        var parent=new SimpleContainer(1);var host=host(60);parent.setItem(0,host);
        var incoming=new ItemStack(Items.STONE,70);var target=new NestedShulkerTarget(parent,0,()->true);
        assertEquals(70,target.insert(incoming));assertTrue(incoming.isEmpty());
        assertEquals(64,target.getItem(0).getCount());assertEquals(64,target.getItem(1).getCount());assertEquals(2,target.getItem(2).getCount());
        assertEquals("named",host.getHoverName().getString());assertSame(host,parent.getItem(0));
    }
    @Test void emptyMultiCountAndNestingAreRejected() {
        var parent=new SimpleContainer(1);parent.setItem(0,new ItemStack(Blocks.SHULKER_BOX));
        assertFalse(new NestedShulkerTarget(parent,0,()->true).accepts(new ItemStack(Items.STONE)));
        parent.setItem(0,host(1));parent.getItem(0).setCount(2);assertFalse(NestedShulkerTarget.supported(parent.getItem(0)));
        assertEquals(0,new NestedShulkerTarget(parent,0,()->true).insert(new ItemStack(Items.STONE,8)));
        parent.getItem(0).setCount(1);assertEquals(0,new NestedShulkerTarget(parent,0,()->true).insert(new ItemStack(Blocks.SHULKER_BOX)));
    }
    @Test void sameItemDifferentComponentsAndUnrelatedItemsHaveNoAffinity() {
        var parent=new SimpleContainer(1);parent.setItem(0,host(1));var target=new NestedShulkerTarget(parent,0,()->true);
        var named=new ItemStack(Items.STONE,12);named.set(DataComponents.CUSTOM_NAME,Component.literal("other"));
        assertFalse(target.accepts(named));assertEquals(0,target.insert(named));assertEquals(12,named.getCount());
        assertFalse(target.accepts(new ItemStack(Items.DIRT)));
    }
    @Test void replacedComponentIdenticalHostAndChangedHostAreRejected() {
        var parent=new SimpleContainer(1);parent.setItem(0,host(1));var target=new NestedShulkerTarget(parent,0,()->true);
        parent.setItem(0,parent.getItem(0).copy());var incoming=new ItemStack(Items.STONE,8);
        assertEquals(0,target.insert(incoming));assertEquals(8,incoming.getCount());
        target=new NestedShulkerTarget(parent,0,()->true);parent.getItem(0).set(DataComponents.CUSTOM_NAME,Component.literal("moved"));
        assertEquals(0,target.insert(incoming));assertEquals(8,incoming.getCount());
    }
    @Test void revalidationFailurePreservesHostAndSource() {
        var parent=new SimpleContainer(1);var host=host(1);parent.setItem(0,host);
        var count=new java.util.concurrent.atomic.AtomicInteger();var target=new NestedShulkerTarget(parent,0,()->count.incrementAndGet()==1);
        var incoming=new ItemStack(Items.STONE,8);var before=host.copy();
        assertEquals(0,target.insert(incoming));assertEquals(8,incoming.getCount());assertTrue(ItemStack.matches(before,host));
    }
    @Test void failingWritebackRollsBackBeforeSourceDebit() {
        var parent=new SimpleContainer(1){boolean fail;@Override public void setChanged(){if(fail)throw new IllegalStateException("fixture");}};
        var host=host(1);parent.setItem(0,host);parent.fail=true;
        var target=new NestedShulkerTarget(parent,0,()->true);var incoming=new ItemStack(Items.STONE,8);var before=host.copy();
        assertEquals(0,target.insert(incoming));assertEquals(8,incoming.getCount());assertTrue(ItemStack.matches(before,host));
    }
    @Test void fullPhysicalMatchStillEstablishesAffinityAndDirtyParentIsNotified() {
        var changed=new java.util.concurrent.atomic.AtomicInteger();
        var parent=new SimpleContainer(1){@Override public void setChanged(){changed.incrementAndGet();}};parent.setItem(0,host(64));
        int baseline=changed.get();var target=new NestedShulkerTarget(parent,0,()->true);var incoming=new ItemStack(Items.STONE,8);
        assertTrue(target.accepts(incoming));assertEquals(8,target.insert(incoming));assertEquals(8,target.getItem(1).getCount());
        assertEquals(baseline+1,changed.get());
    }
}

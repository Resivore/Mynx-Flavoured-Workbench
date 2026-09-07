package tempeststudios.quickstacknearby;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

import java.lang.reflect.Method;
import java.util.List;

/** Live-search enumeration checks use real block entities but no client history or profile state. */
public final class C10NearbySearchGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void liveSnapshotIncludesActualTopLevelAndNestedContents(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper); ServerPlayer player = player(helper);
        chest.setItem(0, new ItemStack(Items.DIRT, 13));
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.DIAMOND, 4));
        ItemStack shulker = new ItemStack(Blocks.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        chest.setItem(1, shulker);
        List<NearbySearchPayload.Entry> snapshot = NearbySearchService.snapshot(player);
        BlockPos outer = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(snapshot.stream().anyMatch(e -> e.stack().is(Items.DIRT) && e.count() == 13 && e.position().equals(outer)),
                "live snapshot omitted actual top-level storage");
        helper.assertTrue(snapshot.stream().anyMatch(e -> e.stack().is(Items.DIAMOND) && e.count() == 4
                        && e.position().equals(outer) && !e.nestedName().isEmpty()),
                "nested result did not retain its outer physical host and path metadata");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void emptyContainersProduceNoHistoricalSearchEntries(GameTestHelper helper) {
        chest(helper); ServerPlayer player = player(helper);
        helper.assertTrue(NearbySearchService.snapshot(player).isEmpty(),
                "empty storage produced remembered or affinity-derived search results");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void staleLiveSearchTargetIsRejectedAndComponentVariantsStayDistinct(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper); ServerPlayer player = player(helper);
        ItemStack first = new ItemStack(Items.POISONOUS_POTATO, 2);
        first.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("first"));
        ItemStack second = new ItemStack(Items.POISONOUS_POTATO, 3);
        second.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("second"));
        chest.setItem(0, first); chest.setItem(1, second);
        List<NearbySearchPayload.Entry> snapshot = NearbySearchService.snapshot(player);
        helper.assertTrue(snapshot.stream().filter(e -> e.stack().is(Items.POISONOUS_POTATO)).count() == 2,
                "component-distinct actual stacks were merged into a fabricated search result");
        BlockPos outer = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(NearbySearchService.validTarget(player, outer, first, ""), "current result target was rejected");
        chest.removeItemNoUpdate(0);
        helper.assertTrue(!NearbySearchService.validTarget(player, outer, first, ""), "stale snapshot target rotated the player toward a removed item");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void emptyCsrReservationIsNotFabricatedAsNearbySearchContent(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1); helper.setBlock(pos, Blocks.BARREL);
        BarrelBlockEntity barrel = helper.getBlockEntity(pos, BarrelBlockEntity.class); ServerPlayer player = player(helper);
        ItemStack reservation = new ItemStack(Items.POISONOUS_POTATO);
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 3).orElseThrow(), reservation);
        helper.assertTrue(NearbySearchService.snapshot(player).stream().noneMatch(entry -> ItemStack.isSameItemSameComponents(entry.stack(), reservation)),
                "CSR reservation metadata appeared as historical Nearby Search content");
        helper.succeed();
    }

    private static ChestBlockEntity chest(GameTestHelper helper) { BlockPos pos = new BlockPos(1, 2, 1); helper.setBlock(pos, Blocks.CHEST); return helper.getBlockEntity(pos, ChestBlockEntity.class); }
    private static ServerPlayer player(GameTestHelper helper) { ServerPlayer result = helper.makeMockServerPlayerInLevel(); BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 1)); result.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5); return result; }
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException { method.invoke(this, helper); }
}

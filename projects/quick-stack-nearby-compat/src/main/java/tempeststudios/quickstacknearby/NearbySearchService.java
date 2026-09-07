package tempeststudios.quickstacknearby;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bounded, live, read-only QSN-range storage enumeration for Nearby Search. */
final class NearbySearchService {
    static final int MAX_RECORDS = 512;

    private NearbySearchService() {}

    static List<NearbySearchPayload.Entry> snapshot(ServerPlayer player) {
        ServerLevel level = ServerPlayerCompat.serverLevel(player);
        BlockPos center = player.blockPosition();
        QuickStackServerConfig config = QuickStackServerConfig.getInstance();
        int horizontal = config.horizontalRadius();
        int vertical = config.verticalRadius();
        BlockPos min = center.offset(-horizontal, -vertical, -horizontal);
        BlockPos max = center.offset(horizontal, vertical, horizontal);
        Set<BlockPos> seen = new HashSet<>();
        List<LiveContainer> containers = new ArrayList<>();
        for (BlockPos raw : BlockPos.betweenClosed(min, max)) {
            BlockPos position = raw.immutable();
            if (!seen.add(position) || !level.isLoaded(position)) continue;
            QuickStackService.ScannedContainer scanned = QuickStackService.scanContainer(level, player, center, position);
            if (scanned == null) continue;
            seen.addAll(scanned.positions());
            BlockPos aim = nearest(center, scanned.positions());
            String name = level.getBlockState(aim).getBlock().getName().getString();
            containers.add(new LiveContainer(scanned.container(), aim, name, Math.sqrt(scanned.distance())));
        }
        containers.sort(Comparator.comparingDouble(LiveContainer::distance));
        List<NearbySearchPayload.Entry> entries = new ArrayList<>();
        for (LiveContainer container : containers) {
            enumerate(container, entries);
            if (entries.size() >= MAX_RECORDS) break;
        }
        return List.copyOf(entries);
    }

    static boolean validTarget(ServerPlayer player, BlockPos target, ItemStack expected, String nestedName) {
        ServerLevel level = ServerPlayerCompat.serverLevel(player);
        BlockPos center = player.blockPosition();
        QuickStackServerConfig config = QuickStackServerConfig.getInstance();
        // A client coordinate is only a selection hint.  The same rectangular QSN discovery
        // boundary used by snapshot() remains the server's authorization boundary.
        if (Math.abs(target.getX() - center.getX()) > config.horizontalRadius()
                || Math.abs(target.getZ() - center.getZ()) > config.horizontalRadius()
                || Math.abs(target.getY() - center.getY()) > config.verticalRadius()) return false;
        if (!level.isLoaded(target)) return false;
        QuickStackService.ScannedContainer scanned = QuickStackService.scanContainer(level, player, center, target);
        return scanned != null && containsLiveResult(scanned.container(), expected, nestedName);
    }

    private static boolean containsLiveResult(Container container, ItemStack expected, String nestedName) {
        if (expected == null || expected.isEmpty()) return false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack actual = container.getItem(slot);
            if (nestedName == null || nestedName.isEmpty()) {
                if (ItemStack.isSameItemSameComponents(actual, expected)) return true;
                continue;
            }
            if (!NestedShulkerTarget.supported(actual) || !nestedName.equals(actual.getHoverName().getString())) continue;
            NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
            actual.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
            for (ItemStack nested : contents) if (ItemStack.isSameItemSameComponents(nested, expected)) return true;
        }
        return false;
    }

    private static void enumerate(LiveContainer outer, List<NearbySearchPayload.Entry> out) {
        for (int slot = 0; slot < outer.container().getContainerSize() && out.size() < MAX_RECORDS; slot++) {
            ItemStack stack = outer.container().getItem(slot);
            if (stack.isEmpty()) continue;
            out.add(new NearbySearchPayload.Entry(stack.copy(), stack.getCount(), outer.position(), outer.name(), "", outer.distance()));
            if (!NestedShulkerTarget.supported(stack)) continue;
            NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
            stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
            String nestedName = stack.getHoverName().getString();
            for (ItemStack nested : contents) {
                if (nested.isEmpty() || out.size() >= MAX_RECORDS) continue;
                // Empty CSR reservations cannot appear here: only actual component contents are enumerated.
                out.add(new NearbySearchPayload.Entry(nested.copy(), nested.getCount(), outer.position(), outer.name(), nestedName, outer.distance()));
            }
        }
    }

    private static BlockPos nearest(BlockPos center, List<BlockPos> positions) {
        return positions.stream().min(Comparator.comparingDouble(center::distSqr)).orElseThrow().immutable();
    }

    private record LiveContainer(Container container, BlockPos position, String name, double distance) {}
}

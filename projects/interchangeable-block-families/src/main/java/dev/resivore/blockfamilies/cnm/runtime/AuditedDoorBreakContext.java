package dev.resivore.blockfamilies.cnm.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Server-thread scope for one survival player break of an audited alternate
 * door. CNM evaluates loot once per physical segment, while IBF equivalence
 * defines one logical item; the context lets the drop hook suppress those
 * segment evaluations and emit the source design exactly once at completion.
 */
public final class AuditedDoorBreakContext {
    private static final ThreadLocal<BreakContext> ACTIVE = new ThreadLocal<>();

    private AuditedDoorBreakContext() {
    }

    public static void clear() {
        ACTIVE.remove();
    }

    public static void begin(ServerLevel level, BlockPos pos, Item sourceItem) {
        ACTIVE.set(new BreakContext(level, pos.immutable(), sourceItem));
    }

    public static boolean suppressesSegmentDrop(Item item) {
        BreakContext context = ACTIVE.get();
        return context != null && context.sourceItem() == item;
    }

    public static void finish(boolean destroyMethodSucceeded) {
        BreakContext context = ACTIVE.get();
        ACTIVE.remove();
        if (context == null || !destroyMethodSucceeded) {
            return;
        }
        if (context.level().getBlockState(context.pos()).is(Block.byItem(context.sourceItem()))) {
            return;
        }
        Block.popResource(context.level(), context.pos(), context.sourceItem().getDefaultInstance());
    }

    private record BreakContext(ServerLevel level, BlockPos pos, Item sourceItem) {
    }
}

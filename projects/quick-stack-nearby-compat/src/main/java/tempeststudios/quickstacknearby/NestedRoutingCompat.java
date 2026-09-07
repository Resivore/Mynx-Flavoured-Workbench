package tempeststudios.quickstacknearby;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds C9's one-level child targets after QSN has admitted the outer container. */
final class NestedRoutingCompat {
    private NestedRoutingCompat() {}

    static List<QuickStackMoveEngine.Target> children(Container parent, ServerPlayer player, Container source,
            int firstSourceSlot, int exclusiveLastSourceSlot, QuickStackMoveEngine.SourceRules rules) {
        List<QuickStackMoveEngine.Target> result = new ArrayList<>();
        for (int slot = 0; slot < parent.getContainerSize(); slot++) {
            ItemStack host = parent.getItem(slot);
            if (!NestedShulkerTarget.supported(host) || !parent.canPlaceItem(slot, host)) continue;
            NestedShulkerTarget nested = new NestedShulkerTarget(parent, slot, () -> player.isAlive() && parent.stillValid(player));
            LinkedHashSet<QuickStackMoveEngine.StackKey> keys = new LinkedHashSet<>();
            for (int sourceSlot = Math.max(0, firstSourceSlot); sourceSlot < Math.min(exclusiveLastSourceSlot, source.getContainerSize()); sourceSlot++) {
                ItemStack incoming = source.getItem(sourceSlot);
                if (!incoming.isEmpty() && !rules.isLocked(sourceSlot)
                        && rules.movableCount(sourceSlot, incoming.getCount()) > 0 && nested.accepts(incoming)) {
                    keys.add(QuickStackMoveEngine.StackKey.of(incoming));
                }
            }
            if (!keys.isEmpty()) result.add(new QuickStackMoveEngine.Target(nested, Set.copyOf(keys)));
        }
        return result;
    }
}

package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.ArrayList;
import java.util.List;

/** Workbench-only exclusions applied to QSN's completed nearby-target scan. */
public final class QsnDestinationExclusions {
    private QsnDestinationExclusions() {}

    public static List<QuickStackMoveEngine.Target> filterVanillaShelves(
            List<QuickStackMoveEngine.Target> targets) {
        if (targets == null || targets.isEmpty()) {
            return targets;
        }

        List<QuickStackMoveEngine.Target> filtered = new ArrayList<>(targets.size());
        for (QuickStackMoveEngine.Target target : targets) {
            if (!(target.container() instanceof ShelfBlockEntity)) {
                filtered.add(target);
            }
        }
        return filtered.size() == targets.size() ? targets : filtered;
    }
}

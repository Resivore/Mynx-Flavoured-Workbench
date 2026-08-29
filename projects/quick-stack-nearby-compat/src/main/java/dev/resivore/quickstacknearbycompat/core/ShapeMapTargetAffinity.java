package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Adds exact source keys to frozen QSN targets when CNM's ShapeMap admits the item pair. */
public final class ShapeMapTargetAffinity {
    private ShapeMapTargetAffinity() {}

    public static List<QuickStackMoveEngine.Target> augmentTargets(
            Container source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            List<QuickStackMoveEngine.Target> targets,
            QuickStackMoveEngine.SourceRules sourceRules) {
        if (targets == null || targets.isEmpty() || !CnmShapeMapResolver.isAvailable()) {
            return targets;
        }

        QuickStackMoveEngine.SourceRules rules = sourceRules == null
                ? QuickStackMoveEngine.SourceRules.EMPTY
                : sourceRules;
        List<SourceKey> sourceKeys = new ArrayList<>();
        int lastSourceSlot = Math.min(exclusiveLastSourceSlot, source.getContainerSize());
        for (int sourceSlot = Math.max(0, firstSourceSlot); sourceSlot < lastSourceSlot; sourceSlot++) {
            ItemStack stack = source.getItem(sourceSlot);
            if (stack.isEmpty() || rules.isLocked(sourceSlot) || rules.movableCount(sourceSlot, stack.getCount()) <= 0) {
                continue;
            }
            sourceKeys.add(new SourceKey(stack, QuickStackMoveEngine.StackKey.of(stack)));
        }
        if (sourceKeys.isEmpty()) {
            return targets;
        }

        List<QuickStackMoveEngine.Target> augmented = new ArrayList<>(targets.size());
        boolean changed = false;
        for (QuickStackMoveEngine.Target target : targets) {
            LinkedHashSet<QuickStackMoveEngine.StackKey> acceptedTypes = new LinkedHashSet<>(target.acceptedTypes());
            Container targetContainer = target.container();
            for (int targetSlot = 0; targetSlot < targetContainer.getContainerSize(); targetSlot++) {
                ItemStack targetStack = targetContainer.getItem(targetSlot);
                if (targetStack.isEmpty()) {
                    continue;
                }
                for (SourceKey sourceKey : sourceKeys) {
                    if (CnmShapeMapResolver.inSameShapeSet(
                            sourceKey.stack().getItem(),
                            targetStack.getItem())) {
                        acceptedTypes.add(sourceKey.key());
                    }
                }
            }

            if (acceptedTypes.equals(target.acceptedTypes())) {
                augmented.add(target);
            } else {
                changed = true;
                augmented.add(new QuickStackMoveEngine.Target(
                        targetContainer,
                        Collections.unmodifiableSet(acceptedTypes)
                ));
            }
        }
        return changed ? List.copyOf(augmented) : targets;
    }

    private record SourceKey(ItemStack stack, QuickStackMoveEngine.StackKey key) {}
}

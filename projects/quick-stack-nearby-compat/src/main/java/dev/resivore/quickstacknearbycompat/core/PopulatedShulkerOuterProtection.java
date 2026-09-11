package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Action-start protection for populated vanilla shulkers used as loose QSN sources.
 *
 * <p>This deliberately overlays only the native loose-source rules.  C16's carried-container
 * adapter receives the unmodified user rules, so an unlocked populated carrier can still drain
 * its direct physical contents after the loose phase.</p>
 */
public final class PopulatedShulkerOuterProtection {
    private static final int SHULKER_SLOT_COUNT = 27;

    private PopulatedShulkerOuterProtection() {}

    public static QuickStackMoveEngine.SourceRules snapshotLooseRules(
            Inventory inventory, QuickStackMoveEngine.SourceRules userRules) {
        Set<Integer> protectedSlots = new HashSet<>();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            if (isPhysicallyNonEmptyShulker(inventory.getItem(slot))) {
                protectedSlots.add(slot);
            }
        }
        return overlay(userRules, protectedSlots);
    }

    /** A vanilla shulker is populated only when one of its 27 physical slots has an item. */
    public static boolean isPhysicallyNonEmptyShulker(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock)) {
            return false;
        }
        NonNullList<ItemStack> physical = NonNullList.withSize(SHULKER_SLOT_COUNT, ItemStack.EMPTY);
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(physical);
        return physical.stream().anyMatch(candidate -> !candidate.isEmpty());
    }

    static QuickStackMoveEngine.SourceRules overlay(
            QuickStackMoveEngine.SourceRules userRules, Set<Integer> protectedSlots) {
        QuickStackMoveEngine.SourceRules normalized = userRules == null
                ? QuickStackMoveEngine.SourceRules.EMPTY : userRules;
        if (protectedSlots.isEmpty()) return normalized;

        Map<Integer, QuickStackMoveEngine.SlotRule> rules = new HashMap<>(normalized.slotRules());
        for (int slot : protectedSlots) {
            // The transient lock wins only for this native loose-source invocation.
            rules.put(slot, new QuickStackMoveEngine.SlotRule(true, 0));
        }
        return new QuickStackMoveEngine.SourceRules(Map.copyOf(rules));
    }
}

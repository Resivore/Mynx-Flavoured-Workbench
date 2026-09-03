package com.yungnickyoung.minecraft.ribbits.world.loot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/** Shared mechanical identity for a Ribbit Village Explorer Map search miss. */
public final class RibbitVillageExplorerMap {
    public static final String SUCCESS_NAME_KEY = "item.ribbits.ribbit_village_explorer_map";
    public static final String FAILURE_NAME_KEY = "item.ribbits.uncharted_ribbit_map";
    public static final String FAILURE_LORE_KEY = "item.ribbits.uncharted_ribbit_map.lore";
    public static final String FAILED_MARKER_KEY = "ribbits:failed_ribbit_village_map";

    private static final CustomData FAILED_MARKER = createFailedMarker();

    private RibbitVillageExplorerMap() {
    }

    private static CustomData createFailedMarker() {
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(FAILED_MARKER_KEY, true);
        return CustomData.of(marker);
    }

    /**
     * Creates the display stack used by the Sorcerer service. The loot item modifier produces the
     * same components data-first; matching itself is intentionally based on the private marker,
     * never the player-visible name or lore.
     */
    public static ItemStack createFailedMap(int count) {
        ItemStack stack = new ItemStack(Items.MAP, count);
        stack.set(DataComponents.CUSTOM_DATA, FAILED_MARKER);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(FAILURE_NAME_KEY));
        stack.set(DataComponents.LORE,
                new ItemLore(List.of(Component.translatable(FAILURE_LORE_KEY))));
        return stack;
    }

    public static CustomData failedMarker() {
        return FAILED_MARKER;
    }

    /**
     * Vanilla ExplorationMapFunction returns its input map unchanged on a miss and creates a new
     * filled map with MAP_ID on a hit. Preserve only that complete success shape; every other
     * result is replaced rather than component-edited so even transient stale data is impossible.
     */
    public static ItemStack finalizeSearchResult(ItemStack stack) {
        if (stack.is(Items.FILLED_MAP) && stack.has(DataComponents.MAP_ID)) {
            stack.setCount(1);
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable(SUCCESS_NAME_KEY));
            return stack;
        }
        return createFailedMap(1);
    }

    /** Exact private marker and empty-map identity; display text is not an authenticity signal. */
    public static boolean isFailedMap(ItemStack stack) {
        return stack.is(Items.MAP)
                && !stack.has(DataComponents.MAP_ID)
                && FAILED_MARKER.equals(stack.get(DataComponents.CUSTOM_DATA));
    }
}

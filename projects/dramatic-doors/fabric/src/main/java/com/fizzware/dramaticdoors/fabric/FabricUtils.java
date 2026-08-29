package com.fizzware.dramaticdoors.fabric;

import com.fizzware.dramaticdoors.compat.CompatChecker;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.Item;
import oshi.util.tuples.Pair;

public final class FabricUtils implements CompatChecker {
    public static final FabricUtils INSTANCE = new FabricUtils();

    private FabricUtils() {
    }

    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    @Override
    public boolean isDev() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    /**
     * Minecraft 26.2 removed Fabric's item-group mutation API. Dramatic Doors'
     * own tabs are populated through vanilla display generators instead.
     */
    public static void assignItemsToTabs() {
    }

    public static void addMainTabEntries(CreativeModeTab.Output output) {
        addMatching(output, null);
    }

    public static void addChippedTabEntries(CreativeModeTab.Output output) {
        addMatching(output, "chipped");
    }

    public static void addMacawTabEntries(CreativeModeTab.Output output) {
        addMatching(output, "macaw");
    }

    public static void addManyIdeasTabEntries(CreativeModeTab.Output output) {
        addMatching(output, "manyideas");
    }

    private static void addMatching(CreativeModeTab.Output output, String family) {
        for (Pair<String, Item> pair : DDRegistry.DOOR_ITEMS) {
            boolean optionalFamily = pair.getA().contains("chipped")
                    || pair.getA().contains("macaw")
                    || pair.getA().contains("manyideas");
            if ((family == null && !optionalFamily) || (family != null && pair.getA().contains(family))) {
                output.accept(pair.getB(), TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }
}

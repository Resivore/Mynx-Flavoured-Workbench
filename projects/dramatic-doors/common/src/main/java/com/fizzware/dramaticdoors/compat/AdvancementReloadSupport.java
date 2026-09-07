package com.fizzware.dramaticdoors.compat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps generated advancement additions separate from the immutable input
 * snapshots supplied by the Minecraft 26.2 reload pipeline.
 */
public final class AdvancementReloadSupport {
    private AdvancementReloadSupport() {
    }

    public static <K, V> Map<K, V> copyForGeneratedEntries(Map<K, V> advancements, boolean hasGeneratedEntries) {
        return hasGeneratedEntries ? new LinkedHashMap<>(advancements) : advancements;
    }
}

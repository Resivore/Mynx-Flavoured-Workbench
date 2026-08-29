package com.crispytwig.naturalist.port.loot;

/**
 * Stable boundary for optional downstream loot policy.
 *
 * <p>Phase 1 deliberately installs no loot-table callbacks and therefore leaves every upstream
 * entity loot table and code-owned drop unchanged. Future Workbench policy belongs behind this
 * boundary rather than in entity, AI, interaction, or rendering classes.</p>
 */
public final class NaturalistDropPolicy {
    private NaturalistDropPolicy() {
    }

    public static void initializeUpstreamBaseline() {
        // Intentionally empty: the preservation baseline is the staged upstream loot/data corpus.
    }
}

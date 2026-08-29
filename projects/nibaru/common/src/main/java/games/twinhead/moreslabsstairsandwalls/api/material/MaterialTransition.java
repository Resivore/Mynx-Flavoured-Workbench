package games.twinhead.moreslabsstairsandwalls.api.material;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;

public record MaterialTransition(Type type, ModBlocks target) {
    public enum Type {
        SPREADABLE_BASE,
        STRIPPED,
        NEXT_OXIDATION,
        PREVIOUS_OXIDATION,
        WAXED,
        UNWAXED,
        CORAL_DEATH,
        CONCRETE_HARDENING,
        PATH_REVERSION,
        PATH_TARGET,
        /** Canonical material-owned item-drop target for matching derived geometry. */
        DROP_BASE,
        PODZOL_GROWTH
    }
}

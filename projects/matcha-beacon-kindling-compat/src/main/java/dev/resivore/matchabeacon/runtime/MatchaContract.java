package dev.resivore.matchabeacon.runtime;

import net.minecraft.resources.Identifier;

import java.util.Set;

public final class MatchaContract {
    public static final String PLAYER_LOCK_TAG = "SummonedTrader";
    public static final String MARKER_TAG = "beacon_kindling";
    public static final String MARKER_VISITING_TAG = "summoned_trader";
    public static final String TRADER_TAG = "summoned_by_beacon";
    public static final String OWNED_MARKER_TAG = "matcha_beacon_kindling_compat_owned";
    public static final String OWNED_TRADER_TAG = "matcha_beacon_kindling_compat_owned_trader";
    public static final String TIMER_OBJECTIVE = "wandering_trader_timer_score";

    public static final Identifier ITEM_MODEL = Identifier.parse("minecraft:beacon_kindling");
    public static final Identifier ADVANCEMENT = Identifier.parse("main:mechanics/beacon_kindling");
    public static final Identifier SUMMON_FUNCTION = Identifier.parse(
            "main:mechanic/wandering_trader/summon_wandering_trader");

    public static final String PLACEMENT_FUNCTION =
            "main:mechanic/wandering_trader/beacon_kindling_placed";
    public static final String TIMER_FUNCTION =
            "main:mechanic/wandering_trader/check_wandering_trader_timer_loop";
    public static final String INITIALISE_FUNCTION =
            "main:mechanic/wandering_trader/initialise_wandering_trader_spawn";
    public static final String SUMMON_FUNCTION_ID = SUMMON_FUNCTION.toString();
    public static final String KILL_FUNCTION =
            "main:mechanic/wandering_trader/kill_wandering_trader";
    public static final String EARLY_KILL_FUNCTION =
            "main:mechanic/wandering_trader/kill_wandering_trader_early";
    public static final String KILL_BEACON_FUNCTION =
            "main:mechanic/wandering_trader/kill_this_beacon";

    private static final Set<String> PATCHED_FUNCTIONS = Set.of(
            PLACEMENT_FUNCTION,
            TIMER_FUNCTION,
            INITIALISE_FUNCTION,
            SUMMON_FUNCTION_ID,
            KILL_FUNCTION,
            EARLY_KILL_FUNCTION,
            KILL_BEACON_FUNCTION
    );

    public static final String DUPLICATE_MESSAGE =
            "You have already summoned a Wandering Trader, please wait patiently while they travel";
    public static final String APPROACH_MESSAGE =
            "A Wandering Trader has spotted your beacon, they will arrive in 10 minutes";
    public static final String LOST_MESSAGE =
            "The Wandering Trader has lost sight of your beacon...";
    public static final String LEFT_MESSAGE = "The Wandering Trader has left";

    private MatchaContract() {
    }

    public static boolean isPatchedFunction(String functionId) {
        return PATCHED_FUNCTIONS.contains(functionId);
    }
}

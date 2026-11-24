package dev.resivore.matchaheart;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Exact identifiers and order-independent replacement logic for the Matcha heart boundary. */
public final class HeartDataContract {
    public static final String MATCHA_ARCHIVE = "projects/matcha-flavoured-data/upstream/MF_1_12_5_26_2.zip";
    public static final String MATCHA_SHA256 =
            "966843D0AEB3860479721EF34791A631E62BC225FDAA918DD2AD90C49977E5B4";
    public static final String RECIPE_ID = "matcha:crafting/crystal_heart";
    public static final String REINFORCED_RECIPE_ID = "matcha_heart_death_compat:reinforced_crystal_heart";
    public static final String RESONANT_FAVOUR_RECIPE_ID =
            "matcha_heart_death_compat:resonant_favour";
    public static final String MATCHA_BASE_ITEM = "minecraft:poisonous_potato";
    public static final String MATCHA_ITEM_MODEL = "matcha:heart_container";
    public static final String MATCHA_ITEM_NAME = "item.kleispack.crystal_heart";
    public static final String ADVANCEMENT_ID = "matcha:mechanics/crystal_heart_used";
    public static final String PROCESS_FUNCTION_ID = "matcha:mechanics/heart_container/use_crystal_heart";
    public static final String DEATH_FUNCTION_ID = "matcha:mechanics/heart_container/detect_death";
    public static final Set<String> BLOCKED_FUNCTIONS = Set.of(PROCESS_FUNCTION_ID, DEATH_FUNCTION_ID);
    public static final Map<String, String> LOOT_TABLE_RESOURCES;
    public static final Map<String, String> RECIPE_RESOURCES;
    public static final Map<String, String> ADVANCEMENT_RESOURCES;

    static {
        LinkedHashMap<String, String> lootTables = new LinkedHashMap<>();
        lootTables.put("minecraft:gameplay/fishing/deep_dark",
                "data/minecraft/loot_table/gameplay/fishing/deep_dark.json");
        lootTables.put("minecraft:blocks/sculk_sensor",
                "data/minecraft/loot_table/blocks/sculk_sensor.json");
        lootTables.put("minecraft:blocks/calibrated_sculk_sensor",
                "data/minecraft/loot_table/blocks/calibrated_sculk_sensor.json");
        lootTables.put("minecraft:blocks/sculk_shrieker",
                "data/minecraft/loot_table/blocks/sculk_shrieker.json");
        LOOT_TABLE_RESOURCES = Collections.unmodifiableMap(lootTables);

        LinkedHashMap<String, String> recipes = new LinkedHashMap<>();
        recipes.put(RECIPE_ID, "data/matcha/recipe/crafting/crystal_heart.json");
        recipes.put("matcha:crafting/sculk_sensor", "data/matcha/recipe/crafting/sculk_sensor.json");
        recipes.put("matcha:crafting/sculk_shrieker", "data/matcha/recipe/crafting/sculk_shrieker.json");
        RECIPE_RESOURCES = Collections.unmodifiableMap(recipes);

        LinkedHashMap<String, String> advancements = new LinkedHashMap<>();
        advancements.put(ADVANCEMENT_ID,
                "data/matcha/advancement/mechanics/crystal_heart_used.json");
        advancements.put("matcha:recipe_unlocks/echo_shard",
                "data/matcha/advancement/recipe_unlocks/echo_shard.json");
        ADVANCEMENT_RESOURCES = Collections.unmodifiableMap(advancements);
    }

    private HeartDataContract() {}

    public static <T> Map<String, T> replaceExact(
            Map<String, T> resolved, String targetId, Supplier<T> authoritativeValue) {
        LinkedHashMap<String, T> result = new LinkedHashMap<>(resolved);
        T value = authoritativeValue.get();
        if (value == null) throw new IllegalStateException("Authoritative contract value was null for " + targetId);
        result.put(targetId, value);
        return Map.copyOf(result);
    }

    public static boolean blocksFunction(String id) {
        return BLOCKED_FUNCTIONS.contains(id);
    }
}

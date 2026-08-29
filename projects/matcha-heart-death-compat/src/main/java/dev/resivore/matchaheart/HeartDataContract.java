package dev.resivore.matchaheart;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Exact identifiers and order-independent replacement logic for the Matcha heart boundary. */
public final class HeartDataContract {
    public static final String MATCHA_ARCHIVE = "originals/datapacks/Matcha_Flavoured_1_12.zip";
    public static final String MATCHA_SHA256 =
            "6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248";
    public static final String RECIPE_ID = "crafting:crystal_heart";
    public static final String REINFORCED_RECIPE_ID = "matcha_heart_death_compat:reinforced_crystal_heart";
    public static final String MATCHA_BASE_ITEM = "minecraft:poisonous_potato";
    public static final String MATCHA_ITEM_MODEL = "minecraft:heart_container";
    public static final String MATCHA_ITEM_NAME = "item.kleispack.crystal_heart";
    public static final String ADVANCEMENT_ID = "main:mechanics/heart_container_obtained";
    public static final String PROCESS_FUNCTION_ID = "main:mechanic/heart_container/process_heart_container";
    public static final String DEATH_FUNCTION_ID = "main:mechanic/heart_container/detect_death";
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
        recipes.put(RECIPE_ID, "data/crafting/recipe/crystal_heart.json");
        recipes.put("crafting:sculk_sensor", "data/crafting/recipe/sculk_sensor.json");
        recipes.put("crafting:sculk_shrieker", "data/crafting/recipe/sculk_shrieker.json");
        RECIPE_RESOURCES = Collections.unmodifiableMap(recipes);

        LinkedHashMap<String, String> advancements = new LinkedHashMap<>();
        advancements.put(ADVANCEMENT_ID,
                "data/main/advancement/mechanics/heart_container_obtained.json");
        advancements.put("main:recipe_unlocks/echo_shard",
                "data/main/advancement/recipe_unlocks/echo_shard.json");
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

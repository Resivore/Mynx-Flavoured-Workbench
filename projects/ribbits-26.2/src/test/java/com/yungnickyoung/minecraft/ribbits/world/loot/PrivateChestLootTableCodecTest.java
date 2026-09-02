package com.yungnickyoung.minecraft.ribbits.world.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateChestLootTableCodecTest {
    private static final String FISHERMAN = "data/ribbits/loot_table/chests/fisherman_main.json";
    private static final String SORCERER = "data/ribbits/loot_table/chests/sorcerer.json";

    private static RegistryOps<JsonElement> registryOps;
    private static Registry<Item> itemRegistry;

    @BeforeAll
    static void bootstrapMinecraftRegistryContext() throws ReflectiveOperationException {
        SharedConstants.tryDetectVersion();

        // Item.CODEC is intentionally bound to BuiltInRegistries.ITEM in Minecraft 26.2.
        // Populate that exact registry without freezing it, register the one private Ribbits
        // item referenced by these tables, then freeze it exactly once. Gradle forks every test
        // class so no other bootstrap can race or pre-freeze this exact codec context.
        Field bootstrapped = Bootstrap.class.getDeclaredField("isBootstrapped");
        bootstrapped.setAccessible(true);
        bootstrapped.setBoolean(null, true);
        invokeBuiltInRegistryPhase("createContents");
        ResourceKey<Item> giantLilypadKey = ResourceKey.create(
                Registries.ITEM, Identifier.parse("ribbits:giant_lilypad"));
        Registry.register(BuiltInRegistries.ITEM, giantLilypadKey,
                new Item(new Item.Properties().setId(giantLilypadKey)));
        invokeBuiltInRegistryPhase("freeze");
        itemRegistry = BuiltInRegistries.ITEM;

        MappedRegistry<Enchantment> enchantments = new MappedRegistry<>(
                Registries.ENCHANTMENT, Lifecycle.stable());
        enchantments.bindTags(Map.of(
                TagKey.create(Registries.ENCHANTMENT, Identifier.parse("minecraft:on_random_loot")),
                List.of()));
        Registry<Enchantment> frozenEnchantments = enchantments.freeze();

        List<Registry<?>> registries = new ArrayList<>(
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
                        .registries()
                        .map(RegistryAccess.RegistryEntry::value)
                        .filter(registry -> !registry.key().equals(Registries.ITEM))
                        .filter(registry -> !registry.key().equals(Registries.ENCHANTMENT))
                        .toList());
        registries.add(itemRegistry);
        registries.add(frozenEnchantments);
        registryOps = RegistryOps.create(
                JsonOps.INSTANCE, new RegistryAccess.ImmutableRegistryAccess(registries));
        assertTrue(registryOps.getter(Registries.ITEM).orElseThrow()
                .get(giantLilypadKey).isPresent());
    }

    private static void invokeBuiltInRegistryPhase(String methodName)
            throws ReflectiveOperationException {
        Method method = BuiltInRegistries.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(null);
    }

    @Test
    void bothRepairedTablesPassTheExactMinecraftTwentySixLootCodec() throws IOException {
        Map<String, JsonObject> tables = loadPrivateTablesOrSyntheticFixtures();
        assertEquals(List.of(FISHERMAN, SORCERER), new ArrayList<>(tables.keySet()));

        validateTable(tables.get(FISHERMAN), 15, 4, 4, false);
        validateTable(tables.get(SORCERER), 1, 6, 2, true);
    }

    @Test
    void minecraftTwentySixCodecRejectsAirButAcceptsTheFaithfulEmptyEntry() throws IOException {
        JsonObject repaired = loadPrivateTablesOrSyntheticFixtures().get(FISHERMAN);
        LootTable.DIRECT_CODEC.parse(registryOps, repaired).getOrThrow();

        JsonObject invalid = repaired.deepCopy();
        JsonObject entry = invalid.getAsJsonArray("pools")
                .get(1).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", "minecraft:air");

        var result = LootTable.DIRECT_CODEC.parse(registryOps, invalid);
        assertTrue(result.error().isPresent());
        assertTrue(result.error().orElseThrow().message().contains("Item must not be minecraft:air"));
    }

    private static void validateTable(JsonObject table, int emptyWeight,
                                      int firstPoolEntries, int secondPoolEntries,
                                      boolean expectPotionMigration) {
        assertEquals("minecraft:chest", table.get("type").getAsString());
        JsonArray pools = table.getAsJsonArray("pools");
        assertEquals(2, pools.size());
        assertEquals(firstPoolEntries,
                pools.get(0).getAsJsonObject().getAsJsonArray("entries").size());
        assertEquals(secondPoolEntries,
                pools.get(1).getAsJsonObject().getAsJsonArray("entries").size());

        JsonObject empty = pools.get(1).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("minecraft:empty", empty.get("type").getAsString());
        assertEquals(emptyWeight, empty.get("weight").getAsInt());
        assertFalse(empty.has("name"));

        List<String> itemIds = new ArrayList<>();
        collectItemIds(table, itemIds);
        assertFalse(itemIds.isEmpty());
        for (String itemId : itemIds) {
            Identifier id = Identifier.parse(itemId);
            assertTrue(itemRegistry.containsKey(id), "missing Minecraft 26.2 item: " + id);
            Item resolved = itemRegistry.getValue(id);
            assertNotNull(resolved, "unresolved Minecraft 26.2 item: " + id);
            assertNotSame(net.minecraft.world.item.Items.AIR, resolved,
                    "item resolves to minecraft:air: " + id);
            assertNotEquals("minecraft:air", itemId);
            assertNotEquals(BuiltInRegistries.ITEM.getKey(net.minecraft.world.item.Items.AIR), id);
        }

        if (expectPotionMigration) {
            JsonObject potion = pools.get(1).getAsJsonObject()
                    .getAsJsonArray("entries").get(1).getAsJsonObject();
            assertEquals("minecraft:potion", potion.get("name").getAsString());
            JsonArray functions = potion.getAsJsonArray("functions");
            assertEquals(1, functions.size());
            JsonObject setPotion = functions.get(0).getAsJsonObject();
            assertEquals("minecraft:set_potion", setPotion.get("function").getAsString());
            assertEquals("minecraft:strong_leaping", setPotion.get("id").getAsString());
        }

        LootTable.DIRECT_CODEC.parse(registryOps, table).getOrThrow();
    }

    private static void collectItemIds(JsonElement element, List<String> itemIds) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectItemIds(child, itemIds));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("type")
                && "minecraft:item".equals(object.get("type").getAsString())
                && object.has("name")) {
            itemIds.add(object.get("name").getAsString());
        }
        object.entrySet().forEach(entry -> collectItemIds(entry.getValue(), itemIds));
    }

    private static Map<String, JsonObject> loadPrivateTablesOrSyntheticFixtures() throws IOException {
        String configured = System.getProperty("privateResourcesDir");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("RIBBITS_PRIVATE_RESOURCES_DIR");
        }
        if (configured == null || configured.isBlank()) {
            return syntheticFixtures();
        }

        Path root = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(root), "RIBBITS_PRIVATE_RESOURCES_DIR is not a directory");
        Map<String, JsonObject> tables = new LinkedHashMap<>();
        for (String relative : List.of(FISHERMAN, SORCERER)) {
            Path path = root.resolve(relative.replace('/', java.io.File.separatorChar)).normalize();
            assertTrue(path.startsWith(root));
            assertTrue(Files.isRegularFile(path), "missing private loot table: " + relative);
            try (Stream<Path> files = Files.walk(root)) {
                long matches = files.filter(Files::isRegularFile)
                        .map(candidate -> root.relativize(candidate).toString().replace('\\', '/'))
                        .filter(relative::equals)
                        .count();
                assertEquals(1, matches, "missing or duplicated private loot table: " + relative);
            }
            tables.put(relative, JsonParser.parseString(Files.readString(path)).getAsJsonObject());
        }
        return tables;
    }

    private static Map<String, JsonObject> syntheticFixtures() {
        Map<String, JsonObject> tables = new LinkedHashMap<>();
        tables.put(FISHERMAN, syntheticTable(4, 4, 15, false));
        tables.put(SORCERER, syntheticTable(6, 2, 1, true));
        return tables;
    }

    private static JsonObject syntheticTable(int firstPoolEntries, int secondPoolEntries,
                                             int emptyWeight, boolean potion) {
        JsonObject table = JsonParser.parseString("""
                {
                  "type": "minecraft:chest",
                  "pools": [
                    {"rolls": 1.0, "bonus_rolls": 0.0, "entries": []},
                    {"rolls": 1.0, "bonus_rolls": 0.0, "entries": []}
                  ]
                }
                """).getAsJsonObject();
        JsonArray first = table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        for (int i = 0; i < firstPoolEntries; i++) {
            first.add(JsonParser.parseString("""
                    {"type":"minecraft:item","weight":5,"name":"minecraft:stone"}
                    """));
        }
        JsonArray second = table.getAsJsonArray("pools").get(1).getAsJsonObject().getAsJsonArray("entries");
        JsonObject empty = new JsonObject();
        empty.addProperty("type", "minecraft:empty");
        empty.addProperty("weight", emptyWeight);
        second.add(empty);
        if (potion) {
            second.add(JsonParser.parseString("""
                    {
                      "type":"minecraft:item",
                      "weight":1,
                      "functions":[{"function":"minecraft:set_potion","id":"minecraft:strong_leaping"}],
                      "name":"minecraft:potion"
                    }
                    """));
        } else {
            while (second.size() < secondPoolEntries) {
                second.add(JsonParser.parseString("""
                        {"type":"minecraft:item","weight":1,"name":"minecraft:fishing_rod"}
                        """));
            }
        }
        return table;
    }
}

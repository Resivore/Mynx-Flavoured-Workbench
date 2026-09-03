package com.yungnickyoung.minecraft.ribbits.world.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.module.LootFunctionModule;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RibbitVillageExplorerMapContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));
    private static RegistryOps<JsonElement> registryOps;

    @BeforeAll
    static void bootstrapMinecraftRegistries() throws ReflectiveOperationException {
        SharedConstants.tryDetectVersion();

        // Production Fabric initialization runs while built-in registries accept mod entries.
        // This isolated test JVM reproduces that phase before freezing the registry, matching the
        // established private-loot codec test bootstrap rather than reflectively unfreezing it.
        Field bootstrapped = Bootstrap.class.getDeclaredField("isBootstrapped");
        bootstrapped.setAccessible(true);
        bootstrapped.setBoolean(null, true);
        invokeBuiltInRegistryPhase("createContents");
        LootFunctionModule.init();
        invokeBuiltInRegistryPhase("freeze");
        RegistryAccess builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        HolderLookup.Provider vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries()
                                .filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())
                )
        );
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
        registryOps = RegistryOps.create(JsonOps.INSTANCE, registries);
    }

    private static void invokeBuiltInRegistryPhase(String methodName)
            throws ReflectiveOperationException {
        Method method = BuiltInRegistries.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(null);
    }

    @Test
    void structureTagContainsOnlyTheCanonicalRibbitVillage() throws IOException {
        JsonObject tag = readObject(
                "common/src/main/resources/data/ribbits/tags/worldgen/structure/"
                        + "on_ribbit_village_explorer_maps.json");
        assertFalse(tag.get("replace").getAsBoolean());
        JsonArray values = tag.getAsJsonArray("values");
        assertEquals(1, values.size());
        assertEquals("ribbits:ribbit_village", values.get(0).getAsString());
    }

    @Test
    void sharedModifierUsesTheExactSynchronousSearchAndMapIdSuccessGate() throws IOException {
        JsonArray modifier = readArray(
                "common/src/main/resources/data/ribbits/item_modifier/"
                        + "ribbit_village_explorer_result.json");
        assertEquals(2, modifier.size());

        JsonObject search = modifier.get(0).getAsJsonObject();
        assertEquals("minecraft:exploration_map", search.get("function").getAsString());
        // TagKey.codec uses the bare tag ID in data JSON. The equivalent command/documentation
        // notation includes the leading '#': #ribbits:on_ribbit_village_explorer_maps.
        assertEquals("ribbits:on_ribbit_village_explorer_maps",
                search.get("destination").getAsString());
        assertEquals("minecraft:village_plains", search.get("decoration").getAsString());
        assertEquals(100, search.get("search_radius").getAsInt());
        assertEquals(2, search.get("zoom").getAsInt());
        assertFalse(search.get("skip_existing_chunks").getAsBoolean());

        JsonObject finalizerJson = modifier.get(1).getAsJsonObject();
        assertEquals("ribbits:finalize_ribbit_village_explorer_result",
                finalizerJson.get("function").getAsString());
        assertSame(LootFunctionModule.RIBBIT_VILLAGE_EXPLORER_RESULT,
                BuiltInRegistries.LOOT_FUNCTION_TYPE.getValue(
                        RibbitsCommon.id(LootFunctionModule.RIBBIT_VILLAGE_EXPLORER_RESULT_ID)));

        LootItemFunctions.ROOT_CODEC.parse(registryOps, modifier).getOrThrow();
        LootItemFunction finalizer = LootItemFunctions.TYPED_CODEC
                .parse(registryOps, finalizerJson).getOrThrow();
        JsonElement encoded = LootItemFunctions.TYPED_CODEC
                .encodeStart(registryOps, finalizer).getOrThrow();
        assertEquals(finalizerJson, encoded, "custom finalizer codec round-trip");
    }

    @Test
    void successAndEveryMissProduceExactlyOneStableVisibleResult() throws IOException {
        JsonArray modifier = readArray(
                "common/src/main/resources/data/ribbits/item_modifier/"
                        + "ribbit_village_explorer_result.json");
        LootItemFunction finalizer = LootItemFunctions.TYPED_CODEC
                .parse(registryOps, modifier.get(1)).getOrThrow();

        ItemStack successfulMap = new ItemStack(Items.FILLED_MAP, 3);
        successfulMap.set(DataComponents.MAP_ID, new MapId(4));
        ItemStack success = finalizer.apply(successfulMap, null);
        assertSame(successfulMap, success, "the vanilla map and its saved-data identity are retained");
        assertEquals(1, success.getCount());
        assertTrue(success.is(Items.FILLED_MAP));
        assertEquals(new MapId(4), success.get(DataComponents.MAP_ID));
        assertEquals(Component.translatable(RibbitVillageExplorerMap.SUCCESS_NAME_KEY),
                success.get(DataComponents.CUSTOM_NAME));
        assertFalse(RibbitVillageExplorerMap.isFailedMap(success));

        ItemStack malformed = new ItemStack(Items.FILLED_MAP, 7);
        malformed.set(DataComponents.MAP_DECORATIONS,
                net.minecraft.world.item.component.MapDecorations.EMPTY);
        malformed.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
        malformed.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        malformed.set(DataComponents.CUSTOM_NAME,
                Component.translatable(RibbitVillageExplorerMap.SUCCESS_NAME_KEY));
        CompoundTag unrelatedData = new CompoundTag();
        unrelatedData.putBoolean("unrelated:stale", true);
        malformed.set(DataComponents.CUSTOM_DATA, CustomData.of(unrelatedData));
        ItemStack failure = finalizer.apply(malformed, null);
        assertNotSame(malformed, failure, "every non-success result is a fresh empty map");
        assertEquals(1, failure.getCount());
        assertTrue(failure.is(Items.MAP));
        assertFalse(failure.has(DataComponents.MAP_ID));
        assertFalse(failure.has(DataComponents.MAP_DECORATIONS));
        assertFalse(failure.has(DataComponents.MAP_COLOR));
        assertFalse(failure.has(DataComponents.MAP_POST_PROCESSING));
        assertTrue(DataComponents.MAP_POST_PROCESSING.isTransient(),
                "26.2 map_post_processing is network-only and cannot be removed by set_components");
        assertFalse(failure.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertEquals(RibbitVillageExplorerMap.failedMarker(),
                failure.get(DataComponents.CUSTOM_DATA),
                "failure finalization replaces stale custom data with the exact private marker");
        assertTrue(RibbitVillageExplorerMap.isFailedMap(failure));
        assertEquals(Component.translatable(RibbitVillageExplorerMap.FAILURE_NAME_KEY),
                failure.get(DataComponents.CUSTOM_NAME));
        assertEquals(new ItemLore(List.of(Component.translatable(
                        RibbitVillageExplorerMap.FAILURE_LORE_KEY))),
                failure.get(DataComponents.LORE));
        assertTrue(ItemStack.isSameItemSameComponents(
                RibbitVillageExplorerMap.createFailedMap(1), failure),
                "failure has exactly the canonical fresh-map component shape");

        // ExplorationMapFunction returns the controlled initial empty map unchanged when no
        // structure is found. The shared finalizer must turn that precise outcome into one
        // visible, marked failure rather than leaving the barrel empty or leaking a success name.
        ItemStack unchangedExplorationMiss = finalizer.apply(new ItemStack(Items.MAP), null);
        assertTrue(RibbitVillageExplorerMap.isFailedMap(unchangedExplorationMiss));
        assertEquals(1, unchangedExplorationMiss.getCount());
        assertEquals(Component.translatable(RibbitVillageExplorerMap.FAILURE_NAME_KEY),
                unchangedExplorationMiss.get(DataComponents.CUSTOM_NAME));

        ItemStack wrongItemWithId = new ItemStack(Items.MAP);
        wrongItemWithId.set(DataComponents.MAP_ID, new MapId(17));
        assertTrue(RibbitVillageExplorerMap.isFailedMap(
                finalizer.apply(wrongItemWithId, null)),
                "MAP_ID alone cannot turn an empty map into a success");
    }

    @Test
    void barrelLootTableHasOneResultEntryAndReferencesOnlyTheSharedModifier()
            throws IOException {
        JsonObject table = readObject(
                "common/src/main/resources/data/ribbits/loot_table/chests/swamp_hut_map.json");
        assertEquals("minecraft:chest", table.get("type").getAsString());
        JsonArray pools = table.getAsJsonArray("pools");
        assertEquals(1, pools.size());
        JsonObject pool = pools.get(0).getAsJsonObject();
        assertEquals(1, pool.get("rolls").getAsInt());
        JsonArray entries = pool.getAsJsonArray("entries");
        assertEquals(1, entries.size());
        JsonObject entry = entries.get(0).getAsJsonObject();
        assertEquals("minecraft:item", entry.get("type").getAsString());
        assertEquals("minecraft:map", entry.get("name").getAsString());
        JsonArray functions = entry.getAsJsonArray("functions");
        assertEquals(1, functions.size());
        JsonObject reference = functions.get(0).getAsJsonObject();
        assertEquals("minecraft:reference", reference.get("function").getAsString());
        assertEquals("ribbits:ribbit_village_explorer_result",
                reference.get("name").getAsString());
        LootTable.DIRECT_CODEC.parse(registryOps, table).getOrThrow();
    }

    @Test
    void failedMapAuthenticityIsPrivateMarkerBasedRatherThanDisplayText() {
        ItemStack genuine = RibbitVillageExplorerMap.createFailedMap(1);
        assertTrue(RibbitVillageExplorerMap.isFailedMap(genuine));
        genuine.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed but genuine"));
        assertTrue(RibbitVillageExplorerMap.isFailedMap(genuine));

        ItemStack forgedText = new ItemStack(Items.MAP);
        forgedText.set(DataComponents.CUSTOM_NAME,
                Component.translatable(RibbitVillageExplorerMap.FAILURE_NAME_KEY));
        forgedText.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable(RibbitVillageExplorerMap.FAILURE_LORE_KEY))));
        assertFalse(RibbitVillageExplorerMap.isFailedMap(forgedText));

        ItemStack staleMapId = RibbitVillageExplorerMap.createFailedMap(1);
        staleMapId.set(DataComponents.MAP_ID, new MapId(99));
        assertFalse(RibbitVillageExplorerMap.isFailedMap(staleMapId));
    }

    @Test
    void finalizerRegistrationAndImplementationRemainCommonSideOnly() throws IOException {
        String common = Files.readString(PROJECT_ROOT.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/RibbitsCommon.java"));
        String module = Files.readString(PROJECT_ROOT.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/LootFunctionModule.java"));
        String function = Files.readString(PROJECT_ROOT.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/world/loot/"
                        + "RibbitVillageExplorerResultFunction.java"));
        assertEquals(1, occurrences(common, "LootFunctionModule.init();"));
        assertEquals(1, occurrences(module, "Registry.register("));
        assertTrue(module.contains("BuiltInRegistries.LOOT_FUNCTION_TYPE"));
        assertTrue(function.contains("RibbitVillageExplorerMap.finalizeSearchResult(stack)"));
        assertFalse((common + module + function).contains("net.minecraft.client"));
    }

    private static JsonObject readObject(String relative) throws IOException {
        return JsonParser.parseString(Files.readString(PROJECT_ROOT.resolve(relative)))
                .getAsJsonObject();
    }

    private static JsonArray readArray(String relative) throws IOException {
        return JsonParser.parseString(Files.readString(PROJECT_ROOT.resolve(relative)))
                .getAsJsonArray();
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int from = 0;
        while ((from = value.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}

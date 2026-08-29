package dev.resivore.matchaheart;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootTable;

/** Loads this mod's packaged canonical JSON rather than the winning pack resource. */
public final class AuthoritativeData {
    private AuthoritativeData() {}

    public static JsonElement readJson(String resourcePath) {
        var container = FabricLoader.getInstance()
                .getModContainer(MatchaHeartDeathCompat.MOD_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing owning mod container: " + MatchaHeartDeathCompat.MOD_ID));
        var path = container.findPath(resourcePath)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing canonical packaged resource: " + resourcePath));
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read canonical packaged resource: " + resourcePath, exception);
        }
    }

    public static Recipe<?> decodeRecipe(String resourcePath, HolderLookup.Provider registries) {
        return Recipe.CODEC.parse(
                registries.createSerializationContext(JsonOps.INSTANCE), readJson(resourcePath)).getOrThrow();
    }

    public static Advancement decodeAdvancement(String resourcePath, HolderLookup.Provider registries) {
        return Advancement.CODEC.parse(
                registries.createSerializationContext(JsonOps.INSTANCE), readJson(resourcePath)).getOrThrow();
    }

    public static LootTable decodeLootTable(String resourcePath, HolderLookup.Provider registries) {
        return LootTable.DIRECT_CODEC.parse(
                registries.createSerializationContext(JsonOps.INSTANCE), readJson(resourcePath)).getOrThrow();
    }
}

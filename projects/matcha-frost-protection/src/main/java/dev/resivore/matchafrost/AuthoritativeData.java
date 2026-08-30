package dev.resivore.matchafrost;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.Recipe;

final class AuthoritativeData {
    private AuthoritativeData() {}

    static Recipe<?> decodeRecipe(String resourcePath, HolderLookup.Provider registries) {
        return Recipe.CODEC.parse(
                registries.createSerializationContext(JsonOps.INSTANCE),
                readJson(resourcePath)).getOrThrow();
    }

    private static JsonElement readJson(String resourcePath) {
        var container = FabricLoader.getInstance()
                .getModContainer(MatchaFrostProtection.MOD_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing owning mod container: " + MatchaFrostProtection.MOD_ID));
        var path = container.findPath(resourcePath)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing canonical packaged resource: " + resourcePath));
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read canonical packaged resource: " + resourcePath, exception);
        }
    }
}

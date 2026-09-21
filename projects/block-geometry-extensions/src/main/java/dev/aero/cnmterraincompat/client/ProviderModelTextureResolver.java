package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves textures for one known provider block through its real model/blockstate chain. */
public final class ProviderModelTextureResolver {
    private ProviderModelTextureResolver() {}

    public static Textures resolve(ResourceManager manager, Identifier providerBlock) {
        Map<String, String> variables = variables(manager, providerBlock, new LinkedHashSet<>());
        String side = first(variables, providerBlock, "side", "all", "texture", "particle");
        String top = first(variables, providerBlock, "top", "end", "all", "side", "particle");
        String bottom = first(variables, providerBlock, "bottom", "top", "end", "all", "side", "particle");
        if (side == null || top == null || bottom == null) {
            throw new IllegalStateException("Known provider block has no ordinary texture contract: "
                    + providerBlock + " " + variables);
        }
        return new Textures(side, top, bottom);
    }

    private static Map<String, String> variables(ResourceManager manager, Identifier model,
            Set<Identifier> visiting) {
        if (!visiting.add(model)) throw new IllegalStateException("Cyclic provider model parent: " + model);
        try {
            Resource resource = manager.getResource(modelResourceId(model)).orElse(null);
            if (resource == null) return blockStateVariables(manager, model, visiting);
            JsonObject root;
            try (var reader = resource.openAsReader()) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read provider model " + model, exception);
            }
            Map<String, String> result = new LinkedHashMap<>();
            if (root.has("parent")) {
                result.putAll(variables(manager, Identifier.parse(root.get("parent").getAsString()), visiting));
            }
            if (root.has("textures") && root.get("textures").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("textures").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        result.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            return result;
        } finally {
            visiting.remove(model);
        }
    }

    /** Follows blockstate-selected models when models/block/&lt;block-id&gt;.json is absent. */
    private static Map<String, String> blockStateVariables(ResourceManager manager, Identifier model,
            Set<Identifier> visiting) {
        if (model.getPath().startsWith("block/")) {
            throw new IllegalStateException("Missing provider model " + model);
        }
        Identifier blockState = Identifier.fromNamespaceAndPath(model.getNamespace(),
                "blockstates/" + model.getPath() + ".json");
        Resource resource = manager.getResource(blockState).orElseThrow(() ->
                new IllegalStateException("Missing provider model and blockstate for " + model));
        JsonObject root;
        try (var reader = resource.openAsReader()) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read provider blockstate " + blockState, exception);
        }
        List<Identifier> models = new ArrayList<>();
        collectBlockStateModels(root, models);
        if (models.isEmpty()) {
            throw new IllegalStateException("Provider blockstate has no model: " + blockState);
        }
        IllegalStateException failure = null;
        Map<String, String> firstVariables = null;
        for (Identifier visualModel : models) {
            try {
                Map<String, String> variables = variables(manager, visualModel, visiting);
                if (firstVariables == null) firstVariables = variables;
                if (hasOrdinaryTextureContract(variables)) return variables;
            } catch (IllegalStateException exception) {
                failure = exception;
            }
        }
        if (firstVariables != null) return firstVariables;
        throw new IllegalStateException("Cannot resolve provider visual model through " + blockState, failure);
    }

    private static void collectBlockStateModels(JsonElement value, List<Identifier> models) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            JsonElement model = object.get("model");
            if (model != null && model.isJsonPrimitive() && model.getAsJsonPrimitive().isString()) {
                models.add(Identifier.parse(model.getAsString()));
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (!entry.getKey().equals("model")) collectBlockStateModels(entry.getValue(), models);
            }
        } else if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) collectBlockStateModels(child, models);
        }
    }

    private static boolean hasOrdinaryTextureContract(Map<String, String> variables) {
        return firstValue(variables, "side", "all", "texture", "particle") != null
                && firstValue(variables, "top", "end", "all", "side", "particle") != null
                && firstValue(variables, "bottom", "top", "end", "all", "side", "particle") != null;
    }

    private static Identifier modelResourceId(Identifier model) {
        String path = model.getPath().startsWith("block/") ? "models/" + model.getPath()
                : "models/block/" + model.getPath();
        return Identifier.fromNamespaceAndPath(model.getNamespace(), path + ".json");
    }

    private static String first(Map<String, String> variables, Identifier canonical, String... names) {
        String value = firstValue(variables, names);
        if (value == null) return null;
        if (value.contains(":")) return value;
        return canonical.getNamespace() + ":" + (value.contains("/") ? value : "block/" + value);
    }

    private static String firstValue(Map<String, String> variables, String... names) {
        for (String name : names) {
            String value = resolveAlias(variables, variables.get(name));
            if (value != null) return value;
        }
        for (String value : variables.values()) {
            String resolved = resolveAlias(variables, value);
            if (resolved != null) return resolved;
        }
        return null;
    }

    private static String resolveAlias(Map<String, String> variables, String value) {
        int guard = 0;
        while (value != null && value.startsWith("#") && guard++ < 32) {
            value = variables.get(value.substring(1));
        }
        return value == null || value.startsWith("#") ? null : value;
    }

    public record Textures(String side, String top, String bottom) {}
}

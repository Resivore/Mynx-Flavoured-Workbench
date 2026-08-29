package com.crispytwig.naturalist.client.model.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads only the legacy override metadata needed by the 26.2 compatibility wrappers. */
@Environment(EnvType.CLIENT)
public final class LegacyItemModelResources {
    private static final Logger LOGGER = LogUtils.getLogger();

    private LegacyItemModelResources() {
    }

    public static Map<Identifier, ItemModelOverrides> scanOverrides(ResourceManager resourceManager,
                                                                     Collection<Identifier> itemIds) {
        Map<Identifier, ItemModelOverrides> result = new LinkedHashMap<>();
        for (Identifier itemId : itemIds) {
            Identifier resourceId = Identifier.fromNamespaceAndPath(itemId.getNamespace(),
                    "models/item/" + itemId.getPath() + ".json");
            Resource resource = resourceManager.getResource(resourceId).orElse(null);
            if (resource == null) {
                LOGGER.warn("Missing legacy item model {} while preparing Naturalist item properties", resourceId);
                result.put(itemId, ItemModelOverrides.EMPTY);
                continue;
            }
            try (Reader reader = resource.openAsReader()) {
                result.put(itemId, parseOverrides(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Unable to read legacy item overrides from {}", resourceId, exception);
                result.put(itemId, ItemModelOverrides.EMPTY);
            }
        }
        return Map.copyOf(result);
    }

    public static ItemModelOverrides parseOverrides(JsonObject root) {
        if (!root.has("overrides") || !root.get("overrides").isJsonArray()) {
            return ItemModelOverrides.EMPTY;
        }
        List<ItemModelOverrides.Entry> entries = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("overrides")) {
            JsonObject object = element.getAsJsonObject();
            Identifier model = Identifier.parse(object.get("model").getAsString());
            Map<Identifier, Float> predicates = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> predicate : object.getAsJsonObject("predicate").entrySet()) {
                predicates.put(Identifier.parse(predicate.getKey()), predicate.getValue().getAsFloat());
            }
            entries.add(new ItemModelOverrides.Entry(model, predicates));
        }
        return new ItemModelOverrides(entries);
    }
}

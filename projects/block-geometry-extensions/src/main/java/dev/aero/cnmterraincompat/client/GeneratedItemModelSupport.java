package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/** Shared item-definition projection, including exact canonical tint-source inheritance. */
final class GeneratedItemModelSupport {
    private GeneratedItemModelSupport() {}

    static JsonObject itemDefinition(ResourceManager manager,
            NibaruMaterialProfile profile, String itemModel) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(profile, "profile");
        JsonObject root = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", itemModel);
        parentItemTints(manager, profile).ifPresent(tints -> model.add("tints", tints));
        root.add("model", model);
        return root;
    }

    private static Optional<JsonArray> parentItemTints(ResourceManager manager,
            NibaruMaterialProfile profile) {
        Identifier parent = profile.canonicalParentId();
        Identifier resourceId = Identifier.fromNamespaceAndPath(parent.getNamespace(),
                "items/" + parent.getPath() + ".json");
        Resource resource = manager.getResource(resourceId).orElse(null);
        if (resource == null) {
            if (profile.tintProfile() != TintProfile.NONE) {
                throw new IllegalStateException("Missing canonical tinted item definition: " + resourceId);
            }
            return Optional.empty();
        }
        try (var reader = resource.openAsReader()) {
            JsonElement parsed = JsonParser.parseReader(reader);
            JsonObject root = parsed.getAsJsonObject();
            JsonObject model = root.has("model") && root.get("model").isJsonObject()
                    ? root.getAsJsonObject("model") : null;
            if (model != null && model.has("tints") && model.get("tints").isJsonArray()) {
                return Optional.of(model.getAsJsonArray("tints").deepCopy());
            }
            if (profile.tintProfile() != TintProfile.NONE) {
                throw new IllegalStateException("Canonical tinted item has no tint sources: " + resourceId);
            }
            return Optional.empty();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot read canonical item definition: " + resourceId, exception);
        }
    }
}

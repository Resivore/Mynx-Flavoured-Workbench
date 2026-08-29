package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.LayerGeneratedData;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Injects BGE-owned Layer client assets into CNM's existing ordinary resource pack. */
public final class LayerGeneratedResources {
    private LayerGeneratedResources() {}

    /** Generates one deterministic resource set after CNM has finished its normal asset pass. */
    public static GenerationSummary generate(ResourceManager manager) {
        Objects.requireNonNull(manager, "manager");
        List<LayerGeneratedData.Binding> bindings = LayerGeneratedData.bindings();
        JsonObject language = new JsonObject();
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".layers", "Layers");

        int modelCount = 0;
        int selectorCount = 0;
        for (LayerGeneratedData.Binding binding : bindings) {
            boolean canonicalFull = canonicalFullModelReusable(manager, binding.profile());
            LayerModelProjection.Projection projection = LayerModelProjection.project(
                    binding.profile(), binding.id(), canonicalFull);
            writeClient(blockStateResource(binding.id()), projection.blockState());
            for (Map.Entry<String, JsonObject> model : projection.models().entrySet()) {
                writeClient(modelResource(model.getKey()), model.getValue());
            }
            writeClient(itemResource(binding.id()), itemDefinition(manager, binding, projection.itemModel()));
            language.addProperty(translationKey(binding.id()), displayName(binding.profile()));
            modelCount += projection.models().size();
            selectorCount += projection.blockState().getAsJsonObject("variants").size();
        }

        writeClient(languageResource(), language);
        return new GenerationSummary(bindings.size(), modelCount, selectorCount);
    }

    private static JsonObject itemDefinition(ResourceManager manager,
            LayerGeneratedData.Binding binding, String itemModel) {
        JsonObject root = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", itemModel);
        parentItemTints(manager, binding.profile()).ifPresent(tints -> model.add("tints", tints));
        root.add("model", model);
        return root;
    }

    private static java.util.Optional<JsonArray> parentItemTints(ResourceManager manager,
            NibaruMaterialProfile profile) {
        Identifier parent = profile.canonicalParentId();
        Identifier resourceId = Identifier.fromNamespaceAndPath(parent.getNamespace(),
                "items/" + parent.getPath() + ".json");
        Resource resource = manager.getResource(resourceId).orElse(null);
        if (resource == null) {
            if (profile.tintProfile() != TintProfile.NONE) {
                throw new IllegalStateException("Missing canonical tinted item definition: " + resourceId);
            }
            return java.util.Optional.empty();
        }
        try (var reader = resource.openAsReader()) {
            JsonElement parsed = JsonParser.parseReader(reader);
            JsonObject root = parsed.getAsJsonObject();
            JsonObject model = root.has("model") && root.get("model").isJsonObject()
                    ? root.getAsJsonObject("model") : null;
            if (model != null && model.has("tints") && model.get("tints").isJsonArray()) {
                return java.util.Optional.of(model.getAsJsonArray("tints").deepCopy());
            }
            if (profile.tintProfile() != TintProfile.NONE) {
                throw new IllegalStateException("Canonical tinted item has no tint sources: " + resourceId);
            }
            return java.util.Optional.empty();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot read canonical item definition: " + resourceId, exception);
        }
    }

    private static boolean canonicalFullModelReusable(ResourceManager manager,
            NibaruMaterialProfile profile) {
        if (profile.orientationPolicy() != NibaruMaterialProfile.OrientationPolicy.UNIFORM
                || profile.insetVisualContract().isPresent()) return false;
        Identifier parent = profile.canonicalParentId();
        Identifier model = Identifier.fromNamespaceAndPath(parent.getNamespace(),
                "models/block/" + parent.getPath() + ".json");
        return manager.getResource(model).isPresent();
    }

    private static void writeClient(Identifier id, JsonElement json) {
        ClutterNoMore.RESOURCES.addJson(PackType.CLIENT_RESOURCES, id, json);
    }

    private static Identifier blockStateResource(Identifier layer) {
        return Identifier.fromNamespaceAndPath(layer.getNamespace(),
                "blockstates/" + layer.getPath() + ".json");
    }

    private static Identifier itemResource(Identifier layer) {
        return Identifier.fromNamespaceAndPath(layer.getNamespace(),
                "items/" + layer.getPath() + ".json");
    }

    private static Identifier languageResource() {
        // Keep generated Layer strings separate from the module's authored en_us.json so the
        // runtime pack cannot shadow accepted Dirt/Grass Slab and Vertical Slab translations.
        return Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID + "_generated",
                "lang/en_us.json");
    }

    private static Identifier modelResource(String modelId) {
        Identifier model = Identifier.parse(modelId);
        return Identifier.fromNamespaceAndPath(model.getNamespace(),
                "models/" + model.getPath() + ".json");
    }

    private static String translationKey(Identifier layer) {
        return "block." + layer.getNamespace() + "." + layer.getPath().replace('/', '.');
    }

    /** A normal material-first BGE name, without the collision-avoidance namespace path. */
    public static String displayName(NibaruMaterialProfile profile) {
        return AssetGenerator.langName(profile.canonicalParentId().getPath() + "_layer");
    }

    public record GenerationSummary(int familyCount, int modelCount, int selectorCount) {}
}

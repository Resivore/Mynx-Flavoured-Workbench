package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.BgeGeometryRole;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.LayerGeneratedData;
import dev.aero.cnmterraincompat.QuarterGeometryGeneratedData;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Injects BGE-owned Corner and Quarter Column assets into CNM's runtime resource pack. */
public final class QuarterGeometryGeneratedResources {
    private QuarterGeometryGeneratedResources() {}

    /** Generates deterministic resources after all BGE geometry blocks have been registered. */
    public static GenerationSummary generate(ResourceManager manager) {
        Objects.requireNonNull(manager, "manager");
        List<QuarterGeometryGeneratedData.Binding> corners =
                QuarterGeometryGeneratedData.bindings(BgeGeometryRole.CORNER);
        List<QuarterGeometryGeneratedData.Binding> columns =
                QuarterGeometryGeneratedData.bindings(BgeGeometryRole.QUARTER_COLUMN);

        int modelCount = 0;
        int selectorCount = 0;
        for (QuarterGeometryGeneratedData.Binding binding : corners) {
            CornerColumnModelProjection.Projection projection =
                    CornerColumnModelProjection.projectCorner(
                            manager, binding.profile(), binding.id());
            writeProjection(manager, binding, projection);
            modelCount += projection.models().size();
            selectorCount += projection.blockState().getAsJsonObject("variants").size();
        }
        for (QuarterGeometryGeneratedData.Binding binding : columns) {
            CornerColumnModelProjection.Projection projection =
                    CornerColumnModelProjection.projectColumn(
                            manager, binding.profile(), binding.id());
            writeProjection(manager, binding, projection);
            modelCount += projection.models().size();
            selectorCount += projection.blockState().getAsJsonObject("variants").size();
        }

        // This write intentionally follows Layer generation and replaces that language resource with
        // a superset; DynamicResourcePack keeps one JSON value per exact resource identifier.
        writeClient(languageResource(), combinedLanguage(corners, columns));
        return new GenerationSummary(corners.size(), columns.size(), modelCount, selectorCount);
    }

    private static void writeProjection(ResourceManager manager,
            QuarterGeometryGeneratedData.Binding binding,
            CornerColumnModelProjection.Projection projection) {
        writeClient(blockStateResource(binding.id()), projection.blockState());
        for (Map.Entry<String, JsonObject> model : projection.models().entrySet()) {
            writeClient(modelResource(model.getKey()), model.getValue());
        }
        writeClient(itemResource(binding.id()), GeneratedItemModelSupport.itemDefinition(
                manager, binding.profile(), projection.itemModel()));
    }

    private static JsonObject combinedLanguage(List<QuarterGeometryGeneratedData.Binding> corners,
            List<QuarterGeometryGeneratedData.Binding> columns) {
        JsonObject language = new JsonObject();
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".layers", "Layers");
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".corners", "Corners");
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".quarter_columns",
                "Quarter Columns");
        for (LayerGeneratedData.Binding layer : LayerGeneratedData.bindings()) {
            language.addProperty(translationKey(layer.id()),
                    LayerGeneratedResources.displayName(layer.profile()));
        }
        for (QuarterGeometryGeneratedData.Binding corner : corners) {
            language.addProperty(translationKey(corner.id()), cornerDisplayName(corner.profile()));
        }
        for (QuarterGeometryGeneratedData.Binding column : columns) {
            language.addProperty(translationKey(column.id()), quarterColumnDisplayName(column.profile()));
        }
        return language;
    }

    private static void writeClient(Identifier id, JsonElement json) {
        ClutterNoMore.RESOURCES.addJson(PackType.CLIENT_RESOURCES, id, json);
    }

    private static Identifier blockStateResource(Identifier shape) {
        return Identifier.fromNamespaceAndPath(shape.getNamespace(),
                "blockstates/" + shape.getPath() + ".json");
    }

    private static Identifier itemResource(Identifier shape) {
        return Identifier.fromNamespaceAndPath(shape.getNamespace(),
                "items/" + shape.getPath() + ".json");
    }

    private static Identifier languageResource() {
        return Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID + "_generated",
                "lang/en_us.json");
    }

    private static Identifier modelResource(String modelId) {
        Identifier model = Identifier.parse(modelId);
        return Identifier.fromNamespaceAndPath(model.getNamespace(),
                "models/" + model.getPath() + ".json");
    }

    private static String translationKey(Identifier shape) {
        return "block." + shape.getNamespace() + "." + shape.getPath().replace('/', '.');
    }

    public static String cornerDisplayName(NibaruMaterialProfile profile) {
        return AssetGenerator.langName(profile.canonicalParentId().getPath() + "_corner");
    }

    public static String quarterColumnDisplayName(NibaruMaterialProfile profile) {
        return AssetGenerator.langName(profile.canonicalParentId().getPath() + "_quarter_column");
    }

    public record GenerationSummary(int cornerFamilyCount, int columnFamilyCount,
            int modelCount, int selectorCount) {}
}

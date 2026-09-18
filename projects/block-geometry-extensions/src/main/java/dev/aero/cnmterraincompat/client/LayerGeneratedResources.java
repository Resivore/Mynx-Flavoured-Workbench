package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.LayerGeneratedData;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.HugeMushroomBlock;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Injects BGE-owned Layer client assets into CNM's existing ordinary resource pack. */
public final class LayerGeneratedResources {
    private LayerGeneratedResources() {}

    /** Generates one deterministic resource set after CNM has finished its normal asset pass. */
    public static GenerationSummary generate(ResourceManager manager) {
        return generate(manager, true);
    }

    static GenerationSummary generate(ResourceManager manager, boolean writeLanguage) {
        Objects.requireNonNull(manager, "manager");
        return generateBindings(manager, writeLanguage, LayerGeneratedData.bindings());
    }

    /** Exercises the same runtime writer for only the late optional-provider profiles. */
    public static GenerationSummary generateExternalForValidation(ResourceManager manager) {
        Objects.requireNonNull(manager, "manager");
        return generateBindings(manager, false, LayerGeneratedData.bindings().stream()
                .filter(binding -> isLateExternal(binding.profile())).toList());
    }

    private static boolean isLateExternal(NibaruMaterialProfile profile) {
        return profile.family() == null && !profile.canonicalParentId().getNamespace().equals("minecraft");
    }

    private static GenerationSummary generateBindings(ResourceManager manager, boolean writeLanguage,
            List<LayerGeneratedData.Binding> bindings) {
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
            writeClient(itemResource(binding.id()), GeneratedItemModelSupport.itemDefinition(
                    manager, binding.profile(), projection.itemModel()));
            language.addProperty(translationKey(binding.id()), displayName(binding.profile()));
            modelCount += projection.models().size();
            selectorCount += projection.blockState().getAsJsonObject("variants").size();
        }

        if (writeLanguage) writeClient(languageResource(), language);
        return new GenerationSummary(bindings.size(), modelCount, selectorCount);
    }

    private static boolean canonicalFullModelReusable(ResourceManager manager,
            NibaruMaterialProfile profile) {
        if (profile.orientationPolicy() != NibaruMaterialProfile.OrientationPolicy.UNIFORM
                || profile.insetVisualContract().isPresent()) return false;
        // A uniformly skinned BGE family can still be rooted at a provider HugeMushroomBlock.
        // Its provider model owns directional/interior semantics that do not belong to the
        // legacy full Layer state, so keep the BGE-generated uniformly textured full cube.
        if (profile.visualProfile() == VisualProfile.UNIFORM
                && profile.canonicalParent() instanceof HugeMushroomBlock) return false;
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

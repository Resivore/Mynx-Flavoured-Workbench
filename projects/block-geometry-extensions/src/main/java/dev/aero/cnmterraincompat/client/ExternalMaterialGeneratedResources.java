package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.AxisModelContract;
import dev.aero.cnmterraincompat.BgeGeometryRole;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.ExternalMaterialFamilies;
import dev.aero.cnmterraincompat.LayerGeneratedData;
import dev.aero.cnmterraincompat.QuarterGeometryGeneratedData;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Complete client resource projection for the five late forms CNM could not have scanned. */
public final class ExternalMaterialGeneratedResources {
    private ExternalMaterialGeneratedResources() {}

    public static GenerationSummary generate(ResourceManager manager) {
        int blockStates = 0;
        int models = 0;
        int items = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            NibaruMaterialProfile profile = binding.profile();
            Identifier slab = id(binding.slab());
            Identifier stairs = id(binding.stairs());
            Identifier wall = id(binding.wall());
            Identifier vertical = id(binding.verticalSlab());
            Identifier step = id(binding.step());
            String verticalItemModel = model(vertical);
            String stepItemModel = model(step);

            if (binding.isGeneratedRole("slab")) {
                if (profile.orientationPolicy() == NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED) {
                    NativeAxisModelContract.AxisUvPolicy nativePolicy = NativeAxisModelContract.AxisUvPolicy.valueOf(
                            AxisGeneratedResources.policy(manager, profile.canonicalParentId()).name());
                    NativeAxisModelContract.GeneratedBlockResources resources =
                            NativeAxisModelContract.slab(profile, nativePolicy);
                    write(blockState(slab), resources.blockState());
                    models += writeModels(resources.models());
                } else models += writeSlab(profile, slab);
                blockStates++;
            }
            if (binding.isGeneratedRole("stairs")) {
                if (profile.orientationPolicy() == NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED) {
                    NativeAxisModelContract.AxisUvPolicy nativePolicy = NativeAxisModelContract.AxisUvPolicy.valueOf(
                            AxisGeneratedResources.policy(manager, profile.canonicalParentId()).name());
                    NativeAxisModelContract.GeneratedBlockResources resources =
                            NativeAxisModelContract.stairs(profile, nativePolicy);
                    write(blockState(stairs), resources.blockState());
                    models += writeModels(resources.models());
                } else models += writeStairs(manager, profile, stairs);
                blockStates++;
            }
            if (binding.isGeneratedRole("wall")) {
                // Wall state remains the normal post/low/tall multipart contract. Directional
                // material faces are expressed by the model templates, never an AXIS property.
                models += writeWall(manager, profile, wall);
                blockStates++;
            }

            if (profile.orientationPolicy() == NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED) {
                // CNM's normal Vertical Slab and Step templates are the established item-preview
                // contract: their display transforms centre the Vertical Slab and present the Step
                // horizontally. AxisModelContract only produces placed-state signature models.
                // Keep that state-model collection out of the item-preview decision.
                models += writeAxisItemPreviewModels(profile, vertical, step);
                AxisModelContract.AxisUvPolicy policy = AxisGeneratedResources.policy(
                        manager, profile.canonicalParentId());
                Map<String, JsonObject> verticalModels =
                        AxisModelContract.verticalGeneratedModels(profile, vertical, policy);
                verticalModels.forEach((modelId, model) -> write(modelResource(modelId), model));
                write(blockState(vertical), AxisModelContract.verticalBlockState(vertical, policy));
                models += verticalModels.size();
                Map<String, JsonObject> stepModels =
                        AxisModelContract.stepGeneratedModels(profile, step, policy);
                stepModels.forEach((modelId, model) -> write(modelResource(modelId), model));
                write(blockState(step), AxisModelContract.stepBlockState(step, policy));
                models += stepModels.size();
            } else {
                models += writeVertical(profile, vertical);
                models += writeStep(profile, step);
            }
            blockStates += 2;

            if (binding.isGeneratedRole("slab")) {
                write(item(slab), GeneratedItemModelSupport.itemDefinition(manager, profile, model(slab)));
                items++;
            }
            if (binding.isGeneratedRole("stairs")) {
                write(item(stairs), GeneratedItemModelSupport.itemDefinition(manager, profile, model(stairs)));
                items++;
            }
            if (binding.isGeneratedRole("wall")) {
                write(item(wall), GeneratedItemModelSupport.itemDefinition(manager, profile, model(wall) + "_inventory"));
                items++;
            }
            write(item(vertical), GeneratedItemModelSupport.itemDefinition(manager, profile, verticalItemModel));
            write(item(step), GeneratedItemModelSupport.itemDefinition(manager, profile, stepItemModel));
            items += 2;
        }
        write(Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID + "_generated", "lang/en_us.json"),
                combinedLanguage());
        return new GenerationSummary(ExternalMaterialFamilies.all().size(), blockStates, models, items);
    }

    private static int writeSlab(NibaruMaterialProfile profile, Identifier id) {
        JsonObject variants = new JsonObject();
        variants.add("type=bottom", selection(model(id)));
        variants.add("type=top", selection(model(id) + "_top"));
        variants.add("type=double", selection(model(id) + "_double"));
        write(blockState(id), variants(variants));
        if (profile.tintProfile() == TintProfile.NONE) {
            write(modelResource(id), template("minecraft:block/slab", profile));
            write(modelResource(id, "_top"), template("minecraft:block/slab_top", profile));
            write(modelResource(id, "_double"), template("minecraft:block/cube_bottom_top", profile));
        } else {
            write(modelResource(id), cuboidModel(profile, List.of(new int[] {0, 0, 0, 16, 8, 16})));
            write(modelResource(id, "_top"), cuboidModel(profile, List.of(new int[] {0, 8, 0, 16, 16, 16})));
            write(modelResource(id, "_double"), cuboidModel(profile, List.of(new int[] {0, 0, 0, 16, 16, 16})));
        }
        return 3;
    }

    private static int writeStairs(ResourceManager manager, NibaruMaterialProfile profile, Identifier id) {
        JsonObject state = templateBlockState(manager,
                Identifier.fromNamespaceAndPath("minecraft", "blockstates/oak_stairs.json"),
                "minecraft:block/oak_stairs", model(id));
        write(blockState(id), state);
        if (profile.tintProfile() == TintProfile.NONE) {
            write(modelResource(id), template("minecraft:block/stairs", profile));
            write(modelResource(id, "_inner"), template("minecraft:block/inner_stairs", profile));
            write(modelResource(id, "_outer"), template("minecraft:block/outer_stairs", profile));
        } else {
            write(modelResource(id), cuboidModel(profile, List.of(
                    new int[] {0, 0, 0, 16, 8, 16}, new int[] {0, 8, 8, 16, 16, 16})));
            write(modelResource(id, "_inner"), cuboidModel(profile, List.of(
                    new int[] {0, 0, 0, 16, 8, 16}, new int[] {0, 8, 8, 16, 16, 16},
                    new int[] {8, 8, 0, 16, 16, 8})));
            write(modelResource(id, "_outer"), cuboidModel(profile, List.of(
                    new int[] {0, 0, 0, 16, 8, 16}, new int[] {8, 8, 8, 16, 16, 16})));
        }
        return 3;
    }

    private static int writeWall(ResourceManager manager, NibaruMaterialProfile profile, Identifier id) {
        JsonObject state = templateBlockState(manager,
                Identifier.fromNamespaceAndPath("minecraft", "blockstates/cobblestone_wall.json"),
                "minecraft:block/cobblestone_wall", model(id));
        write(blockState(id), state);
        if (profile.visualProfile() == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.PILLAR) {
            write(modelResource(id, "_post"), columnWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_column_wall_post", profile));
            write(modelResource(id, "_side"), columnWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_column_wall_side", profile));
            write(modelResource(id, "_side_tall"), columnWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_column_wall_side_tall", profile));
            write(modelResource(id, "_inventory"), columnWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_column_wall_inventory", profile));
        } else if (profile.visualProfile()
                == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.LEAVES_CUTOUT_TINTED) {
            write(modelResource(id, "_post"), leafWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_post", profile));
            write(modelResource(id, "_side"), leafWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_side", profile));
            write(modelResource(id, "_side_tall"), leafWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_side_tall", profile));
            write(modelResource(id, "_inventory"), leafWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_inventory", profile));
        } else if (profile.tintProfile() == TintProfile.NONE) {
            write(modelResource(id, "_post"), wallTemplate("minecraft:block/template_wall_post", profile));
            write(modelResource(id, "_side"), wallTemplate("minecraft:block/template_wall_side", profile));
            write(modelResource(id, "_side_tall"), wallTemplate("minecraft:block/template_wall_side_tall", profile));
            write(modelResource(id, "_inventory"), wallTemplate("minecraft:block/wall_inventory", profile));
        } else {
            write(modelResource(id, "_post"), cuboidModel(profile, List.of(new int[] {4, 0, 4, 12, 16, 12})));
            write(modelResource(id, "_side"), cuboidModel(profile, List.of(new int[] {5, 0, 0, 11, 14, 11})));
            write(modelResource(id, "_side_tall"), cuboidModel(profile, List.of(new int[] {5, 0, 0, 11, 16, 11})));
            write(modelResource(id, "_inventory"), cuboidModel(profile, List.of(
                    new int[] {4, 0, 4, 12, 16, 12}, new int[] {0, 0, 5, 16, 14, 11},
                    new int[] {5, 0, 0, 11, 14, 16})));
        }
        // Vanilla's cobblestone blockstate names its multipart models _post/_side/_side_tall.
        return 4;
    }

    private static int writeVertical(NibaruMaterialProfile profile, Identifier id) {
        JsonObject variants = new JsonObject();
        int rotation = 0;
        for (String facing : List.of("north", "east", "south", "west")) {
            variants.add("facing=" + facing + ",double=false", selection(model(id), 0, rotation, true));
            variants.add("facing=" + facing + ",double=true", selection(model(id) + "_double", 0, 0, true));
            rotation += 90;
        }
        write(blockState(id), variants(variants));
        String tint = profile.tintProfile() == TintProfile.NONE ? "" : "_tinted";
        write(modelResource(id), template("clutternomore:block/templates/vertical_slab" + tint, profile));
        write(modelResource(id, "_double"), template("clutternomore:block/templates/vertical_slab_double" + tint, profile));
        return 2;
    }

    private static int writeStep(NibaruMaterialProfile profile, Identifier id) {
        JsonObject variants = new JsonObject();
        int rotation = 0;
        for (String facing : List.of("north", "east", "south", "west")) {
            variants.add("facing=" + facing + ",type=bottom", selection(model(id), 0, rotation, true));
            variants.add("facing=" + facing + ",type=top", selection(model(id) + "_top", 0, rotation, true));
            variants.add("facing=" + facing + ",type=double", selection(model(id) + "_double", 0, rotation, true));
            rotation += 90;
        }
        write(blockState(id), variants(variants));
        String tint = profile.tintProfile() == TintProfile.NONE ? "" : "_tinted";
        write(modelResource(id), template("clutternomore:block/templates/step" + tint, profile));
        write(modelResource(id, "_top"), template("clutternomore:block/templates/step_top" + tint, profile));
        write(modelResource(id, "_double"), template("clutternomore:block/templates/step_double" + tint, profile));
        return 3;
    }

    /**
     * Writes only the normal CNM preview models for an axis-aligned late family. These models
     * intentionally remain absent from the axis-aware placed-state selectors.
     */
    private static int writeAxisItemPreviewModels(
            NibaruMaterialProfile profile, Identifier vertical, Identifier step) {
        String tint = profile.tintProfile() == TintProfile.NONE ? "" : "_tinted";
        write(modelResource(vertical), template("clutternomore:block/templates/vertical_slab" + tint, profile));
        write(modelResource(step), template("clutternomore:block/templates/step" + tint, profile));
        return 2;
    }

    private static JsonObject template(String parent, NibaruMaterialProfile profile) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        result.add("textures", textures(profile));
        return result;
    }

    private static JsonObject wallTemplate(String parent, NibaruMaterialProfile profile) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        JsonObject textures = new JsonObject();
        textures.addProperty("wall", texture(profile.textureRoles().side()));
        result.add("textures", textures);
        return result;
    }

    /** Accepted Oak Log wall route: bark on vertical faces and end grain on horizontal faces. */
    private static JsonObject columnWallTemplate(String parent, NibaruMaterialProfile profile) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        result.add("textures", textures(profile));
        return result;
    }

    /** Accepted normal leaf-wall geometry; tint registration remains profile-controlled. */
    private static JsonObject leafWallTemplate(String parent, NibaruMaterialProfile profile) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        result.addProperty("render_type", "cutout_mipped");
        JsonObject textures = new JsonObject();
        textures.addProperty("wall", texture(profile.textureRoles().side()));
        result.add("textures", textures);
        return result;
    }

    /** Complete tinted geometry for the standard forms whose vanilla parents have no tint index. */
    private static JsonObject cuboidModel(NibaruMaterialProfile profile, List<int[]> cuboids) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:block/block");
        root.add("textures", textures(profile));
        JsonArray elements = new JsonArray();
        for (int[] bounds : cuboids) {
            JsonObject element = new JsonObject();
            element.add("from", numbers(bounds[0], bounds[1], bounds[2]));
            element.add("to", numbers(bounds[3], bounds[4], bounds[5]));
            JsonObject faces = new JsonObject();
            for (Direction face : Direction.values()) {
                JsonObject encoded = new JsonObject();
                encoded.addProperty("texture", switch (face) {
                    case UP -> "#top";
                    case DOWN -> "#bottom";
                    default -> "#side";
                });
                encoded.addProperty("tintindex", 0);
                faces.add(face.getSerializedName(), encoded);
            }
            element.add("faces", faces);
            elements.add(element);
        }
        root.add("elements", elements);
        return root;
    }

    private static JsonObject textures(NibaruMaterialProfile profile) {
        JsonObject textures = new JsonObject();
        textures.addProperty("side", texture(profile.textureRoles().side()));
        textures.addProperty("top", texture(profile.textureRoles().top()));
        textures.addProperty("bottom", texture(profile.textureRoles().bottom()));
        textures.addProperty("particle", texture(profile.textureRoles().particle()));
        return textures;
    }

    private static JsonObject templateBlockState(ResourceManager manager, Identifier resourceId,
            String sourceModel, String targetModel) {
        Resource resource = manager.getResource(resourceId).orElseThrow(() ->
                new IllegalStateException("Missing vanilla blockstate template " + resourceId));
        try (var reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            replaceStrings(root, sourceModel, targetModel);
            return root;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot project vanilla blockstate " + resourceId, exception);
        }
    }

    private static void replaceStrings(JsonElement value, String source, String target) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            for (String key : List.copyOf(object.keySet())) {
                JsonElement child = object.get(key);
                if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    object.addProperty(key, child.getAsString().replace(source, target));
                } else replaceStrings(child, source, target);
            }
        } else if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) replaceStrings(child, source, target);
        }
    }

    private static JsonObject combinedLanguage() {
        JsonObject language = new JsonObject();
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".layers", "Layers");
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".corners", "Corners");
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".quarter_columns", "Quarter Columns");
        for (LayerGeneratedData.Binding layer : LayerGeneratedData.bindings())
            language.addProperty(translation(layer.id()), LayerGeneratedResources.displayName(layer.profile()));
        for (QuarterGeometryGeneratedData.Binding corner :
                QuarterGeometryGeneratedData.bindings(BgeGeometryRole.CORNER))
            language.addProperty(translation(corner.id()), QuarterGeometryGeneratedResources.cornerDisplayName(corner.profile()));
        for (QuarterGeometryGeneratedData.Binding column :
                QuarterGeometryGeneratedData.bindings(BgeGeometryRole.QUARTER_COLUMN))
            language.addProperty(translation(column.id()), QuarterGeometryGeneratedResources.quarterColumnDisplayName(column.profile()));
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            for (Map.Entry<String, Block> role : binding.roles().entrySet()) {
                if (role.getKey().equals("block") || role.getKey().equals("layer")
                        || role.getKey().equals("corner") || role.getKey().equals("quarter_column")) continue;
                if (!binding.isGeneratedRole(role.getKey())) continue;
                Identifier id = id(role.getValue());
                language.addProperty(translation(id), AssetGenerator.langName(
                        binding.spec().id().getPath() + "_" + role.getKey()));
            }
        }
        return language;
    }

    private static int writeModels(Map<String, JsonObject> models) {
        models.forEach((modelId, model) -> write(modelResource(modelId), model));
        return models.size();
    }
    private static JsonObject variants(JsonObject variants) { JsonObject root = new JsonObject(); root.add("variants", variants); return root; }
    private static JsonObject selection(String model) { return selection(model, 0, 0, false); }
    private static JsonObject selection(String model, int x, int y, boolean uvlock) {
        JsonObject value = new JsonObject(); value.addProperty("model", model);
        if (x != 0) value.addProperty("x", x); if (y != 0) value.addProperty("y", y);
        if (uvlock) value.addProperty("uvlock", true); return value;
    }
    private static JsonArray numbers(int... values) { JsonArray result = new JsonArray(); for (int value : values) result.add(value); return result; }
    private static String texture(String path) { return path.contains(":") ? path : "minecraft:block/" + path; }
    private static String translation(Identifier id) { return "block." + id.getNamespace() + "." + id.getPath().replace('/', '.'); }
    private static String model(Identifier id) { return id.getNamespace() + ":block/" + id.getPath(); }
    private static Identifier id(Block block) { return BuiltInRegistries.BLOCK.getKey(block); }
    private static Identifier blockState(Identifier id) { return Identifier.fromNamespaceAndPath(id.getNamespace(), "blockstates/" + id.getPath() + ".json"); }
    private static Identifier item(Identifier id) { return Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json"); }
    private static Identifier modelResource(Identifier id) { return modelResource(id, ""); }
    private static Identifier modelResource(Identifier id, String suffix) { return Identifier.fromNamespaceAndPath(id.getNamespace(), "models/block/" + id.getPath() + suffix + ".json"); }
    private static Identifier modelResource(String model) { Identifier id = Identifier.parse(model); return Identifier.fromNamespaceAndPath(id.getNamespace(), "models/" + id.getPath() + ".json"); }
    private static void write(Identifier id, JsonElement json) { ClutterNoMore.RESOURCES.addJson(PackType.CLIENT_RESOURCES, id, json); }

    public record GenerationSummary(int familyCount, int blockStateCount, int modelCount, int itemCount) {}
}

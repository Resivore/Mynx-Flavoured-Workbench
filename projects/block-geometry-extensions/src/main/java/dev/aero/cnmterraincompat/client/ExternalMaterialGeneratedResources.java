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

            if (profile.orientationPolicy() == NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED) {
                NativeAxisModelContract.AxisUvPolicy nativePolicy = NativeAxisModelContract.AxisUvPolicy.valueOf(
                        AxisGeneratedResources.policy(manager, profile.canonicalParentId()).name());
                NativeAxisModelContract.GeneratedBlockResources slabResources =
                        NativeAxisModelContract.slab(profile, nativePolicy);
                write(blockState(slab), slabResources.blockState());
                models += writeModels(slabResources.models());
                NativeAxisModelContract.GeneratedBlockResources stairResources =
                        NativeAxisModelContract.stairs(profile, nativePolicy);
                write(blockState(stairs), stairResources.blockState());
                models += writeModels(stairResources.models());
                models += writeAxisWall(profile, wall);
                blockStates += 3;
            } else {
                models += writeSlab(profile, slab);
                models += writeStairs(manager, profile, stairs);
                models += writeWall(manager, profile, wall);
                blockStates += 3;
            }

            if (profile.orientationPolicy() == NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED) {
                AxisModelContract.AxisUvPolicy policy = AxisGeneratedResources.policy(
                        manager, profile.canonicalParentId());
                Map<String, JsonObject> verticalModels =
                        AxisModelContract.verticalGeneratedModels(profile, vertical, policy);
                verticalModels.forEach((modelId, model) -> write(modelResource(modelId), model));
                write(blockState(vertical), AxisModelContract.verticalBlockState(vertical, policy));
                models += verticalModels.size();
                verticalItemModel = verticalModels.keySet().iterator().next();
                Map<String, JsonObject> stepModels =
                        AxisModelContract.stepGeneratedModels(profile, step, policy);
                stepModels.forEach((modelId, model) -> write(modelResource(modelId), model));
                write(blockState(step), AxisModelContract.stepBlockState(step, policy));
                models += stepModels.size();
                stepItemModel = stepModels.keySet().iterator().next();
            } else {
                models += writeVertical(profile, vertical);
                models += writeStep(profile, step);
            }
            blockStates += 2;

            write(item(slab), GeneratedItemModelSupport.itemDefinition(manager, profile, model(slab)));
            write(item(stairs), GeneratedItemModelSupport.itemDefinition(manager, profile, model(stairs)));
            write(item(wall), GeneratedItemModelSupport.itemDefinition(manager, profile, model(wall) + "_inventory"));
            write(item(vertical), GeneratedItemModelSupport.itemDefinition(manager, profile, verticalItemModel));
            write(item(step), GeneratedItemModelSupport.itemDefinition(manager, profile, stepItemModel));
            items += 5;
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
        write(modelResource(id), template("minecraft:block/slab", profile));
        write(modelResource(id, "_top"), template("minecraft:block/slab_top", profile));
        write(modelResource(id, "_double"), template("minecraft:block/cube_bottom_top", profile));
        return 3;
    }

    private static int writeStairs(ResourceManager manager, NibaruMaterialProfile profile, Identifier id) {
        JsonObject state = templateBlockState(manager,
                Identifier.fromNamespaceAndPath("minecraft", "blockstates/oak_stairs.json"),
                "minecraft:block/oak_stairs", model(id));
        write(blockState(id), state);
        write(modelResource(id), template("minecraft:block/stairs", profile));
        write(modelResource(id, "_inner"), template("minecraft:block/inner_stairs", profile));
        write(modelResource(id, "_outer"), template("minecraft:block/outer_stairs", profile));
        return 3;
    }

    private static int writeWall(ResourceManager manager, NibaruMaterialProfile profile, Identifier id) {
        JsonObject state = templateBlockState(manager,
                Identifier.fromNamespaceAndPath("minecraft", "blockstates/cobblestone_wall.json"),
                "minecraft:block/cobblestone_wall", model(id));
        write(blockState(id), state);
        write(modelResource(id, "_post"), wallTemplate("minecraft:block/template_wall_post", profile));
        write(modelResource(id, "_side"), wallTemplate("minecraft:block/template_wall_side", profile));
        write(modelResource(id, "_side_tall"), wallTemplate("minecraft:block/template_wall_side_tall", profile));
        write(modelResource(id, "_inventory"), wallTemplate("minecraft:block/wall_inventory", profile));
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

    private static int writeAxisWall(NibaruMaterialProfile profile, Identifier id) {
        JsonArray multipart = new JsonArray();
        Map<String, JsonObject> models = new LinkedHashMap<>();
        for (Direction.Axis axis : Direction.Axis.values()) {
            String axisName = axis.name().toLowerCase(Locale.ROOT);
            String post = model(id) + "_axis_" + axisName + "_post";
            models.put(post, cuboidModel(profile, axis, List.of(new int[] {4, 0, 4, 12, 16, 12})));
            multipart.add(part("axis", axisName, "up", "true", post));
            for (Direction direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                String side = direction.getSerializedName();
                String low = model(id) + "_axis_" + axisName + "_" + side + "_side";
                String tall = low + "_tall";
                models.put(low, cuboidModel(profile, axis, List.of(wallSide(direction, 14))));
                models.put(tall, cuboidModel(profile, axis, List.of(wallSide(direction, 16))));
                multipart.add(part("axis", axisName, side, "low", low));
                multipart.add(part("axis", axisName, side, "tall", tall));
            }
        }
        JsonObject state = new JsonObject();
        state.add("multipart", multipart);
        write(blockState(id), state);
        models.forEach((modelId, json) -> write(modelResource(modelId), json));
        write(modelResource(id, "_inventory"), cuboidModel(profile, Direction.Axis.Y, List.of(
                new int[] {4, 0, 4, 12, 16, 12}, new int[] {0, 0, 5, 16, 14, 11},
                new int[] {5, 0, 0, 11, 14, 16})));
        return models.size() + 1;
    }

    private static int[] wallSide(Direction direction, int height) {
        return switch (direction) {
            case NORTH -> new int[] {5, 0, 0, 11, height, 11};
            case SOUTH -> new int[] {5, 0, 5, 11, height, 16};
            case WEST -> new int[] {0, 0, 5, 11, height, 11};
            case EAST -> new int[] {5, 0, 5, 16, height, 11};
            default -> throw new IllegalArgumentException("Wall side must be horizontal");
        };
    }

    private static JsonObject cuboidModel(NibaruMaterialProfile profile, Direction.Axis materialAxis,
            List<int[]> cuboids) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:block/block");
        JsonObject textures = textures(profile);
        root.add("textures", textures);
        JsonArray elements = new JsonArray();
        for (int[] bounds : cuboids) {
            JsonObject element = new JsonObject();
            element.add("from", numbers(bounds[0], bounds[1], bounds[2]));
            element.add("to", numbers(bounds[3], bounds[4], bounds[5]));
            JsonObject faces = new JsonObject();
            for (Direction face : Direction.values()) {
                JsonObject encoded = new JsonObject();
                encoded.addProperty("texture", face.getAxis() == materialAxis ? "#top" : "#side");
                faces.add(face.getSerializedName(), encoded);
            }
            element.add("faces", faces);
            elements.add(element);
        }
        root.add("elements", elements);
        return root;
    }

    private static JsonObject part(String firstKey, String firstValue,
            String secondKey, String secondValue, String model) {
        JsonObject result = new JsonObject();
        JsonObject when = new JsonObject();
        when.addProperty(firstKey, firstValue);
        when.addProperty(secondKey, secondValue);
        result.add("when", when);
        result.add("apply", selection(model));
        return result;
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

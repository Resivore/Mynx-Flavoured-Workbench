package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.BgeColumnBlock;
import dev.aero.cnmterraincompat.BgeCornerBlock;
import dev.aero.cnmterraincompat.BgeGeometryRole;
import dev.aero.cnmterraincompat.CnmShapeMapCandidateBridge;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.LayerGeneratedData;
import dev.aero.cnmterraincompat.QuarterGeometryGeneratedData;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Client-resource closure for a truly profile-free CNM component.  Before ShapeMap has selected
 * its parent, the real admitted CNM source supplies a provisional visual reference so first bake
 * has complete assets.  Once resolved, models read only the elected parent's canonical resource
 * contract; neither resource path nor texture name ever decides family identity.
 */
public final class ResolvedCnmCandidateResources {
    private static final Direction[] FACES = Direction.values();

    private ResolvedCnmCandidateResources() {}

    static GenerationSummary generate(ResourceManager manager) {
        Objects.requireNonNull(manager, "manager");
        // The registered Layer ID is stable from admission through resolution, unlike the
        // provisional visual source and later elected canonical source.  Key the write plan by
        // that identity so a resolved family replaces its provisional rendering exactly once.
        Map<Identifier, FamilyResources> families = new LinkedHashMap<>();
        for (CnmShapeMapCandidateBridge.ProvisionalGenericFamily family
                : CnmShapeMapCandidateBridge.provisionalGenericFamilies()) {
            families.put(family.roles().get(BgeGeometryRole.LAYER),
                    new FamilyResources(family.visualSource(), family.roles(), family.wall()));
        }
        for (CnmShapeMapCandidateBridge.ResolvedGenericFamily family
                : CnmShapeMapCandidateBridge.resolvedGenericFamilies()) {
            // The same registered role IDs deliberately replace their provisional visual source.
            // This is resource reconciliation, never family election.
            families.put(family.roles().get(BgeGeometryRole.LAYER),
                    new FamilyResources(family.canonicalParent(), family.roles(), family.wall()));
        }
        int models = 0;
        for (FamilyResources family : families.values()) {
            Textures textures = Textures.resolve(manager, family.visualSource());
            if (family.wall().isPresent()) models += writeWall(manager, family.wall().orElseThrow(), textures);
            models += writeLayer(family.roles().get(BgeGeometryRole.LAYER), textures);
            models += writeCorner(family.roles().get(BgeGeometryRole.CORNER), textures);
            models += writeColumn(family.roles().get(BgeGeometryRole.QUARTER_COLUMN), textures);
        }
        if (!families.isEmpty()) writeLanguage(CnmShapeMapCandidateBridge.resolvedGenericFamilies());
        return new GenerationSummary(families.size(), models);
    }

    /**
     * Exercises the same geometry writer for an admitted visual source whose registered family
     * identity is outside this test seam. The supplied IDs are resource-only and never register
     * or bind a material parent.
     */
    public static GenerationSummary generateVisualSourceForValidation(ResourceManager manager,
            Identifier visualSource, Map<BgeGeometryRole, Identifier> roles) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(visualSource, "visualSource");
        Objects.requireNonNull(roles, "roles");
        if (!roles.keySet().containsAll(List.of(BgeGeometryRole.LAYER, BgeGeometryRole.CORNER,
                BgeGeometryRole.QUARTER_COLUMN))) {
            throw new IllegalArgumentException("Validation visual source is missing a BGE tail: " + roles);
        }
        Identifier layer = roles.get(BgeGeometryRole.LAYER);
        return writeFamilies(manager, Map.of(layer, new FamilyResources(visualSource, Map.copyOf(roles),
                java.util.Optional.empty())));
    }

    /**
     * First-bake regression seam. It deliberately uses only admitted visual references, before
     * a caller relies on any elected parent. A later ordinary generation replaces the same role
     * resources from the resolved parent without adding another functional family.
     */
    public static GenerationSummary generateProvisionalForValidation(ResourceManager manager,
            Set<Identifier> visualSources) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(visualSources, "visualSources");
        Map<Identifier, FamilyResources> families = new LinkedHashMap<>();
        for (CnmShapeMapCandidateBridge.ProvisionalGenericFamily family
                : CnmShapeMapCandidateBridge.provisionalGenericFamilies()) {
            if (!visualSources.contains(family.visualSource())) continue;
            families.put(family.roles().get(BgeGeometryRole.LAYER),
                    new FamilyResources(family.visualSource(), family.roles(), family.wall()));
        }
        return writeFamilies(manager, families);
    }

    private static GenerationSummary writeFamilies(ResourceManager manager,
            Map<Identifier, FamilyResources> families) {
        int models = 0;
        for (FamilyResources family : families.values()) {
            Textures textures = Textures.resolve(manager, family.visualSource());
            if (family.wall().isPresent()) models += writeWall(manager, family.wall().orElseThrow(), textures);
            models += writeLayer(family.roles().get(BgeGeometryRole.LAYER), textures);
            models += writeCorner(family.roles().get(BgeGeometryRole.CORNER), textures);
            models += writeColumn(family.roles().get(BgeGeometryRole.QUARTER_COLUMN), textures);
        }
        return new GenerationSummary(families.size(), models);
    }

    private static int writeLayer(Identifier id, Textures textures) {
        JsonObject variants = new JsonObject();
        int count = 0;
        String item = null;
        for (Direction facing : FACES) for (int layers = 1; layers <= 4; layers++) {
            String model = modelId(id, "_" + facing.getSerializedName() + "_" + layers);
            write(modelResource(model), model(textures, List.of(layerBounds(facing, layers))));
            variants.add("facing=" + facing.getSerializedName() + ",layers=" + layers,
                    variant(model));
            if (facing == Direction.UP && layers == 1) item = model;
            count++;
        }
        write(blockStateResource(id), variants(variants));
        write(itemResource(id), item(item));
        return count;
    }

    /** Normal Wall resources are emitted only for the generic carrier CNM did not already own. */
    private static int writeWall(ResourceManager manager, Identifier id, Textures textures) {
        JsonObject state = templateBlockState(manager,
                Identifier.fromNamespaceAndPath("minecraft", "blockstates/cobblestone_wall.json"),
                "minecraft:block/cobblestone_wall", modelId(id, ""));
        write(blockStateResource(id), state);
        write(modelResource(modelId(id, "_post")), wallModel("minecraft:block/template_wall_post", textures));
        write(modelResource(modelId(id, "_side")), wallModel("minecraft:block/template_wall_side", textures));
        write(modelResource(modelId(id, "_side_tall")), wallModel("minecraft:block/template_wall_side_tall", textures));
        write(modelResource(modelId(id, "_inventory")), wallModel("minecraft:block/wall_inventory", textures));
        write(itemResource(id), item(modelId(id, "_inventory")));
        return 4;
    }

    private static int writeCorner(Identifier id, Textures textures) {
        JsonObject variants = new JsonObject();
        String item = null;
        int count = 0;
        for (BgeCornerBlock.Orientation orientation : BgeCornerBlock.Orientation.values()) {
            String model = modelId(id, "_" + orientation.getSerializedName());
            write(modelResource(model), model(textures, cornerBoxes(orientation)));
            variants.add("facing=" + orientation.stateFacing().getSerializedName(), variant(model));
            if (orientation == BgeCornerBlock.Orientation.SOUTH_WEST) item = model;
            count++;
        }
        write(blockStateResource(id), variants(variants));
        write(itemResource(id), item(item));
        return count;
    }

    private static int writeColumn(Identifier id, Textures textures) {
        JsonObject variants = new JsonObject();
        String item = null;
        int count = 0;
        for (BgeColumnBlock.Occupancy occupancy : BgeColumnBlock.Occupancy.values()) {
            String model = modelId(id, "_" + occupancy.getSerializedName());
            write(modelResource(model), model(textures, columnBoxes(occupancy)));
            variants.add("occupancy=" + occupancy.getSerializedName(), variant(model));
            if (occupancy == BgeColumnBlock.Occupancy.SE) item = model;
            count++;
        }
        write(blockStateResource(id), variants(variants));
        write(itemResource(id), item(item));
        return count;
    }

    private static void writeLanguage(List<CnmShapeMapCandidateBridge.ResolvedGenericFamily> families) {
        JsonObject language = new JsonObject();
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".layers", "Layers");
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".corners", "Corners");
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".quarter_columns", "Quarter Columns");
        LayerGeneratedData.bindings().forEach(binding -> language.addProperty(translation(binding.id()),
                LayerGeneratedResources.displayName(binding.profile())));
        QuarterGeometryGeneratedData.bindings(BgeGeometryRole.CORNER).forEach(binding ->
                language.addProperty(translation(binding.id()),
                        QuarterGeometryGeneratedResources.cornerDisplayName(binding.profile())));
        QuarterGeometryGeneratedData.bindings(BgeGeometryRole.QUARTER_COLUMN).forEach(binding ->
                language.addProperty(translation(binding.id()),
                        QuarterGeometryGeneratedResources.quarterColumnDisplayName(binding.profile())));
        for (CnmShapeMapCandidateBridge.ResolvedGenericFamily family : families) {
            family.roles().forEach((role, id) -> language.addProperty(translation(id),
                    AssetGenerator.langName(family.canonicalParent().getPath() + "_" + roleName(role))));
        }
        write(Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID + "_generated", "lang/en_us.json"), language);
    }

    private static String roleName(BgeGeometryRole role) {
        return switch (role) {
            case LAYER -> "layer";
            case CORNER -> "corner";
            case QUARTER_COLUMN -> "quarter_column";
            default -> throw new IllegalArgumentException("Not a generated tail role: " + role);
        };
    }

    private static JsonObject model(Textures textures, List<int[]> boxes) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", "minecraft:block/block");
        JsonObject textureJson = new JsonObject();
        textureJson.addProperty("side", textures.side());
        textureJson.addProperty("top", textures.top());
        textureJson.addProperty("bottom", textures.bottom());
        textureJson.addProperty("particle", textures.side());
        result.add("textures", textureJson);
        JsonArray elements = new JsonArray();
        for (int[] box : boxes) elements.add(element(box));
        result.add("elements", elements);
        return result;
    }

    private static JsonObject wallModel(String parent, Textures textures) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        JsonObject textureJson = new JsonObject();
        textureJson.addProperty("wall", textures.side());
        textureJson.addProperty("particle", textures.side());
        result.add("textures", textureJson);
        return result;
    }

    private static JsonObject element(int[] box) {
        JsonObject result = new JsonObject();
        result.add("from", numbers(box[0], box[1], box[2]));
        result.add("to", numbers(box[3], box[4], box[5]));
        JsonObject faces = new JsonObject();
        for (Direction face : FACES) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", textureRole(face));
            if (onBoundary(box, face)) encoded.addProperty("cullface", face.getSerializedName());
            faces.add(face.getSerializedName(), encoded);
        }
        result.add("faces", faces);
        return result;
    }

    private static String textureRole(Direction face) {
        return switch (face) {
            case UP -> "#top";
            case DOWN -> "#bottom";
            default -> "#side";
        };
    }

    private static boolean onBoundary(int[] box, Direction face) {
        return switch (face) {
            case DOWN -> box[1] == 0;
            case UP -> box[4] == 16;
            case NORTH -> box[2] == 0;
            case SOUTH -> box[5] == 16;
            case WEST -> box[0] == 0;
            case EAST -> box[3] == 16;
        };
    }

    private static int[] layerBounds(Direction facing, int layers) {
        int depth = layers * 4;
        return switch (facing) {
            case UP -> box(0, 0, 0, 16, depth, 16);
            case DOWN -> box(0, 16 - depth, 0, 16, 16, 16);
            case NORTH -> box(0, 0, 0, 16, 16, depth);
            case SOUTH -> box(0, 0, 16 - depth, 16, 16, 16);
            case WEST -> box(0, 0, 0, depth, 16, 16);
            case EAST -> box(16 - depth, 0, 0, 16, 16, 16);
        };
    }

    private static List<int[]> cornerBoxes(BgeCornerBlock.Orientation orientation) {
        return switch (orientation) {
            case SOUTH_WEST -> List.of(nw(), ne(), sw());
            case NORTH_WEST -> List.of(nw(), ne(), se());
            case NORTH_EAST -> List.of(nw(), sw(), se());
            case SOUTH_EAST -> List.of(ne(), sw(), se());
        };
    }

    private static List<int[]> columnBoxes(BgeColumnBlock.Occupancy occupancy) {
        return switch (occupancy) {
            case NW -> List.of(nw());
            case NE -> List.of(ne());
            case SW -> List.of(sw());
            case SE -> List.of(se());
            case NW_SE -> List.of(nw(), se());
            case NE_SW -> List.of(ne(), sw());
        };
    }

    private static int[] nw() { return box(0, 0, 0, 8, 16, 8); }
    private static int[] ne() { return box(8, 0, 0, 16, 16, 8); }
    private static int[] sw() { return box(0, 0, 8, 8, 16, 16); }
    private static int[] se() { return box(8, 0, 8, 16, 16, 16); }
    private static int[] box(int x0, int y0, int z0, int x1, int y1, int z1) {
        return new int[]{x0, y0, z0, x1, y1, z1};
    }

    private static JsonObject variants(JsonObject variants) {
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    private static JsonObject variant(String model) {
        JsonObject result = new JsonObject();
        result.addProperty("model", model);
        return result;
    }

    private static JsonObject item(String model) {
        JsonObject root = new JsonObject();
        JsonObject value = new JsonObject();
        value.addProperty("type", "minecraft:model");
        value.addProperty("model", model);
        root.add("model", value);
        return root;
    }

    private static JsonObject templateBlockState(ResourceManager manager, Identifier resourceId,
            String sourceModel, String targetModel) {
        Resource resource = manager.getResource(resourceId).orElseThrow(() ->
                new IllegalStateException("Missing vanilla wall blockstate template " + resourceId));
        try (var reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            replaceStrings(root, sourceModel, targetModel);
            return root;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot project vanilla wall blockstate " + resourceId, exception);
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

    private static Identifier blockStateResource(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "blockstates/" + id.getPath() + ".json");
    }

    private static Identifier itemResource(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json");
    }

    private static Identifier modelResource(String model) {
        Identifier id = Identifier.parse(model);
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "models/" + id.getPath() + ".json");
    }

    private static String modelId(Identifier id, String suffix) {
        return id.getNamespace() + ":block/" + id.getPath() + suffix;
    }

    private static String translation(Identifier id) {
        return "block." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    private static JsonArray numbers(int... values) {
        JsonArray result = new JsonArray();
        for (int value : values) result.add(value);
        return result;
    }

    private static void write(Identifier id, JsonElement json) {
        BgeGeneratedResourceWriter.write(id, json);
    }

    public record GenerationSummary(int familyCount, int modelCount) {}

    private record FamilyResources(Identifier visualSource, Map<BgeGeometryRole, Identifier> roles,
            java.util.Optional<Identifier> wall) {}

    /** Resolves the final model inheritance chain only after ShapeMap selected the parent. */
    private record Textures(String side, String top, String bottom) {
        private static Textures resolve(ResourceManager manager, Identifier canonical) {
            Map<String, String> variables = variables(manager, canonical, new LinkedHashSet<>());
            String side = first(variables, canonical, "side", "all", "texture", "particle");
            String top = first(variables, canonical, "top", "end", "all", "side", "particle");
            String bottom = first(variables, canonical, "bottom", "top", "end", "all", "side", "particle");
            if (side == null || top == null || bottom == null) {
                throw new IllegalStateException("Resolved CNM parent has no ordinary texture contract: "
                        + canonical + " " + variables);
            }
            return new Textures(side, top, bottom);
        }

        private static Map<String, String> variables(ResourceManager manager, Identifier model,
                Set<Identifier> visiting) {
            if (!visiting.add(model)) throw new IllegalStateException("Cyclic canonical model parent: " + model);
            try {
                Resource resource = manager.getResource(modelResourceId(model)).orElse(null);
                if (resource == null) return blockStateVariables(manager, model, visiting);
                JsonObject root;
                try (var reader = resource.openAsReader()) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot read canonical model " + model, exception);
                }
                Map<String, String> result = new LinkedHashMap<>();
                if (root.has("parent")) result.putAll(variables(manager,
                        Identifier.parse(root.get("parent").getAsString()), visiting));
                if (root.has("textures") && root.get("textures").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("textures").entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) result.put(entry.getKey(),
                                entry.getValue().getAsString());
                    }
                }
                return result;
            } finally {
                visiting.remove(model);
            }
        }

        /**
         * A block ID normally names {@code models/block/<id>}. BBB beam standard forms instead
         * name their concrete models from the blockstate (for example,
         * {@code block/beam/acacia_beam_stairs_inner}). Keep the ShapeMap parent unchanged and
         * follow that provider-owned visual indirection only when the direct model is absent.
         */
        private static Map<String, String> blockStateVariables(ResourceManager manager, Identifier model,
                Set<Identifier> visiting) {
            if (model.getPath().startsWith("block/")) {
                throw new IllegalStateException("Missing canonical model for resolved CNM parent: " + model);
            }
            Identifier blockState = Identifier.fromNamespaceAndPath(model.getNamespace(),
                    "blockstates/" + model.getPath() + ".json");
            Resource resource = manager.getResource(blockState).orElseThrow(() ->
                    new IllegalStateException("Missing canonical model for resolved CNM parent: " + model));
            JsonObject root;
            try (var reader = resource.openAsReader()) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read canonical blockstate " + blockState, exception);
            }
            List<Identifier> models = new ArrayList<>();
            collectBlockStateModels(root, models);
            if (models.isEmpty()) {
                throw new IllegalStateException("Canonical blockstate has no model for resolved CNM parent: "
                        + model + " " + blockState);
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
            throw new IllegalStateException("Cannot resolve a canonical visual model through blockstate "
                    + blockState + " for resolved CNM parent: " + model, failure);
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
            return value == null ? null : texture(canonical, value);
        }

        private static String firstValue(Map<String, String> variables, String... names) {
            for (String name : names) {
                String value = resolve(variables, variables.get(name));
                if (value != null) return value;
            }
            for (String value : variables.values()) {
                String resolved = resolve(variables, value);
                if (resolved != null) return resolved;
            }
            return null;
        }

        private static String resolve(Map<String, String> variables, String value) {
            int guard = 0;
            while (value != null && value.startsWith("#") && guard++ < 32) {
                value = variables.get(value.substring(1));
            }
            return value == null || value.startsWith("#") ? null : value;
        }

        private static String texture(Identifier canonical, String value) {
            if (value.contains(":")) return value;
            return canonical.getNamespace() + ":" + (value.contains("/") ? value : "block/" + value);
        }
    }
}

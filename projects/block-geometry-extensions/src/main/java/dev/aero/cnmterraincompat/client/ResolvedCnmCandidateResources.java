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
 * Client-resource closure for a truly profile-free CNM component.  ShapeMap has already selected
 * its parent before this writer runs.  Models only read that parent's canonical resource contract;
 * they never use model paths, texture names, or registration names to decide family identity.
 */
final class ResolvedCnmCandidateResources {
    private static final Direction[] FACES = Direction.values();

    private ResolvedCnmCandidateResources() {}

    static GenerationSummary generate(ResourceManager manager) {
        Objects.requireNonNull(manager, "manager");
        List<CnmShapeMapCandidateBridge.ResolvedGenericFamily> families =
                CnmShapeMapCandidateBridge.resolvedGenericFamilies();
        int models = 0;
        for (CnmShapeMapCandidateBridge.ResolvedGenericFamily family : families) {
            Textures textures = Textures.resolve(manager, family.canonicalParent());
            models += writeLayer(family.roles().get(BgeGeometryRole.LAYER), textures);
            models += writeCorner(family.roles().get(BgeGeometryRole.CORNER), textures);
            models += writeColumn(family.roles().get(BgeGeometryRole.QUARTER_COLUMN), textures);
        }
        if (!families.isEmpty()) writeLanguage(families);
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

    record GenerationSummary(int familyCount, int modelCount) {}

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
                Resource resource = manager.getResource(modelResourceId(model)).orElseThrow(() ->
                        new IllegalStateException("Missing canonical model for resolved CNM parent: " + model));
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

        private static Identifier modelResourceId(Identifier model) {
            String path = model.getPath().startsWith("block/") ? "models/" + model.getPath()
                    : "models/block/" + model.getPath();
            return Identifier.fromNamespaceAndPath(model.getNamespace(), path + ".json");
        }

        private static String first(Map<String, String> variables, Identifier canonical, String... names) {
            for (String name : names) {
                String value = resolve(variables, variables.get(name));
                if (value != null) return texture(canonical, value);
            }
            for (String value : variables.values()) {
                String resolved = resolve(variables, value);
                if (resolved != null) return texture(canonical, resolved);
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

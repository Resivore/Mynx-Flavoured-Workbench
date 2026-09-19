package games.twinhead.moreslabsstairsandwalls.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Completes the static Nibaru production-resource closure for catalog families added after the
 * immutable upstream resource snapshot.  This is deliberately material-typed: it derives every
 * missing standard role from {@link ModBlocks}, rather than carrying a per-release counterpart
 * filename list that can register a block without its client/server resources.
 */
public final class NativeMaterialResourceGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String NAMESPACE = NativeAxisModelContract.PROVIDER_NAMESPACE;
    private static Path generatedRoot;
    private static List<Path> immutableOverlays;

    private NativeMaterialResourceGenerator() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 3) throw new IllegalArgumentException(
                "Expected generated-resource root and two immutable resource-overlay arguments");
        SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        generatedRoot = root;
        immutableOverlays = List.of(Path.of(args[1]).toAbsolutePath().normalize(),
                Path.of(args[2]).toAbsolutePath().normalize());
        Path assets = root.resolve("assets").resolve(NAMESPACE).normalize();
        Path data = root.resolve("data").resolve(NAMESPACE).normalize();
        requireContained(root, assets);
        requireContained(root, data);

        int completedFamilies = 0;
        int completedRoles = 0;
        for (ModBlocks family : ModBlocks.values()) {
            int before = completedRoles;
            if (family.hasBlock(ModBlocks.BlockType.SLAB)) {
                writeSlab(assets, data, family);
                completedRoles++;
            }
            if (family.hasBlock(ModBlocks.BlockType.STAIRS)) {
                writeStairs(assets, data, family);
                completedRoles++;
            }
            if (family.hasBlock(ModBlocks.BlockType.WALL)) {
                writeWall(assets, data, family);
                completedRoles++;
            }
            if (completedRoles != before) completedFamilies++;
        }
        System.out.println("NATIVE_MATERIAL_RESOURCE_CLOSURE families=" + completedFamilies
                + " roles=" + completedRoles);
    }

    private static void writeSlab(Path assets, Path data, ModBlocks family) throws IOException {
        Identifier id = family.getId(ModBlocks.BlockType.SLAB);
        if (!NativeAxisModelContract.applies(family)) {
            Map<String, String> textures = textures(family);
            JsonObject variants = new JsonObject();
            variants.add("type=bottom", modelReference(model(id)));
            variants.add("type=top", modelReference(model(id) + "_top"));
            variants.add("type=double", modelReference(canonicalModel(family)));
            JsonObject state = new JsonObject();
            state.add("variants", variants);
            writeIfAbsent(blockState(assets, id), state);
            writeIfAbsent(blockModel(assets, id, ""), template("minecraft:block/slab", textures));
            writeIfAbsent(blockModel(assets, id, "_top"), template("minecraft:block/slab_top", textures));
        }
        writeItem(assets, id, model(id));
        writeLoot(data, id, true);
    }

    private static void writeStairs(Path assets, Path data, ModBlocks family) throws IOException {
        Identifier id = family.getId(ModBlocks.BlockType.STAIRS);
        if (!NativeAxisModelContract.applies(family)) {
            Map<String, String> textures = textures(family);
            writeIfAbsent(blockState(assets, id), blockStateTemplate("oak_stairs",
                    Map.of("minecraft:block/oak_stairs", model(id),
                            "minecraft:block/oak_stairs_inner", model(id) + "_inner",
                            "minecraft:block/oak_stairs_outer", model(id) + "_outer")));
            writeIfAbsent(blockModel(assets, id, ""), template("minecraft:block/stairs", textures));
            writeIfAbsent(blockModel(assets, id, "_inner"), template("minecraft:block/inner_stairs", textures));
            writeIfAbsent(blockModel(assets, id, "_outer"), template("minecraft:block/outer_stairs", textures));
        }
        writeItem(assets, id, model(id));
        writeLoot(data, id, false);
    }

    private static void writeWall(Path assets, Path data, ModBlocks family) throws IOException {
        Identifier id = family.getId(ModBlocks.BlockType.WALL);
        Map<String, String> textures = textures(family);
        writeIfAbsent(blockState(assets, id), blockStateTemplate("cobblestone_wall", Map.of(
                "minecraft:block/cobblestone_wall_post", model(id) + "_post",
                "minecraft:block/cobblestone_wall_side", model(id) + "_side",
                "minecraft:block/cobblestone_wall_side_tall", model(id) + "_side_tall")));
        boolean pillar = NativeAxisModelContract.applies(family);
        writeIfAbsent(blockModel(assets, id, "_post"), template(pillar
                ? NAMESPACE + ":block/template_column_wall_post" : "minecraft:block/template_wall_post", textures));
        writeIfAbsent(blockModel(assets, id, "_side"), template(pillar
                ? NAMESPACE + ":block/template_column_wall_side" : "minecraft:block/template_wall_side", textures));
        writeIfAbsent(blockModel(assets, id, "_side_tall"), template(pillar
                ? NAMESPACE + ":block/template_column_wall_side_tall" : "minecraft:block/template_wall_side_tall", textures));
        JsonObject inventory = new JsonObject();
        inventory.addProperty("parent", "minecraft:block/wall_inventory");
        JsonObject wallTexture = new JsonObject();
        wallTexture.addProperty("wall", textures.get("side"));
        inventory.add("textures", wallTexture);
        writeIfAbsent(blockModel(assets, id, "_inventory"), inventory);
        writeItem(assets, id, model(id) + "_inventory");
        writeLoot(data, id, false);
    }

    private static void writeItem(Path assets, Identifier id, String parent) throws IOException {
        JsonObject itemModel = new JsonObject();
        itemModel.addProperty("parent", parent);
        writeIfAbsent(itemModel(assets, id), itemModel);
        JsonObject reference = new JsonObject();
        reference.addProperty("type", "minecraft:model");
        reference.addProperty("model", item(id));
        JsonObject definition = new JsonObject();
        definition.add("model", reference);
        writeIfAbsent(itemDefinition(assets, id), definition);
        writeLanguage(assets, id);
    }

    private static void writeLoot(Path data, Identifier id, boolean doubled) throws IOException {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", id.toString());
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);
        pool.addProperty("bonus_rolls", 0.0);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        if (doubled) {
            JsonObject condition = new JsonObject();
            condition.addProperty("condition", "minecraft:block_state_property");
            condition.addProperty("block", id.toString());
            JsonObject properties = new JsonObject();
            properties.addProperty("type", "double");
            condition.add("properties", properties);
            JsonObject count = new JsonObject();
            count.addProperty("function", "minecraft:set_count");
            count.addProperty("count", 2.0);
            count.addProperty("add", false);
            JsonArray conditions = new JsonArray();
            conditions.add(condition);
            count.add("conditions", conditions);
            JsonObject decay = new JsonObject();
            decay.addProperty("function", "minecraft:explosion_decay");
            JsonArray functions = new JsonArray();
            functions.add(count);
            functions.add(decay);
            entry.add("functions", functions);
        } else {
            JsonObject survives = new JsonObject();
            survives.addProperty("condition", "minecraft:survives_explosion");
            JsonArray conditions = new JsonArray();
            conditions.add(survives);
            pool.add("conditions", conditions);
        }
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonArray pools = new JsonArray();
        pools.add(pool);
        root.add("pools", pools);
        Path target = data.resolve("loot_table/blocks").resolve(id.getPath() + ".json").normalize();
        requireContained(data, target);
        writeIfAbsent(target, root);
    }

    private static Map<String, String> textures(ModBlocks family) {
        Identifier source = BuiltInRegistries.BLOCK.getKey(family.parentBlock);
        if (source == null) throw new IllegalStateException("Missing canonical block identity for " + family);
        String base = source.getPath();
        String side = family.textureId.isEmpty() ? base : family.textureId;
        String top = family.topId.isEmpty() ? side : family.topId;
        String bottom = family.bottomId.isEmpty() ? top : family.bottomId;
        if (family.modelType == ModBlocks.ModelType.LOG) {
            side = family.textureId.isEmpty() ? base : family.textureId;
            top = family.topId.isEmpty() ? base + "_top" : family.topId;
            bottom = family.bottomId.isEmpty() ? top : family.bottomId;
        } else if (family.modelType == ModBlocks.ModelType.CUBE_BOTTOM_TOP) {
            side = family.textureId.isEmpty() ? base + "_side" : family.textureId;
            top = family.topId.isEmpty() ? base + "_top" : family.topId;
            bottom = family.bottomId.isEmpty() ? base + "_bottom" : family.bottomId;
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("side", texture(source, side));
        result.put("top", texture(source, top));
        result.put("bottom", texture(source, bottom));
        result.put("particle", texture(source, side));
        return result;
    }

    private static String texture(Identifier source, String path) {
        return path.contains(":") ? path : source.getNamespace() + ":block/" + path;
    }

    private static JsonObject template(String parent, Map<String, String> textures) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        JsonObject encoded = new JsonObject();
        textures.forEach(encoded::addProperty);
        result.add("textures", encoded);
        return result;
    }

    private static JsonObject modelReference(String id) {
        JsonObject result = new JsonObject();
        result.addProperty("model", id);
        return result;
    }

    private static String canonicalModel(ModBlocks family) {
        Identifier source = BuiltInRegistries.BLOCK.getKey(family.parentBlock);
        if (source == null) throw new IllegalStateException("Missing canonical block identity for " + family);
        return source.getNamespace() + ":block/" + source.getPath();
    }

    /** Uses Mojang's production selector topology, changing only its model references. */
    private static JsonObject blockStateTemplate(String vanillaBlock, Map<String, String> replacements)
            throws IOException {
        String resource = "assets/minecraft/blockstates/" + vanillaBlock + ".json";
        InputStream stream = NativeMaterialResourceGenerator.class.getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("Canonical Minecraft blockstate is absent from the runtime classpath: "
                    + resource);
        }
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject state = JsonParser.parseReader(reader).getAsJsonObject();
            replaceModelReferences(state, replacements);
            return state;
        }
    }

    private static void replaceModelReferences(com.google.gson.JsonElement element,
            Map<String, String> replacements) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(value -> replaceModelReferences(value, replacements));
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        if (object.has("model") && object.get("model").isJsonPrimitive()) {
            String replacement = replacements.get(object.get("model").getAsString());
            if (replacement != null) object.addProperty("model", replacement);
        }
        object.entrySet().forEach(entry -> replaceModelReferences(entry.getValue(), replacements));
    }

    private static void writeLanguage(Path assets, Identifier id) throws IOException {
        Path target = assets.resolve("lang/en_us.json").normalize();
        requireContained(assets, target);
        JsonObject language = Files.exists(target)
                ? JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8)).getAsJsonObject()
                : new JsonObject();
        String key = "block." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        if (!language.has(key)) {
            language.addProperty(key, displayName(id.getPath()));
            write(target, language);
        }
    }

    private static String displayName(String path) {
        StringBuilder result = new StringBuilder();
        for (String word : path.split("_")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return result.toString();
    }

    private static Path blockState(Path assets, Identifier id) { return assets.resolve("blockstates").resolve(id.getPath() + ".json"); }
    private static Path blockModel(Path assets, Identifier id, String suffix) { return assets.resolve("models/block").resolve(id.getPath() + suffix + ".json"); }
    private static Path itemModel(Path assets, Identifier id) { return assets.resolve("models/item").resolve(id.getPath() + ".json"); }
    private static Path itemDefinition(Path assets, Identifier id) { return assets.resolve("items").resolve(id.getPath() + ".json"); }
    private static String model(Identifier id) { return id.getNamespace() + ":block/" + id.getPath(); }
    private static String item(Identifier id) { return id.getNamespace() + ":item/" + id.getPath(); }

    private static void writeIfAbsent(Path target, JsonObject contents) throws IOException {
        if (!Files.exists(target) && !isAuthoredOverlay(target)) write(target, contents);
    }

    /** The copied upstream snapshot intentionally excludes authored overrides.  Never recreate one. */
    private static boolean isAuthoredOverlay(Path generatedTarget) {
        Path normalized = generatedTarget.toAbsolutePath().normalize();
        if (!normalized.startsWith(generatedRoot)) {
            throw new IllegalArgumentException("Generated resource escapes its target root: " + generatedTarget);
        }
        Path relative = generatedRoot.relativize(normalized);
        return immutableOverlays.stream().anyMatch(root -> Files.isRegularFile(root.resolve(relative)));
    }

    private static void write(Path target, JsonObject contents) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, GSON.toJson(contents) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static void requireContained(Path root, Path target) {
        if (!target.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Generated resource escapes its target root: " + target);
        }
    }
}

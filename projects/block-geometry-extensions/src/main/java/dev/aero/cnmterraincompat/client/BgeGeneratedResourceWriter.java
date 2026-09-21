package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.ExternalMaterialFamilies;
import dev.aero.cnmterraincompat.ExternalMaterialStateBridge;
import dev.tazer.clutternomore.ClutterNoMore;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Writes BGE-generated client resources to both of CNM's real resource paths.
 *
 * <p>CNM's {@code AssetGenerator.write} mirrors only the {@code clutternomore} namespace.
 * BGE owns blocks in {@code cnm_terrain_slabs_compat}, so putting their JSON only in CNM's
 * in-memory pack leaves the runtime-generated pack without their blockstates, models, and item
 * definitions on a later client reload. Keep the two stores byte-equivalent and preserve each
 * resource's actual namespace.</p>
 */
final class BgeGeneratedResourceWriter {
    private BgeGeneratedResourceWriter() {}

    static void write(Identifier id, JsonElement json) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(json, "json");
        Optional<ExternalMaterialStateBridge> bridge = bridgeFor(id);
        if (bridge.isPresent() && bridge.get().requiresBridge()
                && !bridge.get().materialProperties().isEmpty()) {
            if (bridge.get().isBlinklamp() && isItem(id)) {
                // Enderscape's canonical inventory definition explicitly selects luminance 4.
                // The generic base model is intentionally absent, so an unsuffixed generated
                // selector icon would otherwise bake as missing before it can be displayed.
                writeRaw(id, MaterialStateResources.item(bridge.get(), json));
                return;
            }
            writeRaw(id, MaterialStateResources.blockState(bridge.get(), id, json));
            if (isModel(id)) {
                for (MaterialStateResources.StateVariant variant
                        : MaterialStateResources.modelVariants(bridge.get())) {
                    writeRaw(modelVariant(id, variant.modelSuffix()),
                            MaterialStateResources.model(bridge.get(), variant, json));
                }
            }
            return;
        }
        writeRaw(id, json);
    }

    private static void writeRaw(Identifier id, JsonElement json) {
        ClutterNoMore.RESOURCES.addJson(PackType.CLIENT_RESOURCES, id, json);
        // Generation also occurs while server-side GameTests build their logical resource view.
        // CNM's client config class is not safe to load there, and no client runtime pack exists.
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT
                || !runtimeAssetGenerationEnabled()) return;

        Path relative = Path.of(id.getPath());
        Path target = ClutterNoMore.pack.resolve("assets").resolve(id.getNamespace()).resolve(relative);
        Path parent = target.getParent();
        if (parent == null) throw new IllegalArgumentException("Generated resource has no parent: " + id);
        try {
            ClutterNoMore.writeFile(parent, target,
                    dev.tazer.clutternomore.common.data.CNMPackResources.serializeJson(json));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot mirror generated BGE resource " + id, exception);
        }
    }

    private static Optional<ExternalMaterialStateBridge> bridgeFor(Identifier resource) {
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            ExternalMaterialStateBridge bridge = binding.spec().materialStateBridge();
            if (!bridge.requiresBridge()) continue;
            for (Block block : binding.canonicalDerived()) {
                Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                if (blockId == null || !resource.getNamespace().equals(blockId.getNamespace())) continue;
                String root = blockId.getPath();
                String path = resource.getPath();
                if (path.equals("blockstates/" + root + ".json")
                        || path.startsWith("models/block/" + root)
                        || path.equals("items/" + root + ".json")) return Optional.of(bridge);
            }
        }
        return Optional.empty();
    }

    private static boolean isModel(Identifier id) { return id.getPath().startsWith("models/block/"); }
    private static boolean isItem(Identifier id) { return id.getPath().startsWith("items/"); }

    private static Identifier modelVariant(Identifier id, String suffix) {
        String path = id.getPath();
        return Identifier.fromNamespaceAndPath(id.getNamespace(),
                path.substring(0, path.length() - ".json".length()) + suffix + ".json");
    }

    /** JSON-only projection keeps material variants independent of every geometry selector. */
    private static final class MaterialStateResources {
        private MaterialStateResources() {}

        static JsonElement blockState(ExternalMaterialStateBridge bridge, Identifier id, JsonElement source) {
            if (!id.getPath().startsWith("blockstates/") || !source.isJsonObject()) return source;
            JsonObject root = source.getAsJsonObject().deepCopy();
            if (root.has("variants")) root.add("variants", variants(bridge, root.getAsJsonObject("variants")));
            if (root.has("multipart")) root.add("multipart", multipart(bridge, root.getAsJsonArray("multipart")));
            return root;
        }

        static JsonElement model(ExternalMaterialStateBridge bridge, StateVariant variant,
                JsonElement source) {
            JsonElement copy = source.deepCopy();
            if (bridge.isBlisteredMagnia()) {
                replace(copy, "enderscape:block/blistered_magnia",
                        "enderscape:block/blistered_magnia" + variant.modelSuffix());
            } else if (bridge.isBlinklamp()) {
                replace(copy, "enderscape:block/blinklamp",
                        "enderscape:block/blinklamp" + variant.modelSuffix());
            }
            return copy;
        }

        static JsonElement item(ExternalMaterialStateBridge bridge, JsonElement source) {
            JsonElement copy = source.deepCopy();
            if (bridge.isBlinklamp() && copy.isJsonObject()) {
                JsonObject root = copy.getAsJsonObject();
                if (root.has("model") && root.get("model").isJsonObject()) {
                    JsonObject model = root.getAsJsonObject("model");
                    if (model.has("model") && model.get("model").isJsonPrimitive()) {
                        model.addProperty("model", model.get("model").getAsString() + "_luminance4");
                    }
                }
            }
            return copy;
        }

        private static JsonObject variants(ExternalMaterialStateBridge bridge, JsonObject variants) {
            JsonObject projected = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
                for (StateVariant variant : states(bridge)) {
                    String key = entry.getKey().isEmpty() ? variant.selector()
                            : entry.getKey() + "," + variant.selector();
                    projected.add(key, selection(variant, entry.getValue()));
                }
            }
            return projected;
        }

        private static JsonArray multipart(ExternalMaterialStateBridge bridge, JsonArray source) {
            JsonArray projected = new JsonArray();
            for (JsonElement element : source) for (StateVariant variant : states(bridge)) {
                JsonObject part = element.getAsJsonObject().deepCopy();
                JsonObject when = part.has("when") && part.get("when").isJsonObject()
                        ? part.getAsJsonObject("when") : new JsonObject();
                String[] pair = variant.selector().split("=", 2);
                when.addProperty(pair[0], pair[1]);
                part.add("when", when);
                if (part.has("apply")) part.add("apply", selection(variant, part.get("apply")));
                projected.add(part);
            }
            return projected;
        }

        private static JsonElement selection(StateVariant variant, JsonElement source) {
            JsonElement copy = source.deepCopy();
            if (variant.modelSuffix() != null) rewriteModels(copy, variant.modelSuffix());
            return copy;
        }

        private static void rewriteModels(JsonElement element, String suffix) {
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("model") && object.get("model").isJsonPrimitive()) {
                    String model = object.get("model").getAsString();
                    object.addProperty("model", model + suffix);
                }
                object.entrySet().forEach(entry -> rewriteModels(entry.getValue(), suffix));
            } else if (element.isJsonArray()) for (JsonElement child : element.getAsJsonArray())
                rewriteModels(child, suffix);
        }

        private static void replace(JsonElement element, String expected, String replacement) {
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                for (String key : object.keySet().toArray(String[]::new)) {
                    JsonElement child = object.get(key);
                    if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()
                            && child.getAsString().equals(expected)) object.addProperty(key, replacement);
                    else replace(child, expected, replacement);
                }
            } else if (element.isJsonArray()) for (JsonElement child : element.getAsJsonArray())
                replace(child, expected, replacement);
        }

        private static java.util.List<StateVariant> states(ExternalMaterialStateBridge bridge) {
            if (bridge.isFixedMagnia()) {
                java.util.List<StateVariant> values = new java.util.ArrayList<>();
                for (int power = 0; power <= 15; power++) values.add(new StateVariant("power=" + power, null));
                return values;
            }
            if (bridge.isBlinklamp()) {
                java.util.List<StateVariant> values = new java.util.ArrayList<>();
                for (int luminance = 0; luminance <= 7; luminance++) {
                    int modelLevel = switch (luminance) {
                        case 0 -> 0;
                        case 1, 2 -> 1;
                        case 3, 4 -> 2;
                        case 5, 6 -> 3;
                        case 7 -> 4;
                        default -> throw new IllegalStateException("Unexpected Blinklamp luminance " + luminance);
                    };
                    values.add(new StateVariant("luminance=" + luminance,
                            "_luminance" + modelLevel));
                }
                return values;
            }
            return java.util.List.of(new StateVariant("polarity=none", null),
                    new StateVariant("polarity=alluring", "_alluring"),
                    new StateVariant("polarity=repulsive", "_repulsive"));
        }

        static java.util.List<StateVariant> modelVariants(ExternalMaterialStateBridge bridge) {
            return states(bridge).stream().filter(variant -> variant.modelSuffix() != null)
                    .collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toMap(StateVariant::modelSuffix, variant -> variant,
                                    (first, ignored) -> first, java.util.LinkedHashMap::new),
                            models -> java.util.List.copyOf(models.values())));
        }

        private record StateVariant(String selector, String modelSuffix) {}
    }

    /** CNM deliberately keeps its config library as a non-transitive runtime dependency. */
    private static boolean runtimeAssetGenerationEnabled() {
        try {
            Class<?> clientClass = Class.forName("dev.tazer.clutternomore.ClutterNoMoreClient");
            Object config = clientClass.getField("CLIENT_CONFIG").get(null);
            Object tracked = config.getClass().getField("RUNTIME_ASSET_GENERATION").get(config);
            Object value = tracked.getClass().getMethod("value").invoke(tracked);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read CNM runtime asset-generation setting", exception);
        }
    }
}

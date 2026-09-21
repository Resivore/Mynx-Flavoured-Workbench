package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.ExplicitShapeMapFamilies;
import dev.aero.cnmterraincompat.ExternalMaterialFamilies;
import dev.aero.cnmterraincompat.PrivateBeamFamilies;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Final item-only presentation pass over the exact user-visible ShapeMap catalog. */
public final class CatalogPreviewGeneratedResources {
    private static final String SUFFIX = "_bge_preview";

    private CatalogPreviewGeneratedResources() {}

    public static GenerationSummary generate(ResourceManager manager) {
        Map<Identifier, PreviewRole> written = new LinkedHashMap<>();
        int wrappers = 0;
        int definitions = 0;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            // This is the same exact nine-item sequence installed into ShapeMap. Positions 2-5
            // are Stair, Wall, Vertical Slab and Step respectively.
            List<Item> visible = ExplicitShapeMapFamilies.variationItems(profile);
            wrappers += write(manager, profile, block(visible.get(2)), PreviewRole.STAIRS, written);
            wrappers += write(manager, profile, block(visible.get(3)), PreviewRole.WALL, written);
            wrappers += write(manager, profile, block(visible.get(4)), PreviewRole.VERTICAL_SLAB, written);
            wrappers += write(manager, profile, block(visible.get(5)), PreviewRole.STEP, written);
            definitions += 4;
        }
        return new GenerationSummary(wrappers, definitions, Map.copyOf(written));
    }

    private static int write(ResourceManager manager, NibaruMaterialProfile profile, Block block,
            PreviewRole role, Map<Identifier, PreviewRole> written) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
            throw new IllegalStateException("Visible ShapeMap preview block is unregistered: " + block);
        }
        PreviewRole previous = written.putIfAbsent(id, role);
        if (previous != null) {
            if (previous != role) throw new IllegalStateException(
                    "Visible ShapeMap item has two preview roles: " + id + " " + previous + "/" + role);
            return 0;
        }

        if (role == PreviewRole.WALL && PrivateBeamFamilies.isPrivateBeam(profile.canonicalParentId())) {
            // BBB's Pale Oak Beam reference deliberately uses its authored post model directly.
            write(item(id), definition(manager, profile, id, model(id) + "_post"));
            return 0;
        }

        String fallback = role == PreviewRole.WALL ? model(id) + "_inventory" : model(id);
        boolean bridged = ExternalMaterialFamilies.fromSource(profile.canonicalParentId())
                .map(binding -> binding.spec().materialStateBridge().requiresBridge()).orElse(false);
        String parent = bridged ? fallback : existingItemModel(manager, id, fallback);
        String preview = model(id) + SUFFIX;
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("parent", parent);
        JsonObject display = new JsonObject();
        double translation = role == PreviewRole.VERTICAL_SLAB || role == PreviewRole.STEP ? 1.75 : 0;
        display.add("gui", transform(30, 135, 0, translation, 0, 0, .625, .625, .625));
        wrapper.add("display", display);
        write(modelResource(id), wrapper);
        write(item(id), definition(manager, profile, id, preview));
        return 1;
    }

    private static Block block(Item item) {
        if (item instanceof BlockItem blockItem) return blockItem.getBlock();
        throw new IllegalStateException("Visible ShapeMap geometry is not a BlockItem: "
                + BuiltInRegistries.ITEM.getKey(item));
    }

    private static String existingItemModel(ResourceManager manager, Identifier id, String fallback) {
        JsonObject definition = read(manager, item(id));
        if (definition == null || !definition.has("model") || !definition.get("model").isJsonObject()) {
            return fallback;
        }
        JsonObject model = definition.getAsJsonObject("model");
        if (!model.has("model") || !model.get("model").isJsonPrimitive()) return fallback;
        String target = model.get("model").getAsString();
        // A resource reload can see this generated pack from the previous pass. Unwrap it so the
        // new wrapper never parents itself.
        for (int depth = 0; depth < 8 && target.contains(SUFFIX); depth++) {
            Identifier targetId = Identifier.parse(target);
            JsonObject previous = read(manager, Identifier.fromNamespaceAndPath(targetId.getNamespace(),
                    "models/" + targetId.getPath() + ".json"));
            if (previous == null || !previous.has("parent")) return fallback;
            target = previous.get("parent").getAsString();
        }
        return target.contains(SUFFIX) ? fallback : target;
    }

    private static JsonObject definition(ResourceManager manager, NibaruMaterialProfile profile,
            Identifier id, String preview) {
        JsonObject existing = read(manager, item(id));
        if (existing != null && existing.has("model") && existing.get("model").isJsonObject()) {
            JsonObject copy = existing.deepCopy();
            JsonObject model = copy.getAsJsonObject("model");
            if (model.has("model") && model.get("model").isJsonPrimitive()) {
                model.addProperty("model", preview);
                return copy;
            }
        }
        return GeneratedItemModelSupport.itemDefinition(manager, profile, preview);
    }

    private static JsonObject read(ResourceManager manager, Identifier resourceId) {
        Resource resource = manager.getResource(resourceId).orElse(null);
        if (resource == null) return null;
        try (var reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot read preview source " + resourceId, exception);
        }
    }

    private static JsonObject transform(double rx, double ry, double rz,
            double tx, double ty, double tz, double sx, double sy, double sz) {
        JsonObject transform = new JsonObject();
        transform.add("rotation", numbers(rx, ry, rz));
        transform.add("translation", numbers(tx, ty, tz));
        transform.add("scale", numbers(sx, sy, sz));
        return transform;
    }

    private static JsonArray numbers(double... values) {
        JsonArray result = new JsonArray();
        for (double value : values) result.add(value);
        return result;
    }

    private static String model(Identifier id) { return id.getNamespace() + ":block/" + id.getPath(); }
    private static Identifier item(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json");
    }
    private static Identifier modelResource(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(),
                "models/block/" + id.getPath() + SUFFIX + ".json");
    }
    private static void write(Identifier id, JsonObject json) { BgeGeneratedResourceWriter.write(id, json); }

    public enum PreviewRole { STAIRS, WALL, VERTICAL_SLAB, STEP }

    public record GenerationSummary(int wrapperCount, int itemDefinitionCount,
            Map<Identifier, PreviewRole> roles) {}
}

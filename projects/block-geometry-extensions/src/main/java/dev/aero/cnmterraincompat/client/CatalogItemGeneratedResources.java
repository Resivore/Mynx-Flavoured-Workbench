package dev.aero.cnmterraincompat.client;

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

/**
 * Final item-only catalog pass: restores C92 model chains and supplies Beam-specific material UVs.
 *
 * <p>C93 wrote {@code *_bge_preview} wrappers into CNM's persistent generated pack. Merely
 * removing that writer would leave those definitions active after an upgrade, so this pass
 * deliberately unwraps historical targets and rewrites every visible catalog item to its C92
 * model chain. It emits no catalog display transform and never writes a blockstate or placed
 * model. Beam Vertical Slabs and Steps are the sole model exception: their item definitions use
 * dedicated inventory-only models over BBB's axis-aware placed geometry and carry the exact C92
 * CNM display members.</p>
 */
public final class CatalogItemGeneratedResources {
    private static final String HISTORICAL_PREVIEW_SUFFIX = "_bge_preview";

    private CatalogItemGeneratedResources() {}

    public static GenerationSummary generate(ResourceManager manager) {
        Map<Identifier, ItemRole> written = new LinkedHashMap<>();
        int itemDefinitions = 0;
        int beamModels = 0;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            List<Item> visible = ExplicitShapeMapFamilies.variationItems(profile);
            itemDefinitions += write(manager, profile, block(visible.get(2)), ItemRole.STAIRS, written);
            itemDefinitions += write(manager, profile, block(visible.get(3)), ItemRole.WALL, written);
            int vertical = write(manager, profile, block(visible.get(4)), ItemRole.VERTICAL_SLAB, written);
            int step = write(manager, profile, block(visible.get(5)), ItemRole.STEP, written);
            itemDefinitions += vertical + step;
            if (BeamItemModelContract.applies(profile.canonicalParentId())) {
                beamModels += vertical + step;
            }
        }
        return new GenerationSummary(itemDefinitions, beamModels, Map.copyOf(written));
    }

    private static int write(ResourceManager manager, NibaruMaterialProfile profile, Block block,
            ItemRole role, Map<Identifier, ItemRole> written) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
            throw new IllegalStateException("Visible ShapeMap item block is unregistered: " + block);
        }
        ItemRole previous = written.putIfAbsent(id, role);
        if (previous != null) {
            if (previous != role) throw new IllegalStateException(
                    "Visible ShapeMap item has two catalog roles: " + id + " " + previous + "/" + role);
            return 0;
        }

        String target;
        if ((role == ItemRole.VERTICAL_SLAB || role == ItemRole.STEP)
                && BeamItemModelContract.applies(profile.canonicalParentId())) {
            target = model(id) + BeamItemModelContract.MODEL_SUFFIX;
            JsonObject itemModel = role == ItemRole.VERTICAL_SLAB
                    ? BeamItemModelContract.verticalSlab(id)
                    : BeamItemModelContract.step(id);
            write(modelResource(id, BeamItemModelContract.MODEL_SUFFIX), itemModel);
        } else if (role == ItemRole.WALL
                && PrivateBeamFamilies.isPrivateBeam(profile.canonicalParentId())) {
            // C93 corrected these three private walls to BBB's authored post-only item topology.
            target = model(id) + "_post";
        } else {
            String fallback = role == ItemRole.WALL ? model(id) + "_inventory" : model(id);
            target = historicalItemTarget(manager, id, fallback);
        }

        // Blinklamp's writer adds its canonical luminance-4 suffix. Historical C93 definitions
        // already contain it, so normalize before passing through the same production writer.
        boolean blinklamp = ExternalMaterialFamilies.fromSource(profile.canonicalParentId())
                .map(binding -> binding.spec().materialStateBridge().isBlinklamp()).orElse(false);
        if (blinklamp && target.endsWith("_luminance4")) {
            target = target.substring(0, target.length() - "_luminance4".length());
        }
        write(item(id), definition(manager, profile, id, target));
        return 1;
    }

    private static Block block(Item item) {
        if (item instanceof BlockItem blockItem) return blockItem.getBlock();
        throw new IllegalStateException("Visible ShapeMap geometry is not a BlockItem: "
                + BuiltInRegistries.ITEM.getKey(item));
    }

    private static String historicalItemTarget(ResourceManager manager, Identifier id, String fallback) {
        JsonObject definition = read(manager, item(id));
        String target = itemTarget(definition);
        if (target == null) return fallback;
        for (int depth = 0; depth < 8 && target.contains(HISTORICAL_PREVIEW_SUFFIX); depth++) {
            Identifier targetId = Identifier.parse(target);
            JsonObject wrapper = read(manager, Identifier.fromNamespaceAndPath(targetId.getNamespace(),
                    "models/" + targetId.getPath() + ".json"));
            if (wrapper == null || !wrapper.has("parent")
                    || !wrapper.get("parent").isJsonPrimitive()) return fallback;
            target = wrapper.get("parent").getAsString();
        }
        return target.contains(HISTORICAL_PREVIEW_SUFFIX) ? fallback : target;
    }

    private static JsonObject definition(ResourceManager manager, NibaruMaterialProfile profile,
            Identifier id, String target) {
        JsonObject existing = read(manager, item(id));
        if (existing != null && existing.has("model") && existing.get("model").isJsonObject()) {
            JsonObject copy = existing.deepCopy();
            JsonObject model = copy.getAsJsonObject("model");
            if (model.has("model") && model.get("model").isJsonPrimitive()) {
                model.addProperty("model", target);
                return copy;
            }
        }
        return GeneratedItemModelSupport.itemDefinition(manager, profile, target);
    }

    private static String itemTarget(JsonObject definition) {
        if (definition == null || !definition.has("model")
                || !definition.get("model").isJsonObject()) return null;
        JsonObject model = definition.getAsJsonObject("model");
        return model.has("model") && model.get("model").isJsonPrimitive()
                ? model.get("model").getAsString() : null;
    }

    private static JsonObject read(ResourceManager manager, Identifier resourceId) {
        Resource resource = manager.getResource(resourceId).orElse(null);
        if (resource == null) return null;
        try (var reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot read catalog item source " + resourceId, exception);
        }
    }

    private static String model(Identifier id) { return id.getNamespace() + ":block/" + id.getPath(); }
    private static Identifier item(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json");
    }
    private static Identifier modelResource(Identifier id, String suffix) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(),
                "models/block/" + id.getPath() + suffix + ".json");
    }
    private static void write(Identifier id, JsonObject json) { BgeGeneratedResourceWriter.write(id, json); }

    public enum ItemRole { STAIRS, WALL, VERTICAL_SLAB, STEP }

    public record GenerationSummary(int itemDefinitionCount, int beamItemModelCount,
            Map<Identifier, ItemRole> roles) {}
}

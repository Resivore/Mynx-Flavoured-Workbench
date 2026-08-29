package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.tazer.clutternomore.common.data.DataGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Common-safe generated loot and tag resources for BGE-owned Layer blocks. */
public final class LayerGeneratedData {
    private static final Identifier LAYERS_TAG = Identifier.fromNamespaceAndPath(
            CnmTerrainCompat.MOD_ID, "layers");

    private LayerGeneratedData() {}

    /** Called once after all Layer blocks/items are registered, including on dedicated servers. */
    public static GenerationSummary generate() {
        List<Binding> bindings = bindings();
        LinkedHashMap<Identifier, LinkedHashSet<Identifier>> blockTags = new LinkedHashMap<>();
        LinkedHashMap<Identifier, LinkedHashSet<Identifier>> itemTags = new LinkedHashMap<>();
        addTag(blockTags, LAYERS_TAG, bindings);
        addTag(itemTags, LAYERS_TAG, bindings);

        for (Binding binding : bindings) {
            DataGenerator.writeServerData(lootResource(binding.id()), lootTable(binding));
            for (TagKey<Block> tag : binding.profile().derivedBlockTags()) {
                addTag(blockTags, tag.location(), binding.id());
            }
        }
        writeTags("block", blockTags);
        writeTags("item", itemTags);
        return new GenerationSummary(bindings.size(), blockTags.size(), itemTags.size());
    }

    /** Exact registered Layer bindings in canonical Nibaru family order. */
    public static List<Binding> bindings() {
        List<Binding> result = new ArrayList<>();
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            Block block = NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.LAYER)
                    .orElseThrow(() -> new IllegalStateException("Missing registered BGE Layer for "
                            + profile.canonicalParentId()));
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
                throw new IllegalStateException("Unregistered BGE Layer for " + profile.canonicalParentId());
            }
            if (block == profile.canonicalParent()) {
                throw new IllegalStateException("Canonical material was rebound as its own Layer: " + id);
            }
            result.add(new Binding(profile, block, id));
        }
        return List.copyOf(result);
    }

    /**
     * State-count-preserving drop table. Typed dirt-surface/path transitions drop the matching
     * Dirt Layer, just as their canonical Nibaru geometries do; other materials drop themselves.
     */
    public static JsonObject lootTable(Binding binding) {
        Identifier layer = binding.id();
        Identifier drop = dropItem(binding);
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", drop.toString());
        JsonArray functions = new JsonArray();
        for (int count = 1; count <= 4; count++) {
            JsonObject setCount = new JsonObject();
            setCount.addProperty("function", "minecraft:set_count");
            setCount.addProperty("count", count);
            JsonObject condition = new JsonObject();
            condition.addProperty("condition", "minecraft:block_state_property");
            condition.addProperty("block", layer.toString());
            JsonObject properties = new JsonObject();
            properties.addProperty("layers", Integer.toString(count));
            condition.add("properties", properties);
            JsonArray conditions = new JsonArray();
            conditions.add(condition);
            setCount.add("conditions", conditions);
            functions.add(setCount);
        }
        JsonObject explosionDecay = new JsonObject();
        explosionDecay.addProperty("function", "minecraft:explosion_decay");
        functions.add(explosionDecay);
        entry.add("functions", functions);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        root.add("pools", pools);
        return root;
    }

    /** Resolves the exact geometry-preserving drop target from canonical typed transitions. */
    public static Identifier dropItem(Binding binding) {
        return binding.profile().transitions().stream()
                .filter(transition -> transition.type() == MaterialTransition.Type.DROP_BASE)
                .findFirst()
                .flatMap(transition -> NibaruMaterialProfiles.fromFamily(transition.target()))
                .flatMap(profile -> NibaruProviderAdapter.derived(
                        profile, DerivedGeometrySupport.Geometry.LAYER))
                .map(BuiltInRegistries.BLOCK::getKey)
                .orElse(binding.id());
    }

    public static JsonObject tagFile(Iterable<Identifier> values) {
        JsonObject root = new JsonObject();
        root.addProperty("replace", false);
        JsonArray encoded = new JsonArray();
        values.forEach(id -> encoded.add(id.toString()));
        root.add("values", encoded);
        return root;
    }

    private static void writeTags(String registryPath,
            Map<Identifier, LinkedHashSet<Identifier>> tags) {
        tags.forEach((tag, values) -> DataGenerator.writeServerData(
                Identifier.fromNamespaceAndPath(tag.getNamespace(),
                        "tags/" + registryPath + "/" + tag.getPath() + ".json"),
                tagFile(values)));
    }

    private static void addTag(Map<Identifier, LinkedHashSet<Identifier>> tags,
            Identifier tag, List<Binding> bindings) {
        LinkedHashSet<Identifier> values = tags.computeIfAbsent(tag, ignored -> new LinkedHashSet<>());
        bindings.forEach(binding -> values.add(binding.id()));
    }

    private static void addTag(Map<Identifier, LinkedHashSet<Identifier>> tags,
            Identifier tag, Identifier value) {
        tags.computeIfAbsent(tag, ignored -> new LinkedHashSet<>()).add(value);
    }

    private static Identifier lootResource(Identifier layer) {
        return Identifier.fromNamespaceAndPath(layer.getNamespace(),
                "loot_table/blocks/" + layer.getPath() + ".json");
    }

    public record Binding(NibaruMaterialProfile profile, Block block, Identifier id) {
        public Binding {
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(id, "id");
        }
    }

    public record GenerationSummary(int familyCount, int blockTagCount, int itemTagCount) {}
}

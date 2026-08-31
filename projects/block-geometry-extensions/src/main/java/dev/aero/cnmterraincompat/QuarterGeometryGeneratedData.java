package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.tazer.clutternomore.common.data.DataGenerator;
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

/** Common-safe generated loot and tags for BGE Corner and Quarter Column. */
public final class QuarterGeometryGeneratedData {
    private QuarterGeometryGeneratedData() {}

    public static GenerationSummary generate() {
        LinkedHashMap<Identifier, LinkedHashSet<Identifier>> blockTags = new LinkedHashMap<>();
        LinkedHashMap<Identifier, LinkedHashSet<Identifier>> itemTags = new LinkedHashMap<>();
        int familyCount = 0;

        // DataGenerator retains one value per resource key, so this final BGE tag writer
        // must preserve the Layer values written earlier in the same generation pass.
        // Role-specific tags remain owned by their individual generators.
        for (LayerGeneratedData.Binding binding : LayerGeneratedData.bindings()) {
            for (TagKey<Block> tag : binding.profile().derivedBlockTags()) {
                addTag(blockTags, tag.location(), binding.id());
            }
        }

        for (BgeGeometryRole role : List.of(BgeGeometryRole.CORNER, BgeGeometryRole.QUARTER_COLUMN)) {
            List<Binding> bindings = bindings(role);
            familyCount += bindings.size();
            Identifier roleTag = Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                    role == BgeGeometryRole.CORNER ? "corners" : "quarter_columns");
            addTag(blockTags, roleTag, bindings);
            addTag(itemTags, roleTag, bindings);
            for (Binding binding : bindings) {
                DataGenerator.writeServerData(lootResource(binding.id()), lootTable(binding));
                for (TagKey<Block> tag : binding.profile().derivedBlockTags()) {
                    addTag(blockTags, tag.location(), binding.id());
                }
            }
        }

        writeTags("block", blockTags);
        writeTags("item", itemTags);
        return new GenerationSummary(familyCount, blockTags.size(), itemTags.size());
    }

    /** Exact registered bindings in canonical Nibaru family order. */
    public static List<Binding> bindings(BgeGeometryRole role) {
        if (role != BgeGeometryRole.CORNER && role != BgeGeometryRole.QUARTER_COLUMN) {
            throw new IllegalArgumentException("Not a quarter-volume BGE role: " + role);
        }
        List<Binding> result = new ArrayList<>();
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            Block block = NibaruProviderAdapter.derived(profile, role)
                    .orElseThrow(() -> new IllegalStateException("Missing registered BGE " + role
                            + " for " + profile.canonicalParentId()));
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
                throw new IllegalStateException("Unregistered BGE " + role + " for "
                        + profile.canonicalParentId());
            }
            if (block == profile.canonicalParent()) {
                throw new IllegalStateException("Canonical material rebound as " + role + ": " + id);
            }
            result.add(new Binding(profile, role, block, id));
        }
        return List.copyOf(result);
    }

    /** One funded blockspace always returns one canonical source block. */
    public static JsonObject lootTable(Binding binding) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", dropItem(binding).toString());
        JsonArray functions = new JsonArray();
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

    public static Identifier dropItem(Binding binding) {
        return binding.profile().transitions().stream()
                .filter(transition -> transition.type() == MaterialTransition.Type.DROP_BASE)
                .findFirst()
                .flatMap(transition -> NibaruMaterialProfiles.fromFamily(transition.target()))
                .map(NibaruMaterialProfile::canonicalParentId)
                .orElse(binding.profile().canonicalParentId());
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

    private static Identifier lootResource(Identifier shape) {
        return Identifier.fromNamespaceAndPath(shape.getNamespace(),
                "loot_table/blocks/" + shape.getPath() + ".json");
    }

    public record Binding(NibaruMaterialProfile profile, BgeGeometryRole role, Block block, Identifier id) {
        public Binding {
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(id, "id");
        }
    }

    public record GenerationSummary(int familyCount, int blockTagCount, int itemTagCount) {}
}

package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.tazer.clutternomore.common.data.DataGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Common-safe late data pass for external standard geometry and its material tags. */
public final class ExternalMaterialGeneratedData {
    private ExternalMaterialGeneratedData() {}

    public static GenerationSummary generate() {
        LinkedHashMap<Identifier, LinkedHashSet<Identifier>> blockTags = new LinkedHashMap<>();
        LinkedHashMap<Identifier, LinkedHashSet<Identifier>> itemTags = new LinkedHashMap<>();
        int lootCount = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            Map<String, Block> roles = binding.roles();
            for (Map.Entry<String, Block> role : roles.entrySet()) {
                if (role.getKey().equals("block")) continue;
                Identifier id = registeredId(role.getValue());
                for (TagKey<Block> tag : binding.profile().derivedBlockTags()) add(blockTags, tag.location(), id);
            }
            addRole(blockTags, itemTags, "minecraft", "slabs", roles.get("slab"));
            addRole(blockTags, itemTags, "minecraft", "stairs", roles.get("stairs"));
            addRole(blockTags, itemTags, "minecraft", "walls", roles.get("wall"));
            addRole(blockTags, itemTags, "clutternomore", "vertical_slabs", roles.get("vertical_slab"));
            addRole(blockTags, itemTags, "clutternomore", "steps", roles.get("step"));

            for (String role : List.of("slab", "stairs", "wall", "vertical_slab", "step")) {
                // Provider-native roles retain provider-owned loot and data. BGE publishes only
                // the roles it actually registered for this exact canonical source variant.
                if (!binding.isGeneratedRole(role)) continue;
                Block block = roles.get(role);
                Identifier id = registeredId(block);
                DataGenerator.writeServerData(Identifier.fromNamespaceAndPath(id.getNamespace(),
                        "loot_table/blocks/" + id.getPath() + ".json"),
                        selfDrop(id, role.equals("slab") ? "type" :
                                role.equals("vertical_slab") ? "double" : null));
                lootCount++;
            }
        }

        // Quarter/Layer writers also publish these keys. Preserve every native entry when this
        // later external pass adds standard roles to the same material tag.
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            for (BgeGeometryRole role : List.of(BgeGeometryRole.LAYER,
                    BgeGeometryRole.CORNER, BgeGeometryRole.QUARTER_COLUMN)) {
                Block block = NibaruProviderAdapter.derived(profile, role).orElse(null);
                if (block == null) continue;
                for (TagKey<Block> tag : profile.derivedBlockTags()) add(blockTags, tag.location(), registeredId(block));
            }
        }
        writeTags("block", blockTags);
        writeTags("item", itemTags);
        return new GenerationSummary(ExternalMaterialFamilies.all().size(), lootCount,
                blockTags.size(), itemTags.size());
    }

    public static JsonObject selfDrop(Identifier id, String doubledProperty) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", id.toString());
        if (doubledProperty != null) {
            JsonObject function = new JsonObject();
            function.addProperty("function", "minecraft:set_count");
            function.addProperty("count", 2);
            JsonObject condition = new JsonObject();
            condition.addProperty("condition", "minecraft:block_state_property");
            condition.addProperty("block", id.toString());
            JsonObject properties = new JsonObject();
            properties.addProperty(doubledProperty, doubledProperty.equals("type") ? "double" : "true");
            condition.add("properties", properties);
            JsonArray conditions = new JsonArray();
            conditions.add(condition);
            function.add("conditions", conditions);
            JsonArray functions = new JsonArray();
            functions.add(function);
            entry.add("functions", functions);
        }
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(survives);
        pool.add("conditions", conditions);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        root.add("pools", pools);
        return root;
    }

    private static void addRole(Map<Identifier, LinkedHashSet<Identifier>> blockTags,
            Map<Identifier, LinkedHashSet<Identifier>> itemTags,
            String namespace, String path, Block block) {
        Identifier tag = Identifier.fromNamespaceAndPath(namespace, path);
        Identifier value = registeredId(block);
        add(blockTags, tag, value);
        add(itemTags, tag, value);
    }

    private static Identifier registeredId(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey()))
            throw new IllegalStateException("Unregistered external material geometry " + block);
        return id;
    }

    private static void writeTags(String registry,
            Map<Identifier, LinkedHashSet<Identifier>> tags) {
        tags.forEach((tag, values) -> DataGenerator.writeServerData(
                Identifier.fromNamespaceAndPath(tag.getNamespace(),
                        "tags/" + registry + "/" + tag.getPath() + ".json"),
                LayerGeneratedData.tagFile(values)));
    }

    private static void add(Map<Identifier, LinkedHashSet<Identifier>> tags,
            Identifier tag, Identifier value) {
        tags.computeIfAbsent(tag, ignored -> new LinkedHashSet<>()).add(value);
    }

    public record GenerationSummary(int familyCount, int lootTableCount,
            int blockTagCount, int itemTagCount) {}
}

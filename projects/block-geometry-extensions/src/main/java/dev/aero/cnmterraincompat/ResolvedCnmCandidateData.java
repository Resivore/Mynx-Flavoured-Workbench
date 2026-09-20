package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.tazer.clutternomore.common.data.DataGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server-data closure for a CNM family that could not be typed until ShapeMap selected its
 * canonical member.  It deliberately has no material-tag inference: only BGE's explicit role
 * tags and the exact resolved canonical drop are valid without a material profile. The existing
 * role tags intentionally remain a catalog-only index: adding a profile-free component there
 * would falsely advertise it as a typed Nibaru material.
 */
final class ResolvedCnmCandidateData {
    private ResolvedCnmCandidateData() {}

    static GenerationSummary generate() {
        List<CnmShapeMapCandidateBridge.ResolvedGenericFamily> families =
                CnmShapeMapCandidateBridge.resolvedGenericFamilies();
        // Do not replace the established static tag resources when CNM has no currently
        // profile-free component.  In that normal case the early catalog pass is authoritative.
        if (families.isEmpty()) return new GenerationSummary(0, 0);
        for (CnmShapeMapCandidateBridge.ResolvedGenericFamily family : families) {
            family.roles().values().forEach(role -> DataGenerator.writeServerData(lootResource(role),
                    canonicalDrop(family.canonicalParent())));
            family.wall().ifPresent(wall -> DataGenerator.writeServerData(lootResource(wall),
                    canonicalDrop(family.canonicalParent())));
        }
        int walls = (int) families.stream().filter(family -> family.wall().isPresent()).count();
        return new GenerationSummary(families.size(), families.size() * 3 + walls);
    }

    static JsonObject canonicalDrop(Identifier canonical) {
        if (!BuiltInRegistries.BLOCK.containsKey(canonical)) {
            throw new IllegalStateException("Resolved CNM canonical source is not a registered block: "
                    + canonical);
        }
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", canonical.toString());
        JsonObject decay = new JsonObject();
        decay.addProperty("function", "minecraft:explosion_decay");
        JsonArray functions = new JsonArray();
        functions.add(decay);
        entry.add("functions", functions);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        root.add("pools", pools);
        return root;
    }

    private static Identifier lootResource(Identifier block) {
        return Identifier.fromNamespaceAndPath(block.getNamespace(),
                "loot_table/blocks/" + block.getPath() + ".json");
    }

    record GenerationSummary(int familyCount, int lootTableCount) {}
}

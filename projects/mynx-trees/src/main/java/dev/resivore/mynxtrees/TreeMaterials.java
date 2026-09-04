package dev.resivore.mynxtrees;

import com.google.gson.*;

/** Pure transformation isolated for regression checks against target-version data. */
final class TreeMaterials {
    static void transform(JsonObject feature, String species) {
        String type = feature.get("type").getAsString();
        if (!type.equals("minecraft:tree") && !(species.equals("silver_birch") && type.equals("minecraft:fallen_tree")))
            throw new IllegalStateException("Mynx Trees requires a tree/fallen-tree source; found " + type);
        if (!species.equals("cherry")) replaceMaterials(feature, species);
        if (species.equals("silver_birch")) return;
        JsonArray decorators = feature.getAsJsonObject("config").getAsJsonArray("decorators");
        boolean nemo = false;
        for (int i = decorators.size()-1; i >= 0; --i) {
            String id = decorators.get(i).getAsJsonObject().get("type").getAsString();
            if (id.equals("nemos-blooming-blossom:cherry_tree_decorator")) {
                nemo = true;
                if (species.equals("wisteria")) decorators.remove(i);
            }
            if (id.equals("mynx_trees:grove_flowers")) decorators.remove(i);
        }
        // Ordinary cherry keeps its loaded Nemo behavior. Wisteria never runs Nemo's pink-petal path.
        if (species.equals("wisteria") || !nemo) {
            JsonObject decoration = new JsonObject(); decoration.addProperty("type", "mynx_trees:grove_flowers");
            decoration.addProperty("wisteria", species.equals("wisteria")); decorators.add(decoration);
        }
    }
    private static void replaceMaterials(JsonElement element, String species) {
        if (element.isJsonObject()) {
            var obj = element.getAsJsonObject();
            for (var entry : obj.entrySet()) {
                JsonElement value = entry.getValue();
                if (entry.getKey().equals("Name") && value.isJsonPrimitive()) {
                    String old = value.getAsString(); String from = species.equals("silver_birch") ? "birch" : "cherry";
                    if (old.equals("minecraft:"+from+"_log")) entry.setValue(new JsonPrimitive("mynx_trees:"+species+"_log"));
                    else if (old.equals("minecraft:"+from+"_leaves")) entry.setValue(new JsonPrimitive("mynx_trees:"+species+"_leaves"));
                } else replaceMaterials(value, species);
            }
        } else if (element.isJsonArray()) for (JsonElement value : element.getAsJsonArray()) replaceMaterials(value, species);
    }
}

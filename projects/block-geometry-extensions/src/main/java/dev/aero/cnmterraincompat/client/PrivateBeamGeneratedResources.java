package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.ExternalMaterialFamilies;
import dev.aero.cnmterraincompat.PrivateBeamFamilies;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** Runtime JSON for BGE-owned Beam roots; the PNGs themselves are supplied only by private build assembly. */
final class PrivateBeamGeneratedResources {
    private PrivateBeamGeneratedResources() {}

    static void generate(ResourceManager manager) {
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            if (!PrivateBeamFamilies.isPrivateBeam(binding.spec().id())) continue;
            Identifier id = BuiltInRegistries.BLOCK.getKey(binding.source());
            NibaruMaterialProfile profile = binding.profile();
            String model = id.getNamespace() + ":block/" + id.getPath();
            JsonObject variants = new JsonObject();
            variants.add("axis=y", selection(model, 0, 0));
            variants.add("axis=z", selection(model, 90, 0));
            variants.add("axis=x", selection(model, 90, 90));
            JsonObject blockState = new JsonObject();
            blockState.add("variants", variants);
            write(blockState(id), blockState);

            JsonObject root = new JsonObject();
            root.addProperty("parent", "minecraft:block/cube_column");
            JsonObject textures = new JsonObject();
            textures.addProperty("side", profile.textureRoles().side());
            textures.addProperty("end", profile.textureRoles().top());
            textures.addProperty("particle", profile.textureRoles().side());
            root.add("textures", textures);
            write(model(id), root);
            write(item(id), GeneratedItemModelSupport.itemDefinition(manager, profile, model));
        }
    }

    private static JsonObject selection(String model, int x, int y) {
        JsonObject result = new JsonObject();
        result.addProperty("model", model);
        if (x != 0) result.addProperty("x", x);
        if (y != 0) result.addProperty("y", y);
        return result;
    }

    private static Identifier blockState(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "blockstates/" + id.getPath() + ".json");
    }
    private static Identifier model(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "models/block/" + id.getPath() + ".json");
    }
    private static Identifier item(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json");
    }
    private static void write(Identifier id, JsonObject json) { BgeGeneratedResourceWriter.write(id, json); }
}

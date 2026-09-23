package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonObject;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;

/** Resolves the optional Bookshelf end texture supplied by the active resource packs. */
final class BookshelfResourceTextures {
    private static final Identifier BOOKSHELF = Identifier.parse("minecraft:bookshelf");
    private static final Identifier END_TEXTURE = Identifier.parse(
            "minecraft:textures/block/bookshelf_top.png");
    private static final String PACK_END = "minecraft:block/bookshelf_top";
    private static final String VANILLA_END = "minecraft:block/oak_planks";

    private BookshelfResourceTextures() {}

    static JsonObject apply(ResourceProvider manager, NibaruMaterialProfile profile, JsonObject model) {
        if (!BOOKSHELF.equals(profile.canonicalParentId())) return model;
        return apply(manager.getResource(END_TEXTURE).isPresent(), profile, model);
    }

    /** A pure seam for controlled pack-present and pack-absent resource checks. */
    static JsonObject apply(boolean endTexturePresent, NibaruMaterialProfile profile, JsonObject model) {
        if (!endTexturePresent || !BOOKSHELF.equals(profile.canonicalParentId())) return model;
        JsonObject textures = model.getAsJsonObject("textures");
        if (textures == null) return model;
        // Older Bookshelf forms are overridden by Matcha Overlays to use this exact path.
        // New generated forms select it only while a pack supplies the PNG. Their canonical
        // material profile remains oak_planks, so removing the pack restores valid vanilla UVs.
        for (String role : new String[]{"top", "bottom"}) {
            if (textures.has(role) && VANILLA_END.equals(textures.get(role).getAsString())) {
                textures.addProperty(role, PACK_END);
            }
        }
        return model;
    }
}

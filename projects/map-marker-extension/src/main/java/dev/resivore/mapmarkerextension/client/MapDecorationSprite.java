package dev.resivore.mapmarkerextension.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

public final class MapDecorationSprite {
    private MapDecorationSprite() {
    }

    public static TextureAtlasSprite get(String decorationAssetId) {
        return Minecraft.getInstance()
            .getAtlasManager()
            .getAtlasOrThrow(AtlasIds.MAP_DECORATIONS)
            .getSprite(Identifier.parse(decorationAssetId));
    }
}

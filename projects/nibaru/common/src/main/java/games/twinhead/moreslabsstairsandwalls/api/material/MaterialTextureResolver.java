package games.twinhead.moreslabsstairsandwalls.api.material;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;

/** Pure visual identity resolution; material transition targets are intentionally not consulted. */
public final class MaterialTextureResolver {
    private MaterialTextureResolver() {}

    public static String uniformTextureId(ModBlocks family) {
        return family.textureId == null || family.textureId.isEmpty()
                ? BuiltInRegistries.BLOCK.getKey(family.parentBlock).getPath()
                : family.textureId;
    }
}

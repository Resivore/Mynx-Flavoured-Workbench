package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class CnmTerrainCompat implements ModInitializer {
    public static final String MOD_ID = "cnm_terrain_slabs_compat";
    public static final Identifier DIRT_VERTICAL_SLAB_ID = id("dirt_vertical_slab");
    public static final Identifier GRASS_VERTICAL_SLAB_ID = id("grass_vertical_slab");
    public static final Identifier DIRT_SLAB_ID = id("dirt_slab");
    public static final Identifier GRASS_SLAB_ID = id("grass_slab");

    public static final VerticalSlabBlock DIRT_VERTICAL_SLAB = new DirtVerticalSlab(
            properties(DIRT_VERTICAL_SLAB_ID, Blocks.DIRT));
    public static final VerticalSlabBlock GRASS_VERTICAL_SLAB = new GrassVerticalSlab(
            properties(GRASS_VERTICAL_SLAB_ID, Blocks.GRASS_BLOCK));
    public static final DirtHorizontalSlab DIRT_SLAB = new DirtHorizontalSlab(
            properties(DIRT_SLAB_ID, Blocks.DIRT));
    public static final GrassHorizontalSlab GRASS_SLAB = new GrassHorizontalSlab(
            properties(GRASS_SLAB_ID, Blocks.GRASS_BLOCK));
    private static boolean layersRegistered;

    @Override
    public void onInitialize() {
        register(DIRT_VERTICAL_SLAB_ID, DIRT_VERTICAL_SLAB);
        register(GRASS_VERTICAL_SLAB_ID, GRASS_VERTICAL_SLAB);
        register(DIRT_SLAB_ID, DIRT_SLAB);
        register(GRASS_SLAB_ID, GRASS_SLAB);
        CanonicalGeometryRegistry.register(
                DIRT_VERTICAL_SLAB,
                GRASS_VERTICAL_SLAB,
                DIRT_SLAB,
                GRASS_SLAB);
        GrassFamilyBehavior.registerDefaults();
    }

    /** Called from CNM's registry-bootstrap tail before the built-in registries freeze. */
    public static synchronized void registerLayers() {
        if (layersRegistered) return;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            Identifier id = layerId(profile);
            Block layer = NibaruProviderAdapter.createLayer(profile, layerProperties(id, profile));
            register(id, layer);
        }
        LayerGeneratedData.generate();
        layersRegistered = true;
    }

    /** Collision-safe registry identity derived from the typed canonical profile. */
    public static Identifier layerId(NibaruMaterialProfile profile) {
        Identifier parent = profile.canonicalParentId();
        return Identifier.fromNamespaceAndPath(MOD_ID,
                parent.getNamespace() + "/" + parent.getPath() + "_layer");
    }

    private static BlockBehaviour.Properties layerProperties(Identifier ownId,
            NibaruMaterialProfile profile) {
        return BlockBehaviour.Properties.ofFullCopy(profile.canonicalParent())
                .setId(ResourceKey.create(Registries.BLOCK, ownId));
    }

    private static BlockBehaviour.Properties properties(Identifier ownId, Block source) {
        return BlockBehaviour.Properties.ofFullCopy(source)
                .setId(ResourceKey.create(Registries.BLOCK, ownId))
                .randomTicks();
    }

    private static void register(Identifier id, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, id), block);
        Item.Properties properties = new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .useBlockDescriptionPrefix();
        Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, id), new BlockItem(block, properties));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

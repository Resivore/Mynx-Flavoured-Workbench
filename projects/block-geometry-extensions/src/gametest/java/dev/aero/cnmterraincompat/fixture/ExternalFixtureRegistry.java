package dev.aero.cnmterraincompat.fixture;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.LinkedHashMap;
import java.util.Map;

/** Late provider fixtures with exact production IDs and deliberately distinct live properties. */
public final class ExternalFixtureRegistry {
    private static final Map<Identifier, Block> BLOCKS = new LinkedHashMap<>();

    private ExternalFixtureRegistry() {}

    public static void registerMacawsPaths() {
        String[] materials = "andesite diorite granite sandstone red_sandstone brick stone mossy_stone cobbled_deepslate deepslate mud_brick blackstone dark_prismarine".split(" ");
        String[] patterns = "running_bond windmill_weave flagstone crystal_floor".split(" ");
        for (String material : materials) for (String pattern : patterns) {
            String full = material + "_" + pattern;
            Block source = register("mcwpaths", full, Blocks.DEEPSLATE_TILES, false);
            registerSlab("mcwpaths", full + "_slab", source);
            registerStairs("mcwpaths", full + "_stairs", source);
            // BGE validates that the provider's old Path form remains only reference data.
            register("mcwpaths", full + "_path", Blocks.DEEPSLATE_TILES, false);
        }
        register("mcwpaths", "podzol_path_block", Blocks.PODZOL, false);
        register("mcwpaths", "dirt_path_block", Blocks.DIRT_PATH, false);
        register("mcwpaths", "gravel_path_block", Blocks.GRAVEL, false);
        register("mcwpaths", "sand_path_block", Blocks.SAND, false);
        register("mcwpaths", "red_sand_path_block", Blocks.RED_SAND, false);
    }

    public static void registerMynxTrees() {
        Block wisteriaLog = register("mynx_trees", "wisteria_log", Blocks.CHERRY_LOG, true);
        Block wisteriaWood = register("mynx_trees", "wisteria_wood", Blocks.CHERRY_WOOD, true);
        register("mynx_trees", "wisteria_leaves", Blocks.CHERRY_LEAVES, false);
        Block silverLog = register("mynx_trees", "silver_birch_log", Blocks.BIRCH_LOG, true);
        Block silverWood = register("mynx_trees", "silver_birch_wood", Blocks.BIRCH_WOOD, true);
        register("mynx_trees", "silver_birch_leaves", Blocks.BIRCH_LEAVES, false);
        StrippableBlockRegistry.register(wisteriaLog, Blocks.STRIPPED_PALE_OAK_LOG);
        StrippableBlockRegistry.register(wisteriaWood, Blocks.STRIPPED_PALE_OAK_WOOD);
        StrippableBlockRegistry.register(silverLog, Blocks.STRIPPED_BIRCH_LOG);
        StrippableBlockRegistry.register(silverWood, Blocks.STRIPPED_BIRCH_WOOD);
        addFire("mynx_trees", "wisteria_log", 5, 5);
        addFire("mynx_trees", "wisteria_wood", 5, 5);
        addFire("mynx_trees", "silver_birch_log", 5, 5);
        addFire("mynx_trees", "silver_birch_wood", 5, 5);
        addFire("mynx_trees", "wisteria_leaves", 30, 60);
        addFire("mynx_trees", "silver_birch_leaves", 30, 60);
    }

    public static void registerRibbits() {
        Block source = register("ribbits", "mossy_oak_planks", Blocks.OAK_PLANKS, false);
        registerSlab("ribbits", "mossy_oak_planks_slab", source);
        registerStairs("ribbits", "mossy_oak_planks_stairs", source);
        addFire("ribbits", "mossy_oak_planks", 5, 20);
        addFire("ribbits", "mossy_oak_planks_slab", 5, 20);
        addFire("ribbits", "mossy_oak_planks_stairs", 5, 20);
        registerHugeMushroom("red_toadstool", Blocks.RED_MUSHROOM_BLOCK);
        registerHugeMushroom("brown_toadstool", Blocks.BROWN_MUSHROOM_BLOCK);
        registerHugeMushroom("toadstool_stem", Blocks.MUSHROOM_STEM);
    }

    private static void registerHugeMushroom(String path, Block template) {
        Identifier id = Identifier.fromNamespaceAndPath("ribbits", path);
        register("ribbits", path, new HugeMushroomBlock(BlockBehaviour.Properties.ofFullCopy(template)
                .setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    /** Exact BBB beam parent/standard-form IDs used to exercise the optional completion hook. */
    public static void registerBuildingButBetter() {
        for (String material : new String[] {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak"}) {
            Block beam = register("bbb", material + "_beam", Blocks.STRIPPED_OAK_LOG, true);
            // BBB's standard forms carry their own horizontal/directional state.  Keep the
            // fixture's constructor properties state-neutral: copying a pillar's AXIS-backed
            // properties into vanilla test blocks is rejected by the 26.2 constructors.
            registerBbbSlab(material, beam);
            registerBbbStairs(material, beam);
            register("bbb", material + "_wall", new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .setId(ResourceKey.create(Registries.BLOCK,
                            Identifier.fromNamespaceAndPath("bbb", material + "_wall")))));
        }
    }

    /** Exact Enderscape 3.0.2 source IDs used by the optional-provider integration fixture. */
    public static void registerEnderscape() {
        for (String path : new String[] {"veiled_log", "veiled_wood", "celestial_stem",
                "celestial_hyphae", "murublight_stem", "murublight_hyphae", "shadoline_pillar",
                "dusk_purpur_pillar"}) {
            register("enderscape", path, Blocks.OAK_LOG, true);
        }
        for (String path : new String[] {"chiseled_end_stone", "cracked_end_stone_bricks",
                "chiseled_purpur", "nebulite_block", "chiseled_shadoline", "chiseled_veradite",
                "chiseled_mirestone", "cracked_mirestone_bricks", "chiseled_kurodite",
                "alluring_magnia", "repulsive_magnia", "chiseled_dusk_purpur",
                "blistered_magnia", "void_shale", "celestial_cap", "murublight_cap", "end_lamp",
                "blinklamp", "drift_jelly_block", "veiled_end_stone", "celestial_overgrowth",
                "corrupt_overgrowth", "celestial_path", "corrupt_path"}) {
            register("enderscape", path, Blocks.END_STONE, false);
        }
        register("enderscape", "veiled_leaves", Blocks.OAK_LEAVES, false);
    }

    private static Block registerBbbSlab(String material, Block beam) {
        Identifier id = Identifier.fromNamespaceAndPath("bbb", material + "_beam_slab");
        return register("bbb", id.getPath(), new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                .setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    private static Block registerBbbStairs(String material, Block beam) {
        Identifier id = Identifier.fromNamespaceAndPath("bbb", material + "_beam_stairs");
        return register("bbb", id.getPath(), new StairBlock(Blocks.OAK_PLANKS.defaultBlockState(),
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                        .setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    private static Block register(String namespace, String path, Block source, boolean axis) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(source)
                .setId(ResourceKey.create(Registries.BLOCK, id));
        Block block = axis ? new RotatedPillarBlock(properties) : new Block(properties);
        return register(namespace, path, block);
    }

    private static Block registerSlab(String namespace, String path, Block source) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        return register(namespace, path, new SlabBlock(BlockBehaviour.Properties.ofFullCopy(source)
                .setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    private static Block registerStairs(String namespace, String path, Block source) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        return register(namespace, path, new StairBlock(source.defaultBlockState(),
                BlockBehaviour.Properties.ofFullCopy(source)
                        .setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    private static Block register(String namespace, String path, Block block) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, id), block);
        Item.Properties itemProperties = new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .useBlockDescriptionPrefix();
        Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, id),
                new BlockItem(block, itemProperties));
        BLOCKS.put(id, block);
        return block;
    }

    private static void addFire(String namespace, String path, int ignite, int burn) {
        FlammableBlockRegistry.getDefaultInstance().add(
                BLOCKS.get(Identifier.fromNamespaceAndPath(namespace, path)), ignite, burn);
    }
}

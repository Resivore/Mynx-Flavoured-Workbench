package net.penumbra.enderscape;

import com.starfish_studios.bbb.compat.EnderscapeWoodIntegration;
import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only Enderscape entrypoint that registers the six exact source blocks
 * after BBB's dependent-mod initializer has installed its completion callback.
 */
public final class Enderscape implements ModInitializer {
    private static final List<String> BASE_MATERIALS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak"
    );
    private static final LinkedHashMap<String, Block> MUTABLE_SOURCES = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Block> MUTABLE_RECIPE_SLABS = new LinkedHashMap<>();

    public static final Map<String, Block> SOURCES = java.util.Collections.unmodifiableMap(MUTABLE_SOURCES);
    public static final Map<String, Block> RECIPE_SLABS = java.util.Collections.unmodifiableMap(MUTABLE_RECIPE_SLABS);
    public static Map<String, Block> BASE_BLOCKS = Map.of();
    public static Map<String, Item> BASE_ITEMS = Map.of();
    public static List<BBBContent.StoneFamily> BASE_STONE_FAMILIES = List.of();
    public static boolean BASE_PHASE_OBSERVED;
    public static boolean LATE_REGISTRATION_COMPLETE;

    @Override
    public void onInitialize() {
        // Loader deliberately shuffles candidate entrypoints in development. Force only BBB's
        // idempotent standalone registry phase here, then let its normal integration entrypoint
        // exercise either the already-installed callback or the immediate explicit-ID lookup.
        BBBContent.initialize();
        List<String> actualBaseMaterials = BBBContent.WOOD_FAMILIES.stream()
                .map(BBBContent.WoodFamily::material)
                .toList();
        if (BBBContent.BLOCKS.size() != 171
                || BBBContent.ITEMS.size() != 172
                || !actualBaseMaterials.equals(BASE_MATERIALS)
                || BBBContent.BEAM_FAMILIES.size() != 12
                || BBBContent.LATTICES.size() != 12
                || BBBContent.enderscapeFamiliesRegistered()
                || EnderscapeWoodIntegration.familiesRegistered()) {
            throw new IllegalStateException("BBB did not expose its exact standalone registry before Enderscape: "
                    + "blocks=" + BBBContent.BLOCKS.size()
                    + ", items=" + BBBContent.ITEMS.size()
                    + ", wood=" + actualBaseMaterials
                    + ", beams=" + BBBContent.BEAM_FAMILIES.size()
                    + ", lattices=" + BBBContent.LATTICES.size());
        }
        BASE_BLOCKS = Map.copyOf(BBBContent.BLOCKS);
        BASE_ITEMS = Map.copyOf(BBBContent.ITEMS);
        BASE_STONE_FAMILIES = List.copyOf(BBBContent.STONE_FAMILIES);
        BASE_PHASE_OBSERVED = true;

        registerPlanks("veiled_planks");
        registerAxial("stripped_veiled_log");
        registerPlanks("celestial_planks");
        registerAxial("stripped_celestial_stem");
        registerPlanks("murublight_planks");
        registerAxial("stripped_murublight_stem");
        registerRecipeSlab("veiled_slab");
        registerRecipeSlab("celestial_slab");
        registerRecipeSlab("murublight_slab");

        if (MUTABLE_SOURCES.size() != 6
                || MUTABLE_RECIPE_SLABS.size() != 3) {
            throw new IllegalStateException("Enderscape fixture did not register its exact source/support set");
        }
        LATE_REGISTRATION_COMPLETE = true;
    }

    private static Block registerPlanks(String path) {
        Identifier id = id(path);
        return register(path, new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                .setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    private static Block registerAxial(String path) {
        Identifier id = id(path);
        return register(path, new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)
                .setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    private static Block registerRecipeSlab(String path) {
        Identifier id = id(path);
        Block slab = registerBlockAndItem(path, new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)
                .setId(ResourceKey.create(Registries.BLOCK, id))));
        MUTABLE_RECIPE_SLABS.put(path, slab);
        return slab;
    }

    private static Block register(String path, Block block) {
        Block registered = registerBlockAndItem(path, block);
        MUTABLE_SOURCES.put(path, registered);
        return registered;
    }

    private static Block registerBlockAndItem(String path, Block block) {
        Identifier id = id(path);
        if (BuiltInRegistries.BLOCK.containsKey(id)) {
            throw new IllegalStateException("Duplicate Enderscape fixture source " + id);
        }
        Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, id), block);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("enderscape", path);
    }

    public Enderscape() {}
}

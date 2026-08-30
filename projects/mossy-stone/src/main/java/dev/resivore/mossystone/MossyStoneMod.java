package dev.resivore.mossystone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MossyStoneMod implements ModInitializer {
    public static final String MOD_ID = "mossy_stone";

    public static final Identifier BLOCK_ID = id("mossy_stone");
    public static final Identifier SLAB_ID = id("mossy_stone_slab");
    public static final Identifier STAIRS_ID = id("mossy_stone_stairs");
    public static final Identifier WALL_ID = id("mossy_stone_wall");

    public static final Block MOSSY_STONE = new Block(properties(BLOCK_ID));
    public static final SlabBlock MOSSY_STONE_SLAB = new SlabBlock(properties(SLAB_ID));
    public static final StairBlock MOSSY_STONE_STAIRS = new StairBlock(
            MOSSY_STONE.defaultBlockState(), properties(STAIRS_ID));
    public static final WallBlock MOSSY_STONE_WALL = new WallBlock(properties(WALL_ID));

    @Override
    public void onInitialize() {
        register(BLOCK_ID, MOSSY_STONE);
        register(SLAB_ID, MOSSY_STONE_SLAB);
        register(STAIRS_ID, MOSSY_STONE_STAIRS);
        register(WALL_ID, MOSSY_STONE_WALL);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
            output.accept(MOSSY_STONE, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            output.accept(MOSSY_STONE_SLAB, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            output.accept(MOSSY_STONE_STAIRS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            output.accept(MOSSY_STONE_WALL, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
    }

    private static BlockBehaviour.Properties properties(Identifier id) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, id));
    }

    private static void register(Identifier id, Block block) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

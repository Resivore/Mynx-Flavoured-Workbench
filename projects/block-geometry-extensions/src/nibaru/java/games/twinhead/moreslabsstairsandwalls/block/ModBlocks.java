package games.twinhead.moreslabsstairsandwalls.block;

import games.twinhead.moreslabsstairsandwalls.registry.fabric.ModRegistry;
import games.twinhead.moreslabsstairsandwalls.MoreSlabsStairsAndWalls;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;

public enum ModBlocks {

    GRASS_BLOCK(builder(Blocks.GRASS_BLOCK).shovel().modelType(ModelType.GRASS).addBlockTags(BlockTags.DIRT)),
    PODZOL(builder(Blocks.PODZOL).shovel().modelType(ModelType.GRASS).addBlockTags(BlockTags.OVERRIDES_MUSHROOM_LIGHT_REQUIREMENT, BlockTags.DIRT)),
    MYCELIUM(builder(Blocks.MYCELIUM).shovel().modelType(ModelType.GRASS).addBlockTags(BlockTags.DIRT, BlockTags.OVERRIDES_MUSHROOM_LIGHT_REQUIREMENT)),
    DIRT(builder(Blocks.DIRT).shovel().addBlockTags(BlockTags.DIRT)),
    DIRT_PATH(builder(Blocks.DIRT_PATH).shovel().modelType(ModelType.PATH)),
    COARSE_DIRT(builder(Blocks.COARSE_DIRT).shovel().addBlockTags(BlockTags.DIRT)),
    ROOTED_DIRT(builder(Blocks.ROOTED_DIRT).shovel().addBlockTags(BlockTags.DIRT)),

    STRIPPED_OAK_LOG(builder(Blocks.STRIPPED_OAK_LOG).axe().modelType(ModelType.LOG)),
    STRIPPED_OAK_WOOD(builder(Blocks.STRIPPED_OAK_WOOD).axe().setAllTexture("stripped_oak_log")),
    OAK_LOG(builder(Blocks.OAK_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_OAK_LOG)),
    OAK_WOOD(builder(Blocks.OAK_WOOD).axe().setAllTexture("oak_log").associatedBlock(STRIPPED_OAK_WOOD)),
    OAK_LEAVES(builder(Blocks.OAK_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)),
    OAK_PLANKS(builder(Blocks.OAK_PLANKS).axe().wallOnly()),

    STRIPPED_SPRUCE_LOG(builder(Blocks.STRIPPED_SPRUCE_LOG).axe().modelType(ModelType.LOG)),
    STRIPPED_SPRUCE_WOOD(builder(Blocks.STRIPPED_SPRUCE_WOOD).axe().setAllTexture("stripped_spruce_log")),
    SPRUCE_LOG(builder(Blocks.SPRUCE_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_SPRUCE_LOG)),
    SPRUCE_WOOD(builder(Blocks.SPRUCE_WOOD).axe().setAllTexture("spruce_log").associatedBlock(STRIPPED_SPRUCE_WOOD)),
    SPRUCE_LEAVES(builder(Blocks.SPRUCE_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)),
    SPRUCE_PLANKS(builder(Blocks.SPRUCE_PLANKS).axe().wallOnly()),

    STRIPPED_BIRCH_LOG(builder(Blocks.STRIPPED_BIRCH_LOG).axe().modelType(ModelType.LOG)),
    STRIPPED_BIRCH_WOOD(builder(Blocks.STRIPPED_BIRCH_WOOD).axe().setAllTexture("stripped_birch_log")),
    BIRCH_LOG(builder(Blocks.BIRCH_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_BIRCH_LOG)),
    BIRCH_WOOD(builder(Blocks.BIRCH_WOOD).axe().setAllTexture("birch_log").associatedBlock(STRIPPED_BIRCH_WOOD)),
    BIRCH_LEAVES(builder(Blocks.BIRCH_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)),
    BIRCH_PLANKS(builder(Blocks.BIRCH_PLANKS).axe().wallOnly()),

    STRIPPED_JUNGLE_LOG(builder(Blocks.STRIPPED_JUNGLE_LOG).axe().modelType(ModelType.LOG)),
    STRIPPED_JUNGLE_WOOD(builder(Blocks.STRIPPED_JUNGLE_WOOD).axe().setAllTexture("stripped_jungle_log")),
    JUNGLE_LOG(builder(Blocks.JUNGLE_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_JUNGLE_LOG)),
    JUNGLE_WOOD(builder(Blocks.JUNGLE_WOOD).axe().setAllTexture("jungle_log").associatedBlock(STRIPPED_JUNGLE_WOOD)),
    JUNGLE_LEAVES(builder(Blocks.JUNGLE_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)),
    JUNGLE_PLANKS(builder(Blocks.JUNGLE_PLANKS).axe().wallOnly()),

    STRIPPED_ACACIA_LOG(builder(Blocks.STRIPPED_ACACIA_LOG).axe().modelType(ModelType.LOG)),
    STRIPPED_ACACIA_WOOD(builder(Blocks.STRIPPED_ACACIA_WOOD).axe().setAllTexture("stripped_acacia_log")),
    ACACIA_LOG(builder(Blocks.ACACIA_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_ACACIA_LOG)),
    ACACIA_WOOD(builder(Blocks.ACACIA_WOOD).axe().setAllTexture("acacia_log").associatedBlock(STRIPPED_ACACIA_WOOD)),
    ACACIA_LEAVES(builder(Blocks.ACACIA_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)),
    ACACIA_PLANKS(builder(Blocks.ACACIA_PLANKS).axe().wallOnly()),

    STRIPPED_DARK_OAK_LOG(builder(Blocks.STRIPPED_DARK_OAK_LOG).axe().modelType(ModelType.LOG)),
    STRIPPED_DARK_OAK_WOOD(builder(Blocks.STRIPPED_DARK_OAK_WOOD).axe().setAllTexture("stripped_dark_oak_log")),
    DARK_OAK_LOG(builder(Blocks.DARK_OAK_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_DARK_OAK_LOG)),
    DARK_OAK_WOOD(builder(Blocks.DARK_OAK_WOOD).axe().setAllTexture("dark_oak_log").associatedBlock(STRIPPED_DARK_OAK_WOOD)),
    DARK_OAK_LEAVES(builder(Blocks.DARK_OAK_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)),
    DARK_OAK_PLANKS(builder(Blocks.DARK_OAK_PLANKS).axe().wallOnly()),

    STRIPPED_PALE_OAK_LOG(builder(Blocks.STRIPPED_PALE_OAK_LOG).axe().modelType(ModelType.LOG)),
    STRIPPED_PALE_OAK_WOOD(builder(Blocks.STRIPPED_PALE_OAK_WOOD).axe().setAllTexture("stripped_pale_oak_log")),
    PALE_OAK_LOG(builder(Blocks.PALE_OAK_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_PALE_OAK_LOG)),
    PALE_OAK_WOOD(builder(Blocks.PALE_OAK_WOOD).axe().setAllTexture("pale_oak_log").associatedBlock(STRIPPED_PALE_OAK_WOOD)),
    PALE_OAK_LEAVES(builder(Blocks.PALE_OAK_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)),
    PALE_OAK_PLANKS(builder(Blocks.PALE_OAK_PLANKS).axe().wallOnly()),

    STRIPPED_MANGROVE_LOG(builder(Blocks.STRIPPED_MANGROVE_LOG).axe().modelType(ModelType.LOG)),
    STRIPPED_MANGROVE_WOOD(builder(Blocks.STRIPPED_MANGROVE_WOOD).axe().setAllTexture("stripped_mangrove_log")),
    MANGROVE_LOG(builder(Blocks.MANGROVE_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_MANGROVE_LOG)),
    MANGROVE_WOOD(builder(Blocks.MANGROVE_WOOD).axe().setAllTexture("mangrove_log").associatedBlock(STRIPPED_MANGROVE_WOOD)),
    MANGROVE_LEAVES(builder(Blocks.MANGROVE_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)),
    MANGROVE_PLANKS(builder(Blocks.MANGROVE_PLANKS).axe().wallOnly()),

    MANGROVE_ROOTS(builder(Blocks.MANGROVE_ROOTS).axe().modelType(ModelType.ROOTS)),
    MUDDY_MANGROVE_ROOTS(builder(Blocks.MUDDY_MANGROVE_ROOTS).shovel().modelType(ModelType.CUBE_BOTTOM_TOP)),

    STRIPPED_CHERRY_LOG(builder(Blocks.STRIPPED_CHERRY_LOG).axe().modelType(ModelType.LOG)),
    STRIPPED_CHERRY_WOOD(builder(Blocks.STRIPPED_CHERRY_WOOD).axe().setAllTexture("stripped_cherry_log")),
    CHERRY_LOG(builder(Blocks.CHERRY_LOG).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_CHERRY_LOG)),
    CHERRY_WOOD(builder(Blocks.CHERRY_WOOD).axe().setAllTexture("cherry_log").associatedBlock(STRIPPED_CHERRY_WOOD)),
    CHERRY_LEAVES(builder(Blocks.CHERRY_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES)),
    CHERRY_PLANKS(builder(Blocks.CHERRY_PLANKS).axe().wallOnly()),

    STRIPPED_BAMBOO_BLOCK(builder(Blocks.STRIPPED_BAMBOO_BLOCK).axe().modelType(ModelType.LOG)),
    BAMBOO_BLOCK(builder(Blocks.BAMBOO_BLOCK).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_BAMBOO_BLOCK)),
    BAMBOO_PLANKS(builder(Blocks.BAMBOO_PLANKS).axe().wallOnly()),
    BAMBOO_MOSAIC(builder(Blocks.BAMBOO_MOSAIC).axe().wallOnly()),

    AZALEA_LEAVES(builder(Blocks.AZALEA_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES, BlockTags.MINEABLE_WITH_HOE)),
    FLOWERING_AZALEA_LEAVES(builder(Blocks.FLOWERING_AZALEA_LEAVES).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES, BlockTags.MINEABLE_WITH_HOE)),

    STRIPPED_WARPED_STEM(builder(Blocks.STRIPPED_WARPED_STEM).axe().modelType(ModelType.LOG)),
    STRIPPED_WARPED_HYPHAE(builder(Blocks.STRIPPED_WARPED_HYPHAE).axe().setAllTexture("stripped_warped_stem")),
    WARPED_STEM(builder(Blocks.WARPED_STEM).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_WARPED_STEM)),
    WARPED_HYPHAE(builder(Blocks.WARPED_HYPHAE).axe().setAllTexture("warped_stem").associatedBlock(STRIPPED_WARPED_HYPHAE)),
    WARPED_WART(builder(Blocks.WARPED_WART_BLOCK).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES, BlockTags.MINEABLE_WITH_HOE)),
    WARPED_PLANKS(builder(Blocks.WARPED_PLANKS).axe().wallOnly()),

    STRIPPED_CRIMSON_STEM(builder(Blocks.STRIPPED_CRIMSON_STEM).axe().modelType(ModelType.LOG)),
    STRIPPED_CRIMSON_HYPHAE(builder(Blocks.STRIPPED_CRIMSON_HYPHAE).axe().setAllTexture("stripped_crimson_stem")),
    CRIMSON_STEM(builder(Blocks.CRIMSON_STEM).axe().modelType(ModelType.LOG).associatedBlock(STRIPPED_CRIMSON_STEM)),
    CRIMSON_HYPHAE(builder(Blocks.CRIMSON_HYPHAE).axe().setAllTexture("crimson_stem").associatedBlock(STRIPPED_CRIMSON_HYPHAE)),
    CRIMSON_WART(builder(Blocks.NETHER_WART_BLOCK).hoe().modelType(ModelType.LEAVES).addBlockTags(BlockTags.LEAVES, BlockTags.MINEABLE_WITH_HOE)),
    CRIMSON_PLANKS(builder(Blocks.CRIMSON_PLANKS).axe().wallOnly()),

    GLASS(builder(Blocks.GLASS).modelType(ModelType.GLASS)),
    WHITE_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.WHITE)).modelType(ModelType.GLASS)),
    YELLOW_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.YELLOW)).modelType(ModelType.GLASS)),
    BLACK_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.BLACK)).modelType(ModelType.GLASS)),
    RED_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.RED)).modelType(ModelType.GLASS)),
    PURPLE_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.PURPLE)).modelType(ModelType.GLASS)),
    PINK_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.PINK)).modelType(ModelType.GLASS)),
    ORANGE_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.ORANGE)).modelType(ModelType.GLASS)),
    MAGENTA_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.MAGENTA)).modelType(ModelType.GLASS)),
    LIME_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.LIME)).modelType(ModelType.GLASS)),
    LIGHT_GRAY_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_GRAY)).modelType(ModelType.GLASS)),
    LIGHT_BLUE_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE)).modelType(ModelType.GLASS)),
    GREEN_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.GREEN)).modelType(ModelType.GLASS)),
    GRAY_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.GRAY)).modelType(ModelType.GLASS)),
    CYAN_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.CYAN)).modelType(ModelType.GLASS)),
    BROWN_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.BROWN)).modelType(ModelType.GLASS)),
    BLUE_STAINED_GLASS(builder(Blocks.STAINED_GLASS.pick(DyeColor.BLUE)).modelType(ModelType.GLASS)),

    WHITE_WOOL(builder(Blocks.WOOL.pick(DyeColor.WHITE)).addBlockTags(BlockTags.WOOL)),
    YELLOW_WOOL(builder(Blocks.WOOL.pick(DyeColor.YELLOW)).addBlockTags(BlockTags.WOOL)),
    BLACK_WOOL(builder(Blocks.WOOL.pick(DyeColor.BLACK)).addBlockTags(BlockTags.WOOL)),
    RED_WOOL(builder(Blocks.WOOL.pick(DyeColor.RED)).addBlockTags(BlockTags.WOOL)),
    PURPLE_WOOL(builder(Blocks.WOOL.pick(DyeColor.PURPLE)).addBlockTags(BlockTags.WOOL)),
    PINK_WOOL(builder(Blocks.WOOL.pick(DyeColor.PINK)).addBlockTags(BlockTags.WOOL)),
    ORANGE_WOOL(builder(Blocks.WOOL.pick(DyeColor.ORANGE)).addBlockTags(BlockTags.WOOL)),
    MAGENTA_WOOL(builder(Blocks.WOOL.pick(DyeColor.MAGENTA)).addBlockTags(BlockTags.WOOL)),
    LIME_WOOL(builder(Blocks.WOOL.pick(DyeColor.LIME)).addBlockTags(BlockTags.WOOL)),
    LIGHT_GRAY_WOOL(builder(Blocks.WOOL.pick(DyeColor.LIGHT_GRAY)).addBlockTags(BlockTags.WOOL)),
    LIGHT_BLUE_WOOL(builder(Blocks.WOOL.pick(DyeColor.LIGHT_BLUE)).addBlockTags(BlockTags.WOOL)),
    GREEN_WOOL(builder(Blocks.WOOL.pick(DyeColor.GREEN)).addBlockTags(BlockTags.WOOL)),
    GRAY_WOOL(builder(Blocks.WOOL.pick(DyeColor.GRAY)).addBlockTags(BlockTags.WOOL)),
    CYAN_WOOL(builder(Blocks.WOOL.pick(DyeColor.CYAN)).addBlockTags(BlockTags.WOOL)),
    BROWN_WOOL(builder(Blocks.WOOL.pick(DyeColor.BROWN)).addBlockTags(BlockTags.WOOL)),
    BLUE_WOOL(builder(Blocks.WOOL.pick(DyeColor.BLUE)).addBlockTags(BlockTags.WOOL)),

    WARPED_NYLIUM(builder(Blocks.WARPED_NYLIUM).modelType(ModelType.GRASS).pickaxe()),
    CRIMSON_NYLIUM(builder(Blocks.CRIMSON_NYLIUM).modelType(ModelType.GRASS).pickaxe()),

    SAND(builder(Blocks.SAND).shovel()),
    GRAVEL(builder(Blocks.GRAVEL).shovel()),
    RED_SAND(builder(Blocks.RED_SAND).shovel()),

    BLACK_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.BLACK)).pickaxe()),
    BLACK_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.BLACK)).associatedBlock(BLACK_CONCRETE).shovel()),
    BLUE_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.BLUE)).pickaxe()),
    BLUE_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.BLUE)).associatedBlock(BLUE_CONCRETE).shovel()),
    BROWN_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.BROWN)).pickaxe()),
    BROWN_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.BROWN)).associatedBlock(BROWN_CONCRETE).shovel()),
    CYAN_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.CYAN)).pickaxe()),
    CYAN_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.CYAN)).associatedBlock(CYAN_CONCRETE).shovel()),
    GRAY_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.GRAY)).pickaxe()),
    GRAY_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.GRAY)).associatedBlock(GRAY_CONCRETE).shovel()),
    GREEN_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.GREEN)).pickaxe()),
    GREEN_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.GREEN)).associatedBlock(GREEN_CONCRETE).shovel()),
    LIGHT_BLUE_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.LIGHT_BLUE)).pickaxe()),
    LIGHT_BLUE_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.LIGHT_BLUE)).associatedBlock(LIGHT_BLUE_CONCRETE).shovel()),
    LIGHT_GRAY_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.LIGHT_GRAY)).pickaxe()),
    LIGHT_GRAY_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.LIGHT_GRAY)).associatedBlock(LIGHT_GRAY_CONCRETE).shovel()),
    LIME_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.LIME)).pickaxe()),
    LIME_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.LIME)).associatedBlock(LIME_CONCRETE).shovel()),
    MAGENTA_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.MAGENTA)).pickaxe()),
    MAGENTA_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.MAGENTA)).associatedBlock(MAGENTA_CONCRETE).shovel()),
    ORANGE_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.ORANGE)).pickaxe()),
    ORANGE_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.ORANGE)).associatedBlock(ORANGE_CONCRETE).shovel()),
    PINK_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.PINK)).pickaxe()),
    PINK_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.PINK)).associatedBlock(PINK_CONCRETE).shovel()),
    PURPLE_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.PURPLE)).pickaxe()),
    PURPLE_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.PURPLE)).associatedBlock(PURPLE_CONCRETE).shovel()),
    RED_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.RED)).pickaxe()),
    RED_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.RED)).associatedBlock(RED_CONCRETE).shovel()),
    WHITE_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.WHITE)).pickaxe()),
    WHITE_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.WHITE)).associatedBlock(WHITE_CONCRETE).shovel()),
    YELLOW_CONCRETE(builder(Blocks.CONCRETE.pick(DyeColor.YELLOW)).pickaxe()),
    YELLOW_CONCRETE_POWDER(builder(Blocks.CONCRETE_POWDER.pick(DyeColor.YELLOW)).associatedBlock(YELLOW_CONCRETE).shovel()),

    TERRACOTTA(builder(Blocks.TERRACOTTA).pickaxe()),

    WHITE_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)).pickaxe()),
    YELLOW_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.YELLOW)).pickaxe()),
    BLACK_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.BLACK)).pickaxe()),
    RED_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.RED)).pickaxe()),
    PURPLE_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.PURPLE)).pickaxe()),
    PINK_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.PINK)).pickaxe()),
    ORANGE_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE)).pickaxe()),
    MAGENTA_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.MAGENTA)).pickaxe()),
    LIME_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.LIME)).pickaxe()),
    LIGHT_GRAY_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.LIGHT_GRAY)).pickaxe()),
    LIGHT_BLUE_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE)).pickaxe()),
    GREEN_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.GREEN)).pickaxe()),
    GRAY_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.GRAY)).pickaxe()),
    CYAN_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.CYAN)).pickaxe()),
    BROWN_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.BROWN)).pickaxe()),
    BLUE_TERRACOTTA(builder(Blocks.DYED_TERRACOTTA.pick(DyeColor.BLUE)).pickaxe()),

    WHITE_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.WHITE)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    YELLOW_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.YELLOW)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    BLACK_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.BLACK)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    RED_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.RED)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    PURPLE_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.PURPLE)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    PINK_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.PINK)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    ORANGE_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.ORANGE)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    MAGENTA_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.MAGENTA)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    LIME_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.LIME)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    LIGHT_GRAY_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.LIGHT_GRAY)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    LIGHT_BLUE_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    GREEN_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.GREEN)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    GRAY_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.GRAY)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    CYAN_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.CYAN)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    BROWN_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.BROWN)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),
    BLUE_GLAZED_TERRACOTTA(builder(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.BLUE)).modelType(ModelType.GLAZED_TERRACOTTA).pickaxe()),

    QUARTZ_BRICKS(builder(Blocks.QUARTZ_BRICKS).pickaxe()),
    HONEYCOMB_BLOCK(builder(Blocks.HONEYCOMB_BLOCK)),
    SNOW_BLOCK(builder(Blocks.SNOW_BLOCK).setAllTexture("snow").shovel()),

    MUSHROOM_STEM(builder(Blocks.MUSHROOM_STEM).axe()),
    BROWN_MUSHROOM_BLOCK(builder(Blocks.BROWN_MUSHROOM_BLOCK).axe()),
    RED_MUSHROOM_BLOCK(builder(Blocks.RED_MUSHROOM_BLOCK).axe()),

    PURPUR(builder(Blocks.PURPUR_BLOCK).wallOnly().pickaxe()),
    PURPUR_PILLAR(builder(Blocks.PURPUR_PILLAR).modelType(ModelType.LOG).pickaxe()),

    MOSS_BLOCK(builder(Blocks.MOSS_BLOCK).hoe()),
    PALE_MOSS_BLOCK(builder(Blocks.PALE_MOSS_BLOCK).hoe()),
    CALCITE(builder(Blocks.CALCITE).pickaxe()),
    GLOWSTONE(builder(Blocks.GLOWSTONE)),

    CRACKED_STONE_BRICKS(builder(Blocks.CRACKED_STONE_BRICKS).pickaxe()),
    CHISELED_STONE_BRICKS(builder(Blocks.CHISELED_STONE_BRICKS).pickaxe()),
    CHISELED_RESIN_BRICKS(builder(Blocks.CHISELED_RESIN_BRICKS).pickaxe()),
    CHISELED_CINNABAR(builder(Blocks.CHISELED_CINNABAR).pickaxe()),

    DRIPSTONE_BLOCK(builder(Blocks.DRIPSTONE_BLOCK).pickaxe()),
    SEA_LANTERN(builder(Blocks.SEA_LANTERN)),
    SHROOMLIGHT(builder(Blocks.SHROOMLIGHT).hoe()),
    END_STONE(builder(Blocks.END_STONE).pickaxe()),

    DEEPSLATE(builder(Blocks.DEEPSLATE).setTextures("deepslate","deepslate_top").pickaxe()),

    CRACKED_NETHER_BRICKS(builder(Blocks.CRACKED_NETHER_BRICKS).pickaxe()),
    CRACKED_DEEPSLATE_BRICKS(builder(Blocks.CRACKED_DEEPSLATE_BRICKS).pickaxe()),
    CRACKED_POLISHED_BLACKSTONE_BRICKS(builder(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS).pickaxe()),
    CRACKED_DEEPSLATE_TILES(builder(Blocks.CRACKED_DEEPSLATE_TILES).pickaxe()),

    CHISELED_POLISHED_BLACKSTONE(builder(Blocks.CHISELED_POLISHED_BLACKSTONE).pickaxe()),
    CHISELED_DEEPSLATE(builder(Blocks.CHISELED_DEEPSLATE).pickaxe()),

    CRYING_OBSIDIAN(builder(Blocks.CRYING_OBSIDIAN).pickaxe().addBlockTags(BlockTags.DRAGON_IMMUNE, BlockTags.NEEDS_DIAMOND_TOOL)),

    NETHERRACK(builder(Blocks.NETHERRACK).pickaxe().addBlockTags(BlockTags.INFINIBURN_OVERWORLD, BlockTags.INFINIBURN_END, BlockTags.INFINIBURN_NETHER)),

    BASALT(builder(Blocks.BASALT).modelType(ModelType.CUBE_BOTTOM_TOP).pickaxe()),
    POLISHED_BASALT(builder(Blocks.POLISHED_BASALT).modelType(ModelType.CUBE_BOTTOM_TOP).pickaxe()),
    SMOOTH_BASALT(builder(Blocks.SMOOTH_BASALT).pickaxe()),

    OXIDIZED_COPPER(builder(Blocks.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.OXIDIZED)).setOxidationLevel(WeatheringCopper.WeatherState.OXIDIZED).pickaxe()),
    WEATHERED_COPPER(builder(Blocks.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.WEATHERED)).setOxidationLevel(WeatheringCopper.WeatherState.WEATHERED).associatedBlock(OXIDIZED_COPPER).pickaxe()),
    EXPOSED_COPPER(builder(Blocks.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.EXPOSED)).setOxidationLevel(WeatheringCopper.WeatherState.EXPOSED).associatedBlock(WEATHERED_COPPER).pickaxe()),
    COPPER_BLOCK(builder(Blocks.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.UNAFFECTED)).setOxidationLevel(WeatheringCopper.WeatherState.UNAFFECTED).associatedBlock(EXPOSED_COPPER).pickaxe()),

    WAXED_COPPER_BLOCK(builder(Blocks.COPPER_BLOCK.waxed().pick(WeatheringCopper.WeatherState.UNAFFECTED)).setAllTexture("copper_block").associatedBlock(COPPER_BLOCK).pickaxe()),
    WAXED_EXPOSED_COPPER(builder(Blocks.COPPER_BLOCK.waxed().pick(WeatheringCopper.WeatherState.EXPOSED)).setAllTexture("exposed_copper").associatedBlock(EXPOSED_COPPER).pickaxe()),
    WAXED_WEATHERED_COPPER(builder(Blocks.COPPER_BLOCK.waxed().pick(WeatheringCopper.WeatherState.WEATHERED)).setAllTexture("weathered_copper").associatedBlock(WEATHERED_COPPER).pickaxe()),
    WAXED_OXIDIZED_COPPER(builder(Blocks.COPPER_BLOCK.waxed().pick(WeatheringCopper.WeatherState.OXIDIZED)).setAllTexture("oxidized_copper").associatedBlock(OXIDIZED_COPPER).pickaxe()),

    OXIDIZED_CUT_COPPER(builder(Blocks.CUT_COPPER.weathering().pick(WeatheringCopper.WeatherState.OXIDIZED)).setOxidationLevel(WeatheringCopper.WeatherState.OXIDIZED).wallOnly().pickaxe()),
    WEATHERED_CUT_COPPER(builder(Blocks.CUT_COPPER.weathering().pick(WeatheringCopper.WeatherState.WEATHERED)).setOxidationLevel(WeatheringCopper.WeatherState.WEATHERED).associatedBlock(OXIDIZED_CUT_COPPER).wallOnly().pickaxe()),
    EXPOSED_CUT_COPPER(builder(Blocks.CUT_COPPER.weathering().pick(WeatheringCopper.WeatherState.EXPOSED)).setOxidationLevel(WeatheringCopper.WeatherState.EXPOSED).associatedBlock(WEATHERED_CUT_COPPER).wallOnly().pickaxe()),
    CUT_COPPER(builder(Blocks.CUT_COPPER.weathering().pick(WeatheringCopper.WeatherState.UNAFFECTED)).setOxidationLevel(WeatheringCopper.WeatherState.UNAFFECTED).associatedBlock(EXPOSED_CUT_COPPER).wallOnly().pickaxe()),

    WAXED_CUT_COPPER(builder(Blocks.CUT_COPPER.waxed().pick(WeatheringCopper.WeatherState.UNAFFECTED)).setAllTexture("cut_copper").associatedBlock(CUT_COPPER).wallOnly().pickaxe()),
    WAXED_EXPOSED_CUT_COPPER(builder(Blocks.CUT_COPPER.waxed().pick(WeatheringCopper.WeatherState.EXPOSED)).setAllTexture("exposed_cut_copper").associatedBlock(EXPOSED_CUT_COPPER).wallOnly().pickaxe()),
    WAXED_WEATHERED_CUT_COPPER(builder(Blocks.CUT_COPPER.waxed().pick(WeatheringCopper.WeatherState.WEATHERED)).setAllTexture("weathered_cut_copper").associatedBlock(WEATHERED_CUT_COPPER).wallOnly().pickaxe()),
    WAXED_OXIDIZED_CUT_COPPER(builder(Blocks.CUT_COPPER.waxed().pick(WeatheringCopper.WeatherState.OXIDIZED)).setAllTexture("oxidized_cut_copper").associatedBlock(OXIDIZED_CUT_COPPER).wallOnly().pickaxe()),

    SMOOTH_QUARTZ(builder(Blocks.SMOOTH_QUARTZ).wallOnly().setAllTexture("quartz_block_bottom").pickaxe()),
    QUARTZ_BLOCK(builder(Blocks.QUARTZ_BLOCK).setAllTexture("quartz_block_side").wallOnly().pickaxe()),
    CHISELED_QUARTZ_BLOCK(builder(Blocks.CHISELED_QUARTZ_BLOCK).setTextures("chiseled_quartz_block", "chiseled_quartz_block_top").pickaxe()),
    QUARTZ_PILLAR(builder(Blocks.QUARTZ_PILLAR).setTextures("quartz_pillar_side", "quartz_pillar_top").pickaxe()),

    POLISHED_ANDESITE(builder(Blocks.POLISHED_ANDESITE).pickaxe().wallOnly()),
    POLISHED_GRANITE(builder(Blocks.POLISHED_GRANITE).pickaxe().wallOnly()),
    POLISHED_DIORITE(builder(Blocks.POLISHED_DIORITE).pickaxe().wallOnly()),

    SMOOTH_SANDSTONE(builder(Blocks.SMOOTH_SANDSTONE).setAllTexture("sandstone_top").pickaxe().wallOnly()),
    SMOOTH_RED_SANDSTONE(builder(Blocks.SMOOTH_RED_SANDSTONE).setAllTexture("red_sandstone_top").pickaxe().wallOnly()),
    DARK_PRISMARINE(builder(Blocks.DARK_PRISMARINE).pickaxe().wallOnly()),
    PRISMARINE_BRICKS(builder(Blocks.PRISMARINE_BRICKS).pickaxe().wallOnly()),
    STONE(builder(Blocks.STONE).pickaxe().wallOnly()),

    SMOOTH_STONE(builder(Blocks.SMOOTH_STONE).pickaxe().hasSlab(false)),

    OBSIDIAN(builder(Blocks.OBSIDIAN).pickaxe().addBlockTags(BlockTags.DRAGON_IMMUNE, BlockTags.NEEDS_DIAMOND_TOOL)),

    AMETHYST_BLOCK(builder(Blocks.AMETHYST_BLOCK).pickaxe().addBlockTags(BlockTags.CRYSTAL_SOUND_BLOCKS)),

    RAW_COPPER_BLOCK(builder(Blocks.RAW_COPPER_BLOCK).pickaxe()),
    RAW_GOLD_BLOCK(builder(Blocks.RAW_GOLD_BLOCK).pickaxe()),
    RAW_IRON_BLOCK(builder(Blocks.RAW_IRON_BLOCK).pickaxe()),

    MAGMA_BLOCK(builder(Blocks.MAGMA_BLOCK).setAllTexture("magma").pickaxe().addBlockTags(BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN)),

    SOUL_SAND(builder(Blocks.SOUL_SAND).shovel().addBlockTags(BlockTags.SOUL_SPEED_BLOCKS, BlockTags.SOUL_FIRE_BASE_BLOCKS, BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP)),
    SOUL_SOIL(builder(Blocks.SOUL_SOIL).shovel().addBlockTags(BlockTags.SOUL_SPEED_BLOCKS, BlockTags.SOUL_FIRE_BASE_BLOCKS)),

    CLAY(builder(Blocks.CLAY).shovel()),

    CHISELED_SANDSTONE(builder(Blocks.CHISELED_SANDSTONE).setTextures("chiseled_sandstone", "sandstone_top").pickaxe()),
    CHISELED_RED_SANDSTONE(builder(Blocks.CHISELED_RED_SANDSTONE).setTextures("chiseled_red_sandstone", "red_sandstone_top").pickaxe()),

    CUT_SANDSTONE(builder(Blocks.CUT_SANDSTONE).setTextures("cut_sandstone", "sandstone_top").pickaxe().hasSlab(false)),
    CUT_RED_SANDSTONE(builder(Blocks.CUT_RED_SANDSTONE).setTextures("cut_red_sandstone", "red_sandstone_top").pickaxe().hasSlab(false)),

    CHISELED_NETHER_BRICKS(builder(Blocks.CHISELED_NETHER_BRICKS).pickaxe()),

    COAL_BLOCK(builder(Blocks.COAL_BLOCK).pickaxe()),

    IRON_BLOCK(builder(Blocks.IRON_BLOCK).pickaxe().addBlockTags(BlockTags.BEACON_BASE_BLOCKS)),
    GOLD_BLOCK(builder(Blocks.GOLD_BLOCK).pickaxe().addBlockTags(BlockTags.BEACON_BASE_BLOCKS)),
    DIAMOND_BLOCK(builder(Blocks.DIAMOND_BLOCK).pickaxe().addBlockTags(BlockTags.BEACON_BASE_BLOCKS)),
    NETHERITE_BLOCK(builder(Blocks.NETHERITE_BLOCK).pickaxe().addBlockTags(BlockTags.BEACON_BASE_BLOCKS)),
    LAPIS_BLOCK(builder(Blocks.LAPIS_BLOCK).pickaxe()),
    EMERALD_BLOCK(builder(Blocks.EMERALD_BLOCK).pickaxe().addBlockTags(BlockTags.BEACON_BASE_BLOCKS)),

    GILDED_BLACKSTONE(builder(Blocks.GILDED_BLACKSTONE).pickaxe()),

    PUMPKIN(builder(Blocks.PUMPKIN).modelType(ModelType.CUBE_BOTTOM_TOP).axe()),
    MELON(builder(Blocks.MELON).modelType(ModelType.CUBE_BOTTOM_TOP).axe()),

    DRIED_KELP_BLOCK(builder(Blocks.DRIED_KELP_BLOCK).setTextures("dried_kelp_side", "dried_kelp_bottom", "dried_kelp_top").hoe()),

    BONE_BLOCK(builder(Blocks.BONE_BLOCK).modelType(ModelType.CUBE_BOTTOM_TOP).pickaxe()),

    HAY_BLOCK(builder(Blocks.HAY_BLOCK).modelType(ModelType.CUBE_BOTTOM_TOP).hoe()),

    BOOKSHELF(builder(Blocks.BOOKSHELF).setTextures("bookshelf", "oak_planks").axe()),

    SLIME_BLOCK(builder(Blocks.SLIME_BLOCK).modelType(ModelType.SLIME)),
    HONEY_BLOCK(builder(Blocks.HONEY_BLOCK).modelType(ModelType.HONEY)),

    REDSTONE_BLOCK(builder(Blocks.REDSTONE_BLOCK).pickaxe()),

    DEAD_TUBE_CORAL_BLOCK(builder(Blocks.DEAD_TUBE_CORAL_BLOCK).pickaxe()),
    TUBE_CORAL_BLOCK(builder(Blocks.TUBE_CORAL_BLOCK).pickaxe().associatedBlock(DEAD_TUBE_CORAL_BLOCK)),
    DEAD_BUBBLE_CORAL_BLOCK(builder(Blocks.DEAD_BUBBLE_CORAL_BLOCK).pickaxe()),
    BUBBLE_CORAL_BLOCK(builder(Blocks.BUBBLE_CORAL_BLOCK).pickaxe().associatedBlock(DEAD_BUBBLE_CORAL_BLOCK)),
    DEAD_BRAIN_CORAL_BLOCK(builder(Blocks.DEAD_BRAIN_CORAL_BLOCK).pickaxe()),
    BRAIN_CORAL_BLOCK(builder(Blocks.BRAIN_CORAL_BLOCK).pickaxe().associatedBlock(DEAD_BRAIN_CORAL_BLOCK)),
    DEAD_FIRE_CORAL_BLOCK(builder(Blocks.DEAD_FIRE_CORAL_BLOCK).pickaxe()),
    FIRE_CORAL_BLOCK(builder(Blocks.FIRE_CORAL_BLOCK).pickaxe().associatedBlock(DEAD_FIRE_CORAL_BLOCK)),
    DEAD_HORN_CORAL_BLOCK(builder(Blocks.DEAD_HORN_CORAL_BLOCK).pickaxe()),
    HORN_CORAL_BLOCK(builder(Blocks.HORN_CORAL_BLOCK).pickaxe().associatedBlock(DEAD_HORN_CORAL_BLOCK)),

    OCHRE_FROGLIGHT(builder(Blocks.OCHRE_FROGLIGHT).modelType(ModelType.CUBE_BOTTOM_TOP).hoe()),
    VERDANT_FROGLIGHT(builder(Blocks.VERDANT_FROGLIGHT).modelType(ModelType.CUBE_BOTTOM_TOP).hoe()),
    PEARLESCENT_FROGLIGHT(builder(Blocks.PEARLESCENT_FROGLIGHT).modelType(ModelType.CUBE_BOTTOM_TOP).hoe()),

    SCULK(builder(Blocks.SCULK).hoe()),

    PACKED_MUD(builder(Blocks.PACKED_MUD).pickaxe()),
    MUD(builder(Blocks.MUD).shovel()),

    ICE(builder(Blocks.ICE).modelType(ModelType.TRANSLUCENT).pickaxe()),
    PACKED_ICE(builder(Blocks.PACKED_ICE).pickaxe()),
    BLUE_ICE(builder(Blocks.BLUE_ICE).pickaxe()),

    BEDROCK(builder(Blocks.BEDROCK).pickaxe().addBlockTags(BlockTags.DRAGON_IMMUNE, BlockTags.WITHER_IMMUNE)),



    CHISELED_TUFF(builder(Blocks.CHISELED_TUFF).modelType(ModelType.CUBE_BOTTOM_TOP).setTextures("chiseled_tuff", "chiseled_tuff_top").pickaxe()),
    CHISELED_TUFF_BRICKS(builder(Blocks.CHISELED_TUFF_BRICKS).modelType(ModelType.CUBE_BOTTOM_TOP).setTextures("chiseled_tuff_bricks", "chiseled_tuff_bricks_top").pickaxe()),

    OXIDIZED_COPPER_GRATE(builder(Blocks.COPPER_GRATE.weathering().pick(WeatheringCopper.WeatherState.OXIDIZED)).setOxidationLevel(WeatheringCopper.WeatherState.OXIDIZED).modelType(ModelType.CUTOUT).pickaxe()),
    WEATHERED_COPPER_GRATE(builder(Blocks.COPPER_GRATE.weathering().pick(WeatheringCopper.WeatherState.WEATHERED)).setOxidationLevel(WeatheringCopper.WeatherState.WEATHERED).associatedBlock(OXIDIZED_COPPER_GRATE).modelType(ModelType.CUTOUT).pickaxe()),
    EXPOSED_COPPER_GRATE(builder(Blocks.COPPER_GRATE.weathering().pick(WeatheringCopper.WeatherState.EXPOSED)).setOxidationLevel(WeatheringCopper.WeatherState.EXPOSED).associatedBlock(WEATHERED_COPPER_GRATE).modelType(ModelType.CUTOUT).pickaxe()),
    COPPER_GRATE(builder(Blocks.COPPER_GRATE.weathering().pick(WeatheringCopper.WeatherState.UNAFFECTED)).setOxidationLevel(WeatheringCopper.WeatherState.UNAFFECTED).associatedBlock(EXPOSED_COPPER_GRATE).modelType(ModelType.CUTOUT).pickaxe()),

    WAXED_COPPER_GRATE(builder(Blocks.COPPER_GRATE.waxed().pick(WeatheringCopper.WeatherState.UNAFFECTED)).setAllTexture(ModelType.CUTOUT, "copper_grate").associatedBlock(COPPER_GRATE).pickaxe()),
    WAXED_EXPOSED_COPPER_GRATE(builder(Blocks.COPPER_GRATE.waxed().pick(WeatheringCopper.WeatherState.EXPOSED)).setAllTexture(ModelType.CUTOUT, "exposed_copper_grate").associatedBlock(EXPOSED_COPPER_GRATE).pickaxe()),
    WAXED_WEATHERED_COPPER_GRATE(builder(Blocks.COPPER_GRATE.waxed().pick(WeatheringCopper.WeatherState.WEATHERED)).setAllTexture(ModelType.CUTOUT, "weathered_copper_grate").associatedBlock(WEATHERED_COPPER_GRATE).pickaxe()),
    WAXED_OXIDIZED_COPPER_GRATE(builder(Blocks.COPPER_GRATE.waxed().pick(WeatheringCopper.WeatherState.OXIDIZED)).setAllTexture(ModelType.CUTOUT, "oxidized_copper_grate").associatedBlock(OXIDIZED_COPPER_GRATE).pickaxe()),

    OXIDIZED_CHISELED_COPPER(builder(Blocks.CHISELED_COPPER.weathering().pick(WeatheringCopper.WeatherState.OXIDIZED)).setOxidationLevel(WeatheringCopper.WeatherState.OXIDIZED).pickaxe()),
    WEATHERED_CHISELED_COPPER(builder(Blocks.CHISELED_COPPER.weathering().pick(WeatheringCopper.WeatherState.WEATHERED)).setOxidationLevel(WeatheringCopper.WeatherState.WEATHERED).associatedBlock(OXIDIZED_CHISELED_COPPER).pickaxe()),
    EXPOSED_CHISELED_COPPER(builder(Blocks.CHISELED_COPPER.weathering().pick(WeatheringCopper.WeatherState.EXPOSED)).setOxidationLevel(WeatheringCopper.WeatherState.EXPOSED).associatedBlock(WEATHERED_CHISELED_COPPER).pickaxe()),
    CHISELED_COPPER(builder(Blocks.CHISELED_COPPER.weathering().pick(WeatheringCopper.WeatherState.UNAFFECTED)).setOxidationLevel(WeatheringCopper.WeatherState.UNAFFECTED).associatedBlock(EXPOSED_CHISELED_COPPER).pickaxe()),

    WAXED_CHISELED_COPPER(builder(Blocks.CHISELED_COPPER.waxed().pick(WeatheringCopper.WeatherState.UNAFFECTED)).setAllTexture("chiseled_copper").associatedBlock(CHISELED_COPPER).pickaxe()),
    WAXED_EXPOSED_CHISELED_COPPER(builder(Blocks.CHISELED_COPPER.waxed().pick(WeatheringCopper.WeatherState.EXPOSED)).setAllTexture("exposed_chiseled_copper").associatedBlock(EXPOSED_CHISELED_COPPER).pickaxe()),
    WAXED_WEATHERED_CHISELED_COPPER(builder(Blocks.CHISELED_COPPER.waxed().pick(WeatheringCopper.WeatherState.WEATHERED)).setAllTexture("weathered_chiseled_copper").associatedBlock(WEATHERED_CHISELED_COPPER).pickaxe()),
    WAXED_OXIDIZED_CHISELED_COPPER(builder(Blocks.CHISELED_COPPER.waxed().pick(WeatheringCopper.WeatherState.OXIDIZED)).setAllTexture("oxidized_chiseled_copper").associatedBlock(OXIDIZED_CHISELED_COPPER).pickaxe()),
    ;



    public final Block parentBlock;
    public final ModelType modelType;
    public final boolean hasSlab;
    public final boolean hasStairs;
    public final boolean hasWall;

    public final List<TagKey<Block>> blockTags;
    //private final List<TagKey<Item>> itemTags;

    public final ModBlocks associatedBlock;

    public final String textureId;
    public final String bottomId;
    public final String topId;

    public final WeatheringCopper.WeatherState oxidationLevel;

    ModBlocks(Builder builder){
        this.parentBlock = builder.parentBlock;
        this.hasSlab = builder.hasSlab;
        this.hasStairs = builder.hasStairs;
        this.hasWall = builder.hasWall;
        this.blockTags = builder.blockTags;
        //this.itemTags = builder.itemTags;
        this.associatedBlock = builder.associatedBlock;
        this.modelType = builder.modelType;

        this.textureId = builder.sideTexture;
        this.bottomId = builder.bottomTexture;
        this.topId = builder.topTexture;

        this.oxidationLevel = builder.oxidationLevel;
    }

    public Boolean hasBlock(BlockType type){
        return switch (type){
            case SLAB -> hasSlab;
            case STAIRS -> hasStairs;
            case WALL -> hasWall;
        };
    }

    public BlockBehaviour.Properties getSettings(BlockType type){
        Block block = this.parentBlock;
        BlockBehaviour.Properties settings = BlockBehaviour.Properties.of()
                .sound(block.defaultBlockState().getSoundType())
                .lightLevel(state -> block.defaultBlockState().getLightEmission())
                .mapColor(block.defaultMapColor())
                .destroyTime(block.defaultDestroyTime())
                .explosionResistance(block.getExplosionResistance())
                .friction(block.getFriction())
                .speedFactor(block.getSpeedFactor())
                .pushReaction(block.defaultBlockState().getPistonPushReaction())
                .instrument(block.defaultBlockState().instrument())
                .jumpFactor(block.getJumpFactor())
                .setId(ResourceKey.create(Registries.BLOCK, getId(type)));

        if (block.defaultBlockState().ignitedByLava()){
            settings = settings.ignitedByLava();
        }

        if (!block.defaultBlockState().canOcclude()){
            settings = settings.noOcclusion();
        }

        if (block.defaultBlockState().requiresCorrectToolForDrops()){
            settings = settings.requiresCorrectToolForDrops();
        }

        if (block.defaultBlockState().isRandomlyTicking()){
            settings = settings.randomTicks();
        }

        return settings;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public Identifier getId(BlockType type){
        return Identifier.fromNamespaceAndPath(MoreSlabsStairsAndWalls.MOD_ID, this + "_" + type.toString().toLowerCase());
    }


    public Block getBlock(BlockType type){
        return getBlock(this, type);
    }

    public static Block getBlock(ModBlocks block, BlockType type){
        return ModRegistry.getBlock(block.getId(type));
    }

    public enum BlockType{
        SLAB,
        STAIRS,
        WALL
    }

    private static Builder builder(Block block){
        return new Builder(block);
    }

    private static class Builder {
        private final Block parentBlock;
        private boolean hasSlab = true;
        private boolean hasStairs = true;
        private boolean hasWall = true;
        private ModelType modelType = ModelType.CUBE_ALL;
        private ModBlocks associatedBlock = null;

        private final List<TagKey<Block>> blockTags;
        private final List<TagKey<Item>> itemTags;

        private String sideTexture = "";
        private String bottomTexture = "";
        private String topTexture = "";

        private WeatheringCopper.WeatherState oxidationLevel = null;

        public Builder(Block parentBlock) {
            this.parentBlock = parentBlock;
            this.blockTags = new ArrayList<>();
            this.itemTags = new ArrayList<>();
        }

        public Builder modelType(ModBlocks.ModelType modelType) {
            this.modelType = modelType;
            return this;
        }

        @SafeVarargs
        public final Builder addBlockTags(TagKey<Block>... blockTags) {
            this.blockTags.addAll(List.of(blockTags));
            return this;
        }

        @SafeVarargs
        public final Builder addItemTags(TagKey<Item>... itemTags) {
            this.itemTags.addAll(List.of(itemTags));
            return this;
        }

        public Builder hasSlab(boolean hasSlab) {
            this.hasSlab = hasSlab;
            return this;
        }

        public Builder hasStairs(boolean hasStairs) {
            this.hasStairs = hasStairs;
            return this;
        }

        public Builder hasWall(boolean hasWall) {
            this.hasWall = hasWall;
            return this;
        }

        public Builder associatedBlock(ModBlocks associatedBlock) {
            this.associatedBlock = associatedBlock;
            return this;
        }

        public Builder shovel() {
            this.blockTags.add(BlockTags.MINEABLE_WITH_SHOVEL);
            return this;
        }

        public Builder pickaxe() {
            this.blockTags.add(BlockTags.MINEABLE_WITH_PICKAXE);
            return this;
        }

        public Builder axe() {
            this.blockTags.add(BlockTags.MINEABLE_WITH_AXE);
            return this;
        }

        public Builder hoe() {
            this.blockTags.add(BlockTags.MINEABLE_WITH_HOE);
            return this;
        }

        public Builder setAllTexture(String textureId) {
            this.sideTexture = textureId;
            this.topTexture = textureId;
            this.bottomTexture = textureId;
            this.modelType = ModelType.CUSTOM;
            return this;
        }

        public Builder setAllTexture(ModelType modelType, String textureId) {
            this.sideTexture = textureId;
            this.topTexture = textureId;
            this.bottomTexture = textureId;
            this.modelType = modelType;
            return this;
        }

        public Builder setTextures(String sideTexture, String topTexture, String bottomTexture) {
            this.sideTexture = sideTexture;
            this.topTexture = topTexture;
            this.bottomTexture = bottomTexture;
            this.modelType = ModelType.CUSTOM_SIDE_BOTTOM_TOP;
            return this;
        }

        public Builder setTextures(String sideTexture, String topBottomTexture) {
            return this.setTextures(sideTexture, topBottomTexture, topBottomTexture);
        }

        public Builder setBottomTexture(String bottomTexture) {
            this.bottomTexture = bottomTexture;
            return this;
        }

        public Builder setTopTexture(String topTexture) {
            this.topTexture = topTexture;
            return this;
        }

        public Builder setSideTexture(String sideTexture) {
            this.sideTexture = sideTexture;
            return this;
        }

        public Builder setOxidationLevel(WeatheringCopper.WeatherState oxidationLevel) {
            this.oxidationLevel = oxidationLevel;
            return this;
        }

        public Builder wallOnly(){
            this.hasStairs = false;
            this.hasSlab = false;
            return this;
        }
    }

    public enum ModelType {
        CUBE_ALL,
        CUBE_COLUMN,
        CUBE_BOTTOM_TOP,
        LOG,
        GRASS,
        LEAVES,
        GLASS,
        PATH,
        CUSTOM,
        GLAZED_TERRACOTTA,
        ROTATABLE,
        SLIME,
        HONEY,
        TRANSLUCENT,
        ROOTS,
        CUTOUT,
        CUSTOM_SIDE_BOTTOM_TOP
    }
}


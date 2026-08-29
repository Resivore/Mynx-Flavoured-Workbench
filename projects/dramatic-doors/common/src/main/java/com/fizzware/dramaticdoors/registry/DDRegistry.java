package com.fizzware.dramaticdoors.registry;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.fizzware.dramaticdoors.blocks.ShortDoorBlock;
import com.fizzware.dramaticdoors.blocks.ShortWeatheringDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallSlidingDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallStableDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallWeatheringDoorBlock;
import com.fizzware.dramaticdoors.compat.Compats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import oshi.util.tuples.Pair;

public class DDRegistry
{
	public static final List<Pair<String, Block>> DOOR_BLOCKS = new ArrayList<Pair<String, Block>>();
	public static final List<Pair<String, Item>> DOOR_ITEMS = new ArrayList<Pair<String, Item>>();
	
	public static void registerVanilla() {
		// Register wooden doors.
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_OAK, DDNames.SHORT_OAK, Blocks.OAK_DOOR, BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SPRUCE, DDNames.SHORT_SPRUCE, Blocks.SPRUCE_DOOR, BlockSetType.SPRUCE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BIRCH, DDNames.SHORT_BIRCH, Blocks.BIRCH_DOOR, BlockSetType.BIRCH, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_JUNGLE, DDNames.SHORT_JUNGLE, Blocks.JUNGLE_DOOR, BlockSetType.JUNGLE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ACACIA, DDNames.SHORT_ACACIA, Blocks.ACACIA_DOOR, BlockSetType.ACACIA, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_DARK_OAK, DDNames.SHORT_DARK_OAK, Blocks.DARK_OAK_DOOR, BlockSetType.DARK_OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MANGROVE, DDNames.SHORT_MANGROVE, Blocks.MANGROVE_DOOR, BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CHERRY, DDNames.SHORT_CHERRY, Blocks.CHERRY_DOOR, BlockSetType.CHERRY, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BAMBOO, DDNames.SHORT_BAMBOO, Blocks.BAMBOO_DOOR, BlockSetType.BAMBOO, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CRIMSON, DDNames.SHORT_CRIMSON, Blocks.CRIMSON_DOOR, BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_WARPED, DDNames.SHORT_WARPED, Blocks.WARPED_DOOR, BlockSetType.WARPED, true);
		// Register iron door.
		if (Compats.modChecker.isModLoaded("immersive_weathering")) {
			DDRegistry.registerWeatheringDoorBlockAndItem(DDNames.TALL_IRON, DDNames.SHORT_IRON, Blocks.IRON_DOOR, BlockSetType.IRON, true, WeatherState.UNAFFECTED);
		}
		else {
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_IRON, DDNames.SHORT_IRON, Blocks.IRON_DOOR, BlockSetType.IRON, true);
		}
		// Register copper doors.
		DDRegistry.registerWeatheringDoorBlockAndItem(DDNames.TALL_COPPER, DDNames.SHORT_COPPER, Blocks.COPPER_DOOR.weathering().unaffected(), BlockSetType.COPPER, true, WeatherState.UNAFFECTED);
		DDRegistry.registerWeatheringDoorBlockAndItem(DDNames.TALL_EXPOSED_COPPER, DDNames.SHORT_EXPOSED_COPPER, Blocks.COPPER_DOOR.weathering().exposed(), BlockSetType.COPPER, true, WeatherState.EXPOSED);
		DDRegistry.registerWeatheringDoorBlockAndItem(DDNames.TALL_WEATHERED_COPPER, DDNames.SHORT_WEATHERED_COPPER, Blocks.COPPER_DOOR.weathering().weathered(), BlockSetType.COPPER, true, WeatherState.WEATHERED);
		DDRegistry.registerWeatheringDoorBlockAndItem(DDNames.TALL_OXIDIZED_COPPER, DDNames.SHORT_OXIDIZED_COPPER, Blocks.COPPER_DOOR.weathering().oxidized(), BlockSetType.COPPER, true, WeatherState.OXIDIZED);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_WAXED_COPPER, DDNames.SHORT_WAXED_COPPER, Blocks.COPPER_DOOR.waxed().unaffected(), BlockSetType.COPPER, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_WAXED_EXPOSED_COPPER, DDNames.SHORT_WAXED_EXPOSED_COPPER, Blocks.COPPER_DOOR.waxed().exposed(), BlockSetType.COPPER, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_WAXED_WEATHERED_COPPER, DDNames.SHORT_WAXED_WEATHERED_COPPER, Blocks.COPPER_DOOR.waxed().weathered(), BlockSetType.COPPER, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_WAXED_OXIDIZED_COPPER, DDNames.SHORT_WAXED_OXIDIZED_COPPER, Blocks.COPPER_DOOR.waxed().oxidized(), BlockSetType.COPPER, true);
	}
	
	// Register methods.
	public static void registerDoorBlockAndItem(String tallname, @Nullable String shortname, Block block, BlockSetType blocksettype, boolean includeShort) {
		Block tempBlock;
		Item tempItem;
		if (includeShort) {
			tempBlock = createDoorBlock(shortname, block, blocksettype, false);
			tempItem = createDoorItem(shortname, tempBlock);
			DOOR_BLOCKS.add(new Pair<String, Block>(shortname, tempBlock));
			DOOR_ITEMS.add(new Pair<String, Item>(shortname, tempItem));
		}
		tempBlock = createDoorBlock(tallname, block, blocksettype, true);
		tempItem = createDoorItem(tallname, tempBlock);
		DOOR_BLOCKS.add(new Pair<String, Block>(tallname, tempBlock));
		DOOR_ITEMS.add(new Pair<String, Item>(tallname, tempItem));
	}
	
	public static void registerWeatheringDoorBlockAndItem(String tallname, @Nullable String shortname, Block source, BlockSetType blocksettype, boolean includeShort, WeatheringCopper.WeatherState state) {
		Block tempBlock;
		Item tempItem;
		if (includeShort) {
			tempBlock = createCopperDoorBlock(shortname, source, blocksettype, false, state);
			tempItem = createDoorItem(shortname, tempBlock);
			DOOR_BLOCKS.add(new Pair<String, Block>(shortname, tempBlock));
			DOOR_ITEMS.add(new Pair<String, Item>(shortname, tempItem));
		}
		tempBlock = createCopperDoorBlock(tallname, source, blocksettype, true, state);
		tempItem = createDoorItem(tallname, tempBlock);
		DOOR_BLOCKS.add(new Pair<String, Block>(tallname, tempBlock));
		DOOR_ITEMS.add(new Pair<String, Item>(tallname, tempItem));
	}
	
	public static void registerSlidingDoorBlockAndItem(String tallname, @Nullable String shortname, Block block, BlockSetType blocksettype, boolean includeShort) {
		Block tempBlock;
		Item tempItem;
		if (includeShort) {
			tempBlock = createSlidingDoorBlock(shortname, block, blocksettype, false);
			tempItem = createDoorItem(shortname, tempBlock);
			DOOR_BLOCKS.add(new Pair<String, Block>(shortname, tempBlock));
			DOOR_ITEMS.add(new Pair<String, Item>(shortname, tempItem));
		}
		tempBlock = createSlidingDoorBlock(tallname, block, blocksettype, true);
		tempItem = createDoorItem(tallname, tempBlock);
		DOOR_BLOCKS.add(new Pair<String, Block>(tallname, tempBlock));
		DOOR_ITEMS.add(new Pair<String, Item>(tallname, tempItem));
	}
	
	public static void registerStableDoorBlockAndItem(String tallname, @Nullable String shortname, Block block, BlockSetType blocksettype, boolean includeShort) {
		Block tempBlock = createStableDoorBlock(tallname, block, blocksettype, true);
		Item tempItem = createDoorItem(tallname, tempBlock);
		DOOR_BLOCKS.add(new Pair<String, Block>(tallname, tempBlock));
		DOOR_ITEMS.add(new Pair<String, Item>(tallname, tempItem));
		if (includeShort) {
			tempBlock = createStableDoorBlock(shortname, block, blocksettype, false);
			tempItem = createDoorItem(shortname, tempBlock);
			DOOR_BLOCKS.add(new Pair<String, Block>(shortname, tempBlock));
			DOOR_ITEMS.add(new Pair<String, Item>(shortname, tempItem));
		}
	}
	
	// Create blocks and items.
	protected static Block createCopperDoorBlock(String name, Block source, BlockSetType blocksettype, boolean isTall, WeatheringCopper.WeatherState state) {
		Properties properties = Properties.ofFullCopy(source).setId(blockKey(name));
		return isTall ? new TallWeatheringDoorBlock(blocksettype, state, properties) : new ShortWeatheringDoorBlock(blocksettype, state, properties);
	}
	
	protected static Block createSlidingDoorBlock(String name, Block block, BlockSetType blocksettype, boolean isTall) {
		if (!isTall) {
			throw new IllegalArgumentException("Short version of Macaw sliding doors are currently not supported.");
		}
		Properties properties = Properties.ofFullCopy(block).setId(blockKey(name));
		return new TallSlidingDoorBlock(blocksettype, properties);
	}
	
	protected static Block createStableDoorBlock(String name, Block block, BlockSetType blocksettype, boolean isTall) {
		if (!isTall) {
			throw new IllegalArgumentException("Short version of Macaw stable doors are currently not supported.");
		}
		Properties properties = Properties.ofFullCopy(block).setId(blockKey(name));
		return new TallStableDoorBlock(blocksettype, properties);
	}
	
	protected static Block createDoorBlock(String name, Block block, BlockSetType blocksettype, boolean isTall) {
		Properties properties = Properties.ofFullCopy(block).setId(blockKey(name));
		return isTall ? new TallDoorBlock(blocksettype, properties) : new ShortDoorBlock(blocksettype, properties);
	}

	public static void registerDoorBlockAndItem(String tallname, @Nullable String shortname, Properties properties, BlockSetType blocksettype, boolean includeShort) {
		Block tempBlock;
		if (includeShort) {
			tempBlock = new ShortDoorBlock(blocksettype, properties.setId(blockKey(shortname)));
			DOOR_BLOCKS.add(new Pair<>(shortname, tempBlock));
			DOOR_ITEMS.add(new Pair<>(shortname, createDoorItem(shortname, tempBlock)));
		}
		tempBlock = new TallDoorBlock(blocksettype, properties.setId(blockKey(tallname)));
		DOOR_BLOCKS.add(new Pair<>(tallname, tempBlock));
		DOOR_ITEMS.add(new Pair<>(tallname, createDoorItem(tallname, tempBlock)));
	}
	
	public static Item createDoorItem(String name, Block block) {
		return new BlockItem(block, new Item.Properties().setId(itemKey(name)));
	}

	public static Item createDoorItem(String name, Block block, boolean fireResistant) {
		Item.Properties properties = new Item.Properties().setId(itemKey(name));
		return new BlockItem(block, fireResistant ? properties.fireResistant() : properties);
	}

	private static ResourceKey<Block> blockKey(String name) {
		return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("dramaticdoors", name));
	}

	private static ResourceKey<Item> itemKey(String name) {
		return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dramaticdoors", name));
	}
	
	/* Utility functions for getting blocks by key. */
	public static Block getBlockByKey(Identifier loc) {
    	return getBlockByKey(loc, Blocks.OAK_DOOR);
    }
	
	public static Block getBlockByKey(Identifier loc, Block fallback) {
    	if (BuiltInRegistries.BLOCK.containsKey(loc)) {
    		return BuiltInRegistries.BLOCK.getValue(loc);
    	}
    	else {
    		return fallback; // Fallback
    	}
    }
    
    public static Block getBlockFromIdentifier(Identifier resource) {
    	return getBlockFromIdentifier(resource, Blocks.OAK_DOOR);
    }
    
    public static Block getBlockFromIdentifier(Identifier resource, Block fallback) {
    	return BuiltInRegistries.BLOCK.getOptional(resource).orElse(fallback);
    }
}

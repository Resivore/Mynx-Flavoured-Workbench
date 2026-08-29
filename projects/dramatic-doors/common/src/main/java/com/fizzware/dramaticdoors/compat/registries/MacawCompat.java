package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class MacawCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_STORE, DDNames.SHORT_MACAW_STORE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "store_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SLIDING_GLASS, DDNames.SHORT_MACAW_SLIDING_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "store_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JAIL, DDNames.SHORT_MACAW_JAIL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jail_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_METAL, DDNames.SHORT_MACAW_METAL_REINFORCED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "reinforced_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_METAL_HOSPITAL, DDNames.SHORT_MACAW_METAL_HOSPITAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "metal_hospital_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_METAL_REINFORCED, DDNames.SHORT_MACAW_METAL_REINFORCED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "metal_reinforced_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_METAL_WARNING, DDNames.SHORT_MACAW_METAL_WARNING, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "metal_warning_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_METAL_WINDOWED, DDNames.SHORT_MACAW_METAL_WINDOWED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "metal_windowed_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
		
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_BARN, DDNames.SHORT_MACAW_OAK_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_barn_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_BARN, DDNames.SHORT_MACAW_SPRUCE_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_barn_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_BARN, DDNames.SHORT_MACAW_BIRCH_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_barn_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_BARN, DDNames.SHORT_MACAW_JUNGLE_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_barn_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_BARN, DDNames.SHORT_MACAW_ACACIA_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_barn_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_BARN, DDNames.SHORT_MACAW_DARK_OAK_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_barn_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_BARN, DDNames.SHORT_MACAW_MANGROVE_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_barn_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_BARN, DDNames.SHORT_MACAW_CHERRY_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_barn_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_BARN, DDNames.SHORT_MACAW_BAMBOO_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_barn_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_BARN, DDNames.SHORT_MACAW_CRIMSON_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_barn_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_BARN, DDNames.SHORT_MACAW_WARPED_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_barn_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);
		
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_BARN_GLASS, DDNames.SHORT_MACAW_OAK_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_barn_glass_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_BARN_GLASS, DDNames.SHORT_MACAW_SPRUCE_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_barn_glass_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_BARN_GLASS, DDNames.SHORT_MACAW_BIRCH_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_barn_glass_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_BARN_GLASS, DDNames.SHORT_MACAW_JUNGLE_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_barn_glass_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_BARN_GLASS, DDNames.SHORT_MACAW_ACACIA_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_barn_glass_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_BARN_GLASS, DDNames.SHORT_MACAW_DARK_OAK_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_barn_glass_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_BARN_GLASS, DDNames.SHORT_MACAW_MANGROVE_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_barn_glass_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_BARN_GLASS, DDNames.SHORT_MACAW_CHERRY_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_barn_glass_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_BARN_GLASS, DDNames.SHORT_MACAW_BAMBOO_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_barn_glass_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_BARN_GLASS, DDNames.SHORT_MACAW_CRIMSON_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_barn_glass_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_BARN_GLASS, DDNames.SHORT_MACAW_WARPED_BARN_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_barn_glass_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_OAK_STABLE, DDNames.SHORT_MACAW_OAK_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_stable_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_STABLE, DDNames.SHORT_MACAW_SPRUCE_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_stable_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_STABLE, DDNames.SHORT_MACAW_BIRCH_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_stable_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_STABLE, DDNames.SHORT_MACAW_JUNGLE_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_stable_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_STABLE, DDNames.SHORT_MACAW_ACACIA_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_stable_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_STABLE, DDNames.SHORT_MACAW_DARK_OAK_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_stable_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_STABLE, DDNames.SHORT_MACAW_MANGROVE_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_stable_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_STABLE, DDNames.SHORT_MACAW_CHERRY_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_stable_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_STABLE, DDNames.SHORT_MACAW_BAMBOO_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_stable_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_STABLE, DDNames.SHORT_MACAW_CRIMSON_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_stable_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_STABLE, DDNames.SHORT_MACAW_WARPED_STABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_stable_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_OAK_STABLE_HEAD, DDNames.SHORT_MACAW_OAK_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_stable_head_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_STABLE_HEAD, DDNames.SHORT_MACAW_SPRUCE_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_stable_head_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_STABLE_HEAD, DDNames.SHORT_MACAW_BIRCH_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_stable_head_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_STABLE_HEAD, DDNames.SHORT_MACAW_JUNGLE_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_stable_head_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_STABLE_HEAD, DDNames.SHORT_MACAW_ACACIA_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_stable_head_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_STABLE_HEAD, DDNames.SHORT_MACAW_DARK_OAK_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_stable_head_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_STABLE_HEAD, DDNames.SHORT_MACAW_MANGROVE_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_stable_head_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_STABLE_HEAD, DDNames.SHORT_MACAW_CHERRY_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_stable_head_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_STABLE_HEAD, DDNames.SHORT_MACAW_BAMBOO_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_stable_head_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_STABLE_HEAD, DDNames.SHORT_MACAW_CRIMSON_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_stable_head_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerStableDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_STABLE_HEAD, DDNames.SHORT_MACAW_WARPED_STABLE_HEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_stable_head_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_BARK_GLASS, DDNames.SHORT_MACAW_OAK_BARK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_bark_glass_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_BARK_GLASS, DDNames.SHORT_MACAW_SPRUCE_BARK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_bark_glass_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_BARK_GLASS, DDNames.SHORT_MACAW_BIRCH_BARK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_bark_glass_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_BARK_GLASS, DDNames.SHORT_MACAW_JUNGLE_BARK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_bark_glass_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_BARK_GLASS, DDNames.SHORT_MACAW_ACACIA_BARK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_bark_glass_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_BARK_GLASS, DDNames.SHORT_MACAW_DARK_OAK_BARK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_bark_glass_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_BARK_GLASS, DDNames.SHORT_MACAW_MANGROVE_BARK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_bark_glass_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_BARK_GLASS, DDNames.SHORT_MACAW_CHERRY_BARK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_bark_glass_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_BARK_GLASS, DDNames.SHORT_MACAW_BAMBOO_BARK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_bark_glass_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_STEM_GLASS, DDNames.SHORT_MACAW_CRIMSON_STEM_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_stem_glass_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_STEM_GLASS, DDNames.SHORT_MACAW_WARPED_STEM_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_stem_glass_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_GLASS, DDNames.SHORT_MACAW_OAK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_glass_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_GLASS, DDNames.SHORT_MACAW_SPRUCE_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_glass_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_GLASS, DDNames.SHORT_MACAW_BIRCH_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_glass_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_GLASS, DDNames.SHORT_MACAW_JUNGLE_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_glass_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_GLASS, DDNames.SHORT_MACAW_ACACIA_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_glass_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_GLASS, DDNames.SHORT_MACAW_DARK_OAK_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_glass_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_GLASS, DDNames.SHORT_MACAW_MANGROVE_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_glass_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_GLASS, DDNames.SHORT_MACAW_CHERRY_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_glass_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_GLASS, DDNames.SHORT_MACAW_BAMBOO_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_glass_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_GLASS, DDNames.SHORT_MACAW_CRIMSON_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_glass_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_GLASS, DDNames.SHORT_MACAW_WARPED_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_glass_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_MODERN, DDNames.SHORT_MACAW_OAK_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_modern_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_MODERN, DDNames.SHORT_MACAW_SPRUCE_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_modern_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_MODERN, DDNames.SHORT_MACAW_BIRCH_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_modern_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_MODERN, DDNames.SHORT_MACAW_JUNGLE_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_modern_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_MODERN, DDNames.SHORT_MACAW_ACACIA_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_modern_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_MODERN, DDNames.SHORT_MACAW_DARK_OAK_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_modern_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_MODERN, DDNames.SHORT_MACAW_MANGROVE_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_modern_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_MODERN, DDNames.SHORT_MACAW_CHERRY_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_modern_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_MODERN, DDNames.SHORT_MACAW_BAMBOO_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_modern_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_MODERN, DDNames.SHORT_MACAW_CRIMSON_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_modern_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_MODERN, DDNames.SHORT_MACAW_WARPED_MODERN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_modern_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_OAK_JAPANESE, DDNames.SHORT_MACAW_OAK_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_japanese_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_JAPANESE, DDNames.SHORT_MACAW_SPRUCE_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_japanese_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_JAPANESE, DDNames.SHORT_MACAW_BIRCH_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_japanese_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_JAPANESE, DDNames.SHORT_MACAW_JUNGLE_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_japanese_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_JAPANESE, DDNames.SHORT_MACAW_ACACIA_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_japanese_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_JAPANESE, DDNames.SHORT_MACAW_DARK_OAK_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_japanese_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_JAPANESE, DDNames.SHORT_MACAW_MANGROVE_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_japanese_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_JAPANESE, DDNames.SHORT_MACAW_CHERRY_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_japanese_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_JAPANESE, DDNames.SHORT_MACAW_BAMBOO_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_japanese_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_JAPANESE, DDNames.SHORT_MACAW_CRIMSON_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_japanese_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_JAPANESE, DDNames.SHORT_MACAW_WARPED_JAPANESE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_japanese_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_OAK_JAPANESE2, DDNames.SHORT_MACAW_OAK_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_japanese2_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_JAPANESE2, DDNames.SHORT_MACAW_SPRUCE_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_japanese2_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_JAPANESE2, DDNames.SHORT_MACAW_BIRCH_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_japanese2_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_JAPANESE2, DDNames.SHORT_MACAW_JUNGLE_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_japanese2_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_JAPANESE2, DDNames.SHORT_MACAW_ACACIA_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_japanese2_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_JAPANESE2, DDNames.SHORT_MACAW_DARK_OAK_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_japanese2_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_JAPANESE2, DDNames.SHORT_MACAW_MANGROVE_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_japanese2_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_JAPANESE2, DDNames.SHORT_MACAW_CHERRY_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_japanese2_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_JAPANESE2, DDNames.SHORT_MACAW_BAMBOO_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_japanese2_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_JAPANESE2, DDNames.SHORT_MACAW_CRIMSON_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_japanese2_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerSlidingDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_JAPANESE2, DDNames.SHORT_MACAW_WARPED_JAPANESE2, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_japanese2_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_CLASSIC, DDNames.SHORT_MACAW_SPRUCE_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_classic_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_CLASSIC, DDNames.SHORT_MACAW_BIRCH_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_classic_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_CLASSIC, DDNames.SHORT_MACAW_JUNGLE_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_classic_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_CLASSIC, DDNames.SHORT_MACAW_ACACIA_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_classic_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_CLASSIC, DDNames.SHORT_MACAW_DARK_OAK_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_classic_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_CLASSIC, DDNames.SHORT_MACAW_MANGROVE_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_classic_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_CLASSIC, DDNames.SHORT_MACAW_CHERRY_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_classic_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_CLASSIC, DDNames.SHORT_MACAW_BAMBOO_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_classic_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_CLASSIC, DDNames.SHORT_MACAW_CRIMSON_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_classic_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_CLASSIC, DDNames.SHORT_MACAW_WARPED_CLASSIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_classic_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_COTTAGE, DDNames.SHORT_MACAW_OAK_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_cottage_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_COTTAGE, DDNames.SHORT_MACAW_BIRCH_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_cottage_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_COTTAGE, DDNames.SHORT_MACAW_JUNGLE_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_cottage_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_COTTAGE, DDNames.SHORT_MACAW_ACACIA_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_cottage_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_COTTAGE, DDNames.SHORT_MACAW_DARK_OAK_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_cottage_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_COTTAGE, DDNames.SHORT_MACAW_MANGROVE_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_cottage_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_COTTAGE, DDNames.SHORT_MACAW_CHERRY_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_cottage_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_COTTAGE, DDNames.SHORT_MACAW_BAMBOO_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_cottage_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_COTTAGE, DDNames.SHORT_MACAW_CRIMSON_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_cottage_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_COTTAGE, DDNames.SHORT_MACAW_WARPED_COTTAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_cottage_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_PAPER, DDNames.SHORT_MACAW_OAK_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_paper_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_PAPER, DDNames.SHORT_MACAW_SPRUCE_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_paper_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_PAPER, DDNames.SHORT_MACAW_JUNGLE_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_paper_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_PAPER, DDNames.SHORT_MACAW_ACACIA_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_paper_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_PAPER, DDNames.SHORT_MACAW_DARK_OAK_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_paper_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_PAPER, DDNames.SHORT_MACAW_MANGROVE_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_paper_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_PAPER, DDNames.SHORT_MACAW_CHERRY_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_paper_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_PAPER, DDNames.SHORT_MACAW_BAMBOO_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_paper_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_PAPER, DDNames.SHORT_MACAW_CRIMSON_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_paper_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_PAPER, DDNames.SHORT_MACAW_WARPED_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_paper_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_BEACH, DDNames.SHORT_MACAW_OAK_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_beach_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_BEACH, DDNames.SHORT_MACAW_SPRUCE_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_beach_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_BEACH, DDNames.SHORT_MACAW_BIRCH_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_beach_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_BEACH, DDNames.SHORT_MACAW_ACACIA_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_beach_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_BEACH, DDNames.SHORT_MACAW_DARK_OAK_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_beach_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_BEACH, DDNames.SHORT_MACAW_MANGROVE_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_beach_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_BEACH, DDNames.SHORT_MACAW_CHERRY_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_beach_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_BEACH, DDNames.SHORT_MACAW_BAMBOO_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_beach_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_BEACH, DDNames.SHORT_MACAW_CRIMSON_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_beach_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_BEACH, DDNames.SHORT_MACAW_WARPED_BEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_beach_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_TROPICAL, DDNames.SHORT_MACAW_OAK_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_tropical_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_TROPICAL, DDNames.SHORT_MACAW_SPRUCE_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_tropical_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_TROPICAL, DDNames.SHORT_MACAW_BIRCH_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_tropical_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_TROPICAL, DDNames.SHORT_MACAW_JUNGLE_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_tropical_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_TROPICAL, DDNames.SHORT_MACAW_DARK_OAK_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_tropical_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_TROPICAL, DDNames.SHORT_MACAW_MANGROVE_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_tropical_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_TROPICAL, DDNames.SHORT_MACAW_CHERRY_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_tropical_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_TROPICAL, DDNames.SHORT_MACAW_BAMBOO_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_tropical_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_TROPICAL, DDNames.SHORT_MACAW_CRIMSON_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_tropical_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_TROPICAL, DDNames.SHORT_MACAW_WARPED_TROPICAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_tropical_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_FOUR_PANEL, DDNames.SHORT_MACAW_OAK_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_four_panel_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_FOUR_PANEL, DDNames.SHORT_MACAW_SPRUCE_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_four_panel_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_FOUR_PANEL, DDNames.SHORT_MACAW_BIRCH_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_four_panel_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_FOUR_PANEL, DDNames.SHORT_MACAW_JUNGLE_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_four_panel_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_FOUR_PANEL, DDNames.SHORT_MACAW_ACACIA_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_four_panel_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_FOUR_PANEL, DDNames.SHORT_MACAW_MANGROVE_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_four_panel_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_FOUR_PANEL, DDNames.SHORT_MACAW_CHERRY_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_four_panel_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_FOUR_PANEL, DDNames.SHORT_MACAW_BAMBOO_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_four_panel_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_FOUR_PANEL, DDNames.SHORT_MACAW_CRIMSON_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_four_panel_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_FOUR_PANEL, DDNames.SHORT_MACAW_WARPED_FOUR_PANEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_four_panel_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_SWAMP, DDNames.SHORT_MACAW_OAK_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_swamp_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_SWAMP, DDNames.SHORT_MACAW_SPRUCE_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_swamp_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_SWAMP, DDNames.SHORT_MACAW_BIRCH_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_swamp_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_SWAMP, DDNames.SHORT_MACAW_JUNGLE_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_swamp_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_SWAMP, DDNames.SHORT_MACAW_ACACIA_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_swamp_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_SWAMP, DDNames.SHORT_MACAW_DARK_OAK_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_swamp_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_SWAMP, DDNames.SHORT_MACAW_CHERRY_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_swamp_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_SWAMP, DDNames.SHORT_MACAW_BAMBOO_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_swamp_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_SWAMP, DDNames.SHORT_MACAW_CRIMSON_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_swamp_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_SWAMP, DDNames.SHORT_MACAW_WARPED_SWAMP, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_swamp_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_WAFFLE, DDNames.SHORT_MACAW_OAK_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_waffle_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_WAFFLE, DDNames.SHORT_MACAW_SPRUCE_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_waffle_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_WAFFLE, DDNames.SHORT_MACAW_BIRCH_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_waffle_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_WAFFLE, DDNames.SHORT_MACAW_JUNGLE_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_waffle_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_WAFFLE, DDNames.SHORT_MACAW_ACACIA_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_waffle_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_WAFFLE, DDNames.SHORT_MACAW_DARK_OAK_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_waffle_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_WAFFLE, DDNames.SHORT_MACAW_MANGROVE_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_waffle_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_WAFFLE, DDNames.SHORT_MACAW_BAMBOO_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_waffle_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_WAFFLE, DDNames.SHORT_MACAW_CRIMSON_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_waffle_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_WAFFLE, DDNames.SHORT_MACAW_WARPED_WAFFLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_waffle_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_BAMBOO, DDNames.SHORT_MACAW_OAK_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_bamboo_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_BAMBOO, DDNames.SHORT_MACAW_SPRUCE_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_bamboo_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_BAMBOO, DDNames.SHORT_MACAW_BIRCH_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_bamboo_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_BAMBOO, DDNames.SHORT_MACAW_JUNGLE_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_bamboo_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_BAMBOO, DDNames.SHORT_MACAW_ACACIA_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_bamboo_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_BAMBOO, DDNames.SHORT_MACAW_DARK_OAK_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_bamboo_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_BAMBOO, DDNames.SHORT_MACAW_MANGROVE_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_bamboo_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_BAMBOO, DDNames.SHORT_MACAW_CHERRY_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_bamboo_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_BAMBOO, DDNames.SHORT_MACAW_CRIMSON_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_bamboo_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_BAMBOO, DDNames.SHORT_MACAW_WARPED_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_bamboo_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_NETHER, DDNames.SHORT_MACAW_OAK_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_nether_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_NETHER, DDNames.SHORT_MACAW_SPRUCE_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_nether_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_NETHER, DDNames.SHORT_MACAW_BIRCH_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_nether_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_NETHER, DDNames.SHORT_MACAW_JUNGLE_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_nether_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_NETHER, DDNames.SHORT_MACAW_ACACIA_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_nether_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_NETHER, DDNames.SHORT_MACAW_DARK_OAK_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_nether_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_NETHER, DDNames.SHORT_MACAW_MANGROVE_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_nether_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_NETHER, DDNames.SHORT_MACAW_CHERRY_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_nether_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_NETHER, DDNames.SHORT_MACAW_BAMBOO_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_nether_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_WARPED_NETHER, DDNames.SHORT_MACAW_WARPED_NETHER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "warped_nether_door"), Blocks.WARPED_DOOR), BlockSetType.WARPED, false);

		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_OAK_MYSTIC, DDNames.SHORT_MACAW_OAK_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "oak_mystic_door"), Blocks.OAK_DOOR), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_SPRUCE_MYSTIC, DDNames.SHORT_MACAW_SPRUCE_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "spruce_mystic_door"), Blocks.SPRUCE_DOOR), BlockSetType.SPRUCE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BIRCH_MYSTIC, DDNames.SHORT_MACAW_BIRCH_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "birch_mystic_door"), Blocks.BIRCH_DOOR), BlockSetType.BIRCH, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_JUNGLE_MYSTIC, DDNames.SHORT_MACAW_JUNGLE_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "jungle_mystic_door"), Blocks.JUNGLE_DOOR), BlockSetType.JUNGLE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_ACACIA_MYSTIC, DDNames.SHORT_MACAW_ACACIA_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "acacia_mystic_door"), Blocks.ACACIA_DOOR), BlockSetType.ACACIA, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_DARK_OAK_MYSTIC, DDNames.SHORT_MACAW_DARK_OAK_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_mystic_door"), Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_MANGROVE_MYSTIC, DDNames.SHORT_MACAW_MANGROVE_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_mystic_door"), Blocks.MANGROVE_DOOR), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CHERRY_MYSTIC, DDNames.SHORT_MACAW_CHERRY_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "cherry_mystic_door"), Blocks.CHERRY_DOOR), BlockSetType.CHERRY, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_BAMBOO_MYSTIC, DDNames.SHORT_MACAW_BAMBOO_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_mystic_door"), Blocks.BAMBOO_DOOR), BlockSetType.BAMBOO, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MACAW_CRIMSON_MYSTIC, DDNames.SHORT_MACAW_CRIMSON_MYSTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mcwdoors", "crimson_mystic_door"), Blocks.CRIMSON_DOOR), BlockSetType.CRIMSON, false);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_STORE, Identifier.fromNamespaceAndPath("mcwdoors", "store_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SLIDING_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "sliding_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JAIL, Identifier.fromNamespaceAndPath("mcwdoors", "jail_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_METAL, Identifier.fromNamespaceAndPath("mcwdoors", "metal_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_METAL_HOSPITAL, Identifier.fromNamespaceAndPath("mcwdoors", "metal_hospital_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_METAL_REINFORCED, Identifier.fromNamespaceAndPath("mcwdoors", "metal_reinforced_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_METAL_WARNING, Identifier.fromNamespaceAndPath("mcwdoors", "metal_warning_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_METAL_WINDOWED, Identifier.fromNamespaceAndPath("mcwdoors", "metal_windowed_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "oak_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "birch_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_barn_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "warped_barn_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "oak_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "birch_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_barn_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "warped_barn_glass_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "oak_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "birch_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_stable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "warped_stable_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "oak_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "birch_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_stable_head_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "warped_stable_head_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "oak_bark_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_bark_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "birch_bark_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_bark_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_bark_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_bark_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_bark_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_bark_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_bark_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_STEM_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_stem_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_STEM_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "warped_stem_glass_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "oak_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "birch_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "warped_glass_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "oak_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "birch_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_modern_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "warped_modern_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "oak_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "birch_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_japanese_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "warped_japanese_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "oak_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "birch_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_japanese2_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "warped_japanese2_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_classic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "birch_classic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_classic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_classic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_classic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_classic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_classic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_classic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_classic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "warped_classic_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "oak_cottage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "birch_cottage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_cottage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_cottage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_cottage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_cottage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_cottage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_cottage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_cottage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "warped_cottage_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "oak_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "warped_paper_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "oak_beach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_beach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "birch_beach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_beach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_beach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_beach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_beach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_beach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_beach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "warped_beach_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "oak_tropical_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_tropical_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "birch_tropical_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_tropical_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_tropical_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_tropical_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_tropical_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_tropical_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_tropical_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "warped_tropical_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "oak_four_panel_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_four_panel_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "birch_four_panel_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_four_panel_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_four_panel_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_four_panel_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_four_panel_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_four_panel_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_four_panel_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "warped_four_panel_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "oak_swamp_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_swamp_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "birch_swamp_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_swamp_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_swamp_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_swamp_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_swamp_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_swamp_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_swamp_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "warped_swamp_door"));

		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "oak_waffle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_waffle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "birch_waffle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_waffle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_waffle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_waffle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_waffle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_waffle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_waffle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "warped_waffle_door"));

		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "oak_bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "birch_bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "warped_bamboo_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "oak_nether_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_nether_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "birch_nether_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_nether_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_nether_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_nether_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_nether_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_nether_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_nether_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_WARPED_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "warped_nether_door"));
		
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_OAK_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "oak_mystic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_SPRUCE_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_mystic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BIRCH_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "birch_mystic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_JUNGLE_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_mystic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_ACACIA_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_mystic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_DARK_OAK_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_mystic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_MANGROVE_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_mystic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CHERRY_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_mystic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_BAMBOO_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_mystic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MACAW_CRIMSON_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_mystic_door"));
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_STORE, Identifier.fromNamespaceAndPath("mcwdoors", "store_door"), "tall_macaw_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SLIDING_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "sliding_glass_door"), "tall_macaw_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JAIL, Identifier.fromNamespaceAndPath("mcwdoors", "jail_door"), "tall_macaw_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_METAL, Identifier.fromNamespaceAndPath("mcwdoors", "metal_door"), "tall_macaw_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_METAL_HOSPITAL, Identifier.fromNamespaceAndPath("mcwdoors", "metal_hospital_door"), "tall_macaw_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_METAL_REINFORCED, Identifier.fromNamespaceAndPath("mcwdoors", "metal_reinforced_door"), "tall_macaw_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_METAL_WARNING, Identifier.fromNamespaceAndPath("mcwdoors", "metal_warning_door"), "tall_macaw_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_METAL_WINDOWED, Identifier.fromNamespaceAndPath("mcwdoors", "metal_windowed_door"), "tall_macaw_metal_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "oak_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "birch_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_barn_door"), "tall_macaw_barn_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_BARN, Identifier.fromNamespaceAndPath("mcwdoors", "warped_barn_door"), "tall_macaw_barn_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "oak_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "birch_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_barn_glass_door"), "tall_macaw_barn_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_BARN_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "warped_barn_glass_door"), "tall_macaw_barn_glass_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "oak_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "birch_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_stable_door"), "tall_macaw_stable_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_STABLE, Identifier.fromNamespaceAndPath("mcwdoors", "warped_stable_door"), "tall_macaw_stable_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "oak_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "birch_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_stable_head_door"), "tall_macaw_stable_head_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_STABLE_HEAD, Identifier.fromNamespaceAndPath("mcwdoors", "warped_stable_head_door"), "tall_macaw_stable_head_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "oak_bark_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_bark_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "birch_bark_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_bark_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_bark_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_bark_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_bark_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_bark_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_BARK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_bark_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_STEM_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_stem_glass_door"), "tall_macaw_bark_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_STEM_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "warped_stem_glass_door"), "tall_macaw_bark_glass_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "oak_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "birch_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_glass_door"), "tall_macaw_glass_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_GLASS, Identifier.fromNamespaceAndPath("mcwdoors", "warped_glass_door"), "tall_macaw_glass_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "oak_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "birch_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_modern_door"), "tall_macaw_modern_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_MODERN, Identifier.fromNamespaceAndPath("mcwdoors", "warped_modern_door"), "tall_macaw_modern_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "oak_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "birch_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_japanese_door"), "tall_macaw_japanese_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_JAPANESE, Identifier.fromNamespaceAndPath("mcwdoors", "warped_japanese_door"), "tall_macaw_japanese_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "oak_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "birch_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_japanese2_door"), "tall_macaw_japanese2_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_JAPANESE2, Identifier.fromNamespaceAndPath("mcwdoors", "warped_japanese2_door"), "tall_macaw_japanese2_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_classic_door"), "tall_macaw_classic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "birch_classic_door"), "tall_macaw_classic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_classic_door"), "tall_macaw_classic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_classic_door"), "tall_macaw_classic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_classic_door"), "tall_macaw_classic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_classic_door"), "tall_macaw_classic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_classic_door"), "tall_macaw_classic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_classic_door"), "tall_macaw_classic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_classic_door"), "tall_macaw_classic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_CLASSIC, Identifier.fromNamespaceAndPath("mcwdoors", "warped_classic_door"), "tall_macaw_classic_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "oak_cottage_door"), "tall_macaw_cottage_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "birch_cottage_door"), "tall_macaw_cottage_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_cottage_door"), "tall_macaw_cottage_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_cottage_door"), "tall_macaw_cottage_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_cottage_door"), "tall_macaw_cottage_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_cottage_door"), "tall_macaw_cottage_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_cottage_door"), "tall_macaw_cottage_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_cottage_door"), "tall_macaw_cottage_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_cottage_door"), "tall_macaw_cottage_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_COTTAGE, Identifier.fromNamespaceAndPath("mcwdoors", "warped_cottage_door"), "tall_macaw_cottage_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "oak_paper_door"), "tall_macaw_paper_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_paper_door"), "tall_macaw_paper_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_paper_door"), "tall_macaw_paper_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_paper_door"), "tall_macaw_paper_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_paper_door"), "tall_macaw_paper_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_paper_door"), "tall_macaw_paper_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_paper_door"), "tall_macaw_paper_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_paper_door"), "tall_macaw_paper_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_paper_door"), "tall_macaw_paper_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_PAPER, Identifier.fromNamespaceAndPath("mcwdoors", "warped_paper_door"), "tall_macaw_paper_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "oak_beach_door"), "tall_macaw_beach_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_beach_door"), "tall_macaw_beach_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "birch_beach_door"), "tall_macaw_beach_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_beach_door"), "tall_macaw_beach_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_beach_door"), "tall_macaw_beach_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_beach_door"), "tall_macaw_beach_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_beach_door"), "tall_macaw_beach_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_beach_door"), "tall_macaw_beach_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_beach_door"), "tall_macaw_beach_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_BEACH, Identifier.fromNamespaceAndPath("mcwdoors", "warped_beach_door"), "tall_macaw_beach_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "oak_tropical_door"), "tall_macaw_tropical_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_tropical_door"), "tall_macaw_tropical_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "birch_tropical_door"), "tall_macaw_tropical_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_tropical_door"), "tall_macaw_tropical_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_tropical_door"), "tall_macaw_tropical_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_tropical_door"), "tall_macaw_tropical_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_tropical_door"), "tall_macaw_tropical_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_tropical_door"), "tall_macaw_tropical_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_tropical_door"), "tall_macaw_tropical_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_TROPICAL, Identifier.fromNamespaceAndPath("mcwdoors", "warped_tropical_door"), "tall_macaw_tropical_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "oak_four_panel_door"), "tall_macaw_four_panel_door");			
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_four_panel_door"), "tall_macaw_four_panel_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "birch_four_panel_door"), "tall_macaw_four_panel_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_four_panel_door"), "tall_macaw_four_panel_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_four_panel_door"), "tall_macaw_four_panel_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_four_panel_door"), "tall_macaw_four_panel_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_four_panel_door"), "tall_macaw_four_panel_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_four_panel_door"), "tall_macaw_four_panel_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_four_panel_door"), "tall_macaw_four_panel_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_FOUR_PANEL, Identifier.fromNamespaceAndPath("mcwdoors", "warped_four_panel_door"), "tall_macaw_four_panel_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "oak_swamp_door"), "tall_macaw_swamp_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_swamp_door"), "tall_macaw_swamp_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "birch_swamp_door"), "tall_macaw_swamp_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_swamp_door"), "tall_macaw_swamp_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_swamp_door"), "tall_macaw_swamp_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_swamp_door"), "tall_macaw_swamp_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_swamp_door"), "tall_macaw_swamp_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_swamp_door"), "tall_macaw_swamp_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_swamp_door"), "tall_macaw_swamp_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_SWAMP, Identifier.fromNamespaceAndPath("mcwdoors", "warped_swamp_door"), "tall_macaw_swamp_door");

		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "oak_waffle_door"), "tall_macaw_waffle_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_waffle_door"), "tall_macaw_waffle_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "birch_waffle_door"), "tall_macaw_waffle_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_waffle_door"), "tall_macaw_waffle_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_waffle_door"), "tall_macaw_waffle_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_waffle_door"), "tall_macaw_waffle_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_waffle_door"), "tall_macaw_waffle_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_waffle_door"), "tall_macaw_waffle_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_waffle_door"), "tall_macaw_waffle_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_WAFFLE, Identifier.fromNamespaceAndPath("mcwdoors", "warped_waffle_door"), "tall_macaw_waffle_door");

		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "oak_bamboo_door"), "tall_macaw_bamboo_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_bamboo_door"), "tall_macaw_bamboo_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "birch_bamboo_door"), "tall_macaw_bamboo_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_bamboo_door"), "tall_macaw_bamboo_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_bamboo_door"), "tall_macaw_bamboo_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_bamboo_door"), "tall_macaw_bamboo_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_bamboo_door"), "tall_macaw_bamboo_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_bamboo_door"), "tall_macaw_bamboo_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_bamboo_door"), "tall_macaw_bamboo_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_BAMBOO, Identifier.fromNamespaceAndPath("mcwdoors", "warped_bamboo_door"), "tall_macaw_bamboo_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "oak_nether_door"), "tall_macaw_nether_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_nether_door"), "tall_macaw_nether_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "birch_nether_door"), "tall_macaw_nether_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_nether_door"), "tall_macaw_nether_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_nether_door"), "tall_macaw_nether_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_nether_door"), "tall_macaw_nether_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_nether_door"), "tall_macaw_nether_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_nether_door"), "tall_macaw_nether_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_nether_door"), "tall_macaw_nether_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_WARPED_NETHER, Identifier.fromNamespaceAndPath("mcwdoors", "warped_nether_door"), "tall_macaw_nether_door");
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_OAK_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "oak_mystic_door"), "tall_macaw_mystic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_SPRUCE_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "spruce_mystic_door"), "tall_macaw_mystic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BIRCH_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "birch_mystic_door"), "tall_macaw_mystic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_JUNGLE_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "jungle_mystic_door"), "tall_macaw_mystic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_ACACIA_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "acacia_mystic_door"), "tall_macaw_mystic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_DARK_OAK_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "dark_oak_mystic_door"), "tall_macaw_mystic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_MANGROVE_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "mangrove_mystic_door"), "tall_macaw_mystic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CHERRY_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "cherry_mystic_door"), "tall_macaw_mystic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_BAMBOO_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "bamboo_mystic_door"), "tall_macaw_mystic_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MACAW_CRIMSON_MYSTIC, Identifier.fromNamespaceAndPath("mcwdoors", "crimson_mystic_door"), "tall_macaw_mystic_door");
	}
}

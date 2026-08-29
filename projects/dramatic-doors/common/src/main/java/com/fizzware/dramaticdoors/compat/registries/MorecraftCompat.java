package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.blocks.TallDoorBlock;
import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import oshi.util.tuples.Pair;

public class MorecraftCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MORECRAFT_BONE, DDNames.SHORT_MORECRAFT_BONE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("morecraft", "bone_door")), BlockSetType.STONE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MORECRAFT_GLASS, DDNames.SHORT_MORECRAFT_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("morecraft", "glass_door")), BlockSetType.STONE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MORECRAFT_SOUL_GLASS, DDNames.SHORT_MORECRAFT_SOUL_GLASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("morecraft", "soul_glass_door")), BlockSetType.STONE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MORECRAFT_NETHERWOOD, DDNames.SHORT_MORECRAFT_NETHERWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("morecraft", "netherwood_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MORECRAFT_NETHERBRICK, DDNames.SHORT_MORECRAFT_NETHERBRICK, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("morecraft", "netherbrick_door")), BlockSetType.STONE, true);
		
		Block shortNetheriteDoor = new TallDoorBlock(BlockSetType.IRON, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("morecraft", "netherite_door"), Blocks.IRON_DOOR));
		Block tallNetheriteDoor = new TallDoorBlock(BlockSetType.IRON, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("morecraft", "netherite_door"), Blocks.IRON_DOOR));
		
		DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.SHORT_MORECRAFT_NETHERITE, shortNetheriteDoor));
    	DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.TALL_MORECRAFT_NETHERITE, tallNetheriteDoor));

    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.SHORT_MORECRAFT_NETHERITE, DDRegistry.createDoorItem(DDNames.SHORT_MORECRAFT_NETHERITE, shortNetheriteDoor, true)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.TALL_MORECRAFT_NETHERITE, DDRegistry.createDoorItem(DDNames.TALL_MORECRAFT_NETHERITE, tallNetheriteDoor, true)));
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MORECRAFT_BONE, Identifier.fromNamespaceAndPath("morecraft", "bone_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MORECRAFT_GLASS, Identifier.fromNamespaceAndPath("morecraft", "glass_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MORECRAFT_SOUL_GLASS, Identifier.fromNamespaceAndPath("morecraft", "soul_glass_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MORECRAFT_NETHERWOOD, Identifier.fromNamespaceAndPath("morecraft", "netherwood_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MORECRAFT_NETHERBRICK, Identifier.fromNamespaceAndPath("morecraft", "netherbrick_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MORECRAFT_NETHERITE, Identifier.fromNamespaceAndPath("morecraft", "netherite_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MORECRAFT_BONE, Identifier.fromNamespaceAndPath("morecraft", "bone_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MORECRAFT_GLASS, Identifier.fromNamespaceAndPath("morecraft", "glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MORECRAFT_SOUL_GLASS, Identifier.fromNamespaceAndPath("morecraft", "soul_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MORECRAFT_NETHERWOOD, Identifier.fromNamespaceAndPath("morecraft", "netherwood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MORECRAFT_NETHERBRICK, Identifier.fromNamespaceAndPath("morecraft", "netherbrick_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MORECRAFT_NETHERITE, Identifier.fromNamespaceAndPath("morecraft", "netherite_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MORECRAFT_BONE, Identifier.fromNamespaceAndPath("morecraft", "bone_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MORECRAFT_GLASS, Identifier.fromNamespaceAndPath("morecraft", "glass_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MORECRAFT_SOUL_GLASS, Identifier.fromNamespaceAndPath("morecraft", "soul_glass_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MORECRAFT_NETHERWOOD, Identifier.fromNamespaceAndPath("morecraft", "netherwood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MORECRAFT_NETHERBRICK, Identifier.fromNamespaceAndPath("morecraft", "netherbrick_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MORECRAFT_NETHERITE, Identifier.fromNamespaceAndPath("morecraft", "netherite_door"), false);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MORECRAFT_BONE, Identifier.fromNamespaceAndPath("morecraft", "bone_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MORECRAFT_GLASS, Identifier.fromNamespaceAndPath("morecraft", "glass_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MORECRAFT_SOUL_GLASS, Identifier.fromNamespaceAndPath("morecraft", "soul_glass_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MORECRAFT_NETHERWOOD, Identifier.fromNamespaceAndPath("morecraft", "netherwood_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MORECRAFT_NETHERBRICK, Identifier.fromNamespaceAndPath("morecraft", "netherbrick_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MORECRAFT_NETHERITE, Identifier.fromNamespaceAndPath("morecraft", "netherite_door"), "tall_metal_door");
	}
}

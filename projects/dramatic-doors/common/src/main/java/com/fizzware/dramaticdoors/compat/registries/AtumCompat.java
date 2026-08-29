package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class AtumCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_DEADWOOD, DDNames.SHORT_DEADWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("atum", "deadwood_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_PALM, DDNames.SHORT_PALM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("atum", "palm_door")), BlockSetType.JUNGLE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LIMESTONE, DDNames.SHORT_LIMESTONE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("atum", "limestone_door")), BlockSetType.STONE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LIMESTONE_CRACKED, DDNames.SHORT_LIMESTONE_CRACKED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("atum", "limestone_cracked_door")), BlockSetType.STONE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LIMESTONE_BRICK_SMALL, DDNames.SHORT_LIMESTONE_BRICK_SMALL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("atum", "limestone_brick_small_door")), BlockSetType.STONE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LIMESTONE_BRICK_LARGE, DDNames.SHORT_LIMESTONE_BRICK_LARGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("atum", "limestone_brick_large_door")), BlockSetType.STONE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LIMESTONE_BRICK_CRACKED_BRICK, DDNames.SHORT_LIMESTONE_BRICK_CRACKED_BRICK, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("atum", "limestone_brick_cracked_brick_door")), BlockSetType.STONE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LIMESTONE_BRICK_CHISELED, DDNames.SHORT_LIMESTONE_BRICK_CHISELED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("atum", "limestone_brick_chiseled_door")), BlockSetType.STONE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LIMESTONE_BRICK_CARVED, DDNames.SHORT_LIMESTONE_BRICK_CARVED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("atum", "limestone_brick_carved_door")), BlockSetType.STONE, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_DEADWOOD, Identifier.fromNamespaceAndPath("atum", "deadwood_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_PALM, Identifier.fromNamespaceAndPath("atum", "palm_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LIMESTONE, Identifier.fromNamespaceAndPath("atum", "limestone_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LIMESTONE_CRACKED, Identifier.fromNamespaceAndPath("atum", "limestone_cracked_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LIMESTONE_BRICK_SMALL, Identifier.fromNamespaceAndPath("atum", "limestone_brick_small_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LIMESTONE_BRICK_LARGE, Identifier.fromNamespaceAndPath("atum", "limestone_brick_large_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LIMESTONE_BRICK_CRACKED_BRICK, Identifier.fromNamespaceAndPath("atum", "limestone_brick_cracked_brick_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LIMESTONE_BRICK_CHISELED, Identifier.fromNamespaceAndPath("atum", "limestone_brick_chiseled_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LIMESTONE_BRICK_CARVED, Identifier.fromNamespaceAndPath("atum", "limestone_brick_carved_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_DEADWOOD, Identifier.fromNamespaceAndPath("atum", "deadwood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_PALM, Identifier.fromNamespaceAndPath("atum", "palm_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LIMESTONE, Identifier.fromNamespaceAndPath("atum", "limestone_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LIMESTONE_CRACKED, Identifier.fromNamespaceAndPath("atum", "limestone_cracked_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LIMESTONE_BRICK_SMALL, Identifier.fromNamespaceAndPath("atum", "limestone_brick_small_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LIMESTONE_BRICK_LARGE, Identifier.fromNamespaceAndPath("atum", "limestone_brick_large_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LIMESTONE_BRICK_CRACKED_BRICK, Identifier.fromNamespaceAndPath("atum", "limestone_brick_cracked_brick_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LIMESTONE_BRICK_CHISELED, Identifier.fromNamespaceAndPath("atum", "limestone_brick_chiseled_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LIMESTONE_BRICK_CARVED, Identifier.fromNamespaceAndPath("atum", "limestone_brick_carved_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_DEADWOOD, Identifier.fromNamespaceAndPath("atum", "deadwood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_PALM, Identifier.fromNamespaceAndPath("atum", "palm_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LIMESTONE, Identifier.fromNamespaceAndPath("atum", "limestone_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LIMESTONE_CRACKED, Identifier.fromNamespaceAndPath("atum", "limestone_cracked_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LIMESTONE_BRICK_SMALL, Identifier.fromNamespaceAndPath("atum", "limestone_brick_small_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LIMESTONE_BRICK_LARGE, Identifier.fromNamespaceAndPath("atum", "limestone_brick_large_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LIMESTONE_BRICK_CRACKED_BRICK, Identifier.fromNamespaceAndPath("atum", "limestone_brick_cracked_brick_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LIMESTONE_BRICK_CHISELED, Identifier.fromNamespaceAndPath("atum", "limestone_brick_chiseled_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LIMESTONE_BRICK_CARVED, Identifier.fromNamespaceAndPath("atum", "limestone_brick_carved_door"), false);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_DEADWOOD, Identifier.fromNamespaceAndPath("atum", "deadwood_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_PALM, Identifier.fromNamespaceAndPath("atum", "palm_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LIMESTONE, Identifier.fromNamespaceAndPath("atum", "limestone_door"), "tall_limestone_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LIMESTONE_CRACKED, Identifier.fromNamespaceAndPath("atum", "limestone_cracked_door"), "tall_limestone_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LIMESTONE_BRICK_SMALL, Identifier.fromNamespaceAndPath("atum", "limestone_brick_small_door"), "tall_limestone_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LIMESTONE_BRICK_LARGE, Identifier.fromNamespaceAndPath("atum", "limestone_brick_large_door"), "tall_limestone_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LIMESTONE_BRICK_CRACKED_BRICK, Identifier.fromNamespaceAndPath("atum", "limestone_brick_cracked_brick_door"), "tall_limestone_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LIMESTONE_BRICK_CHISELED, Identifier.fromNamespaceAndPath("atum", "limestone_brick_chiseled_door"), "tall_limestone_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LIMESTONE_BRICK_CARVED, Identifier.fromNamespaceAndPath("atum", "limestone_brick_carved_door"), "tall_limestone_door");
	}
}

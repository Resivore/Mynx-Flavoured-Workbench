package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class TropicraftCompat 
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TC_BAMBOO, DDNames.SHORT_TC_BAMBOO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("tropicraft", "bamboo_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TC_MAHOGANY, DDNames.SHORT_TC_MAHOGANY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("tropicraft", "mahogany_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TC_MANGROVE, DDNames.SHORT_TC_MANGROVE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("tropicraft", "mangrove_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TC_PALM, DDNames.SHORT_TC_PALM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("tropicraft", "palm_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TC_THATCH, DDNames.SHORT_TC_THATCH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("tropicraft", "thatch_door")), BlockSetType.MANGROVE, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TC_BAMBOO, Identifier.fromNamespaceAndPath("tropicraft", "bamboo_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TC_MAHOGANY, Identifier.fromNamespaceAndPath("tropicraft", "mahogany_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TC_MANGROVE, Identifier.fromNamespaceAndPath("tropicraft", "mangrove_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TC_PALM, Identifier.fromNamespaceAndPath("tropicraft", "palm_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TC_THATCH, Identifier.fromNamespaceAndPath("tropicraft", "thatch_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TC_BAMBOO, Identifier.fromNamespaceAndPath("tropicraft", "bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TC_MAHOGANY, Identifier.fromNamespaceAndPath("tropicraft", "mahogany_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TC_MANGROVE, Identifier.fromNamespaceAndPath("tropicraft", "mangrove_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TC_PALM, Identifier.fromNamespaceAndPath("tropicraft", "palm_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TC_THATCH, Identifier.fromNamespaceAndPath("tropicraft", "thatch_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TC_BAMBOO, Identifier.fromNamespaceAndPath("tropicraft", "bamboo_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TC_MAHOGANY, Identifier.fromNamespaceAndPath("tropicraft", "mahogany_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TC_MANGROVE, Identifier.fromNamespaceAndPath("tropicraft", "mangrove_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TC_PALM, Identifier.fromNamespaceAndPath("tropicraft", "palm_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TC_THATCH, Identifier.fromNamespaceAndPath("tropicraft", "thatch_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TC_BAMBOO, Identifier.fromNamespaceAndPath("tropicraft", "bamboo_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TC_MAHOGANY, Identifier.fromNamespaceAndPath("tropicraft", "mahogany_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TC_MANGROVE, Identifier.fromNamespaceAndPath("tropicraft", "mangrove_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TC_PALM, Identifier.fromNamespaceAndPath("tropicraft", "palm_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TC_THATCH, Identifier.fromNamespaceAndPath("tropicraft", "thatch_door"), "tall_wooden_door");
	}
}

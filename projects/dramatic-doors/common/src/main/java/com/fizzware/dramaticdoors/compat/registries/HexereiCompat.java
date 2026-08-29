package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class HexereiCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_HEXEREI_MAHOGANY, DDNames.SHORT_HEXEREI_MAHOGANY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("hexerei", "mahogany_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_HEXEREI_WILLOW, DDNames.SHORT_HEXEREI_WILLOW, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("hexerei", "willow_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_HEXEREI_WITCH_HAZEL, DDNames.SHORT_HEXEREI_WITCH_HAZEL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("hexerei", "witch_hazel_door")), BlockSetType.DARK_OAK, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_HEXEREI_MAHOGANY, Identifier.fromNamespaceAndPath("hexerei", "mahogany_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_HEXEREI_WILLOW, Identifier.fromNamespaceAndPath("hexerei", "willow_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_HEXEREI_WITCH_HAZEL, Identifier.fromNamespaceAndPath("hexerei", "witch_hazel_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_HEXEREI_MAHOGANY, Identifier.fromNamespaceAndPath("hexerei", "mahogany_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_HEXEREI_WILLOW, Identifier.fromNamespaceAndPath("hexerei", "willow_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_HEXEREI_WITCH_HAZEL, Identifier.fromNamespaceAndPath("hexerei", "witch_hazel_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_HEXEREI_MAHOGANY, Identifier.fromNamespaceAndPath("hexerei", "mahogany_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_HEXEREI_WILLOW, Identifier.fromNamespaceAndPath("hexerei", "willow_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_HEXEREI_WITCH_HAZEL, Identifier.fromNamespaceAndPath("hexerei", "witch_hazel_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_HEXEREI_MAHOGANY, Identifier.fromNamespaceAndPath("hexerei", "mahogany_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_HEXEREI_WILLOW, Identifier.fromNamespaceAndPath("hexerei", "willow_door"), "tall_wooden_door");	
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_HEXEREI_WITCH_HAZEL, Identifier.fromNamespaceAndPath("hexerei", "witch_hazel_door"), "tall_wooden_door");	
	}
}

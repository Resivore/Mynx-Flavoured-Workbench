package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class CinderscapesCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SCORCHED, DDNames.SHORT_SCORCHED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("cinderscapes", "scorched_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_UMBRAL, DDNames.SHORT_UMBRAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("cinderscapes", "umbral_door")), BlockSetType.WARPED, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SCORCHED, Identifier.fromNamespaceAndPath("cinderscapes", "scorched_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_UMBRAL, Identifier.fromNamespaceAndPath("cinderscapes", "umbral_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_SCORCHED, Identifier.fromNamespaceAndPath("cinderscapes", "scorched_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_UMBRAL, Identifier.fromNamespaceAndPath("cinderscapes", "umbral_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SCORCHED, Identifier.fromNamespaceAndPath("cinderscapes", "scorched_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_UMBRAL, Identifier.fromNamespaceAndPath("cinderscapes", "umbral_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_SCORCHED, Identifier.fromNamespaceAndPath("cinderscapes", "scorched_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_UMBRAL, Identifier.fromNamespaceAndPath("cinderscapes", "umbral_door"), "tall_wooden_door");
	}
}

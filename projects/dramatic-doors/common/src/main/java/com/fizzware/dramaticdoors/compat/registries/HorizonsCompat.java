package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class HorizonsCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CYPRESS, DDNames.SHORT_CYPRESS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("horizons", "cypress_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_JACARANDA, DDNames.SHORT_JACARANDA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("horizons", "jacaranda_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_REDBUD, DDNames.SHORT_REDBUD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("horizons", "redbud_door")), BlockSetType.BIRCH, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_JACARANDA, Identifier.fromNamespaceAndPath("horizons", "jacaranda_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_REDBUD, Identifier.fromNamespaceAndPath("horizons", "redbud_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CYPRESS, Identifier.fromNamespaceAndPath("horizons", "cypress_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_JACARANDA, Identifier.fromNamespaceAndPath("horizons", "jacaranda_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_REDBUD, Identifier.fromNamespaceAndPath("horizons", "redbud_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CYPRESS, Identifier.fromNamespaceAndPath("horizons", "cypress_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CYPRESS, Identifier.fromNamespaceAndPath("horizons", "cypress_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_JACARANDA, Identifier.fromNamespaceAndPath("horizons", "jacaranda_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_REDBUD, Identifier.fromNamespaceAndPath("horizons", "redbud_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CYPRESS, Identifier.fromNamespaceAndPath("horizons", "cypress_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_JACARANDA, Identifier.fromNamespaceAndPath("horizons", "jacaranda_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_REDBUD, Identifier.fromNamespaceAndPath("horizons", "redbud_door"), "tall_wooden_door");
	}
}

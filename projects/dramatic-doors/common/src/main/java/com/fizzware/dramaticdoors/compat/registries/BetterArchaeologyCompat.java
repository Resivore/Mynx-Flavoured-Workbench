package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class BetterArchaeologyCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ROTTEN, DDNames.SHORT_ROTTEN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betterarcheology", "rotten_door")), BlockSetType.SPRUCE, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ROTTEN, Identifier.fromNamespaceAndPath("betterarcheology", "rotten_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_ROTTEN, Identifier.fromNamespaceAndPath("betterarcheology", "rotten_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ROTTEN, Identifier.fromNamespaceAndPath("betterarcheology", "rotten_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_ROTTEN, Identifier.fromNamespaceAndPath("betterarcheology", "rotten_door"), "tall_wooden_door");
	}
}

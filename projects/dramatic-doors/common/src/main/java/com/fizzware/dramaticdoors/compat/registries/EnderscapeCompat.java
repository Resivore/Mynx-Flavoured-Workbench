package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class EnderscapeCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CELESTIAL, DDNames.SHORT_CELESTIAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("enderscape", "celestial_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MURUSHROOM, DDNames.SHORT_MURUSHROOM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("enderscape", "murushroom_door")), BlockSetType.WARPED, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CELESTIAL, Identifier.fromNamespaceAndPath("enderscape", "celestial_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MURUSHROOM, Identifier.fromNamespaceAndPath("enderscape", "murushroom_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CELESTIAL, Identifier.fromNamespaceAndPath("enderscape", "celestial_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MURUSHROOM, Identifier.fromNamespaceAndPath("enderscape", "murushroom_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CELESTIAL, Identifier.fromNamespaceAndPath("enderscape", "celestial_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MURUSHROOM, Identifier.fromNamespaceAndPath("enderscape", "murushroom_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CELESTIAL, Identifier.fromNamespaceAndPath("enderscape", "celestial_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MURUSHROOM, Identifier.fromNamespaceAndPath("enderscape", "murushroom_door"), "tall_wooden_door");
	}
}

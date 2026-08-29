package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class DesolationCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CHARRED, DDNames.SHORT_CHARRED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("desolation", "charred_door")), BlockSetType.OAK, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CHARRED, Identifier.fromNamespaceAndPath("desolation", "charred_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CHARRED, Identifier.fromNamespaceAndPath("desolation", "charred_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CHARRED, Identifier.fromNamespaceAndPath("desolation", "charred_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CHARRED, Identifier.fromNamespaceAndPath("desolation", "charred_door"), "tall_wooden_door");
	}
}

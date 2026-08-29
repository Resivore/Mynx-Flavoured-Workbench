package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class EndlessBiomesCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_EB_PENUMBRA, DDNames.SHORT_EB_PENUMBRA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("endlessbiomes", "penumbra_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_EB_TWISTED, DDNames.SHORT_EB_TWISTED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("endlessbiomes", "twisted_door")), BlockSetType.WARPED, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CHESTNUT, Identifier.fromNamespaceAndPath("endlessbiomes", "penumbra_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_HOLLY, Identifier.fromNamespaceAndPath("endlessbiomes", "twisted_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CHESTNUT, Identifier.fromNamespaceAndPath("endlessbiomes", "penumbra_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_HOLLY, Identifier.fromNamespaceAndPath("endlessbiomes", "twisted_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CHESTNUT, Identifier.fromNamespaceAndPath("windswept", "penumbra_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_HOLLY, Identifier.fromNamespaceAndPath("endlessbiomes", "twisted_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CHESTNUT, Identifier.fromNamespaceAndPath("endlessbiomes", "penumbra_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_HOLLY, Identifier.fromNamespaceAndPath("endlessbiomes", "twisted_door"), "tall_wooden_door");
	}
}

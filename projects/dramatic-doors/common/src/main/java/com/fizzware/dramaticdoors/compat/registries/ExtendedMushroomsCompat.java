package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class ExtendedMushroomsCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MUSHROOM, DDNames.SHORT_MUSHROOM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("extendedmushrooms", "mushroom_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POISONOUS_MUSHROOM, DDNames.SHORT_POISONOUS_MUSHROOM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("extendedmushrooms", "poisonous_mushroom_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_GLOWSHROOM, DDNames.SHORT_GLOWSHROOM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("extendedmushrooms", "glowshroom_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_HONEY_FUNGUS, DDNames.SHORT_HONEY_FUNGUS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("extendedmushrooms", "honey_fungus_door")), BlockSetType.OAK, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MUSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "mushroom_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POISONOUS_MUSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "poisonous_mushroom_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_GLOWSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "glowshroom_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_HONEY_FUNGUS, Identifier.fromNamespaceAndPath("extendedmushrooms", "honey_fungus_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MUSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "mushroom_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POISONOUS_MUSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "poisonous_mushroom_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_GLOWSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "glowshroom_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_HONEY_FUNGUS, Identifier.fromNamespaceAndPath("extendedmushrooms", "honey_fungus_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MUSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "mushroom_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POISONOUS_MUSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "poisonous_mushroom_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_GLOWSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "glowshroom_mushroom_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_HONEY_FUNGUS, Identifier.fromNamespaceAndPath("extendedmushrooms", "honey_fungus_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MUSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "mushroom_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POISONOUS_MUSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "poisonous_mushroom_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_GLOWSHROOM, Identifier.fromNamespaceAndPath("extendedmushrooms", "glowshroom_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_HONEY_FUNGUS, Identifier.fromNamespaceAndPath("extendedmushrooms", "honey_fungus_door"), "tall_wooden_door");
	}
}

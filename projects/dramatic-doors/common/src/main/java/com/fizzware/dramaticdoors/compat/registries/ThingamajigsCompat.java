package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

// Partially implemented
public class ThingamajigsCompat 
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ALARMED, DDNames.SHORT_ALARMED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "alarmed_door")), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BUBBLE, DDNames.SHORT_BUBBLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "bubble_door")), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_FESTIVE, DDNames.SHORT_FESTIVE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "festive_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LOCKABLE, DDNames.SHORT_LOCKABLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "lockable_door")), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_METALLIC, DDNames.SHORT_METALLIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "metallic_door")), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SCREEN, DDNames.SHORT_SCREEN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "screen_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SNOWMAN, DDNames.SHORT_SNOWMAN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "snowman_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_STONE, DDNames.SHORT_STONE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "stone_door")), BlockSetType.STONE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUBBER_WOOD, DDNames.SHORT_RUBBER_WOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "rubber_wood_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_WHITE_WOOD, DDNames.SHORT_WHITE_WOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thingamajigs", "white_wood_door")), BlockSetType.OAK, false);
	}
	
	private static void registerRecipes() {
		/*DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ALARMED, Identifier.fromNamespaceAndPath("thingamajigs", "alarmed_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BUBBLE, Identifier.fromNamespaceAndPath("thingamajigs", "bubble_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_FESTIVE, Identifier.fromNamespaceAndPath("thingamajigs", "festive_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LOCKABLE, Identifier.fromNamespaceAndPath("thingamajigs", "lockable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_METALLIC, Identifier.fromNamespaceAndPath("thingamajigs", "metallic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SCREEN, Identifier.fromNamespaceAndPath("thingamajigs", "screen_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SNOWMAN, Identifier.fromNamespaceAndPath("thingamajigs", "snowman_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_STONE, Identifier.fromNamespaceAndPath("thingamajigs", "stone_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUBBER_WOOD, Identifier.fromNamespaceAndPath("thingamajigs", "rubber_wood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_WHITE_WOOD, Identifier.fromNamespaceAndPath("thingamajigs", "white_wood_door"));*/
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_ALARMED, Identifier.fromNamespaceAndPath("thingamajigs", "alarmed_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BUBBLE, Identifier.fromNamespaceAndPath("thingamajigs", "bubble_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_FESTIVE, Identifier.fromNamespaceAndPath("thingamajigs", "festive_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LOCKABLE, Identifier.fromNamespaceAndPath("thingamajigs", "lockable_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_METALLIC, Identifier.fromNamespaceAndPath("thingamajigs", "metallic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_SCREEN, Identifier.fromNamespaceAndPath("thingamajigs", "screen_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_SNOWMAN, Identifier.fromNamespaceAndPath("thingamajigs", "snowman_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_STONE, Identifier.fromNamespaceAndPath("thingamajigs", "stone_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUBBER_WOOD, Identifier.fromNamespaceAndPath("thingamajigs", "rubber_wood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_WHITE_WOOD, Identifier.fromNamespaceAndPath("thingamajigs", "white_wood_door"));
	
		/*DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ALARMED, Identifier.fromNamespaceAndPath("thingamajigs", "alarmed_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BUBBLE, Identifier.fromNamespaceAndPath("thingamajigs", "bubble_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_FESTIVE, Identifier.fromNamespaceAndPath("thingamajigs", "festive_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LOCKABLE, Identifier.fromNamespaceAndPath("thingamajigs", "lockable_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_METALLIC, Identifier.fromNamespaceAndPath("thingamajigs", "metallic_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SCREEN, Identifier.fromNamespaceAndPath("thingamajigs", "screen_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SNOWMAN, Identifier.fromNamespaceAndPath("thingamajigs", "snowman_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_STONE, Identifier.fromNamespaceAndPath("thingamajigs", "stone_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUBBER_WOOD, Identifier.fromNamespaceAndPath("thingamajigs", "rubber_wood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_WHITE_WOOD, Identifier.fromNamespaceAndPath("thingamajigs", "white_wood_door"), true);*/
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_ALARMED, Identifier.fromNamespaceAndPath("thingamajigs", "alarmed_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BUBBLE, Identifier.fromNamespaceAndPath("thingamajigs", "bubble_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_FESTIVE, Identifier.fromNamespaceAndPath("thingamajigs", "festive_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LOCKABLE, Identifier.fromNamespaceAndPath("thingamajigs", "lockable_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_METALLIC, Identifier.fromNamespaceAndPath("thingamajigs", "metallic_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_SCREEN, Identifier.fromNamespaceAndPath("thingamajigs", "screen_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_SNOWMAN, Identifier.fromNamespaceAndPath("thingamajigs", "snowman_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_STONE, Identifier.fromNamespaceAndPath("thingamajigs", "stone_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUBBER_WOOD, Identifier.fromNamespaceAndPath("thingamajigs", "rubber_wood_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_WHITE_WOOD, Identifier.fromNamespaceAndPath("thingamajigs", "white_wood_door"), "tall_wooden_door");
	}
}

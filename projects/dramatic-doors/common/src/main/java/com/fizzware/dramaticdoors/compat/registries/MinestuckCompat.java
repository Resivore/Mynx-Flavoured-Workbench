package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

// Partially Implemented
public class MinestuckCompat 
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CARVED, DDNames.SHORT_CARVED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "carved_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CINDERED, DDNames.SHORT_CINDERED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "cindered_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CRUXITE, DDNames.SHORT_CRUXITE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "cruxite_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_DEAD, DDNames.SHORT_DEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "dead_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_END, DDNames.SHORT_END, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "end_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_FROST, DDNames.SHORT_FROST, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "frost_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_GLOWING, DDNames.SHORT_GLOWING, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "glowing_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LACQUERED, DDNames.SHORT_LACQUERED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "lacquered_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_PERFECTLY_GENERIC, DDNames.SHORT_PERFECTLY_GENERIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "perfectly_generic_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RAINBOW, DDNames.SHORT_RAINBOW, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "rainbow_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SHADEWOOD, DDNames.SHORT_SHADEWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "shadewood_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TREATED, DDNames.SHORT_TREATED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "treated_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BLOOD_ASPECT, DDNames.SHORT_BLOOD_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "blood_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BREATH_ASPECT, DDNames.SHORT_BREATH_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "breath_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_DOOM_ASPECT, DDNames.SHORT_DOOM_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "doom_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_HEART_ASPECT, DDNames.SHORT_HEART_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "heart_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_HOPE_ASPECT, DDNames.SHORT_HOPE_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "hope_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LIFE_ASPECT, DDNames.SHORT_LIFE_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "life_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LIGHT_ASPECT, DDNames.SHORT_LIGHT_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "light_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MIND_ASPECT, DDNames.SHORT_MIND_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "mind_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RAGE_ASPECT, DDNames.SHORT_RAGE_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "rage_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SPACE_ASPECT, DDNames.SHORT_SPACE_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "space_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TIME_ASPECT, DDNames.SHORT_TIME_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "time_aspect_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_VOID_ASPECT, DDNames.SHORT_VOID_ASPECT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("minestuck", "void_aspect_door")), BlockSetType.OAK, false);
	}
	
	private static void registerRecipes() {
		/*DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CARVED, Identifier.fromNamespaceAndPath("minestuck", "carved_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CINDERED, Identifier.fromNamespaceAndPath("minestuck", "cindered_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CRUXITE, Identifier.fromNamespaceAndPath("minestuck", "cruxite_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_DEAD, Identifier.fromNamespaceAndPath("minestuck", "dead_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_END, Identifier.fromNamespaceAndPath("minestuck", "end_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_FROST, Identifier.fromNamespaceAndPath("minestuck", "frost_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_GLOWING, Identifier.fromNamespaceAndPath("minestuck", "glowing_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LACQUERED, Identifier.fromNamespaceAndPath("minestuck", "lacquered_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_PERFECTLY_GENERIC, Identifier.fromNamespaceAndPath("minestuck", "perfectly_generic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RAINBOW, Identifier.fromNamespaceAndPath("minestuck", "rainbow_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SHADEWOOD, Identifier.fromNamespaceAndPath("minestuck", "shadewood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TREATED, Identifier.fromNamespaceAndPath("minestuck", "treated_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BLOOD_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "blood_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BREATH_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "breath_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_DOOM_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "doom_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_HEART_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "heart_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_HOPE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "hope_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LIFE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "life_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LIGHT_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "light_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MIND_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "mind_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RAGE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "rage_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SPACE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "space_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TIME_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "time_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_VOID_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "void_aspect_door"));*/
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CARVED, Identifier.fromNamespaceAndPath("minestuck", "carved_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CINDERED, Identifier.fromNamespaceAndPath("minestuck", "cindered_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CRUXITE, Identifier.fromNamespaceAndPath("minestuck", "cruxite_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_DEAD, Identifier.fromNamespaceAndPath("minestuck", "dead_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_END, Identifier.fromNamespaceAndPath("minestuck", "end_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_FROST, Identifier.fromNamespaceAndPath("minestuck", "frost_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_GLOWING, Identifier.fromNamespaceAndPath("minestuck", "glowing_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LACQUERED, Identifier.fromNamespaceAndPath("minestuck", "lacquered_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_PERFECTLY_GENERIC, Identifier.fromNamespaceAndPath("minestuck", "perfectly_generic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RAINBOW, Identifier.fromNamespaceAndPath("minestuck", "rainbow_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_SHADEWOOD, Identifier.fromNamespaceAndPath("minestuck", "shadewood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TREATED, Identifier.fromNamespaceAndPath("minestuck", "treated_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BLOOD_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "blood_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BREATH_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "breath_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_DOOM_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "doom_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_HEART_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "heart_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_HOPE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "hope_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LIFE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "life_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LIGHT_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "light_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MIND_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "mind_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RAGE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "rage_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_SPACE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "space_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TIME_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "time_aspect_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_VOID_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "void_aspect_door"));
		
		/*DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CARVED, Identifier.fromNamespaceAndPath("minestuck", "carved_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CINDERED, Identifier.fromNamespaceAndPath("minestuck", "cindered_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CRUXITE, Identifier.fromNamespaceAndPath("minestuck", "cruxite_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_DEAD, Identifier.fromNamespaceAndPath("minestuck", "dead_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_END, Identifier.fromNamespaceAndPath("minestuck", "end_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_FROST, Identifier.fromNamespaceAndPath("minestuck", "frost_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_GLOWING, Identifier.fromNamespaceAndPath("minestuck", "glowing_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LACQUERED, Identifier.fromNamespaceAndPath("minestuck", "lacquered_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_PERFECTLY_GENERIC, Identifier.fromNamespaceAndPath("minestuck", "perfectly_generic_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RAINBOW, Identifier.fromNamespaceAndPath("minestuck", "rainbow_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SHADEWOOD, Identifier.fromNamespaceAndPath("minestuck", "shadewood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TREATED, Identifier.fromNamespaceAndPath("minestuck", "treated_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BLOOD_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "blood_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BREATH_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "breath_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_DOOM_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "doom_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_HEART_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "heart_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_HOPE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "hope_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LIFE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "life_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LIGHT_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "light_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MIND_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "mind_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RAGE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "rage_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SPACE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "space_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TIME_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "time_aspect_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_VOID_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "void_aspect_door"), true);*/
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CARVED, Identifier.fromNamespaceAndPath("minestuck", "carved_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CINDERED, Identifier.fromNamespaceAndPath("minestuck", "cindered_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CRUXITE, Identifier.fromNamespaceAndPath("minestuck", "cruxite_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_DEAD, Identifier.fromNamespaceAndPath("minestuck", "dead_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_END, Identifier.fromNamespaceAndPath("minestuck", "end_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_FROST, Identifier.fromNamespaceAndPath("minestuck", "frost_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_GLOWING, Identifier.fromNamespaceAndPath("minestuck", "glowing_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LACQUERED, Identifier.fromNamespaceAndPath("minestuck", "lacquered_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_PERFECTLY_GENERIC, Identifier.fromNamespaceAndPath("minestuck", "perfectly_generic_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RAINBOW, Identifier.fromNamespaceAndPath("minestuck", "rainbow_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_SHADEWOOD, Identifier.fromNamespaceAndPath("minestuck", "shadewood_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TREATED, Identifier.fromNamespaceAndPath("minestuck", "treated_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BLOOD_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "blood_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BREATH_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "breath_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_DOOM_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "doom_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_HEART_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "heart_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_HOPE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "hope_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LIFE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "life_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LIGHT_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "light_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MIND_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "mind_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RAGE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "rage_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_SPACE_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "space_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TIME_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "time_aspect_door"), "tall_minestuck_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_VOID_ASPECT, Identifier.fromNamespaceAndPath("minestuck", "void_aspect_door"), "tall_minestuck_door");
	}
}

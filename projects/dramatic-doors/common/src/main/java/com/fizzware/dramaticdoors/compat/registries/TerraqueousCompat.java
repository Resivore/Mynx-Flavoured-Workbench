package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class TerraqueousCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_LIGHT_CLOUD, DDNames.SHORT_TQ_LIGHT_CLOUD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "light_cloud_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_DENSE_CLOUD, DDNames.SHORT_TQ_DENSE_CLOUD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "dense_cloud_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_STORM_CLOUD, DDNames.SHORT_TQ_STORM_CLOUD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "storm_cloud_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_APPLE, DDNames.SHORT_TQ_APPLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "apple_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_BANANA, DDNames.SHORT_TQ_BANANA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "banana_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_CHERRY, DDNames.SHORT_TQ_CHERRY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "cherry_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_COCONUT, DDNames.SHORT_TQ_COCONUT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "coconut_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_LEMON, DDNames.SHORT_TQ_LEMON, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "lemon_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_MANGO, DDNames.SHORT_TQ_MANGO, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "mango_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_MULBERRY, DDNames.SHORT_TQ_MULBERRY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "mulberry_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_ORANGE, DDNames.SHORT_TQ_ORANGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "orange_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_PEACH, DDNames.SHORT_TQ_PEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "peach_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_PEAR, DDNames.SHORT_TQ_PEAR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "pear_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TQ_PLUM, DDNames.SHORT_TQ_PLUM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terraqueous", "plum_door")), BlockSetType.OAK, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_LIGHT_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "light_cloud_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_DENSE_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "dense_cloud_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_STORM_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "storm_cloud_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_APPLE, Identifier.fromNamespaceAndPath("terraqueous", "apple_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_BANANA, Identifier.fromNamespaceAndPath("terraqueous", "banana_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_CHERRY, Identifier.fromNamespaceAndPath("terraqueous", "cherry_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_COCONUT, Identifier.fromNamespaceAndPath("terraqueous", "coconut_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_LEMON, Identifier.fromNamespaceAndPath("terraqueous", "lemon_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_MANGO, Identifier.fromNamespaceAndPath("terraqueous", "mango_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_MULBERRY, Identifier.fromNamespaceAndPath("terraqueous", "mulberry_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_ORANGE, Identifier.fromNamespaceAndPath("terraqueous", "orange_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_PEACH, Identifier.fromNamespaceAndPath("terraqueous", "peach_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_PEAR, Identifier.fromNamespaceAndPath("terraqueous", "pear_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TQ_PLUM, Identifier.fromNamespaceAndPath("terraqueous", "plum_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_LIGHT_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "light_cloud_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_DENSE_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "dense_cloud_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_STORM_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "storm_cloud_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_APPLE, Identifier.fromNamespaceAndPath("terraqueous", "apple_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_BANANA, Identifier.fromNamespaceAndPath("terraqueous", "banana_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_CHERRY, Identifier.fromNamespaceAndPath("terraqueous", "cherry_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_COCONUT, Identifier.fromNamespaceAndPath("terraqueous", "coconut_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_LEMON, Identifier.fromNamespaceAndPath("terraqueous", "lemon_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_MANGO, Identifier.fromNamespaceAndPath("terraqueous", "mango_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_MULBERRY, Identifier.fromNamespaceAndPath("terraqueous", "mulberry_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_ORANGE, Identifier.fromNamespaceAndPath("terraqueous", "orange_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_PEACH, Identifier.fromNamespaceAndPath("terraqueous", "peach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_PEAR, Identifier.fromNamespaceAndPath("terraqueous", "pear_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TQ_PLUM, Identifier.fromNamespaceAndPath("terraqueous", "plum_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_LIGHT_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "azalea_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_DENSE_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "ebony_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_STORM_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "ebony_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_APPLE, Identifier.fromNamespaceAndPath("terraqueous", "apple_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_BANANA, Identifier.fromNamespaceAndPath("terraqueous", "banana_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_CHERRY, Identifier.fromNamespaceAndPath("terraqueous", "cherry_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_COCONUT, Identifier.fromNamespaceAndPath("terraqueous", "coconut_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_LEMON, Identifier.fromNamespaceAndPath("terraqueous", "lemon_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_MANGO, Identifier.fromNamespaceAndPath("terraqueous", "mango_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_MULBERRY, Identifier.fromNamespaceAndPath("terraqueous", "mulberry_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_ORANGE, Identifier.fromNamespaceAndPath("terraqueous", "orange_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_PEACH, Identifier.fromNamespaceAndPath("terraqueous", "peach_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_PEAR, Identifier.fromNamespaceAndPath("terraqueous", "pear_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TQ_PLUM, Identifier.fromNamespaceAndPath("terraqueous", "plum_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_LIGHT_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "azalea_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_DENSE_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "ebony_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_STORM_CLOUD, Identifier.fromNamespaceAndPath("terraqueous", "ebony_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_APPLE, Identifier.fromNamespaceAndPath("terraqueous", "apple_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_BANANA, Identifier.fromNamespaceAndPath("terraqueous", "banana_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_CHERRY, Identifier.fromNamespaceAndPath("terraqueous", "cherry_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_COCONUT, Identifier.fromNamespaceAndPath("terraqueous", "coconut_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_LEMON, Identifier.fromNamespaceAndPath("terraqueous", "lemon_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_MANGO, Identifier.fromNamespaceAndPath("terraqueous", "mango_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_MULBERRY, Identifier.fromNamespaceAndPath("terraqueous", "mulberry_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_ORANGE, Identifier.fromNamespaceAndPath("terraqueous", "orange_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_PEACH, Identifier.fromNamespaceAndPath("terraqueous", "peach_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_PEAR, Identifier.fromNamespaceAndPath("terraqueous", "pear_door"), "tall_tq_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TQ_PLUM, Identifier.fromNamespaceAndPath("terraqueous", "plum_door"), "tall_tq_wooden_door");

	}
}

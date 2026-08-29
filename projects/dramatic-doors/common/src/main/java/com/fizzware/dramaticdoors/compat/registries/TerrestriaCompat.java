package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class TerrestriaCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TERRESTRIA_CYPRESS, DDNames.SHORT_TERRESTRIA_CYPRESS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terrestria", "cypress_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TERRESTRIA_HEMLOCK, DDNames.SHORT_TERRESTRIA_HEMLOCK, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terrestria", "hemlock_door")), BlockSetType.JUNGLE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TERRESTRIA_JAPANESE_MAPLE, DDNames.SHORT_TERRESTRIA_JAPANESE_MAPLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terrestria", "japanese_maple_door")), BlockSetType.CHERRY, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TERRESTRIA_RAINBOW_EUCALYPTUS, DDNames.SHORT_TERRESTRIA_RAINBOW_EUCALYPTUS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terrestria", "rainbow_eucalyptus_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TERRESTRIA_REDWOOD, DDNames.SHORT_TERRESTRIA_REDWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terrestria", "redwood_door")), BlockSetType.SPRUCE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TERRESTRIA_RUBBER, DDNames.SHORT_TERRESTRIA_RUBBER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terrestria", "rubber_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TERRESTRIA_SAKURA, DDNames.SHORT_TERRESTRIA_SAKURA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terrestria", "sakura_door")), BlockSetType.CHERRY, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TERRESTRIA_WILLOW, DDNames.SHORT_TERRESTRIA_WILLOW, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terrestria", "willow_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_TERRESTRIA_YUCCA_PALM, DDNames.SHORT_TERRESTRIA_YUCCA_PALM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("terrestria", "yucca_palm_door")), BlockSetType.JUNGLE, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TERRESTRIA_CYPRESS, Identifier.fromNamespaceAndPath("terrestria", "cypress_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TERRESTRIA_HEMLOCK, Identifier.fromNamespaceAndPath("terrestria", "hemlock_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TERRESTRIA_JAPANESE_MAPLE, Identifier.fromNamespaceAndPath("terrestria", "japanese_maple_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TERRESTRIA_RAINBOW_EUCALYPTUS, Identifier.fromNamespaceAndPath("terrestria", "rainbow_eucalyptus_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TERRESTRIA_REDWOOD, Identifier.fromNamespaceAndPath("terrestria", "redwood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TERRESTRIA_RUBBER, Identifier.fromNamespaceAndPath("terrestria", "rubber_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TERRESTRIA_SAKURA, Identifier.fromNamespaceAndPath("terrestria", "sakura_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TERRESTRIA_WILLOW, Identifier.fromNamespaceAndPath("terrestria", "willow_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_TERRESTRIA_YUCCA_PALM, Identifier.fromNamespaceAndPath("terrestria", "yucca_palm_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TERRESTRIA_CYPRESS, Identifier.fromNamespaceAndPath("terrestria", "cypress_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TERRESTRIA_HEMLOCK, Identifier.fromNamespaceAndPath("terrestria", "hemlock_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TERRESTRIA_JAPANESE_MAPLE, Identifier.fromNamespaceAndPath("terrestria", "japanese_maple_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TERRESTRIA_RAINBOW_EUCALYPTUS, Identifier.fromNamespaceAndPath("terrestria", "rainbow_eucalyptus_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TERRESTRIA_REDWOOD, Identifier.fromNamespaceAndPath("terrestria", "redwood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TERRESTRIA_RUBBER, Identifier.fromNamespaceAndPath("terrestria", "rubber_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TERRESTRIA_SAKURA, Identifier.fromNamespaceAndPath("terrestria", "sakura_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TERRESTRIA_WILLOW, Identifier.fromNamespaceAndPath("terrestria", "willow_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_TERRESTRIA_YUCCA_PALM, Identifier.fromNamespaceAndPath("terrestria", "yucca_palm_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TERRESTRIA_CYPRESS, Identifier.fromNamespaceAndPath("terrestria", "cypress_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TERRESTRIA_HEMLOCK, Identifier.fromNamespaceAndPath("terrestria", "hemlock_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TERRESTRIA_JAPANESE_MAPLE, Identifier.fromNamespaceAndPath("terrestria", "japanese_maple_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TERRESTRIA_RAINBOW_EUCALYPTUS, Identifier.fromNamespaceAndPath("terrestria", "rainbow_eucalyptus_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TERRESTRIA_REDWOOD, Identifier.fromNamespaceAndPath("terrestria", "redwood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TERRESTRIA_RUBBER, Identifier.fromNamespaceAndPath("terrestria", "rubber_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TERRESTRIA_SAKURA, Identifier.fromNamespaceAndPath("terrestria", "sakura_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TERRESTRIA_WILLOW, Identifier.fromNamespaceAndPath("terrestria", "willow_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_TERRESTRIA_YUCCA_PALM, Identifier.fromNamespaceAndPath("terrestria", "yucca_palm_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TERRESTRIA_CYPRESS, Identifier.fromNamespaceAndPath("terrestria", "cypress_door"), "tall_terrestria_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TERRESTRIA_HEMLOCK, Identifier.fromNamespaceAndPath("terrestria", "hemlock_door"), "tall_terrestria_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TERRESTRIA_JAPANESE_MAPLE, Identifier.fromNamespaceAndPath("terrestria", "japanese_maple_door"), "tall_terrestria_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TERRESTRIA_RAINBOW_EUCALYPTUS, Identifier.fromNamespaceAndPath("terrestria", "rainbow_eucalyptus_door"), "tall_terrestria_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TERRESTRIA_REDWOOD, Identifier.fromNamespaceAndPath("terrestria", "redwood_door"), "tall_terrestria_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TERRESTRIA_RUBBER, Identifier.fromNamespaceAndPath("terrestria", "rubber_door"), "tall_terrestria_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TERRESTRIA_SAKURA, Identifier.fromNamespaceAndPath("terrestria", "sakura_door"), "tall_terrestria_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TERRESTRIA_WILLOW, Identifier.fromNamespaceAndPath("terrestria", "willow_door"), "tall_terrestria_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_TERRESTRIA_YUCCA_PALM, Identifier.fromNamespaceAndPath("terrestria", "yucca_palm_door"), "tall_terrestria_wooden_door");
	}
}

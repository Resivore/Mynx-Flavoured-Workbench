package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class AurorasDecorationsCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}

	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_AD_AZALEA, DDNames.SHORT_AD_AZALEA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("aurorasdeco", "azalea_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_AD_JACARANDA, DDNames.SHORT_AD_JACARANDA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("aurorasdeco", "jacaranda_door")), BlockSetType.OAK, true);
	}

	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_AD_AZALEA, Identifier.fromNamespaceAndPath("aurorasdeco", "azalea_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_AD_JACARANDA, Identifier.fromNamespaceAndPath("aurorasdeco", "jacaranda_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_AD_AZALEA, Identifier.fromNamespaceAndPath("aurorasdeco", "azalea_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_AD_JACARANDA, Identifier.fromNamespaceAndPath("aurorasdeco", "jacaranda_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_AD_AZALEA, Identifier.fromNamespaceAndPath("aurorasdeco", "azalea_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_AD_JACARANDA, Identifier.fromNamespaceAndPath("aurorasdeco", "jacaranda_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_AD_AZALEA, Identifier.fromNamespaceAndPath("aurorasdeco", "azalea_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_AD_JACARANDA, Identifier.fromNamespaceAndPath("aurorasdeco", "jacaranda_door"), "tall_wooden_door");
		
		// Add recipes for processing vanilla doors into short doors.
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_OAK + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "oak_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SPRUCE + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "spruce_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BIRCH + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "birch_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_JUNGLE + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "jungle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ACACIA + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "acacia_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_DARK_OAK + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "dark_oak_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MANGROVE + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "mangrove_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CHERRY + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "cherry_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BAMBOO + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "bamboo_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CRIMSON + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "crimson_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_WARPED + "_woodcutting", Identifier.fromNamespaceAndPath("minecraft", "warped_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_OAK, Identifier.fromNamespaceAndPath("minecraft", "oak_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SPRUCE, Identifier.fromNamespaceAndPath("minecraft", "spruce_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BIRCH, Identifier.fromNamespaceAndPath("minecraft", "birch_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_JUNGLE, Identifier.fromNamespaceAndPath("minecraft", "jungle_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ACACIA, Identifier.fromNamespaceAndPath("minecraft", "acacia_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_DARK_OAK, Identifier.fromNamespaceAndPath("minecraft", "dark_oak_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MANGROVE, Identifier.fromNamespaceAndPath("minecraft", "mangrove_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CHERRY, Identifier.fromNamespaceAndPath("minecraft", "cherry_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BAMBOO, Identifier.fromNamespaceAndPath("minecraft", "bamboo_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CRIMSON, Identifier.fromNamespaceAndPath("minecraft", "crimson_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_WARPED, Identifier.fromNamespaceAndPath("minecraft", "warped_door"), true);
	}
}

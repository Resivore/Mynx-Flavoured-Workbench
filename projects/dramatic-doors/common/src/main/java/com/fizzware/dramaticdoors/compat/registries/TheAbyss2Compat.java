package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class TheAbyss2Compat 
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BLARU, DDNames.SHORT_BLARU, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("theabyss", "blaru_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOG, DDNames.SHORT_BOG, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("theabyss", "bog_door")), BlockSetType.WARPED, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_FROZEN, DDNames.SHORT_FROZEN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("theabyss", "frozen_door")), BlockSetType.SPRUCE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ABYSSAL_JUNGLE, DDNames.SHORT_ABYSSAL_JUNGLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("theabyss", "jungle_door")), BlockSetType.JUNGLE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SAL, DDNames.SHORT_SAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("theabyss", "sal_door")), BlockSetType.WARPED, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SLIMED, DDNames.SHORT_SLIMED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("theabyss", "slimed_door")), BlockSetType.BIRCH, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_VIGILANT, DDNames.SHORT_VIGILANT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("theabyss", "vigilant_door")), BlockSetType.DARK_OAK, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BLARU, Identifier.fromNamespaceAndPath("theabyss", "blaru_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOG, Identifier.fromNamespaceAndPath("theabyss", "bog_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_FROZEN, Identifier.fromNamespaceAndPath("theabyss", "frozen_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ABYSSAL_JUNGLE, Identifier.fromNamespaceAndPath("theabyss", "jungle_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SAL, Identifier.fromNamespaceAndPath("theabyss", "sal_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SLIMED, Identifier.fromNamespaceAndPath("theabyss", "slimed_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_VIGILANT, Identifier.fromNamespaceAndPath("theabyss", "vigilant_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BLARU, Identifier.fromNamespaceAndPath("theabyss", "blaru_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOG, Identifier.fromNamespaceAndPath("theabyss", "bog_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_FROZEN, Identifier.fromNamespaceAndPath("theabyss", "frozen_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_ABYSSAL_JUNGLE, Identifier.fromNamespaceAndPath("theabyss", "jungle_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_SAL, Identifier.fromNamespaceAndPath("theabyss", "sal_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_SLIMED, Identifier.fromNamespaceAndPath("theabyss", "slimed_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_VIGILANT, Identifier.fromNamespaceAndPath("theabyss", "vigilant_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BLARU, Identifier.fromNamespaceAndPath("theabyss", "blaru_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOG, Identifier.fromNamespaceAndPath("theabyss", "bog_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_FROZEN, Identifier.fromNamespaceAndPath("theabyss", "frozen_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ABYSSAL_JUNGLE, Identifier.fromNamespaceAndPath("theabyss", "jungle_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SAL, Identifier.fromNamespaceAndPath("theabyss", "sal_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SLIMED, Identifier.fromNamespaceAndPath("theabyss", "slimed_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_VIGILANT, Identifier.fromNamespaceAndPath("theabyss", "vigilant_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BLARU, Identifier.fromNamespaceAndPath("theabyss", "blaru_door"), "tall_abyss_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOG, Identifier.fromNamespaceAndPath("theabyss", "bog_door"), "tall_abyss_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_FROZEN, Identifier.fromNamespaceAndPath("theabyss", "frozen_door"), "tall_abyss_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_ABYSSAL_JUNGLE, Identifier.fromNamespaceAndPath("theabyss", "jungle_door"), "tall_abyss_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_SAL, Identifier.fromNamespaceAndPath("theabyss", "sal_door"), "tall_abyss_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_SLIMED, Identifier.fromNamespaceAndPath("theabyss", "slimed_door"), "tall_abyss_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_VIGILANT, Identifier.fromNamespaceAndPath("theabyss", "vigilant_door"), "tall_abyss_wooden_door");
	}
}

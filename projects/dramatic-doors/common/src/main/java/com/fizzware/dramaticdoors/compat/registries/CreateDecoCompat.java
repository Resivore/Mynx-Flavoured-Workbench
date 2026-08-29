package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class CreateDecoCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ANDESITE, DDNames.SHORT_ANDESITE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "andesite_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BRASS, DDNames.SHORT_BRASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "brass_door")), BlockSetType.IRON, true);
		//DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CD_COPPER, DDNames.SHORT_CD_COPPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "copper_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CD_INDUSTRIAL_IRON, DDNames.SHORT_CD_INDUSTRIAL_IRON, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "industrial_iron_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ZINC, DDNames.SHORT_ZINC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "zinc_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LOCKED_ANDESITE, DDNames.SHORT_LOCKED_ANDESITE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "locked_andesite_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LOCKED_BRASS, DDNames.SHORT_LOCKED_BRASS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "locked_brass_door")), BlockSetType.IRON, true);
		//DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LOCKED_COPPER, DDNames.SHORT_LOCKED_COPPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "locked_copper_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LOCKED_INDUSTRIAL_IRON, DDNames.SHORT_LOCKED_INDUSTRIAL_IRON, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "locked_industrial_iron_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_LOCKED_ZINC, DDNames.SHORT_LOCKED_ZINC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("createdeco", "locked_zinc_door")), BlockSetType.IRON, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ANDESITE, Identifier.fromNamespaceAndPath("createdeco", "andesite_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BRASS, Identifier.fromNamespaceAndPath("createdeco", "brass_door"), false);
		//DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CD_COPPER, Identifier.fromNamespaceAndPath("createdeco", "copper_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CD_INDUSTRIAL_IRON, Identifier.fromNamespaceAndPath("createdeco", "industrial_iron_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ZINC, Identifier.fromNamespaceAndPath("createdeco", "zinc_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LOCKED_ANDESITE, Identifier.fromNamespaceAndPath("createdeco", "locked_andesite_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LOCKED_BRASS, Identifier.fromNamespaceAndPath("createdeco", "locked_brass_door"), false);
		//DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LOCKED_COPPER, Identifier.fromNamespaceAndPath("createdeco", "locked_copper_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LOCKED_INDUSTRIAL_IRON, Identifier.fromNamespaceAndPath("createdeco", "locked_industrial_iron_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_LOCKED_ZINC, Identifier.fromNamespaceAndPath("createdeco", "locked_zinc_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_ANDESITE, Identifier.fromNamespaceAndPath("createdeco", "andesite_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BRASS, Identifier.fromNamespaceAndPath("createdeco", "brass_door"));
		//DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CD_COPPER, Identifier.fromNamespaceAndPath("createdeco", "copper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CD_INDUSTRIAL_IRON, Identifier.fromNamespaceAndPath("createdeco", "industrial_iron_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_ZINC, Identifier.fromNamespaceAndPath("createdeco", "zinc_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LOCKED_ANDESITE, Identifier.fromNamespaceAndPath("createdeco", "locked_andesite_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LOCKED_BRASS, Identifier.fromNamespaceAndPath("createdeco", "locked_brass_door"));
		//DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LOCKED_COPPER, Identifier.fromNamespaceAndPath("createdeco", "locked_copper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LOCKED_INDUSTRIAL_IRON, Identifier.fromNamespaceAndPath("createdeco", "locked_industrial_iron_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_LOCKED_ZINC, Identifier.fromNamespaceAndPath("createdeco", "locked_zinc_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ANDESITE, Identifier.fromNamespaceAndPath("createdeco", "andesite_door"));
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BRASS, Identifier.fromNamespaceAndPath("createdeco", "brass_door"));
		//DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CD_COPPER, Identifier.fromNamespaceAndPath("createdeco", "copper_door"));
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CD_INDUSTRIAL_IRON, Identifier.fromNamespaceAndPath("createdeco", "industrial_iron_door"));
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ZINC, Identifier.fromNamespaceAndPath("createdeco", "zinc_door"));
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LOCKED_ANDESITE, Identifier.fromNamespaceAndPath("createdeco", "locked_andesite_door"));
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LOCKED_BRASS, Identifier.fromNamespaceAndPath("createdeco", "locked_brass_door"));
		//DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LOCKED_COPPER, Identifier.fromNamespaceAndPath("createdeco", "locked_copper_door"));
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LOCKED_INDUSTRIAL_IRON, Identifier.fromNamespaceAndPath("createdeco", "locked_industrial_iron_door"));
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_LOCKED_ZINC, Identifier.fromNamespaceAndPath("createdeco", "locked_zinc_door"));
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_ANDESITE, Identifier.fromNamespaceAndPath("createdeco", "andesite_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BRASS, Identifier.fromNamespaceAndPath("createdeco", "brass_door"), "tall_metal_door");
		//DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CD_COPPER, Identifier.fromNamespaceAndPath("createdeco", "copper_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CD_INDUSTRIAL_IRON, Identifier.fromNamespaceAndPath("createdeco", "industrial_iron_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_ZINC, Identifier.fromNamespaceAndPath("createdeco", "zinc_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LOCKED_ANDESITE, Identifier.fromNamespaceAndPath("createdeco", "locked_andesite_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LOCKED_BRASS, Identifier.fromNamespaceAndPath("createdeco", "locked_brass_door"), "tall_metal_door");
		//DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LOCKED_COPPER, Identifier.fromNamespaceAndPath("createdeco", "locked_copper_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LOCKED_INDUSTRIAL_IRON, Identifier.fromNamespaceAndPath("createdeco", "locked_industrial_iron_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_LOCKED_ZINC, Identifier.fromNamespaceAndPath("createdeco", "locked_zinc_door"), "tall_metal_door");
	}
}

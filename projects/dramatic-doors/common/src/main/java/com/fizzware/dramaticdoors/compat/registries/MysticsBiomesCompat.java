package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class MysticsBiomesCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MB_CHERRY, DDNames.SHORT_MB_CHERRY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mysticsbiomes", "cherry_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MB_JACARANDA, DDNames.SHORT_MB_JACARANDA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mysticsbiomes", "jacaranda_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MB_MAPLE, DDNames.SHORT_MB_MAPLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mysticsbiomes", "maple_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MB_PEACH, DDNames.SHORT_MB_PEACH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mysticsbiomes", "peach_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MB_SEA_FOAM, DDNames.SHORT_MB_SEA_FOAM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mysticsbiomes", "sea_foam_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MB_STRAWBERRY, DDNames.SHORT_MB_STRAWBERRY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("mysticsbiomes", "strawberry_door")), BlockSetType.OAK, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MB_CHERRY, Identifier.fromNamespaceAndPath("mysticsbiomes", "cherry_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MB_JACARANDA, Identifier.fromNamespaceAndPath("mysticsbiomes", "jacaranda_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MB_MAPLE, Identifier.fromNamespaceAndPath("mysticsbiomes", "maple_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MB_PEACH, Identifier.fromNamespaceAndPath("mysticsbiomes", "peach_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MB_SEA_FOAM, Identifier.fromNamespaceAndPath("mysticsbiomes", "sea_foam_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MB_STRAWBERRY, Identifier.fromNamespaceAndPath("mysticsbiomes", "strawberry_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MB_CHERRY, Identifier.fromNamespaceAndPath("mysticsbiomes", "cherry_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MB_JACARANDA, Identifier.fromNamespaceAndPath("mysticsbiomes", "jacaranda_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MB_MAPLE, Identifier.fromNamespaceAndPath("mysticsbiomes", "maple_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MB_PEACH, Identifier.fromNamespaceAndPath("mysticsbiomes", "peach_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MB_SEA_FOAM, Identifier.fromNamespaceAndPath("mysticsbiomes", "sea_foam_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MB_STRAWBERRY, Identifier.fromNamespaceAndPath("mysticsbiomes", "strawberry_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MB_CHERRY, Identifier.fromNamespaceAndPath("mysticsbiomes", "cherry_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MB_JACARANDA, Identifier.fromNamespaceAndPath("mysticsbiomes", "jacaranda_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MB_MAPLE, Identifier.fromNamespaceAndPath("mysticsbiomes", "maple_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MB_PEACH, Identifier.fromNamespaceAndPath("mysticsbiomes", "peach_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MB_SEA_FOAM, Identifier.fromNamespaceAndPath("mysticsbiomes", "sea_foam_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MB_STRAWBERRY, Identifier.fromNamespaceAndPath("mysticsbiomes", "strawberry_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MB_CHERRY, Identifier.fromNamespaceAndPath("mysticsbiomes", "cherry_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MB_JACARANDA, Identifier.fromNamespaceAndPath("mysticsbiomes", "jacaranda_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MB_MAPLE, Identifier.fromNamespaceAndPath("mysticsbiomes", "maple_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MB_PEACH, Identifier.fromNamespaceAndPath("mysticsbiomes", "peach_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MB_SEA_FOAM, Identifier.fromNamespaceAndPath("mysticsbiomes", "sea_foam_door"), "tall_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MB_STRAWBERRY, Identifier.fromNamespaceAndPath("mysticsbiomes", "strawberry_door"), "tall_wooden_door");
	}
}

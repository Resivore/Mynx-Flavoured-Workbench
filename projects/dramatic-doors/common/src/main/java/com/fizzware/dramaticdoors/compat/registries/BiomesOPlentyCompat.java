package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class BiomesOPlentyCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_DEAD, DDNames.SHORT_BOP_DEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "dead_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_EMPYREAL, DDNames.SHORT_BOP_EMPYREAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "empyreal_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_FIR, DDNames.SHORT_BOP_FIR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "fir_door")), BlockSetType.SPRUCE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_HELLBARK, DDNames.SHORT_BOP_HELLBARK, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "hellbark_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_JACARANDA, DDNames.SHORT_BOP_JACARANDA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "jacaranda_door")), BlockSetType.ACACIA, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_MAGIC, DDNames.SHORT_BOP_MAGIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "magic_door")), BlockSetType.BIRCH, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_MAHOGANY, DDNames.SHORT_BOP_MAHOGANY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "mahogany_door")), BlockSetType.ACACIA, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_MAPLE, DDNames.SHORT_BOP_MAPLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "maple_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_PALM, DDNames.SHORT_BOP_PALM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "palm_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_PINE, DDNames.SHORT_BOP_PINE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "pine_door")), BlockSetType.SPRUCE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_REDWOOD, DDNames.SHORT_BOP_REDWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "redwood_door")), BlockSetType.JUNGLE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_UMBRAN, DDNames.SHORT_BOP_UMBRAN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "umbran_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BOP_WILLOW, DDNames.SHORT_BOP_WILLOW, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomesoplenty", "willow_door")), BlockSetType.MANGROVE, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_DEAD, Identifier.fromNamespaceAndPath("biomesoplenty", "dead_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_EMPYREAL, Identifier.fromNamespaceAndPath("biomesoplenty", "empyreal_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_FIR, Identifier.fromNamespaceAndPath("biomesoplenty", "fir_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_HELLBARK, Identifier.fromNamespaceAndPath("biomesoplenty", "hellbark_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_JACARANDA, Identifier.fromNamespaceAndPath("biomesoplenty", "jacaranda_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_MAGIC, Identifier.fromNamespaceAndPath("biomesoplenty", "magic_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_MAHOGANY, Identifier.fromNamespaceAndPath("biomesoplenty", "mahogany_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_MAPLE, Identifier.fromNamespaceAndPath("biomesoplenty", "maple_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_PALM, Identifier.fromNamespaceAndPath("biomesoplenty", "palm_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_PINE, Identifier.fromNamespaceAndPath("biomesoplenty", "pine_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_REDWOOD, Identifier.fromNamespaceAndPath("biomesoplenty", "redwood_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_UMBRAN, Identifier.fromNamespaceAndPath("biomesoplenty", "umbran_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BOP_WILLOW, Identifier.fromNamespaceAndPath("biomesoplenty", "willow_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_DEAD, Identifier.fromNamespaceAndPath("biomesoplenty", "dead_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_EMPYREAL, Identifier.fromNamespaceAndPath("biomesoplenty", "empyreal_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_FIR, Identifier.fromNamespaceAndPath("biomesoplenty", "fir_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_HELLBARK, Identifier.fromNamespaceAndPath("biomesoplenty", "hellbark_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_JACARANDA, Identifier.fromNamespaceAndPath("biomesoplenty", "jacaranda_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_MAGIC, Identifier.fromNamespaceAndPath("biomesoplenty", "magic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_MAHOGANY, Identifier.fromNamespaceAndPath("biomesoplenty", "mahogany_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_MAPLE, Identifier.fromNamespaceAndPath("biomesoplenty", "maple_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_PALM, Identifier.fromNamespaceAndPath("biomesoplenty", "palm_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_PINE, Identifier.fromNamespaceAndPath("biomesoplenty", "pine_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_REDWOOD, Identifier.fromNamespaceAndPath("biomesoplenty", "redwood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_UMBRAN, Identifier.fromNamespaceAndPath("biomesoplenty", "umbran_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BOP_WILLOW, Identifier.fromNamespaceAndPath("biomesoplenty", "willow_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_DEAD, Identifier.fromNamespaceAndPath("biomesoplenty", "dead_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_EMPYREAL, Identifier.fromNamespaceAndPath("biomesoplenty", "empyreal_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_FIR, Identifier.fromNamespaceAndPath("biomesoplenty", "fir_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_HELLBARK, Identifier.fromNamespaceAndPath("biomesoplenty", "hellbark_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_JACARANDA, Identifier.fromNamespaceAndPath("biomesoplenty", "jacaranda_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_MAGIC, Identifier.fromNamespaceAndPath("biomesoplenty", "magic_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_MAHOGANY, Identifier.fromNamespaceAndPath("biomesoplenty", "mahogany_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_MAPLE, Identifier.fromNamespaceAndPath("biomesoplenty", "maple_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_PALM, Identifier.fromNamespaceAndPath("biomesoplenty", "palm_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_PINE, Identifier.fromNamespaceAndPath("biomesoplenty", "pine_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_REDWOOD, Identifier.fromNamespaceAndPath("biomesoplenty", "redwood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_UMBRAN, Identifier.fromNamespaceAndPath("biomesoplenty", "umbran_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BOP_WILLOW, Identifier.fromNamespaceAndPath("biomesoplenty", "willow_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_DEAD, Identifier.fromNamespaceAndPath("biomesoplenty", "dead_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_EMPYREAL, Identifier.fromNamespaceAndPath("biomesoplenty", "empyreal_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_FIR, Identifier.fromNamespaceAndPath("biomesoplenty", "fir_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_HELLBARK, Identifier.fromNamespaceAndPath("biomesoplenty", "hellbark_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_JACARANDA, Identifier.fromNamespaceAndPath("biomesoplenty", "jacaranda_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_MAGIC, Identifier.fromNamespaceAndPath("biomesoplenty", "magic_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_MAHOGANY, Identifier.fromNamespaceAndPath("biomesoplenty", "mahogany_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_MAPLE, Identifier.fromNamespaceAndPath("biomesoplenty", "maple_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_PALM, Identifier.fromNamespaceAndPath("biomesoplenty", "palm_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_PINE, Identifier.fromNamespaceAndPath("biomesoplenty", "pine_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_REDWOOD, Identifier.fromNamespaceAndPath("biomesoplenty", "redwood_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_UMBRAN, Identifier.fromNamespaceAndPath("biomesoplenty", "umbran_door"), "tall_bop_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BOP_WILLOW, Identifier.fromNamespaceAndPath("biomesoplenty", "willow_door"), "tall_bop_wooden_door");
	}
}

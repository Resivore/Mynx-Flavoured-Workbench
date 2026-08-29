package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class BetterNetherCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ANCHOR_TREE, DDNames.SHORT_ANCHOR_TREE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "anchor_tree_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BONE_CIN, DDNames.SHORT_BONE_CIN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "bone_cin_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BONE_REED, DDNames.SHORT_BONE_REED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "bone_reed_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MUSHROOM_FIR, DDNames.SHORT_MUSHROOM_FIR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NETHER_MUSHROOM, DDNames.SHORT_NETHER_MUSHROOM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NETHER_REED, DDNames.SHORT_NETHER_REED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NETHER_SAKURA, DDNames.SHORT_NETHER_SAKURA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUBEUS, DDNames.SHORT_RUBEUS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_STALAGNATE, DDNames.SHORT_STALAGNATE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_WART, DDNames.SHORT_WART, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_WILLOW, DDNames.SHORT_BN_WILLOW, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door")), BlockSetType.CRIMSON, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ANCHOR_TREE, Identifier.fromNamespaceAndPath("betternether", "anchor_tree_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BONE_CIN, Identifier.fromNamespaceAndPath("betternether", "bone_cincinnasite_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BONE_REED, Identifier.fromNamespaceAndPath("betternether", "bone_reed_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MUSHROOM_FIR, Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NETHER_MUSHROOM, Identifier.fromNamespaceAndPath("betternether", "nether_mushroom_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NETHER_REED, Identifier.fromNamespaceAndPath("betternether", "nether_reed_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NETHER_SAKURA, Identifier.fromNamespaceAndPath("betternether", "nether_sakura_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUBEUS, Identifier.fromNamespaceAndPath("betternether", "rubeus_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_STALAGNATE, Identifier.fromNamespaceAndPath("betternether", "stalagnate_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_WART, Identifier.fromNamespaceAndPath("betternether", "wart_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_WILLOW, Identifier.fromNamespaceAndPath("betternether", "willow_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_ANCHOR_TREE, Identifier.fromNamespaceAndPath("betternether", "anchor_tree_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BONE_CIN, Identifier.fromNamespaceAndPath("betternether", "bone_cincinnasite_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BONE_REED, Identifier.fromNamespaceAndPath("betternether", "bone_reed_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MUSHROOM_FIR, Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NETHER_MUSHROOM, Identifier.fromNamespaceAndPath("betternether", "nether_mushroom_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NETHER_REED, Identifier.fromNamespaceAndPath("betternether", "nether_reed_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NETHER_SAKURA, Identifier.fromNamespaceAndPath("betternether", "nether_sakura_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUBEUS, Identifier.fromNamespaceAndPath("betternether", "rubeus_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_STALAGNATE, Identifier.fromNamespaceAndPath("betternether", "stalagnate_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_WART, Identifier.fromNamespaceAndPath("betternether", "wart_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_WILLOW, Identifier.fromNamespaceAndPath("betternether", "willow_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ANCHOR_TREE, Identifier.fromNamespaceAndPath("betternether", "anchor_tree_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BONE_CIN, Identifier.fromNamespaceAndPath("betternether", "bone_cincinnasite_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BONE_REED, Identifier.fromNamespaceAndPath("betternether", "bone_reed_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MUSHROOM_FIR, Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NETHER_MUSHROOM, Identifier.fromNamespaceAndPath("betternether", "nether_mushroom_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NETHER_REED, Identifier.fromNamespaceAndPath("betternether", "nether_reed_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NETHER_SAKURA, Identifier.fromNamespaceAndPath("betternether", "nether_sakura_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUBEUS, Identifier.fromNamespaceAndPath("betternether", "rubeus_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_STALAGNATE, Identifier.fromNamespaceAndPath("betternether", "stalagnate_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_WART, Identifier.fromNamespaceAndPath("betternether", "wart_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_WILLOW, Identifier.fromNamespaceAndPath("betternether", "willow_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_ANCHOR_TREE, Identifier.fromNamespaceAndPath("betternether", "anchor_tree_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BONE_CIN, Identifier.fromNamespaceAndPath("betternether", "bone_cincinnasite_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BONE_REED, Identifier.fromNamespaceAndPath("betternether", "bone_reed_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MUSHROOM_FIR, Identifier.fromNamespaceAndPath("betternether", "mushroom_fir_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NETHER_MUSHROOM, Identifier.fromNamespaceAndPath("betternether", "nether_mushroom_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NETHER_REED, Identifier.fromNamespaceAndPath("betternether", "nether_reed_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NETHER_SAKURA, Identifier.fromNamespaceAndPath("betternether", "nether_sakura_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUBEUS, Identifier.fromNamespaceAndPath("betternether", "rubeus_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_STALAGNATE, Identifier.fromNamespaceAndPath("betternether", "stalagnate_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_WART, Identifier.fromNamespaceAndPath("betternether", "wart_door"), "tall_bn_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_WILLOW, Identifier.fromNamespaceAndPath("betternether", "willow_door"), "tall_bn_wooden_door");
	}
}

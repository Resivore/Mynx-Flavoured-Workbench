package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class BetweenlandsCompat 
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_GIANT_ROOT, DDNames.SHORT_GIANT_ROOT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_GIANT_ROOT_TREATED, DDNames.SHORT_GIANT_ROOT_TREATED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_HEARTHGROVE, DDNames.SHORT_HEARTHGROVE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_HEARTHGROVE_TREATED, DDNames.SHORT_HEARTHGROVE_TREATED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NIBBLETWIG, DDNames.SHORT_NIBBLETWIG, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NIBBLETWIG_TREATED, DDNames.SHORT_NIBBLETWIG_TREATED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ROTTEN_WOOD, DDNames.SHORT_ROTTEN_WOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ROTTEN_WOOD_TREATED, DDNames.SHORT_ROTTEN_WOOD_TREATED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_wood_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUBBER_TREE_PLANK, DDNames.SHORT_RUBBER_TREE_PLANK, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUBBER_TREE_PLANK_TREATED, DDNames.SHORT_RUBBER_TREE_PLANK_TREATED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door")), BlockSetType.MANGROVE, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_WEEDWOOD, DDNames.SHORT_WEEDWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_WEEDWOOD_TREATED, DDNames.SHORT_WEEDWOOD_TREATED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door")), BlockSetType.OAK, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SCABYST, DDNames.SHORT_SCABYST, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "scabyst_door")), BlockSetType.IRON, false);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_SYRMORITE, DDNames.SHORT_SYRMORITE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("thebetweenlands", "syrmorite_door")), BlockSetType.IRON, false);
	}
	
	private static void registerRecipes() {
		/*DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_GIANT_ROOT, Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_GIANT_ROOT_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_HEARTHGROVE, Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_HEARTHGROVE_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NIBBLETWIG, Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NIBBLETWIG_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ROTTEN_WOOD, Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_plank_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ROTTEN_WOOD_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_plank_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUBBER_TREE_PLANK, Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUBBER_TREE_PLANK_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_WEEDWOOD, Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_WEEDWOOD_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SCABYST, Identifier.fromNamespaceAndPath("thebetweenlands", "scabyst_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_SYRMORITE, Identifier.fromNamespaceAndPath("thebetweenlands", "syrmorite_door_item"));*/
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_GIANT_ROOT, Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_GIANT_ROOT_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_HEARTHGROVE, Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_HEARTHGROVE_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NIBBLETWIG, Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NIBBLETWIG_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_ROTTEN_WOOD, Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_plank_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_ROTTEN_WOOD_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_plank_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUBBER_TREE_PLANK, Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUBBER_TREE_PLANK_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_WEEDWOOD, Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_WEEDWOOD_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door_treated_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_SCABYST, Identifier.fromNamespaceAndPath("thebetweenlands", "scabyst_door_item"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_SYRMORITE, Identifier.fromNamespaceAndPath("thebetweenlands", "syrmorite_door_item"));
		
		/*DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_GIANT_ROOT, Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_GIANT_ROOT_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door_treated_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_HEARTHGROVE, Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_HEARTHGROVE_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door_treated_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NIBBLETWIG, Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NIBBLETWIG_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door_treated_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ROTTEN_WOOD, Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_plank_door_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ROTTEN_WOOD_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_plank_door_treated_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUBBER_TREE_PLANK, Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUBBER_TREE_PLANK_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door_treated_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_WEEDWOOD, Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_WEEDWOOD_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door_treated_item"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SCABYST, Identifier.fromNamespaceAndPath("thebetweenlands", "scabyst_door_item"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_SYRMORITE, Identifier.fromNamespaceAndPath("thebetweenlands", "syrmorite_door_item"), false);*/
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_GIANT_ROOT, Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_GIANT_ROOT_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "giant_root_door_treated_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_HEARTHGROVE, Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_HEARTHGROVE_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "hearthgrove_door_treated_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NIBBLETWIG, Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NIBBLETWIG_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "nibbletwig_door_treated_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_ROTTEN_WOOD, Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_plank_door_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_ROTTEN_WOOD_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "rotten_plank_door_treated_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUBBER_TREE_PLANK, Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUBBER_TREE_PLANK_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "rubber_tree_plank_door_treated_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_WEEDWOOD, Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_WEEDWOOD_TREATED, Identifier.fromNamespaceAndPath("thebetweenlands", "weedwood_door_treated_item"), "tall_bl_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_SCABYST, Identifier.fromNamespaceAndPath("thebetweenlands", "scabyst_door_item"), "tall_bl_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_SYRMORITE, Identifier.fromNamespaceAndPath("thebetweenlands", "syrmorite_door_item"), "tall_bl_metal_door");
	}
}

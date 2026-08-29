package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class NaturesSpiritCompat 
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_ASPEN, DDNames.SHORT_NS_ASPEN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "aspen_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_CEDAR, DDNames.SHORT_NS_CEDAR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "cedar_door")), BlockSetType.SPRUCE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_COCONUT, DDNames.SHORT_NS_COCONUT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "coconut_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_CYPRESS, DDNames.SHORT_NS_CYPRESS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "cypress_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_FIR, DDNames.SHORT_NS_FIR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "fir_door")), BlockSetType.SPRUCE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_GHAF, DDNames.SHORT_NS_GHAF, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "ghaf_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_JOSHUA, DDNames.SHORT_NS_JOSHUA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "joshua_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_LARCH, DDNames.SHORT_NS_LARCH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "larch_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_MAHOGANY, DDNames.SHORT_NS_MAHOGANY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "mahogany_door")), BlockSetType.JUNGLE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_MAPLE, DDNames.SHORT_NS_MAPLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "maple_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_OLIVE, DDNames.SHORT_NS_OLIVE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "olive_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_PALO_VERDE, DDNames.SHORT_NS_PALO_VERDE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "palo_verde_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_REDWOOD, DDNames.SHORT_NS_REDWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "redwood_door")), BlockSetType.SPRUCE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_SAXAUL, DDNames.SHORT_NS_SAXAUL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "saxaul_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_SUGI, DDNames.SHORT_NS_SUGI, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "sugi_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_WILLOW, DDNames.SHORT_NS_WILLOW, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "willow_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_WISTERIA, DDNames.SHORT_NS_WISTERIA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "wisteria_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_PAPER, DDNames.SHORT_NS_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "paper_door")), BlockSetType.BIRCH, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_FRAMED_PAPER, DDNames.SHORT_NS_FRAMED_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "framed_paper_door")), BlockSetType.BIRCH, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_NS_BLOOMING_PAPER, DDNames.SHORT_NS_BLOOMING_PAPER, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("natures_spirit", "blooming_paper_door")), BlockSetType.BIRCH, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_ASPEN, Identifier.fromNamespaceAndPath("natures_spirit", "aspen_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_CEDAR, Identifier.fromNamespaceAndPath("natures_spirit", "cedar_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_COCONUT, Identifier.fromNamespaceAndPath("natures_spirit", "coconut_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_CYPRESS, Identifier.fromNamespaceAndPath("natures_spirit", "cypress_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_FIR, Identifier.fromNamespaceAndPath("natures_spirit", "fir_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_GHAF, Identifier.fromNamespaceAndPath("natures_spirit", "ghaf_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_JOSHUA, Identifier.fromNamespaceAndPath("natures_spirit", "joshua_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_LARCH, Identifier.fromNamespaceAndPath("natures_spirit", "larch_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_MAHOGANY, Identifier.fromNamespaceAndPath("natures_spirit", "mahogany_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_MAPLE, Identifier.fromNamespaceAndPath("natures_spirit", "maple_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_OLIVE, Identifier.fromNamespaceAndPath("natures_spirit", "olive_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_PALO_VERDE, Identifier.fromNamespaceAndPath("natures_spirit", "palo_verde_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_REDWOOD, Identifier.fromNamespaceAndPath("natures_spirit", "redwood_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_SAXAUL, Identifier.fromNamespaceAndPath("natures_spirit", "saxaul_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_SUGI, Identifier.fromNamespaceAndPath("natures_spirit", "sugi_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_WILLOW, Identifier.fromNamespaceAndPath("natures_spirit", "willow_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_WISTERIA, Identifier.fromNamespaceAndPath("natures_spirit", "wisteria_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "paper_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_FRAMED_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "framed_paper_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_NS_BLOOMING_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "blooming_paper_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_ASPEN, Identifier.fromNamespaceAndPath("natures_spirit", "aspen_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_CEDAR, Identifier.fromNamespaceAndPath("natures_spirit", "cedar_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_COCONUT, Identifier.fromNamespaceAndPath("natures_spirit", "coconut_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_CYPRESS, Identifier.fromNamespaceAndPath("natures_spirit", "cypress_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_FIR, Identifier.fromNamespaceAndPath("natures_spirit", "fir_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_GHAF, Identifier.fromNamespaceAndPath("natures_spirit", "ghaf_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_JOSHUA, Identifier.fromNamespaceAndPath("natures_spirit", "joshua_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_LARCH, Identifier.fromNamespaceAndPath("natures_spirit", "larch_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_MAHOGANY, Identifier.fromNamespaceAndPath("natures_spirit", "mahogany_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_MAPLE, Identifier.fromNamespaceAndPath("natures_spirit", "maple_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_OLIVE, Identifier.fromNamespaceAndPath("natures_spirit", "olive_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_PALO_VERDE, Identifier.fromNamespaceAndPath("natures_spirit", "palo_verde_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_REDWOOD, Identifier.fromNamespaceAndPath("natures_spirit", "redwood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_SAXAUL, Identifier.fromNamespaceAndPath("natures_spirit", "saxaul_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_SUGI, Identifier.fromNamespaceAndPath("natures_spirit", "sugi_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_WILLOW, Identifier.fromNamespaceAndPath("natures_spirit", "willow_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_WISTERIA, Identifier.fromNamespaceAndPath("natures_spirit", "wisteria_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_FRAMED_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "framed_paper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_NS_BLOOMING_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "blooming_paper_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_ASPEN, Identifier.fromNamespaceAndPath("natures_spirit", "aspen_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_CEDAR, Identifier.fromNamespaceAndPath("natures_spirit", "cedar_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_COCONUT, Identifier.fromNamespaceAndPath("natures_spirit", "coconut_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_CYPRESS, Identifier.fromNamespaceAndPath("natures_spirit", "cypress_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_FIR, Identifier.fromNamespaceAndPath("natures_spirit", "fir_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_GHAF, Identifier.fromNamespaceAndPath("natures_spirit", "ghaf_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_JOSHUA, Identifier.fromNamespaceAndPath("natures_spirit", "joshua_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_LARCH, Identifier.fromNamespaceAndPath("natures_spirit", "larch_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_MAHOGANY, Identifier.fromNamespaceAndPath("natures_spirit", "mahogany_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_MAPLE, Identifier.fromNamespaceAndPath("natures_spirit", "maple_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_OLIVE, Identifier.fromNamespaceAndPath("natures_spirit", "olive_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_PALO_VERDE, Identifier.fromNamespaceAndPath("natures_spirit", "palo_verde_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_REDWOOD, Identifier.fromNamespaceAndPath("natures_spirit", "redwood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_SAXAUL, Identifier.fromNamespaceAndPath("natures_spirit", "saxaul_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_SUGI, Identifier.fromNamespaceAndPath("natures_spirit", "sugi_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_WILLOW, Identifier.fromNamespaceAndPath("natures_spirit", "willow_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_WISTERIA, Identifier.fromNamespaceAndPath("natures_spirit", "wisteria_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "paper_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_FRAMED_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "framed_paper_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_NS_BLOOMING_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "blooming_paper_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_ASPEN, Identifier.fromNamespaceAndPath("natures_spirit", "aspen_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_CEDAR, Identifier.fromNamespaceAndPath("natures_spirit", "cedar_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_COCONUT, Identifier.fromNamespaceAndPath("natures_spirit", "coconut_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_CYPRESS, Identifier.fromNamespaceAndPath("natures_spirit", "cypress_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_FIR, Identifier.fromNamespaceAndPath("natures_spirit", "fir_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_GHAF, Identifier.fromNamespaceAndPath("natures_spirit", "ghaf_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_JOSHUA, Identifier.fromNamespaceAndPath("natures_spirit", "joshua_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_LARCH, Identifier.fromNamespaceAndPath("natures_spirit", "larch_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_MAHOGANY, Identifier.fromNamespaceAndPath("natures_spirit", "mahogany_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_MAPLE, Identifier.fromNamespaceAndPath("natures_spirit", "maple_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_OLIVE, Identifier.fromNamespaceAndPath("natures_spirit", "olive_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_PALO_VERDE, Identifier.fromNamespaceAndPath("natures_spirit", "palo_verde_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_REDWOOD, Identifier.fromNamespaceAndPath("natures_spirit", "redwood_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_SAXAUL, Identifier.fromNamespaceAndPath("natures_spirit", "saxaul_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_SUGI, Identifier.fromNamespaceAndPath("natures_spirit", "sugi_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_WILLOW, Identifier.fromNamespaceAndPath("natures_spirit", "willow_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_WISTERIA, Identifier.fromNamespaceAndPath("natures_spirit", "wisteria_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "paper_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_FRAMED_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "framed_paper_door"), "tall_ns_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_NS_BLOOMING_PAPER, Identifier.fromNamespaceAndPath("natures_spirit", "blooming_paper_door"), "tall_ns_wooden_door");
	}
}

package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class PokecubeCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_AGED, DDNames.SHORT_POKECUBE_AGED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "aged_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_CONCRETE, DDNames.SHORT_POKECUBE_CONCRETE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "concrete_door"), Blocks.IRON_DOOR), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_CORRUPTED, DDNames.SHORT_POKECUBE_CORRUPTED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "corrupted_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_DISTORTIC, DDNames.SHORT_POKECUBE_DISTORTIC, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "distortic_door")), BlockSetType.WARPED, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_INVERTED, DDNames.SHORT_POKECUBE_INVERTED, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "inverted_door")), BlockSetType.WARPED, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_MIRAGE, DDNames.SHORT_POKECUBE_MIRAGE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "mirage_door")), BlockSetType.WARPED, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_TEMPORAL, DDNames.SHORT_POKECUBE_TEMPORAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "temporal_door")), BlockSetType.WARPED, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_ENIGMA, DDNames.SHORT_POKECUBE_ENIGMA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "enigma_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_LEPPA, DDNames.SHORT_POKECUBE_LEPPA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "leppa_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_NANAB, DDNames.SHORT_POKECUBE_NANAB, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "nanab_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_ORAN, DDNames.SHORT_POKECUBE_ORAN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "oran_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_PECHA, DDNames.SHORT_POKECUBE_PECHA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "pecha_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_POKECUBE_SITRUS, DDNames.SHORT_POKECUBE_SITRUS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("pokecube", "sitrus_door")), BlockSetType.OAK, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_AGED, Identifier.fromNamespaceAndPath("pokecube", "aged_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_CONCRETE, Identifier.fromNamespaceAndPath("pokecube", "concrete_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_CORRUPTED, Identifier.fromNamespaceAndPath("pokecube", "corrupted_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_DISTORTIC, Identifier.fromNamespaceAndPath("pokecube", "distortic_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_INVERTED, Identifier.fromNamespaceAndPath("pokecube", "inverted_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_MIRAGE, Identifier.fromNamespaceAndPath("pokecube", "mirage_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_TEMPORAL, Identifier.fromNamespaceAndPath("pokecube", "temporal_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_ENIGMA, Identifier.fromNamespaceAndPath("pokecube", "enigma_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_LEPPA, Identifier.fromNamespaceAndPath("pokecube", "leppa_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_NANAB, Identifier.fromNamespaceAndPath("pokecube", "nanab_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_ORAN, Identifier.fromNamespaceAndPath("pokecube", "oran_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_PECHA, Identifier.fromNamespaceAndPath("pokecube", "pecha_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_POKECUBE_SITRUS, Identifier.fromNamespaceAndPath("pokecube", "sitrus_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_AGED, Identifier.fromNamespaceAndPath("pokecube", "aged_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_CONCRETE, Identifier.fromNamespaceAndPath("pokecube", "concrete_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_CORRUPTED, Identifier.fromNamespaceAndPath("pokecube", "corrupted_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_DISTORTIC, Identifier.fromNamespaceAndPath("pokecube", "distortic_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_INVERTED, Identifier.fromNamespaceAndPath("pokecube", "inverted_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_MIRAGE, Identifier.fromNamespaceAndPath("pokecube", "mirage_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_TEMPORAL, Identifier.fromNamespaceAndPath("pokecube", "temporal_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_ENIGMA, Identifier.fromNamespaceAndPath("pokecube", "enigma_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_LEPPA, Identifier.fromNamespaceAndPath("pokecube", "leppa_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_NANAB, Identifier.fromNamespaceAndPath("pokecube", "nanab_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_ORAN, Identifier.fromNamespaceAndPath("pokecube", "oran_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_PECHA, Identifier.fromNamespaceAndPath("pokecube", "pecha_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_POKECUBE_SITRUS, Identifier.fromNamespaceAndPath("pokecube", "sitrus_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_AGED, Identifier.fromNamespaceAndPath("pokecube", "aged_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_CONCRETE, Identifier.fromNamespaceAndPath("pokecube", "concrete_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_CORRUPTED, Identifier.fromNamespaceAndPath("pokecube", "corrupted_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_DISTORTIC, Identifier.fromNamespaceAndPath("pokecube", "distortic_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_INVERTED, Identifier.fromNamespaceAndPath("pokecube", "inverted_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_MIRAGE, Identifier.fromNamespaceAndPath("pokecube", "mirage_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_TEMPORAL, Identifier.fromNamespaceAndPath("pokecube", "temporal_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_ENIGMA, Identifier.fromNamespaceAndPath("pokecube", "enigma_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_LEPPA, Identifier.fromNamespaceAndPath("pokecube", "leppa_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_NANAB, Identifier.fromNamespaceAndPath("pokecube", "nanab_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_ORAN, Identifier.fromNamespaceAndPath("pokecube", "oran_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_PECHA, Identifier.fromNamespaceAndPath("pokecube", "pecha_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_POKECUBE_SITRUS, Identifier.fromNamespaceAndPath("pokecube", "sitrus_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_AGED, Identifier.fromNamespaceAndPath("pokecube", "aged_door"), "tall_pokecube_legends_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_CONCRETE, Identifier.fromNamespaceAndPath("pokecube", "concrete_door"), "tall_pokecube_legends_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_CORRUPTED, Identifier.fromNamespaceAndPath("pokecube", "corrupted_door"), "tall_pokecube_legends_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_DISTORTIC, Identifier.fromNamespaceAndPath("pokecube", "distortic_door"), "tall_pokecube_legends_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_INVERTED, Identifier.fromNamespaceAndPath("pokecube", "inverted_door"), "tall_pokecube_legends_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_MIRAGE, Identifier.fromNamespaceAndPath("pokecube", "mirage_door"), "tall_pokecube_legends_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_TEMPORAL, Identifier.fromNamespaceAndPath("pokecube", "temporal_door"), "tall_pokecube_legends_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_ENIGMA, Identifier.fromNamespaceAndPath("pokecube", "enigma_door"), "tall_pokecube_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_LEPPA, Identifier.fromNamespaceAndPath("pokecube", "leppa_door"), "tall_pokecube_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_NANAB, Identifier.fromNamespaceAndPath("pokecube", "nanab_door"), "tall_pokecube_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_ORAN, Identifier.fromNamespaceAndPath("pokecube", "oran_door"), "tall_pokecube_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_PECHA, Identifier.fromNamespaceAndPath("pokecube", "pecha_door"), "tall_pokecube_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_POKECUBE_SITRUS, Identifier.fromNamespaceAndPath("pokecube", "sitrus_door"), "tall_pokecube_wooden_door");
	}
}

package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class EldritchEndCompat 
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_ETYR, DDNames.SHORT_ETYR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("eldritch_end", "etyr_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_CORRUPTED_ETYR, DDNames.SHORT_CORRUPTED_ETYR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("eldritch_end", "corrupted_etyr_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_DECADENT_ETYR, DDNames.SHORT_DECADENT_ETYR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("eldritch_end", "decadent_etyr_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_PERTURBED_ETYR, DDNames.SHORT_PERTURBED_ETYR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("eldritch_end", "perturbed_etyr_door")), BlockSetType.IRON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_PRIMORDIAL, DDNames.SHORT_PRIMORDIAL, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("eldritch_end", "primordial_door")), BlockSetType.WARPED, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "etyr_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_CORRUPTED_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "corrupted_etyr_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_DECADENT_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "decadent_etyr_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_PERTURBED_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "perturbed_etyr_door"), false);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_PRIMORDIAL, Identifier.fromNamespaceAndPath("eldritch_end", "primordial_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "etyr_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CORRUPTED_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "corrupted_etyr_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_DECADENT_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "decadent_etyr_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_PERTURBED_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "perturbed_etyr_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_PRIMORDIAL, Identifier.fromNamespaceAndPath("eldritch_end", "primordial_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "etyr_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_CORRUPTED_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "corrupted_etyr_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_DECADENT_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "decadent_etyr_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_PERTURBED_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "perturbed_etyr_door"), false);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_PRIMORDIAL, Identifier.fromNamespaceAndPath("eldritch_end", "primordial_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "etyr_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CORRUPTED_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "corrupted_etyr_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_DECADENT_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "decadent_etyr_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_PERTURBED_ETYR, Identifier.fromNamespaceAndPath("eldritch_end", "perturbed_etyr_door"), "tall_metal_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_PRIMORDIAL, Identifier.fromNamespaceAndPath("eldritch_end", "primordial_door"), "tall_wooden_door");
	}
}

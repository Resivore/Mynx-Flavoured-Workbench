package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class RegionsUnexploredCompat
{
	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_BAOBAB, DDNames.SHORT_RUE_BAOBAB, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "baobab_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_BLUE_BIOSHROOM, DDNames.SHORT_RUE_BLUE_BIOSHROOM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "blue_bioshroom_door")), BlockSetType.WARPED, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_GREEN_BIOSHROOM, DDNames.SHORT_RUE_GREEN_BIOSHROOM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "green_bioshroom_door")), BlockSetType.WARPED, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_PINK_BIOSHROOM, DDNames.SHORT_RUE_PINK_BIOSHROOM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "pink_bioshroom_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_YELLOW_BIOSHROOM, DDNames.SHORT_RUE_YELLOW_BIOSHROOM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "yellow_bioshroom_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_BLACKWOOD, DDNames.SHORT_RUE_BLACKWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "blackwood_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_BRIMWOOD, DDNames.SHORT_RUE_BRIMWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "brimwood_door")), BlockSetType.CRIMSON, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_COBALT, DDNames.SHORT_RUE_COBALT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "cypress_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_CYPRESS, DDNames.SHORT_RUE_CYPRESS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "cypress_door")), BlockSetType.MANGROVE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_DEAD, DDNames.SHORT_RUE_DEAD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "dead_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_EUCALYPTUS, DDNames.SHORT_RUE_EUCALYPTUS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "eucalyptus_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_JOSHUA, DDNames.SHORT_RUE_JOSHUA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "joshua_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_KAPOK, DDNames.SHORT_RUE_KAPOK, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "joshua_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_LARCH, DDNames.SHORT_RUE_LARCH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "larch_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_MAGNOLIA, DDNames.SHORT_RUE_MAGNOLIA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "magnolia_door")), BlockSetType.CHERRY, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_MAPLE, DDNames.SHORT_RUE_MAPLE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "maple_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_MAUVE, DDNames.SHORT_RUE_MAUVE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "mauve_door")), BlockSetType.OAK, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_PALM, DDNames.SHORT_RUE_PALM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "palm_door")), BlockSetType.BAMBOO, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_PINE, DDNames.SHORT_RUE_PINE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "pine_door")), BlockSetType.SPRUCE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_REDWOOD, DDNames.SHORT_RUE_REDWOOD, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "redwood_door")), BlockSetType.JUNGLE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_SOCOTRA, DDNames.SHORT_RUE_SOCOTRA, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "socotra_door")), BlockSetType.JUNGLE, true);
		DDRegistry.registerDoorBlockAndItem(DDNames.TALL_RUE_WILLOW, DDNames.SHORT_RUE_WILLOW, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("regions_unexplored", "willow_door")), BlockSetType.MANGROVE, true);
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_BAOBAB, Identifier.fromNamespaceAndPath("regions_unexplored", "baobab_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_BLUE_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "blue_bioshroom_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_GREEN_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "green_bioshroom_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_PINK_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "pink_bioshroom_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_YELLOW_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "yellow_bioshroom_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_BLACKWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "blackwood_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_BRIMWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "brimwood_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_COBALT, Identifier.fromNamespaceAndPath("regions_unexplored", "cobalt_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_CYPRESS, Identifier.fromNamespaceAndPath("regions_unexplored", "cypress_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_DEAD, Identifier.fromNamespaceAndPath("regions_unexplored", "dead_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_EUCALYPTUS, Identifier.fromNamespaceAndPath("regions_unexplored", "eucalyptus_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_JOSHUA, Identifier.fromNamespaceAndPath("regions_unexplored", "joshua_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_KAPOK, Identifier.fromNamespaceAndPath("regions_unexplored", "kapok_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_LARCH, Identifier.fromNamespaceAndPath("regions_unexplored", "larch_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_MAGNOLIA, Identifier.fromNamespaceAndPath("regions_unexplored", "magnolia_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_MAPLE, Identifier.fromNamespaceAndPath("regions_unexplored", "maple_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_MAUVE, Identifier.fromNamespaceAndPath("regions_unexplored", "mauve_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_PALM, Identifier.fromNamespaceAndPath("regions_unexplored", "palm_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_PINE, Identifier.fromNamespaceAndPath("regions_unexplored", "pine_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_REDWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "redwood_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_SOCOTRA, Identifier.fromNamespaceAndPath("regions_unexplored", "socotra_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_RUE_WILLOW, Identifier.fromNamespaceAndPath("regions_unexplored", "willow_door"), true);
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_BAOBAB, Identifier.fromNamespaceAndPath("regions_unexplored", "baobab_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_BLUE_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "blue_bioshroom_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_GREEN_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "green_bioshroom_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_PINK_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "pink_bioshroom_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_YELLOW_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "yellow_bioshroom_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_BLACKWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "blackwood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_BRIMWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "brimwood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_COBALT, Identifier.fromNamespaceAndPath("regions_unexplored", "cobalt_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_CYPRESS, Identifier.fromNamespaceAndPath("regions_unexplored", "cypress_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_DEAD, Identifier.fromNamespaceAndPath("regions_unexplored", "dead_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_EUCALYPTUS, Identifier.fromNamespaceAndPath("regions_unexplored", "eucalyptus_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_JOSHUA, Identifier.fromNamespaceAndPath("regions_unexplored", "joshua_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_KAPOK, Identifier.fromNamespaceAndPath("regions_unexplored", "kapok_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_LARCH, Identifier.fromNamespaceAndPath("regions_unexplored", "larch_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_MAGNOLIA, Identifier.fromNamespaceAndPath("regions_unexplored", "magnolia_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_MAPLE, Identifier.fromNamespaceAndPath("regions_unexplored", "maple_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_MAUVE, Identifier.fromNamespaceAndPath("regions_unexplored", "mauve_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_PALM, Identifier.fromNamespaceAndPath("regions_unexplored", "palm_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_PINE, Identifier.fromNamespaceAndPath("regions_unexplored", "pine_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_REDWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "redwood_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_SOCOTRA, Identifier.fromNamespaceAndPath("regions_unexplored", "socotra_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_RUE_WILLOW, Identifier.fromNamespaceAndPath("regions_unexplored", "willow_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_BAOBAB, Identifier.fromNamespaceAndPath("regions_unexplored", "baobab_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_BLUE_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "blue_bioshroom_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_GREEN_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "green_bioshroom_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_PINK_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "pink_bioshroom_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_YELLOW_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "yellow_bioshroom_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_BLACKWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "blackwood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_BRIMWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "brimwood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_COBALT, Identifier.fromNamespaceAndPath("regions_unexplored", "cobalt_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_CYPRESS, Identifier.fromNamespaceAndPath("regions_unexplored", "cypress_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_DEAD, Identifier.fromNamespaceAndPath("regions_unexplored", "dead_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_EUCALYPTUS, Identifier.fromNamespaceAndPath("regions_unexplored", "eucalyptus_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_JOSHUA, Identifier.fromNamespaceAndPath("regions_unexplored", "joshua_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_KAPOK, Identifier.fromNamespaceAndPath("regions_unexplored", "kapok_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_LARCH, Identifier.fromNamespaceAndPath("regions_unexplored", "larch_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_MAGNOLIA, Identifier.fromNamespaceAndPath("regions_unexplored", "magnolia_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_MAPLE, Identifier.fromNamespaceAndPath("regions_unexplored", "maple_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_MAUVE, Identifier.fromNamespaceAndPath("regions_unexplored", "mauve_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_PALM, Identifier.fromNamespaceAndPath("regions_unexplored", "palm_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_PINE, Identifier.fromNamespaceAndPath("regions_unexplored", "pine_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_REDWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "redwood_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_SOCOTRA, Identifier.fromNamespaceAndPath("regions_unexplored", "socotra_door"), true);
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_RUE_WILLOW, Identifier.fromNamespaceAndPath("regions_unexplored", "willow_door"), true);
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_BAOBAB, Identifier.fromNamespaceAndPath("regions_unexplored", "baobab_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_BLUE_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "blue_bioshroom_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_GREEN_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "green_bioshroom_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_PINK_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "pink_bioshroom_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_YELLOW_BIOSHROOM, Identifier.fromNamespaceAndPath("regions_unexplored", "yellow_bioshroom_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_BLACKWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "blackwood_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_BRIMWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "brimwood_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_COBALT, Identifier.fromNamespaceAndPath("regions_unexplored", "cobalt_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_CYPRESS, Identifier.fromNamespaceAndPath("regions_unexplored", "cypress_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_DEAD, Identifier.fromNamespaceAndPath("regions_unexplored", "dead_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_EUCALYPTUS, Identifier.fromNamespaceAndPath("regions_unexplored", "eucalyptus_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_JOSHUA, Identifier.fromNamespaceAndPath("regions_unexplored", "joshua_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_KAPOK, Identifier.fromNamespaceAndPath("regions_unexplored", "kapok_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_LARCH, Identifier.fromNamespaceAndPath("regions_unexplored", "larch_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_MAGNOLIA, Identifier.fromNamespaceAndPath("regions_unexplored", "magnolia_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_MAPLE, Identifier.fromNamespaceAndPath("regions_unexplored", "maple_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_MAUVE, Identifier.fromNamespaceAndPath("regions_unexplored", "mauve_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_PALM, Identifier.fromNamespaceAndPath("regions_unexplored", "palm_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_PINE, Identifier.fromNamespaceAndPath("regions_unexplored", "pine_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_REDWOOD, Identifier.fromNamespaceAndPath("regions_unexplored", "redwood_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_SOCOTRA, Identifier.fromNamespaceAndPath("regions_unexplored", "redwood_door"), "tall_rue_wooden_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_RUE_WILLOW, Identifier.fromNamespaceAndPath("regions_unexplored", "willow_door"), "tall_rue_wooden_door");
	}
}

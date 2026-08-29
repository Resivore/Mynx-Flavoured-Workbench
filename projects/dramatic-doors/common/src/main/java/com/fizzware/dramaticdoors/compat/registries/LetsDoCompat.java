package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.compat.CompatChecker;
import com.fizzware.dramaticdoors.compat.Compats;
import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class LetsDoCompat 
{
	public static void registerCompat(CompatChecker checker) {
		registerBlocksItems(checker);
		registerRecipes(checker);
	}
	
	private static void registerBlocksItems(CompatChecker checker) {
		if (Compats.isModLoaded("beachparty", checker)) {
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BEACHPARTY_PALM, DDNames.SHORT_BEACHPARTY_PALM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("beachparty", "palm_door")), BlockSetType.ACACIA, true);			
		}
		if (Compats.isModLoaded("bloomingnature", checker)) {
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_ASPEN, DDNames.SHORT_BN_ASPEN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("bloomingnature", "aspen_door")), BlockSetType.BIRCH, true);
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_BAOBAB, DDNames.SHORT_BN_BAOBAB, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("bloomingnature", "baobab_door")), BlockSetType.ACACIA, true);
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_CHESTNUT, DDNames.SHORT_BN_CHESTNUT, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("bloomingnature", "chestnut_door")), BlockSetType.OAK, true);
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_EBONY, DDNames.SHORT_BN_EBONY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("bloomingnature", "ebony_door")), BlockSetType.DARK_OAK, true);
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_FAN_PALM, DDNames.SHORT_BN_FAN_PALM, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("bloomingnature", "fan_palm_door")), BlockSetType.JUNGLE, true);
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_FIR, DDNames.SHORT_BN_FIR, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("bloomingnature", "fir_door")), BlockSetType.SPRUCE, true);
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_LARCH, DDNames.SHORT_BN_LARCH, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("bloomingnature", "larch_door")), BlockSetType.OAK, true);
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_SWAMP_CYPRESS, DDNames.SHORT_BN_SWAMP_CYPRESS, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("bloomingnature", "swamp_cypress_door")), BlockSetType.MANGROVE, true);
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_BN_SWAMP_OAK, DDNames.SHORT_BN_SWAMP_OAK, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("bloomingnature", "oak_door")), BlockSetType.OAK, true);
		}
		if (Compats.isModLoaded("meadow", checker)) {
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MEADOW_PINE, DDNames.SHORT_MEADOW_PINE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("meadow", "pine_door")), BlockSetType.SPRUCE, true);
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_MEADOW_PINE_BARN, DDNames.SHORT_MEADOW_PINE_BARN, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("meadow", "pine_barn_door")), BlockSetType.SPRUCE, true);
		}
		if (Compats.isModLoaded("vinery", checker)) {
			DDRegistry.registerDoorBlockAndItem(DDNames.TALL_VINERY_CHERRY, DDNames.SHORT_VINERY_CHERRY, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("vinery", "dark_cherry_door")), BlockSetType.CHERRY, true);
		}
	}
	
	private static void registerRecipes(CompatChecker checker) {
		if (Compats.isModLoaded("beachparty", checker)) {
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BEACHPARTY_PALM, Identifier.fromNamespaceAndPath("beachparty", "palm_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BEACHPARTY_PALM, Identifier.fromNamespaceAndPath("beachparty", "palm_door"));
			
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BEACHPARTY_PALM, Identifier.fromNamespaceAndPath("beachparty", "palm_door"), true);
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BEACHPARTY_PALM, Identifier.fromNamespaceAndPath("beachparty", "palm_door"), "tall_wooden_door");
		}
		if (Compats.isModLoaded("bloomingnature", checker)) {
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_ASPEN, Identifier.fromNamespaceAndPath("bloomingnature", "aspen_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_BAOBAB, Identifier.fromNamespaceAndPath("bloomingnature", "baobab_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_CHESTNUT, Identifier.fromNamespaceAndPath("bloomingnature", "chestnut_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_EBONY, Identifier.fromNamespaceAndPath("bloomingnature", "ebony_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_FAN_PALM, Identifier.fromNamespaceAndPath("bloomingnature", "fan_palm_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_FIR, Identifier.fromNamespaceAndPath("bloomingnature", "fir_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_LARCH, Identifier.fromNamespaceAndPath("bloomingnature", "larch_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_SWAMP_CYPRESS, Identifier.fromNamespaceAndPath("bloomingnature", "swamp_cypress_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_BN_SWAMP_OAK, Identifier.fromNamespaceAndPath("bloomingnature", "swamp_oak_door"), true);
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_ASPEN, Identifier.fromNamespaceAndPath("bloomingnature", "aspen_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_BAOBAB, Identifier.fromNamespaceAndPath("bloomingnature", "baobab_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_CHESTNUT, Identifier.fromNamespaceAndPath("bloomingnature", "chestnut_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_EBONY, Identifier.fromNamespaceAndPath("bloomingnature", "ebony_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_FAN_PALM, Identifier.fromNamespaceAndPath("bloomingnature", "fan_palm_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_FIR, Identifier.fromNamespaceAndPath("bloomingnature", "fir_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_LARCH, Identifier.fromNamespaceAndPath("bloomingnature", "larch_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_SWAMP_CYPRESS, Identifier.fromNamespaceAndPath("bloomingnature", "swamp_cypress_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_BN_SWAMP_OAK, Identifier.fromNamespaceAndPath("bloomingnature", "swamp_oak_door"));
			
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_ASPEN, Identifier.fromNamespaceAndPath("bloomingnature", "aspen_door"), true);
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_BAOBAB, Identifier.fromNamespaceAndPath("bloomingnature", "baobab_door"), true);
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_CHESTNUT, Identifier.fromNamespaceAndPath("bloomingnature", "chestnut_door"), true);
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_EBONY, Identifier.fromNamespaceAndPath("bloomingnature", "ebony_door"), true);
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_FAN_PALM, Identifier.fromNamespaceAndPath("bloomingnature", "fan_palm_door"), true);
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_FIR, Identifier.fromNamespaceAndPath("bloomingnature", "fir_door"), true);
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_LARCH, Identifier.fromNamespaceAndPath("bloomingnature", "larch_door"), true);
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_SWAMP_CYPRESS, Identifier.fromNamespaceAndPath("bloomingnature", "swamp_cypress_door"), true);
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_BN_SWAMP_OAK, Identifier.fromNamespaceAndPath("bloomingnature", "swamp_oak_door"), true);
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_ASPEN, Identifier.fromNamespaceAndPath("bloomingnature", "aspen_door"), "tall_letsdo_wooden_door");
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_BAOBAB, Identifier.fromNamespaceAndPath("bloomingnature", "baobab_door"), "tall_letsdo_wooden_door");
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_CHESTNUT, Identifier.fromNamespaceAndPath("bloomingnature", "chestnut_door"), "tall_letsdo_wooden_door");
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_EBONY, Identifier.fromNamespaceAndPath("bloomingnature", "ebony_door"), "tall_letsdo_wooden_door");
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_FAN_PALM, Identifier.fromNamespaceAndPath("bloomingnature", "fan_palm_door"), "tall_letsdo_wooden_door");
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_FIR, Identifier.fromNamespaceAndPath("bloomingnature", "fir_door"), "tall_letsdo_wooden_door");
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_LARCH, Identifier.fromNamespaceAndPath("bloomingnature", "larch_door"), "tall_letsdo_wooden_door");
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_SWAMP_CYPRESS, Identifier.fromNamespaceAndPath("bloomingnature", "swamp_cypress_door"), "tall_letsdo_wooden_door");
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_BN_SWAMP_OAK, Identifier.fromNamespaceAndPath("bloomingnature", "swamp_oak_door"), "tall_letsdo_wooden_door");
		}
		if (Compats.isModLoaded("meadow", checker)) {
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MEADOW_PINE, Identifier.fromNamespaceAndPath("meadow", "pine_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_MEADOW_PINE_BARN, Identifier.fromNamespaceAndPath("meadow", "pine_barn_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MEADOW_PINE, Identifier.fromNamespaceAndPath("meadow", "pine_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_MEADOW_PINE_BARN, Identifier.fromNamespaceAndPath("meadow", "pine_barn_door"));
			
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MEADOW_PINE, Identifier.fromNamespaceAndPath("meadow", "pine_door"), true);
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_MEADOW_PINE_BARN, Identifier.fromNamespaceAndPath("meadow", "pine_barn_door"), true);
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MEADOW_PINE, Identifier.fromNamespaceAndPath("meadow", "pine_door"), "tall_wooden_door");
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_MEADOW_PINE_BARN, Identifier.fromNamespaceAndPath("meadow", "pine_barn_door"), "tall_wooden_door");
		}
		if (Compats.isModLoaded("vinery", checker)) {
			DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_VINERY_CHERRY, Identifier.fromNamespaceAndPath("vinery", "dark_cherry_door"));
			DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_VINERY_CHERRY, Identifier.fromNamespaceAndPath("vinery", "dark_cherry_door"));
			
			DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_VINERY_CHERRY, Identifier.fromNamespaceAndPath("vinery", "dark_cherry_door"), true);
			DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_VINERY_CHERRY, Identifier.fromNamespaceAndPath("vinery", "dark_cherry_door"), "tall_wooden_door");
		}
	}
}

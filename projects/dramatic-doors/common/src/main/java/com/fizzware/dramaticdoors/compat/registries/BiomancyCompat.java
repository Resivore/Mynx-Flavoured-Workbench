package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.blocks.ShortDoorBlock;
import com.fizzware.dramaticdoors.blocks.ShortFleshDoorBlock;
import com.fizzware.dramaticdoors.blocks.ShortFullFleshDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallFleshDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallFullFleshDoorBlock;
import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import oshi.util.tuples.Pair;

public class BiomancyCompat
{
    public static final Block SHORT_FLESH_DOOR = new ShortFleshDoorBlock(BlockSetType.STONE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomancy", "flesh_door")));
    public static final Block SHORT_FULL_FLESH_DOOR = new ShortFullFleshDoorBlock(BlockSetType.STONE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomancy", "full_flesh_door")));
    public static final Block SHORT_FLESHKIN_DOOR = new ShortDoorBlock(BlockSetType.STONE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomancy", "fleshkin_door")));

    public static final Block TALL_FLESH_DOOR = new TallFleshDoorBlock(BlockSetType.STONE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomancy", "flesh_door")));
    public static final Block TALL_FULL_FLESH_DOOR = new TallFullFleshDoorBlock(BlockSetType.STONE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomancy", "full_flesh_door")));
    public static final Block TALL_FLESHKIN_DOOR = new TallDoorBlock(BlockSetType.STONE, DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("biomancy", "fleshkin_door")));

	public static void registerCompat() {
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
		DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.SHORT_FLESH, SHORT_FLESH_DOOR));
		DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.SHORT_FULL_FLESH, SHORT_FULL_FLESH_DOOR));
		DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.SHORT_FLESHKIN, SHORT_FLESHKIN_DOOR));
		DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.TALL_FLESH, TALL_FLESH_DOOR));
		DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.TALL_FULL_FLESH, TALL_FULL_FLESH_DOOR));
		DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.TALL_FLESHKIN, TALL_FLESHKIN_DOOR));

    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.SHORT_FLESH, DDRegistry.createDoorItem(DDNames.SHORT_FLESH, SHORT_FLESH_DOOR)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.SHORT_FULL_FLESH, DDRegistry.createDoorItem(DDNames.SHORT_FULL_FLESH, SHORT_FULL_FLESH_DOOR)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.SHORT_FLESHKIN, DDRegistry.createDoorItem(DDNames.SHORT_FLESHKIN, SHORT_FLESHKIN_DOOR)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.TALL_FLESH, DDRegistry.createDoorItem(DDNames.TALL_FLESH, TALL_FLESH_DOOR)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.TALL_FULL_FLESH, DDRegistry.createDoorItem(DDNames.TALL_FULL_FLESH, TALL_FULL_FLESH_DOOR)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.TALL_FLESHKIN, DDRegistry.createDoorItem(DDNames.TALL_FLESHKIN, TALL_FLESHKIN_DOOR)));
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_FLESH, Identifier.fromNamespaceAndPath("biomancy", "flesh_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_FULL_FLESH, Identifier.fromNamespaceAndPath("biomancy", "full_flesh_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.SHORT_FLESHKIN, Identifier.fromNamespaceAndPath("biomancy", "fleshkin_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_FLESH, Identifier.fromNamespaceAndPath("biomancy", "flesh_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_FULL_FLESH, Identifier.fromNamespaceAndPath("biomancy", "full_flesh_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_FLESHKIN, Identifier.fromNamespaceAndPath("biomancy", "fleshkin_door"));
		
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_FLESH, Identifier.fromNamespaceAndPath("biomancy", "flesh_door"));
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_FULL_FLESH, Identifier.fromNamespaceAndPath("biomancy", "full_flesh_door"));
		DDCompatRecipe.createShortDoorRecipe(DDNames.SHORT_FLESHKIN, Identifier.fromNamespaceAndPath("biomancy", "fleshkin_door"));
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_FLESH, Identifier.fromNamespaceAndPath("biomancy", "flesh_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_FULL_FLESH, Identifier.fromNamespaceAndPath("biomancy", "full_flesh_door"), "tall_misc_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_FLESHKIN, Identifier.fromNamespaceAndPath("biomancy", "fleshkin_door"), "tall_misc_door");
	}
}

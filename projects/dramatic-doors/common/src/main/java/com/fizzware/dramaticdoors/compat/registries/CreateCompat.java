package com.fizzware.dramaticdoors.compat.registries;

import com.fizzware.dramaticdoors.blocks.TallCreateSlidingDoorBlock;
import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import oshi.util.tuples.Pair;

public class CreateCompat
{
	public static boolean initialized = false;
	
	public static TallCreateSlidingDoorBlock TALL_ANDESITE_DOOR;
	public static TallCreateSlidingDoorBlock TALL_BRASS_DOOR;
	public static TallCreateSlidingDoorBlock TALL_COPPER_DOOR;
	public static TallCreateSlidingDoorBlock TALL_FRAMED_GLASS_DOOR;
	public static TallCreateSlidingDoorBlock TALL_TRAIN_DOOR;
	
	public static void registerCompat() {
		if (initialized) {
			return;
		}
		initialized = true;
		registerBlocksItems();
		registerRecipes();
	}
	
	private static void registerBlocksItems() {
    	DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.TALL_CREATE_ANDESITE, CreateCompat.TALL_ANDESITE_DOOR));
    	DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.TALL_CREATE_BRASS, CreateCompat.TALL_BRASS_DOOR));
    	DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.TALL_CREATE_COPPER, CreateCompat.TALL_COPPER_DOOR));
    	DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.TALL_CREATE_FRAMED_GLASS, CreateCompat.TALL_FRAMED_GLASS_DOOR));
    	DDRegistry.DOOR_BLOCKS.add(new Pair<String, Block>(DDNames.TALL_CREATE_TRAIN, CreateCompat.TALL_TRAIN_DOOR));

    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.TALL_CREATE_ANDESITE, DDRegistry.createDoorItem(DDNames.TALL_CREATE_ANDESITE, CreateCompat.TALL_ANDESITE_DOOR)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.TALL_CREATE_BRASS, DDRegistry.createDoorItem(DDNames.TALL_CREATE_BRASS, CreateCompat.TALL_BRASS_DOOR)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.TALL_CREATE_COPPER, DDRegistry.createDoorItem(DDNames.TALL_CREATE_COPPER, CreateCompat.TALL_COPPER_DOOR)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.TALL_CREATE_FRAMED_GLASS, DDRegistry.createDoorItem(DDNames.TALL_CREATE_FRAMED_GLASS, CreateCompat.TALL_FRAMED_GLASS_DOOR)));
    	DDRegistry.DOOR_ITEMS.add(new Pair<String, Item>(DDNames.TALL_CREATE_TRAIN, DDRegistry.createDoorItem(DDNames.TALL_CREATE_TRAIN, CreateCompat.TALL_TRAIN_DOOR)));
	}
	
	private static void registerRecipes() {
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CREATE_ANDESITE, Identifier.fromNamespaceAndPath("create", "andesite_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CREATE_BRASS, Identifier.fromNamespaceAndPath("create", "brass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CREATE_COPPER, Identifier.fromNamespaceAndPath("create", "copper_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CREATE_FRAMED_GLASS, Identifier.fromNamespaceAndPath("create", "framed_glass_door"));
		DDCompatAdvancement.createRecipeAdvancement(DDNames.TALL_CREATE_TRAIN, Identifier.fromNamespaceAndPath("create", "train_door"));
		
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CREATE_ANDESITE, Identifier.fromNamespaceAndPath("create", "andesite_door"), "tall_create_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CREATE_BRASS, Identifier.fromNamespaceAndPath("create", "brass_door"), "tall_create_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CREATE_COPPER, Identifier.fromNamespaceAndPath("create", "copper_door"), "tall_create_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CREATE_FRAMED_GLASS, Identifier.fromNamespaceAndPath("create", "framed_glass_door"), "tall_create_door");
		DDCompatRecipe.createTallDoorRecipe(DDNames.TALL_CREATE_TRAIN, Identifier.fromNamespaceAndPath("create", "train_door"), "tall_create_door");
	}
}

package com.fizzware.dramaticdoors.tags;

import com.fizzware.dramaticdoors.DramaticDoors;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class DDBlockTags
{
	public static final TagKey<Block> SHORT_DOORS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, "short_doors"));
	public static final TagKey<Block> SHORT_WOODEN_DOORS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, "short_wooden_doors"));
	public static final TagKey<Block> MOB_INTERACTABLE_SHORT_DOORS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, "mob_interactable_short_doors"));

	public static final TagKey<Block> TALL_DOORS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, "tall_doors"));
	public static final TagKey<Block> TALL_WOODEN_DOORS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, "tall_wooden_doors"));
	public static final TagKey<Block> MOB_INTERACTABLE_TALL_DOORS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, "mob_interactable_tall_doors"));
}

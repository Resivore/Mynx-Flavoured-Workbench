/*package com.fizzware.dramaticdoors.fabric.compat;

import com.fizzware.dramaticdoors.DDRegistry;
import com.fizzware.dramaticdoors.blocks.TallCreateSlidingDoorBlock;
import com.fizzware.dramaticdoors.compat.registries.CreateCompat;
import com.fizzware.dramaticdoors.fabric.addons.create.ShortDoorMovingInteraction;
import com.fizzware.dramaticdoors.fabric.addons.create.TallDoorMovingInteraction;
import com.fizzware.dramaticdoors.fabric.addons.create.TallFabricCreateSlidingDoorBlock;
import com.fizzware.dramaticdoors.fabric.addons.create.TallFabricCreateSlidingDoorBlockEntity;
import com.fizzware.dramaticdoors.fabric.addons.create.TallSlidingDoorMovementBehaviour;
import com.fizzware.dramaticdoors.tags.DDBlockTags;
import com.simibubi.create.AllInteractionBehaviours;
import com.simibubi.create.AllMovementBehaviours;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class CreateFabricCompat
{
	public static BlockEntityType<TallFabricCreateSlidingDoorBlockEntity> TALL_SLIDING_DOOR_BLOCK_ENTITY;

	public static void registerCompat() {
		if (!CreateCompat.initialized) {
			CreateCompat.TALL_ANDESITE_DOOR = new TallFabricCreateSlidingDoorBlock(DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("create", "andesite_door"), Blocks.IRON_DOOR), BlockSetType.IRON, true);
			CreateCompat.TALL_BRASS_DOOR = new TallFabricCreateSlidingDoorBlock(DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("create", "brass_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
			CreateCompat.TALL_COPPER_DOOR = new TallFabricCreateSlidingDoorBlock(DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("create", "copper_door"), Blocks.IRON_DOOR), BlockSetType.IRON, true);
			CreateCompat.TALL_FRAMED_GLASS_DOOR = new TallFabricCreateSlidingDoorBlock(DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("create", "framed_glass_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
			CreateCompat.TALL_TRAIN_DOOR = new TallFabricCreateSlidingDoorBlock(DDRegistry.getBlockFromIdentifier(Identifier.fromNamespaceAndPath("create", "train_door"), Blocks.IRON_DOOR), BlockSetType.IRON, false);
		}
    	CreateCompat.registerCompat();
    	
		TallDoorMovingInteraction tallDoorBehaviour = new TallDoorMovingInteraction();
		AllInteractionBehaviours.registerBehaviourProvider(state -> {
			if (state.is(DDBlockTags.TALL_WOODEN_DOORS) || state.is(DDBlockTags.HAND_OPENABLE_TALL_METAL_DOORS)) {
				return tallDoorBehaviour;
			}
			return null;
		});
		ShortDoorMovingInteraction shortDoorBehaviour = new ShortDoorMovingInteraction();
		AllInteractionBehaviours.registerBehaviourProvider(state -> {
			if (state.is(DDBlockTags.SHORT_WOODEN_DOORS) || state.is(DDBlockTags.HAND_OPENABLE_SHORT_METAL_DOORS)) {
				return shortDoorBehaviour;
			}
			return null;
		});
		TallSlidingDoorMovementBehaviour tallSlidingDoorMovementBehaviour = new TallSlidingDoorMovementBehaviour();
		AllMovementBehaviours.registerBehaviourProvider(state -> {
			if (state.getBlock() instanceof TallCreateSlidingDoorBlock) {
				return tallSlidingDoorMovementBehaviour;
			}
			return null;
		});
	}
}
*/
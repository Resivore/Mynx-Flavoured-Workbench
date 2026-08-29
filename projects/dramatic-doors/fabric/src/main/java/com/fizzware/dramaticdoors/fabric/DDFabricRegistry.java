package com.fizzware.dramaticdoors.fabric;

import java.util.Set;

import com.fizzware.dramaticdoors.DramaticDoors;
import com.fizzware.dramaticdoors.blockentities.DDBlockEntities;
import com.fizzware.dramaticdoors.blockentities.TallNetheriteDoorBlockEntity;
import com.fizzware.dramaticdoors.compat.Compats;
//import com.fizzware.dramaticdoors.compat.registries.CreateCompat;
import com.fizzware.dramaticdoors.compat.registries.SupplementariesCompat;
import com.fizzware.dramaticdoors.registry.DDCreativeTabs;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.registry.DDRegistry;
//import com.fizzware.dramaticdoors.fabric.addons.create.TallFabricCreateSlidingDoorBlockEntity;
//import com.fizzware.dramaticdoors.fabric.compat.CreateFabricCompat;
import com.fizzware.dramaticdoors.tags.DDItemTags;

import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
//import net.minecraft.world.level.block.entity.BlockEntityType;
import oshi.util.tuples.Pair;

public class DDFabricRegistry
{
	
	public static void registerBlocksItems() {
		// Iterate through the blocks and items to register.
		for (Pair<String, Block> pair : DDRegistry.DOOR_BLOCKS) {
			Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, pair.getA()), pair.getB());
		}
		for (Pair<String, Item> pair : DDRegistry.DOOR_ITEMS) {
			Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, pair.getA()), pair.getB());
		}
	}

	public static void registerBlockEntities() {
		if (Compats.isModLoaded("supplementaries", FabricUtils.INSTANCE)) {
			DDBlockEntities.TALL_NETHERITE_DOOR = new BlockEntityType<>(TallNetheriteDoorBlockEntity::new, Set.of(SupplementariesCompat.SHORT_NETHERITE_DOOR, SupplementariesCompat.TALL_NETHERITE_DOOR));
			Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, "tall_netherite_door"), DDBlockEntities.TALL_NETHERITE_DOOR);
		}
		/*if (Compats.isModLoaded("create", FabricUtils.INSTANCE)) {
			CreateFabricCompat.TALL_SLIDING_DOOR_BLOCK_ENTITY = BlockEntityType.Builder.of(TallFabricCreateSlidingDoorBlockEntity::new, CreateCompat.TALL_ANDESITE_DOOR, CreateCompat.TALL_BRASS_DOOR, CreateCompat.TALL_COPPER_DOOR, CreateCompat.TALL_FRAMED_GLASS_DOOR, CreateCompat.TALL_TRAIN_DOOR).build(null);
			Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, "tall_sliding_door"), CreateFabricCompat.TALL_SLIDING_DOOR_BLOCK_ENTITY);
		}*/
	}

	public static void registerCreativeTabs() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DDCreativeTabs.MAIN_TAB, CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0).title(Component.translatable("itemGroup.dramaticdoors")).icon(() -> new ItemStack(item(DDNames.TALL_OAK))).displayItems((parameters, output) -> FabricUtils.addMainTabEntries(output)).build());
		if (Compats.isModLoaded("chipped", FabricUtils.INSTANCE)) {
			Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DDCreativeTabs.CHIPPED_TAB, CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1).title(Component.translatable("itemGroup.dramaticdoors_chipped")).icon(() -> new ItemStack(item(DDNames.TALL_CHIPPED_BIRCH_GATED))).displayItems((parameters, output) -> FabricUtils.addChippedTabEntries(output)).build());
		}
		if (Compats.isModLoaded("mcwdoors", FabricUtils.INSTANCE)) {
			Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DDCreativeTabs.MACAW_TAB, CreativeModeTab.builder(CreativeModeTab.Row.TOP, 2).title(Component.translatable("itemGroup.dramaticdoors_macaw")).icon(() -> new ItemStack(item(DDNames.TALL_MACAW_DARK_OAK_BARN))).displayItems((parameters, output) -> FabricUtils.addMacawTabEntries(output)).build());
		}
		if (Compats.isModLoaded("manyideas_doors", FabricUtils.INSTANCE)) {
			Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DDCreativeTabs.MANYIDEAS_TAB, CreativeModeTab.builder(CreativeModeTab.Row.TOP, 3).title(Component.translatable("itemGroup.dramaticdoors_manyideas")).icon(() -> new ItemStack(item(DDNames.TALL_MANYIDEAS_CRIMSON_BLANK))).displayItems((parameters, output) -> FabricUtils.addManyIdeasTabEntries(output)).build());
		}
	}

	public static void registerWeatherables() {
		OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_EXPOSED_COPPER)));
		OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_EXPOSED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WEATHERED_COPPER)));
		OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WEATHERED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_OXIDIZED_COPPER)));
		OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_EXPOSED_COPPER)));
		OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_EXPOSED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WEATHERED_COPPER)));
		OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WEATHERED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_OXIDIZED_COPPER)));
		OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WAXED_COPPER)));
		OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_EXPOSED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WAXED_EXPOSED_COPPER)));
		OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WEATHERED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WAXED_WEATHERED_COPPER)));
		OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_OXIDIZED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WAXED_OXIDIZED_COPPER)));
		OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WAXED_COPPER)));
		OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_EXPOSED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WAXED_EXPOSED_COPPER)));
		OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WEATHERED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WAXED_WEATHERED_COPPER)));
		OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_OXIDIZED_COPPER)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WAXED_OXIDIZED_COPPER)));
		if (Compats.isModLoaded("immersive_weathering", FabricUtils.INSTANCE)) {
			OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_EXPOSED_IRON)));
			OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_EXPOSED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WEATHERED_IRON)));
			OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WEATHERED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_RUSTED_IRON)));
			OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_EXPOSED_IRON)));
			OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_EXPOSED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WEATHERED_IRON)));
			OxidizableBlocksRegistry.registerNextStage(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WEATHERED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_RUSTED_IRON)));
			OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WAXED_IRON)));
			OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_EXPOSED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WAXED_EXPOSED_IRON)));
			OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WEATHERED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WAXED_WEATHERED_IRON)));
			OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_RUSTED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_WAXED_RUSTED_IRON)));
			OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WAXED_IRON)));
			OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_EXPOSED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WAXED_EXPOSED_IRON)));
			OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WEATHERED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WAXED_WEATHERED_IRON)));
			OxidizableBlocksRegistry.registerWaxable(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_RUSTED_IRON)), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.TALL_WAXED_RUSTED_IRON)));
		}
	}
	
	public static void registerFuels() {
		// Set up fuel. Only wooden doors can be used as fuel. Nether wood automatically excluded.
		FuelValueEvents.BUILD.register((builder, context) -> {
			builder.add(DDItemTags.TALL_WOODEN_DOORS, 300);
			builder.add(DDItemTags.SHORT_WOODEN_DOORS, 100);
		});
	}

	private static Item item(String name) {
		return DDRegistry.DOOR_ITEMS.stream().filter(pair -> pair.getA().equals(name)).findFirst().orElseThrow().getB();
	}
}

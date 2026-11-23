package dev.resivore.mynxtrees;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.*;
import java.util.function.Function;

final class NativeWoods {
    static final Block MOSSY_LOG = MynxTrees.block("mossy_oak_log", RotatedPillarBlock::new, Blocks.OAK_LOG);
    static final Block MOSSY_WOOD = MynxTrees.block("mossy_oak_wood", RotatedPillarBlock::new, Blocks.OAK_WOOD);

    static void register() {
        logs("silver_birch", MynxTrees.SILVER_LOG, MynxTrees.SILVER_WOOD);
        logs("wisteria", MynxTrees.WISTERIA_LOG, MynxTrees.WISTERIA_WOOD);
        logs("mossy_oak", MOSSY_LOG, MOSSY_WOOD);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out -> out.accept(MOSSY_LOG));
        family("silver_birch", WoodType.BIRCH);
        family("wisteria", WoodType.CHERRY);
    }

    private static void logs(String family, Block log, Block wood) {
        Block strippedLog = MynxTrees.block("stripped_" + family + "_log", RotatedPillarBlock::new, Blocks.STRIPPED_OAK_LOG);
        Block strippedWood = MynxTrees.block("stripped_" + family + "_wood", RotatedPillarBlock::new, Blocks.STRIPPED_OAK_WOOD);
        StrippableBlockRegistry.register(log, strippedLog);
        StrippableBlockRegistry.register(wood, strippedWood);
        for (var block : new Block[]{log, wood, strippedLog, strippedWood}) {
            FlammableBlockRegistry.getDefaultInstance().add(block, 5, 5);
            CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out -> out.accept(block));
        }
    }

    private static void family(String name, WoodType base) {
        var set = BlockSetTypeBuilder.copyOf(base.setType()).register(MynxTrees.id(name));
        var wood = WoodTypeBuilder.copyOf(base).register(MynxTrees.id(name), set);
        var planks = building(name, "planks", Blocks.BIRCH_PLANKS, Block::new);
        building(name, "stairs", Blocks.BIRCH_STAIRS, p -> new StairBlock(planks.defaultBlockState(), p) {});
        building(name, "slab", Blocks.BIRCH_SLAB, SlabBlock::new);
        building(name, "fence", Blocks.BIRCH_FENCE, FenceBlock::new);
        building(name, "fence_gate", Blocks.BIRCH_FENCE_GATE, p -> new FenceGateBlock(wood, p));
        building(name, "button", Blocks.BIRCH_BUTTON, p -> new ButtonBlock(set, 30, p) {});
        building(name, "pressure_plate", Blocks.BIRCH_PRESSURE_PLATE, p -> new PressurePlateBlock(set, p) {});
        building(name, "trapdoor", Blocks.BIRCH_TRAPDOOR, p -> new TrapDoorBlock(set, p) {});
        Block door = bare(name + "_door", Blocks.BIRCH_DOOR, p -> new DoorBlock(set, p) {});
        item(door, p -> new DoubleHighBlockItem(door, p));
        Block sign = bare(name + "_sign", Blocks.BIRCH_SIGN, p -> new StandingSignBlock(wood, p));
        Block wall = bare(name + "_wall_sign", Blocks.BIRCH_WALL_SIGN, p -> new WallSignBlock(wood,
                p.overrideLootTable(sign.getLootTable()).overrideDescription(sign.getDescriptionId())));
        Block hanging = bare(name + "_hanging_sign", Blocks.BIRCH_HANGING_SIGN, p -> new CeilingHangingSignBlock(wood, p));
        Block hangingWall = bare(name + "_wall_hanging_sign", Blocks.BIRCH_WALL_HANGING_SIGN, p -> new WallHangingSignBlock(wood,
                p.overrideLootTable(hanging.getLootTable()).overrideDescription(hanging.getDescriptionId())));
        Block shelf = bare(name + "_shelf", Blocks.BIRCH_SHELF, ShelfBlock::new);
        BlockEntityTypes.SIGN.addValidBlock(sign);
        BlockEntityTypes.SIGN.addValidBlock(wall);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(hanging);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(hangingWall);
        BlockEntityTypes.SHELF.addValidBlock(shelf);
        FlammableBlockRegistry.getDefaultInstance().add(shelf, 5, 20);
        item(sign, p -> new SignItem(sign, wall, p.stacksTo(16)));
        item(hanging, p -> new HangingSignItem(hanging, hangingWall, p.stacksTo(16)));
        item(shelf, p -> new BlockItem(shelf, p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)));
        WoodBoats.register(name);
    }

    private static Block bare(String name, Block reference, Function<Properties, Block> create) {
        var key = ResourceKey.create(Registries.BLOCK, MynxTrees.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, create.apply(Properties.ofFullCopy(reference).setId(key)));
    }

    private static Block building(String family, String form, Block reference, Function<Properties, Block> create) {
        Block block = bare(family + "_" + form, reference, create);
        item(block, p -> new BlockItem(block, p));
        var fire = FlammableBlockRegistry.getDefaultInstance();
        var entry = fire.get(reference);
        if (entry != null) fire.add(block, entry);
        return block;
    }

    private static void item(Block block, Function<Item.Properties, Item> create) {
        var key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, create.apply(new Item.Properties().setId(key).useBlockDescriptionPrefix()));
        var tab = block instanceof SignBlock || block instanceof ShelfBlock
                ? CreativeModeTabs.FUNCTIONAL_BLOCKS : CreativeModeTabs.BUILDING_BLOCKS;
        CreativeModeTabEvents.modifyOutputEvent(tab).register(out -> out.accept(item));
    }
}

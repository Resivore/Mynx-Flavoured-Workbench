package dev.resivore.amc;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.*;

import java.util.function.Function;

final class MossyOakFixtures {
    static void register() {
        var wood = WoodTypeBuilder.copyOf(WoodType.OAK).register(id("mossy_oak"), BlockSetType.OAK);
        Block sign = block("sign", Blocks.OAK_SIGN, p -> new StandingSignBlock(wood, p));
        Block wall = block("wall_sign", Blocks.OAK_WALL_SIGN, p -> new WallSignBlock(wood,
                p.overrideLootTable(sign.getLootTable()).overrideDescription(sign.getDescriptionId())));
        Block hanging = block("hanging_sign", Blocks.OAK_HANGING_SIGN, p -> new CeilingHangingSignBlock(wood, p));
        Block hangingWall = block("wall_hanging_sign", Blocks.OAK_WALL_HANGING_SIGN, p -> new WallHangingSignBlock(wood,
                p.overrideLootTable(hanging.getLootTable()).overrideDescription(hanging.getDescriptionId())));
        Block shelf = block("shelf", Blocks.OAK_SHELF, ShelfBlock::new);
        BlockEntityTypes.SIGN.addValidBlock(sign);
        BlockEntityTypes.SIGN.addValidBlock(wall);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(hanging);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(hangingWall);
        BlockEntityTypes.SHELF.addValidBlock(shelf);
        item(sign, p -> new SignItem(sign, wall, p.stacksTo(16)));
        item(hanging, p -> new HangingSignItem(hanging, hangingWall, p.stacksTo(16)));
        item(shelf, p -> new BlockItem(shelf, p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("architectural_material_closure", path);
    }

    private static Block block(String suffix, Block oak, Function<BlockBehaviour.Properties, Block> factory) {
        var key = ResourceKey.create(Registries.BLOCK, id("mossy_oak_" + suffix));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(oak).setId(key)));
    }

    private static void item(Block block, Function<Item.Properties, Item> factory) {
        var key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        var item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key).useBlockDescriptionPrefix()));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> out.accept(item));
    }
}


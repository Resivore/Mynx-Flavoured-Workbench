package dev.resivore.amc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.kikoz.mcwwindows.init.BlockInit;
import net.kikoz.mcwwindows.objects.ArrowSill;
import net.kikoz.mcwwindows.objects.GothicWindow;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.function.Function;

public final class AndesiteBlocks implements ModInitializer {
    @Override
    public void onInitialize() {
        Item button = register("andesite_button", Blocks.STONE_BUTTON,
                props -> new ButtonBlock(BlockSetType.STONE, 20, props));
        Item plate = register("andesite_pressure_plate", Blocks.STONE_PRESSURE_PLATE,
                props -> new PressurePlateBlock(BlockSetType.STONE, props));
        Item window = register("andesite_gothic_window", BlockInit.STONE_BRICK_GOTHIC, GothicWindow::new);
        Item slit = register("andesite_arrow_slit", BlockInit.STONE_BRICK_ARROW_SLIT, ArrowSill::new);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
            output.accept(button);
            output.accept(plate);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
            output.accept(window);
            output.accept(slit);
        });
    }

    private static Item register(String name, Block reference, Function<BlockBehaviour.Properties, Block> factory) {
        var id = Identifier.fromNamespaceAndPath("architectural_material_closure", name);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = Registry.register(BuiltInRegistries.BLOCK, key,
                factory.apply(BlockBehaviour.Properties.ofFullCopy(reference).setId(key)));
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }
}

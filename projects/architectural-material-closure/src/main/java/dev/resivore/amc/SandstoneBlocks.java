package dev.resivore.amc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.kikoz.mcwwindows.init.BlockInit;
import net.kikoz.mcwwindows.objects.ArrowSill;
import net.kikoz.mcwwindows.objects.GothicWindow;
import net.kikoz.mcwwindows.objects.Parapet;
import net.kikoz.mcwwindows.objects.Shutter;
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

import java.util.List;
import java.util.function.Function;

public final class SandstoneBlocks implements ModInitializer {
    @Override
    public void onInitialize() {
        registerFamily("sandstone");
        registerFamily("red_sandstone");
    }

    private static void registerFamily(String material) {
        var button = register(material, "button", Blocks.STONE_BUTTON,
                props -> new ButtonBlock(BlockSetType.STONE, 20, props));
        var plate = register(material, "pressure_plate", Blocks.STONE_PRESSURE_PLATE,
                props -> new PressurePlateBlock(BlockSetType.STONE, props));
        var building = List.of(
                register(material, "gothic_window", BlockInit.STONE_BRICK_GOTHIC, GothicWindow::new),
                register(material, "arrow_slit", BlockInit.STONE_BRICK_ARROW_SLIT, ArrowSill::new),
                register(material, "parapet", BlockInit.ANDESITE_PARAPET, Parapet::new),
                register(material, "louvered_shutter", BlockInit.ANDESITE_LOUVERED_SHUTTER, Shutter::new));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
            output.accept(button);
            output.accept(plate);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(output -> building.forEach(output::accept));
    }

    private static Item register(String material, String form, Block reference,
                                 Function<BlockBehaviour.Properties, Block> factory) {
        var id = Identifier.fromNamespaceAndPath("architectural_material_closure", material + "_" + form);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = Registry.register(BuiltInRegistries.BLOCK, key,
                factory.apply(BlockBehaviour.Properties.ofFullCopy(reference).setId(key)));
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block,
                new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }
}

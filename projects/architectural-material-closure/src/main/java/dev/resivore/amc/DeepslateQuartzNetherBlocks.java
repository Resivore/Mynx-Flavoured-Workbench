package dev.resivore.amc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import com.mcwpaths.kikoz.init.BlockInit;
import com.mcwpaths.kikoz.objects.EngravedBlock;
import com.mcwpaths.kikoz.objects.FacingPathBlock;
import com.mcwpaths.kikoz.objects.PathBlock;
import net.kikoz.mcwwindows.objects.ConnectedWindow;
import net.kikoz.mcwwindows.objects.Window;
import net.kikoz.mcwwindows.objects.WindowBarred;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DeepslateQuartzNetherBlocks implements ModInitializer {
    @Override
    public void onInitialize() {
        registerFamily("deepslate");
        registerFamily("quartz");
        registerFamily("nether_bricks");
    }

    private static void registerFamily(String material) {
        var button = register(material, "button", Blocks.STONE_BUTTON,
                props -> new ButtonBlock(BlockSetType.STONE, 20, props));
        var plate = register(material, "pressure_plate", Blocks.STONE_PRESSURE_PLATE,
                props -> new PressurePlateBlock(BlockSetType.STONE, props));
        var building = new ArrayList<Item>();
        if (material.equals("nether_bricks")) {
            building.add(register(material, "window", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_WINDOW, ConnectedWindow::new));
            building.add(register(material, "window2", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_WINDOW2, WindowBarred::new));
            building.add(register(material, "four_window", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_FOUR_WINDOW, WindowBarred::new));
            building.add(register(material, "pane_window", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_PANE_WINDOW, Window::new));
        } else {
            building.add(register(material, "gothic_window", net.kikoz.mcwwindows.init.BlockInit.STONE_BRICK_GOTHIC, GothicWindow::new));
            building.add(register(material, "arrow_slit", net.kikoz.mcwwindows.init.BlockInit.STONE_BRICK_ARROW_SLIT, ArrowSill::new));
        }
        building.add(register(material, "parapet", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_PARAPET, Parapet::new));
        building.add(register(material, "louvered_shutter", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_LOUVERED_SHUTTER, Shutter::new));
        if (!material.equals("deepslate")) registerPaths(material, building);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
            output.accept(button);
            output.accept(plate);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(output -> building.forEach(output::accept));
    }

    private static void registerPaths(String material, List<Item> paths) {
        for (var ref : List.of(BlockInit.DARK_PRISMARINE_RUNNING_BOND_PATH, BlockInit.DARK_PRISMARINE_STREWN_ROCKY_PATH,
                BlockInit.DARK_PRISMARINE_WINDMILL_WEAVE_PATH, BlockInit.DARK_PRISMARINE_FLAGSTONE_PATH,
                BlockInit.DARK_PRISMARINE_CRYSTAL_FLOOR_PATH, BlockInit.DARK_PRISMARINE_DIAMOND_PAVING,
                BlockInit.DARK_PRISMARINE_BASKET_WEAVE_PAVING, BlockInit.DARK_PRISMARINE_SQUARE_PAVING,
                BlockInit.DARK_PRISMARINE_HONEYCOMB_PAVING, BlockInit.DARK_PRISMARINE_CLOVER_PAVING)) {
            paths.add(registerPath(material, ref, PathBlock::new));
        }
        paths.add(registerPath(material, BlockInit.DARK_PRISMARINE_DUMBLE_PAVING, FacingPathBlock::new));
        for (var ref : List.of(BlockInit.DARK_PRISMARINE_RUNNING_BOND, BlockInit.DARK_PRISMARINE_FLAGSTONE,
                BlockInit.DARK_PRISMARINE_WINDMILL_WEAVE, BlockInit.DARK_PRISMARINE_CRYSTAL_FLOOR)) {
            paths.add(registerPath(material, ref, EngravedBlock::new));
        }
    }

    private static Item registerPath(String material, Block reference, Function<BlockBehaviour.Properties, Block> factory) {
        String form = BuiltInRegistries.BLOCK.getKey(reference).getPath().substring("dark_prismarine_".length());
        return register(material, form, reference, factory);
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

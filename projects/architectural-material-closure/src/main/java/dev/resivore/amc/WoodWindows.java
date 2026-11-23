package dev.resivore.amc;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.kikoz.mcwwindows.objects.ConnectedWindow;
import net.kikoz.mcwwindows.objects.Window;
import net.kikoz.mcwwindows.objects.WindowBarred;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.kikoz.mcwwindows.init.BlockInit;
import java.util.function.Function;

final class WoodWindows {
    private static final String AMC = "architectural_material_closure";

    static void register(String wood) {
        finish(wood, BlockInit.OAK_WINDOW, BlockInit.OAK_WINDOW2, BlockInit.OAK_FOUR_WINDOW, BlockInit.OAK_PANE_WINDOW);
        finish("stripped_" + wood, BlockInit.STRIPPED_OAK_LOG_WINDOW, BlockInit.STRIPPED_OAK_LOG_WINDOW2,
                BlockInit.STRIPPED_OAK_LOG_FOUR_WINDOW, BlockInit.STRIPPED_OAK_PANE_WINDOW);
        finish(wood + "_plank", BlockInit.OAK_PLANK_WINDOW, BlockInit.OAK_PLANK_WINDOW2,
                BlockInit.OAK_PLANK_FOUR_WINDOW, BlockInit.OAK_PLANK_PANE_WINDOW);
    }

    private static void finish(String prefix, Block window, Block barred, Block four, Block pane) {
        register(prefix + "_window", window, ConnectedWindow::new);
        register(prefix + "_window2", barred, WindowBarred::new);
        register(prefix + "_four_window", four, WindowBarred::new);
        register(prefix + "_pane_window", pane, Window::new);
    }

    private static void register(String name, Block oak, Function<BlockBehaviour.Properties, Block> create) {
        var id = Identifier.fromNamespaceAndPath(AMC, name);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = Registry.register(BuiltInRegistries.BLOCK, key, create.apply(BlockBehaviour.Properties.ofFullCopy(oak).setId(key)));
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        var item = Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out -> out.accept(item));
    }
}


package dev.resivore.amc;

import com.starfish_studios.bbb.block.ColumnBlock;
import com.starfish_studios.bbb.block.FrameBlock;
import com.starfish_studios.bbb.block.MouldingBlock;
import com.starfish_studios.bbb.block.StoneFenceBlock;
import com.starfish_studios.bbb.block.UrnBlock;
import com.starfish_studios.bbb.item.DescriptionBlockItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import com.mcwpaths.kikoz.init.BlockInit;
import com.mcwpaths.kikoz.objects.EngravedBlock;
import com.mcwpaths.kikoz.objects.FacingPathBlock;
import com.mcwpaths.kikoz.objects.PathBlock;
import net.kikoz.mcwwindows.objects.ArrowSill;
import net.kikoz.mcwwindows.objects.GothicWindow;
import net.kikoz.mcwwindows.objects.ConnectedWindow;
import net.kikoz.mcwwindows.objects.Parapet;
import net.kikoz.mcwwindows.objects.Shutter;
import net.kikoz.mcwwindows.objects.Window;
import net.kikoz.mcwwindows.objects.WindowBarred;
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
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class BaseStoneFamilies implements ModInitializer {
    @Override
    public void onInitialize() {
        registerFamily("tuff", Blocks.TUFF);
        registerFamily("cinnabar", Blocks.CINNABAR);
        registerFamily("sulfur", Blocks.SULFUR);
        registerFamily("smooth_basalt", Blocks.SMOOTH_BASALT);
    }

    private static void registerFamily(String material, Block base) {
        var button = register(material, "button", Blocks.STONE_BUTTON,
                p -> new ButtonBlock(BlockSetType.STONE, 20, p), BlockItem::new);
        var plate = register(material, "pressure_plate", Blocks.STONE_PRESSURE_PLATE,
                p -> new PressurePlateBlock(BlockSetType.STONE, p), BlockItem::new);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
            output.accept(button);
            output.accept(plate);
        });
        var building = new ArrayList<>(List.of(
                register(material, "gothic_window", net.kikoz.mcwwindows.init.BlockInit.STONE_BRICK_GOTHIC, GothicWindow::new, BlockItem::new),
                register(material, "arrow_slit", net.kikoz.mcwwindows.init.BlockInit.STONE_BRICK_ARROW_SLIT, ArrowSill::new, BlockItem::new),
                register(material, "window", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_WINDOW, ConnectedWindow::new, BlockItem::new),
                register(material, "window2", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_WINDOW2, WindowBarred::new, BlockItem::new),
                register(material, "four_window", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_FOUR_WINDOW, WindowBarred::new, BlockItem::new),
                register(material, "pane_window", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_PANE_WINDOW, Window::new, BlockItem::new),
                register(material, "parapet", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_PARAPET, Parapet::new, BlockItem::new),
                register(material, "louvered_shutter", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_LOUVERED_SHUTTER, Shutter::new, BlockItem::new),
                register(material, "column", base, p -> new ColumnBlock(p.noOcclusion()), DescriptionBlockItem::new),
                register(material, "urn", base, p -> new UrnBlock(p.noOcclusion().pushReaction(PushReaction.DESTROY)),
                        DescriptionBlockItem::new),
                register(material, "moulding", base, p -> new MouldingBlock(base.defaultBlockState(), p.noOcclusion()),
                        DescriptionBlockItem::new),
                register(material, "fence", base, p -> new StoneFenceBlock(p.noOcclusion()), DescriptionBlockItem::new),
                register(material, "frame", base, p -> new FrameBlock(p.noOcclusion().noCollision()
                        .pushReaction(PushReaction.DESTROY)), DescriptionBlockItem::new)));
        for (var ref : List.of(BlockInit.DARK_PRISMARINE_RUNNING_BOND_PATH, BlockInit.DARK_PRISMARINE_STREWN_ROCKY_PATH,
                BlockInit.DARK_PRISMARINE_WINDMILL_WEAVE_PATH, BlockInit.DARK_PRISMARINE_FLAGSTONE_PATH,
                BlockInit.DARK_PRISMARINE_CRYSTAL_FLOOR_PATH, BlockInit.DARK_PRISMARINE_DIAMOND_PAVING,
                BlockInit.DARK_PRISMARINE_BASKET_WEAVE_PAVING, BlockInit.DARK_PRISMARINE_SQUARE_PAVING,
                BlockInit.DARK_PRISMARINE_HONEYCOMB_PAVING, BlockInit.DARK_PRISMARINE_CLOVER_PAVING)) {
            building.add(registerPath(material, ref, PathBlock::new));
        }
        building.add(registerPath(material, BlockInit.DARK_PRISMARINE_DUMBLE_PAVING, FacingPathBlock::new));
        for (var ref : List.of(BlockInit.DARK_PRISMARINE_RUNNING_BOND, BlockInit.DARK_PRISMARINE_FLAGSTONE,
                BlockInit.DARK_PRISMARINE_WINDMILL_WEAVE, BlockInit.DARK_PRISMARINE_CRYSTAL_FLOOR)) {
            building.add(registerPath(material, ref, EngravedBlock::new));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(output -> building.forEach(output::accept));
    }

    private static Item registerPath(String material, Block ref, Function<BlockBehaviour.Properties, Block> factory) {
        String form = BuiltInRegistries.BLOCK.getKey(ref).getPath().substring("dark_prismarine_".length());
        return register(material, form, ref, factory, BlockItem::new);
    }

    private static Item register(String material, String form, Block reference,
                                 Function<BlockBehaviour.Properties, Block> factory,
                                 BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        var id = Identifier.fromNamespaceAndPath("architectural_material_closure", material + "_" + form);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = Registry.register(BuiltInRegistries.BLOCK, key,
                factory.apply(BlockBehaviour.Properties.ofFullCopy(reference).setId(key)));
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, itemKey, itemFactory.apply(block,
                new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }
}

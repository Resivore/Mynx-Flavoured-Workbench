package dev.resivore.amc;

import com.starfish_studios.bbb.block.ColumnBlock;
import com.starfish_studios.bbb.block.FrameBlock;
import com.starfish_studios.bbb.block.MouldingBlock;
import com.starfish_studios.bbb.block.StoneFenceBlock;
import com.starfish_studios.bbb.block.UrnBlock;
import com.starfish_studios.bbb.item.DescriptionBlockItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.kikoz.mcwwindows.init.BlockInit;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class MudBricksBlocks implements ModInitializer {
    @Override
    public void onInitialize() {
        var base = Blocks.PACKED_MUD;
        var button = register("button", Blocks.STONE_BUTTON,
                props -> new ButtonBlock(BlockSetType.STONE, 20, props), BlockItem::new);
        var plate = register("pressure_plate", Blocks.STONE_PRESSURE_PLATE,
                props -> new PressurePlateBlock(BlockSetType.STONE, props), BlockItem::new);
        var building = List.of(
                register("window", BlockInit.ANDESITE_WINDOW, ConnectedWindow::new, BlockItem::new),
                register("window2", BlockInit.ANDESITE_WINDOW2, WindowBarred::new, BlockItem::new),
                register("four_window", BlockInit.ANDESITE_FOUR_WINDOW, WindowBarred::new, BlockItem::new),
                register("pane_window", BlockInit.ANDESITE_PANE_WINDOW, Window::new, BlockItem::new),
                register("parapet", BlockInit.ANDESITE_PARAPET, Parapet::new, BlockItem::new),
                register("louvered_shutter", BlockInit.ANDESITE_LOUVERED_SHUTTER, Shutter::new, BlockItem::new),
                register("column", base, p -> new ColumnBlock(p.noOcclusion()), DescriptionBlockItem::new),
                register("urn", base, p -> new UrnBlock(p.noOcclusion().pushReaction(PushReaction.DESTROY)),
                        DescriptionBlockItem::new),
                register("moulding", base, p -> new MouldingBlock(base.defaultBlockState(), p.noOcclusion()),
                        DescriptionBlockItem::new),
                register("fence", base, p -> new StoneFenceBlock(p.noOcclusion()), DescriptionBlockItem::new),
                register("frame", base, p -> new FrameBlock(p.noOcclusion().noCollision()
                        .pushReaction(PushReaction.DESTROY)), DescriptionBlockItem::new));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
            output.accept(button);
            output.accept(plate);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(output -> building.forEach(output::accept));
    }

    private static Item register(String form, Block reference,
                                 Function<BlockBehaviour.Properties, Block> factory,
                                 BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        var id = Identifier.fromNamespaceAndPath("architectural_material_closure", "mud_bricks_" + form);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = Registry.register(BuiltInRegistries.BLOCK, key,
                factory.apply(BlockBehaviour.Properties.ofFullCopy(reference).setId(key)));
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, itemKey, itemFactory.apply(block,
                new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }
}

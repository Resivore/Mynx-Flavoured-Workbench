package dev.resivore.amc;

import com.mcwpaths.kikoz.init.BlockInit;
import com.mcwpaths.kikoz.objects.EngravedBlock;
import com.mcwpaths.kikoz.objects.FacingPathBlock;
import com.mcwpaths.kikoz.objects.PathBlock;
import com.starfish_studios.bbb.block.ColumnBlock;
import com.starfish_studios.bbb.block.FrameBlock;
import com.starfish_studios.bbb.block.MouldingBlock;
import com.starfish_studios.bbb.block.StoneFenceBlock;
import com.starfish_studios.bbb.block.UrnBlock;
import com.starfish_studios.bbb.item.DescriptionBlockItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.kikoz.mcwwindows.objects.ArrowSill;
import net.kikoz.mcwwindows.objects.ConnectedWindow;
import net.kikoz.mcwwindows.objects.GothicWindow;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Registered at the return of the optional provider's initializer, before registry freeze. */
public record EnderscapeFamilies(String material, Block base) {
    private static final String AMC = "architectural_material_closure";

    public static void registerAfterProvider() {
        var loader = FabricLoader.getInstance();
        if (!loader.isModLoaded("enderscape")) return;
        var families = new ArrayList<EnderscapeFamilies>();
        for (String name : List.of("veradite", "mirestone", "kurodite")) {
            var base = requireBlockItem(name);
            requireBlockItem(name + "_slab");
            requireBlockItem("polished_" + name + "_button");
            requireBlockItem("polished_" + name + "_pressure_plate");
            families.add(new EnderscapeFamilies(name, base));
        }
        var container = loader.getModContainer(AMC).orElseThrow();
        if (!ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(AMC, "enderscape"),
                container, PackActivationType.ALWAYS_ENABLED)) {
            throw new IllegalStateException("Missing AMC Enderscape resource pack");
        }
        families.forEach(EnderscapeFamilies::registerForms);
    }

    private static Block requireBlockItem(String path) {
        var id = Identifier.fromNamespaceAndPath("enderscape", path);
        var block = BuiltInRegistries.BLOCK.getOptional(id).orElseThrow();
        if (!(BuiltInRegistries.ITEM.getOptional(id).orElseThrow() instanceof BlockItem item)
                || item.getBlock() != block || block.asItem() != item) {
            throw new IllegalStateException("Invalid Enderscape block item " + id);
        }
        return block;
    }

    private void registerForms() {
        var building = new ArrayList<>(List.of(
                register("gothic_window", net.kikoz.mcwwindows.init.BlockInit.STONE_BRICK_GOTHIC, GothicWindow::new, BlockItem::new),
                register("arrow_slit", net.kikoz.mcwwindows.init.BlockInit.STONE_BRICK_ARROW_SLIT, ArrowSill::new, BlockItem::new),
                register("window", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_WINDOW, ConnectedWindow::new, BlockItem::new),
                register("window2", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_WINDOW2, WindowBarred::new, BlockItem::new),
                register("four_window", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_FOUR_WINDOW, WindowBarred::new, BlockItem::new),
                register("pane_window", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_PANE_WINDOW, Window::new, BlockItem::new),
                register("parapet", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_PARAPET, Parapet::new, BlockItem::new),
                register("louvered_shutter", net.kikoz.mcwwindows.init.BlockInit.ANDESITE_LOUVERED_SHUTTER, Shutter::new, BlockItem::new),
                register("column", base, p -> new ColumnBlock(p.noOcclusion()), DescriptionBlockItem::new),
                register("urn", base, p -> new UrnBlock(p.noOcclusion().pushReaction(PushReaction.DESTROY)),
                        DescriptionBlockItem::new),
                register("moulding", base, p -> new MouldingBlock(base.defaultBlockState(), p.noOcclusion()),
                        DescriptionBlockItem::new),
                register("fence", base, p -> new StoneFenceBlock(p.noOcclusion()), DescriptionBlockItem::new),
                register("frame", base, p -> new FrameBlock(p.noOcclusion().noCollision()
                        .pushReaction(PushReaction.DESTROY)), DescriptionBlockItem::new)));
        for (var ref : List.of(BlockInit.DARK_PRISMARINE_RUNNING_BOND_PATH, BlockInit.DARK_PRISMARINE_STREWN_ROCKY_PATH,
                BlockInit.DARK_PRISMARINE_WINDMILL_WEAVE_PATH, BlockInit.DARK_PRISMARINE_FLAGSTONE_PATH,
                BlockInit.DARK_PRISMARINE_CRYSTAL_FLOOR_PATH, BlockInit.DARK_PRISMARINE_DIAMOND_PAVING,
                BlockInit.DARK_PRISMARINE_BASKET_WEAVE_PAVING, BlockInit.DARK_PRISMARINE_SQUARE_PAVING,
                BlockInit.DARK_PRISMARINE_HONEYCOMB_PAVING, BlockInit.DARK_PRISMARINE_CLOVER_PAVING)) {
            building.add(registerPath(ref, PathBlock::new));
        }
        building.add(registerPath(BlockInit.DARK_PRISMARINE_DUMBLE_PAVING, FacingPathBlock::new));
        for (var ref : List.of(BlockInit.DARK_PRISMARINE_RUNNING_BOND, BlockInit.DARK_PRISMARINE_FLAGSTONE,
                BlockInit.DARK_PRISMARINE_WINDMILL_WEAVE, BlockInit.DARK_PRISMARINE_CRYSTAL_FLOOR)) {
            building.add(registerPath(ref, EngravedBlock::new));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(output -> building.forEach(output::accept));
    }

    private Item registerPath(Block ref, Function<BlockBehaviour.Properties, Block> factory) {
        String form = BuiltInRegistries.BLOCK.getKey(ref).getPath().substring("dark_prismarine_".length());
        return register(form, ref, factory, BlockItem::new);
    }

    private Item register(String form, Block reference,
                          Function<BlockBehaviour.Properties, Block> factory,
                          BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        var id = Identifier.fromNamespaceAndPath(AMC, material + "_" + form);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var properties = BlockBehaviour.Properties.ofFullCopy(reference).setId(key)
                .strength(base.defaultDestroyTime(), base.getExplosionResistance())
                .sound(base.defaultBlockState().getSoundType());
        if (base.defaultBlockState().requiresCorrectToolForDrops()) properties.requiresCorrectToolForDrops();
        var block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties));
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, itemKey, itemFactory.apply(block,
                new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }
}

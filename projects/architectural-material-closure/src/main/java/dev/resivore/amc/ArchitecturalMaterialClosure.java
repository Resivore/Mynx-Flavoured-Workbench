package dev.resivore.amc;

import com.mcwpaths.kikoz.objects.PathBlock;
import com.starfish_studios.bbb.block.ColumnBlock;
import com.starfish_studios.bbb.block.FrameBlock;
import com.starfish_studios.bbb.block.MouldingBlock;
import com.starfish_studios.bbb.block.StoneFenceBlock;
import com.starfish_studios.bbb.block.UrnBlock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.kikoz.mcwwindows.objects.ArrowSill;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * C3's local-only material registry. The matching BBB and Macaw blockstates,
 * models and texture treatment are generated from the owner's installed JARs
 * into ignored build inputs; this tracked code contains only the safe mapping
 * and uses the providers' stateful block classes directly.
 */
public final class ArchitecturalMaterialClosure implements ModInitializer {
    public static final String MOD_ID = "architectural_material_closure";

    private static final List<MaterialSpec> MATERIALS = List.of(
            material("stone"), material("andesite"), material("diorite"), material("granite"),
            material("brick"), material("mossy_stone_brick"), material("cobbled_deepslate"),
            material("deepslate"), material("mud_brick"), material("blackstone"), material("prismarine"),
            material("dark_prismarine"), material("sandstone"), material("red_sandstone"), material("quartz"),
            material("nether_brick"), material("end_brick")
    );

    private static final List<String> BBB_FORMS = List.of("column", "urn", "moulding", "fence", "frame");
    private static final List<String> PATH_FORMS = List.of(
            "running_bond_path", "strewn_rocky_path", "windmill_weave_path", "flagstone_path", "crystal_floor_path",
            "diamond_paving", "basket_weave_paving", "square_paving", "honeycomb_paving", "clover_paving", "dumble_paving");
    private static final List<String> WINDOW_FORMS = List.of(
            "window", "window2", "four_window", "pane_window", "parapet", "gothic", "arrow_slit", "louvered_shutter");

    private static final List<Identifier> OWNED_IDS = MATERIALS.stream()
            .flatMap(material -> allForms().stream().map(form -> id(material.id() + "_" + form)))
            .toList();
    private static Map<Identifier, Block> blocks;

    @Override
    public void onInitialize() {
        blocks().forEach(ArchitecturalMaterialClosure::register);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
                blocks().values().stream().map(Block::asItem).map(Item::getDefaultInstance).forEach(output::accept));
    }

    public static List<Identifier> ownedIds() {
        return OWNED_IDS;
    }

    private static List<String> allForms() {
        List<String> forms = new ArrayList<>(BBB_FORMS.size() + PATH_FORMS.size() + WINDOW_FORMS.size());
        forms.addAll(BBB_FORMS);
        forms.addAll(PATH_FORMS);
        forms.addAll(WINDOW_FORMS);
        return List.copyOf(forms);
    }

    private static synchronized Map<Identifier, Block> blocks() {
        if (blocks == null) blocks = createBlocks();
        return blocks;
    }

    private static Map<Identifier, Block> createBlocks() {
        Map<Identifier, Block> blocks = new LinkedHashMap<>();
        for (MaterialSpec material : MATERIALS) {
            add(blocks, material, "column", new ColumnBlock(properties(material, "column").noOcclusion()));
            add(blocks, material, "urn", new UrnBlock(properties(material, "urn").noOcclusion()));
            add(blocks, material, "moulding", new MouldingBlock(source(material).defaultBlockState(), properties(material, "moulding").noOcclusion()));
            add(blocks, material, "fence", new StoneFenceBlock(properties(material, "fence").noOcclusion()));
            add(blocks, material, "frame", new FrameBlock(properties(material, "frame").noOcclusion().noCollision()));

            for (String form : PATH_FORMS) {
                add(blocks, material, form, new PathBlock(properties(material, form).noOcclusion()));
            }
            add(blocks, material, "window", new Window(properties(material, "window").noOcclusion()));
            add(blocks, material, "window2", new WindowBarred(properties(material, "window2").noOcclusion()));
            add(blocks, material, "four_window", new WindowBarred(properties(material, "four_window").noOcclusion()));
            add(blocks, material, "pane_window", new Window(properties(material, "pane_window").noOcclusion()));
            add(blocks, material, "parapet", new Parapet(properties(material, "parapet").noOcclusion()));
            add(blocks, material, "gothic", new GothicWindow(properties(material, "gothic").noOcclusion()));
            add(blocks, material, "arrow_slit", new ArrowSill(properties(material, "arrow_slit").noOcclusion()));
            add(blocks, material, "louvered_shutter", new Shutter(properties(material, "louvered_shutter").noOcclusion()));
        }
        return Map.copyOf(blocks);
    }

    private static void add(Map<Identifier, Block> blocks, MaterialSpec material, String form, Block block) {
        Identifier id = id(material.id() + "_" + form);
        if (blocks.put(id, block) != null) {
            throw new IllegalStateException("Duplicate AMC C3 block id " + id);
        }
    }

    private static BlockBehaviour.Properties properties(MaterialSpec material, String form) {
        Identifier id = id(material.id() + "_" + form);
        return BlockBehaviour.Properties.ofFullCopy(source(material))
                .setId(ResourceKey.create(Registries.BLOCK, id));
    }

    private static void register(Identifier id, Block block) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }

    private static MaterialSpec material(String id) {
        return new MaterialSpec(id);
    }

    private static Block source(MaterialSpec material) {
        return switch (material.id()) {
            case "stone" -> Blocks.STONE;
            case "andesite" -> Blocks.ANDESITE;
            case "diorite" -> Blocks.DIORITE;
            case "granite" -> Blocks.GRANITE;
            case "brick" -> Blocks.BRICKS;
            case "mossy_stone_brick" -> Blocks.MOSSY_STONE_BRICKS;
            case "cobbled_deepslate" -> Blocks.COBBLED_DEEPSLATE;
            case "deepslate" -> Blocks.DEEPSLATE;
            case "mud_brick" -> Blocks.MUD_BRICKS;
            case "blackstone" -> Blocks.POLISHED_BLACKSTONE;
            case "prismarine" -> Blocks.PRISMARINE_BRICKS;
            case "dark_prismarine" -> Blocks.DARK_PRISMARINE;
            case "sandstone" -> Blocks.SANDSTONE;
            case "red_sandstone" -> Blocks.RED_SANDSTONE;
            case "quartz" -> Blocks.QUARTZ_BLOCK;
            case "nether_brick" -> Blocks.NETHER_BRICKS;
            case "end_brick" -> Blocks.END_STONE_BRICKS;
            default -> throw new IllegalArgumentException("Unknown AMC C3 material " + material.id());
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private record MaterialSpec(String id) {
    }
}

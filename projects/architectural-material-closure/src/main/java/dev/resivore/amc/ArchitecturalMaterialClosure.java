package dev.resivore.amc;

import com.starfish_studios.bbb.block.ColumnBlock;
import com.starfish_studios.bbb.block.FrameBlock;
import com.starfish_studios.bbb.block.MouldingBlock;
import com.starfish_studios.bbb.block.StoneFenceBlock;
import com.starfish_studios.bbb.block.UrnBlock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * C2's local-only masonry-detail materialization. The literal IDs are stable,
 * while the matching BBB resource topology is derived at build time from the
 * exact private reference artifact and is never tracked or published.
 */
public final class ArchitecturalMaterialClosure implements ModInitializer {
    public static final String MOD_ID = "architectural_material_closure";

    private static final List<MaterialSpec> MATERIALS = List.of(
            new MaterialSpec("andesite", "Andesite"),
            new MaterialSpec("diorite", "Diorite"),
            new MaterialSpec("granite", "Granite"),
            new MaterialSpec("brick", "Brick"),
            new MaterialSpec("mossy_stone_brick", "Mossy Stone Brick"),
            new MaterialSpec("cobbled_deepslate", "Cobbled Deepslate"),
            new MaterialSpec("mud_brick", "Mud Brick"),
            new MaterialSpec("dark_prismarine", "Dark Prismarine")
    );

    private static final List<Identifier> OWNED_IDS = MATERIALS.stream()
            .flatMap(material -> List.of(
                    id(material.id() + "_column"),
                    id(material.id() + "_urn"),
                    id(material.id() + "_moulding"),
                    id(material.id() + "_fence"),
                    id(material.id() + "_frame")).stream())
            .toList();
    private static List<Family> families;

    @Override
    public void onInitialize() {
        for (Family family : families()) {
            register(family.columnId(), family.column());
            register(family.urnId(), family.urn());
            register(family.mouldingId(), family.moulding());
            register(family.fenceId(), family.fence());
            register(family.frameId(), family.frame());
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
                ownedItems().stream().map(Item::getDefaultInstance).forEach(output::accept));
    }

    public static List<Identifier> ownedIds() {
        return OWNED_IDS;
    }

    static List<Item> ownedItems() {
        return families().stream().flatMap(family -> family.blocks().stream()).map(Block::asItem).toList();
    }

    private static synchronized List<Family> families() {
        if (families == null) {
            families = createFamilies();
        }
        return families;
    }

    private static List<Family> createFamilies() {
        List<Family> families = new ArrayList<>();
        for (MaterialSpec material : MATERIALS) {
            Identifier columnId = id(material.id() + "_column");
            Identifier urnId = id(material.id() + "_urn");
            Identifier mouldingId = id(material.id() + "_moulding");
            Identifier fenceId = id(material.id() + "_fence");
            Identifier frameId = id(material.id() + "_frame");
            families.add(new Family(
                    material.id(),
                    columnId, new ColumnBlock(properties(material, columnId).noOcclusion()),
                    urnId, new UrnBlock(properties(material, urnId).noOcclusion()),
                    mouldingId, new MouldingBlock(copySource(material.id()).defaultBlockState(),
                            properties(material, mouldingId).noOcclusion()),
                    fenceId, new StoneFenceBlock(properties(material, fenceId).noOcclusion()),
                    frameId, new FrameBlock(properties(material, frameId).noOcclusion().noCollision())
            ));
        }
        return List.copyOf(families);
    }

    private static BlockBehaviour.Properties properties(MaterialSpec material, Identifier id) {
        return BlockBehaviour.Properties.ofFullCopy(copySource(material.id()))
                .setId(ResourceKey.create(Registries.BLOCK, id));
    }

    private static Block copySource(String material) {
        return switch (material) {
            case "andesite" -> Blocks.ANDESITE;
            case "diorite" -> Blocks.DIORITE;
            case "granite" -> Blocks.GRANITE;
            case "brick" -> Blocks.BRICKS;
            case "mossy_stone_brick" -> Blocks.MOSSY_STONE_BRICKS;
            case "cobbled_deepslate" -> Blocks.COBBLED_DEEPSLATE;
            case "mud_brick" -> Blocks.MUD_BRICKS;
            case "dark_prismarine" -> Blocks.DARK_PRISMARINE;
            default -> throw new IllegalArgumentException("Unknown C1 material: " + material);
        };
    }

    private static void register(Identifier id, Block block) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private record MaterialSpec(String id, String displayName) {
    }

    public record Family(
            String material,
            Identifier columnId, Block column,
            Identifier urnId, Block urn,
            Identifier mouldingId, Block moulding,
            Identifier fenceId, StoneFenceBlock fence,
            Identifier frameId, FrameBlock frame
    ) {
        public List<Identifier> ids() {
            return List.of(columnId, urnId, mouldingId, fenceId, frameId);
        }

        public List<Block> blocks() {
            return List.of(column, urn, moulding, fence, frame);
        }
    }
}

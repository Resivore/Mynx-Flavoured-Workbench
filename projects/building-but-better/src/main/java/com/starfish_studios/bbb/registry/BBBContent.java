package com.starfish_studios.bbb.registry;

import com.starfish_studios.bbb.BuildingButBetter;
import com.starfish_studios.bbb.block.BalustradeBlock;
import com.starfish_studios.bbb.block.BrazierBlock;
import com.starfish_studios.bbb.block.ColumnBlock;
import com.starfish_studios.bbb.block.FacingConnectingBlock;
import com.starfish_studios.bbb.block.FacingSlabBlock;
import com.starfish_studios.bbb.block.FrameBlock;
import com.starfish_studios.bbb.block.HammerableBlock;
import com.starfish_studios.bbb.block.IronFenceBlock;
import com.starfish_studios.bbb.block.LatticeBlock;
import com.starfish_studios.bbb.block.MouldingBlock;
import com.starfish_studios.bbb.block.PalletBlock;
import com.starfish_studios.bbb.block.StoneFenceBlock;
import com.starfish_studios.bbb.block.SupportBlock;
import com.starfish_studios.bbb.block.UrnBlock;
import com.starfish_studios.bbb.block.WoodenLanternBlock;
import com.starfish_studios.bbb.block.WoodenWallBlock;
import com.starfish_studios.bbb.item.HammerItem;
import com.starfish_studios.bbb.item.DescriptionBlockItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Direct Fabric registry for the curated 2.0pre4 preservation scope.
 *
 * <p>The family records are deliberately authoritative. Compatibility code can
 * consume their source material and geometry references without parsing registry
 * names or guessing relationships.</p>
 */
public final class BBBContent {
    public static final List<String> WOOD_FORMS = List.of(
            "balustrade", "lattice", "wall", "beam", "beam_stairs", "beam_slab",
            "support", "pallet", "frame", "lantern", "trim"
    );
    public static final List<String> STONE_FORMS = List.of(
            "column", "urn", "moulding", "fence", "frame"
    );

    private static final List<WoodSpec> WOOD_SPECS = List.of(
            new WoodSpec("oak", Blocks.OAK_PLANKS),
            new WoodSpec("spruce", Blocks.SPRUCE_PLANKS),
            new WoodSpec("birch", Blocks.BIRCH_PLANKS),
            new WoodSpec("jungle", Blocks.JUNGLE_PLANKS),
            new WoodSpec("acacia", Blocks.ACACIA_PLANKS),
            new WoodSpec("dark_oak", Blocks.DARK_OAK_PLANKS),
            new WoodSpec("crimson", Blocks.CRIMSON_PLANKS),
            new WoodSpec("warped", Blocks.WARPED_PLANKS),
            new WoodSpec("mangrove", Blocks.MANGROVE_PLANKS),
            new WoodSpec("bamboo", Blocks.BAMBOO_PLANKS),
            new WoodSpec("cherry", Blocks.CHERRY_PLANKS),
            // Pale Oak is a port-native 26.2 extension, not an inferred registry variant.
            // Its beam properties deliberately use the native stripped-log material.
            new WoodSpec("pale_oak", Blocks.PALE_OAK_PLANKS, Blocks.STRIPPED_PALE_OAK_LOG)
    );

    private static final List<StoneSpec> STONE_SPECS = List.of(
            new StoneSpec("stone", Blocks.STONE, Blocks.STONE_BRICKS),
            new StoneSpec("blackstone", Blocks.BLACKSTONE, Blocks.BLACKSTONE),
            new StoneSpec("deepslate", Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS),
            new StoneSpec("nether_brick", Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS),
            new StoneSpec("sandstone", Blocks.SANDSTONE, Blocks.SANDSTONE),
            new StoneSpec("red_sandstone", Blocks.RED_SANDSTONE, Blocks.RED_SANDSTONE),
            new StoneSpec("quartz", Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK)
    );

    private static final LinkedHashMap<String, Block> MUTABLE_BLOCKS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Item> MUTABLE_ITEMS = new LinkedHashMap<>();
    private static final List<WoodFamily> MUTABLE_WOOD_FAMILIES = new ArrayList<>();
    private static final List<StoneFamily> MUTABLE_STONE_FAMILIES = new ArrayList<>();
    private static final List<BeamFamily> MUTABLE_BEAM_FAMILIES = new ArrayList<>();
    private static final List<Block> MUTABLE_LATTICES = new ArrayList<>();

    public static final Map<String, Block> BLOCKS = Collections.unmodifiableMap(MUTABLE_BLOCKS);
    public static final Map<String, Item> ITEMS = Collections.unmodifiableMap(MUTABLE_ITEMS);
    public static final List<WoodFamily> WOOD_FAMILIES = Collections.unmodifiableList(MUTABLE_WOOD_FAMILIES);
    public static final List<StoneFamily> STONE_FAMILIES = Collections.unmodifiableList(MUTABLE_STONE_FAMILIES);
    public static final List<BeamFamily> BEAM_FAMILIES = Collections.unmodifiableList(MUTABLE_BEAM_FAMILIES);
    public static final List<Block> LATTICES = Collections.unmodifiableList(MUTABLE_LATTICES);

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, id("tab")
    );
    public static final TagKey<Item> HAMMERS = TagKey.create(Registries.ITEM, id("hammers"));
    public static final TagKey<Item> FRAME_ITEMS = TagKey.create(Registries.ITEM, id("frames"));
    public static final TagKey<Block> WOODEN_FRAMES = TagKey.create(Registries.BLOCK, id("wooden_frames"));
    public static final TagKey<Block> STONE_FRAMES = TagKey.create(Registries.BLOCK, id("stone_frames"));
    public static final TagKey<Block> FRAMES = TagKey.create(Registries.BLOCK, id("frames"));
    public static final TagKey<Block> METAL_FENCES = TagKey.create(Registries.BLOCK, id("metal_fences"));
    public static final TagKey<Block> STONE_FENCES = TagKey.create(Registries.BLOCK, id("stone_fences"));

    public static Item HAMMER;
    public static CreativeModeTab TAB;
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;

        for (WoodSpec spec : WOOD_SPECS) {
            registerWoodFamily(spec);
        }
        for (StoneSpec spec : STONE_SPECS) {
            registerStoneFamily(spec);
        }

        registerBlock("brazier", properties(Blocks.IRON_BLOCK, "brazier")
                .lightLevel(state -> state.getValue(BrazierBlock.LIT) ? 15 : 0)
                .noOcclusion().pushReaction(PushReaction.DESTROY).strength(1.0F, 1.5F), BrazierBlock::new);
        registerBlock("soul_brazier", properties(Blocks.IRON_BLOCK, "soul_brazier")
                .lightLevel(state -> state.getValue(BrazierBlock.LIT) ? 10 : 0)
                .noOcclusion().pushReaction(PushReaction.DESTROY).strength(1.0F, 1.5F), BrazierBlock::new);
        registerBlock("rope", properties(Blocks.OAK_PLANKS, "rope")
                .forceSolidOn().strength(0.1F).sound(SoundType.WOOL).noOcclusion(), ChainBlock::new);
        registerBlock("iron_fence", properties(Blocks.IRON_BARS, "iron_fence").noOcclusion(), IronFenceBlock::new);

        HAMMER = registerItem("hammer", new HammerItem(itemProperties("hammer").durability(256)));

        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY,
                FabricCreativeModeTab.builder()
                        .icon(() -> new ItemStack(HAMMER))
                        .title(Component.translatable("itemGroup.bbb.tab"))
                        .displayItems((parameters, output) -> MUTABLE_ITEMS.values().forEach(output::accept))
                        .build());

        registerHammerCallbacks();
        FuelValueEvents.BUILD.register((builder, context) -> {
            for (WoodFamily family : MUTABLE_WOOD_FAMILIES) {
                family.blocks().values().forEach(block -> builder.add(block.asItem(), 100));
            }
        });

        if (MUTABLE_BLOCKS.size() != 171 || MUTABLE_ITEMS.size() != 172) {
            throw new IllegalStateException("Curated BBB registry drift: blocks=" + MUTABLE_BLOCKS.size()
                    + ", items=" + MUTABLE_ITEMS.size());
        }
        initialized = true;
    }

    private static void registerWoodFamily(WoodSpec spec) {
        LinkedHashMap<String, Block> family = new LinkedHashMap<>();
        String material = spec.id();
        Block source = spec.planks();

        family.put("balustrade", registerBlock(material + "_balustrade",
                properties(source, material + "_balustrade").noOcclusion(), BalustradeBlock::new));

        Block lattice = registerBlock(material + "_lattice",
                properties(source, material + "_lattice").noOcclusion().pushReaction(PushReaction.DESTROY),
                LatticeBlock::new);
        family.put("lattice", lattice);
        MUTABLE_LATTICES.add(lattice);

        Block wall = registerBlock(material + "_wall", properties(source, material + "_wall"), WoodenWallBlock::new);
        family.put("wall", wall);

        Block beam = registerBlock(material + "_beam", properties(spec.beamMaterial(), material + "_beam"),
                RotatedPillarBlock::new);
        family.put("beam", beam);

        Block beamStairs = registerBlock(material + "_beam_stairs", properties(source, material + "_beam_stairs"),
                props -> new BBBStairBlock(source.defaultBlockState(), props));
        family.put("beam_stairs", beamStairs);

        Block beamSlab = registerBlock(material + "_beam_slab", properties(source, material + "_beam_slab"),
                FacingSlabBlock::new);
        family.put("beam_slab", beamSlab);

        family.put("support", registerBlock(material + "_support",
                properties(source, material + "_support").noOcclusion(), SupportBlock::new));
        family.put("pallet", registerBlock(material + "_pallet",
                properties(source, material + "_pallet").noOcclusion(), PalletBlock::new));
        family.put("frame", registerBlock(material + "_frame",
                properties(source, material + "_frame").noOcclusion().noCollision().pushReaction(PushReaction.DESTROY),
                FrameBlock::new));
        family.put("lantern", registerBlock(material + "_lantern",
                properties(source, material + "_lantern").lightLevel(state -> 15).noOcclusion()
                        .pushReaction(PushReaction.DESTROY).strength(0.3F), WoodenLanternBlock::new));
        family.put("trim", registerBlock(material + "_trim",
                properties(source, material + "_trim"), FacingConnectingBlock::new));

        WoodFamily metadata = new WoodFamily(material, source, Collections.unmodifiableMap(family));
        MUTABLE_WOOD_FAMILIES.add(metadata);
        MUTABLE_BEAM_FAMILIES.add(new BeamFamily(material, source, beam, beamSlab, beamStairs, wall));
    }

    private static void registerStoneFamily(StoneSpec spec) {
        LinkedHashMap<String, Block> family = new LinkedHashMap<>();
        String material = spec.id();

        family.put("column", registerBlock(material + "_column",
                properties(spec.masonry(), material + "_column").noOcclusion(), ColumnBlock::new));
        family.put("urn", registerBlock(material + "_urn",
                properties(spec.foundation(), material + "_urn").noOcclusion().pushReaction(PushReaction.DESTROY),
                UrnBlock::new));
        family.put("moulding", registerBlock(material + "_moulding",
                properties(spec.masonry(), material + "_moulding").noOcclusion(),
                props -> new MouldingBlock(spec.masonry().defaultBlockState(), props)));
        family.put("fence", registerBlock(material + "_fence",
                properties(spec.masonry(), material + "_fence").noOcclusion(), StoneFenceBlock::new));
        family.put("frame", registerBlock(material + "_frame",
                properties(spec.foundation(), material + "_frame").noOcclusion().noCollision()
                        .pushReaction(PushReaction.DESTROY), FrameBlock::new));

        MUTABLE_STONE_FAMILIES.add(new StoneFamily(material, spec.foundation(), spec.masonry(),
                Collections.unmodifiableMap(family)));
    }

    private static Block registerBlock(String path, BlockBehaviour.Properties properties, BlockFactory factory) {
        if (MUTABLE_BLOCKS.containsKey(path)) throw new IllegalStateException("Duplicate BBB block " + path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(path));
        Block block = factory.create(properties.setId(key));
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        MUTABLE_BLOCKS.put(path, block);

        Item.Properties itemProperties = itemProperties(path).useBlockDescriptionPrefix();
        registerItem(path, new DescriptionBlockItem(block, itemProperties));
        return block;
    }

    private static Item registerItem(String path, Item item) {
        if (MUTABLE_ITEMS.putIfAbsent(path, item) != null) {
            throw new IllegalStateException("Duplicate BBB item " + path);
        }
        Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, id(path)), item);
        return item;
    }

    private static BlockBehaviour.Properties properties(Block source, String path) {
        return BlockBehaviour.Properties.ofFullCopy(source)
                .setId(ResourceKey.create(Registries.BLOCK, id(path)));
    }

    private static Item.Properties itemProperties(String path) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(path)));
    }

    private static void registerHammerCallbacks() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            // Fabric invokes this callback before vanilla's spectator gate.
            if (player.isSpectator()) return InteractionResult.PASS;
            if (!player.getItemInHand(hand).is(HAMMERS)) return InteractionResult.PASS;
            BlockState state = level.getBlockState(hit.getBlockPos());
            if (player.isShiftKeyDown()
                    && !(state.getBlock() instanceof BalustradeBlock)
                    && !(state.getBlock() instanceof MouldingBlock)
                    && !(state.getBlock() instanceof StoneFenceBlock)) {
                return InteractionResult.PASS;
            }
            if (state.getBlock() instanceof HammerableBlock hammerable) {
                return hammerable.onHammerUse(state, level, hit.getBlockPos(), player, hand, hit);
            }
            return InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            if (!player.getItemInHand(hand).is(HAMMERS)) return InteractionResult.PASS;
            // Item.canAttackBlock was removed in 26.2; preserve the upstream
            // Hammer's creative-mode block-breaking guard after Frame.attack.
            // SUCCESS sends the client action to the server while cancelling breakage.
            if (player.isCreative()) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof FrameBlock frame) {
                    frame.resetCenter(state, level, pos);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BuildingButBetter.MOD_ID, path);
    }

    public static Block block(String path) {
        Block block = MUTABLE_BLOCKS.get(path);
        if (block == null) throw new IllegalArgumentException("Unknown curated BBB block: " + path);
        return block;
    }

    public static Item item(String path) {
        Item item = MUTABLE_ITEMS.get(path);
        if (item == null) throw new IllegalArgumentException("Unknown curated BBB item: " + path);
        return item;
    }

    public record WoodFamily(String material, Block sourcePlanks, Map<String, Block> blocks) {
        public Block form(String form) {
            Block block = blocks.get(form);
            if (block == null) throw new IllegalArgumentException("Unknown wood form " + form);
            return block;
        }
    }

    public record StoneFamily(String material, Block foundationSource, Block masonrySource,
                              Map<String, Block> blocks) {
        public Block form(String form) {
            Block block = blocks.get(form);
            if (block == null) throw new IllegalArgumentException("Unknown stone form " + form);
            return block;
        }
    }

    public record BeamFamily(String material, Block sourcePlanks, Block beam, Block beamSlab,
                             Block beamStairs, Block wall) {
        public net.minecraft.core.Direction.Axis beamAxis(BlockState state) {
            return state.getValue(RotatedPillarBlock.AXIS);
        }

        public net.minecraft.core.Direction.Axis slabMaterialAxis(BlockState state) {
            return state.getValue(FacingSlabBlock.FACING).getAxis();
        }
    }

    private record WoodSpec(String id, Block planks, Block beamMaterial) {
        private WoodSpec(String id, Block planks) {
            this(id, planks, Blocks.STRIPPED_OAK_LOG);
        }
    }

    private record StoneSpec(String id, Block foundation, Block masonry) {}

    @FunctionalInterface
    private interface BlockFactory {
        Block create(BlockBehaviour.Properties properties);
    }

    private static final class BBBStairBlock extends StairBlock {
        private BBBStairBlock(BlockState baseState, BlockBehaviour.Properties properties) {
            super(baseState, properties);
        }
    }

    private BBBContent() {}
}

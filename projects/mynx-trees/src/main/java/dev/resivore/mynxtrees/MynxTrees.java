package dev.resivore.mynxtrees;

import java.util.*;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.registry.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class MynxTrees implements ModInitializer {
    public static final SimpleParticleType WISTERIA_PARTICLE = Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("wisteria"), FabricParticleTypes.simple());
    public static final Block SILVER_LOG = block("silver_birch_log", RotatedPillarBlock::new, Blocks.BIRCH_LOG);
    public static final Block SILVER_WOOD = block("silver_birch_wood", RotatedPillarBlock::new, Blocks.BIRCH_WOOD);
    public static final Block WISTERIA_LOG = block("wisteria_log", RotatedPillarBlock::new, Blocks.CHERRY_LOG);
    public static final Block WISTERIA_WOOD = block("wisteria_wood", RotatedPillarBlock::new, Blocks.CHERRY_WOOD);
    public static final Block SILVER_LEAVES = block("silver_birch_leaves", p -> new TintedParticleLeavesBlock(0.01F, p), Blocks.BIRCH_LEAVES);
    public static final Block WISTERIA_LEAVES = block("wisteria_leaves", p -> new UntintedParticleLeavesBlock(0.1F, WISTERIA_PARTICLE, p), Blocks.CHERRY_LEAVES);
    public static final Block SILVER_SAPLING = block("silver_birch_sapling", p -> new TreeSapling(new TreeGrower("mynx_trees:silver_birch", Optional.empty(), Optional.of(configured("silver_super_birch_bees_0002")), Optional.of(configured("silver_super_birch_bees"))), "super_birch_bees_0002", p), Blocks.BIRCH_SAPLING);
    public static final Block WISTERIA_SAPLING = block("wisteria_sapling", p -> new TreeSapling(new TreeGrower("mynx_trees:wisteria", Optional.empty(), Optional.of(configured("wisteria_cherry")), Optional.of(configured("wisteria_cherry_bees_005"))), "cherry", p), Blocks.CHERRY_SAPLING);
    public static final Block SWEET_VIOLETS = block("sweet_violets", Violets::new, Blocks.PINK_PETALS);
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("mynx_trees", path); }
    public static ResourceKey<ConfiguredFeature<?, ?>> configured(String path) { return ResourceKey.create(Registries.CONFIGURED_FEATURE, id(path)); }
    private static ResourceKey<PlacedFeature> placed(String path) { return ResourceKey.create(Registries.PLACED_FEATURE, id(path)); }
    private static ResourceKey<PlacedFeature> vanillaPlaced(String path) { return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace(path)); }
    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, Block base) {
        var key = ResourceKey.create(Registries.BLOCK, id(name));
        var block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key)));
        Registry.register(BuiltInRegistries.ITEM, id(name), new BlockItem(block, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name))).useBlockDescriptionPrefix()));
        return block;
    }
    @Override public void onInitialize() {
        Registry.register(BuiltInRegistries.FEATURE, id("material_tree"), new MaterialTreeFeature());
        GroveFlowers.register();
        StrippableBlockRegistry.register(SILVER_LOG, Blocks.STRIPPED_BIRCH_LOG);
        StrippableBlockRegistry.register(SILVER_WOOD, Blocks.STRIPPED_BIRCH_WOOD);
        StrippableBlockRegistry.register(WISTERIA_LOG, Blocks.STRIPPED_PALE_OAK_LOG);
        StrippableBlockRegistry.register(WISTERIA_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD);
        for (Block b : List.of(SILVER_LOG, SILVER_WOOD, WISTERIA_LOG, WISTERIA_WOOD)) FlammableBlockRegistry.getDefaultInstance().add(b, 5, 5);
        for (Block b : List.of(SILVER_LEAVES, WISTERIA_LEAVES)) {
            FlammableBlockRegistry.getDefaultInstance().add(b, 30, 60);
            CompostableRegistry.INSTANCE.add(b, 0.3F);
        }
        for (Block b : List.of(SILVER_SAPLING, WISTERIA_SAPLING)) CompostableRegistry.INSTANCE.add(b, 0.3F);
        CompostableRegistry.INSTANCE.add(SWEET_VIOLETS, 0.3F);
        FuelValueEvents.BUILD.register((builder, context) -> {
            for (Block b : List.of(SILVER_LOG, SILVER_WOOD, WISTERIA_LOG, WISTERIA_WOOD)) builder.add(b, 300);
            builder.add(SILVER_SAPLING, 100); builder.add(WISTERIA_SAPLING, 100);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            for (Block b : List.of(SILVER_LOG,SILVER_WOOD,SILVER_LEAVES,SILVER_SAPLING,WISTERIA_LOG,WISTERIA_WOOD,WISTERIA_LEAVES,WISTERIA_SAPLING,SWEET_VIOLETS)) entries.accept(b);
        });
        BiomeModifications.create(id("biome_materials")).add(ModificationPhase.REPLACEMENTS, BiomeSelectors.includeByKey(Biomes.OLD_GROWTH_BIRCH_FOREST), context -> {
            if (context.getGenerationSettings().removeFeature(GenerationStep.Decoration.VEGETAL_DECORATION, vanillaPlaced("birch_tall")))
                context.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed("silver_birch_trees"));
            context.getEffects().setGrassColorOverride(0xB0C73A);
        }).add(ModificationPhase.REPLACEMENTS, BiomeSelectors.includeByKey(Biomes.CHERRY_GROVE), context -> {
            if (context.getGenerationSettings().removeFeature(GenerationStep.Decoration.VEGETAL_DECORATION, vanillaPlaced("trees_cherry"))) {
                context.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed("grove_trees"));
                context.getGenerationSettings().removeFeature(GenerationStep.Decoration.VEGETAL_DECORATION, vanillaPlaced("flower_cherry"));
            }
        });
    }
    private static final class TreeSapling extends SaplingBlock {
        private final ResourceKey<ConfiguredFeature<?, ?>> source;
        TreeSapling(TreeGrower grower, String source, BlockBehaviour.Properties properties) {
            super(grower, properties);
            this.source = ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.withDefaultNamespace(source));
        }
        @Override public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return false;
            var feature = server.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(source).value();
            if (!(feature.config() instanceof net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration tree)) return false;
            return level.isInsideBuildHeight(pos.above(tree.trunkPlacer.getBaseHeight()));
        }
    }
    private static final class Violets extends FlowerBedBlock {
        Violets(BlockBehaviour.Properties properties) { super(properties); }
    }
}

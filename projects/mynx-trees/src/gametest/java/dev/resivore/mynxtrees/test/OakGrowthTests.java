package dev.resivore.mynxtrees.test;

import dev.resivore.mynxtrees.MynxTrees;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class OakGrowthTests {
    public record Trace(boolean control, List<String> features) {}
    public static final ScopedValue<Trace> TRACE = ScopedValue.newInstance();
    private record Outcome(Map<BlockPos, BlockState> blocks, List<String> features, long nextRandom) {}

    @GameTest
    public void selectedTrees(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 20, 2));
        test.setTime(6000);
        level.setBlock(pos.below().west(), Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        test.runAfterDelay(5, () -> compareTrees(test, pos));
    }

    private static void compareTrees(GameTestHelper test, BlockPos pos) {
        var level = test.getLevel();
        boolean farmTweaks = FabricLoader.getInstance().isModLoaded("farmtweaks");
        boolean replant = FabricLoader.getInstance().isModLoaded("replantswamp");
        test.assertTrue(farmTweaks == Boolean.getBoolean("mynx.test.farmtweaks")
                && replant == Boolean.getBoolean("mynx.test.replant"), "wrong provider classpath");
        checkGrowthStages(test, pos);
        for (var biome : List.of(Biomes.PLAINS, Biomes.MANGROVE_SWAMP, Biomes.SWAMP)) {
            setBiome(test, pos, biome);
            for (boolean tick : List.of(false, true)) {
                for (boolean fancy : List.of(false, true)) {
                    for (boolean flowers : List.of(false, true)) {
                        boolean swamp = (farmTweaks || replant) && biome.equals(Biomes.SWAMP)
                                || replant && biome.equals(Biomes.MANGROVE_SWAMP);
                        String key = swamp ? "swamp_oak" : (fancy ? "fancy_oak" : "oak") + (flowers ? "_bees_005" : "");
                        int rate = swamp ? 3 : fancy ? 10 : 0;
                        var tree = compare(test, pos, oakSeed(tick, fancy), "minecraft:" + key, rate, random -> {
                            if (flowers) level.setBlock(pos.east(2), Blocks.DANDELION.defaultBlockState(), Block.UPDATE_NONE);
                            grow(level, pos, Blocks.OAK_SAPLING, tick, random);
                        });
                        test.assertTrue(has(tree, Blocks.OAK_LEAVES), "missing canopy for " + key);
                        if (swamp) test.assertTrue(has(tree, Blocks.VINE), "missing swamp vines");
                    }
                }
            }
        }

        for (var entry : Map.of(MynxTrees.SILVER_SAPLING, "silver_super_birch_bees_0002",
                                MynxTrees.WISTERIA_SAPLING, "wisteria_cherry").entrySet()) {
            compare(test, pos, 42, "mynx_trees:" + entry.getValue(), 0,
                    random -> grow(level, pos, entry.getKey(), false, random));
        }

        // A failed native placement must restore the staged sapling.
        boolean provider = farmTweaks || replant;
        var failed = compare(test, pos, 42, provider ? "minecraft:swamp_oak" : "minecraft:oak", provider ? 3 : 0,
                random -> {
                    level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                    grow(level, pos, Blocks.OAK_SAPLING, false, random);
                });
        test.assertTrue(failed.blocks.get(pos).is(Blocks.OAK_SAPLING)
                && failed.blocks.get(pos).getValue(SaplingBlock.STAGE) == 1, "lost blocked sapling");

        compareWorldgen(test, pos);
        test.succeed();
    }

    private static void checkGrowthStages(GameTestHelper test, BlockPos pos) {
        var level = test.getLevel();
        var trace = new Trace(false, new ArrayList<>());
        var state = Blocks.OAK_SAPLING.defaultBlockState();
        level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_NONE);
        level.setBlock(pos, state, Block.UPDATE_NONE);
        ScopedValue.where(TRACE, trace).run(() -> {
            ((SaplingBlock)Blocks.OAK_SAPLING).performBonemeal(level, RandomSource.create(0), pos, state);
            var staged = level.getBlockState(pos);
            test.assertTrue(staged == state.setValue(SaplingBlock.STAGE, 1), "first growth step skipped staging");
            // Seed zero misses the tick's one-in-seven growth check.
            staged.randomTick(level, pos, RandomSource.create(0));
            test.assertTrue(level.getBlockState(pos) == staged && trace.features.isEmpty(), "premature feature placement");
        });
    }

    private static void compareWorldgen(GameTestHelper test, BlockPos pos) {
        var level = test.getLevel();
        // Use the vanilla FEATURES step with writable GameTest chunks as the region's backing store.
        var chunk = level.getChunkAt(pos);
        var region = new WorldGenRegion(level, StaticCache2D.create(chunk.getPos().x(), chunk.getPos().z(), 0,
                (x, z) -> null), ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES), chunk) {
            @Override public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean required) {
                return level.getChunk(x, z);
            }
        };
        for (String key : List.of("oak", "swamp_oak", "fallen_oak_tree", "fancy_oak", "fancy_oak_bees",
                "fancy_oak_bees_002", "fancy_oak_bees_005", "fancy_oak_leaf_litter", "fancy_oak_bees_0002_leaf_litter")) {
            int rate = key.startsWith("fancy_oak") ? 10 : key.equals("oak") ? 0 : 3;
            var natural = compare(test, pos, 71, "minecraft:" + key, rate, random -> place(level, region, pos, key, random));
            test.assertTrue(has(natural, Blocks.OAK_LOG) || has(natural, mossyLog()), "empty worldgen tree " + key);
            if (key.equals("swamp_oak")) test.assertTrue(has(natural, Blocks.VINE), "missing worldgen swamp vines");
            if (key.equals("fancy_oak_bees")) test.assertTrue(has(natural, Blocks.BEE_NEST), "missing bee nest");
            if (key.endsWith("leaf_litter")) test.assertTrue(has(natural, Blocks.LEAF_LITTER), "missing leaf litter");
            compare(test, pos, 71, "minecraft:" + key, 0, random -> place(level, level, pos, key, random));
        }
    }

    private static void setBiome(GameTestHelper test, BlockPos pos, ResourceKey<Biome> biome) {
        var level = test.getLevel();
        var holder = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome);
        FillBiomeCommand.fill(level, pos.offset(-8, -8, -8), pos.offset(8, 8, 8), holder);
        test.assertTrue(level.getBiome(pos).is(biome), "wrong biome for " + pos);
    }

    private static long oakSeed(boolean tick, boolean fancy) {
        for (long seed = 0; ; seed++) {
            var random = RandomSource.create(seed);
            if (tick && random.nextInt(7) != 0) continue;
            if ((random.nextFloat() < 0.1F) == fancy) return seed;
        }
    }

    private static void grow(ServerLevel level, BlockPos pos, Block sapling, boolean tick, RandomSource random) {
        var state = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        level.setBlock(pos, state, Block.UPDATE_NONE);
        if (tick) {
            var chunk = level.getChunkAt(pos).getPos();
            var engine = level.getChunkSource().getLightEngine();
            var ready = engine.waitForPendingTasks(chunk.x(), chunk.z());
            level.getServer().managedBlock(() -> {
                engine.tryScheduleUpdate();
                return ready.isDone();
            });
            ready.join();
            if (level.getMaxLocalRawBrightness(pos.above()) < 9)
                throw new IllegalStateException("fixture light " + level.getMaxLocalRawBrightness(pos.above())
                        + " by " + level.getBlockState(pos.below().west()) + " at " + pos);
        }
        if (tick) state.randomTick(level, pos, random);
        else ((SaplingBlock)sapling).performBonemeal(level, random, pos, state);
    }

    private static void place(ServerLevel level, WorldGenLevel target, BlockPos pos, String key, RandomSource random) {
        var registry = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        var feature = registry.getValue(Identifier.withDefaultNamespace(key));
        feature.place(target, level.getChunkSource().getGenerator(), random, pos);
    }

    private static Block mossyLog() {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mynx_trees", "mossy_oak_log"));
    }

    private static boolean has(Outcome tree, Block block) {
        return tree.blocks.values().stream().anyMatch(state -> state.is(block));
    }

    private static Outcome compare(GameTestHelper test, BlockPos pos, long seed, String key, int rate,
                                   Consumer<RandomSource> attempt) {
        var level = test.getLevel();
        var baseline = run(level, pos, seed, attempt, true);
        var actual = run(level, pos, seed, attempt, false);
        test.assertTrue(!actual.features.isEmpty() && actual.features.getFirst().equals(key + "/" + rate)
                && actual.features.stream().skip(1).allMatch(nested -> nested.endsWith("/0")),
                "expected " + key + "/" + rate + ", saw " + actual.features);
        test.assertTrue(baseline.features.equals(actual.features) && baseline.nextRandom == actual.nextRandom,
                "selection or RNG diverged for " + key);
        test.assertTrue(baseline.blocks.keySet().equals(actual.blocks.keySet()), "geometry diverged for " + key);
        var mossy = mossyLog();
        for (var entry : baseline.blocks.entrySet()) {
            var p = entry.getKey();
            var expected = entry.getValue();
            if (expected.is(Blocks.OAK_LOG) && rate > 0) {
                long roll = RandomSupport.mixStafford13(p.asLong() ^ level.getSeed() ^ 0x54d8abd9207a3bf1L);
                if (RandomSource.create(roll).nextInt(rate) == 0) expected = mossy.withPropertiesOf(expected);
            }
            if (actual.blocks.get(p) != expected) test.fail("state mismatch for " + key + " at " + p
                    + ": expected " + expected + ", got " + actual.blocks.get(p));
        }
        return actual;
    }

    private static Outcome run(ServerLevel level, BlockPos pos, long seed, Consumer<RandomSource> attempt, boolean control) {
        var volume = BlockPos.betweenClosed(pos.offset(-12, -1, -12), pos.offset(12, 24, 12));
        for (var p : volume) {
            if (p.equals(pos.below().west())) continue;
            var state = p.getY() < pos.getY() ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (level.getBlockState(p) != state) level.setBlock(p, state, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        var random = RandomSource.create(seed);
        var trace = new Trace(control, new ArrayList<>());
        ScopedValue.where(TRACE, trace).run(() -> attempt.accept(random));
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (var p : volume) {
            var state = level.getBlockState(p);
            if (!state.isAir()) blocks.put(p.immutable(), state);
        }
        return new Outcome(blocks, trace.features(), random.nextLong());
    }
}

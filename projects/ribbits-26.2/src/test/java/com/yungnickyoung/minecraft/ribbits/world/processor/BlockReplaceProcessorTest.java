package com.yungnickyoung.minecraft.ribbits.world.processor;

import com.yungnickyoung.minecraft.yungsapi.api.world.randomize.BlockStateRandomizer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockReplaceProcessorTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void nonTargetBlockPassesThroughByIdentity() throws ReflectiveOperationException {
        BlockReplaceProcessor processor = processor(
                Blocks.STONE.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(),
                false,
                false);
        StructureTemplate.StructureBlockInfo input = info(Blocks.OAK_PLANKS.defaultBlockState(), null);

        StructureTemplate.StructureBlockInfo result = processor.processBlock(
                null, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, input, settings());

        assertSame(input, result);
    }

    @Test
    void replacementPreservesTemplateNbt() throws ReflectiveOperationException {
        BlockReplaceProcessor processor = processor(
                Blocks.STONE.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(),
                false,
                false);
        CompoundTag nbt = new CompoundTag();
        nbt.putString("marker", "preserved");

        StructureTemplate.StructureBlockInfo result = processor.processBlock(
                null,
                BlockPos.ZERO,
                BlockPos.ZERO,
                BlockPos.ZERO,
                info(Blocks.STONE.defaultBlockState(), nbt),
                settings());

        assertTrue(result.state().is(Blocks.DIRT));
        assertSame(nbt, result.nbt());
    }

    @Test
    void copyPropertiesAndPreserveWaterlogMatchExactTransformation() throws ReflectiveOperationException {
        BlockState inputState = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, net.minecraft.core.Direction.EAST)
                .setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockReplaceProcessor processor = processor(
                Blocks.OAK_STAIRS.defaultBlockState(),
                Blocks.COBBLESTONE_STAIRS.defaultBlockState(),
                true,
                true);

        BlockState result = processor.processBlock(
                null,
                BlockPos.ZERO,
                BlockPos.ZERO,
                BlockPos.ZERO,
                info(inputState, null),
                settings()).state();

        assertTrue(result.is(Blocks.COBBLESTONE_STAIRS));
        assertEquals(inputState.getValue(StairBlock.FACING), result.getValue(StairBlock.FACING));
        assertEquals(inputState.getValue(StairBlock.HALF), result.getValue(StairBlock.HALF));
        assertEquals(inputState.getValue(StairBlock.SHAPE), result.getValue(StairBlock.SHAPE));
        assertTrue(result.getValue(BlockStateProperties.WATERLOGGED));
    }

    @Test
    void sameSeedAndPositionReproduceSameHalfRandomizerOutcome() throws ReflectiveOperationException {
        BlockStateRandomizer output = redDefaultBrownHalfRandomizer();
        BlockReplaceProcessor processor = processor(
                Blocks.STONE.defaultBlockState(),
                output,
                false,
                false);
        long seed = 0x52494242495453L;
        BlockPos position = new BlockPos(37, 72, -19);

        BlockState first = processAt(processor, seed, position);
        BlockState second = processAt(processor, seed, position);

        assertEquals(first, second);
        assertTrue(first.is(Blocks.RED_MUSHROOM) || first.is(Blocks.BROWN_MUSHROOM));
    }

    @Test
    void exactHalfEntryAndDefaultCanReachBothBranchesWithoutStatisticalClaim()
            throws ReflectiveOperationException {
        BlockStateRandomizer output = redDefaultBrownHalfRandomizer();
        assertEquals(Blocks.RED_MUSHROOM.defaultBlockState(), output.getDefaultBlockState());
        assertEquals(
                Map.of(Blocks.BROWN_MUSHROOM.defaultBlockState(), 0.5F),
                output.getEntriesAsMap());

        BlockReplaceProcessor processor = processor(
                Blocks.STONE.defaultBlockState(),
                output,
                false,
                false);
        List<Long> seeds = List.of(
                0L,
                0x52494242495453L,
                -1L,
                Long.MIN_VALUE,
                Long.MAX_VALUE,
                0x13579BDF2468ACE0L,
                0x0F0E0D0C0B0A0908L,
                -0x123456789ABCDEFL);
        List<BlockPos> positions = List.of(
                BlockPos.ZERO,
                new BlockPos(1, 64, 1),
                new BlockPos(-37, 72, 19),
                new BlockPos(2048, -32, -4096));
        Set<BlockState> observed = new HashSet<>();

        for (int i = 0; i < seeds.size(); i++) {
            observed.add(processAt(processor, seeds.get(i), positions.get(i % positions.size())));
        }

        // This fixed sample checks deterministic branch reachability only. The entry/default
        // assertions above are the authoritative exact 0.5 brown / 0.5 red contract.
        assertEquals(
                Set.of(
                        Blocks.RED_MUSHROOM.defaultBlockState(),
                        Blocks.BROWN_MUSHROOM.defaultBlockState()),
                observed);
    }

    @Test
    void villagePlacementSettingsProduceStableIndependentRollsByGlobalPosition()
            throws ReflectiveOperationException {
        BlockReplaceProcessor processor = processor(
                Blocks.STONE.defaultBlockState(),
                redDefaultBrownHalfRandomizer(),
                false,
                false);
        // SinglePoolElement#getSettings constructs the settings used for jigsaw placement
        // without injecting a RandomSource. Vanilla therefore derives a fresh random from
        // each global block position passed to StructurePlaceSettings#getRandom.
        StructurePlaceSettings villageSettings = new StructurePlaceSettings();
        Map<BlockPos, BlockState> firstPass = new LinkedHashMap<>();

        for (int x = 0; x < 128; x++) {
            BlockPos position = new BlockPos(x, 64, x * 31 - 2048);
            firstPass.put(position, processAt(processor, villageSettings, position));
        }

        assertEquals(
                Set.of(
                        Blocks.RED_MUSHROOM.defaultBlockState(),
                        Blocks.BROWN_MUSHROOM.defaultBlockState()),
                new HashSet<>(firstPass.values()));
        firstPass.forEach((position, expected) ->
                assertEquals(expected, processAt(processor, villageSettings, position)));
    }

    @Test
    void explicitlyInjectedPlacementRandomIgnoresPosition()
            throws ReflectiveOperationException {
        BlockReplaceProcessor processor = processor(
                Blocks.STONE.defaultBlockState(),
                redDefaultBrownHalfRandomizer(),
                false,
                false);
        long seed = 0x52494242495453L;

        BlockState atOrigin = processAt(
                processor,
                new StructurePlaceSettings().setRandom(RandomSource.create(seed)),
                BlockPos.ZERO);
        BlockState farAway = processAt(
                processor,
                new StructurePlaceSettings().setRandom(RandomSource.create(seed)),
                new BlockPos(8192, 96, -4096));

        assertEquals(atOrigin, farAway);
    }

    @Test
    void processorUsesStructurePlacementRandomnessAtTheGlobalBlockPosition() throws IOException {
        String source = Files.readString(PROJECT_ROOT.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/world/processor/BlockReplaceProcessor.java"));

        assertTrue(source.contains(
                "structurePlacementData.getRandom(blockInfoGlobal.pos())"));
        assertFalse(source.contains("new Random("));
        assertFalse(source.contains("ThreadLocalRandom"));
    }

    private static BlockReplaceProcessor processor(BlockState target, BlockState output,
                                                   boolean copyProperties, boolean preserveWaterlog)
            throws ReflectiveOperationException {
        return processor(
                target,
                new BlockStateRandomizer(output),
                copyProperties,
                preserveWaterlog);
    }

    private static BlockReplaceProcessor processor(BlockState target, BlockStateRandomizer output,
                                                   boolean copyProperties, boolean preserveWaterlog)
            throws ReflectiveOperationException {
        Constructor<BlockReplaceProcessor> constructor = BlockReplaceProcessor.class.getDeclaredConstructor(
                BlockState.class,
                BlockStateRandomizer.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(
                target,
                output,
                false,
                false,
                copyProperties,
                preserveWaterlog);
    }

    private static BlockStateRandomizer redDefaultBrownHalfRandomizer() {
        return new BlockStateRandomizer(
                List.of(new BlockStateRandomizer.Entry(
                        Blocks.BROWN_MUSHROOM.defaultBlockState(),
                        0.5F)),
                Blocks.RED_MUSHROOM.defaultBlockState());
    }

    private static BlockState processAt(BlockReplaceProcessor processor, long seed, BlockPos position) {
        return processAt(processor, settings(seed), position);
    }

    private static BlockState processAt(
            BlockReplaceProcessor processor,
            StructurePlaceSettings settings,
            BlockPos position) {
        return processor.processBlock(
                null,
                BlockPos.ZERO,
                BlockPos.ZERO,
                BlockPos.ZERO,
                info(position, Blocks.STONE.defaultBlockState(), null),
                settings).state();
    }

    private static StructureTemplate.StructureBlockInfo info(BlockState state, CompoundTag nbt) {
        return info(BlockPos.ZERO, state, nbt);
    }

    private static StructureTemplate.StructureBlockInfo info(
            BlockPos position, BlockState state, CompoundTag nbt) {
        return new StructureTemplate.StructureBlockInfo(position, state, nbt);
    }

    private static StructurePlaceSettings settings() {
        return settings(0x52494242495453L);
    }

    private static StructurePlaceSettings settings(long seed) {
        return new StructurePlaceSettings().setRandom(RandomSource.create(seed));
    }
}

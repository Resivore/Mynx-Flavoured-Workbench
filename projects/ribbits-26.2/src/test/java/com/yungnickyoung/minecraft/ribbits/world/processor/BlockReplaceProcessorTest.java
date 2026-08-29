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

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockReplaceProcessorTest {
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

    private static BlockReplaceProcessor processor(BlockState target, BlockState output,
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
                new BlockStateRandomizer(output),
                false,
                false,
                copyProperties,
                preserveWaterlog);
    }

    private static StructureTemplate.StructureBlockInfo info(BlockState state, CompoundTag nbt) {
        return new StructureTemplate.StructureBlockInfo(BlockPos.ZERO, state, nbt);
    }

    private static StructurePlaceSettings settings() {
        return new StructurePlaceSettings().setRandom(RandomSource.create(0x52494242495453L));
    }
}

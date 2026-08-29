package games.twinhead.moreslabsstairsandwalls.block.spreadable;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.lighting.LightEngine;

import java.util.ArrayList;
import java.util.List;

/** Nibaru-owned, geometry-neutral spreadable material lifecycle and pairing API. */
public final class SpreadableSemantics {
    private static final List<Pair> PAIRS = new ArrayList<>();
    private static boolean nativePairsRegistered;

    private SpreadableSemantics() {}

    public static void registerNativePairs() {
        if (nativePairsRegistered) return;
        nativePairsRegistered = true;
        registerPair(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK);
        registerPair(Blocks.DIRT, Blocks.MYCELIUM, Blocks.MYCELIUM);
        for (ModBlocks.BlockType type : ModBlocks.BlockType.values()) {
            Block dirt = ModBlocks.DIRT.getBlock(type);
            registerPair(dirt, ModBlocks.GRASS_BLOCK.getBlock(type), Blocks.GRASS_BLOCK);
            registerPair(dirt, ModBlocks.MYCELIUM.getBlock(type), Blocks.MYCELIUM);
        }
    }

    public static void registerPair(Block base, Block spreadable, Block material) {
        if (base == null || spreadable == null || material == null) return;
        PAIRS.removeIf(pair -> pair.material == material
                && (pair.base == base || pair.spreadable == spreadable));
        PAIRS.add(new Pair(base, spreadable, material));
    }

    public static boolean isBaseTarget(BlockState state) {
        return PAIRS.stream().anyMatch(pair -> state.is(pair.base));
    }

    public static boolean isSpreadableSource(BlockState state) {
        return PAIRS.stream().anyMatch(pair -> state.is(pair.spreadable));
    }

    public static Block materialFor(BlockState source) {
        return PAIRS.stream().filter(pair -> source.is(pair.spreadable))
                .map(Pair::material).findFirst().orElse(source.getBlock());
    }

    public static BlockState toBase(BlockState state) {
        return PAIRS.stream().filter(pair -> state.is(pair.spreadable)).findFirst()
                .map(pair -> copyShared(state, pair.base.defaultBlockState())).orElse(state);
    }

    public static BlockState toSpreadable(BlockState state, Block material) {
        return PAIRS.stream().filter(pair -> state.is(pair.base) && pair.material == material).findFirst()
                .map(pair -> withSnowy(copyShared(state, pair.spreadable.defaultBlockState()), false)).orElse(state);
    }

    public static void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.setBlockAndUpdate(pos, toBase(state));
            return;
        }
        spreadFrom(state, level, pos, random, true);
    }

    public static void spreadFrom(BlockState state, ServerLevel level, BlockPos pos,
                                  RandomSource random, boolean includeVanillaBase) {
        if (!isSpreadableSource(state) || level.getMaxLocalRawBrightness(pos.above()) < 9) return;
        Block material = materialFor(state);
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1,
                    random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (includeVanillaBase || !level.getBlockState(target).is(Blocks.DIRT)) {
                trySpread(level, material, target);
            }
        }
    }

    public static boolean trySpread(ServerLevel level, Block material, BlockPos targetPos) {
        BlockState oldState = level.getBlockState(targetPos);
        BlockState newState = toSpreadable(oldState, material);
        if (newState == oldState || !canSurvive(newState, level, targetPos)
                || level.getFluidState(targetPos.above()).is(FluidTags.WATER)) return false;
        level.setBlockAndUpdate(targetPos, withSnowy(newState, level.getBlockState(targetPos.above()).is(BlockTags.SNOW)));
        return true;
    }

    public static boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getBlock() instanceof SpreadableGeometry geometry) {
            SpreadableGeometry.Exposure exposure = geometry.spreadableExposure(state, level, pos);
            if (exposure != SpreadableGeometry.Exposure.DEFAULT) {
                return exposure == SpreadableGeometry.Exposure.EXPOSED;
            }
        }
        BlockState above = level.getBlockState(pos.above());
        if (above.is(Blocks.SNOW) && above.getValue(SnowLayerBlock.LAYERS) == 1) return true;
        if (above.getFluidState().getAmount() == 8) return false;
        int dampening = LightEngine.getLightDampeningInto(Blocks.GRASS_BLOCK.defaultBlockState(), above,
                Direction.UP, above.getLightDampening());
        return dampening < 15;
    }

    public static BlockState copyShared(BlockState source, BlockState target) {
        BlockState result = target;
        for (Property<?> property : source.getProperties()) {
            if (result.hasProperty(property)) result = copy(source, result, property);
        }
        return result;
    }

    private static BlockState withSnowy(BlockState state, boolean snowy) {
        return state.hasProperty(BlockStateProperties.SNOWY) ? state.setValue(BlockStateProperties.SNOWY, snowy) : state;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState source, BlockState target, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }

    private record Pair(Block base, Block spreadable, Block material) {}
}

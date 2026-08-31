package dev.aero.cnmterraincompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.UnaryOperator;

public final class GrassFamilyBehavior {
    private GrassFamilyBehavior() {
    }

    static void registerDefaults() {
        SpreadableSemantics.registerPair(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK);
        register(CnmTerrainCompat.DIRT_VERTICAL_SLAB, CnmTerrainCompat.GRASS_VERTICAL_SLAB,
                dirt -> DirtVerticalSlab.copyGeometry(dirt, CnmTerrainCompat.GRASS_VERTICAL_SLAB.defaultBlockState()),
                grass -> DirtVerticalSlab.copyGeometry(grass, CnmTerrainCompat.DIRT_VERTICAL_SLAB.defaultBlockState()));
        register(CnmTerrainCompat.DIRT_SLAB, CnmTerrainCompat.GRASS_SLAB,
                dirt -> DirtHorizontalSlab.copyGeometry(dirt, CnmTerrainCompat.GRASS_SLAB.defaultBlockState()),
                grass -> DirtHorizontalSlab.copyGeometry(grass, CnmTerrainCompat.DIRT_SLAB.defaultBlockState()));
    }

    public static void registerCanonicalDirtVertical(Block dirt) {
        SpreadableSemantics.registerPair(dirt, CnmTerrainCompat.GRASS_VERTICAL_SLAB, Blocks.GRASS_BLOCK);
    }

    public static void registerCanonicalStep(Block dirt, Block grass) {
        SpreadableSemantics.registerPair(dirt, grass, Blocks.GRASS_BLOCK);
    }

    public static BlockState copySharedGeometry(BlockState source, BlockState target) {
        return SpreadableSemantics.copyShared(source, target);
    }

    public static void register(Block dirt, Block grass,
                                UnaryOperator<BlockState> toGrass,
                                UnaryOperator<BlockState> toDirt) {
        SpreadableSemantics.registerPair(dirt, grass, Blocks.GRASS_BLOCK);
    }

    public static boolean isGrassSource(BlockState state) {
        return SpreadableSemantics.isSpreadableSource(state);
    }

    public static boolean isDirtTarget(BlockState state) {
        return SpreadableSemantics.isBaseTarget(state);
    }

    public static BlockState convertDirtGeometryToGrass(BlockState state) {
        return SpreadableSemantics.toSpreadable(state, Blocks.GRASS_BLOCK);
    }

    public static BlockState convertGrassGeometryToDirt(BlockState state) {
        return SpreadableSemantics.toBase(state);
    }

    public static void spreadFrom(BlockState sourceState, ServerLevel level, BlockPos sourcePos,
                                  RandomSource random, boolean includeVanillaDirt) {
        GeometrySpreadableBehavior.spreadFrom(
                sourceState, level, sourcePos, random, includeVanillaDirt);
    }

    public static boolean tryConvertDirtAt(ServerLevel level, BlockPos targetPos) {
        return GeometrySpreadableBehavior.trySpread(level, Blocks.GRASS_BLOCK, targetPos);
    }

}

package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Shared fixed-foliage values and native-form registration targets. */
public final class FoliageTintContract {
    /** Exact Minecraft 26.2 ARGB multiplier registered for {@code minecraft:spruce_leaves}. */
    public static final int SPRUCE_FIXED_ARGB = 0xFF619961;
    /** Retained fixed birch multiplier; its existing profile behavior is intentionally unchanged. */
    public static final int BIRCH_FIXED_ARGB = 0x80A755;

    private FoliageTintContract() {}

    public static boolean isFixed(TintProfile profile) {
        return profile == TintProfile.FOLIAGE_SPRUCE || profile == TintProfile.FOLIAGE_BIRCH;
    }

    public static int fixedArgb(TintProfile profile) {
        return switch (profile) {
            case FOLIAGE_SPRUCE -> SPRUCE_FIXED_ARGB;
            case FOLIAGE_BIRCH -> BIRCH_FIXED_ARGB;
            default -> throw new IllegalArgumentException("No fixed foliage tint for " + profile);
        };
    }

    public static List<Block> nativeTargets(ModBlocks block) {
        List<Block> targets = new ArrayList<>();
        for (ModBlocks.BlockType type : ModBlocks.BlockType.values()) {
            if (block.hasBlock(type)) targets.add(block.getBlock(type));
        }
        return List.copyOf(targets);
    }
}

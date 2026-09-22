package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Shared fixed-foliage values and native-form registration targets. */
public final class FoliageTintContract {
    /** Exact Minecraft 26.2 ARGB multiplier registered for {@code minecraft:spruce_leaves}. */
    public static final int SPRUCE_FIXED_ARGB = 0xFF619961;
    /** Retained fixed birch multiplier; its existing profile behavior is intentionally unchanged. */
    public static final int BIRCH_FIXED_ARGB = 0x80A755;
    private static volatile Set<Block> frozenSpruceGeometryTargets = Set.of();

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

    /**
     * Every non-root geometry in the exact spruce family whose final tint lookup must retain
     * Minecraft's fixed spruce multiplier. Before bootstrap freezes the inventory, this is
     * evaluated from live registrations so it also covers CNM-owned late roles without relying on
     * registry-name guesses.
     */
    public static Set<Block> spruceGeometryTargets() {
        Set<Block> frozen = frozenSpruceGeometryTargets;
        return frozen.isEmpty() ? collectSpruceGeometryTargets() : frozen;
    }

    static synchronized void freezeSpruceGeometryTargets() {
        if (!frozenSpruceGeometryTargets.isEmpty()) return;
        Set<Block> targets = collectSpruceGeometryTargets();
        if (targets.size() != 8 || targets.contains(Blocks.SPRUCE_LEAVES)) {
            throw new IllegalStateException("Expected exactly eight non-root BGE spruce geometries, found "
                    + targets.size());
        }
        frozenSpruceGeometryTargets = targets;
    }

    private static Set<Block> collectSpruceGeometryTargets() {
        Set<Block> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        targets.addAll(nativeTargets(ModBlocks.SPRUCE_LEAVES));
        NibaruMaterialProfiles.fromBlock(Blocks.SPRUCE_LEAVES)
                .ifPresent(profile -> targets.addAll(NibaruProviderAdapter.tintTargets(profile)));
        targets.remove(Blocks.SPRUCE_LEAVES);
        return Collections.unmodifiableSet(targets);
    }

    public static boolean isSpruceGeometry(Block block) {
        return frozenSpruceGeometryTargets.contains(block);
    }
}

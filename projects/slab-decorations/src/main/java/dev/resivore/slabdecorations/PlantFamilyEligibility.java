package dev.resivore.slabdecorations;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.Set;

/** Exact vanilla plant identities admitted by the first foliage tranche. */
public final class PlantFamilyEligibility {
    private static final Set<Block> SIMPLE_FOLIAGE = Set.of(
            Blocks.SHORT_GRASS,
            Blocks.FERN,
            Blocks.DEAD_BUSH,
            Blocks.DANDELION,
            Blocks.GOLDEN_DANDELION,
            Blocks.TORCHFLOWER,
            Blocks.POPPY,
            Blocks.BLUE_ORCHID,
            Blocks.ALLIUM,
            Blocks.AZURE_BLUET,
            Blocks.RED_TULIP,
            Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP,
            Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER,
            Blocks.WITHER_ROSE,
            Blocks.LILY_OF_THE_VALLEY,
            Blocks.OPEN_EYEBLOSSOM,
            Blocks.CLOSED_EYEBLOSSOM
    );

    private static final Set<Block> DOUBLE_GRASS_COMPANIONS = Set.of(
            Blocks.TALL_GRASS,
            Blocks.LARGE_FERN
    );

    private static final Set<Block> SAME_STATE_GROWABLES = Set.of(
            Blocks.NETHER_WART
    );

    /** Exact additional outputs emitted by the three canonical substrate features. */
    private static final Set<Block> SUBSTRATE_FEATURE_OUTPUTS = Set.of(
            Blocks.PINK_PETALS,
            Blocks.WILDFLOWERS,
            Blocks.AZALEA,
            Blocks.FLOWERING_AZALEA,
            Blocks.MOSS_CARPET,
            Blocks.PALE_MOSS_CARPET
    );

    private PlantFamilyEligibility() {
    }

    public static Optional<Family> family(Block block) {
        if (SIMPLE_FOLIAGE.contains(block)) return Optional.of(Family.SIMPLE_FOLIAGE);
        if (DOUBLE_GRASS_COMPANIONS.contains(block)) return Optional.of(Family.DOUBLE_GRASS_COMPANION);
        if (SAME_STATE_GROWABLES.contains(block)) return Optional.of(Family.SAME_STATE_GROWABLE);
        if (SUBSTRATE_FEATURE_OUTPUTS.contains(block)) return Optional.of(Family.SUBSTRATE_FEATURE_OUTPUT);
        return Optional.empty();
    }

    public static boolean isEligible(Block block) {
        return family(block).isPresent();
    }

    public static boolean isDoubleGrassCompanion(Block block) {
        return DOUBLE_GRASS_COMPANIONS.contains(block);
    }

    public static boolean isPaleMossCarpetCompanion(Block block) {
        return block == Blocks.PALE_MOSS_CARPET;
    }

    public static boolean isSubstrateFeatureOutput(Block block) {
        return SUBSTRATE_FEATURE_OUTPUTS.contains(block);
    }

    /**
     * Mirrors the admitted vanilla plant's substrate tag against a canonical full-block state.
     * This is deliberately separate from geometry resolution so render-only block views cannot
     * lower an otherwise invalid plant merely because some Nibaru slab happens to be below it.
     */
    public static boolean acceptsCanonicalParent(Block plant, BlockState canonicalParent) {
        if (!isEligible(plant)) return false;
        if (plant == Blocks.PINK_PETALS || plant == Blocks.WILDFLOWERS) {
            return canonicalParent.is(Blocks.GRASS_BLOCK);
        }
        if (plant == Blocks.AZALEA || plant == Blocks.FLOWERING_AZALEA
                || plant == Blocks.MOSS_CARPET) {
            return canonicalParent.is(Blocks.MOSS_BLOCK);
        }
        if (plant == Blocks.PALE_MOSS_CARPET) {
            return canonicalParent.is(Blocks.PALE_MOSS_BLOCK);
        }
        if (plant == Blocks.DEAD_BUSH) {
            return canonicalParent.is(BlockTags.SUPPORTS_DRY_VEGETATION);
        }
        if (plant == Blocks.WITHER_ROSE) {
            return canonicalParent.is(BlockTags.SUPPORTS_WITHER_ROSE);
        }
        if (plant == Blocks.NETHER_WART) {
            return canonicalParent.is(BlockTags.SUPPORTS_NETHER_WART);
        }
        return canonicalParent.is(BlockTags.SUPPORTS_VEGETATION);
    }

    public enum Family {
        SIMPLE_FOLIAGE,
        DOUBLE_GRASS_COMPANION,
        SAME_STATE_GROWABLE,
        SUBSTRATE_FEATURE_OUTPUT
    }
}

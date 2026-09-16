package dev.resivore.slabdecorations;

import dev.resivore.slabdecorations.mixin.GrowingPlantBlockAccessor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.BigDripleafStemBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SmallDripleafBlock;
import net.minecraft.world.level.block.SporeBlossomBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Classifies slab-compatible foliage by Minecraft behavior and attachment contracts.
 *
 * <p>This class intentionally owns no registry-ID permission table. A family only describes how
 * to find a physical root and how to align its representation; the block's own projected vanilla
 * survival result remains the permission decision.</p>
 */
public final class PlantFamilyEligibility {
    private PlantFamilyEligibility() {
    }

    public static Optional<Family> family(BlockState state) {
        var block = state.getBlock();

        // Minecraft 26.2 has no shared hanging-foliage superclass for these two single-block
        // decorations. Their concrete classes are still behavior contracts, not registry IDs.
        if (block instanceof HangingRootsBlock || block instanceof SporeBlossomBlock) {
            return Optional.of(Family.CEILING_FOLIAGE);
        }

        // Pale hanging moss is its own downward-growing, same-block column implementation.
        if (block instanceof HangingMossBlock) {
            return Optional.of(Family.HANGING_MOSS_COLUMN);
        }

        // Generic downward-growing head/body columns cover cave vines, weeping vines, and
        // compatible modded implementations. Upward and aquatic growing plants deliberately fail
        // closed instead of being inferred from IDs.
        if (block instanceof GrowingPlantBlock) {
            if (block instanceof LiquidBlockContainer) return Optional.empty();
            if (block instanceof GrowingPlantBlockAccessor accessor
                    && accessor.slabDecorations$getGrowthDirection() == Direction.DOWN) {
                return Optional.of(Family.DOWNWARD_GROWING_COLUMN);
            }
            return Optional.empty();
        }

        // These two dripleaf implementations do not extend VegetationBlock, but share one rooted
        // upward-supported column contract.
        if (block instanceof BigDripleafBlock || block instanceof BigDripleafStemBlock) {
            return Optional.of(Family.DRIPLEAF_COLUMN);
        }

        // Small dripleaf is both a double plant and a liquid container. Its dry, upward-supported
        // state is deliberately handled before the aquatic-family exclusion below.
        if (block instanceof SmallDripleafBlock) {
            return Optional.of(Family.DOUBLE_HEIGHT_VEGETATION);
        }

        if (block instanceof MossyCarpetBlock
                || block instanceof CarpetBlock && !(block instanceof WoolCarpetBlock)) {
            return Optional.of(Family.SURFACE_FOLIAGE);
        }

        if (!(block instanceof VegetationBlock)) {
            return Optional.empty();
        }

        // Fail closed for behavior families whose ordinary lifecycle depends on farmland, a side
        // attachment, fluid occupancy, or a tree/giant-fungus substrate transformation. This is a
        // class/tag boundary, not a list of permitted or forbidden species IDs.
        if (block instanceof CropBlock
                || block instanceof PitcherCropBlock
                || block instanceof StemBlock
                || block instanceof AttachedStemBlock
                || block instanceof SaplingBlock
                || block instanceof NetherFungusBlock
                || block instanceof LilyPadBlock
                || block instanceof LiquidBlockContainer) {
            return Optional.empty();
        }

        if (block instanceof DoublePlantBlock) {
            return Optional.of(Family.DOUBLE_HEIGHT_VEGETATION);
        }
        return Optional.of(Family.UPWARD_VEGETATION);
    }

    public static boolean isEligible(BlockState state) {
        return family(state).isPresent();
    }

    public enum Family {
        UPWARD_VEGETATION,
        DOUBLE_HEIGHT_VEGETATION,
        DRIPLEAF_COLUMN,
        SURFACE_FOLIAGE,
        CEILING_FOLIAGE,
        DOWNWARD_GROWING_COLUMN,
        HANGING_MOSS_COLUMN
    }
}

package dev.aero.cnmterraincompat;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Explicit one-source-per-blockspace placement contract for compound BGE geometry.
 *
 * <p>A normal placement into replaceable world space is funded and consumes one
 * item. Returning a state here identifies a compatible mutation of an already
 * funded BGE blockspace; {@link BgeBlockItem} performs partial growth through the
 * normal placement pipeline but refunds its otherwise automatic item consumption.
 * A full-occupancy successor that canonicalizes to the material block is a normal
 * one-item placement and is deliberately not refunded.
 * A click which cannot expand may still fund a normal placement in a replaceable
 * adjacent blockspace; if that placement is blocked, vanilla failure consumes nothing.</p>
 */
public interface BlockspaceFundedGeometry {
    /**
     * Computes the exact compatible in-place successor, or {@code null} when the
     * click is full, wrong-face, wrong-item, or otherwise incompatible.
     */
    @Nullable
    BlockState expandedState(BlockState existing, BlockPlaceContext context);

}

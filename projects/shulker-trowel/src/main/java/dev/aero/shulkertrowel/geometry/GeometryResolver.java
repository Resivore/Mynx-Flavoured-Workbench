package dev.aero.shulkertrowel.geometry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/** Resolves an exact source block without broadening to related material variants. */
public interface GeometryResolver {
    Optional<BlockItem> resolveGeometry(Block sourceBlock, TargetGeometry targetGeometry);
}

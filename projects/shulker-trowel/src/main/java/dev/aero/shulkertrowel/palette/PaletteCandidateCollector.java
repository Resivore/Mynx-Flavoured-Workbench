package dev.aero.shulkertrowel.palette;

import dev.aero.shulkertrowel.geometry.GeometryResolver;
import dev.aero.shulkertrowel.geometry.TargetGeometry;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Filters unresolved materials before quantity weighting. */
public final class PaletteCandidateCollector {
    private final GeometryResolver geometryResolver;

    public PaletteCandidateCollector(GeometryResolver geometryResolver) {
        this.geometryResolver = geometryResolver;
    }

    public List<PaletteCandidate> collect(
            NonNullList<ItemStack> contents,
            TargetGeometry targetGeometry
    ) {
        List<PaletteCandidate> candidates = new ArrayList<>();
        for (int slot = 0; slot < contents.size(); slot++) {
            ItemStack sourceStack = contents.get(slot);
            if (sourceStack.isEmpty() || !(sourceStack.getItem() instanceof BlockItem sourceItem)) continue;

            Optional<BlockItem> placementItem =
                    geometryResolver.resolveGeometry(sourceItem.getBlock(), targetGeometry);
            if (placementItem.isPresent()) {
                candidates.add(new PaletteCandidate(
                        slot,
                        sourceStack.getCount(),
                        sourceItem,
                        placementItem.get()
                ));
            }
        }
        return List.copyOf(candidates);
    }
}

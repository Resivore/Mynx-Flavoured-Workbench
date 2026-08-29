package dev.aero.shulkertrowel.geometry;

import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * Resolves only exact Nibaru material profiles and CNM geometries owned by the
 * accepted integration. ShapeMap equivalence is deliberately not consulted.
 */
public final class CnmNibaruGeometryResolver implements GeometryResolver {
    @Override
    public Optional<BlockItem> resolveGeometry(Block sourceBlock, TargetGeometry targetGeometry) {
        if (targetGeometry == TargetGeometry.FULL) {
            return exactBlockItem(sourceBlock);
        }

        Optional<NibaruMaterialProfile> profile = NibaruProviderAdapter.profile(sourceBlock)
                .or(() -> NibaruProviderAdapter.runtimeBinding(sourceBlock)
                        .map(NibaruProviderAdapter.RuntimeBinding::profile));
        if (profile.isEmpty()) return Optional.empty();

        Optional<Block> target = switch (targetGeometry) {
            case FULL -> Optional.of(sourceBlock);
            case SLAB -> profile.get().effectiveSlabSource();
            case STAIR -> profile.get().effectiveStairSource();
            case WALL -> profile.get().nativeWall();
            case VERTICAL_SLAB -> NibaruProviderAdapter.derived(
                    profile.get(), DerivedGeometrySupport.Geometry.VERTICAL_SLAB);
            case STEP -> NibaruProviderAdapter.derived(
                    profile.get(), DerivedGeometrySupport.Geometry.STEP);
        };
        return target.flatMap(CnmNibaruGeometryResolver::exactBlockItem);
    }

    private static Optional<BlockItem> exactBlockItem(Block block) {
        Item item = block.asItem();
        return item instanceof BlockItem blockItem && blockItem.getBlock() == block
                ? Optional.of(blockItem)
                : Optional.empty();
    }
}

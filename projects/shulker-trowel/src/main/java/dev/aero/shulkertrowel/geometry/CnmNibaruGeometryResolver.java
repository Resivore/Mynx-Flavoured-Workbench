package dev.aero.shulkertrowel.geometry;

import dev.aero.cnmterraincompat.BgeGeometryCatalog;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * Resolves only exact Nibaru material profiles, Trowel-native roles, and BGE
 * catalog descriptors. ShapeMap equivalence is deliberately not consulted.
 */
public final class CnmNibaruGeometryResolver implements GeometryResolver {
    @Override
    public Optional<BlockItem> resolveGeometry(Block sourceBlock, TargetGeometry targetGeometry) {
        if (targetGeometry.nativeRole().orElse(null) == TargetGeometry.NativeRole.FULL) {
            return exactBlockItem(sourceBlock);
        }

        Optional<NibaruMaterialProfile> profile = NibaruProviderAdapter.profile(sourceBlock)
                .or(() -> NibaruProviderAdapter.runtimeBinding(sourceBlock)
                        .map(NibaruProviderAdapter.RuntimeBinding::profile));
        if (profile.isEmpty()) return Optional.empty();

        Optional<BlockItem> target = targetGeometry.nativeRole()
                .flatMap(role -> switch (role) {
                    case FULL -> exactBlockItem(sourceBlock);
                    case SLAB -> profile.get().effectiveSlabSource()
                            .flatMap(CnmNibaruGeometryResolver::exactBlockItem);
                    case STAIR -> profile.get().effectiveStairSource()
                            .flatMap(CnmNibaruGeometryResolver::exactBlockItem);
                    case WALL -> profile.get().nativeWall()
                            .flatMap(CnmNibaruGeometryResolver::exactBlockItem);
                })
                .or(() -> targetGeometry.bgeDescriptor()
                        .flatMap(descriptor -> resolveCatalogItem(profile.get(), descriptor)));
        return target;
    }

    private static Optional<BlockItem> resolveCatalogItem(NibaruMaterialProfile profile,
            BgeGeometryCatalog.Descriptor descriptor) {
        if (!descriptor.isAvailable(profile)) return Optional.empty();

        Optional<Block> resolvedBlock = descriptor.resolveBlock(profile);
        Optional<Item> resolvedItem = descriptor.resolveItem(profile);
        if (resolvedBlock.isEmpty() || resolvedItem.isEmpty()) return Optional.empty();
        Item item = resolvedItem.get();
        return item instanceof BlockItem blockItem && blockItem.getBlock() == resolvedBlock.get()
                ? Optional.of(blockItem)
                : Optional.empty();
    }

    private static Optional<BlockItem> exactBlockItem(Block block) {
        Item item = block.asItem();
        return item instanceof BlockItem blockItem && blockItem.getBlock() == block
                ? Optional.of(blockItem)
                : Optional.empty();
    }
}

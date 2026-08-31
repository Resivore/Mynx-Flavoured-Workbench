package dev.aero.shulkertrowel.client;

import dev.aero.shulkertrowel.geometry.CnmNibaruGeometryResolver;
import dev.aero.shulkertrowel.geometry.TargetGeometry;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Actual representative geometry items in the trowel catalog's stable selector order. */
final class TrowelGeometryIcons {
    private static final CnmNibaruGeometryResolver RESOLVER = new CnmNibaruGeometryResolver();
    private static volatile List<Item> items;

    private TrowelGeometryIcons() {}

    static List<Item> items() {
        List<Item> current = items;
        if (current != null) return current;

        current = TargetGeometry.ordered().stream().map(TrowelGeometryIcons::representativeItem).toList();
        items = current;
        return current;
    }

    private static Item representativeItem(TargetGeometry geometry) {
        return RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, geometry)
                .or(() -> geometry.bgeDescriptor().flatMap(ignored ->
                        NibaruMaterialProfiles.all().stream()
                                .map(profile -> RESOLVER.resolveGeometry(
                                        profile.canonicalParent(), geometry))
                                .flatMap(java.util.Optional::stream)
                                .findFirst()))
                .orElseThrow(() -> new IllegalStateException(
                        "No exact material exposes catalog geometry " + geometry.key()))
                .asItem();
    }

    static ItemStack stack(TargetGeometry geometry) {
        return new ItemStack(items().get(geometry.selectorIndex()));
    }

    static ItemStack stack(int selectorIndex) {
        return stack(TargetGeometry.fromSelectorIndex(selectorIndex).orElse(TargetGeometry.FULL));
    }
}

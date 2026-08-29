package dev.aero.shulkertrowel.client;

import dev.aero.shulkertrowel.geometry.CnmNibaruGeometryResolver;
import dev.aero.shulkertrowel.geometry.TargetGeometry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.List;

/** Stable representative items rendered inside CNM's existing six-slot overlay. */
final class TrowelGeometryIcons {
    private static final CnmNibaruGeometryResolver RESOLVER = new CnmNibaruGeometryResolver();
    private static volatile List<Item> items;

    private TrowelGeometryIcons() {}

    static List<Item> items() {
        List<Item> current = items;
        if (current != null) return current;

        current = Arrays.stream(TargetGeometry.values())
                .map(geometry -> RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, geometry)
                        .orElseThrow(() -> new IllegalStateException(
                                "Accepted Nibaru stack did not expose oak-planks " + geometry))
                        .asItem())
                .toList();
        items = current;
        return current;
    }

    static ItemStack stack(TargetGeometry geometry) {
        return new ItemStack(items().get(geometry.networkId()));
    }

    static ItemStack stack(int geometryId) {
        return stack(TargetGeometry.byNetworkId(geometryId));
    }
}

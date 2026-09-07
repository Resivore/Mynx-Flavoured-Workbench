package tempeststudios.quickstacknearby;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

/** Optional Double Barrels public-contract bridge; no provider classes are linked into C10. */
final class DoubleBarrelCompat {
    private DoubleBarrelCompat() {}
    static Resolved resolve(ServerLevel level, BlockPos position, Container container) {
        if (!(container instanceof BarrelBlockEntity barrel) || !FabricLoader.getInstance().isModLoaded("doublebarrels")) return null;
        try {
            Class<?> api = Class.forName("com.mozko.doublebarrels.DoubleBarrelAccess");
            Method connected = api.getMethod("isConnected");
            if (!(boolean) connected.invoke(barrel)) return null;
            BlockPos partner = (BlockPos) api.getMethod("getConnectionPos").invoke(barrel);
            if (partner == null || !level.isLoaded(partner)) return null;
            Container combined = (Container) api.getMethod("getCombinedInventory").invoke(barrel);
            if (combined == null) return null;
            List<BlockPos> positions = List.of(position.immutable(), partner.immutable()).stream().sorted(
                    Comparator.comparingInt((BlockPos value) -> value.getX())
                            .thenComparingInt(value -> value.getY()).thenComparingInt(value -> value.getZ())).toList();
            return new Resolved(combined, positions);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Loaded Double Barrels lacks the audited public inventory contract", e);
        }
    }
    record Resolved(Container container, List<BlockPos> positions) {}
}

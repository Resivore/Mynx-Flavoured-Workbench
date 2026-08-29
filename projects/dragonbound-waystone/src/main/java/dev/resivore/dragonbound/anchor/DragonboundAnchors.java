package dev.resivore.dragonbound.anchor;

import dev.resivore.dragonbound.DragonboundWaystone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.Optional;

/**
 * Small server-facing API for the shared Dragonbound anchor.
 */
public final class DragonboundAnchors {
    private DragonboundAnchors() {
    }

    /**
     * Uses the server's global data storage, not a dimension-local storage.
     */
    public static DragonboundAnchorData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.getDataStorage().computeIfAbsent(DragonboundAnchorData.TYPE);
    }

    public static Optional<AnchorBinding> active(MinecraftServer server) {
        return get(server).active();
    }

    public static AnchorBinding bind(ServerLevel level, BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");

        DragonboundAnchorData data = get(level.getServer());
        Optional<AnchorBinding> previous = data.active();
        AnchorBinding replacement = data.bind(level.dimension(), pos);

        previous.filter(binding -> !sameLocation(binding, replacement)).ifPresent(binding ->
                DragonboundWaystone.LOGGER.warn(
                        "Dragonbound Waystone at {} {} superseded still-physical anchor at {} {}; only generation {} is authoritative",
                        replacement.dimension().identifier(),
                        replacement.pos(),
                        binding.dimension().identifier(),
                        binding.pos(),
                        replacement.generation()));

        return replacement;
    }

    public static boolean clearIfMatching(ServerLevel level, BlockPos pos) {
        Objects.requireNonNull(level, "level");
        return get(level.getServer()).clearIfMatching(level.dimension(), pos);
    }

    public static boolean matches(MinecraftServer server, AnchorBinding expected) {
        return get(server).matches(expected);
    }

    private static boolean sameLocation(AnchorBinding first, AnchorBinding second) {
        return first.dimension().equals(second.dimension()) && first.pos().equals(second.pos());
    }
}

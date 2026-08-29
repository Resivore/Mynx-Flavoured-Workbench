package dev.aero.shulkertrowel.sound;

/**
 * Marks only the server-authoritative BlockItem placement performed by the
 * trowel. Minecraft's canonical sound call can then include the placing player
 * in its single server broadcast instead of assuming client prediction.
 */
public final class PlacementSoundBroadcastScope implements AutoCloseable {
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

    private boolean closed;

    private PlacementSoundBroadcastScope() {
    }

    public static PlacementSoundBroadcastScope open() {
        Integer depth = DEPTH.get();
        DEPTH.set(depth == null ? 1 : depth + 1);
        return new PlacementSoundBroadcastScope();
    }

    public static <T> T routeExcludedSource(T originalSource) {
        return isActive() ? null : originalSource;
    }

    static boolean isActive() {
        Integer depth = DEPTH.get();
        return depth != null && depth > 0;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        Integer depth = DEPTH.get();
        if (depth == null) {
            return;
        }
        if (depth <= 1) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth - 1);
        }
    }
}

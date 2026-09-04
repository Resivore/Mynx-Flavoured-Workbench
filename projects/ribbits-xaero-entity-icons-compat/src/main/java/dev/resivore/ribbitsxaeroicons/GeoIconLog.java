package dev.resivore.ribbitsxaeroicons;

import java.lang.System.Logger.Level;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded diagnostics: each failure category is emitted once per resource generation. */
public final class GeoIconLog {
    private static final System.Logger LOGGER =
            System.getLogger("ribbits_xaero_entity_icons_compat");
    private static final Set<String> EMITTED = ConcurrentHashMap.newKeySet();

    private GeoIconLog() {
    }

    public static void activation(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void failure(String category, Throwable failure) {
        String key = ReloadGeneration.current() + ":" + category;
        if (EMITTED.add(key)) {
            LOGGER.log(Level.WARNING, "Ribbits Xaero icon provider declined [" + category + "]", failure);
        }
    }

    public static void failure(String category, String detail) {
        String key = ReloadGeneration.current() + ":" + category;
        if (EMITTED.add(key)) {
            LOGGER.log(Level.WARNING,
                    "Ribbits Xaero icon provider declined [" + category + "]: " + detail);
        }
    }

    public static void reloaded(long generation, boolean evicted) {
        EMITTED.clear();
        LOGGER.log(Level.INFO, "Ribbits Xaero icon resources advanced to generation "
                + generation + "; Ribbit cache evicted=" + evicted);
    }
}

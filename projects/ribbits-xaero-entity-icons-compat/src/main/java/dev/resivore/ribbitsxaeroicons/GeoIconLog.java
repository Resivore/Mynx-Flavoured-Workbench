package dev.resivore.ribbitsxaeroicons;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Bounded structural events, routed through the normal Fabric client log. */
public final class GeoIconLog {
    private static final Logger LOGGER = LogManager.getLogger("ribbits_xaero_entity_icons_compat");
    private static final Set<String> EMITTED = ConcurrentHashMap.newKeySet();
    private static final int LIMIT = 2048;
    private GeoIconLog() {}
    public static void activation(String message) { LOGGER.info(message); }
    public static void stage(String stage, Object identity, String detail) {
        String key = ReloadGeneration.current() + ":" + stage + ":" + identity;
        if (EMITTED.size() < LIMIT && EMITTED.add(key)) {
            LOGGER.info("Ribbits icon stage={} identity={} generation={} {}",
                    stage, identity, ReloadGeneration.current(), detail);
        }
    }
    public static void failure(String category, Throwable failure) {
        failure(category, failure.getClass().getSimpleName() + ": " + failure.getMessage());
    }
    public static void failure(String category, String detail) {
        stage("rejected:" + category, "ribbits:ribbit", detail);
    }
    public static void reloaded(long generation, boolean evicted) {
        EMITTED.clear();
        LOGGER.info("Ribbits icon cache invalidated generation={} successfulAndFailedEvicted={}", generation, evicted);
    }
}

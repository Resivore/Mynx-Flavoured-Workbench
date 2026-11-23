package dev.resivore.bgecomplementary;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reports a compatibility failure once per session. */
public final class BgeComplementaryLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("bge-complementary");
    private static final AtomicBoolean UNAVAILABLE_CANONICAL_BINDING_API_LOGGED = new AtomicBoolean();

    private BgeComplementaryLog() {}

    /** Logs one controlled no-op when BGE's late-bound contract cannot be consumed safely. */
    public static void unavailableCanonicalBindingApi() {
        if (UNAVAILABLE_CANONICAL_BINDING_API_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("BGE × Complementary bridge inactive: BGE's canonical-binding API is "
                    + "unavailable or incompatible; leaving Iris maps unchanged.");
        }
    }
}

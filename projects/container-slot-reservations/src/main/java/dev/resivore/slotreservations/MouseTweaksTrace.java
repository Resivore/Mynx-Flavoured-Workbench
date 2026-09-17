package dev.resivore.slotreservations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Narrow, opt-in trace for the Mouse Tweaks/CSR handoff. Enable with
 * {@code -Dcontainer_slot_reservations.debugMouseTweaks=true}.
 */
public final class MouseTweaksTrace {
    public static final String PROPERTY = "container_slot_reservations.debugMouseTweaks";
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ContainerSlotReservations.MOD_ID + "/mouse_tweaks");

    private MouseTweaksTrace() {}

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static void event(int stage, String event, String detail) {
        if (enabled()) {
            LOGGER.info("[CSR Mouse Tweaks {}/18] {} — {}", stage, event, detail);
        }
    }
}

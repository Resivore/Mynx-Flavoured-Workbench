package dev.aero.shulkertrowel.client;

import dev.aero.shulkertrowel.ShulkerTrowel;
import dev.tazer.clutternomore.ClutterNoMoreClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reads CNM's own input settings without creating a second config or key binding. */
final class CnmInputSettings {
    private static boolean warned;

    private CnmInputSettings() {}

    static String inputType() {
        Object value = trackedValue("HOLD");
        return value instanceof Enum<?> inputType ? inputType.name() : "HOLD";
    }

    static boolean wrapScrolling() {
        Object value = trackedValue("WRAP_SCROLLING");
        return value instanceof Boolean enabled && enabled;
    }

    private static Object trackedValue(String fieldName) {
        try {
            Field configField = ClutterNoMoreClient.class.getField("CLIENT_CONFIG");
            Object config = configField.get(null);
            Field trackedField = config.getClass().getField(fieldName);
            Object tracked = trackedField.get(config);
            Class<?> trackedValue = Class.forName(
                    "folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue",
                    false,
                    tracked.getClass().getClassLoader()
            );
            Method value = trackedValue.getMethod("value");
            return value.invoke(tracked);
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!warned) {
                warned = true;
                ShulkerTrowel.LOGGER.warn(
                        "Could not read CNM input settings; using conservative defaults",
                        exception
                );
            }
            return null;
        }
    }
}

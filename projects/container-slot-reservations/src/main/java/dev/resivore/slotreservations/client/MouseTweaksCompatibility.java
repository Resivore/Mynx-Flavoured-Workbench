package dev.resivore.slotreservations.client;

import net.fabricmc.loader.api.FabricLoader;

/** Absent-safe detection of Mouse Tweaks' configured native RMB drag path. */
final class MouseTweaksCompatibility {
    private static final String MOD_ID = "mousetweaks";

    private MouseTweaksCompatibility() {}

    static boolean isActive() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /** CSR keeps its established right-drag behavior when Mouse Tweaks has that tweak disabled. */
    static boolean ownsRightDrag() {
        if (!isActive()) return false;
        try {
            Class<?> main = Class.forName("yalter.mousetweaks.Main", false,
                    MouseTweaksCompatibility.class.getClassLoader());
            Object config = main.getField("config").get(null);
            return config != null && main.getClassLoader()
                    .loadClass("yalter.mousetweaks.Config").getField("rmbTweak").getBoolean(config);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}

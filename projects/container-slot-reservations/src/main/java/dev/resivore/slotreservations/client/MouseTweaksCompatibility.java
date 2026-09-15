package dev.resivore.slotreservations.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;

/** Optional, reflection-only wheel handoff to Mouse Tweaks' public client entrypoint. */
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

    static boolean consumeWheel(Screen screen, double mouseX, double mouseY, double vertical) {
        if (!isActive()) return false;
        try {
            Class<?> main = Class.forName("yalter.mousetweaks.Main", false,
                    MouseTweaksCompatibility.class.getClassLoader());
            Method method = main.getMethod("onMouseScrolled", Screen.class,
                    double.class, double.class, double.class);
            return Boolean.TRUE.equals(method.invoke(null, screen, mouseX, mouseY, vertical));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // An incompatible optional provider must leave established CSR scrolling intact.
            return false;
        }
    }
}

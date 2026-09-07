package tempeststudios.quickstacknearby;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ClientScreenCompat {
    private ClientScreenCompat() {
    }

    public static void setScreen(Minecraft client, Screen screen) {
        if (trySetScreen(client, screen)) {
            return;
        }
        if (client.gui != null && trySetScreen(client.gui, screen)) {
            return;
        }
        throw new IllegalStateException("No compatible screen setter found for this Minecraft 26.x runtime.");
    }

    private static boolean trySetScreen(Object target, Screen screen) {
        try {
            Method method = target.getClass().getMethod("setScreen", Screen.class);
            method.invoke(target, screen);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to open Quick Stack rules screen.", e);
        }
    }
}

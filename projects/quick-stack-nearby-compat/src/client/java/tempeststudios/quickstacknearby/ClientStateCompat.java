package tempeststudios.quickstacknearby;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class ClientStateCompat {
    private ClientStateCompat() {
    }

    static boolean hasSingleplayerServer(Minecraft client) {
        return client.hasSingleplayerServer();
    }

    static boolean isScreenOpen(Minecraft client) {
        Object gui = fieldValue(client, "gui");
        Object screen = gui != null ? invoke(gui, "screen") : null;
        if (screen != null) {
            return true;
        }
        return fieldValue(client, "screen") != null;
    }

    private static Object fieldValue(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getField(name);
            return field.get(target);
        } catch (NoSuchFieldException ignored) {
            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException | IllegalAccessException ignoredAgain) {
                return null;
            }
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (NoSuchMethodException ignored) {
            try {
                Method method = target.getClass().getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignoredAgain) {
                return null;
            }
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }
}

package dev.resivore.offhandqol.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class OffhandCarriedCompatibility {
    private static final String CARRIED_MOD_ID = "carried_container_auto_routing";
    private static final Method ROUTE_INCOMING = findRoute("routeIncoming");
    private static final Method ROUTE_PLAYER_ORIGIN = findRoute("routePlayerOrigin");

    private OffhandCarriedCompatibility() {}

    public static boolean isAvailable() {
        return ROUTE_INCOMING != null;
    }

    public static boolean isPlayerOriginAvailable() {
        return ROUTE_PLAYER_ORIGIN != null;
    }

    public static boolean routeIncoming(
            Player player,
            ItemStack incoming,
            int excludedInventorySlot,
            boolean excludeOffhand
    ) {
        return invoke(ROUTE_INCOMING, player, incoming, excludedInventorySlot, excludeOffhand);
    }

    public static boolean routePlayerOrigin(
            Player player,
            ItemStack incoming,
            int excludedInventorySlot,
            boolean excludeOffhand
    ) {
        return invoke(ROUTE_PLAYER_ORIGIN, player, incoming, excludedInventorySlot, excludeOffhand);
    }

    private static boolean invoke(Method route, Player player, ItemStack incoming,
                                  int excludedInventorySlot, boolean excludeOffhand) {
        if (route == null || incoming.isEmpty()) return false;
        try {
            return (boolean) route.invoke(null, player, incoming, excludedInventorySlot, excludeOffhand);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Carried Routing compatibility hook became inaccessible", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Carried Routing compatibility hook failed", cause);
        }
    }

    private static Method findRoute(String name) {
        if (!FabricLoader.getInstance().isModLoaded(CARRIED_MOD_ID)) return null;
        try {
            return Class.forName("dev.resivore.carriedrouting.api.OffhandCompatibilityHook")
                    .getMethod(name, Player.class, ItemStack.class, int.class, boolean.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        }
    }
}

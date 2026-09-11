package dev.resivore.quickstacknearbycompat.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/** Class-loading-safe optional bridge to CCAR's public lock contract. */
final class RoutingLockResolver {
    static final String CCAR_MOD_ID = "carried_container_auto_routing";
    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingLockResolver.class);
    private static final AtomicBoolean REPORTED_FAILURE = new AtomicBoolean();
    private static volatile Method isLocked;
    private static volatile boolean failed;

    private RoutingLockResolver() {}

    /** CCAR absent means unlocked; a loaded but un-linkable provider fails closed. */
    static boolean mayDrain(ItemStack carrier) {
        if (!FabricLoader.getInstance().isModLoaded(CCAR_MOD_ID)) return true;
        if (failed) return false;
        try {
            Method method = isLocked;
            if (method == null) {
                method = Class.forName("dev.resivore.carriedrouting.RoutingLock")
                        .getMethod("isLocked", ItemStack.class);
                isLocked = method;
            }
            return !((Boolean) method.invoke(null, carrier));
        } catch (ReflectiveOperationException | LinkageError | ClassCastException failure) {
            failed = true;
            if (REPORTED_FAILURE.compareAndSet(false, true)) {
                LOGGER.error("CCAR is loaded but RoutingLock.isLocked(ItemStack) is unavailable; "
                        + "carried-container QSN sources are disabled for safety", failure);
            }
            return false;
        }
    }
}

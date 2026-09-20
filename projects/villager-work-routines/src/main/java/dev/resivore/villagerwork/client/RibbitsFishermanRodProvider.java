package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

/**
 * Optional reflection-only edge to Ribbits' client-owned Fisherman rod visual.
 *
 * <p>VWR deliberately does not compile against Ribbits or package any of its protected model or
 * texture material. A missing provider, missing private resource payload, or failed provider call
 * simply leaves the existing stick fallback available.</p>
 */
final class RibbitsFishermanRodProvider {
    private static final String BRIDGE_CLASS =
            "com.yungnickyoung.minecraft.ribbits.client.render.RibbitsFishermanRodBridge";
    private static Method availableMethod;
    private static Method submitMethod;
    private static boolean lookedUp;

    private RibbitsFishermanRodProvider() {
    }

    static boolean isAvailable() {
        resolve();
        if (availableMethod == null) return false;
        try {
            return Boolean.TRUE.equals(availableMethod.invoke(null));
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return false;
        }
    }

    static Vec3 submit(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        resolve();
        if (submitMethod == null) return null;
        try {
            Object result = submitMethod.invoke(null, poseStack, collector, light);
            return result instanceof Vec3 tip ? tip : null;
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return null;
        }
    }

    private static void resolve() {
        if (lookedUp) return;
        lookedUp = true;
        try {
            Class<?> bridge = Class.forName(BRIDGE_CLASS, false,
                    RibbitsFishermanRodProvider.class.getClassLoader());
            availableMethod = bridge.getMethod("isAvailable");
            submitMethod = bridge.getMethod("submit", PoseStack.class, SubmitNodeCollector.class, int.class);
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError ignored) {
            availableMethod = null;
            submitMethod = null;
        }
    }
}

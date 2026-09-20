package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.SubmitNodeCollector;

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
    private static Method armLocalTipMethod;
    private static Method tipXMethod;
    private static Method tipYMethod;
    private static Method tipZMethod;
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

    /** Submits the provider-owned rod and reports whether the actual visual was accepted. */
    static boolean submit(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        resolve();
        if (submitMethod == null) return false;
        try {
            Object result = submitMethod.invoke(null, poseStack, collector, light);
            return result != null;
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * The provider's explicit physical shaft endpoint relative to the villager crossed-arms grip.
     * This is model/arm-local space, never an implied generic vector or a camera-relative point.
     */
    static ArmLocalRodTip armLocalTip() {
        resolve();
        if (armLocalTipMethod == null || tipXMethod == null || tipYMethod == null || tipZMethod == null)
            return null;
        try {
            Object result = armLocalTipMethod.invoke(null);
            if (result == null) return null;
            Object x = tipXMethod.invoke(result);
            Object y = tipYMethod.invoke(result);
            Object z = tipZMethod.invoke(result);
            if (!(x instanceof Number xNumber) || !(y instanceof Number yNumber)
                    || !(z instanceof Number zNumber)) return null;
            ArmLocalRodTip tip = new ArmLocalRodTip(xNumber.floatValue(), yNumber.floatValue(),
                    zNumber.floatValue());
            return tip.isFinite() ? tip : null;
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
            armLocalTipMethod = bridge.getMethod("armLocalTip");
            Class<?> tipType = Class.forName(BRIDGE_CLASS + "$ArmLocalRodTip", false,
                    RibbitsFishermanRodProvider.class.getClassLoader());
            tipXMethod = tipType.getMethod("x");
            tipYMethod = tipType.getMethod("y");
            tipZMethod = tipType.getMethod("z");
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError ignored) {
            availableMethod = null;
            submitMethod = null;
            armLocalTipMethod = null;
            tipXMethod = null;
            tipYMethod = null;
            tipZMethod = null;
        }
    }

    record ArmLocalRodTip(float x, float y, float z) {
        boolean isFinite() {
            return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z);
        }
    }
}

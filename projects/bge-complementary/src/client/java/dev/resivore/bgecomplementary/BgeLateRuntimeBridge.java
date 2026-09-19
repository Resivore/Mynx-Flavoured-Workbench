package dev.resivore.bgecomplementary;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Executes the BGE contract check and direct bridge calls only from Iris's completed-map RETURN
 * hooks. This class is never reachable from Mixin configuration/bootstrap.
 */
public final class BgeLateRuntimeBridge {
    private BgeLateRuntimeBridge() {}

    public static void inheritMaterialIds(Object2IntMap<BlockState> materialIds) {
        if (!runIfSupported(BgeCanonicalBindingApi::isAvailable,
                () -> BgeComplementaryLog.materialMap(
                        BgeShaderMaterialBridge.inheritMaterialIds(materialIds)))) {
            BgeComplementaryLog.unavailableCanonicalBindingApi();
        }
    }

    public static void inheritLayerTypes(Map<Block, Object> layerTypes) {
        if (!runIfSupported(BgeCanonicalBindingApi::isAvailable,
                () -> BgeComplementaryLog.layerMap(
                        BgeShaderMaterialBridge.inheritLayerTypes(layerTypes)))) {
            BgeComplementaryLog.unavailableCanonicalBindingApi();
        }
    }

    /**
     * The direct BGE bridge is evaluated only after the reflected contract has succeeded. A
     * changed future API can still fail linkage between the probe and invocation; make that an
     * inert bridge rather than leaking a bootstrap/runtime linkage failure into Iris.
     */
    static boolean runIfSupported(BooleanSupplier capability, Runnable bridge) {
        try {
            if (!capability.getAsBoolean()) return false;
            bridge.run();
            return true;
        } catch (LinkageError ignored) {
            return false;
        }
    }
}

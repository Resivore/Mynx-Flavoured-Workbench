package dev.resivore.bgecomplementary;

import net.fabricmc.loader.api.FabricLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Aggregate-only diagnostics: one material-map line and, when relevant, one layer-map line. */
public final class BgeComplementaryLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("bge-complementary");
    private static final AtomicBoolean UNAVAILABLE_CANONICAL_BINDING_API_LOGGED = new AtomicBoolean();

    private BgeComplementaryLog() {}

    public static void materialMap(ShaderMaterialInheritance.Result result) {
        LOGGER.info("BGE × Complementary bridge active (Iris {}, BGE {}): inherited {} material states; "
                        + "explicit {}, missing parent {}, missing projection {}.",
                version("iris"), version("cnm_terrain_slabs_compat"), result.inherited(),
                result.explicitPhysical(), result.missingParent(), result.missingCanonical());
    }

    public static void layerMap(ShaderMaterialInheritance.Result result) {
        if (result.inherited() == 0 && result.explicitPhysical() == 0) return;
        LOGGER.info("BGE × Complementary bridge inherited {} Iris layer.* block classifications; "
                        + "explicit BGE layer entries retained: {}.",
                result.inherited(), result.explicitPhysical());
    }

    /** Logs one controlled no-op when BGE's late-bound contract cannot be consumed safely. */
    public static void unavailableCanonicalBindingApi() {
        if (UNAVAILABLE_CANONICAL_BINDING_API_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("BGE × Complementary bridge inactive: BGE's canonical-binding API is "
                    + "unavailable or incompatible; leaving Iris maps unchanged.");
        }
    }

    private static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("absent");
    }
}

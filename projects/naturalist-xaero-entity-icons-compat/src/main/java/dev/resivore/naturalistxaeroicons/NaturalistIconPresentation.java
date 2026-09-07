package dev.resivore.naturalistxaeroicons;

import net.minecraft.world.entity.Entity;

/** Per-render-call context for the one native Xaero capture that needs only framing correction. */
public final class NaturalistIconPresentation {
    private static final ThreadLocal<NaturalistModelContracts.Presentation> NATIVE_PRESENTATION = new ThreadLocal<>();

    private NaturalistIconPresentation() {}

    public static void begin(Entity entity) {
        if (NaturalistModelContracts.isNativePresentationOverride(entity)) {
            NATIVE_PRESENTATION.set(NaturalistModelContracts.nativePresentation(entity));
        }
    }

    public static NaturalistModelContracts.Presentation currentNativePresentation() {
        return NATIVE_PRESENTATION.get();
    }

    public static void end() {
        NATIVE_PRESENTATION.remove();
    }
}

package dev.resivore.slotreservations.client;

/** Mixin bridge carrying per-item alpha from extraction to the later GUI atlas blit. */
public interface GhostGuiItemRenderStateAccess {
    int containerSlotReservations$getAlpha();

    void containerSlotReservations$setAlpha(int alpha);
}

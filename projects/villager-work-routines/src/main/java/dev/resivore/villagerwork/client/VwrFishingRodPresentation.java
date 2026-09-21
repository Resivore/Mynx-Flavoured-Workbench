package dev.resivore.villagerwork.client;

/** Client render-state identity only; it never exposes or mutates server WorkCoordinator state. */
public interface VwrFishingRodPresentation {
    int villagerWork$entityId();
    void villagerWork$setEntityId(int entityId);
    float villagerWork$partialTick();
    void villagerWork$setPartialTick(float partialTick);
}

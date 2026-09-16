package dev.resivore.villagerwork.client;

/** Client render-state data only; it never exposes or mutates server WorkCoordinator state. */
public interface VwrFishingRodPresentation {
    boolean villagerWork$renderFishingRod();
    void villagerWork$setRenderFishingRod(boolean renderFishingRod);
    int villagerWork$entityId();
    void villagerWork$setEntityId(int entityId);
}

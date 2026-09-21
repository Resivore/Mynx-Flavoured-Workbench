package dev.resivore.villagerwork.mixin.client;

import dev.resivore.villagerwork.client.VwrFishingRodPresentation;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VillagerRenderState.class)
abstract class VillagerRenderStateMixin implements VwrFishingRodPresentation {
    @Unique private int villagerWork$entityId = -1;

    @Override public int villagerWork$entityId() { return villagerWork$entityId; }

    @Override public void villagerWork$setEntityId(int entityId) { villagerWork$entityId = entityId; }
}

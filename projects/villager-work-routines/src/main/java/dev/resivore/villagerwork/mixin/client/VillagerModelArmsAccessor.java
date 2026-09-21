package dev.resivore.villagerwork.mixin.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the active CEM-replaced arms root so VWR can follow its real folded-hands child. */
@Mixin(VillagerModel.class)
public interface VillagerModelArmsAccessor {
    @Accessor("arms")
    ModelPart villagerWork$getArms();
}

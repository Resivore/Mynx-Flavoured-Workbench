package dev.resivore.villagerwork.mixin.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only diagnostic access to the actual arms part supplied to the VWR render layer. */
@Mixin(VillagerModel.class)
public interface VillagerModelArmsAccessor {
    @Accessor("arms")
    ModelPart villagerWork$getArms();
}

package dev.resivore.slabdecorations.mixin.client;

import dev.resivore.slabdecorations.SlabPlantRaycast;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityPickMixin {
    @Inject(method = "pick", at = @At("RETURN"), cancellable = true)
    private void slabDecorations$preferVisibleShiftedPlant(
            double range,
            float partialTick,
            boolean withLiquids,
            CallbackInfoReturnable<HitResult> cir) {
        Entity entity = (Entity) (Object) this;
        Vec3 from = entity.getEyePosition(partialTick);
        Vec3 direction = entity.getViewVector(partialTick);
        Vec3 to = from.add(direction.scale(range));
        cir.setReturnValue(SlabPlantRaycast.preferShiftedPlant(entity.level(), from, to, cir.getReturnValue()));
    }
}

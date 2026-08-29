package dev.resivore.sweetberryhorseimmunity.mixin;

import dev.resivore.sweetberryhorseimmunity.SweetBerryHorseImmunity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SweetBerryBushBlock.class)
abstract class SweetBerryBushBlockMixin {
    private static final String ENTITY_INSIDE_METHOD = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V";

    @Inject(method = ENTITY_INSIDE_METHOD, at = @At("HEAD"), cancellable = true, require = 1)
    private void sweetBerryHorseImmunity$bypassBerryEffects(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effectApplier,
        boolean isPrecise,
        CallbackInfo callbackInfo
    ) {
        Entity vehicle = entity.getVehicle();
        if (SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(
            entity.getType(),
            entity instanceof Player,
            vehicle == null ? null : vehicle.getType()
        )) {
            entity.resetFallDistance();
            callbackInfo.cancel();
        }
    }
}

package dev.resivore.sweetberryhorseimmunity.mixin;

import dev.resivore.sweetberryhorseimmunity.SweetBerryHorseImmunity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
abstract class PowderSnowBlockMixin {
    private static final String CAN_ENTITY_WALK_ON_POWDER_SNOW_METHOD =
        "canEntityWalkOnPowderSnow(Lnet/minecraft/world/entity/Entity;)Z";
    private static final String GET_COLLISION_SHAPE_METHOD =
        "getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;";

    @Inject(
        method = CAN_ENTITY_WALK_ON_POWDER_SNOW_METHOD,
        at = @At("HEAD"),
        cancellable = true,
        require = 1
    )
    private static void horseImmunities$walkOnPowderSnow(
        Entity entity,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(entity)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(
        method = GET_COLLISION_SHAPE_METHOD,
        at = @At("HEAD"),
        cancellable = true,
        require = 1
    )
    private void horseImmunities$standOnPowderSnow(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context,
        CallbackInfoReturnable<VoxelShape> callbackInfo
    ) {
        if (context.isPlacement() || !(context instanceof EntityCollisionContext entityContext)) {
            return;
        }

        Entity entity = entityContext.getEntity();
        if (entity != null
            && SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(entity)
            && context.isAbove(Shapes.block(), pos, false)) {
            callbackInfo.setReturnValue(Shapes.block());
        }
    }
}

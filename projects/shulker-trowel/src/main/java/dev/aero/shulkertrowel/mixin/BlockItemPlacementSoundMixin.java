package dev.aero.shulkertrowel.mixin;

import dev.aero.shulkertrowel.sound.PlacementSoundBroadcastScope;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Includes the trowel user in BlockItem's one canonical server sound broadcast. */
@Mixin(BlockItem.class)
abstract class BlockItemPlacementSoundMixin {
    @ModifyArg(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
            ),
            index = 0,
            require = 1
    )
    private Entity shulkerTrowel$includePlacingPlayer(Entity excludedSource) {
        return PlacementSoundBroadcastScope.routeExcludedSource(excludedSource);
    }
}

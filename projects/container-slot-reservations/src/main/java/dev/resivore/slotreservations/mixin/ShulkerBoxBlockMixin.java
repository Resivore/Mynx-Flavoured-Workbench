package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {
    @Redirect(
            method = "playerWillDestroy",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;isEmpty()Z"
            ),
            require = 1
    )
    private boolean containerSlotReservations$isEmptyForCreativeDrop(ShulkerBoxBlockEntity shulker) {
        return shulker.isEmpty()
                && (SupportedContainerResolver.resolve(shulker, 0).isEmpty()
                || ReservationStore.getData(shulker).isEmpty());
    }
}

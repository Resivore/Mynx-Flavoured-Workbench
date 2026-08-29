package dev.resivore.matchabeacon.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.resivore.matchabeacon.runtime.BeaconKindlingCoordinator;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpawnEggItem.class)
abstract class SpawnEggItemMixin {
    @WrapMethod(method = "useOn")
    private InteractionResult matchaBeacon$captureExactSpawnedMarker(
            UseOnContext context,
            Operation<InteractionResult> original) {
        BeaconKindlingCoordinator.beginSpawnEggUse(context);
        try {
            return original.call(context);
        } finally {
            BeaconKindlingCoordinator.endSpawnEggUse(context);
        }
    }
}

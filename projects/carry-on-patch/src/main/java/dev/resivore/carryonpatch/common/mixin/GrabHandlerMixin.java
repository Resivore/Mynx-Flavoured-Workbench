package dev.resivore.carryonpatch.common.mixin;

import dev.resivore.carryonpatch.common.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.chermew.grabandgo.event.GrabHandler", remap = false)
public abstract class GrabHandlerMixin {
    @Inject(method = {"placeEntity", "placeBlock"}, at = @At("HEAD"), cancellable = true, require = 2)
    private static void carryOnPatch$place(Player player, Level level, BlockPos pos, CompoundTag data,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        cir.setReturnValue(CarryPlacement.place(player, level, pos, data));
    }
}

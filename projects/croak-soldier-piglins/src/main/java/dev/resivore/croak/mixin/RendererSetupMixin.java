package dev.resivore.croak.mixin;

import dev.resivore.croak.*;

import net.minecraft.client.renderer.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderers.class)
abstract class RendererSetupMixin {
    @Inject(method = "createEntityRenderers", at = @At("RETURN"))
    private static void croak$reload(EntityRendererProvider.Context context, CallbackInfoReturnable<?> ci) {
        var resource = context.getResourceManager().getResource(SoldierPack.MODEL);
        SoldierRenderer.current = resource.isPresent() && resource.get().source() instanceof SoldierPack
            ? new SoldierRenderer(context) : null;
    }
}

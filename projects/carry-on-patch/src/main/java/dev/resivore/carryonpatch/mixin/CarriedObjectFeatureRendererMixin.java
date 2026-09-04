package dev.resivore.carryonpatch.mixin;

import dev.resivore.carryonpatch.RenderIdAssigningEntityCache;
import java.util.HashMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Installs the assigning map at GrabAndGo's one shared synthetic-entity cache boundary. */
@Mixin(
        targets = "org.chermew.grabandgo.client.render.CarriedObjectFeatureRenderer",
        remap = false
)
abstract class CarriedObjectFeatureRendererMixin {
    @Redirect(
            method = "<clinit>()V",
            at = @At(
                    value = "NEW",
                    target = "Ljava/util/HashMap;",
                    ordinal = 0,
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private static HashMap<String, Entity> carryOnPatch$installAssigningCache() {
        return new RenderIdAssigningEntityCache();
    }
}

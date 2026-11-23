package dev.resivore.croak.mixin;

import dev.resivore.croak.SoldierBinding;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinRenderer.class)
abstract class PiglinRendererMixin implements SoldierBinding.Owner {
    @Unique private SoldierBinding croak$visual;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void croak$setup(EntityRendererProvider.Context context, ModelLayerLocation adult, ModelLayerLocation baby,
                             ArmorModelSet<ModelLayerLocation> adultArmor, ArmorModelSet<ModelLayerLocation> babyArmor,
                             CallbackInfo ci) {
        croak$visual = new SoldierBinding(context);
    }

    @Override public SoldierBinding croak$binding() { return croak$visual; }
}

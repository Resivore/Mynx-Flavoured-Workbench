package dev.resivore.mynxfloratrades.mixin;

import dev.resivore.mynxfloratrades.FloristRegistry;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PoiType.class)
abstract class PoiTypeMixin {
    @Inject(method = "is", at = @At("RETURN"), cancellable = true)
    private void mynxFloraTrades$acceptAllFlowerPotVariants(BlockState state,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && FloristRegistry.isFloristPoi((PoiType) (Object) this)
                && FloristRegistry.isFlowerPot(state)) {
            cir.setReturnValue(true);
        }
    }
}

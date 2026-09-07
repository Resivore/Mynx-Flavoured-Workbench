package dev.resivore.mynxfloratrades.mixin;

import dev.resivore.mynxfloratrades.FloristRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PoiTypes.class)
abstract class PoiTypesMixin {
    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void mynxFloraTrades$registerFlorist(Registry<PoiType> registry,
                                                          CallbackInfoReturnable<PoiType> cir) {
        FloristRegistry.registerPoi(registry);
    }

    @Inject(method = "forState", at = @At("RETURN"), cancellable = true)
    private static void mynxFloraTrades$recognizeDynamicFlowerPots(BlockState state,
                                                                     CallbackInfoReturnable<Optional<Holder<PoiType>>> cir) {
        if (cir.getReturnValue().isEmpty() && FloristRegistry.isFlowerPot(state)) {
            cir.setReturnValue(Optional.of(FloristRegistry.floristHolder()));
        }
    }
}

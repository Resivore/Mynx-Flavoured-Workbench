package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ProviderStickyMaterialSemantics;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Widens only Fabric's exact sticky-material classification for provider-bound CNM geometry. */
@Mixin(PistonStructureResolver.class)
abstract class ProviderPistonStickyMixin {
    @Inject(method = "isSticky", at = @At("HEAD"), cancellable = true)
    private static void cnmTerrainCompat$recognizeProviderGeometry(BlockState state,
            CallbackInfoReturnable<Boolean> cir) {
        if (ProviderStickyMaterialSemantics.isDerivedSticky(state)) cir.setReturnValue(true);
    }

    @Inject(method = "canStickToEachOther", at = @At("HEAD"), cancellable = true)
    private static void cnmTerrainCompat$preserveHoneySlimeSeparation(BlockState state, BlockState adjacent,
            CallbackInfoReturnable<Boolean> cir) {
        if (ProviderStickyMaterialSemantics.isDerivedSticky(state)
                || ProviderStickyMaterialSemantics.isDerivedSticky(adjacent))
            cir.setReturnValue(!ProviderStickyMaterialSemantics.opposed(state, adjacent));
    }
}

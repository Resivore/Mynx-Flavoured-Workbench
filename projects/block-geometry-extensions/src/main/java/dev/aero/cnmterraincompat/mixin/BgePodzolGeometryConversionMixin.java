package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.PodzolGeometryConversion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves local Corner/Column identity through Nibaru's typed Podzol transition. */
@Mixin(value = PodzolGeometryConversion.class, remap = false)
abstract class BgePodzolGeometryConversionMixin {
    @Inject(method = "convert", at = @At("HEAD"), cancellable = true, require = 1)
    private static void cnmTerrainCompat$convertLocalGeometry(BlockState source,
            CallbackInfoReturnable<BlockState> cir) {
        var binding = NibaruProviderAdapter.runtimeBinding(source.getBlock()).orElse(null);
        if (binding == null || binding.role().legacyGeometry().isPresent()) return;
        var target = binding.profile().transition(MaterialTransition.Type.PODZOL_GROWTH)
                .flatMap(edge -> NibaruMaterialProfiles.fromFamily(edge.target()))
                .flatMap(profile -> NibaruProviderAdapter.derived(profile, binding.role()));
        target.map(block -> block.withPropertiesOf(source)).ifPresent(cir::setReturnValue);
    }
}

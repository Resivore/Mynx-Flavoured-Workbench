package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.client.AxisGeneratedResources;
import dev.aero.cnmterraincompat.client.GlazedGeneratedResources;
import dev.aero.cnmterraincompat.client.InsetGeneratedResources;
import dev.tazer.clutternomore.client.assets.VerticalSlabGenerator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extends CNM's existing Vertical generator pass with provider-owned independent material state. */
@Mixin(value = VerticalSlabGenerator.class, remap = false)
abstract class GlazedVerticalAssetMixin {
    @Inject(method = "generateBlock", at = @At("TAIL"), require = 1)
    private static void cnmTerrainCompat$extendGlazedState(Identifier parent, Identifier shape,
            ResourceManager manager, CallbackInfo ci) {
        GlazedGeneratedResources.extendVertical(parent, shape);
        InsetGeneratedResources.extendVertical(parent, shape);
        AxisGeneratedResources.extendVertical(manager, parent, shape);
    }
}

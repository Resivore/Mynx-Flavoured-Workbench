package dev.aero.cnmterraincompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.tazer.clutternomore.client.assets.StepGenerator;
import dev.tazer.clutternomore.client.assets.VerticalSlabGenerator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * Regenerates integration-owned provider models when their semantic contract changes.
 * CNM normally treats its persistent generated pack as an immutable cache and returns
 * before consulting the provider, which left Canary 1.10's corrected roles unused.
 */
@Mixin(value = {VerticalSlabGenerator.class, StepGenerator.class}, remap = false)
abstract class ProviderAssetRefreshMixin {
    private static final String PROVIDER_PATH = "more_slabs_stairs_and_walls/";

    @ModifyExpressionValue(
            method = "generateBlock",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceManager;getResource(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;"),
            require = 1)
    private static Optional<Resource> cnmTerrainCompat$refreshProviderModels(
            Optional<Resource> existing,
            @Local(argsOnly = true, ordinal = 1) Identifier shape) {
        return shape.getPath().startsWith(PROVIDER_PATH) ? Optional.empty() : existing;
    }
}

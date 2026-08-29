package dev.aero.cnmterraincompat.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import dev.aero.cnmterraincompat.ProviderVisualAdapter;
import dev.tazer.clutternomore.client.assets.StepGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Decorates generated provider Step models from the declared render layer. */
@Mixin(value = StepGenerator.class, remap = false)
abstract class ProviderStepModelMixin {
    @ModifyArg(method = "generateBlock", at = @At(value = "INVOKE",
            target = "Ldev/tazer/clutternomore/client/assets/AssetGenerator;write(Ljava/lang/String;Lcom/google/gson/JsonElement;)V"),
            index = 1, require = 3)
    private static JsonElement cnmTerrainCompat$models(JsonElement model,
            @Local(argsOnly = true, ordinal = 0) Identifier parent) {
        return NibaruMaterialProfiles.fromId(parent)
                .map(profile -> ProviderVisualAdapter.decorateModel(profile, model)).orElse(model);
    }
}

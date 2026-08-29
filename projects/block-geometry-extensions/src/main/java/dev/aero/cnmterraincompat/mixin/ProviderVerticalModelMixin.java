package dev.aero.cnmterraincompat.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import dev.aero.cnmterraincompat.ProviderVisualAdapter;
import dev.tazer.clutternomore.client.assets.VerticalSlabGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Decorates generated provider Vertical models from the declared render layer. */
@Mixin(value = VerticalSlabGenerator.class, remap = false)
abstract class ProviderVerticalModelMixin {
    @ModifyArg(method = "generateBlock", at = @At(value = "INVOKE",
            target = "Ldev/tazer/clutternomore/client/assets/AssetGenerator;write(Ljava/lang/String;Lcom/google/gson/JsonElement;)V",
            ordinal = 0), index = 1, require = 1)
    private static JsonElement cnmTerrainCompat$single(JsonElement model,
            @Local(argsOnly = true, ordinal = 0) Identifier parent) {
        return decorate(parent, model);
    }

    @ModifyArg(method = "generateBlock", at = @At(value = "INVOKE",
            target = "Ldev/tazer/clutternomore/client/assets/AssetGenerator;write(Ljava/lang/String;Lcom/google/gson/JsonElement;)V",
            ordinal = 1), index = 1, require = 1)
    private static JsonElement cnmTerrainCompat$double(JsonElement model,
            @Local(argsOnly = true, ordinal = 0) Identifier parent) {
        return decorate(parent, model);
    }

    private static JsonElement decorate(Identifier parent, JsonElement model) {
        return NibaruMaterialProfiles.fromId(parent)
                .map(profile -> ProviderVisualAdapter.decorateModel(profile, model)).orElse(model);
    }
}

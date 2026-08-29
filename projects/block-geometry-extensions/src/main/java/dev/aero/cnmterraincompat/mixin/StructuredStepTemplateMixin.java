package dev.aero.cnmterraincompat.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.tazer.clutternomore.client.assets.StepGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Selects surface-band-aware CNM geometry templates from provider visual policy. */
@Mixin(value = StepGenerator.class, remap = false)
abstract class StructuredStepTemplateMixin {
    private static final String TEMPLATE_ROOT = "clutternomore:block/templates/provider/structured_step";

    @ModifyArg(method = "generateBlock", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/JsonObject;addProperty(Ljava/lang/String;Ljava/lang/String;)V", ordinal = 0),
            index = 1, require = 1)
    private static String cnmTerrainCompat$bottomTemplate(String original,
            @Local(argsOnly = true, ordinal = 0) Identifier parent) {
        return template(parent, original, "");
    }

    @ModifyArg(method = "generateBlock", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/JsonObject;addProperty(Ljava/lang/String;Ljava/lang/String;)V", ordinal = 1),
            index = 1, require = 1)
    private static String cnmTerrainCompat$doubleTemplate(String original,
            @Local(argsOnly = true, ordinal = 0) Identifier parent) {
        return template(parent, original, "_double");
    }

    @ModifyArg(method = "generateBlock", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/JsonObject;addProperty(Ljava/lang/String;Ljava/lang/String;)V", ordinal = 2),
            index = 1, require = 1)
    private static String cnmTerrainCompat$topTemplate(String original,
            @Local(argsOnly = true, ordinal = 0) Identifier parent) {
        return template(parent, original, "_top");
    }

    private static String template(Identifier parent, String original, String suffix) {
        return NibaruMaterialProfiles.fromId(parent)
                .filter(profile -> profile.visualProfile() == VisualProfile.GLASS_EDGE)
                .map(profile -> "clutternomore:block/templates/provider/glass_step" + suffix)
                .or(() -> NibaruMaterialProfiles.fromId(parent)
                .filter(profile -> profile.visualProfile() == VisualProfile.ROOTS)
                .map(profile -> "clutternomore:block/templates/provider/roots_step" + suffix)
                .or(() -> NibaruMaterialProfiles.fromId(parent)
                .filter(profile -> profile.surfaceSamplingPolicy()
                        == NibaruMaterialProfile.SurfaceSamplingPolicy.PATH_LOWERED_SURFACE)
                .map(profile -> "clutternomore:block/templates/provider/path_step" + suffix)
                .or(() -> NibaruMaterialProfiles.fromId(parent)
                .filter(profile -> profile.surfaceSamplingPolicy()
                        == NibaruMaterialProfile.SurfaceSamplingPolicy.NATIVE_STAIR_SURFACE_BAND)
                .map(profile -> TEMPLATE_ROOT + suffix))))
                .orElse(original);
    }
}

package dev.aero.cnmterraincompat.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.tazer.clutternomore.client.assets.VerticalSlabGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Selects lowered models for profiles that declare PATH_CONVERSION. */
@Mixin(value = VerticalSlabGenerator.class, remap = false)
abstract class PathVerticalTemplateMixin {
    @ModifyArg(method = "generateBlock", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/JsonObject;addProperty(Ljava/lang/String;Ljava/lang/String;)V", ordinal = 0), index = 1)
    private static String cnmTerrainCompat$single(String original,
            @Local(argsOnly = true, ordinal = 0) Identifier parent) {
        return template(parent, original, "path_vertical_slab");
    }

    @ModifyArg(method = "generateBlock", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/JsonObject;addProperty(Ljava/lang/String;Ljava/lang/String;)V", ordinal = 1), index = 1)
    private static String cnmTerrainCompat$double(String original,
            @Local(argsOnly = true, ordinal = 0) Identifier parent) {
        return template(parent, original, "path_vertical_slab_double");
    }

    private static String template(Identifier parent, String original, String name) {
        return NibaruMaterialProfiles.fromId(parent)
                .filter(p -> p.visualProfile() == VisualProfile.GLASS_EDGE)
                .map(p -> "clutternomore:block/templates/provider/glass_" + name.substring("path_".length()))
                .or(() -> NibaruMaterialProfiles.fromId(parent)
                .filter(p -> p.visualProfile() == VisualProfile.ROOTS)
                .map(p -> "clutternomore:block/templates/provider/roots_" + name.substring("path_".length()))
                .or(() -> NibaruMaterialProfiles.fromId(parent)
                .filter(p -> p.capabilities().contains(BehaviorCapability.PATH_CONVERSION))
                .map(p -> "clutternomore:block/templates/provider/" + name)))
                .orElse(original);
    }
}

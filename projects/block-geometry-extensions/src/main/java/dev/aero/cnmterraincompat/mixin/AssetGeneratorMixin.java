package dev.aero.cnmterraincompat.mixin;

import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.CnmGeneratedLanguage;
import dev.aero.cnmterraincompat.MaterialAxisState;
import dev.aero.cnmterraincompat.client.BgeGeneratedResources;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies semantic provider texture roles instead of scraping one shallow model. */
@Mixin(value = AssetGenerator.class, remap = false)
abstract class AssetGeneratorMixin {
    @Inject(method = "generate", at = @At("TAIL"), require = 1)
    private static void cnmTerrainCompat$generateBgeGeometry(ResourceManager manager, CallbackInfo ci) {
        BgeGeneratedResources.generate(manager);
    }

    /**
     * CNM also exposes its generated resources as the enabled file/clutternomore
     * pack. Do not mistake that persistent mirror for an authored translation
     * source when rebuilding the generated English language file.
     */
    @Redirect(
            method = "translatedKeys",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"),
            require = 1)
    private static boolean cnmTerrainCompat$skipGeneratedLanguagePacks(String runtimePack, Object sourcePackId) {
        return sourcePackId instanceof String id && CnmGeneratedLanguage.isSelfGeneratedPack(id);
    }

    @Inject(method = "getTextures", at = @At("HEAD"), cancellable = true, require = 1)
    private static void cnmTerrainCompat$providerTextures(ResourceManager manager, Identifier parent,
            CallbackInfoReturnable<JsonObject> cir) {
        NibaruMaterialProfiles.fromId(parent).ifPresent(profile -> cir.setReturnValue(textures(profile)));
    }

    @Inject(method = "checkTint", at = @At("HEAD"), cancellable = true, require = 1)
    private static void cnmTerrainCompat$providerTint(ResourceManager manager, Identifier parent,
            CallbackInfoReturnable<Boolean> cir) {
        NibaruMaterialProfiles.fromId(parent)
                .ifPresent(profile -> cir.setReturnValue(profile.tintProfile() != TintProfile.NONE));
    }

    private static JsonObject textures(NibaruMaterialProfile profile) {
        NibaruMaterialProfile.TextureRoles roles = profile.textureRoles();
        boolean materialAxis = MaterialAxisState.applies(profile);
        JsonObject textures = new JsonObject();
        textures.addProperty("side", texture(roles.side()));
        textures.addProperty("top", texture(roles.top()));
        // Vanilla-style axes are unsigned: both axial ends use the canonical top/end role.
        // This also avoids Canary 42's synthetic, nonexistent *_bottom role for CUBE_BOTTOM_TOP pillars.
        textures.addProperty("bottom", texture(materialAxis ? roles.top() : roles.bottom()));
        textures.addProperty("particle", texture(materialAxis ? roles.side() : roles.particle()));
        if (!roles.overlay().isEmpty()) textures.addProperty("overlay", texture(roles.overlay()));
        return textures;
    }

    private static String texture(String path) {
        return path.contains(":") ? path : "minecraft:block/" + path;
    }
}

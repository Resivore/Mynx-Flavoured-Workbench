package com.fizzware.dramaticdoors.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fizzware.dramaticdoors.DramaticDoors;
import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

@Mixin(ServerAdvancementManager.class)
public abstract class AdvancementManagerMixin
{
    @Shadow
    @Final
    private HolderLookup.Provider registries;

	@Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void interceptApply(Map<Identifier, Advancement> map, ResourceManager manager, ProfilerFiller profiler, CallbackInfo info) {
		for (JsonObject advancementJson : DDCompatAdvancement.RECIPE_ADVANCEMENTS) {
            Identifier advancementId = Identifier.fromNamespaceAndPath(
                    DramaticDoors.MOD_ID,
                    "recipes/redstone/" + advancementJson.getAsJsonObject("rewards")
                            .getAsJsonArray("recipes")
                            .get(0)
                            .getAsString()
                            .replace(DramaticDoors.MOD_ID + ":", ""));
            Advancement advancement = Advancement.CODEC
                    .parse(registries.createSerializationContext(JsonOps.INSTANCE), advancementJson)
                    .getOrThrow(error -> new IllegalStateException("Could not decode generated advancement " + advancementId + ": " + error));
            map.put(advancementId, advancement);
        }
    }
}

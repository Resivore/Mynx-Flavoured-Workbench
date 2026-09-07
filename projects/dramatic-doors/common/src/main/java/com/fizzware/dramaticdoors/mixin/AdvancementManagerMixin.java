package com.fizzware.dramaticdoors.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.fizzware.dramaticdoors.DramaticDoors;
import com.fizzware.dramaticdoors.compat.AdvancementReloadSupport;
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

	@ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"), argsOnly = true)
    private Map<Identifier, Advancement> interceptApply(Map<Identifier, Advancement> map) {
		Map<Identifier, Advancement> mutableMap = AdvancementReloadSupport.copyForGeneratedEntries(
				map,
				!DDCompatAdvancement.RECIPE_ADVANCEMENTS.isEmpty());
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
            mutableMap.put(advancementId, advancement);
        }
		return mutableMap;
    }
}

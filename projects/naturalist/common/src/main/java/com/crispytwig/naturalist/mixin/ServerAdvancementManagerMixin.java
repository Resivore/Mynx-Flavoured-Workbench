package com.crispytwig.naturalist.mixin;

import com.crispytwig.naturalist.server.advancement.NaturalistAdvancementCompatibility;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(ServerAdvancementManager.class)
public class ServerAdvancementManagerMixin {
    // 26.2 consumes the decoded map here to build both holders and the tree.
    // Run after argument-replacing HEAD hooks, including Matcha heart's Map.copyOf.
    @ModifyReceiver(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"), require = 1)
    private Map<Identifier, Advancement> naturalist$prepareAdvancements(Map<Identifier, Advancement> map,
                                                                       BiConsumer<? super Identifier, ? super Advancement> action) {
        return NaturalistAdvancementCompatibility.prepareForPublication(map);
    }
}

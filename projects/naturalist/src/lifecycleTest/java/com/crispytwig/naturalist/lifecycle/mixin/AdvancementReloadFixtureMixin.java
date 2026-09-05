package com.crispytwig.naturalist.lifecycle.mixin;

import com.crispytwig.naturalist.lifecycle.AdvancementReloadFixture;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.Map;

@Mixin(ServerAdvancementManager.class)
public abstract class AdvancementReloadFixtureMixin {
    @ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private Map<Identifier, Advancement> fixture$immutableReplacement(Map<Identifier, Advancement> incoming) {
        return AdvancementReloadFixture.replacement.apply(incoming);
    }
}

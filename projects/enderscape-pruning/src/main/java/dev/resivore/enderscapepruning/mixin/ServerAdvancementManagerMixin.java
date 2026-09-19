package dev.resivore.enderscapepruning.mixin;

import dev.resivore.enderscapepruning.PruningAdvancements;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/** Removes only entire suppressed child branches, before trigger publication. */
@Mixin(ServerAdvancementManager.class)
abstract class ServerAdvancementManagerMixin {
    @ModifyVariable(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private Map<Identifier, Advancement> enderscapePruning$removeSuppressedAdvancements(
            Map<Identifier, Advancement> resolved) {
        return PruningAdvancements.removeSuppressed(resolved);
    }
}

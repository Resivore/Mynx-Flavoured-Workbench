package dev.resivore.matchaheart.mixin;

import dev.resivore.matchaheart.AuthoritativeData;
import dev.resivore.matchaheart.HeartDataContract;
import dev.resivore.matchaheart.MatchaHeartDeathCompat;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerAdvancementManager.class)
abstract class ServerAdvancementManagerMixin {
    @Shadow @Final private HolderLookup.Provider registries;

    @ModifyVariable(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private Map<Identifier, Advancement> matchaHeart$enforceAdvancements(
            Map<Identifier, Advancement> resolved) {
        try {
            LinkedHashMap<Identifier, Advancement> enforced = new LinkedHashMap<>(resolved);
            HeartDataContract.ADVANCEMENT_RESOURCES.forEach((id, path) -> enforced.put(
                    Identifier.parse(id), AuthoritativeData.decodeAdvancement(path, this.registries)));
            MatchaHeartDeathCompat.LOGGER.info(
                    "Enforced authoritative advancement contracts for {} before trigger publication",
                    HeartDataContract.ADVANCEMENT_RESOURCES.keySet());
            return Map.copyOf(enforced);
        } catch (RuntimeException exception) {
            MatchaHeartDeathCompat.LOGGER.error(
                    "FATAL: could not enforce authoritative advancement contracts; aborting data reload", exception);
            throw new IllegalStateException("Unsafe Matcha advancement contracts", exception);
        }
    }
}

package dev.resivore.matchavillagerestoration.mixin;

import dev.resivore.matchavillagerestoration.MatchaVanillaVillageRestoration;
import dev.resivore.matchavillagerestoration.VillagePackPrecedence;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MultiPackResourceManager.class)
abstract class MultiPackResourceManagerMixin {
    @Unique
    private static final Logger MATCHA_VILLAGE_RESTORATION_LOGGER =
            LoggerFactory.getLogger(MatchaVanillaVillageRestoration.MOD_ID);

    @Unique
    private static final AtomicBoolean MATCHA_VILLAGE_RESTORATION_LOGGED = new AtomicBoolean();

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static List<PackResources> matchaVillageRestoration$prioritizeBuiltInPack(
            List<PackResources> packs) {
        List<PackResources> prioritized = VillagePackPrecedence.prioritize(packs);
        if (prioritized != packs && MATCHA_VILLAGE_RESTORATION_LOGGED.compareAndSet(false, true)) {
            MATCHA_VILLAGE_RESTORATION_LOGGER.info(
                    "Placed {} at highest server-data priority without reordering any other pack.",
                    MatchaVanillaVillageRestoration.BUILTIN_PACK_ID);
        }
        return prioritized;
    }
}

package dev.resivore.mynxmatchatradetweaks.mixin;

import dev.resivore.mynxmatchatradetweaks.MynxMatchaTradeTweaks;
import dev.resivore.mynxmatchatradetweaks.TradePackPrecedence;
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
    private static final Logger MYNX_MATCHA_TRADE_TWEAKS_LOGGER =
            LoggerFactory.getLogger(MynxMatchaTradeTweaks.MOD_ID);

    @Unique
    private static final AtomicBoolean MYNX_MATCHA_TRADE_TWEAKS_LOGGED = new AtomicBoolean();

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static List<PackResources> mynxMatchaTradeTweaks$prioritizeBuiltInPack(
            List<PackResources> packs) {
        List<PackResources> prioritized = TradePackPrecedence.prioritize(packs);
        if (prioritized != packs && MYNX_MATCHA_TRADE_TWEAKS_LOGGED.compareAndSet(false, true)) {
            MYNX_MATCHA_TRADE_TWEAKS_LOGGER.info(
                    "Placed {} at highest server-data priority without reordering any other pack.",
                    MynxMatchaTradeTweaks.BUILTIN_PACK_ID);
        }
        return prioritized;
    }
}

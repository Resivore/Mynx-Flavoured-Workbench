package dev.resivore.matchaheart.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MultiPackResourceManager.class)
abstract class HeartPackPriorityMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static List<PackResources> matchaHeart$prioritize(List<PackResources> packs) {
        for (PackResources pack : packs) {
            if (pack.packId().equals("matcha_heart_death_compat:heart_progression")) {
                var ordered = new ArrayList<>(packs);
                ordered.remove(pack);
                ordered.add(pack);
                return ordered;
            }
        }
        return packs;
    }
}

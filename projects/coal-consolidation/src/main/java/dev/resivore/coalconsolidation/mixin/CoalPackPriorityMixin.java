package dev.resivore.coalconsolidation.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MultiPackResourceManager.class)
abstract class CoalPackPriorityMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static List<PackResources> coalConsolidation$prioritize(List<PackResources> packs) {
        for (PackResources pack : packs) {
            if (pack.packId().equals("coal_consolidation:coal_recipes")) {
                var ordered = new ArrayList<>(packs);
                ordered.remove(pack);
                ordered.add(pack);
                return ordered;
            }
        }
        return packs;
    }
}

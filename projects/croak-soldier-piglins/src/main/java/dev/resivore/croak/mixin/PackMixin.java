package dev.resivore.croak.mixin;

import dev.resivore.croak.*;

import net.minecraft.server.packs.*;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import java.util.*;

@Mixin(ReloadableResourceManager.class)
abstract class PackMixin {
    @Shadow @Final private PackType type;

    @ModifyVariable(method = "createReload", at = @At("HEAD"), argsOnly = true)
    private List<PackResources> croak$mount(List<PackResources> packs) {
        if (type != PackType.CLIENT_RESOURCES) return packs;
        var source = SoldierPack.load();
        if (source.isEmpty()) return packs;
        var result = new ArrayList<>(packs);
        result.add(source.get());
        return result;
    }
}

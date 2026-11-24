package dev.resivore.matchaflavoureddata.mixin;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPackResourceManager.class)
abstract class MatchaGenerationGuard {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void matchaData$verifyGeneration(PackType type, List<PackResources> packs, CallbackInfo ci) {
        if (type != PackType.SERVER_DATA) return;
        for (PackResources pack : packs) {
            if (pack.getResource(type, Identifier.parse("main:function/setup/tick.mcfunction")) != null
                    || pack.getResource(type, Identifier.parse("main:function/setup/load.mcfunction")) != null) {
                throw new IllegalStateException("Disable the legacy Matcha 1.12 datapack: " + pack.packId());
            }
            if (!pack.packId().equals("matcha_flavoured_data:matcha_flavoured_1_12_5")
                    && pack.getResource(type, Identifier.parse("matcha:function/timers/tick.mcfunction")) != null) {
                throw new IllegalStateException("Multiple Matcha data providers: " + pack.packId());
            }
        }
    }
}

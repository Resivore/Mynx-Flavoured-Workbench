package dev.resivore.mynxfloratrades.mixin;

import dev.resivore.mynxfloratrades.FloristRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerProfession.class)
abstract class VillagerProfessionMixin {
    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void mynxFloraTrades$registerFlorist(Registry<VillagerProfession> registry,
                                                          CallbackInfoReturnable<VillagerProfession> cir) {
        FloristRegistry.registerProfession(registry);
    }
}

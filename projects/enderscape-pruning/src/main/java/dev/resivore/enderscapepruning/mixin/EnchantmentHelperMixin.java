package dev.resivore.enderscapepruning.mixin;

import dev.resivore.enderscapepruning.PruningContract;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Defensive normal-enchanting filter layered on top of the narrow tag cleanup. */
@Mixin(EnchantmentHelper.class)
abstract class EnchantmentHelperMixin {
    @Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"), cancellable = true, require = 1)
    private static void enderscapePruning$filterNewEnchantmentResults(
            int value,
            ItemStack itemStack,
            Stream<Holder<Enchantment>> source,
            CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        List<EnchantmentInstance> available = new ArrayList<>(cir.getReturnValue());
        available.removeIf(instance -> PruningContract.isSuppressedEnchantment(instance.enchantment())
                || (PruningContract.isResonance(instance.enchantment()) && !PruningContract.isMagniaAttractor(itemStack)));
        cir.setReturnValue(available);
    }
}

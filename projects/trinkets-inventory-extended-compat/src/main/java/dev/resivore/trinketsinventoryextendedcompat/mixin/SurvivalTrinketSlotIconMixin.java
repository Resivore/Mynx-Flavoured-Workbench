package dev.resivore.trinketsinventoryextendedcompat.mixin;

import eu.pb4.trinkets.impl.SurvivalTrinketSlot;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Corrects the legacy Crafting Slots charm icon identifier for the 26.2 GUI atlas. */
@Mixin(value = SurvivalTrinketSlot.class, remap = false)
abstract class SurvivalTrinketSlotIconMixin {
    private static final Identifier LEGACY_CHARM_ICON =
            Identifier.fromNamespaceAndPath("trinkets", "gui/slots/charm");
    private static final Identifier CURRENT_CHARM_ICON =
            Identifier.fromNamespaceAndPath("trinkets", "container/slots/charm");

    @Inject(method = "getNoItemIcon", at = @At("RETURN"), cancellable = true)
    private void trinketsInventoryExtendedCompat$correctLegacyCharmIcon(
            CallbackInfoReturnable<Identifier> cir) {
        if (LEGACY_CHARM_ICON.equals(cir.getReturnValue())) {
            cir.setReturnValue(CURRENT_CHARM_ICON);
        }
    }
}

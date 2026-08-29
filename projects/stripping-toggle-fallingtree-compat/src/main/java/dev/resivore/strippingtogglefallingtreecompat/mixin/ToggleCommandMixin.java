package dev.resivore.strippingtogglefallingtreecompat.mixin;

import dev.resivore.strippingtogglefallingtreecompat.ToggleAuthority;
import fr.rakambda.fallingtree.common.FallingTreeCommon;
import fr.rakambda.fallingtree.common.command.ToggleCommand;
import fr.rakambda.fallingtree.common.wrapper.IPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ToggleCommand.class, remap = false)
public abstract class ToggleCommandMixin {
    @Shadow @Final private FallingTreeCommon<?> mod;

    @Inject(
            method = "apply(Lfr/rakambda/fallingtree/common/wrapper/IPlayer;)I",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void strippingToggleCompat$reportMasterState(
            IPlayer player,
            CallbackInfoReturnable<Integer> callback) {
        boolean enabled = ToggleAuthority.isEnabled(player.getTags());
        mod.notifyPlayer(player, mod.translate(ToggleAuthority.statusTranslationKey(enabled)));
        callback.setReturnValue(1);
    }
}

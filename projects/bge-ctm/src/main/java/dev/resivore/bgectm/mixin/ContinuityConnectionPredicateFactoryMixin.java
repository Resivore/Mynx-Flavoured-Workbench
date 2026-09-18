package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.continuity.ContactFilteringConnectionPredicate;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.properties.BasicConnectingCtmProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Decorates the one exact predicate factory used by Continuity's connecting processors. */
@Mixin(BasicConnectingCtmProperties.class)
abstract class ContinuityConnectionPredicateFactoryMixin {
    @Inject(method = "getConnectionPredicate", at = @At("RETURN"), cancellable = true, require = 1)
    private void bgeCtm$decorateConnectionPredicate(
            CallbackInfoReturnable<ConnectionPredicate> callback) {
        ConnectionPredicate predicate = callback.getReturnValue();
        if (!(predicate instanceof ContactFilteringConnectionPredicate)) {
            callback.setReturnValue(new ContactFilteringConnectionPredicate(predicate));
        }
    }
}

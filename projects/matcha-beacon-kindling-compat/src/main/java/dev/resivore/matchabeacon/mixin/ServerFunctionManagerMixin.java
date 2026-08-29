package dev.resivore.matchabeacon.mixin;

import dev.resivore.matchabeacon.runtime.BeaconKindlingCoordinator;
import dev.resivore.matchabeacon.runtime.FunctionExecutionGuard;
import dev.resivore.matchabeacon.runtime.MatchaContract;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerFunctionManager.class)
abstract class ServerFunctionManagerMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, require = 1)
    private void matchaBeacon$replaceUnsafeLifecycle(
            CommandFunction<CommandSourceStack> function,
            CommandSourceStack source,
            CallbackInfo ci) {
        String functionId = function.id().toString();
        if (!MatchaContract.isPatchedFunction(functionId)) {
            return;
        }
        if (MatchaContract.PLACEMENT_FUNCTION.equals(functionId)) {
            BeaconKindlingCoordinator.handlePlacementFunction(source);
            ci.cancel();
            return;
        }
        if (!FunctionExecutionGuard.allows(functionId, source.getEntity())) {
            ci.cancel();
        }
    }
}

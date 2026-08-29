package dev.resivore.matchaheart.mixin;

import dev.resivore.matchaheart.HeartDataContract;
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
    private void matchaHeart$blockConflictingFunctions(
            CommandFunction<CommandSourceStack> function,
            CommandSourceStack source,
            CallbackInfo ci) {
        String id = function.id().toString();
        if (HeartDataContract.blocksFunction(id)) {
            ci.cancel();
        }
    }
}

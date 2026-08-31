package dev.resivore.matchafrost.mixin;

import dev.resivore.matchafrost.MatchaFreezingWaterFunctionEnforcer;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerFunctionManager.class)
abstract class ServerFunctionManagerMixin {
    @Shadow private ServerFunctionLibrary library;

    @Inject(
            method = "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void matchaFrost$enforceFreezingWaterGuard(
            Identifier requested,
            CallbackInfoReturnable<Optional<CommandFunction<CommandSourceStack>>> callbackInfo) {
        if (MatchaFreezingWaterFunctionEnforcer.targets(requested)) {
            callbackInfo.setReturnValue(MatchaFreezingWaterFunctionEnforcer.resolve(
                    requested,
                    this.library.getFunctions()));
        }
    }
}

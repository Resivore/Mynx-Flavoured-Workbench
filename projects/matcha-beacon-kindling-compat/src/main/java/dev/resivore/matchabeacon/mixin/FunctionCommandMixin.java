package dev.resivore.matchabeacon.mixin;

import com.mojang.brigadier.CommandDispatcher;
import dev.resivore.matchabeacon.runtime.BeaconKindlingCoordinator;
import dev.resivore.matchabeacon.runtime.FunctionExecutionGuard;
import dev.resivore.matchabeacon.runtime.MatchaContract;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.FunctionCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FunctionCommand.class)
abstract class FunctionCommandMixin {
    @Inject(
        method = "instantiateAndQueueFunctions",
        at = @At("HEAD"),
        cancellable = true,
        require = 1
    )
    private static <T extends ExecutionCommandSource<T>> void matchaBeaconKindlingCompat$interceptNestedFunction(
        CompoundTag arguments,
        ExecutionControl<T> executionControl,
        CommandDispatcher<T> dispatcher,
        T source,
        CommandFunction<T> function,
        Identifier functionId,
        CommandResultCallback callback,
        boolean returnCommand,
        CallbackInfo ci
    ) {
        String id = functionId.toString();
        if (!MatchaContract.isPatchedFunction(id)) {
            return;
        }

        if (!(source instanceof CommandSourceStack commandSource)) {
            ci.cancel();
            return;
        }

        if (MatchaContract.PLACEMENT_FUNCTION.equals(id)) {
            BeaconKindlingCoordinator.handlePlacementFunction(commandSource);
            ci.cancel();
            return;
        }

        if (!FunctionExecutionGuard.allows(id, commandSource.getEntity())) {
            ci.cancel();
        }
    }
}

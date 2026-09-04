package dev.resivore.carryonpatch.common.mixin;

import dev.resivore.carryonpatch.common.CarryPersistence;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayer.class, remap = false)
public abstract class ServerPlayerCarryMixin {
    // Upstream attempts placement on death. If it fails, preserve carry data and any partial-
    // placement lock on the replacement player rather than abandoning it on the dead instance.
    @Inject(method = "restoreFrom", at = @At("TAIL"), require = 1)
    private void carryOnPatch$copyRemainingCarry(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        var reporter = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(reporter, oldPlayer.registryAccess());
        CarryPersistence.save(oldPlayer, output);
        CarryPersistence.load((ServerPlayer)(Object)this,
                TagValueInput.create(reporter, oldPlayer.registryAccess(), output.buildResult()));
        if (!reporter.isEmpty()) throw new IllegalStateException("Could not preserve carry state on player replacement: " + reporter.getReport());
    }
}

package dev.resivore.frozenlibshutdowncompat.mixin;

import dev.resivore.frozenlibshutdowncompat.DaemonCachedExecutorFactory;
import java.util.concurrent.ExecutorService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.frozenblock.lib.cape.api.CapeUtil", remap = false)
abstract class CapeUtilExecutorMixin {
    @Redirect(
        method = "registerCapesFromURL(Ljava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/Executors;newCachedThreadPool()Ljava/util/concurrent/ExecutorService;"
        ),
        require = 1,
        remap = false
    )
    private static ExecutorService frozenlibShutdownCompat$createDaemonExecutor() {
        return DaemonCachedExecutorFactory.create();
    }
}

package dev.resivore.stacksarestackscontainerfixes.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.a5ho9999.stacksarestacks.StacksAreStacksMod", remap = false)
public interface StacksAreStacksModInvoker {
    @Invoker(value = "setStackSizes", remap = false)
    static void stacksAreStacksContainerFixes$setStackSizes(MinecraftServer server) {
        throw new AssertionError("Mixin invoker was not transformed");
    }
}

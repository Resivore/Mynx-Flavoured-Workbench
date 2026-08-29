package dev.resivore.stacksarestackscontainerfixes;

import dev.resivore.stacksarestackscontainerfixes.mixin.StacksAreStacksModInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class StacksAreStacksContainerFixesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                StacksAreStacksModInvoker.stacksAreStacksContainerFixes$setStackSizes(null));
    }
}

package dev.resivore.stacksarestackscontainerfixes;

import dev.resivore.stacksarestackscontainerfixes.mixin.StacksAreStacksModInvoker;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StacksAreStacksContainerFixesClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Stacks Are Stacks Container Fixes");
    private static final AtomicBoolean CALLBACK_REGISTERED = new AtomicBoolean();
    private static final ClientAlignmentCoordinator ALIGNMENT = new ClientAlignmentCoordinator();

    @Override
    public void onInitializeClient() {
        if (!CALLBACK_REGISTERED.compareAndSet(false, true)) {
            LOGGER.info("Client configuration alignment callback is already registered; skipping duplicate registration");
            return;
        }

        ClientConfigurationConnectionEvents.COMPLETE.register(
                StacksAreStacksContainerFixesClient::onConfigurationComplete);
        LOGGER.info("Registered client holder alignment at configuration completion");
    }

    private static void onConfigurationComplete(
            ClientConfigurationPacketListenerImpl listener,
            Minecraft client
    ) {
        LOGGER.info("Registry synchronization completed; evaluating Stacks Are Stacks client holder alignment");

        try {
            ClientAlignmentCoordinator.Attempt attempt = ALIGNMENT.align(
                    listener,
                    client.isSameThread(),
                    StacksAreStacksContainerFixesClient::inspectItemHolderReadiness,
                    readyEpoch -> {
                        ClientAlignmentCoordinator.HolderReadiness readiness = readyEpoch.readiness();
                        LOGGER.info(
                                "Item holder readiness passed for binding epoch {} ({}/{} bound); invoking audited upstream alignment",
                                readyEpoch.epochNumber(),
                                readiness.boundHolders(),
                                readiness.totalHolders());
                        StacksAreStacksModInvoker.stacksAreStacksContainerFixes$setStackSizes(null);
                    });

            logResult(attempt);
        } catch (RuntimeException | Error failure) {
            LOGGER.error("Stacks Are Stacks client holder alignment did not complete", failure);
            throw failure;
        }
    }

    private static ClientAlignmentCoordinator.HolderReadiness inspectItemHolderReadiness() {
        int bound = 0;
        int total = 0;
        for (var item : BuiltInRegistries.ITEM) {
            total++;
            Holder<?> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
            if (holder instanceof Holder.Reference<?> reference && reference.areComponentsBound()) {
                bound++;
            }
        }
        return new ClientAlignmentCoordinator.HolderReadiness(bound, total);
    }

    private static void logResult(ClientAlignmentCoordinator.Attempt attempt) {
        switch (attempt.outcome()) {
            case ALIGNED -> LOGGER.info(
                    "Stacks Are Stacks client holder alignment completed for binding epoch {}",
                    attempt.epochNumber());
            case ALREADY_ALIGNED -> LOGGER.info(
                    "Skipping Stacks Are Stacks client holder alignment for binding epoch {}: already aligned",
                    attempt.epochNumber());
            case HOLDERS_NOT_READY -> LOGGER.error(
                    "Item holder readiness failed for binding epoch {} ({}/{} bound); upstream alignment was not invoked",
                    attempt.epochNumber(),
                    attempt.readiness().boundHolders(),
                    attempt.readiness().totalHolders());
            case WRONG_THREAD -> LOGGER.error(
                    "Skipping Stacks Are Stacks client holder alignment for binding epoch {}: callback was not on the client thread",
                    attempt.epochNumber());
            case EPOCH_ALREADY_REJECTED -> LOGGER.error(
                    "Skipping Stacks Are Stacks client holder alignment for binding epoch {}: this epoch was already rejected",
                    attempt.epochNumber());
            case OVERLAPPING -> LOGGER.error(
                    "Skipping Stacks Are Stacks client holder alignment: binding epoch {} is already in progress",
                    attempt.epochNumber());
            case ALIGNMENT_FAILED -> LOGGER.error(
                    "Stacks Are Stacks client holder alignment failed for binding epoch {}",
                    attempt.epochNumber());
        }
    }
}

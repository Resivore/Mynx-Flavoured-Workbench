package dev.resivore.carriedrouting;

import dev.resivore.carriedrouting.client.RoutedPickupSoundState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutedPickupSoundArchitectureTest {
    @Test
    void routedPickupStateIsOneShotAndUsesTheRequestedPitchMultiplier() {
        RoutedPickupSoundState.mark(41);
        assertTrue(RoutedPickupSoundState.consume(41));
        assertFalse(RoutedPickupSoundState.consume(41));
        assertEquals(0.80F, RoutedPickupSoundState.PITCH_MULTIPLIER);
    }

    @Test
    void onlyTheActualCarriedInsertionResultCanMarkTheWorldPickupCue() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        String routing = Files.readString(root.resolve("src/main/java/dev/resivore/carriedrouting/RoutingService.java"));
        String itemEntity = Files.readString(root.resolve("src/main/java/dev/resivore/carriedrouting/mixin/ItemEntityMixin.java"));
        String client = Files.readString(root.resolve("src/main/java/dev/resivore/carriedrouting/mixin/client/ClientPacketListenerMixin.java"));

        assertTrue(routing.contains("int carriedContainerItemsMoved = beforeCarriedContainers - incoming.getCount();"));
        assertTrue(routing.contains("boolean routedToCarriedContainer() { return carriedContainerItemsMoved > 0; }"));
        assertTrue(itemEntity.contains("routeIncomingStackWithResult("));
        assertTrue(itemEntity.contains(".routedToCarriedContainer();"));
        assertTrue(itemEntity.contains("Player;take(Lnet/minecraft/world/entity/Entity;I)V"));
        assertTrue(itemEntity.contains("new RoutedPickupSoundPayload"));
        assertFalse(itemEntity.contains("RoutingContext.QUICK_MOVE"));
        assertTrue(client.contains("method = \"handleTakeItemEntity\""));
        assertTrue(client.contains("SoundEvents.ITEM_PICKUP"));
        assertTrue(client.contains("args.set(6, (float) args.get(6) * RoutedPickupSoundState.PITCH_MULTIPLIER)"));
        assertFalse(client.contains("args.set(5,"), "The normal vanilla pickup volume must not change");
    }
}

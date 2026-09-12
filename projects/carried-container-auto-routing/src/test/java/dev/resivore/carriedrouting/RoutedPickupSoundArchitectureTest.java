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
    void fallbackDecisionSendsOnlyOneAppropriateCueForCustomWorldMovement() {
        assertEquals(RoutedPickupAudioDecision.FallbackCue.NONE,
                RoutedPickupAudioDecision.fallbackCue(0, false, true),
                "No custom movement must not synthesize a cue");
        assertEquals(RoutedPickupAudioDecision.FallbackCue.NONE,
                RoutedPickupAudioDecision.fallbackCue(3, false, true),
                "Vanilla Player.take feedback must suppress an ordinary fallback");
        assertEquals(RoutedPickupAudioDecision.FallbackCue.NONE,
                RoutedPickupAudioDecision.fallbackCue(3, true, true),
                "The existing carried vanilla-packet cue must not be doubled");
        assertEquals(RoutedPickupAudioDecision.FallbackCue.NORMAL_PITCH,
                RoutedPickupAudioDecision.fallbackCue(3, false, false),
                "Ordinary custom movement without Player.take gets one normal fallback");
        assertEquals(RoutedPickupAudioDecision.FallbackCue.LOWER_PITCH,
                RoutedPickupAudioDecision.fallbackCue(3, true, false),
                "Actual carried-container movement without Player.take gets one lower fallback");
        assertEquals(RoutedPickupAudioDecision.FallbackCue.NONE,
                RoutedPickupAudioDecision.fallbackCue(0, true, false),
                "Full, locked, or otherwise nonaccepting destinations stay silent");
    }

    @Test
    void onlyActualWorldPickupMovementCanChooseTheFallbackOrExistingVanillaCue() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        String routing = Files.readString(root.resolve("src/main/java/dev/resivore/carriedrouting/RoutingService.java"));
        String itemEntity = Files.readString(root.resolve("src/main/java/dev/resivore/carriedrouting/mixin/ItemEntityMixin.java"));
        String client = Files.readString(root.resolve("src/main/java/dev/resivore/carriedrouting/mixin/client/ClientPacketListenerMixin.java"));
        String clientEntrypoint = Files.readString(root.resolve("src/main/java/dev/resivore/carriedrouting/client/CarriedContainerAutoRoutingClient.java"));
        String fallbackPayload = Files.readString(root.resolve("src/main/java/dev/resivore/carriedrouting/RoutedPickupSoundFallbackPayload.java"));
        String pitch = Files.readString(root.resolve("src/main/java/dev/resivore/carriedrouting/client/RoutedPickupSoundState.java"));

        assertTrue(routing.contains("int carriedContainerItemsMoved = beforeCarriedContainers - incoming.getCount();"));
        assertTrue(routing.contains("boolean routedToCarriedContainer() { return carriedContainerItemsMoved > 0; }"));
        assertTrue(itemEntity.contains("routeIncomingStackWithResult("));
        assertTrue(itemEntity.contains("carriedRouting$customItemsMoved = beforeRouting - incoming.getCount();"));
        assertTrue(itemEntity.contains("carriedRouting$routedToCarriedContainer = result.routedToCarriedContainer();"));
        assertTrue(itemEntity.contains("at = @At(\"HEAD\")"));
        assertTrue(itemEntity.contains("Player;take(Lnet/minecraft/world/entity/Entity;I)V"));
        assertTrue(itemEntity.contains("carriedRouting$vanillaTakeReached = true;"));
        assertTrue(itemEntity.contains("new RoutedPickupSoundPayload"));
        assertTrue(itemEntity.contains("at = @At(\"RETURN\")"));
        assertTrue(itemEntity.contains("RoutedPickupAudioDecision.fallbackCue("));
        assertTrue(itemEntity.contains("new RoutedPickupSoundFallbackPayload("));
        assertTrue(itemEntity.contains("carriedRouting$pickupX"));
        assertFalse(itemEntity.contains("RoutingContext.QUICK_MOVE"));
        assertFalse(itemEntity.contains("ClientboundTakeItemEntityPacket"));
        assertTrue(client.contains("method = \"handleTakeItemEntity\""));
        assertTrue(client.contains("SoundEvents.ITEM_PICKUP"));
        assertTrue(client.contains("args.set(6, (float) args.get(6) * RoutedPickupSoundState.PITCH_MULTIPLIER)"));
        assertFalse(client.contains("args.set(5,"), "The normal vanilla pickup volume must not change");
        assertTrue(clientEntrypoint.contains("RoutedPickupSoundFallbackPayload.TYPE"));
        assertTrue(clientEntrypoint.contains("playLocalSound("));
        assertTrue(clientEntrypoint.contains("SoundSource.PLAYERS"));
        assertTrue(clientEntrypoint.contains("0.2F, pitch, false"));
        assertTrue(clientEntrypoint.contains("payload.x(), payload.y(), payload.z()"));
        assertTrue(fallbackPayload.contains("double x"));
        assertTrue(fallbackPayload.contains("boolean lowerPitch"));
        assertTrue(pitch.contains("(random.nextFloat() - random.nextFloat()) * 1.4F + 2.0F"));
    }
}

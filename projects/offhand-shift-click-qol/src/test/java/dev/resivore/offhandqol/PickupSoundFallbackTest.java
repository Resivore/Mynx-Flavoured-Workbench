package dev.resivore.offhandqol;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickupSoundFallbackTest {
    @Test
    void standaloneDecisionOnlySynthesizesOneCueWhenCustomMovementHasNoVanillaTake() {
        assertFalse(OffhandPickupAudioDecision.shouldSendFallback(0, true, false),
                "No custom movement must not synthesize a cue");
        assertFalse(OffhandPickupAudioDecision.shouldSendFallback(4, true, false),
                "Vanilla Player.take feedback must not be doubled");
        assertTrue(OffhandPickupAudioDecision.shouldSendFallback(4, false, false),
                "Standalone custom movement without Player.take gets one normal fallback");
        assertFalse(OffhandPickupAudioDecision.shouldSendFallback(0, false, false),
                "Full or no-space pickups stay silent when nothing was custom-routed");
        assertFalse(OffhandPickupAudioDecision.shouldSendFallback(4, false, true),
                "CCAR coexistence preserves the established Offhand yield seam");
    }

    @Test
    void fallbackIsGroundPickupOnlyAndUsesVanillaAudioSemantics() throws Exception {
        Path root = Path.of(System.getProperty("workspaceRoot"));
        String pickupMixin = Files.readString(root.resolve(
                "projects/offhand-shift-click-qol/src/main/java/dev/resivore/offhandqol/mixin/ItemEntityMixin.java"));
        String client = Files.readString(root.resolve(
                "projects/offhand-shift-click-qol/src/main/java/dev/resivore/offhandqol/client/OffhandShiftClickQolClient.java"));
        String metadata = Files.readString(root.resolve(
                "projects/offhand-shift-click-qol/src/main/resources/fabric.mod.json"));

        assertTrue(pickupMixin.contains("at = @At(\"HEAD\")"));
        assertTrue(pickupMixin.contains("OffhandRoutingService.routeIncoming(player, incoming, -1, false)"));
        assertTrue(pickupMixin.contains("offhandQol$customItemsMoved = beforeRouting - incoming.getCount();"));
        assertTrue(pickupMixin.contains("Player;take(Lnet/minecraft/world/entity/Entity;I)V"));
        assertTrue(pickupMixin.contains("offhandQol$vanillaTakeReached = true;"));
        assertTrue(pickupMixin.contains("at = @At(\"RETURN\")"));
        assertTrue(pickupMixin.contains("OffhandPickupAudioDecision.shouldSendFallback("));
        assertTrue(pickupMixin.contains("new OffhandPickupSoundFallbackPayload("));
        assertTrue(pickupMixin.contains("isModLoaded(\"carried_container_auto_routing\")"));
        assertFalse(pickupMixin.contains("QUICK_MOVE"));
        assertFalse(pickupMixin.contains("ClientboundTakeItemEntityPacket"));
        assertTrue(client.contains("SoundEvents.ITEM_PICKUP"));
        assertTrue(client.contains("SoundSource.PLAYERS"));
        assertTrue(client.contains("0.2F, pitch, false"));
        assertTrue(client.contains("(client.level.getRandom().nextFloat() - client.level.getRandom().nextFloat()) * 1.4F + 2.0F"));
        assertTrue(metadata.contains("OffhandShiftClickQolClient"));
    }
}

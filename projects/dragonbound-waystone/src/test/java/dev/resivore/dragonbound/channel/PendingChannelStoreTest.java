package dev.resivore.dragonbound.channel;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PendingChannelStoreTest {
    @Test
    void eachPlayerCanHaveOnlyOnePendingChannel() {
        PendingChannelStore store = new PendingChannelStore();
        UUID playerId = UUID.randomUUID();

        assertTrue(store.add(channel(playerId, 11L)));
        assertFalse(store.add(channel(playerId, 12L)));
        assertEquals(1, store.size());
        assertTrue(store.isCurrent(playerId, 11L));
        assertFalse(store.isCurrent(playerId, 12L));
    }

    @Test
    void staleTokenCannotRemoveAReplacement() {
        PendingChannelStore store = new PendingChannelStore();
        UUID playerId = UUID.randomUUID();

        assertTrue(store.add(channel(playerId, 20L)));
        assertTrue(store.removeExact(playerId, 20L).isPresent());
        assertTrue(store.add(channel(playerId, 21L)));

        assertTrue(store.removeExact(playerId, 20L).isEmpty());
        assertTrue(store.isCurrent(playerId, 21L));
        assertEquals(1, store.size());
    }

    @Test
    void exactRemovalIsIdempotent() {
        PendingChannelStore store = new PendingChannelStore();
        UUID playerId = UUID.randomUUID();

        assertTrue(store.add(channel(playerId, 30L)));
        assertTrue(store.removeExact(playerId, 30L).isPresent());
        assertTrue(store.removeExact(playerId, 30L).isEmpty());
        assertFalse(store.isCurrent(playerId, 30L));
        assertEquals(0, store.size());
    }

    private static PendingChannel channel(UUID playerId, long token) {
        return new PendingChannel(
                playerId,
                token,
                null,
                null,
                null,
                0,
                40,
                0,
                null,
                ReturnSource.IMBUED_VOID_PEARL,
                null,
                1_200);
    }
}

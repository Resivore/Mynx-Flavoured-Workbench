package dev.resivore.carriedrouting.client;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.RandomSource;

/** Client-thread-only, event-scoped marker consumed by the matching vanilla take-item packet. */
public final class RoutedPickupSoundState {
    public static final float PITCH_MULTIPLIER = 0.80F;
    private static final Set<Integer> ROUTED_ENTITY_IDS = new HashSet<>();

    private RoutedPickupSoundState() {}

    public static void mark(int itemEntityId) { ROUTED_ENTITY_IDS.add(itemEntityId); }

    public static boolean consume(int itemEntityId) { return ROUTED_ENTITY_IDS.remove(itemEntityId); }

    /** Exact Minecraft 26.2 item branch from ClientPacketListener.handleTakeItemEntity. */
    public static float vanillaPickupPitch(RandomSource random) {
        return (random.nextFloat() - random.nextFloat()) * 1.4F + 2.0F;
    }
}

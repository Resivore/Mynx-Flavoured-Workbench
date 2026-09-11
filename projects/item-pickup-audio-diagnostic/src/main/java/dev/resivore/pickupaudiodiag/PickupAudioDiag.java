package dev.resivore.pickupaudiodiag;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds diagnostic-only correlation metadata. It never reaches Minecraft state,
 * packets, CCAR, sound arguments, playback APIs, disk, or configuration.
 */
public final class PickupAudioDiag {
    public static final Identifier ITEM_PICKUP_ID = Identifier.fromNamespaceAndPath("minecraft", "entity.item.pickup");

    private static final Logger LOGGER = LoggerFactory.getLogger("PickupAudioDiag");
    private static final AtomicLong NEXT_SEQUENCE = new AtomicLong();
    private static final Map<ClientboundTakeItemEntityPacket, Trace> PACKET_TRACES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Trace> LOCAL_SOUND_TRACE = new ThreadLocal<>();

    private PickupAudioDiag() {
    }

    public static boolean isPickupIdentifier(Identifier identifier) {
        return ITEM_PICKUP_ID.equals(identifier);
    }

    public static void logInitialization(
            String minecraftVersion,
            String ccarVersion,
            String soundPhysicsPerfectedVersion,
            String soundPhysicsShutdownCompatVersion,
            String inventoryExtendedVersion) {
        LOGGER.info("[PickupAudioDiag][INIT] minecraft={} ccar={} sound_physics_perfected={} "
                        + "sound_physics_shutdown_compat={} inventory_extended={}",
                minecraftVersion,
                ccarVersion,
                soundPhysicsPerfectedVersion,
                soundPhysicsShutdownCompatVersion,
                inventoryExtendedVersion);
    }

    public static void packetReceived(ClientboundTakeItemEntityPacket packet) {
        Trace trace = new Trace(NEXT_SEQUENCE.incrementAndGet(), System.nanoTime());
        PACKET_TRACES.put(packet, trace);
        log(trace, "PACKET", "itemEntity={} collector={} amount={}",
                packet.getItemId(), packet.getPlayerId(), packet.getAmount());
    }

    public static void packetCompleted(ClientboundTakeItemEntityPacket packet) {
        PACKET_TRACES.remove(packet);
    }

    /** Starts a synchronous context only around vanilla's exact local-sound invocation. */
    public static void beginVanillaLocalSound(ClientboundTakeItemEntityPacket packet) {
        Trace trace = PACKET_TRACES.get(packet);
        if (trace != null) {
            LOCAL_SOUND_TRACE.set(trace);
        }
    }

    /** Clears the diagnostic context after that exact local-sound invocation returns. */
    public static void finishVanillaLocalSound() {
        LOCAL_SOUND_TRACE.remove();
    }

    public static void localSound(
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch,
            boolean distanceDelay) {
        if (isPickupIdentifier(sound.location())) {
            log(currentTrace(), "LOCAL_SOUND",
                    "event={} source={} volume={} pitch={} x={} y={} z={} distanceDelay={}",
                    sound.location(), source, volume, pitch, x, y, z, distanceDelay);
        }
    }

    public static void soundManager(SoundInstance soundInstance) {
        if (isPickup(soundInstance)) {
            logSound(currentTrace(), "MANAGER", soundInstance);
        }
    }

    public static void engineEnter(SoundInstance soundInstance) {
        if (isPickup(soundInstance)) {
            logSound(currentTrace(), "ENGINE_ENTER", soundInstance);
        }
    }

    /** This point is immediately before SoundEngine's first vanilla field read, after all HEAD callbacks. */
    public static void engineSurvivedHead(SoundInstance soundInstance) {
        if (isPickup(soundInstance)) {
            logSound(currentTrace(), "ENGINE_SURVIVED_HEAD", soundInstance);
        }
    }

    /** This point follows SoundInstance.resolve inside the actual 26.2 SoundEngine.play body. */
    public static void resolvedSound(SoundInstance soundInstance) {
        if (!isPickup(soundInstance)) {
            return;
        }

        Sound resolved = soundInstance.getSound();
        log(currentTrace(), "RESOLVED",
                "event={} sampleLocation={} samplePath={} sampleType={} stream={} attenuationDistance={}",
                soundInstance.getIdentifier(),
                resolved.getLocation(),
                resolved.getPath(),
                resolved.getType(),
                resolved.shouldStream(),
                resolved.getAttenuationDistance());
    }

    public static void engineReturned(SoundInstance soundInstance, SoundEngine.PlayResult result) {
        if (isPickup(soundInstance)) {
            log(currentTrace(), "ENGINE_RETURN", "event={} result={}", soundInstance.getIdentifier(), result);
        }
    }

    private static boolean isPickup(SoundInstance soundInstance) {
        return isPickupIdentifier(soundInstance.getIdentifier());
    }

    private static void logSound(Trace trace, String checkpoint, SoundInstance soundInstance) {
        log(trace, checkpoint,
                "event={} source={} volume={} pitch={} attenuation={} relative={} x={} y={} z={} soundClass={}",
                soundInstance.getIdentifier(),
                soundInstance.getSource(),
                soundInstance.getVolume(),
                soundInstance.getPitch(),
                soundInstance.getAttenuation(),
                soundInstance.isRelative(),
                soundInstance.getX(),
                soundInstance.getY(),
                soundInstance.getZ(),
                soundInstance.getClass().getName());
    }

    private static Trace currentTrace() {
        return LOCAL_SOUND_TRACE.get();
    }

    private static void log(Trace trace, String checkpoint, String message, Object... values) {
        String sequence = trace == null ? "unassociated" : Long.toString(trace.sequence());
        long elapsedNanos = trace == null ? -1L : System.nanoTime() - trace.packetReceivedAtNanos();
        Object[] logValues = new Object[4 + values.length];
        logValues[0] = sequence;
        logValues[1] = checkpoint;
        logValues[2] = Thread.currentThread().getName();
        logValues[3] = elapsedNanos;
        System.arraycopy(values, 0, logValues, 4, values.length);
        LOGGER.info("[PickupAudioDiag][{}][{}] thread={} elapsedNanos={} " + message,
                logValues);
    }

    private record Trace(long sequence, long packetReceivedAtNanos) {
    }
}

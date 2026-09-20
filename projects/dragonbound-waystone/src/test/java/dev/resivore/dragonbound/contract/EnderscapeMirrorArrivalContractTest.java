package dev.resivore.dragonbound.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static contract for Dragonbound's pinned Enderscape Mirror arrival-provider integration. */
final class EnderscapeMirrorArrivalContractTest {
    private static final String ENDERSCAPE_SHA256 =
            "9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b";
    private static final String MIRROR_TELEPORT_REGISTER_HOLDER_ARGUMENT = "mirror.teleport";
    private static final String MIRROR_TELEPORT_REGISTRY_PATH =
            "item." + MIRROR_TELEPORT_REGISTER_HOLDER_ARGUMENT;
    private static final String MIRROR_TRANSDIMENSIONAL_TRAVEL_REGISTER_HOLDER_ARGUMENT =
            "mirror.transdimensional_travel";
    private static final String MIRROR_TRANSDIMENSIONAL_TRAVEL_REGISTRY_PATH =
            "item." + MIRROR_TRANSDIMENSIONAL_TRAVEL_REGISTER_HOLDER_ARGUMENT;
    private static final String ITEM_SOUND_REGISTRATION_TEMPLATE = "item.\u0001";
    private static final Path JAVA_ROOT = Path.of("src/main/java/dev/resivore/dragonbound");
    private static final Path ENDERSCAPE_JAR = Path.of(System.getProperty("workbenchRoot", "../.."))
            .resolve("originals/mods/enderscape-fabric-3.0.2+mc26.2.jar");

    @Test
    void exactPinnedEnderscapeInputProvidesTheMirrorArrivalRegistries() throws IOException, NoSuchAlgorithmException {
        assertTrue(Files.isRegularFile(ENDERSCAPE_JAR));
        assertEquals(ENDERSCAPE_SHA256, sha256(ENDERSCAPE_JAR));

        try (ZipFile archive = new ZipFile(ENDERSCAPE_JAR.toFile())) {
            assertNotNull(archive.getEntry("assets/enderscape/particles/mirror_teleport_in.json"));
            assertNotNull(archive.getEntry("assets/enderscape/sounds.json"));
            assertNotNull(archive.getEntry(
                    "net/penumbra/enderscape/registry/particle/EnderscapeParticles.class"));
            assertNotNull(archive.getEntry(
                    "net/penumbra/enderscape/registry/sound/EnderscapeItemSounds.class"));
            assertNotNull(archive.getEntry(
                    "net/penumbra/enderscape/network/ClientboundLodestoneTeleportationInfoPayload.class"));
            assertNotNull(archive.getEntry(
                    "net/penumbra/enderscape/network/ClientboundTransdimensionalTravelSoundPayload.class"));
            assertNotNull(archive.getEntry(
                    "net/penumbra/enderscape/registry/server/EnderscapeServerNetworking.class"));
            assertNotNull(archive.getEntry(
                    "net/penumbra/enderscape/item/component/value/LodestoneTeleportationVisuals.class"));

            String particleRegistrations = new String(
                    archive.getInputStream(archive.getEntry(
                            "net/penumbra/enderscape/registry/particle/EnderscapeParticles.class"))
                            .readAllBytes(),
                    StandardCharsets.ISO_8859_1);
            String soundRegistrations = new String(
                    archive.getInputStream(archive.getEntry(
                            "net/penumbra/enderscape/registry/sound/EnderscapeItemSounds.class"))
                            .readAllBytes(),
                    StandardCharsets.ISO_8859_1);
            String sounds = new String(
                    archive.getInputStream(archive.getEntry("assets/enderscape/sounds.json")).readAllBytes(),
                    StandardCharsets.UTF_8);

            assertTrue(particleRegistrations.contains("mirror_teleport_in"));
            // EnderscapeItemSounds.registerHolder prefixes its local argument with "item."
            // before passing it to Enderscape.registerSoundEventHolder. The literal argument in
            // its static initializer is therefore not the final registry path.
            assertTrue(soundRegistrations.contains(MIRROR_TELEPORT_REGISTER_HOLDER_ARGUMENT));
            assertTrue(soundRegistrations.contains(MIRROR_TRANSDIMENSIONAL_TRAVEL_REGISTER_HOLDER_ARGUMENT));
            assertTrue(soundRegistrations.contains(ITEM_SOUND_REGISTRATION_TEMPLATE));
            assertEquals("item.mirror.teleport", MIRROR_TELEPORT_REGISTRY_PATH);
            assertEquals("item.mirror.transdimensional_travel",
                    MIRROR_TRANSDIMENSIONAL_TRAVEL_REGISTRY_PATH);
            assertTrue(sounds.contains("\"" + MIRROR_TELEPORT_REGISTRY_PATH + "\""));
            assertTrue(sounds.contains("\"" + MIRROR_TRANSDIMENSIONAL_TRAVEL_REGISTRY_PATH + "\""));
        }
    }

    @Test
    void confirmedArrivalIsTheOnlyMirrorEffectSiteAndPreservesSuccessOrdering() throws IOException {
        String manager = Files.readString(JAVA_ROOT.resolve("channel/DragonboundChannelManager.java"));
        String effects = Files.readString(JAVA_ROOT.resolve("channel/ChannelEffects.java"));

        int teleport = manager.indexOf("arrived = player.teleport(new TeleportTransition(");
        int arrivalConfirmed = manager.indexOf("ChannelRules.shouldApplySuccessEffect(confirmed)");
        int heldItemConfirmed = manager.indexOf("if (!heldItemConfirmed)");
        int success = manager.indexOf("ChannelEffects.successfulTeleport(arrived,");
        int pearlConsumption = manager.indexOf("arrivedHeld.shrink(1)");
        int staffCooldown = manager.indexOf("addCooldown(arrivedHeld, channel.staffCooldownTicks())");

        assertTrue(teleport >= 0);
        assertTrue(teleport < arrivalConfirmed);
        assertTrue(arrivalConfirmed < heldItemConfirmed);
        assertTrue(heldItemConfirmed < success);
        assertTrue(success < pearlConsumption);
        assertTrue(success < staffCooldown);
        assertEquals(1, occurrences(manager, "ChannelEffects.successfulTeleport(arrived,"));
        assertTrue(manager.contains("terminalCleanup(channel, player, ChannelCancellation.TELEPORT_FAILED, false)"));
        assertTrue(manager.contains("terminalCleanup(channel, arrived, ChannelCancellation.TELEPORT_FAILED, false)"));

        String channelStart = effects.substring(
                effects.indexOf("static void channelStarted"), effects.indexOf("static void channelTick"));
        String channelTick = effects.substring(
                effects.indexOf("static void channelTick"), effects.indexOf("static int channelParticleCount"));
        assertFalse(channelStart.contains("MIRROR_"));
        assertFalse(channelTick.contains("MIRROR_"));
        assertFalse(effects.contains("mirror_teleport_out"));
        assertFalse(effects.contains("SoundEvents.ENDERMAN_TELEPORT"));
        assertFalse(effects.contains("portalBurst("));
        assertFalse(effects.contains("foregroundPortalBurst("));
        assertEquals(1, occurrences(effects, "ParticleTypes.PORTAL"));
        assertEquals(1, occurrences(effects, "ParticleTypes.REVERSE_PORTAL"));
    }

    @Test
    void interdimensionalExtrasUseEnderscapesOwnPayloadsOnlyForPostGateCrossDimensionSuccesses()
            throws IOException {
        String manager = Files.readString(JAVA_ROOT.resolve("channel/DragonboundChannelManager.java"));
        String effects = Files.readString(JAVA_ROOT.resolve("channel/ChannelEffects.java"));

        int teleport = manager.indexOf("arrived = player.teleport(new TeleportTransition(");
        int arrivalConfirmed = manager.indexOf("ChannelRules.shouldApplySuccessEffect(confirmed)");
        int heldItemConfirmed = manager.indexOf("if (!heldItemConfirmed)");
        int success = manager.indexOf("ChannelEffects.successfulTeleport(arrived,");
        int crossDimension = manager.indexOf("isCrossDimensionReturn(channel.startDimension(), channel.anchor().dimension())");

        assertTrue(teleport < arrivalConfirmed);
        assertTrue(arrivalConfirmed < heldItemConfirmed);
        assertTrue(heldItemConfirmed < success);
        assertTrue(success < crossDimension);
        assertEquals(1, occurrences(manager, "ChannelEffects.successfulTeleport(arrived,"));
        assertFalse(manager.substring(0, success).contains("successfulInterdimensionalReturn"));
        String cancellationAndFailurePaths = manager.substring(manager.indexOf("private void cancel("));
        assertFalse(cancellationAndFailurePaths.contains("ChannelEffects.successfulTeleport"));

        String successfulTeleport = effects.substring(
                effects.indexOf("static void successfulTeleport"), effects.indexOf("static void channelStarted"));
        int normalArrivalParticles = successfulTeleport.indexOf("level.sendParticles(");
        int normalArrivalSound = successfulTeleport.indexOf("level.playSound(");
        int crossDimensionBranch = successfulTeleport.indexOf("if (crossDimension)");
        int extras = successfulTeleport.indexOf("successfulInterdimensionalReturn(player)");

        assertTrue(normalArrivalParticles >= 0 && normalArrivalParticles < crossDimensionBranch);
        assertTrue(normalArrivalSound >= 0 && normalArrivalSound < crossDimensionBranch);
        assertTrue(crossDimensionBranch < extras);

        String interdimensional = effects.substring(
                effects.indexOf("private static void successfulInterdimensionalReturn"),
                effects.indexOf("private static Identifier mirrorTransdimensionalTravelSoundId"));
        assertTrue(interdimensional.contains("EnderscapeServerNetworking.sendLodestoneTeleportationInfoPayload"));
        assertTrue(interdimensional.contains("LodestoneTeleportationVisuals.DEFAULT"));
        assertTrue(interdimensional.contains("ClientboundTransdimensionalTravelSoundPayload"));
        assertTrue(interdimensional.contains("ServerPlayNetworking.send"));
        assertTrue(interdimensional.contains("true,"));
        assertFalse(effects.contains("mirror_teleport_out"));
        assertFalse(effects.contains("doPreTeleportEffects"));
    }

    @Test
    void requiredProviderIsDeclaredAndNoAssetFallbackIsPackaged() throws IOException {
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        String build = Files.readString(Path.of("build.gradle"));
        String effects = Files.readString(JAVA_ROOT.resolve("channel/ChannelEffects.java"));

        assertTrue(metadata.contains("\"enderscape\": \"=3.0.2\""));
        assertTrue(build.contains("compileOnly files(enderscapeJar)"));
        assertTrue(build.contains(ENDERSCAPE_SHA256));
        assertTrue(effects.contains("orElseThrow"));
        assertTrue(effects.contains("MIRROR_TRANSDIMENSIONAL_TRAVEL_SOUND_ID"));
        assertTrue(effects.contains("item.mirror.transdimensional_travel"));
        String successfulTeleport = effects.substring(
                effects.indexOf("static void successfulTeleport"), effects.indexOf("static void channelStarted"));
        assertFalse(successfulTeleport.contains("ParticleTypes.PORTAL"));
        assertFalse(successfulTeleport.contains("ParticleTypes.REVERSE_PORTAL"));
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int count; (count = input.read(buffer)) > 0;) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}

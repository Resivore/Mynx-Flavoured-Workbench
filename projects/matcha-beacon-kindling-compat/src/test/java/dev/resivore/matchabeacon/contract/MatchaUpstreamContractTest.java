package dev.resivore.matchabeacon.contract;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class MatchaUpstreamContractTest {
    private static final String EXPECTED_ARCHIVE_SHA_256 =
            "966843D0AEB3860479721EF34791A631E62BC225FDAA918DD2AD90C49977E5B4";
    private static final String FUNCTION_DIRECTORY = "data/matcha/function/mechanics/wandering_trader/";
    private static final Set<String> EXPECTED_FUNCTION_ENTRIES = Set.of(
            FUNCTION_DIRECTORY + "beacon_kindling_placed.mcfunction",
            FUNCTION_DIRECTORY + "check_wandering_trader_timer_loop.mcfunction",
            FUNCTION_DIRECTORY + "initialise_wandering_trader_spawn.mcfunction",
            FUNCTION_DIRECTORY + "summon_wandering_trader.mcfunction",
            FUNCTION_DIRECTORY + "kill_wandering_trader.mcfunction",
            FUNCTION_DIRECTORY + "kill_wandering_trader_early.mcfunction",
            FUNCTION_DIRECTORY + "kill_this_beacon.mcfunction", FUNCTION_DIRECTORY + "no_beacons_reset.mcfunction");

    @Test
    void pinsThePristineMatcha112Archive() throws IOException, NoSuchAlgorithmException {
        Path archive = archivePath();
        assertTrue(Files.isRegularFile(archive), () -> "Missing pristine Matcha archive: " + archive);

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(archive)) {
            byte[] buffer = new byte[16 * 1024];
            for (int read; (read = input.read(buffer)) != -1; ) {
                sha256.update(buffer, 0, read);
            }
        }

        String actual = HexFormat.of().formatHex(sha256.digest()).toUpperCase(Locale.ROOT);
        assertEquals(EXPECTED_ARCHIVE_SHA_256, actual, "Unexpected Matcha Flavoured 1.12 archive identity");
    }

    @Test
    void exposesTheExpectedSpawnEggMarkerAndAdvancementRewardSeam() throws IOException {
        try (ZipFile archive = openArchive()) {
            String recipe = compactJson(readEntry(archive, "data/matcha/recipe/crafting/beacon_kindling.json"));
            assertTrue(recipe.contains("\"id\":\"minecraft:chicken_spawn_egg\""),
                    "Beacon Kindling must remain a spawn egg so its exact spawned marker can be captured");
            assertTrue(recipe.contains(
                            "\"minecraft:entity_data\":{\"id\":\"minecraft:marker\","
                                    + "\"NoGravity\":true,\"Invulnerable\":true,"
                                    + "\"Tags\":[\"beacon_kindling\"]}"),
                    "Beacon Kindling must still spawn an invulnerable beacon_kindling marker");
            assertTrue(recipe.contains("\"minecraft:item_model\":\"matcha:beacon_kindling\""),
                    "The compatibility hook identifies Matcha's item-model component");

            String advancement = compactJson(
                    readEntry(archive, "data/matcha/advancement/mechanics/beacon_kindling.json"));
            assertTrue(advancement.contains("\"trigger\":\"minecraft:item_used_on_block\""));
            assertTrue(advancement.contains("\"condition\":\"minecraft:match_tool\""));
            assertTrue(advancement.contains(
                    "\"components\":{\"minecraft:item_model\":\"matcha:beacon_kindling\"}"));
            assertTrue(advancement.contains(
                    "\"rewards\":{\"function\":"
                            + "\"matcha:mechanics/wandering_trader/beacon_kindling_placed\"}"));
            assertFalse(advancement.contains("minecraft:reference"),
                    "The reward seam should not depend on a separate predicate resource");
        }
    }

    @Test
    void pinsTheSevenLegacyFunctionIdsAndTheirInterceptionContract() throws IOException {
        try (ZipFile archive = openArchive()) {
            Set<String> actualEntries = archive.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith(FUNCTION_DIRECTORY) && name.endsWith(".mcfunction"))
                    .collect(Collectors.toUnmodifiableSet());
            assertEquals(EXPECTED_FUNCTION_ENTRIES, actualEntries,
                    "The mixin interception allow-list must cover Matcha's complete legacy lifecycle");

            String loop = readEntry(archive, FUNCTION_DIRECTORY + "check_wandering_trader_timer_loop.mcfunction");
            assertTrue(loop.contains("run function matcha:mechanics/wandering_trader/no_beacons_reset"));
            assertContainsLines(readEntry(archive, FUNCTION_DIRECTORY + "no_beacons_reset.mcfunction"),
                    "kill @e[type=minecraft:wandering_trader,tag=summoned_by_beacon]",
                    "scoreboard players reset @a wandering_trader_timer_score",
                    "tag @a remove SummonedTrader");
            assertContainsLines(readEntry(archive, FUNCTION_DIRECTORY + "summon_wandering_trader.mcfunction"),
                    "tag @s add summoned_trader",
                    "summon wandering_trader ^1 ^ ^ {Invulnerable:1b,Tags:[\"summoned_by_beacon\"]}");
        }
    }

    @Test
    void pinsTimerFakeScoresAndSetupCleanupCaller() throws IOException {
        try (ZipFile archive = openArchive()) {
            String setup = readEntry(archive, "data/matcha/function/setup/scoreboard/create_scoreboards.mcfunction");
            assertContainsLines(setup,
                    "scoreboard objectives add wandering_trader_timer_score dummy",
                    "scoreboard players reset @a wandering_trader_timer_score",
                    "tag @a remove SummonedTrader",
                    "scoreboard players set 10min wandering_trader_timer_score 60",
                    "scoreboard players set 15min wandering_trader_timer_score 90",
                    "scoreboard players set 0 wandering_trader_timer_score 0");

            String load = readEntry(archive, "data/matcha/function/setup/load.mcfunction");
            assertContainsLines(load, "function matcha:setup/scoreboard/create_scoreboards");
        }
    }

    private static Path archivePath() {
        return requiredSystemPath("workbenchRoot")
                .resolve("projects/matcha-flavoured-data/upstream/MF_1_12_5_26_2.zip");
    }

    private static ZipFile openArchive() throws IOException {
        return new ZipFile(archivePath().toFile(), UTF_8);
    }

    private static Path requiredSystemPath(String propertyName) {
        String value = System.getProperty(propertyName);
        assertNotNull(value, () -> "Gradle must provide the " + propertyName + " system property");
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static String readEntry(ZipFile archive, String name) throws IOException {
        ZipEntry entry = archive.getEntry(name);
        assertNotNull(entry, () -> "Missing Matcha ZIP entry: " + name);
        try (InputStream input = archive.getInputStream(entry)) {
            return new String(input.readAllBytes(), UTF_8);
        }
    }

    private static String compactJson(String json) {
        return json.replaceAll("\\s+", "");
    }

    private static void assertContainsLines(String contents, String... expectedLines) {
        Set<String> actualLines = contents.lines()
                .map(String::stripTrailing)
                .collect(Collectors.toUnmodifiableSet());
        for (String expected : expectedLines) {
            assertTrue(actualLines.contains(expected), () -> "Missing exact upstream command: " + expected);
        }
    }
}

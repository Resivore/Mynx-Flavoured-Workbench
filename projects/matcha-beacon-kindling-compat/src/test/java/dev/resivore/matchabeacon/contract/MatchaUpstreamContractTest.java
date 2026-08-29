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
            "6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248";
    private static final String FUNCTION_DIRECTORY = "data/main/function/mechanic/wandering_trader/";
    private static final Set<String> EXPECTED_FUNCTION_ENTRIES = Set.of(
            FUNCTION_DIRECTORY + "beacon_kindling_placed.mcfunction",
            FUNCTION_DIRECTORY + "check_wandering_trader_timer_loop.mcfunction",
            FUNCTION_DIRECTORY + "initialise_wandering_trader_spawn.mcfunction",
            FUNCTION_DIRECTORY + "summon_wandering_trader.mcfunction",
            FUNCTION_DIRECTORY + "kill_wandering_trader.mcfunction",
            FUNCTION_DIRECTORY + "kill_wandering_trader_early.mcfunction",
            FUNCTION_DIRECTORY + "kill_this_beacon.mcfunction");

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
            String recipe = compactJson(readEntry(archive, "data/crafting/recipe/beacon_kindling.json"));
            assertTrue(recipe.contains("\"id\":\"minecraft:chicken_spawn_egg\""),
                    "Beacon Kindling must remain a spawn egg so its exact spawned marker can be captured");
            assertTrue(recipe.contains(
                            "\"minecraft:entity_data\":{\"id\":\"minecraft:marker\","
                                    + "\"NoGravity\":true,\"Invulnerable\":true,"
                                    + "\"Tags\":[\"beacon_kindling\"]}"),
                    "Beacon Kindling must still spawn an invulnerable beacon_kindling marker");
            assertTrue(recipe.contains("\"minecraft:item_model\":\"minecraft:beacon_kindling\""),
                    "The compatibility hook identifies Matcha's item-model component");

            String advancement = compactJson(
                    readEntry(archive, "data/main/advancement/mechanics/beacon_kindling.json"));
            assertTrue(advancement.contains("\"trigger\":\"minecraft:item_used_on_block\""));
            assertTrue(advancement.contains("\"condition\":\"minecraft:match_tool\""));
            assertTrue(advancement.contains(
                    "\"components\":{\"minecraft:item_model\":\"minecraft:beacon_kindling\"}"));
            assertTrue(advancement.contains(
                    "\"rewards\":{\"function\":"
                            + "\"main:mechanic/wandering_trader/beacon_kindling_placed\"}"));
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

            String placed = readEntry(archive, FUNCTION_DIRECTORY + "beacon_kindling_placed.mcfunction");
            assertContainsLines(placed,
                    "execute at @n[type=marker,tag=beacon_kindling] run setblock ~ ~ ~ minecraft:campfire[signal_fire=true]",
                    "execute at @n[type=marker,tag=beacon_kindling] run particle minecraft:flame ~ ~.7 ~ .1 .1 .1 0.07 30",
                    "execute at @n[type=marker,tag=beacon_kindling] run playsound minecraft:item.firecharge.use block @a ~ ~ ~ 1",
                    "execute at @n[type=marker,tag=beacon_kindling] run playsound minecraft:entity.wither.spawn block @a ~ ~ ~ 0.5",
                    "advancement revoke @s only main:mechanics/beacon_kindling",
                    "execute if entity @s[tag=SummonedTrader] run tellraw @s {\"text\":\"You have already summoned a Wandering Trader, please wait patiently while they travel\",\"color\":\"gray\"}",
                    "execute if entity @s[tag=SummonedTrader] run execute as @s run function main:mechanic/wandering_trader/kill_this_beacon",
                    "execute if entity @s[tag=!SummonedTrader] run execute as @s run function main:mechanic/wandering_trader/initialise_wandering_trader_spawn",
                    "execute if entity @s[tag=!SummonedTrader] run tag @s add SummonedTrader");

            String initialise = readEntry(
                    archive, FUNCTION_DIRECTORY + "initialise_wandering_trader_spawn.mcfunction");
            assertContainsLines(initialise,
                    "tellraw @a {\"text\":\"A Wandering Trader has spotted your beacon, they will arrive in 10 minutes\",\"color\":\"gray\"}",
                    "scoreboard players add @n[type=marker,tag=beacon_kindling] wandering_trader_timer_score 0",
                    "schedule function main:mechanic/wandering_trader/check_wandering_trader_timer_loop 1s");

            String loop = readEntry(
                    archive, FUNCTION_DIRECTORY + "check_wandering_trader_timer_loop.mcfunction");
            assertContainsLines(loop,
                    "execute as @e[type=marker,tag=beacon_kindling,tag=!summoned_trader] run execute if score @s wandering_trader_timer_score >= 10min wandering_trader_timer_score run execute as @s run function main:mechanic/wandering_trader/summon_wandering_trader",
                    "execute as @e[type=marker,tag=beacon_kindling,tag=summoned_trader] run execute if score @s wandering_trader_timer_score >= 15min wandering_trader_timer_score run execute as @s run function main:mechanic/wandering_trader/kill_wandering_trader",
                    "execute at @e[type=marker,tag=beacon_kindling] run execute unless block ~ ~ ~ minecraft:campfire[lit=true] run function main:mechanic/wandering_trader/kill_wandering_trader_early",
                    "execute as @a[tag=SummonedTrader] run scoreboard players add @s wandering_trader_timer_score 1",
                    "execute as @e[type=marker,tag=beacon_kindling] run execute if score @s wandering_trader_timer_score < 15min wandering_trader_timer_score run scoreboard players add @s wandering_trader_timer_score 1",
                    "execute as @e[type=marker,tag=beacon_kindling] run execute if score @s wandering_trader_timer_score <= 15min wandering_trader_timer_score run schedule function main:mechanic/wandering_trader/check_wandering_trader_timer_loop 10s");

            String summon = readEntry(archive, FUNCTION_DIRECTORY + "summon_wandering_trader.mcfunction");
            assertContainsLines(summon,
                    "execute as @s run tag @s add summoned_trader",
                    "execute at @s run summon wandering_trader ^1 ^ ^ {Invulnerable:1b,Tags:[\"summoned_by_beacon\"]}",
                    "tellraw @a {\"text\":\"The Wandering Trader has arrived, they will depart in 5 minutes\",\"color\":\"gray\"}");

            String normalKill = readEntry(archive, FUNCTION_DIRECTORY + "kill_wandering_trader.mcfunction");
            assertContainsLines(normalKill,
                    "tp @n[type=minecraft:wandering_trader,tag=summoned_by_beacon] ~ ~-1000 ~",
                    "function main:mechanic/wandering_trader/kill_this_beacon",
                    "tellraw @a {\"text\":\"The Wandering Trader has left\",\"color\":\"gray\"}",
                    "execute as @a run execute if score @s wandering_trader_timer_score >= 0 wandering_trader_timer_score run tag @s remove SummonedTrader");

            String earlyKill = readEntry(
                    archive, FUNCTION_DIRECTORY + "kill_wandering_trader_early.mcfunction");
            assertContainsLines(earlyKill,
                    "kill @n[type=minecraft:wandering_trader,tag=summoned_by_beacon]",
                    "function main:mechanic/wandering_trader/kill_this_beacon",
                    "execute at @a run execute if score @p wandering_trader_timer_score >= 0 wandering_trader_timer_score run tellraw @p {\"text\":\"The Wandering Trader has lost sight of your beacon...\",\"color\":\"gray\"}",
                    "execute as @a run execute if score @s wandering_trader_timer_score >= 0 wandering_trader_timer_score run tag @s remove SummonedTrader");

            String killBeacon = readEntry(archive, FUNCTION_DIRECTORY + "kill_this_beacon.mcfunction");
            assertContainsLines(killBeacon,
                    "scoreboard players reset @s[type=minecraft:marker,tag=beacon_kindling]",
                    "kill @n[type=marker,tag=beacon_kindling]");
        }
    }

    @Test
    void pinsTimerFakeScoresAndSetupCleanupCaller() throws IOException {
        try (ZipFile archive = openArchive()) {
            String setup = readEntry(archive, "data/main/function/setup/scoreboard.mcfunction");
            assertContainsLines(setup,
                    "function main:mechanic/wandering_trader/kill_wandering_trader_early",
                    "scoreboard objectives add wandering_trader_timer_score dummy",
                    "scoreboard players reset @a wandering_trader_timer_score",
                    "tag @a remove SummonedTrader",
                    "scoreboard players set 10min wandering_trader_timer_score 60",
                    "scoreboard players set 15min wandering_trader_timer_score 90",
                    "scoreboard players set 0 wandering_trader_timer_score 0");

            String load = readEntry(archive, "data/main/function/setup/load.mcfunction");
            assertContainsLines(load, "function main:setup/scoreboard");

            String playerUpdate = readEntry(archive, "data/main/function/setup/update_players.mcfunction");
            assertContainsLines(playerUpdate,
                    "execute if score @p version_number < current_version version_number run function main:setup/scoreboard");
        }
    }

    private static Path archivePath() {
        return requiredSystemPath("workbenchRoot")
                .resolve("originals")
                .resolve("datapacks")
                .resolve("Matcha_Flavoured_1_12.zip");
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

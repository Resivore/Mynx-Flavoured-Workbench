package dev.resivore.matchabeacon.contract;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CoordinatorArchitectureTest {
    private static final String COORDINATOR =
            "dev/resivore/matchabeacon/runtime/BeaconKindlingCoordinator.java";
    private static final String CONTRACT = "dev/resivore/matchabeacon/runtime/MatchaContract.java";
    private static final String FUNCTION_GUARD =
            "dev/resivore/matchabeacon/runtime/FunctionExecutionGuard.java";
    private static final String FUNCTION_MIXIN =
            "dev/resivore/matchabeacon/mixin/ServerFunctionManagerMixin.java";
    private static final String NESTED_FUNCTION_MIXIN =
            "dev/resivore/matchabeacon/mixin/FunctionCommandMixin.java";
    private static final String SAVED_DATA =
            "dev/resivore/matchabeacon/state/BeaconKindlingSavedData.java";
    private static final String SUMMON_RECORD = "dev/resivore/matchabeacon/state/SummonRecord.java";

    private static final Set<String> EXPECTED_PATCHED_IDS = Set.of(
            "main:mechanic/wandering_trader/beacon_kindling_placed",
            "main:mechanic/wandering_trader/check_wandering_trader_timer_loop",
            "main:mechanic/wandering_trader/initialise_wandering_trader_spawn",
            "main:mechanic/wandering_trader/summon_wandering_trader",
            "main:mechanic/wandering_trader/kill_wandering_trader",
            "main:mechanic/wandering_trader/kill_wandering_trader_early",
            "main:mechanic/wandering_trader/kill_this_beacon");

    @Test
    void coordinatorUsesServerGlobalSavedDataAndExactServerEvents() throws IOException {
        String coordinator = normalized(source(COORDINATOR));

        assertTrue(coordinator.contains(
                        "server.getDataStorage().computeIfAbsent(BeaconKindlingSavedData.TYPE)"),
                "Summon ownership must live in the MinecraftServer-global SavedData store");
        assertFalse(coordinator.contains("level.getDataStorage("),
                "Per-dimension SavedData would split the global one-summon-per-player authority");
        assertTrue(coordinator.contains("ServerTickEvents.END_SERVER_TICK.register("));
        assertTrue(coordinator.contains("ServerPlayConnectionEvents.JOIN.register("));
        assertTrue(coordinator.contains("ServerEntityEvents.ENTITY_LOAD.register("));
    }

    @Test
    void lifecycleObservesOnlyAlreadyLoadedChunksAndEntities() throws IOException {
        String coordinator = source(COORDINATOR);
        String production = allProductionJava();

        assertTrue(coordinator.contains("getChunkNow("),
                "Beacon validation must use the non-loading chunk lookup");
        assertTrue(coordinator.contains("areEntitiesLoaded(ChunkPos.pack("),
                "Timers must pause until entities in the beacon chunk are available");
        assertFalse(Pattern.compile("\\.getChunk\\s*\\([^)]*\\btrue\\s*\\)", Pattern.DOTALL)
                        .matcher(production).find(),
                "Production must not request chunk generation/loading with getChunk(..., true)");
        for (String forbidden : List.of(
                "addRegionTicket(", "removeRegionTicket(", "addTicket(", "TicketType.",
                "setChunkForced(", "getForcedChunks(", "ForcedChunksSavedData", "forceChunk(")) {
            assertFalse(production.contains(forbidden),
                    () -> "Production must not ticket or force beacon chunks: " + forbidden);
        }
    }

    @Test
    void ownershipUsesPersistedExactMarkerAndTraderUuidsWithoutDiscoveryScans() throws IOException {
        String coordinator = normalized(source(COORDINATOR));
        String record = normalized(source(SUMMON_RECORD));
        String savedData = normalized(source(SAVED_DATA));
        String production = allProductionJava();

        assertTrue(record.contains("UUIDUtil.CODEC.fieldOf(\"marker_uuid\")"));
        assertTrue(record.contains("UUIDUtil.CODEC.optionalFieldOf(\"trader_uuid\")"));
        assertTrue(coordinator.contains("level.getEntity(record.markerId())"));
        assertTrue(coordinator.contains("record.traderId().map(level::getEntity)"));
        assertTrue(savedData.contains("findByMarker(UUID markerId)"));
        assertTrue(savedData.contains("findByTrader(UUID traderId)"));

        assertFalse(Pattern.compile("@[np]\\b").matcher(production).find(),
                "Nearest @n/@p selectors must never choose ownership");
        assertFalse(production.contains("getAllEntities("),
                "Global entity scans must not replace UUID-indexed load events and lookups");
        assertFalse(production.contains("getNearestPlayer("));
        assertFalse(production.contains("getNearestEntity("));
    }

    @Test
    void allSevenLegacyFunctionsAreBlockedAndOnlyExactMarkerSummonIsGuarded() throws IOException {
        String contract = source(CONTRACT);
        String mixin = normalized(source(FUNCTION_MIXIN));
        String nestedMixin = normalized(source(NESTED_FUNCTION_MIXIN));
        String guard = normalized(source(FUNCTION_GUARD));
        String coordinator = normalized(source(COORDINATOR));

        Set<String> actualIds = new HashSet<>();
        Matcher ids = Pattern.compile("\"(main:mechanic/wandering_trader/[a-z_]+)\"")
                .matcher(contract);
        while (ids.find()) {
            actualIds.add(ids.group(1));
        }
        assertEquals(EXPECTED_PATCHED_IDS, actualIds,
                "The interception contract must remain the exact seven Matcha lifecycle functions");

        Set<String> expectedConstants = Set.of(
                "PLACEMENT_FUNCTION", "TIMER_FUNCTION", "INITIALISE_FUNCTION",
                "SUMMON_FUNCTION_ID", "KILL_FUNCTION", "EARLY_KILL_FUNCTION",
                "KILL_BEACON_FUNCTION");
        String patchedSet = section(contract, "PATCHED_FUNCTIONS = Set.of(", ");");
        Set<String> actualConstants = new HashSet<>();
        Matcher constants = Pattern.compile("\\b[A-Z][A-Z0-9_]+\\b").matcher(patchedSet);
        while (constants.find()) {
            actualConstants.add(constants.group());
        }
        assertEquals(expectedConstants, actualConstants);

        assertTrue(mixin.contains("@Mixin(ServerFunctionManager.class)"));
        assertTrue(mixin.contains("@Inject(method = \"execute\", at = @At(\"HEAD\")"));
        assertFunctionInterception(mixin, "functionId", "source");

        assertTrue(nestedMixin.contains("@Mixin(FunctionCommand.class)"));
        assertTrue(nestedMixin.contains("method = \"instantiateAndQueueFunctions\""));
        assertFunctionInterception(nestedMixin, "id", "commandSource");
        assertTrue(guard.contains("ThreadLocal<UUID> PERMITTED_SUMMON_MARKER"));
        assertTrue(guard.contains("MatchaContract.SUMMON_FUNCTION_ID.equals(functionId)"));
        assertTrue(guard.contains("permitted.equals(sourceEntity.getUUID())"));
        assertTrue(coordinator.contains("functions.get(MatchaContract.SUMMON_FUNCTION)"));
        assertTrue(coordinator.contains("FunctionExecutionGuard.withPermittedSummon(marker.getUUID(),"));
        assertEquals(1, occurrences(coordinator, "functions.execute("),
                "The exact guarded Matcha summon must be the only direct legacy function execution");
    }

    @Test
    void playerJoinRecoversStaleMatchaTagAndTimerScore() throws IOException {
        String coordinator = normalized(source(COORDINATOR));
        String join = section(coordinator, "private void onJoin(", "private void handlePlacement(");
        String reset = section(
                coordinator, "private void resetTimerScore(", "private void revokePlacementAdvancement(");

        assertTrue(join.contains("data().get(player.getUUID())"));
        assertTrue(join.contains("StaleLockPolicy.shouldClearMatchaLock(hasMatchaLock, tracked)"));
        assertTrue(join.contains("player.removeTag(MatchaContract.PLAYER_LOCK_TAG)"));
        assertTrue(join.contains("resetTimerScore(player)"));
        assertTrue(reset.contains("getObjective(MatchaContract.TIMER_OBJECTIVE)"));
        assertTrue(reset.contains("resetSinglePlayerScore(holder, objective)"));
    }

    @Test
    void productionResourcesContainNoDatapackOverrides() throws IOException {
        Path resources = projectRoot().resolve("src/main/resources");
        List<String> forbidden = new ArrayList<>();
        try (Stream<Path> files = Files.walk(resources)) {
            files.filter(Files::isRegularFile)
                    .map(resources::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .filter(path -> path.toLowerCase(Locale.ROOT).startsWith("data/main/")
                            || path.toLowerCase(Locale.ROOT).endsWith(".mcfunction"))
                    .sorted()
                    .forEach(forbidden::add);
        }
        assertTrue(forbidden.isEmpty(),
                () -> "Compatibility must intercept runtime functions, not override datapack files: "
                        + forbidden);
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(
                projectRoot().resolve("src/main/java").resolve(relativePath), UTF_8);
    }

    private static String allProductionJava() throws IOException {
        Path sourceRoot = projectRoot().resolve("src/main/java");
        List<Path> files;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
        StringBuilder production = new StringBuilder();
        for (Path file : files) {
            production.append(Files.readString(file, UTF_8)).append('\n');
        }
        return production.toString();
    }

    private static String normalized(String source) {
        return source.replaceAll("\\s+", " ");
    }

    private static String section(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        assertTrue(start >= 0, () -> "Missing source token: " + startToken);
        int contentStart = start + startToken.length();
        int end = source.indexOf(endToken, contentStart);
        assertTrue(end >= 0, () -> "Missing source token after " + startToken + ": " + endToken);
        return source.substring(contentStart, end);
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        for (int index = source.indexOf(token); index >= 0; index = source.indexOf(token, index + token.length())) {
            count++;
        }
        return count;
    }

    private static void assertFunctionInterception(
            String mixinSource,
            String functionIdVariable,
            String commandSourceVariable
    ) {
        assertTrue(mixinSource.contains(
                "MatchaContract.isPatchedFunction(" + functionIdVariable + ")"));
        assertTrue(mixinSource.contains(
                "MatchaContract.PLACEMENT_FUNCTION.equals(" + functionIdVariable + ")"));
        assertTrue(mixinSource.contains(
                "BeaconKindlingCoordinator.handlePlacementFunction(" + commandSourceVariable + ")"));
        assertTrue(mixinSource.contains(
                        "if (!FunctionExecutionGuard.allows(" + functionIdVariable + ", "
                                + commandSourceVariable + ".getEntity())) { ci.cancel(); }"),
                "Every patched function except the replacement placement seam must be denied by default");
    }

    private static Path projectRoot() {
        String value = System.getProperty("projectRoot");
        assertNotNull(value, "Gradle must provide the projectRoot system property");
        return Path.of(value).toAbsolutePath().normalize();
    }
}

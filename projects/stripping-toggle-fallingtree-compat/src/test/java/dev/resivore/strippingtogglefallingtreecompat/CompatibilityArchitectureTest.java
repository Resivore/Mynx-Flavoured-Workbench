package dev.resivore.strippingtogglefallingtreecompat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityArchitectureTest {
    private static Path projectRoot() {
        return Path.of(System.getProperty("projectRoot"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(projectRoot().resolve(relative));
    }

    @Test
    void onlyTheColdFallingTreeCommandSeamIsMixedIn() throws Exception {
        String commandMixin = read("src/main/java/dev/resivore/strippingtogglefallingtreecompat/mixin/ToggleCommandMixin.java");
        String mixins = read("src/main/resources/strippingtoggle_fallingtree_compat.mixins.json");

        assertTrue(commandMixin.contains("apply(Lfr/rakambda/fallingtree/common/wrapper/IPlayer;)I"));
        assertTrue(commandMixin.contains("ToggleAuthority.isEnabled(player.getTags())"));
        assertTrue(commandMixin.contains("require = 1"));
        assertFalse(commandMixin.contains("player.addTag"));
        assertFalse(commandMixin.contains("player.removeTag"));
        assertTrue(commandMixin.contains("ToggleAuthority.statusTranslationKey(enabled)"));
        assertTrue(commandMixin.contains("callback.setReturnValue(1)"));

        assertTrue(mixins.contains("\"required\": true"));
        assertTrue(mixins.contains("\"defaultRequire\": 1"));
        assertTrue(mixins.contains("\"ToggleCommandMixin\""));
        assertFalse(mixins.contains("FallingTreeCommonMixin"));
        assertFalse(Files.exists(projectRoot().resolve(
                "src/main/java/dev/resivore/strippingtogglefallingtreecompat/mixin/FallingTreeCommonMixin.java")));
    }

    @Test
    void joinChangeReconnectRespawnAndCleanLeaveSynchronizationIsWired() throws Exception {
        String common = read("src/main/java/dev/resivore/strippingtogglefallingtreecompat/StrippingToggleFallingTreeCompat.java");
        String client = read("src/main/java/dev/resivore/strippingtogglefallingtreecompat/client/StrippingToggleFallingTreeCompatClient.java");
        String tracker = read("src/main/java/dev/resivore/strippingtogglefallingtreecompat/client/ToggleSyncTracker.java");

        assertTrue(common.contains("PayloadTypeRegistry.serverboundPlay().register"));
        assertTrue(common.contains("ServerPlayNetworking.registerGlobalReceiver"));
        assertTrue(common.contains("ServerPlayerEvents.JOIN"));
        assertTrue(common.contains("synchronizeNativeState(player, false)"));
        assertTrue(common.contains("synchronizeNativeState(context.player(), payload.enabled())"));
        assertTrue(common.contains("ServerPlayerEvents.LEAVE"));
        assertTrue(common.contains("ToggleAuthority::clearNativeState"));
        assertTrue(common.contains("ServerLifecycleEvents.SERVER_STOPPING"));
        assertFalse(common.contains("context.server().execute"));

        assertTrue(client.contains("StrippingToggle.strippingEnabled"));
        assertTrue(client.contains("ClientPlayConnectionEvents.JOIN"));
        assertTrue(client.contains("ClientPlayConnectionEvents.DISCONNECT"));
        assertTrue(client.contains("ClientTickEvents.START_CLIENT_TICK.register"));
        assertFalse(client.contains("ClientTickEvents.END_CLIENT_TICK.register"));
        assertTrue(client.contains("client.player != connectedPlayer"));
        assertTrue(client.contains("ToggleAuthority.synchronizeNativeState(client.player, current)"));
        assertTrue(client.contains("(decision & ToggleSyncTracker.SEND_PENDING) != 0"));
        assertTrue(client.contains("ClientPlayNetworking.canSend(ToggleStatePayload.TYPE)"));
        assertTrue(client.contains("syncTracker.evaluate("));
        assertTrue(client.contains("syncTracker.markSent(current)"));
        assertTrue(client.contains("getModContainer(\"strippingtoggle\")"));
        assertTrue(client.contains("REQUIRED_STRIPPING_TOGGLE_VERSION"));
        assertTrue(tracker.contains("public static final int APPLY_NATIVE_STATE"));
        assertTrue(tracker.contains("public static final int SEND_PENDING"));
        assertFalse(tracker.contains("record SyncDecision"));
    }

    @Test
    void noCompatibilityLookupRemainsInFallingTreeEligibilityOrTickPolling() throws Exception {
        Path javaRoot = projectRoot().resolve("src/main/java");
        StringBuilder allSource = new StringBuilder();
        try (Stream<Path> files = Files.walk(javaRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                allSource.append(Files.readString(file)).append('\n');
            }
        }
        String source = allSource.toString();
        assertFalse(source.contains("playerHasToggledOff"));
        assertFalse(source.contains("ToggleStateStore"));
        assertFalse(source.contains("ConcurrentHashMap"));
        assertFalse(source.contains("getLevel().isServer"));
        assertFalse(Files.exists(projectRoot().resolve(
                "src/main/java/dev/resivore/strippingtogglefallingtreecompat/ToggleStateStore.java")));
    }

    @Test
    void clientOnlyUpstreamLinkDoesNotLeakIntoCommonClasses() throws Exception {
        Path javaRoot = projectRoot().resolve("src/main/java");
        try (Stream<Path> files = Files.walk(javaRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            long upstreamImports = 0;
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                if (source.contains("yungando.strippingtoggle")) {
                    upstreamImports++;
                    assertTrue(file.toString().replace('\\', '/').contains("/client/"));
                }
            }
            assertEquals(1, upstreamImports);
        }
    }

    @Test
    void companionAddsNoSecondControlOrTreeBreakingImplementation() throws Exception {
        Path javaRoot = projectRoot().resolve("src/main/java");
        StringBuilder allSource = new StringBuilder();
        try (Stream<Path> files = Files.walk(javaRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                allSource.append(Files.readString(file)).append('\n');
            }
        }
        String source = allSource.toString();
        assertFalse(source.contains("KeyMappingHelper"));
        assertFalse(source.contains("HudElementRegistry"));
        assertFalse(source.contains("Commands.literal"));
        assertFalse(source.contains("breakTree("));
        assertFalse(source.contains("TreeBuilder"));
        assertFalse(source.contains("LeafBreakingHandler"));
    }

    @Test
    void metadataPinsTheAuditedCompatibilityBoundary() throws Exception {
        String metadata = read("src/main/resources/fabric.mod.json");
        assertTrue(metadata.contains("\"id\": \"strippingtoggle_fallingtree_compat\""));
        assertTrue(metadata.contains("\"environment\": \"*\""));
        assertTrue(metadata.contains("\"strippingtoggle\": \"=1.2.6+26.2\""));
        assertTrue(metadata.contains("\"fallingtree\": \"=25\""));
        assertTrue(metadata.contains("StrippingToggleFallingTreeCompatClient"));

        int dependsStart = metadata.indexOf("\"depends\"");
        int suggestsStart = metadata.indexOf("\"suggests\"");
        assertTrue(dependsStart >= 0 && suggestsStart > dependsStart);
        assertFalse(metadata.substring(dependsStart, suggestsStart).contains("\"strippingtoggle\""));
    }
}

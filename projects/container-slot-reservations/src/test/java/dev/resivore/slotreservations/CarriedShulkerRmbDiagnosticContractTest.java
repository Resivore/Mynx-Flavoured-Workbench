package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Canary 26 tests the always-on diagnostic contract rather than inferring runtime success. */
final class CarriedShulkerRmbDiagnosticContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void diagnosticIsAlwaysOnAndCoversTheFullExistingTransactionPath() throws IOException {
        String trace = source("CarriedShulkerRmbTrace.java");
        String collector = source("client/CarriedShulkerRmbCollector.java");
        String server = source("CarriedShulkerInventoryActions.java");
        String networking = source("ReservationNetworking.java");

        assertTrue(trace.contains("[CSR-C26-RMB]"));
        assertFalse(trace.contains("System.getProperty"));
        assertFalse(trace.contains("enabled"));
        for (String stage : List.of("PHYSICAL_RMB_PRESS_SEEN", "FABRIC_ALLOW_CLICK_ENTER",
                "VANILLA_SCREEN_CLICK_ENTER", "SCREEN_CONTEXT", "HOVERED_SLOT", "CREATIVE_WRAPPER",
                "ORDINARY_PLAYER_SLOT_CLASSIFICATION", "CARRIED_SHULKER_CLASSIFICATION", "ORIGIN_DECISION",
                "CAN_SEND", "GESTURE_BEGIN", "CLIENT_PLAN", "PAYLOAD_SEND", "PAYLOAD_SEND_RETURNED",
                "DRAG_SLOT_ENTER", "DRAG_EMPTY_NOOP", "RMB_RELEASE", "RMB_RESET")) {
            assertTrue(collector.contains(stage), "Missing client diagnostic stage " + stage);
        }
        for (String stage : List.of("SERVER_PACKET_RECEIVED", "SERVER_MENU_VALIDATE", "SERVER_SOURCE_RESOLVE",
                "SERVER_CURSOR_VALIDATE", "SERVER_SOURCE_VALIDATE", "SERVER_INSERT_PLAN", "SERVER_COMMIT_BEGIN",
                "SERVER_COMMIT_END", "SERVER_SYNC_SENT")) {
            assertTrue(server.contains(stage) || networking.contains(stage), "Missing server diagnostic stage " + stage);
        }
        assertTrue(collector.indexOf("PHYSICAL_RMB_PRESS_SEEN") < collector.indexOf("ORIGIN_DECISION"),
                "Raw press observation must precede classification rejection");
        assertTrue(server.contains("reason=MALFORMED_PAYLOAD"));
        assertTrue(server.contains("reason=NO_EXACT_LIVE_PLAYER_SLOT"));
    }

    @Test
    void traceKeepsInventoryExtendedCoordinatesAndDistinctDragVisitsVisible() throws IOException {
        String trace = source("CarriedShulkerRmbTrace.java");
        String collector = source("client/CarriedShulkerRmbCollector.java");
        assertTrue(trace.contains("listPosition="));
        assertTrue(trace.contains("slotIndex="));
        assertTrue(trace.contains("physicalSlot="));
        assertTrue(collector.contains("firstVisit="));
        assertTrue(collector.contains("alreadyVisited="));
        assertTrue(collector.contains("EMPTY_ORIGIN_PASSTHROUGH"));
        assertTrue(collector.contains("UNSUPPORTED_CURSOR"));
        assertTrue(collector.contains("NETWORK_UNAVAILABLE"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/dev/resivore/slotreservations").resolve(relative));
    }
}

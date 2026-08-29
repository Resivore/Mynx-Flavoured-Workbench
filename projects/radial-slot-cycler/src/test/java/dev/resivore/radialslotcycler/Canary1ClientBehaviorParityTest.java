package dev.resivore.radialslotcycler;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Canary1ClientBehaviorParityTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String EXPECTED_PROJECTION_SHA256 =
            "D55D47A50975E8CFA2D0481D5F09C84564376DA8DF1FBAEC34EC5526BC5DDEDC";

    @Test
    void clientInputSuppressionSelectionAndSendWiringRemainCanary1Exact() throws Exception {
        String source = Files.readString(ROOT.resolve(
                        "src/main/java/dev/resivore/radialslotcycler/client/RadialSlotCyclerClient.java"))
                .replace("\r\n", "\n")
                .replace("        sessionOpenedAtMillis = System.currentTimeMillis();\n", "")
                .replace("        sessionOpenedAtMillis = 0L;\n", "");

        StringBuilder projection = new StringBuilder(
                "private static final double DEAD_ZONE_RADIUS = 20.0D;");
        for (String signature : List.of(
                "public void onInitializeClient()",
                "private void tick(Minecraft client)",
                "private boolean canUse(Minecraft client)",
                "private boolean openSession(Minecraft client)",
                "private boolean sessionIsStillValid(Minecraft client)",
                "private int pointerSelection(Minecraft client)",
                "private void sendSelection()",
                "private void closeSession(Minecraft client)",
                "private static Identifier id(String path)",
                "private record RadialSession(")) {
            projection.append('\n').append(extractBlock(source, signature));
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String actual = HexFormat.of().withUpperCase().formatHex(
                digest.digest(projection.toString().getBytes(StandardCharsets.UTF_8)));
        assertEquals(EXPECTED_PROJECTION_SHA256, actual);
    }

    private static String extractBlock(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) {
            throw new AssertionError("Missing frozen client source block: " + signature);
        }
        int openingBrace = source.indexOf('{', start);
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("Unclosed frozen client source block: " + signature);
    }
}

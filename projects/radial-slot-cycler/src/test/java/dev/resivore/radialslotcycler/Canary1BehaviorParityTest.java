package dev.resivore.radialslotcycler;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Canary1BehaviorParityTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void commonNetworkingMappingSwapAndInteractionSourcesRemainCanary1Exact() throws Exception {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("src/main/java/dev/resivore/radialslotcycler/RadialSlotCycler.java",
                        "DF4178B555EFBC66B631DA43B4B1E76025AED21DFAC2714315C6CADC1381E67F"),
                Map.entry("src/main/java/dev/resivore/radialslotcycler/client/RadialClientConfig.java",
                        "FBD5409B310744DA2AA3B7F313628E8E84F70C25502F06BA1434DC40CA99B1DA"),
                Map.entry("src/main/java/dev/resivore/radialslotcycler/client/RadialInteractionController.java",
                        "66CB594F14C7785AB5BC9CE625E6C6FCBD28B36B78DC79EE9617B153FE9F39C3"),
                Map.entry("src/main/java/dev/resivore/radialslotcycler/client/RadialSelection.java",
                        "69FA83A207F111A56F5B2287D6C8FB0EB9655E2633A55ECE78BD10B8B1178786"),
                Map.entry("src/main/java/dev/resivore/radialslotcycler/core/ColumnLayout.java",
                        "DB87A8CE0A16A487E1152B77A52F0BB06CB30B587565C6E9DBD3813ED3C50F0A"),
                Map.entry("src/main/java/dev/resivore/radialslotcycler/core/ExactPairwiseSwap.java",
                        "0D22ACA76AF59A491F221AAB5D2D58F07F2799AD854EBF24059584C6DC49B5CE"),
                Map.entry("src/main/java/dev/resivore/radialslotcycler/core/OrdinaryInventorySnapshot.java",
                        "4A7E3E866F19EBCECC977DBDFAE0E4297574FC1125F7114594D3F2B198A9E405"),
                Map.entry("src/main/java/dev/resivore/radialslotcycler/core/SwapRequestValidator.java",
                        "58AD22474FB1ED324CCAE3BABD7222495FCB8B720B2AC88434B28B29A360D65C"),
                Map.entry("src/main/java/dev/resivore/radialslotcycler/network/SwapSlotPayload.java",
                        "5596DF7C345CF159F30F22944D3919C5B3883EA73254D3412777DA368E245AD3"));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String normalized = Files.readString(ROOT.resolve(entry.getKey()))
                    .replace("\r\n", "\n");
            String actual = HexFormat.of().withUpperCase().formatHex(
                    digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
            assertEquals(entry.getValue(), actual, entry.getKey());
        }
    }
}

package com.yungnickyoung.minecraft.ribbits.client.render;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the optional bridge confined to the authored rod geometry and its physical endpoint. */
class RibbitsFishermanRodBridgeContractTest {
    @Test
    void bridgeRendersOnlyTheAuthoredRodAndCapturesThatSameTransformForItsTip() throws IOException {
        Path root = Path.of(System.getProperty("projectRoot"));
        String source = Files.readString(root.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/render/RibbitsFishermanRodBridge.java"));

        assertTrue(source.contains("getBone(\"fishing_rod\")"));
        assertTrue(source.contains("bones.ifPresent(\"fishing_rod_2\", snapshot -> snapshot.skipRender(true))"));
        assertTrue(source.contains("bones.ifPresent(\"fishing_rod_3\", snapshot -> snapshot.skipRender(true))"));
        assertTrue(source.contains("captureOuterTip"));
        assertTrue(source.contains("applyGeometryRoot"),
                "the rendering root and captured endpoint must share one physical transform");
        assertTrue(source.contains("OUTER_TIP_Z_FROM_ROD_PIVOT"));
    }
}

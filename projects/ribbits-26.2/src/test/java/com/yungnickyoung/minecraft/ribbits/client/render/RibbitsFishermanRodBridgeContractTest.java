package com.yungnickyoung.minecraft.ribbits.client.render;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the optional bridge confined to the authored, grip-rebased rod geometry and its tip. */
class RibbitsFishermanRodBridgeContractTest {
    @Test
    void bridgeRendersOnlyTheAuthoredRodAndSharesOneStandaloneBasisWithItsTip() throws IOException {
        Path root = Path.of(System.getProperty("projectRoot"));
        String source = Files.readString(root.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/render/RibbitsFishermanRodBridge.java"));

        assertTrue(source.contains("getBone(\"fishing_rod\")"));
        assertTrue(source.contains("bones.ifPresent(\"fishing_rod_2\", snapshot -> {"));
        assertTrue(source.contains("bones.ifPresent(\"fishing_rod_3\", snapshot -> snapshot.skipRender(true))"));
        assertTrue(source.contains("snapshot.skipChildrenRender(true)"));
        assertTrue(source.contains("record ArmLocalRodTip"));
        assertTrue(source.contains("armLocalTip()"));
        assertTrue(source.contains("StandaloneRodBasis"));
        assertTrue(source.contains("attachGripAtOrigin"));
        assertTrue(source.contains("standaloneBasis"));
        assertTrue(source.contains("applyGeometryRoot"),
                "the rendering root and captured endpoint must share one physical transform");
        assertTrue(source.contains("GRIP_FROM_ROD_PIVOT"));
        assertTrue(source.contains("OUTER_TIP_FROM_ROD_PIVOT"));
        assertFalse(source.contains("ARM_LOCAL_VERTICAL_TRANSLATION"),
                "raw Gecko geometry must not inherit the old item-in-hand translation guesses");
        assertFalse(source.contains("ROTATE_X_DEGREES"),
                "raw Gecko geometry must not inherit the old item-in-hand display rotations");
    }
}

package com.yungnickyoung.minecraft.ribbits.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HugeToadstoolGrowthContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void configuredFeatureKeysPreserveTheIntentionalColorShapeCrossing() throws IOException {
        String keys = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/ConfiguredFeatureModule.java");
        String blocks = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/BlockModule.java");
        String toadstool = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/block/ToadstoolBlock.java");

        assertEquals(1, occurrences(keys, "RibbitsCommon.id(\"huge_red_toadstool\")"));
        assertEquals(1, occurrences(keys, "RibbitsCommon.id(\"huge_brown_toadstool\")"));
        assertTrue(keys.contains("ResourceKey<ConfiguredFeature<?, ?>> HUGE_RED_TOADSTOOL"));
        assertTrue(keys.contains("ResourceKey<ConfiguredFeature<?, ?>> HUGE_BROWN_TOADSTOOL"));

        assertTrue(toadstool.contains(
                "PlacedFeatureModule.TOADSTOOL_PATCH, ConfiguredFeatureModule.HUGE_RED_TOADSTOOL"));
        String brownRegistration = section(blocks,
                "@AutoRegister(\"small_brown_toadstool\")",
                "@AutoRegister(\"mossy_oak_planks\")");
        assertTrue(brownRegistration.contains("PlacedFeatureModule.SMALL_BROWN_TOADSTOOL_PATCH"));
        assertTrue(brownRegistration.contains("ConfiguredFeatureModule.HUGE_BROWN_TOADSTOOL"));
    }

    @Test
    void everyBonemealUseKeepsTheSpreadPathAndUsesVanillasExactHugeRoll() throws IOException {
        String toadstool = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/block/ToadstoolBlock.java");
        String swampPlant = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/block/SwampPlantBlock.java");
        String perform = section(toadstool,
                "public void performBonemeal(",
                "private boolean growHugeToadstool(");

        assertTrue(toadstool.contains("static final double HUGE_GROWTH_CHANCE = 0.4;"));
        assertTrue(perform.contains("random.nextFloat() < HUGE_GROWTH_CHANCE"));
        assertTrue(perform.contains("&& this.growHugeToadstool("));
        assertTrue(perform.contains("return;"), "successful huge growth must stop before spreading");
        assertTrue(perform.contains("super.performBonemeal(serverLevel, random, blockPos, blockState);"),
                "a missed roll or failed huge placement must use the existing same-color patch");
        assertTrue(perform.indexOf("return;") < perform.indexOf("super.performBonemeal("));
        assertFalse(toadstool.contains("isBonemealSuccess("),
                "ToadstoolBlock must retain SwampPlantBlock's always-successful spread entrypoint");
        assertTrue(swampPlant.contains("public boolean isBonemealSuccess("));
        assertTrue(section(swampPlant, "public boolean isBonemealSuccess(", "@Override\n    public void performBonemeal(")
                .contains("return true;"));
    }

    @Test
    void failedHugePlacementMirrorsVanillaRemovalAndRestorationBeforeFallback() throws IOException {
        String toadstool = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/block/ToadstoolBlock.java");
        String growth = toadstool.substring(toadstool.indexOf("private boolean growHugeToadstool("));

        int lookup = growth.indexOf(".lookupOrThrow(Registries.CONFIGURED_FEATURE)");
        int missing = growth.indexOf("if (configuredFeature.isEmpty())");
        int remove = growth.indexOf("serverLevel.removeBlock(blockPos, false);");
        int place = growth.indexOf("configuredFeature.get().place(");
        int success = growth.indexOf("return true;", place);
        int restore = growth.indexOf("serverLevel.setBlock(blockPos, blockState, Block.UPDATE_ALL);");
        int failure = growth.indexOf("return false;", restore);

        assertTrue(lookup >= 0);
        assertTrue(lookup < missing);
        assertTrue(missing < remove, "a missing configured feature must not remove the source plant");
        assertTrue(remove < place, "the source plant must be removed before vanilla feature validation");
        assertTrue(place < success, "successful placement must report success");
        assertTrue(success < restore, "restoration belongs only to the failed-placement path");
        assertTrue(restore < failure);
        assertEquals(1, occurrences(growth, "removeBlock(blockPos, false)"));
        assertEquals(1, occurrences(growth, "setBlock(blockPos, blockState, Block.UPDATE_ALL)"));
    }

    private static String section(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(startIndex >= 0, "missing section start: " + start);
        assertTrue(endIndex > startIndex, "missing section end: " + end);
        return source.substring(startIndex, endIndex);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String read(String relative) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relative));
    }
}

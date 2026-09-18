package com.yungnickyoung.minecraft.ribbits.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmallBrownToadstoolContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void smallBrownRegistrationKeepsExistingSmallAndHugeToadstoolIdentitiesIntact() throws IOException {
        String blocks = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/BlockModule.java");
        String registration = section(blocks,
                "@AutoRegister(\"small_brown_toadstool\")",
                "@AutoRegister(\"mossy_oak_planks\")");

        assertEquals(1, occurrences(blocks, "@AutoRegister(\"small_brown_toadstool\")"));
        assertTrue(registration.contains("public static final AutoRegisterBlock SMALL_BROWN_TOADSTOOL"));
        assertTrue(registration.contains("new ToadstoolBlock("));
        assertTrue(registration.contains(".mapColor(MapColor.PLANT)"));
        assertTrue(registration.contains(".instabreak()"));
        assertTrue(registration.contains(".noCollision()"));
        assertTrue(registration.contains(".sound(SoundType.SMALL_DRIPLEAF)"));
        assertTrue(registration.contains(".ignitedByLava()"));
        assertTrue(registration.contains(".setId(RegisterHelper.blockKey(\"small_brown_toadstool\"))"));
        assertTrue(registration.contains("PlacedFeatureModule.SMALL_BROWN_TOADSTOOL_PATCH"));
        assertTrue(registration.contains("ConfiguredFeatureModule.HUGE_BROWN_TOADSTOOL"));
        assertEquals(1, occurrences(registration, ".withItem(Item.Properties::new)"));
        assertFalse(registration.contains(".stacksTo("),
                "the small brown BlockItem must retain the default stack size");

        String hugeBrown = section(blocks,
                "@AutoRegister(\"brown_toadstool\")",
                "@AutoRegister(\"red_toadstool\")");
        assertTrue(hugeBrown.contains("public static final AutoRegisterBlock BROWN_TOADSTOOL"));
        assertTrue(hugeBrown.contains("new HugeMushroomBlock("));
        assertTrue(hugeBrown.contains("RegisterHelper.blockKey(\"brown_toadstool\")"));

        String hugeRed = section(blocks,
                "@AutoRegister(\"red_toadstool\")",
                "@AutoRegister(\"toadstool_stem\")");
        assertTrue(hugeRed.contains("public static final AutoRegisterBlock RED_TOADSTOOL"));
        assertTrue(hugeRed.contains("new HugeMushroomBlock("));
        assertTrue(hugeRed.contains("RegisterHelper.blockKey(\"red_toadstool\")"));

        String smallRed = section(blocks,
                "@AutoRegister(\"toadstool\")",
                "@AutoRegister(\"small_brown_toadstool\")");
        assertTrue(smallRed.contains("public static final AutoRegisterBlock TOADSTOOL"));
        assertTrue(smallRed.contains("new ToadstoolBlock("));
        assertTrue(smallRed.contains("RegisterHelper.blockKey(\"toadstool\")"));
        assertEquals(1, occurrences(smallRed, ".withItem(Item.Properties::new)"));
        assertFalse(smallRed.contains(".stacksTo("),
                "the existing small red BlockItem must retain the default stack size");
    }

    @Test
    void bothSmallToadstoolsShareShapeAndPlacementRulesButUseDifferentBonemealPatches() throws IOException {
        String toadstool = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/block/ToadstoolBlock.java");
        String swampPlant = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/block/SwampPlantBlock.java");
        String placedFeatures = read(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/PlacedFeatureModule.java");

        assertTrue(toadstool.contains("public class ToadstoolBlock extends SwampPlantBlock"));
        assertTrue(toadstool.contains(
                "Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0)"));
        assertTrue(toadstool.contains(
                "this(properties, PlacedFeatureModule.TOADSTOOL_PATCH, ConfiguredFeatureModule.HUGE_RED_TOADSTOOL);"));
        assertTrue(toadstool.contains(
                "public ToadstoolBlock(Properties properties, ResourceKey<PlacedFeature> bonemealPatch)"));
        assertTrue(toadstool.contains(
                "ResourceKey<ConfiguredFeature<?, ?>> hugeFeature)"));
        assertTrue(toadstool.contains("super(properties, bonemealPatch);"));
        assertTrue(toadstool.contains("this.hugeFeature = hugeFeature;"));
        assertTrue(toadstool.contains("return SHAPE.move(offset.x, offset.y, offset.z);"));
        assertFalse(toadstool.contains("getCloneItemStack("),
                "both registered variants must retain Block's ordinary pick-block result");
        assertFalse(swampPlant.contains("getCloneItemStack("),
                "the shared vegetation base must retain Block's ordinary pick-block result");

        assertTrue(swampPlant.contains("extends VegetationBlock implements BonemealableBlock"));
        assertTrue(swampPlant.contains("return true;"));
        assertTrue(swampPlant.contains(".getOptional(this.bonemealPatch)"));
        assertTrue(swampPlant.contains("feature.place(serverLevel"));

        assertEquals(1, occurrences(placedFeatures,
                "RibbitsCommon.id(\"small_brown_toadstool_patch\")"));
        assertEquals(1, occurrences(placedFeatures, "RibbitsCommon.id(\"toadstool_patch\")"));
    }

    @Test
    void creativeAndCompostingExposureAreSingleAndMatchTheSmallRedToadstool() throws IOException {
        String creative = read(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule.java");
        String items = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/ItemModule.java");

        assertEquals(1, occurrences(creative,
                "CreativeEntry.of(\"small_brown_toadstool\", BlockModule.SMALL_BROWN_TOADSTOOL::get)"));
        assertEquals(1, occurrences(items,
                "BlockModule.SMALL_BROWN_TOADSTOOL.get().asItem(), 0.65F"));
        assertEquals(1, occurrences(items, "BlockModule.TOADSTOOL.get().asItem(), 0.65F"));
        assertEquals(1, occurrences(items, "BlockModule.BROWN_TOADSTOOL.get().asItem(), 0.85F"));
        assertFalse(items.contains("BlockModule.SMALL_BROWN_TOADSTOOL.get().asItem(), 0.85F"));
    }

    @Test
    void newSmallBrownToadstoolDoesNotEnterExistingRecipesOrTrades() throws IOException {
        assertFalse(read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/recipe/ToadstoolHeartRecipe.java")
                .contains("SMALL_BROWN_TOADSTOOL"));
        assertFalse(read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/RibbitTradeModule.java")
                .contains("small_brown_toadstool"));
    }

    @Test
    void productionReferencesStayConfinedToRegistrationPresentationAndBonemeal() throws IOException {
        Set<String> actual = new TreeSet<>();
        for (String sourceRoot : Set.of("common/src/main/java", "fabric/src/main/java")) {
            Path root = PROJECT_ROOT.resolve(sourceRoot);
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    String source = Files.readString(path);
                    if (source.contains("small_brown_toadstool")
                            || source.contains("SMALL_BROWN_TOADSTOOL")) {
                        actual.add(PROJECT_ROOT.relativize(path).toString().replace('\\', '/'));
                    }
                }
            }
        }

        assertEquals(Set.of(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/BlockModule.java",
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule.java",
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/ItemModule.java",
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/PlacedFeatureModule.java"
        ), actual, "the new ID must not enter ordinary worldgen, trades, recipes, or unrelated systems");
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

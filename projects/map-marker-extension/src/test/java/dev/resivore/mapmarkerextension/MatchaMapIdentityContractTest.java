package dev.resivore.mapmarkerextension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.core.MapMarkerIdentity;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

final class MatchaMapIdentityContractTest {
    private static final Path PROJECT = Path.of(System.getProperty("projectRoot"));
    private static final Path ORIGINALS = Path.of(System.getProperty(
        "originalsRoot",
        PROJECT.resolve("../../originals").normalize().toString()
    ));
    private static final Path MATCHA = ORIGINALS.resolve(
        "datapacks/Matcha_Flavoured_1_12.zip"
    ).normalize();

    @Test
    void matchaTradeDefinitionsExposeEveryMappedTranslationKeyAndLegacyType()
        throws IOException {
        try (ZipFile zip = new ZipFile(MATCHA.toFile())) {
            for (TradeIdentity trade : trades()) {
                ZipEntry entry = zip.getEntry("data/minecraft/villager_trade/" + trade.path());
                assertNotNull(entry, trade.path());
                String json = entryText(zip, entry);
                assertTrue(json.contains(
                    "\"translate\": \"" + trade.identity().itemNameTranslationKey() + "\""
                ), trade.path());
                if (trade.identity() == MapMarkerIdentity.WOODLAND_MANSION) {
                    assertFalse(json.contains("\"decoration\""), trade.path());
                } else {
                    assertTrue(json.contains(
                        "\"decoration\": \"" + trade.identity().sourceDecorationTypeId() + "\""
                    ), trade.path());
                }
            }
        }
    }

    @Test
    void stableItemNameIsUniversalWhileOnlyAbbeyAndPapalOwnMatchaItemModels()
        throws IOException {
        Set<MapMarkerIdentity> identitiesWithItemModel = new LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(MATCHA.toFile())) {
            for (TradeIdentity trade : trades()) {
                ZipEntry entry = zip.getEntry("data/minecraft/villager_trade/" + trade.path());
                assertNotNull(entry, trade.path());
                if (entryText(zip, entry).contains("\"minecraft:item_model\"")) {
                    identitiesWithItemModel.add(trade.identity());
                }
            }
        }
        assertEquals(
            Set.of(MapMarkerIdentity.ABBEY, MapMarkerIdentity.PAPAL_OUTPOST),
            identitiesWithItemModel
        );
    }

    @Test
    void vanillaBuriedTreasureRetainsItsStableNameAndRedX() throws IOException {
        try (InputStream input = MatchaMapIdentityContractTest.class.getClassLoader()
            .getResourceAsStream("data/minecraft/loot_table/chests/shipwreck_map.json")) {
            assertNotNull(input);
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"translate\": \"filled_map.buried_treasure\""));
            assertTrue(json.contains("\"decoration\": \"minecraft:red_x\""));
        }
    }

    private static List<TradeIdentity> trades() {
        return List.of(
            trade("cartographer/1/mineshaft.json", MapMarkerIdentity.BURIED_MINESHAFT),
            trade("cartographer/1/papal_outpost.json", MapMarkerIdentity.PAPAL_OUTPOST),
            trade("cartographer/1/witch_hut.json", MapMarkerIdentity.WITCH_HUT),
            trade("cartographer/2/desert_temple.json", MapMarkerIdentity.DESERT_PYRAMID),
            trade("cartographer/2/jungle_ruin.json", MapMarkerIdentity.JUNGLE_PYRAMID),
            trade("cartographer/2/trail_ruin.json", MapMarkerIdentity.TRAIL_RUINS),
            trade("cartographer/3/abbey.json", MapMarkerIdentity.ABBEY),
            trade("cartographer/3/ocean_monument.json", MapMarkerIdentity.OCEAN_MONUMENT),
            trade("cartographer/3/trial_chamber.json", MapMarkerIdentity.TRIAL_CHAMBER),
            trade("cartographer/3/warm_ocean_ruin.json", MapMarkerIdentity.WARM_OCEAN_RUINS),
            trade("cartographer/4/ancient_city.json", MapMarkerIdentity.ANCIENT_CITY),
            trade("cartographer/5/woodland_mansion.json", MapMarkerIdentity.WOODLAND_MANSION),
            trade("wandering_trader/village_desert.json", MapMarkerIdentity.DESERT_VILLAGE),
            trade("wandering_trader/village_plains.json", MapMarkerIdentity.PLAINS_VILLAGE),
            trade("wandering_trader/village_savanna.json", MapMarkerIdentity.SAVANNAH_VILLAGE),
            trade("wandering_trader/village_snowy.json", MapMarkerIdentity.SNOWY_VILLAGE),
            trade("wandering_trader/village_taiga.json", MapMarkerIdentity.TAIGA_VILLAGE)
        );
    }

    private static TradeIdentity trade(String path, MapMarkerIdentity identity) {
        return new TradeIdentity(path, identity);
    }

    private static String entryText(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream input = zip.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record TradeIdentity(String path, MapMarkerIdentity identity) {
    }
}

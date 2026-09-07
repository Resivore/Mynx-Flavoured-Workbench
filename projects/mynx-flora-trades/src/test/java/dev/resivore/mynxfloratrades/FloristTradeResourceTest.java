package dev.resivore.mynxfloratrades;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloristTradeResourceTest {
    @Test
    void floristLevelOneOrderPrependsGlowcapPotSalesWithoutChangingFlowers() throws Exception {
        String orderedTrades = Files.readString(Path.of(
                "src/main/resources/data/mynx_flora_trades/tags/villager_trade/florist/level_1.json"));
        List<String> expected = List.of("glowcap_flower_pots", "glowcap_decorated_pot", "hyssop", "blue_lupine",
                "pink_lupine", "purple_lupine", "red_lupine", "yellow_lupine", "tassel", "meadow_sage");
        List<Integer> positions = new ArrayList<>();
        for (String trade : expected) positions.add(orderedTrades.indexOf("florist/" + trade));
        assertTrue(positions.stream().allMatch(position -> position >= 0), "a required Florist trade is absent");
        for (int index = 1; index < positions.size(); index++) {
            assertTrue(positions.get(index - 1) < positions.get(index), "Florist trade order changed: " + orderedTrades);
        }
    }

    @Test
    void glowcapPotSalesHaveExactDirectionAndEconomy() throws Exception {
        assertTrade("glowcap_flower_pots", "minecraft:flower_pot", 4);
        assertTrade("glowcap_decorated_pot", "minecraft:decorated_pot", 1);
    }

    @Test
    void everyExistingFlowerTradeKeepsOneEmeraldForEightFloraWithNoXp() throws Exception {
        Path directory = Path.of("src/main/resources/data/mynx_flora_trades/villager_trade/florist");
        List<Path> trades;
        try (var paths = Files.list(directory)) {
            trades = paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
        assertEquals(10, trades.size());
        for (Path trade : trades) {
            if (trade.getFileName().toString().startsWith("glowcap_")) continue;
            String json = Files.readString(trade);
            assertTrue(json.contains("\"wants\":{\"id\":\"minecraft:emerald\"}"), trade.toString());
            assertTrue(json.contains("\"gives\":{\"id\":\"mynx_regions_unexplored:"), trade.toString());
            assertTrue(json.contains("\"count\":8}"), trade.toString());
            assertTrue(json.contains("\"xp\":0"), trade.toString());
        }
    }

    private static void assertTrade(String name, String output, int count) throws Exception {
        String json = Files.readString(Path.of("src/main/resources/data/mynx_flora_trades/villager_trade/florist/" + name + ".json"));
        assertTrue(json.contains("\"wants\":{\"id\":\"ribbits:glowcap\"}"), json);
        assertTrue(json.contains("\"gives\":{\"id\":\"" + output + "\",\"count\":" + count + "}"), json);
        assertTrue(json.contains("\"max_uses\":16.0"), json);
        assertTrue(json.contains("\"price_multiplier\":0.0"), json);
        assertTrue(json.contains("\"xp\":0"), json);
    }
}

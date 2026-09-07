package dev.resivore.mynxfloratrades;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloristTradeResourceTest {
    @Test
    void everyFloristTradeKeepsOneEmeraldForEightFloraWithNoXp() throws Exception {
        Path directory = Path.of("src/main/resources/data/mynx_flora_trades/villager_trade/florist");
        List<Path> trades;
        try (var paths = Files.list(directory)) {
            trades = paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
        assertEquals(8, trades.size());
        for (Path trade : trades) {
            String json = Files.readString(trade);
            assertTrue(json.contains("\"wants\":{\"id\":\"minecraft:emerald\"}"), trade.toString());
            assertTrue(json.contains("\"gives\":{\"id\":\"mynx_regions_unexplored:"), trade.toString());
            assertTrue(json.contains("\"count\":8}"), trade.toString());
            assertTrue(json.contains("\"xp\":0"), trade.toString());
        }
    }
}

package dev.resivore.mynxfloratrades;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionsUnexploredPotContractTest {
    @Test
    void representativeMruPottedPlantUsesTheFlowerPotBlockTypeCoveredByTheProductionPoiTest() throws Exception {
        String source = Files.readString(Path.of("../mynx-regions-unexplored/src/main/java/dev/resivore/mynxregions/MynxRegionsUnexplored.java"));
        assertTrue(source.contains("POTTED_HYSSOP = pot(\"potted_hyssop\", HYSSOP, 0)"));
        assertTrue(source.contains("new FlowerPotBlock(content, p)"));
    }
}

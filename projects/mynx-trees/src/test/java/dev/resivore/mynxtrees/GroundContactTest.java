package dev.resivore.mynxtrees;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroundContactTest {
    private static final String LOG = "mynx_trees:silver_birch_log[axis=y]";
    private static final BlockPos POS = new BlockPos(15, 16, -16);

    private Set<String> soils() throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of(System.getProperty("projectRoot"),
                "build/generated/resources/data/mynx_trees/tags/block/silver_birch_base_soils.json"))).getAsJsonObject();
        Set<String> soils = new HashSet<>();
        json.getAsJsonArray("values").forEach(value -> soils.add(value.getAsString()));
        return soils;
    }

    private boolean matches(String state, Map<BlockPos, String> world) throws Exception {
        var soils = soils();
        return GroundContact.matches(state, POS, p -> world.getOrDefault(p, "minecraft:air"), LOG::equals, soils::contains);
    }

    @Test void everyDefaultSoilSupportsStandaloneStumpAndPillar() throws Exception {
        for (String soil : soils()) {
            var world = new HashMap<>(Map.of(POS, LOG, POS.below(), soil));
            assertTrue(matches(LOG, world), soil);
            world.put(POS.above(), LOG);
            assertTrue(matches(LOG, world), soil);
            assertFalse(GroundContact.matches(LOG, POS.above(), p -> world.getOrDefault(p, "minecraft:air"), LOG::equals, soils()::contains));
        }
    }

    @Test void nonSoilsAndFloatingColumnsKeepOrdinaryBark() throws Exception {
        for (String support : List.of("minecraft:stone", "minecraft:gravel", "minecraft:sand", "minecraft:air",
                "minecraft:oak_log", "minecraft:farmland", "minecraft:moss_block", LOG)) {
            assertFalse(matches(LOG, Map.of(POS, LOG, POS.below(), support, POS.above(), LOG)), support);
        }
    }

    @Test void adjacentOrSeparatedSoilCannotQualify() throws Exception {
        assertFalse(matches(LOG, Map.of(POS, LOG, POS.east(), "minecraft:dirt", POS.below(2), "minecraft:dirt")));
    }

    @Test void exactIdentityAndUprightAxisAreRequired() throws Exception {
        for (String state : List.of("mynx_trees:silver_birch_log[axis=x]", "mynx_trees:silver_birch_log[axis=z]",
                "mynx_trees:silver_birch_wood[axis=y]", "minecraft:stripped_birch_log[axis=y]",
                "minecraft:stripped_birch_wood[axis=y]", "mynx_trees:wisteria_log[axis=y]",
                "mynx_trees:wisteria_wood[axis=y]", "minecraft:birch_log[axis=y]")) {
            assertFalse(matches(state, Map.of(POS, state, POS.below(), "minecraft:dirt")), state);
        }
    }

    @Test void supportChangesAndTagReloadUseCurrentContextAcrossSectionBoundary() throws Exception {
        var world = new HashMap<>(Map.of(POS, LOG));
        for (String soil : List.of("minecraft:dirt", "minecraft:stone", "minecraft:air", "minecraft:dirt")) {
            world.put(POS.below(), soil);
            assertEquals(soil.equals("minecraft:dirt"), matches(LOG, world));
        }
        assertEquals(1, POS.getY() >> 4);
        assertEquals(0, POS.below().getY() >> 4);
        var liveTag = soils();
        assertTrue(GroundContact.matches(LOG, POS, world::get, LOG::equals, liveTag::contains));
        liveTag.remove("minecraft:dirt");
        assertFalse(GroundContact.matches(LOG, POS, world::get, LOG::equals, liveTag::contains));
        liveTag.add("minecraft:stone"); world.put(POS.below(), "minecraft:stone");
        assertTrue(GroundContact.matches(LOG, POS, world::get, LOG::equals, liveTag::contains));
    }

    @Test void absentOrUnrelatedPositionFallsBackWithoutNeighborQueries() {
        assertFalse(GroundContact.matches(LOG, null, p -> { throw new AssertionError(); }, LOG::equals, x -> true));
        assertFalse(GroundContact.matches(LOG, POS, null, LOG::equals, x -> true));
        assertFalse(GroundContact.matches(LOG, POS, p -> "minecraft:air", LOG::equals, x -> true));
    }

    @Test void boundedSnapshotQueriesOnlyActualLogAndDirectlyBelow() throws Exception {
        List<BlockPos> queries = new ArrayList<>();
        assertTrue(GroundContact.matches(LOG, POS, p -> {
            queries.add(p); return p.equals(POS) ? LOG : "minecraft:dirt";
        }, LOG::equals, soils()::contains));
        assertEquals(List.of(POS, POS.below()), queries);
    }

    @Test void geometryKeysSeparateBranchesModelsAndReloadsAndPreserveNull() {
        Object model = new Object(), child = new Object();
        Object plain = GroundContact.geometryKey(model, false, child);
        assertEquals(plain, GroundContact.geometryKey(model, false, child));
        assertNotEquals(plain, GroundContact.geometryKey(model, true, child));
        assertNotEquals(plain, GroundContact.geometryKey(new Object(), false, child));
        assertNotEquals(plain, GroundContact.geometryKey(model, false, new Object()));
        assertNull(GroundContact.geometryKey(model, true, null));
    }
}

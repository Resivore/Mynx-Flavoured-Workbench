package dev.resivore.blockfamilies.cnm.catalog;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BAR_CHAIN;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BBB_DETAIL;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.FENCE_GATE;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.MASONRY_DETAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Literal C9 contract for the BBB families registered by current BBB main. */
final class BuildingButBetterFamiliesTest {
    private static final List<String> WOODS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak");
    private static final List<String> STONES = List.of(
            "stone", "blackstone", "deepslate", "nether_brick", "sandstone", "red_sandstone", "quartz");
    private static final List<String> ENDERSCAPE_WOODS = List.of("veiled", "celestial", "murublight");

    @Test
    void everyRegisteredWoodHasOneExactTrimDetailFamilyIncludingPaleOak() {
        Map<String, AuditedShapeFamily> details = detailsByPath();
        assertEquals(15, details.size());
        for (String wood : WOODS) {
            assertEquals(List.of(bbb(wood + "_trim"), bbb(wood + "_balustrade"),
                    bbb(wood + "_support"), bbb(wood + "_pallet")),
                    details.get("cnm/bbb_detail/wood/" + wood).members(), wood);
        }
        assertTrue(details.get("cnm/bbb_detail/wood/pale_oak").members().contains(bbb("pale_oak_pallet")));
    }

    @Test
    void enderscapeBbbDetailsUseTheExistingLiteralWoodDetailArchitecture() {
        Map<String, AuditedShapeFamily> details = detailsByPath();
        for (String wood : ENDERSCAPE_WOODS) {
            assertEquals(List.of(bbb(wood + "_trim"), bbb(wood + "_balustrade"),
                    bbb(wood + "_support"), bbb(wood + "_pallet")),
                    details.get("cnm/bbb_detail/wood/enderscape_" + wood).members(), wood);
            assertEquals(bbb(wood + "_trim"),
                    details.get("cnm/bbb_detail/wood/enderscape_" + wood).canonicalParent(), wood);
        }
    }

    @Test
    void existingFenceGateFamiliesAreExtendedInPlaceWithoutFabricatingMossyOak() {
        Map<String, AuditedShapeFamily> fences = AuditedShapeFamilies.families(FENCE_GATE).stream()
                .collect(Collectors.toMap(family -> family.key().getPath(), family -> family));
        assertEquals(16, fences.size());
        for (String wood : WOODS) {
            assertEquals(List.of(minecraft(wood + "_fence"), minecraft(wood + "_fence_gate"),
                    bbb(wood + "_frame"), bbb(wood + "_lattice")),
                    fences.get("cnm/fence_gate/" + wood).members(), wood);
        }
        assertEquals(List.of(id("ribbits:mossy_oak_planks_fence"), id("ribbits:mossy_oak_planks_fence_gate")),
                fences.get("cnm/fence_gate/ribbits_mossy_oak_planks").members());
        Set<Identifier> all = AuditedShapeFamilies.families().stream()
                .flatMap(family -> family.members().stream()).collect(Collectors.toSet());
        assertFalse(all.contains(bbb("mossy_oak_frame")));
        assertFalse(all.contains(bbb("mossy_oak_lattice")));
    }

    @Test
    void enderscapeFenceGateFamiliesAreExtendedInPlaceWithOnlyFrameAndLattice() {
        Map<String, AuditedShapeFamily> fences = AuditedShapeFamilies.families(FENCE_GATE).stream()
                .collect(Collectors.toMap(family -> family.key().getPath(), family -> family));
        assertEquals(16, fences.size());
        for (String wood : ENDERSCAPE_WOODS) {
            AuditedShapeFamily family = fences.get("cnm/fence_gate/enderscape_" + wood);
            assertEquals(List.of(id("enderscape:" + wood + "_fence"),
                    id("enderscape:" + wood + "_fence_gate"),
                    bbb(wood + "_frame"), bbb(wood + "_lattice")), family.members(), wood);
            assertEquals(id("enderscape:" + wood + "_fence"), family.canonicalParent(), wood);
        }
    }

    @Test
    void enderscapeBbbWallsBeamsAndLanternsRemainOutsideIbfFamilies() {
        Set<Identifier> all = AuditedShapeFamilies.families().stream()
                .flatMap(family -> family.members().stream()).collect(Collectors.toSet());
        for (String wood : ENDERSCAPE_WOODS) {
            for (String excluded : List.of("wall", "beam", "beam_stairs", "beam_slab", "lantern")) {
                assertFalse(all.contains(bbb(wood + "_" + excluded)), wood + "_" + excluded);
            }
        }
    }

    @Test
    void sevenStoneDetailFamiliesContainOnlyTheFiveRequestedBBBForms() {
        for (String stone : STONES) {
            AuditedShapeFamily masonry = AuditedShapeFamilies.families(MASONRY_DETAIL).stream()
                    .filter(family -> family.key().getPath().equals("cnm/masonry_detail/" + stone))
                    .findFirst().orElseThrow();
            assertTrue(masonry.members().containsAll(List.of(bbb(stone + "_column"), bbb(stone + "_urn"),
                    bbb(stone + "_moulding"), bbb(stone + "_fence"), bbb(stone + "_frame"))), stone);
        }
        assertEquals(0, detailsByPath().keySet().stream().filter(path -> path.startsWith("cnm/bbb_detail/stone/")).count());
    }

    @Test
    void ironBarsFamilyIsExtendedRatherThanDuplicated() {
        List<AuditedShapeFamily> iron = AuditedShapeFamilies.families(BAR_CHAIN).stream()
                .filter(family -> family.key().getPath().equals("cnm/bar_chain/iron")).toList();
        assertEquals(1, iron.size());
        assertEquals(List.of(minecraft("iron_bars"), bbb("iron_fence"), minecraft("iron_chain"),
                id("auroraslanterns:chandelier/iron")), iron.getFirst().members());
    }

    private static Map<String, AuditedShapeFamily> detailsByPath() {
        return AuditedShapeFamilies.families(BBB_DETAIL).stream()
                .collect(Collectors.toMap(family -> family.key().getPath(), family -> family));
    }

    private static Identifier bbb(String path) {
        return Identifier.fromNamespaceAndPath("bbb", path);
    }

    private static Identifier minecraft(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private static Identifier id(String value) {
        return Identifier.parse(value);
    }
}

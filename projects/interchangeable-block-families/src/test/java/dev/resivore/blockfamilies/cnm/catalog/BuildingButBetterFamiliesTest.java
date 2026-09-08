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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Literal C6 contract for the BBB families registered by current BBB main. */
final class BuildingButBetterFamiliesTest {
    private static final List<String> WOODS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak");
    private static final List<String> STONES = List.of(
            "stone", "blackstone", "deepslate", "nether_brick", "sandstone", "red_sandstone", "quartz");

    @Test
    void everyRegisteredWoodHasOneExactTrimDetailFamilyIncludingPaleOak() {
        Map<String, AuditedShapeFamily> details = detailsByPath();
        assertEquals(19, details.size());
        for (String wood : WOODS) {
            assertEquals(List.of(bbb(wood + "_trim"), bbb(wood + "_balustrade"),
                    bbb(wood + "_support"), bbb(wood + "_pallet")),
                    details.get("cnm/bbb_detail/wood/" + wood).members(), wood);
        }
        assertTrue(details.get("cnm/bbb_detail/wood/pale_oak").members().contains(bbb("pale_oak_pallet")));
    }

    @Test
    void existingFenceGateFamiliesAreExtendedInPlaceWithoutFabricatingMossyOak() {
        Map<String, AuditedShapeFamily> fences = AuditedShapeFamilies.families(FENCE_GATE).stream()
                .collect(Collectors.toMap(family -> family.key().getPath(), family -> family));
        assertEquals(13, fences.size());
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
    void sevenStoneDetailFamiliesContainOnlyTheFiveRequestedBBBForms() {
        Map<String, AuditedShapeFamily> details = detailsByPath();
        for (String stone : STONES) {
            assertEquals(List.of(bbb(stone + "_column"), bbb(stone + "_urn"),
                    bbb(stone + "_moulding"), bbb(stone + "_fence"), bbb(stone + "_frame")),
                    details.get("cnm/bbb_detail/stone/" + stone).members(), stone);
        }
        assertEquals(STONES.size(), details.keySet().stream().filter(path -> path.startsWith("cnm/bbb_detail/stone/")).count());
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

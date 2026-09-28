package dev.resivore.blockfamilies.cnm.catalog;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BAR_CHAIN;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BBB_DETAIL;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BUILDING_ACCESSORY;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.DISPLAY_FIXTURE;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.FENCE_GATE;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.THREE_HIGH_DOOR;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.TRAPDOOR;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.TWO_HIGH_DOOR;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.WINDOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditedShapeFamiliesTest {
    @Test
    void exposesExactAuditedTotals() {
        assertEquals(181, AuditedShapeFamilies.families().size());
        assertEquals(1_396, AuditedShapeFamilies.uniqueMemberCount());
        assertEquals(22, AuditedShapeFamilies.largestFamilySize());
        assertEquals(AuditedShapeFamilies.EXPECTED_FAMILY_COUNT, AuditedShapeFamilies.families().size());
        assertEquals(AuditedShapeFamilies.EXPECTED_UNIQUE_MEMBER_COUNT,
                AuditedShapeFamilies.uniqueMemberCount());
        assertEquals(AuditedShapeFamilies.EXPECTED_LARGEST_FAMILY_SIZE,
                AuditedShapeFamilies.largestFamilySize());
    }

    @Test
    void categoriesHaveExactCountsAndMembershipTotals() {
        Map<AuditedShapeFamily.Category, Integer> expectedFamilies = Map.of(
                TWO_HIGH_DOOR, 13,
                THREE_HIGH_DOOR, 12,
                TRAPDOOR, 13,
                WINDOW, 46,
                DISPLAY_FIXTURE, 15,
                FENCE_GATE, 16,
                BAR_CHAIN, 10,
                BBB_DETAIL, 22,
                BUILDING_ACCESSORY, 34);
        Map<AuditedShapeFamily.Category, Integer> expectedMembers = Map.of(
                TWO_HIGH_DOOR, 273,
                THREE_HIGH_DOOR, 229,
                TRAPDOOR, 208,
                WINDOW, 188,
                DISPLAY_FIXTURE, 45,
                FENCE_GATE, 62,
                BAR_CHAIN, 30,
                BBB_DETAIL, 96,
                BUILDING_ACCESSORY, 265);

        for (AuditedShapeFamily.Category category : AuditedShapeFamily.Category.values()) {
            List<AuditedShapeFamily> families = AuditedShapeFamilies.families(category);
            assertEquals(expectedFamilies.get(category), families.size(), category.name());
            assertEquals(expectedMembers.get(category),
                    families.stream().mapToInt(family -> family.members().size()).sum(),
                    category.name());
        }
    }

    @Test
    void everyKeyAndMemberIsUniqueAndEveryParentIsFirst() {
        Set<Identifier> keys = new HashSet<>();
        Set<Identifier> members = new HashSet<>();

        for (AuditedShapeFamily family : AuditedShapeFamilies.families()) {
            assertTrue(keys.add(family.key()), family.key().toString());
            assertEquals(family.canonicalParent(), family.members().getFirst(), family.key().toString());
            assertTrue(family.key().getPath().startsWith("cnm/" + family.category().keySegment() + "/"));
            for (Identifier member : family.members()) {
                assertTrue(members.add(member), member.toString());
            }
        }

        assertEquals(181, keys.size());
        assertEquals(1_396, members.size());
    }

    @Test
    void familyAndMemberOrderIsStableAndRepresentative() {
        List<AuditedShapeFamily> families = AuditedShapeFamilies.families();
        assertEquals(id("interchangeable_block_families", "cnm/two_high_door/oak"),
                families.getFirst().key());
        assertEquals(id("interchangeable_block_families", "cnm/window/dark_oak_log"),
                families.get(48).key());
        assertEquals(id("interchangeable_block_families", "cnm/fence_gate/warped"),
                families.get(110).key());
        assertEquals(id("interchangeable_block_families", "cnm/fence_gate/ribbits_mossy_oak_planks"),
                families.get(111).key());
        assertEquals(id("interchangeable_block_families", "cnm/bar_chain/iron"),
                families.get(115).key());
        assertEquals(id("interchangeable_block_families", "cnm/bbb_detail/wood/oak"),
                families.get(125).key());
        assertEquals(id("interchangeable_block_families",
                        "cnm/building_accessory/enderscape_polished_kurodite"),
                families.getLast().key());

        AuditedShapeFamily oakDoors = family("cnm/two_high_door/oak");
        assertEquals(id("minecraft", "oak_door"), oakDoors.members().getFirst());
        assertEquals(id("mcwdoors", "oak_modern_door"), oakDoors.members().get(11));
        assertEquals(id("mcwdoors", "oak_whispering_door"), oakDoors.members().getLast());

        AuditedShapeFamily darkPrismarine = family("cnm/window/dark_prismarine");
        assertEquals(List.of(
                        id("mcwwindows", "dark_prismarine_window"),
                        id("mcwwindows", "dark_prismarine_window2"),
                        id("mcwwindows", "dark_prismarine_four_window"),
                        id("mcwwindows", "dark_prismarine_pane_window"),
                        id("mcwwindows", "dark_prismarine_brick_gothic")),
                darkPrismarine.members());
    }

    @Test
    void exactFamilySizeDistributionsAreStable() {
        assertEquals(Map.of(9, 1L, 22, 12L), sizeDistribution(TWO_HIGH_DOOR));
        assertEquals(Map.of(9, 1L, 20, 11L), sizeDistribution(THREE_HIGH_DOOR));
        assertEquals(Map.of(4, 1L, 17, 12L), sizeDistribution(TRAPDOOR));
        assertEquals(Map.of(4, 42L, 5, 4L), sizeDistribution(WINDOW));
        assertEquals(Map.of(3, 15L), sizeDistribution(DISPLAY_FIXTURE));
        assertEquals(Map.of(2, 1L, 4, 15L), sizeDistribution(FENCE_GATE));
        assertEquals(Map.of(2, 1L, 3, 8L, 4, 1L), sizeDistribution(BAR_CHAIN));
        assertEquals(Map.of(4, 15L, 5, 6L, 6, 1L), sizeDistribution(BBB_DETAIL));
        assertEquals(Map.of(2, 8L, 3, 1L, 4, 1L, 9, 11L, 11, 13L),
                sizeDistribution(BUILDING_ACCESSORY));
    }

    @Test
    void auditedExclusionsNeverEnterTheCatalog() {
        Set<Identifier> members = AuditedShapeFamilies.families().stream()
                .flatMap(family -> family.members().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        List<Identifier> exclusions = List.of(
                id("mcwdoors", "cherry_waffle_door"),
                id("mcwdoors", "garage_white_door"),
                id("mcwdoors", "garage_silver_door"),
                id("mcwdoors", "garage_gray_door"),
                id("mcwdoors", "garage_black_door"),
                id("mcwdoors", "iron_portcullis"),
                id("mcwdoors", "wooden_portcullis"),
                id("minecraft", "copper_door"),
                id("minecraft", "waxed_oxidized_copper_door"),
                id("minecraft", "copper_trapdoor"),
                id("minecraft", "waxed_oxidized_copper_trapdoor"),
                id("dramaticdoors", "tall_copper_door"),
                id("dramaticdoors", "tall_waxed_oxidized_copper_door"),
                id("dramaticdoors", "tall_pale_oak_door"),
                id("dramaticdoors", "short_oak_door"),
                id("dramaticdoors", "tall_macaw_oak_classic_door"),
                id("dramaticdoors", "tall_macaw_spruce_cottage_door"),
                id("dramaticdoors", "tall_macaw_birch_paper_door"),
                id("dramaticdoors", "tall_macaw_jungle_beach_door"),
                id("dramaticdoors", "tall_macaw_acacia_tropical_door"),
                id("dramaticdoors", "tall_macaw_dark_oak_four_panel_door"),
                id("dramaticdoors", "tall_macaw_mangrove_swamp_door"),
                id("dramaticdoors", "tall_macaw_cherry_waffle_door"),
                id("dramaticdoors", "tall_macaw_crimson_nether_door"),
                id("dramaticdoors", "tall_macaw_warped_mystic_door"),
                id("mcwwindows", "andesite_louvered_shutter"),
                id("mcwwindows", "diorite_louvered_shutter"),
                id("mcwwindows", "granite_louvered_shutter"),
                id("mcwwindows", "end_brick_gothic"),
                id("mcwwindows", "nether_brick_gothic"),
                id("mcwwindows", "mud_brick_gothic"),
                id("mcwwindows", "stone_brick_arrow_slit"),
                id("mcwwindows", "cobblestone_arrow_slit"),
                id("mcwwindows", "ender_brick_arrow_slit"),
                id("mcwwindows", "ender_brick_arrow_slit"),
                id("mcwwindows", "mud_brick_arrow_slit"),
                id("mcwwindows", "blackstone_brick_arrow_slit"),
                id("mcwwindows", "prismarine_brick_arrow_slit"),
                id("mcwwindows", "dark_prismarine_brick_arrow_slit"),
                id("mcwwindows", "crimson_window"),
                id("mcwwindows", "stripped_warped_log_window"),
                id("minecraft", "nether_brick_fence"),
                id("enderscape", "veiled_door"),
                id("enderscape", "veiled_trapdoor"),
                id("enderscape", "celestial_door"),
                id("enderscape", "celestial_trapdoor"),
                id("enderscape", "murublight_door"),
                id("enderscape", "murublight_trapdoor"));

        for (Identifier exclusion : exclusions) {
            assertFalse(members.contains(exclusion), exclusion.toString());
        }
    }

    @Test
    void publicListsAndRecordMembersAreImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> AuditedShapeFamilies.families().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> AuditedShapeFamilies.families(TWO_HIGH_DOOR).clear());
        assertThrows(UnsupportedOperationException.class,
                () -> AuditedShapeFamilies.families().getFirst().members().clear());
    }

    @Test
    void recordDefensivelyCopiesMembersAndRejectsInvalidOrder() {
        Identifier parent = id("test", "parent");
        Identifier alternative = id("test", "alternative");
        List<Identifier> mutableMembers = new ArrayList<>(List.of(parent, alternative));
        AuditedShapeFamily family = new AuditedShapeFamily(
                id("test", "family"), TWO_HIGH_DOOR, parent, mutableMembers);

        mutableMembers.clear();
        assertEquals(List.of(parent, alternative), family.members());
        assertThrows(IllegalArgumentException.class, () -> new AuditedShapeFamily(
                id("test", "wrong_order"), TWO_HIGH_DOOR, parent, List.of(alternative, parent)));
        assertThrows(IllegalArgumentException.class, () -> new AuditedShapeFamily(
                id("test", "duplicate"), TWO_HIGH_DOOR, parent, List.of(parent, parent)));
    }

    @Test
    void canonicalExpandedCatalogHasStableDigest() throws NoSuchAlgorithmException {
        assertEquals("4B753028B9820913D068901D8B3BA93F13BAD6DAB6C95AA312BA70F2B1C4B8B5",
                digest(AuditedShapeFamilies.families()));
    }

    private static String digest(List<AuditedShapeFamily> families) throws NoSuchAlgorithmException {
        StringBuilder serialization = new StringBuilder();
        for (AuditedShapeFamily family : families) {
            serialization.append(family.key()).append('\t')
                    .append(family.category().name()).append('\t')
                    .append(family.canonicalParent()).append('\t');
            for (int index = 0; index < family.members().size(); index++) {
                if (index > 0) serialization.append(',');
                serialization.append(family.members().get(index));
            }
            serialization.append('\n');
        }
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(serialization.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<Integer, Long> sizeDistribution(AuditedShapeFamily.Category category) {
        return AuditedShapeFamilies.families(category).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        family -> family.members().size(),
                        java.util.stream.Collectors.counting()));
    }

    private static AuditedShapeFamily family(String keyPath) {
        Identifier key = id("interchangeable_block_families", keyPath);
        return AuditedShapeFamilies.families().stream()
                .filter(family -> family.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}

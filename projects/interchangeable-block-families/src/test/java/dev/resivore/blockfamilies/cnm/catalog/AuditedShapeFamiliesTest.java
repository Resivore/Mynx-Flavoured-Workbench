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
        assertEquals(254, AuditedShapeFamilies.families().size());
        assertEquals(1_879, AuditedShapeFamilies.uniqueMemberCount());
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
                WINDOW, 62,
                DISPLAY_FIXTURE, 15,
                FENCE_GATE, 16,
                BAR_CHAIN, 10,
                BBB_DETAIL, 42,
                BUILDING_ACCESSORY, 71);
        Map<AuditedShapeFamily.Category, Integer> expectedMembers = Map.of(
                TWO_HIGH_DOOR, 273,
                THREE_HIGH_DOOR, 229,
                TRAPDOOR, 208,
                WINDOW, 275,
                DISPLAY_FIXTURE, 45,
                FENCE_GATE, 62,
                BAR_CHAIN, 30,
                BBB_DETAIL, 221,
                BUILDING_ACCESSORY, 536);

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

        assertFalse(keys.contains(id("interchangeable_block_families", "cnm/building_accessory/end_stone_bricks_button")));
        assertTrue(members.stream().noneMatch(member -> member.getNamespace().equals("extshape")));
        assertEquals(254, keys.size());
        assertEquals(1_879, members.size());
    }

    @Test
    void familyAndMemberOrderIsStableAndRepresentative() {
        List<AuditedShapeFamily> families = AuditedShapeFamilies.families();
        assertEquals(id("interchangeable_block_families", "cnm/two_high_door/oak"),
                families.getFirst().key());
        assertEquals(id("interchangeable_block_families", "cnm/window/dark_oak_log"),
                families.get(48).key());
        assertEquals(id("interchangeable_block_families", "cnm/fence_gate/warped"),
                families.get(126).key());
        assertEquals(id("interchangeable_block_families", "cnm/fence_gate/ribbits_mossy_oak_planks"),
                families.get(127).key());
        assertEquals(id("interchangeable_block_families", "cnm/bar_chain/iron"),
                families.get(131).key());
        assertEquals(id("interchangeable_block_families", "cnm/bbb_detail/wood/oak"),
                families.get(141).key());
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
        assertEquals(Map.of(4, 35L, 5, 27L), sizeDistribution(WINDOW));
        assertEquals(Map.of(3, 15L), sizeDistribution(DISPLAY_FIXTURE));
        assertEquals(Map.of(2, 1L, 4, 15L), sizeDistribution(FENCE_GATE));
        assertEquals(Map.of(2, 1L, 3, 8L, 4, 1L), sizeDistribution(BAR_CHAIN));
        assertEquals(Map.of(4, 15L, 5, 1L, 6, 26L), sizeDistribution(BBB_DETAIL));
        assertEquals(Map.of(2, 3L, 4, 28L, 9, 11L, 11, 29L),
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
                id("mcwwindows", "cobblestone_arrow_slit"),
                id("mcwwindows", "blackstone_brick_arrow_slit"),
                id("architectural_material_closure", "nether_bricks_crystal_floor"),
                id("architectural_material_closure", "nether_bricks_flagstone"),
                id("architectural_material_closure", "nether_bricks_running_bond"),
                id("architectural_material_closure", "nether_bricks_windmill_weave"),
                id("architectural_material_closure", "end_stone_bricks_crystal_floor"),
                id("architectural_material_closure", "end_stone_bricks_flagstone"),
                id("architectural_material_closure", "end_stone_bricks_running_bond"),
                id("architectural_material_closure", "end_stone_bricks_windmill_weave"),
                id("architectural_material_closure", "quartz_crystal_floor"),
                id("architectural_material_closure", "quartz_flagstone"),
                id("architectural_material_closure", "quartz_running_bond"),
                id("architectural_material_closure", "quartz_windmill_weave"),
                id("architectural_material_closure", "prismarine_bricks_running_bond"),
                id("architectural_material_closure", "prismarine_bricks_windmill_weave"),
                id("architectural_material_closure", "prismarine_bricks_crystal_floor"),
                id("architectural_material_closure", "prismarine_bricks_flagstone"),
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
        assertEquals("20056131E05BCBDDE38CAC00D13ED49023D626619DDEF26FB8831FEE8DB872B6",
                digest(AuditedShapeFamilies.families()));
    }

    @Test
    void earlierCanaryCatalogsKeepTheirExactOrderAndMembership() throws NoSuchAlgorithmException {
        List<AuditedShapeFamily> c26 = previousCatalog(AuditedShapeFamilies.families(),
                Set.of("cnm/building_accessory/calcite_button", "cnm/bbb_detail/stone/calcite",
                        "cnm/window/calcite", "cnm/building_accessory/calcite",
                        "cnm/building_accessory/shadoline_block_button", "cnm/bbb_detail/stone/shadoline_block",
                        "cnm/window/shadoline_block", "cnm/building_accessory/shadoline_block"), Map.of());
        assertEquals(246, c26.size());
        assertEquals("755BD84010E39E36A61EAA10AB715C953607396834B054F2285A5FAA82A02539", digest(c26));

        Set<String> c26Keys = new HashSet<>();
        for (String material : List.of("purpur", "dusk_purpur", "resin")) {
            c26Keys.add("cnm/building_accessory/" + material + "_button");
            c26Keys.add("cnm/bbb_detail/stone/" + material);
            c26Keys.add("cnm/window/" + material);
            c26Keys.add("cnm/building_accessory/" + material);
        }
        List<AuditedShapeFamily> c25 = previousCatalog(c26, c26Keys, Map.of());
        assertEquals(234, c25.size());
        assertEquals("4F2E6C7C9BDBF32D7539763E3B49402C8648955B1291314E992A31CF7A6A30FE", digest(c25));

        Set<String> newEnderscapeKeys = new HashSet<>();
        for (String material : List.of("veradite", "mirestone", "kurodite")) {
            newEnderscapeKeys.add("cnm/bbb_detail/stone/" + material);
            newEnderscapeKeys.add("cnm/window/" + material);
            newEnderscapeKeys.add("cnm/building_accessory/" + material);
        }
        List<AuditedShapeFamily> c24 = previousCatalog(c25, newEnderscapeKeys,
                Map.of("cnm/building_accessory/enderscape_polished_veradite", 2,
                        "cnm/building_accessory/enderscape_polished_mirestone", 2,
                        "cnm/building_accessory/enderscape_polished_kurodite", 2));
        assertEquals(225, c24.size());
        assertEquals("BC0038DE836E6208B7AC9FA863CBBE53349EE464DDD98BE9F2ACD9936C13AAD4", digest(c24));

        Set<String> addedKeys = new HashSet<>();
        for (String material : List.of("tuff", "cinnabar", "sulfur", "smooth_basalt")) {
            addedKeys.add("cnm/building_accessory/" + material + "_button");
            addedKeys.add("cnm/bbb_detail/stone/" + material);
            addedKeys.add("cnm/window/" + material);
            addedKeys.add("cnm/building_accessory/" + material);
        }
        List<AuditedShapeFamily> c23 = previousCatalog(c24, addedKeys, Map.of());
        assertEquals(209, c23.size());
        assertEquals("71E85A472C26C030E24439FEDCE4ECBDBE538667FE126F91C9D1B86F89B0198B", digest(c23));

        List<AuditedShapeFamily> c22 = previousCatalog(c23,
                Set.of("cnm/building_accessory/mossy_stone_bricks_button", "cnm/bbb_detail/stone/mossy_stone_bricks",
                        "cnm/window/mossy_stone_bricks"), Map.of());
        assertEquals(206, c22.size());
        assertEquals("01B6DD6FF7B4466963DDD5B6962A4EB3ED0A3BB25153B5229378EEFE5920231E", digest(c22));

        List<AuditedShapeFamily> c21 = previousCatalog(c22,
                Set.of("cnm/building_accessory/end_stone_bricks",
                        "cnm/bbb_detail/stone/end_stone_bricks", "cnm/window/end_stone_bricks"),
                Map.of("cnm/building_accessory/polished_blackstone", 1,
                        "cnm/building_accessory/enderscape_polished_end_stone", 2));
        assertEquals(203, c21.size());
        assertEquals("9700AE6B2D7DBDE923A0D3C5C28422C450BAB5EB73686E23DA83695A3CBDE159", digest(c21));

        List<AuditedShapeFamily> c20 = previousCatalog(c21,
                Set.of("cnm/building_accessory/deepslate_button", "cnm/building_accessory/quartz_button",
                        "cnm/building_accessory/nether_bricks_button", "cnm/building_accessory/quartz",
                        "cnm/building_accessory/nether_bricks", "cnm/window/nether_bricks"),
                Map.of("cnm/window/deepslate", 1, "cnm/window/quartz_block", 1,
                        "cnm/bbb_detail/stone/deepslate", 1, "cnm/bbb_detail/stone/quartz", 1));
        assertEquals(197, c20.size());
        assertEquals("288DC62FCE86619038830C050BD5D6530AFF4C3A2525BAFE93CA16215D9BF8F9", digest(c20));

        Set<String> sandstoneButtons = Set.of("cnm/building_accessory/sandstone_button",
                "cnm/building_accessory/red_sandstone_button");
        Map<String, Integer> sandstoneExtensions = Map.of("cnm/window/sandstone", 1, "cnm/window/red_sandstone", 1,
                "cnm/bbb_detail/stone/sandstone", 1, "cnm/bbb_detail/stone/red_sandstone", 1);
        List<AuditedShapeFamily> c19 = previousCatalog(c20, sandstoneButtons, sandstoneExtensions);
        assertEquals(195, c19.size());
        assertEquals("B1C74A1432ECF8AC0C229AD1A964F0C67D7DC4EAE80FB04EED2D6094CACD2C05", digest(c19));

        Set<String> prismarineAdditions = Set.of("cnm/building_accessory/prismarine_bricks_button",
                "cnm/building_accessory/dark_prismarine_button", "cnm/building_accessory/prismarine_bricks",
                "cnm/bbb_detail/stone/prismarine_bricks", "cnm/bbb_detail/stone/dark_prismarine");
        List<AuditedShapeFamily> c18 = c19.stream()
                .filter(family -> !prismarineAdditions.contains(family.key().getPath()))
                .toList();
        assertEquals(190, c18.size());
        assertEquals("D92138F9362A8CBB8BF7B9D220390072B7639866DA1C9BF50D7010FF4ABB9575", digest(c18));

        Set<String> additions = Set.of("cnm/building_accessory/mud_bricks_button",
                "cnm/bbb_detail/stone/mud_bricks", "cnm/window/mud_bricks");
        List<AuditedShapeFamily> previous = c18.stream()
                .filter(family -> !additions.contains(family.key().getPath()))
                .toList();
        assertEquals(187, previous.size());
        assertEquals("65AFD8CA3B6A59D59FA039A4B971D160CA47455AF94631B80A64D59FC695E6E9", digest(previous));
    }

    private static List<AuditedShapeFamily> previousCatalog(List<AuditedShapeFamily> current,
            Set<String> addedKeys, Map<String, Integer> appendedCounts) {
        return current.stream()
                .filter(family -> !addedKeys.contains(family.key().getPath()))
                .map(family -> {
                    int appended = appendedCounts.getOrDefault(family.key().getPath(), 0);
                    if (appended == 0) return family;
                    return new AuditedShapeFamily(family.key(), family.category(), family.canonicalParent(),
                            family.members().subList(0, family.members().size() - appended));
                })
                .toList();
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

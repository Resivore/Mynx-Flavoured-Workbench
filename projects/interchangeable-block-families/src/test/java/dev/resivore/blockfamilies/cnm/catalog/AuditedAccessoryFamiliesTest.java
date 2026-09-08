package dev.resivore.blockfamilies.cnm.catalog;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BAR_CHAIN;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BUILDING_ACCESSORY;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.FENCE_GATE;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.THREE_HIGH_DOOR;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.TRAPDOOR;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.TWO_HIGH_DOOR;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.WINDOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuditedAccessoryFamiliesTest {
    @Test
    void ironAndEveryCopperFinishHaveOneExactBarChainFamily() {
        List<ExpectedFamily> expected = List.of(
                quadruple("iron", "minecraft:iron_bars", "bbb:iron_fence", "minecraft:iron_chain", "auroraslanterns:chandelier/iron"),
                triple("copper", "minecraft:copper_bars", "minecraft:copper_chain", "auroraslanterns:chandelier/copper"),
                triple("exposed_copper", "minecraft:exposed_copper_bars",
                        "minecraft:exposed_copper_chain", "auroraslanterns:chandelier/exposed_copper"),
                triple("weathered_copper", "minecraft:weathered_copper_bars",
                        "minecraft:weathered_copper_chain", "auroraslanterns:chandelier/weathered_copper"),
                triple("oxidized_copper", "minecraft:oxidized_copper_bars",
                        "minecraft:oxidized_copper_chain", "auroraslanterns:chandelier/oxidized_copper"),
                triple("waxed_copper", "minecraft:waxed_copper_bars",
                        "minecraft:waxed_copper_chain", "auroraslanterns:chandelier/waxed_copper"),
                triple("waxed_exposed_copper", "minecraft:waxed_exposed_copper_bars",
                        "minecraft:waxed_exposed_copper_chain", "auroraslanterns:chandelier/waxed_exposed_copper"),
                triple("waxed_weathered_copper", "minecraft:waxed_weathered_copper_bars",
                        "minecraft:waxed_weathered_copper_chain", "auroraslanterns:chandelier/waxed_weathered_copper"),
                triple("waxed_oxidized_copper", "minecraft:waxed_oxidized_copper_bars",
                        "minecraft:waxed_oxidized_copper_chain", "auroraslanterns:chandelier/waxed_oxidized_copper"));

        List<AuditedShapeFamily> actual = AuditedShapeFamilies.families(BAR_CHAIN);
        assertEquals(expected.size(), actual.size());
        Set<Identifier> members = new LinkedHashSet<>();
        for (int index = 0; index < expected.size(); index++) {
            ExpectedFamily expectedFamily = expected.get(index);
            AuditedShapeFamily actualFamily = actual.get(index);
            assertEquals(id("interchangeable_block_families:cnm/bar_chain/"
                    + expectedFamily.keySuffix()), actualFamily.key());
            assertEquals(BAR_CHAIN, actualFamily.category());
            assertEquals(expectedFamily.members(), actualFamily.members());
            assertEquals(expectedFamily.members().getFirst(), actualFamily.canonicalParent());
            assertTrue(members.addAll(actualFamily.members()), actualFamily.key().toString());
        }
        assertEquals(28, members.size());

        for (int first = 1; first < actual.size(); first++) {
            for (int second = first + 1; second < actual.size(); second++) {
                Set<Identifier> overlap = new HashSet<>(actual.get(first).members());
                overlap.retainAll(actual.get(second).members());
                assertTrue(overlap.isEmpty(), "Copper oxidation/wax finishes overlap: " + overlap);
                assertNotEquals(actual.get(first).canonicalParent(), actual.get(second).canonicalParent());
            }
        }
    }

    @Test
    void buildingAccessoriesStayMaterialBoundedWhileAllowingMissingForms() {
        assertEquals(List.of(
                        id("minecraft:oak_button"),
                        id("minecraft:oak_pressure_plate"),
                        id("mcwpaths:oak_planks_path"),
                        id("mcwwindows:oak_log_parapet"),
                        id("mcwwindows:oak_plank_parapet"),
                        id("mcwwindows:oak_blinds"),
                        id("mcwwindows:oak_curtain_rod")),
                family("oak").members());
        assertEquals(List.of(
                        id("minecraft:spruce_button"),
                        id("minecraft:spruce_pressure_plate"),
                        id("mcwpaths:spruce_planks_path"),
                        id("mcwwindows:spruce_log_parapet"),
                        id("mcwwindows:spruce_plank_parapet"),
                        id("mcwwindows:spruce_blinds"),
                        id("mcwwindows:spruce_curtain_rod")),
                family("spruce").members());
        assertEquals(List.of(
                        id("minecraft:bamboo_button"),
                        id("minecraft:bamboo_pressure_plate"),
                        id("mcwpaths:bamboo_planks_path")),
                family("bamboo").members());
        assertEquals(List.of(
                        id("minecraft:light_weighted_pressure_plate"),
                        id("mcwwindows:golden_curtain_rod")),
                family("gold").members());
        assertEquals(List.of(
                        id("minecraft:heavy_weighted_pressure_plate"),
                        id("mcwwindows:metal_curtain_rod")),
                family("iron").members());
        assertEquals(List.of(
                        id("minecraft:polished_blackstone_button"),
                        id("minecraft:polished_blackstone_pressure_plate"),
                        id("mcwwindows:blackstone_parapet")),
                family("polished_blackstone").members());
        assertEquals(List.of(
                        id("mcwpaths:blackstone_running_bond_path"),
                        id("mcwpaths:blackstone_strewn_rocky_path"),
                        id("mcwpaths:blackstone_windmill_weave_path"),
                        id("mcwpaths:blackstone_flagstone_path"),
                        id("mcwpaths:blackstone_crystal_floor_path"),
                        id("mcwpaths:blackstone_diamond_paving"),
                        id("mcwpaths:blackstone_basket_weave_paving"),
                        id("mcwpaths:blackstone_square_paving"),
                        id("mcwpaths:blackstone_honeycomb_paving"),
                        id("mcwpaths:blackstone_clover_paving"),
                        id("mcwpaths:blackstone_dumble_paving")),
                family("blackstone").members());

        Set<Identifier> oak = Set.copyOf(family("oak").members());
        Set<Identifier> spruce = Set.copyOf(family("spruce").members());
        assertTrue(java.util.Collections.disjoint(oak, spruce));
        for (String wood : List.of(
                "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped")) {
            for (Identifier member : family(wood).members()) {
                assertTrue(member.getPath().startsWith(wood + "_"),
                        family(wood).key() + " crosses its species boundary at " + member);
            }
        }
        assertFalse(family("blackstone").members().contains(id("mcwwindows:blackstone_parapet")));
        assertFalse(allMembers().contains(id("mcwwindows:prismarine_parapet")),
                "Unapproved singleton parapet entered the catalog");

        List<AuditedShapeFamily> accessories = AuditedShapeFamilies.families(BUILDING_ACCESSORY);
        assertEquals(28, accessories.size());
        assertEquals(236, accessories.stream().mapToInt(value -> value.members().size()).sum());
        for (AuditedShapeFamily accessory : accessories) {
            assertTrue(accessory.members().size() >= 2, "Singleton family " + accessory.key());
        }
        for (int first = 0; first < accessories.size(); first++) {
            for (int second = first + 1; second < accessories.size(); second++) {
                Set<Identifier> overlap = new HashSet<>(accessories.get(first).members());
                overlap.retainAll(accessories.get(second).members());
                assertTrue(overlap.isEmpty(), accessories.get(first).key() + " overlaps "
                        + accessories.get(second).key() + ": " + overlap);
            }
        }
    }

    @Test
    void onlyTheExactAuditedMacawPathAndPavingFormsAreAdmitted() {
        Set<String> expected = new LinkedHashSet<>();
        for (String wood : List.of(
                "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped")) {
            expected.add("mcwpaths:" + wood + "_planks_path");
        }
        for (String material : List.of(
                "stone", "andesite", "diorite", "granite", "sandstone", "red_sandstone",
                "brick", "mossy_stone", "cobbled_deepslate", "deepslate", "mud_brick",
                "blackstone", "dark_prismarine")) {
            for (String design : List.of(
                    "running_bond_path", "strewn_rocky_path", "windmill_weave_path",
                    "flagstone_path", "crystal_floor_path")) {
                expected.add("mcwpaths:" + material + "_" + design);
            }
        }
        for (String material : List.of(
                "andesite", "diorite", "granite", "sandstone", "red_sandstone", "brick",
                "cobblestone", "mossy_cobblestone", "cobbled_deepslate", "deepslate", "mud_brick",
                "blackstone", "dark_prismarine")) {
            for (String design : List.of(
                    "diamond_paving", "basket_weave_paving", "square_paving", "honeycomb_paving",
                    "clover_paving", "dumble_paving")) {
                expected.add("mcwpaths:" + material + "_" + design);
            }
        }

        Set<String> actual = new LinkedHashSet<>();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families(BUILDING_ACCESSORY)) {
            for (Identifier member : family.members()) {
                if (member.getNamespace().equals("mcwpaths")) {
                    actual.add(member.toString());
                }
            }
        }
        assertEquals(155, actual.size());
        assertEquals(expected, actual);
        assertEquals(78, actual.stream().filter(value -> value.endsWith("_paving")).count());
        assertTrue(actual.stream().noneMatch(value -> value.endsWith("_path_block")));

        for (String excluded : List.of(
                "mcwpaths:andesite_running_bond",
                "mcwpaths:andesite_crystal_floor",
                "mcwpaths:andesite_flagstone",
                "mcwpaths:andesite_windmill_weave",
                "mcwpaths:andesite_running_bond_slab",
                "mcwpaths:andesite_running_bond_stairs",
                "mcwpaths:andesite_running_bond_paving",
                "mcwpaths:andesite_paving_slab",
                "mcwpaths:andesite_paving_stairs",
                "mcwpaths:dirt_path_block",
                "mcwpaths:gravel_path_block",
                "mcwpaths:podzol_path_block",
                "mcwpaths:sand_path_block",
                "mcwpaths:red_sand_path_block")) {
            assertFalse(allMembers().contains(id(excluded)), excluded);
        }
    }

    @Test
    void legacyCatalogLiteralSerializationRemainsByteStable() throws Exception {
        Set<AuditedShapeFamily.Category> legacy = EnumSet.of(
                TWO_HIGH_DOOR, THREE_HIGH_DOOR, TRAPDOOR, WINDOW, FENCE_GATE);
        List<AuditedShapeFamily> legacyFamilies = AuditedShapeFamilies.families().stream()
                .filter(family -> legacy.contains(family.category()))
                .filter(family -> !family.key().getPath().equals("cnm/fence_gate/ribbits_mossy_oak_planks"))
                .toList();

        assertEquals(96, legacyFamilies.size());
        assertEquals(968, legacyFamilies.stream().mapToInt(family -> family.members().size()).sum());
        assertEquals("F8B115E088749AE24204FB71999CDCFF4BE4867D732C70D2777EE91726D17335",
                digest(legacyFamilies));
    }

    void ribbitsMossyOakFenceGateIsTheOnlyRibbitsFamily() {
        List<AuditedShapeFamily> fences = AuditedShapeFamilies.families(FENCE_GATE);
        AuditedShapeFamily mossyOak = fences.getLast();
        assertEquals(id("interchangeable_block_families:cnm/fence_gate/ribbits_mossy_oak_planks"), mossyOak.key());
        assertEquals(List.of(id("ribbits:mossy_oak_planks_fence"),
                id("ribbits:mossy_oak_planks_fence_gate")), mossyOak.members());
        assertEquals(1, fences.stream().filter(family -> family.members().stream()
                .anyMatch(member -> member.getNamespace().equals("ribbits"))).count());
        assertFalse(allMembers().contains(id("ribbits:mossy_oak_planks_slab")));
    }

    private static ExpectedFamily triple(String keySuffix, String parent, String alternative,
                                         String chandelier) {
        return new ExpectedFamily(keySuffix, List.of(id(parent), id(alternative), id(chandelier)));
    }

    private static ExpectedFamily quadruple(
            String keySuffix, String parent, String firstAlternative, String secondAlternative, String chandelier) {
        return new ExpectedFamily(keySuffix,
                List.of(id(parent), id(firstAlternative), id(secondAlternative), id(chandelier)));
    }

    private static AuditedShapeFamily family(String keySuffix) {
        Identifier key = id("interchangeable_block_families:cnm/building_accessory/" + keySuffix);
        return AuditedShapeFamilies.families(BUILDING_ACCESSORY).stream()
                .filter(value -> value.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private static Set<Identifier> allMembers() {
        Set<Identifier> members = new HashSet<>();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families()) {
            for (Identifier member : family.members()) {
                assertTrue(members.add(member), member + " occurs in more than one family");
            }
        }
        return Set.copyOf(members);
    }

    private static String digest(List<AuditedShapeFamily> families) throws Exception {
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

    private static Identifier id(String value) {
        return Identifier.parse(value);
    }

    private record ExpectedFamily(String keySuffix, List<Identifier> members) {
    }
}

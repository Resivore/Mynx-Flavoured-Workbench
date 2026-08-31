package com.starfish_studios.bbb.porting;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CuratedRegistryContractTest {
    private static final Path PROJECT = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    private static final Path MANIFEST = PROJECT.resolve("src/porting/curated-registry.json");

    private static final Set<String> REMOVED_BLOCKS = ids("""
            oak_layer spruce_layer birch_layer jungle_layer acacia_layer dark_oak_layer
            crimson_layer warped_layer mangrove_layer bamboo_layer bamboo_mosaic_layer cherry_layer
            oak_ladder spruce_ladder birch_ladder jungle_ladder acacia_ladder dark_oak_ladder
            crimson_ladder warped_ladder mangrove_ladder bamboo_ladder cherry_ladder
            stone_block wall_stone_block blackstone_block wall_blackstone_block
            deepslate_block wall_deepslate_block nether_brick_block wall_nether_brick_block
            sandstone_block wall_sandstone_block red_sandstone_block wall_red_sandstone_block
            quartz_block wall_quartz_block
            moss_layer stone_layer cobblestone_layer mossy_cobblestone_layer smooth_stone_layer
            polished_stone_layer stone_tile_layer stone_brick_layer mossy_stone_brick_layer
            granite_layer polished_granite_layer diorite_layer polished_diorite_layer
            andesite_layer polished_andesite_layer cobbled_deepslate_layer polished_deepslate_layer
            deepslate_brick_layer deepslate_tile_layer brick_layer mud_brick_layer sandstone_layer
            smooth_sandstone_layer red_sandstone_layer smooth_red_sandstone_layer prismarine_layer
            prismarine_brick_layer dark_prismarine_layer nether_brick_layer red_nether_brick_layer
            blackstone_layer polished_blackstone_layer polished_blackstone_brick_layer
            end_stone_brick_layer purpur_layer quartz_layer cut_copper_layer
            exposed_cut_copper_layer weathered_cut_copper_layer oxidized_cut_copper_layer
            waxed_cut_copper_layer waxed_exposed_cut_copper_layer waxed_weathered_cut_copper_layer
            waxed_oxidized_cut_copper_layer polished_stone polished_stone_stairs polished_stone_slab
            stone_tiles stone_tile_stairs stone_tile_slab
            """);

    private static final Set<String> INTERNAL_BLOCKS_WITHOUT_ITEMS = Set.of(
            "wall_stone_block", "wall_blackstone_block", "wall_deepslate_block",
            "wall_nether_brick_block", "wall_sandstone_block", "wall_red_sandstone_block",
            "wall_quartz_block"
    );

    @Test
    void manifestExpandsToTheExactCuratedRegistry() throws IOException {
        String json = Files.readString(MANIFEST);
        List<String> woodMaterials = stringArray(json, "wood_materials");
        List<String> woodForms = stringArray(json, "wood_forms");
        List<String> stoneMaterials = stringArray(json, "stone_materials");
        List<String> stoneForms = stringArray(json, "stone_forms");
        List<String> standaloneBlocks = stringArray(json, "standalone_blocks");
        List<String> standaloneItems = stringArray(json, "standalone_items");

        assertEquals(List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "crimson", "warped", "mangrove", "bamboo", "cherry"), woodMaterials);
        assertEquals(List.of("balustrade", "lattice", "wall", "beam", "beam_stairs",
                "beam_slab", "support", "pallet", "frame", "lantern", "trim"), woodForms);
        assertEquals(List.of("stone", "blackstone", "deepslate", "nether_brick", "sandstone",
                "red_sandstone", "quartz"), stoneMaterials);
        assertEquals(List.of("column", "urn", "moulding", "fence", "frame"), stoneForms);
        assertEquals(List.of("brazier", "soul_brazier", "rope", "iron_fence"), standaloneBlocks);
        assertEquals(List.of("hammer"), standaloneItems);

        Set<String> retainedBlocks = retainedBlocks(json);
        Set<String> retainedItems = new LinkedHashSet<>(retainedBlocks);
        retainedItems.addAll(standaloneItems);

        assertEquals(160, retainedBlocks.size());
        assertEquals(161, retainedItems.size());
        assertEquals(160, integer(json, "expected_retained_block_count"));
        assertEquals(161, integer(json, "expected_retained_item_count"));
        assertEquals(247, integer(json, "upstream_block_count"));
        assertEquals(243, integer(json, "upstream_item_count"));
        assertFalse(retainedBlocks.contains("pale_oak_beam"));
        assertTrue(retainedItems.contains("hammer"));
    }

    @Test
    void removalsExactlyReconcileThePublishedRegistryCounts() throws IOException {
        String json = Files.readString(MANIFEST);
        Set<String> retainedBlocks = retainedBlocks(json);
        Set<String> removedItems = new LinkedHashSet<>(REMOVED_BLOCKS);
        removedItems.removeAll(INTERNAL_BLOCKS_WITHOUT_ITEMS);
        removedItems.add("bbb");
        removedItems.add("chisel");

        assertEquals(87, REMOVED_BLOCKS.size());
        assertEquals(82, removedItems.size());
        assertEquals(247, retainedBlocks.size() + REMOVED_BLOCKS.size());
        assertEquals(243, retainedBlocks.size() + 1 + removedItems.size());
        assertTrue(disjoint(retainedBlocks, REMOVED_BLOCKS));
        assertTrue(disjoint(retainedBlocks, removedItems));
        assertTrue(removedItems.containsAll(Set.of("bbb", "chisel", "bamboo_mosaic_layer")));
        assertFalse(removedItems.stream().anyMatch(INTERNAL_BLOCKS_WITHOUT_ITEMS::contains));
    }

    @Test
    void noRemovedFamilyCanEnterThroughTheRetainedFormula() throws IOException {
        Set<String> retained = retainedBlocks(Files.readString(MANIFEST));
        assertTrue(retained.stream().noneMatch(id -> id.endsWith("_layer")));
        assertTrue(retained.stream().noneMatch(id -> id.endsWith("_ladder")));
        assertTrue(retained.stream().noneMatch(id -> id.matches("(?:wall_)?(?:stone|blackstone|deepslate|nether_brick|sandstone|red_sandstone|quartz)_block")));
        assertTrue(retained.stream().noneMatch(id -> id.equals("polished_stone") || id.startsWith("stone_tile")));
        assertTrue(disjoint(retained, REMOVED_BLOCKS));
    }

    private static Set<String> retainedBlocks(String json) {
        Set<String> result = new LinkedHashSet<>();
        for (String material : stringArray(json, "wood_materials")) {
            for (String form : stringArray(json, "wood_forms")) {
                result.add(material + "_" + form);
            }
        }
        for (String material : stringArray(json, "stone_materials")) {
            for (String form : stringArray(json, "stone_forms")) {
                result.add(material + "_" + form);
            }
        }
        result.addAll(stringArray(json, "standalone_blocks"));
        return result;
    }

    private static List<String> stringArray(String json, String key) {
        Matcher field = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL)
                .matcher(json);
        assertTrue(field.find(), () -> "Missing array " + key);
        Matcher values = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(field.group(1));
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        while (values.find()) result.add(values.group(1));
        return List.copyOf(result);
    }

    private static int integer(String json, String key) {
        Matcher field = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(\\d+)").matcher(json);
        assertTrue(field.find(), () -> "Missing integer " + key);
        return Integer.parseInt(field.group(1));
    }

    private static Set<String> ids(String text) {
        return Arrays.stream(text.strip().split("[\\s,]+"))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean disjoint(Set<String> left, Set<String> right) {
        return left.stream().noneMatch(right::contains);
    }
}

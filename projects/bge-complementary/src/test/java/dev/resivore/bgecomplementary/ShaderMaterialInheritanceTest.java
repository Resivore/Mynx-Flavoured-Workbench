package dev.resivore.bgecomplementary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShaderMaterialInheritanceTest {
    @Test
    void projectsPhysicalStatesWithoutLeakingGeometryOnlyState() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("cyan_stained_glass[color=cyan]", 41);

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge_cyan_stair[facing=east,shape=outer_left]"),
                ignored -> Optional.of("cyan_stained_glass[color=cyan]"),
                parent -> parent.startsWith("cyan_stained_glass"));

        assertEquals(41, ids.get("bge_cyan_stair[facing=east,shape=outer_left]"));
        assertEquals(1, result.inherited());
    }

    @Test
    void preservesExplicitPhysicalAssignmentIncludingZero() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("iron_block", 19);
        ids.put("bge_iron_slab", 0);

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge_iron_slab"), ignored -> Optional.of("iron_block"),
                Set.of("iron_block")::contains);

        assertEquals(0, ids.get("bge_iron_slab"));
        assertEquals(1, result.explicitPhysical());
    }

    @Test
    void leavesUnmappedCanonicalParentUnmapped() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge_emerald_corner"), ignored -> Optional.of("emerald_block"),
                Set.of("emerald_block")::contains);

        assertFalse(ids.containsKey("bge_emerald_corner"));
        assertEquals(1, result.missingParent());
    }

    @Test
    void retainsDistinctStainedGlassParentIds() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("cyan_stained_glass", 71);
        ids.put("red_stained_glass", 72);

        ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge_cyan_layer", "bge_red_layer"),
                state -> Optional.of(state.equals("bge_cyan_layer")
                        ? "cyan_stained_glass" : "red_stained_glass"),
                Set.of("cyan_stained_glass", "red_stained_glass")::contains);

        assertEquals(71, ids.get("bge_cyan_layer"));
        assertEquals(72, ids.get("bge_red_layer"));
    }

    @Test
    void excludesGeometrySensitiveParentClasses() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("oak_leaves", 99);

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge_oak_leaves_wall"), ignored -> Optional.of("oak_leaves"),
                Set.of("glass", "iron_block")::contains);

        assertFalse(ids.containsKey("bge_oak_leaves_wall"));
        assertEquals(1, result.ineligible());
    }

    @Test
    void repeatedConstructionIsDeterministicAndIdempotent() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("gold_block", 123);
        List<String> physical = List.of("bge_gold_stair", "bge_gold_wall");

        ShaderMaterialInheritance.Result first = ShaderMaterialInheritance.inheritMissing(ids, physical,
                ignored -> Optional.of("gold_block"), Set.of("gold_block")::contains);
        Map<String, Integer> firstMap = new LinkedHashMap<>(ids);
        ShaderMaterialInheritance.Result second = ShaderMaterialInheritance.inheritMissing(ids, physical,
                ignored -> Optional.of("gold_block"), Set.of("gold_block")::contains);

        assertEquals(2, first.inherited());
        assertEquals(firstMap, ids);
        assertEquals(2, second.explicitPhysical());
    }

    @Test
    void missingProjectionCannotInventAMapping() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("glass", 17);

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge_unknown"), ignored -> Optional.empty(), Set.of("glass")::contains);

        assertFalse(ids.containsKey("bge_unknown"));
        assertEquals(1, result.missingCanonical());
        assertTrue(ids.containsKey("glass"));
    }
}

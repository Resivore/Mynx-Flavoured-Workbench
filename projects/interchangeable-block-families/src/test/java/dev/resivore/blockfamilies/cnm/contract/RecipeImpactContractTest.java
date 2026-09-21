package dev.resivore.blockfamilies.cnm.contract;

import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamilies;
import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeImpactContractTest {
    @Test
    void fixedAuditIdentityAndFamilyTotalsAreVersionBound() throws Exception {
        Properties contract = AuditFixtures.contract();

        assertEquals("ibf-cnm-shapemap-2026-09-21-canary8", contract.getProperty("audit.id"));
        assertEquals("5171db6eb0d575d11d1d57f10dd8148c515fb38d",
                contract.getProperty("audit.starting_commit"));
        assertEquals("9ad4600e62808e5e976392a69b495a2fe4b5d47c",
                contract.getProperty("audit.accepted_ibf_commit"));
        assertEquals("26.2", contract.getProperty("minecraft.version"));
        assertEquals(179, integer(contract, "families.total"));
        assertEquals(1_384, integer(contract, "members.total"));
        assertEquals(1_384, integer(contract, "members.unique"));
        assertEquals(22, integer(contract, "family.largest"));

        for (String key : new String[]{
                "minecraft.mapped.sha256",
                "cnm.jar.sha256",
                "macaws_doors.sha256",
                "macaws_paths.sha256",
                "auroras_lanterns.sha256",
                "ribbits.sha256",
                "bbb.jar.sha256",
                "enderscape.jar.sha256",
                "lithostitched.jar.sha256",
                "trim_patcher.jar.sha256",
                "macaws_trapdoors.sha256",
                "macaws_windows.sha256",
                "dramatic_doors.sha256",
                "dramatic_macaw_source.sha256",
                "accepted_nibaru_integration.jar.sha256",
                "accepted_nibaru_adapter_source.sha256",
                "family.catalog.sha256",
                "recipe.dynamic_modeled_fixture.logical_sha256"
        }) {
            assertTrue(contract.getProperty(key).matches("[0-9A-F]{64}"), key + " is not an exact SHA-256");
        }
    }

    @Test
    void recipeImpactFixtureIsBoundToTheExactExpandedCatalog() throws Exception {
        StringBuilder serialization = new StringBuilder();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families()) {
            serialization.append(family.key()).append('\t')
                    .append(family.category().name()).append('\t')
                    .append(family.canonicalParent()).append('\t');
            for (int index = 0; index < family.members().size(); index++) {
                if (index > 0) serialization.append(',');
                serialization.append(family.members().get(index));
            }
            serialization.append('\n');
        }

        Properties contract = AuditFixtures.contract();
        assertEquals("key-tab-category-name-tab-parent-tab-comma-members-lf",
                contract.getProperty("family.catalog.serialization"));
        assertEquals(contract.getProperty("family.catalog.sha256"),
                AuditFixtures.sha256(serialization.toString().getBytes(StandardCharsets.UTF_8)));

        String recipeFixture = new String(
                AuditFixtures.resourceBytes("dynamic-and-modeled-recipes.tsv"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        assertEquals(contract.getProperty("recipe.dynamic_modeled_fixture.logical_sha256"),
                AuditFixtures.sha256(recipeFixture.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void auditedRecipeResultMathRemainsExact() throws Exception {
        Map<String, Integer> expectedRemovals = Map.of(
                "two_high_doors", 260,
                "three_high_doors", 217,
                "trapdoors", 195,
                "windows_and_shutters_plus_accessories", 215,
                "building_accessories_paths", 143,
                "fence_gates", 12,
                "vanilla_bar_chain_and_accessories", 20,
                "display_fixtures", 24,
                "enderscape_approved_families", 17
        );
        Map<String, Integer> actualRemovals = new LinkedHashMap<>();
        int totalRemoved = 0;
        int dangerousParentRemovals = 0;
        int survivingLiteralRewrites = 0;

        for (String[] row : AuditFixtures.tsv("recipe-impact.tsv", 6)) {
            int auditedRecipes = Integer.parseInt(row[2]);
            int removed = Integer.parseInt(row[3]);
            int dangerous = Integer.parseInt(row[4]);
            int rewrites = Integer.parseInt(row[5]);
            assertTrue(removed <= auditedRecipes, row[0] + " removes more recipes than its fixed corpus contains");
            actualRemovals.put(row[0], removed);
            totalRemoved += removed;
            dangerousParentRemovals += dangerous;
            survivingLiteralRewrites += rewrites;
        }

        Properties contract = AuditFixtures.contract();
        assertEquals(expectedRemovals, actualRemovals);
        assertEquals(260 + 217 + 195 + 215 + 143 + 12 + 20 + 24 + 17, totalRemoved);
        assertEquals(1_103, totalRemoved);
        assertEquals(integer(contract, "recipe.non_parent_results_removed"), totalRemoved);
        assertEquals(0, dangerousParentRemovals);
        assertEquals(integer(contract, "recipe.dangerous_parent_results_removed"), dangerousParentRemovals);
        assertEquals(0, survivingLiteralRewrites);
        assertEquals(integer(contract, "recipe.surviving_literal_rewrites"), survivingLiteralRewrites);
        assertEquals(59, integer(contract, "recipe.dramatic_packaged_static_scanned"));
        assertEquals(0, integer(contract, "recipe.dramatic_packaged_static_removed"));
        assertEquals(9, integer(contract, "recipe.enderscape_custom_conversions_retained"));
        assertEquals(5, integer(contract,
                "recipe.enderscape_custom_alternate_results_retained"));
    }

    private static int integer(Properties properties, String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}

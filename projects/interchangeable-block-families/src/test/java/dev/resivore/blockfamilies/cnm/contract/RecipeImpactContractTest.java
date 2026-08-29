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

        assertEquals("ibf-cnm-shapemap-2026-08-28-canary4", contract.getProperty("audit.id"));
        assertEquals("d82d87a73d54c9fbf13a1d190d0cad8082dba2d4",
                contract.getProperty("audit.starting_commit"));
        assertEquals("b71932fb4c917edfa2c162600b01a4c1f2983dfe",
                contract.getProperty("audit.accepted_ibf_commit"));
        assertEquals("26.2", contract.getProperty("minecraft.version"));
        assertEquals(133, integer(contract, "families.total"));
        assertEquals(1_120, integer(contract, "members.total"));
        assertEquals(1_120, integer(contract, "members.unique"));
        assertEquals(22, integer(contract, "family.largest"));

        for (String key : new String[]{
                "minecraft.mapped.sha256",
                "cnm.jar.sha256",
                "macaws_doors.sha256",
                "macaws_paths.sha256",
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
                "building_accessories_paths", 65,
                "fence_gates", 12,
                "vanilla_bar_chain_and_accessories", 20
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
        assertEquals(260 + 217 + 195 + 215 + 65 + 12 + 20, totalRemoved);
        assertEquals(984, totalRemoved);
        assertEquals(integer(contract, "recipe.non_parent_results_removed"), totalRemoved);
        assertEquals(0, dangerousParentRemovals);
        assertEquals(integer(contract, "recipe.dangerous_parent_results_removed"), dangerousParentRemovals);
        assertEquals(0, survivingLiteralRewrites);
        assertEquals(integer(contract, "recipe.surviving_literal_rewrites"), survivingLiteralRewrites);
        assertEquals(59, integer(contract, "recipe.dramatic_packaged_static_scanned"));
        assertEquals(0, integer(contract, "recipe.dramatic_packaged_static_removed"));
    }

    private static int integer(Properties properties, String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}

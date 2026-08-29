package dev.resivore.blockfamilies.cnm.contract;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapeMapEquivalenceContractTest {
    @Test
    void representativeFamiliesUseLiteralParentsWithoutOverlap() throws Exception {
        Map<String, AuditFixtures.Family> families = AuditFixtures.representativeFamilies();
        Map<String, String> membership = AuditFixtures.memberToFamily(families);

        for (AuditFixtures.Family family : families.values()) {
            assertFalse(family.members().isEmpty(), family.key());
            assertEquals(family.parent(), family.members().getFirst(), family.key());
            assertTrue(family.members().size() <= family.auditedSize(), family.key());
            assertEquals(family.members().size(), new LinkedHashSet<>(family.members()).size(), family.key());
        }
        assertEquals(families.values().stream().mapToInt(family -> family.members().size()).sum(),
                membership.size(), "Representative contract contains an accidental cross-family overlap");
    }

    @Test
    void sameFamilyAndCrossMaterialAffinityAreExplicit() throws Exception {
        Map<String, String> membership = AuditFixtures.memberToFamily(AuditFixtures.representativeFamilies());

        for (String[] row : AuditFixtures.tsv("affinity-cases.tsv", 4)) {
            String sourceFamily = membership.get(row[1]);
            String targetFamily = membership.get(row[2]);
            boolean actual = sourceFamily != null && sourceFamily.equals(targetFamily);
            assertEquals(Boolean.parseBoolean(row[3]), actual, row[0]);
        }
    }

    @Test
    void broadDoorAndTrapdoorTagsCanonicalizeAndDedupeInStableOrder() throws Exception {
        Map<String, String> parentByMember =
                AuditFixtures.memberToParent(AuditFixtures.representativeFamilies());

        for (String[] row : AuditFixtures.tsv("tag-canonicalization.tsv", 4)) {
            List<String> expanded = List.of(row[2].split("\\|", -1));
            List<String> expected = List.of(row[3].split("\\|", -1));
            LinkedHashSet<String> canonical = new LinkedHashSet<>();
            for (String item : expanded) canonical.add(parentByMember.getOrDefault(item, item));
            assertEquals(expected, new ArrayList<>(canonical), row[0] + " / " + row[1]);
        }
    }
}

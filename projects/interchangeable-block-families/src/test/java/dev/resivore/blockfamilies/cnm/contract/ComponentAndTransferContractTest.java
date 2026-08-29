package dev.resivore.blockfamilies.cnm.contract;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentAndTransferContractTest {
    @Test
    void widenedQsnMergeRequiresEqualComponentPatches() throws Exception {
        Map<String, AuditFixtures.Family> families = AuditFixtures.representativeFamilies();
        Map<String, String> membership = AuditFixtures.memberToFamily(families);

        assertEquals("equal_component_patch",
                AuditFixtures.contract().getProperty("component.merge_policy"));
        for (String[] row : AuditFixtures.tsv("component-policy.tsv", 6)) {
            boolean sameShapeFamily = membership.containsKey(row[1])
                    && membership.get(row[1]).equals(membership.get(row[2]));
            boolean patchesEqual = AuditFixtures.componentPatch(row[3])
                    .equals(AuditFixtures.componentPatch(row[4]));
            assertEquals(Boolean.parseBoolean(row[5]), sameShapeFamily && patchesEqual, row[0]);
        }
    }

    @Test
    void transferToFirstMiddleAndLastPreservesCountAndSourcePatch() throws Exception {
        Map<String, AuditFixtures.Family> families = AuditFixtures.representativeFamilies();
        assertEquals("preserve", AuditFixtures.contract().getProperty("transfer.count_policy"));
        assertEquals("preserve_source_patch",
                AuditFixtures.contract().getProperty("transfer.component_patch_policy"));

        for (String[] row : AuditFixtures.tsv("transfer-cases.tsv", 7)) {
            AuditFixtures.Family family = families.get(row[1]);
            int targetIndex = switch (row[3]) {
                case "first" -> 0;
                case "middle" -> family.members().size() / 2;
                case "last" -> family.members().size() - 1;
                default -> throw new AssertionError("Unknown transfer position " + row[3]);
            };
            assertEquals(row[4], family.members().get(targetIndex), row[0] + " fixture order");

            Map<String, String> sourcePatch = AuditFixtures.componentPatch(row[6]);
            ContractStack source = new ContractStack(row[2], Integer.parseInt(row[5]), sourcePatch);
            ContractStack transferred = transmuteCopy(source, family.members().get(targetIndex));

            assertEquals(row[4], transferred.item(), row[0]);
            assertEquals(source.count(), transferred.count(), row[0]);
            assertEquals(source.componentPatch(), transferred.componentPatch(), row[0]);
            assertNotSame(source.componentPatch(), transferred.componentPatch(),
                    row[0] + " must copy rather than alias the source patch");
        }
    }

    @Test
    void sourcePatchComparisonDoesNotTreatTargetPrototypeIdentityAsCustomData() throws Exception {
        Map<String, String> membership =
                AuditFixtures.memberToFamily(AuditFixtures.representativeFamilies());
        ContractStack vanilla = new ContractStack("minecraft:oak_door", 1, Map.of());
        ContractStack macaw = new ContractStack("mcwdoors:oak_barn_door", 1, Map.of());

        assertTrue(canMerge(vanilla, macaw, membership),
                "Different target prototypes are intentional; only explicit component patches gate merging");
    }

    private static ContractStack transmuteCopy(ContractStack source, String targetItem) {
        return new ContractStack(targetItem, source.count(), source.componentPatch());
    }

    private static boolean canMerge(ContractStack source, ContractStack destination,
            Map<String, String> membership) {
        return membership.containsKey(source.item())
                && membership.get(source.item()).equals(membership.get(destination.item()))
                && source.componentPatch().equals(destination.componentPatch());
    }

    private record ContractStack(String item, int count, Map<String, String> componentPatch) {
        private ContractStack {
            componentPatch = Collections.unmodifiableMap(new LinkedHashMap<>(componentPatch));
        }
    }
}

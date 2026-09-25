package dev.resivore.amc;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C1's literal material/form boundary, independent of installed providers. */
final class ArchitecturalMaterialClosureContractTest {
    private static final List<String> MATERIALS = List.of(
            "andesite", "diorite", "granite", "brick", "mossy_stone_brick",
            "cobbled_deepslate", "mud_brick", "dark_prismarine");

    @Test
    void c1OwnsExactlyEightCompleteFiveFormMasonryFamilies() {
        assertEquals(40, ArchitecturalMaterialClosure.ownedIds().size());
        assertEquals(40, Set.copyOf(ArchitecturalMaterialClosure.ownedIds()).size());

        for (String material : MATERIALS) {
            assertEquals(List.of(
                    id(material + "_column"),
                    id(material + "_urn"),
                    id(material + "_moulding"),
                    id(material + "_fence"),
                    id(material + "_frame")), ArchitecturalMaterialClosure.ownedIds().stream()
                    .filter(id -> id.getPath().startsWith(material + "_"))
                    .toList(), material);
        }
    }

    @Test
    void c1DoesNotClaimAProviderNamespaceOrInferAdditionalMaterials() {
        Set<String> namespaces = ArchitecturalMaterialClosure.ownedIds().stream()
                .map(Identifier::getNamespace).collect(Collectors.toSet());
        assertEquals(Set.of(ArchitecturalMaterialClosure.MOD_ID), namespaces);
        assertTrue(ArchitecturalMaterialClosure.ownedIds().stream()
                .noneMatch(id -> id.getPath().contains("blackstone") || id.getPath().contains("quartz")));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ArchitecturalMaterialClosure.MOD_ID, path);
    }
}

package dev.resivore.amc;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C3's literal private-materialization boundary, independent of installed providers. */
final class ArchitecturalMaterialClosureContractTest {
    private static final List<String> MATERIALS = List.of(
            "stone", "andesite", "diorite", "granite", "brick", "mossy_stone_brick",
            "cobbled_deepslate", "deepslate", "mud_brick", "blackstone", "prismarine",
            "dark_prismarine", "sandstone", "red_sandstone", "quartz", "nether_brick", "end_brick");
    private static final List<String> FORMS = List.of(
            "column", "urn", "moulding", "fence", "frame",
            "running_bond_path", "strewn_rocky_path", "windmill_weave_path", "flagstone_path", "crystal_floor_path",
            "diamond_paving", "basket_weave_paving", "square_paving", "honeycomb_paving", "clover_paving", "dumble_paving",
            "window", "window2", "four_window", "pane_window", "parapet", "gothic", "arrow_slit", "louvered_shutter");

    @Test
    void c3OwnsEveryAuditedMasonryProfileAndForm() {
        assertEquals(MATERIALS.size() * FORMS.size(), ArchitecturalMaterialClosure.ownedIds().size());
        assertEquals(MATERIALS.size() * FORMS.size(), Set.copyOf(ArchitecturalMaterialClosure.ownedIds()).size());

        for (String material : MATERIALS) {
            assertEquals(FORMS.stream().map(form -> id(material + "_" + form)).toList(), ArchitecturalMaterialClosure.ownedIds().stream()
                    .filter(id -> id.getPath().startsWith(material + "_"))
                    .toList(), material);
        }
    }

    @Test
    void c3DoesNotClaimAProviderNamespace() {
        Set<String> namespaces = ArchitecturalMaterialClosure.ownedIds().stream()
                .map(Identifier::getNamespace).collect(Collectors.toSet());
        assertEquals(Set.of(ArchitecturalMaterialClosure.MOD_ID), namespaces);
        assertTrue(ArchitecturalMaterialClosure.ownedIds().stream()
                .allMatch(id -> id.getPath().contains("_")));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ArchitecturalMaterialClosure.MOD_ID, path);
    }
}

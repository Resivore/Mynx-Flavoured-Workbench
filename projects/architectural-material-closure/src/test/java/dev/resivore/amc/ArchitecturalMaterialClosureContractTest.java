package dev.resivore.amc;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The C4 contract is a fixed audit, not a runtime registry-name inference. */
final class ArchitecturalMaterialClosureContractTest {
    @Test
    void c4HasOneAuthorityForEveryApprovedCell() {
        assertEquals(17, ArchitecturalMaterialClosure.MATERIALS.size());
        assertEquals(38, ArchitecturalMaterialClosure.FORMS.size());
        assertEquals(646, ArchitecturalMaterialClosure.MATERIALS.size() * ArchitecturalMaterialClosure.FORMS.size());
        assertEquals(237, ArchitecturalMaterialClosure.ownedIds().size());

        int providerOwned = 0;
        for (String material : ArchitecturalMaterialClosure.MATERIALS) {
            for (String form : ArchitecturalMaterialClosure.FORMS) {
                boolean owned = ArchitecturalMaterialClosure.ownedIds().stream()
                        .anyMatch(id -> id.getPath().equals(material + "_" + form));
                assertEquals(!ArchitecturalMaterialClosure.isProviderOwned(material, form), owned, material + "/" + form);
                if (!owned) providerOwned++;
            }
        }
        assertEquals(409, providerOwned);
        assertEquals(237, new HashSet<>(ArchitecturalMaterialClosure.ownedIds()).size());
    }
}

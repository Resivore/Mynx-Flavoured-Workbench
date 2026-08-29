package dev.resivore.slabdecorations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReferenceArtifactContractTest {
    private static final String NIBARU_SHA256 =
            "8DFB6E4F55ACF021118D022D99EC9A78CB2C74C13AD469BAB58A68734969C7B6";
    private static final String CNM_INTEGRATION_SHA256 =
            "04EB0E22E7F5D9F37EB9BA9E72C1FA3D767BDDED5BF7A7CAA4C060CAA7ACFCF2";
    private static final String CNM_UPSTREAM_SHA256 =
            "41A925E70D5E6E8C098BEA7DC88C44486AED46724E35CB2FA4B1622B2A4DBCCE";
    private static final String TERRAIN_SLABS_SHA256 =
            "C67C334C8EA0A6A47EFFA96B740568BF505F41E9484A82E06736819F26F78EDA";

    @Test
    void exactReferenceArtifactsAndRequiredCanonicalSeamsArePresent() throws Exception {
        Path nibaru = reference("nibaruReferenceJar");
        Path integration = reference("cnmIntegrationReferenceJar");
        Path upstream = reference("cnmUpstreamReferenceJar");
        Path terrainSlabs = reference("terrainSlabsReferenceJar");

        assertEquals(NIBARU_SHA256, sha256(nibaru), "accepted Nibaru artifact drifted");
        assertEquals(CNM_INTEGRATION_SHA256, sha256(integration), "accepted CNM integration artifact drifted");
        assertEquals(CNM_UPSTREAM_SHA256, sha256(upstream), "pristine CNM dependency drifted");
        assertEquals(TERRAIN_SLABS_SHA256, sha256(terrainSlabs), "pristine Terrain Slabs reference drifted");

        assertEntries(nibaru, List.of(
                "games/twinhead/moreslabsstairsandwalls/api/material/NibaruMaterialProfiles.class",
                "games/twinhead/moreslabsstairsandwalls/api/material/DerivedMaterialTraits.class",
                "games/twinhead/moreslabsstairsandwalls/mixin/PlantBlockMixin.class"
        ));
        assertEntries(integration, List.of(
                "dev/aero/cnmterraincompat/NibaruProviderAdapter.class"
        ));
        assertEntries(terrainSlabs, List.of(
                "net/countered/terrainslabs/mixin/ontop/place/VegetationBlockMixin.class",
                "net/countered/terrainslabs/mixin/ontop/render/MixinBlockStateBase.class",
                "net/countered/terrainslabs/mixin/ontop/render/target/MixinRaytrace.class",
                "net/countered/terrainslabs/fabric/model/SlabOffsetModel.class"
        ));
    }

    private static Path reference(String property) {
        String configured = System.getProperty(property);
        assertNotNull(configured, () -> "missing Gradle test property " + property);
        Path path = Path.of(configured);
        assertTrue(Files.isRegularFile(path), () -> "reference artifact is not a file: " + path);
        return path;
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read != 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }

    private static void assertEntries(Path jarPath, List<String> expected) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            for (String entry : expected) {
                assertNotNull(jar.getJarEntry(entry), () -> jarPath.getFileName() + " is missing " + entry);
            }
        }
    }
}

package dev.resivore.slabdecorations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private static final String BGE_SHA256 =
            "1A4E4D1CD9C8709720EC84975E70CAFFB5552AC676537B9BBAE42DCA96567E87";
    private static final String CNM_UPSTREAM_SHA256 =
            "41A925E70D5E6E8C098BEA7DC88C44486AED46724E35CB2FA4B1622B2A4DBCCE";
    private static final String TERRAIN_SLABS_SHA256 =
            "C67C334C8EA0A6A47EFFA96B740568BF505F41E9484A82E06736819F26F78EDA";

    @Test
    void exactReferenceArtifactsAndRequiredCanonicalSeamsArePresent() throws Exception {
        Path bge = reference("bgeReferenceJar");
        Path upstream = reference("cnmUpstreamReferenceJar");
        Path terrainSlabs = reference("terrainSlabsReferenceJar");

        assertEquals(BGE_SHA256, sha256(bge), "BGE C58 validation baseline drifted");
        assertEquals(CNM_UPSTREAM_SHA256, sha256(upstream), "pristine CNM dependency drifted");
        assertEquals(TERRAIN_SLABS_SHA256, sha256(terrainSlabs), "pristine Terrain Slabs reference drifted");

        assertEntries(bge, List.of(
                "games/twinhead/moreslabsstairsandwalls/api/material/NibaruMaterialProfiles.class",
                "games/twinhead/moreslabsstairsandwalls/api/material/DerivedMaterialTraits.class",
                "games/twinhead/moreslabsstairsandwalls/mixin/PlantBlockMixin.class",
                "dev/aero/cnmterraincompat/NibaruProviderAdapter.class"
        ));
        assertBgeProviderIdentity(bge);
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

    private static void assertBgeProviderIdentity(Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var metadataEntry = jar.getJarEntry("fabric.mod.json");
            assertNotNull(metadataEntry, "BGE C58 is missing its root fabric.mod.json");
            String metadata;
            try (InputStream input = jar.getInputStream(metadataEntry)) {
                metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertTrue(metadata.contains("\"id\": \"cnm_terrain_slabs_compat\""),
                    "BGE C58 primary provider identity drifted");
            assertTrue(metadata.contains("\"version\": \"4.2.2-bge.canary58.glass-corner-uv+26.2\""),
                    "BGE C58 embedded version drifted");
            assertTrue(metadata.contains("\"provides\": [\"more_slabs_stairs_and_walls\"]"),
                    "BGE C58 no longer provides the stable Nibaru identity");
        }
    }
}

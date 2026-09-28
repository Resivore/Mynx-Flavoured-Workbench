package dev.resivore.blockfamilies.cnm.artifact;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExactProviderArtifactIdentityTest {
    private static final String MINECRAFT_26_2_MERGED_SHA256 =
            "E29FDDD54A12FDBB7A0CCC899AD7CE6B5167F5CBCEF7C305EE3069ED7A4CC1EF";

    private static final Map<String, ArtifactContract> CONTRACTS = Map.ofEntries(
            Map.entry("cnmUpstreamReferenceJar", new ArtifactContract(
                    "41A925E70D5E6E8C098BEA7DC88C44486AED46724E35CB2FA4B1622B2A4DBCCE",
                    "clutternomore", "2.0.7+26.2")),
            Map.entry("macawsDoorsReferenceJar", new ArtifactContract(
                    "00E431D662489FE4A145F0E471837E1CD2BC9BCCE5ABE3F9F9941230765B2DED",
                    "mcwdoors", "1.1.5")),
            Map.entry("macawsPathsReferenceJar", new ArtifactContract(
                    "39128A12CB64FD61286B631714DA742D7B6EA0FA8D4573C103081D01E4993BC2",
                    "mcwpaths", "1.1.1")),
            Map.entry("aurorasLanternsReferenceJar", new ArtifactContract(
                    "E0F8FE41C5ADA5746DE8DB256ECA8D2D6854C08B35FB6C83FAD0EF3D66158A8A",
                    "auroraslanterns", "2.1.1+26.2")),
            Map.entry("ribbitsReferenceJar", new ArtifactContract(
                    "7024EA6FF0FD03DDCC686E18FF7D228B25766B9A46FC5B293DF8C71579D05387",
                    "ribbits", "4.1.6+26.2-mynx-canary17")),
            Map.entry("bbbReferenceJar", new ArtifactContract(
                    "57DDB5DFE62F2EB9F4A2CE22FBEEB5CCE4386BBD93AAB3F7DF0DD8E6D19DDAF0",
                    "bbb", "2.0pre4+26.2-enderscape-dev.7")),
            Map.entry("enderscapeReferenceJar", new ArtifactContract(
                    "9FCC4F59CA88E91F90E7C7D18289F2F859F20C810EEBCCA924764AA15236C40B",
                    "enderscape", "3.0.2")),
            Map.entry("lithostitchedReferenceJar", new ArtifactContract(
                    "A159EC68946521CA07D6D341C143033EE693BC32C34673C953B529E4AB9971D3",
                    "lithostitched", "1.7.13")),
            Map.entry("trimPatcherReferenceJar", new ArtifactContract(
                    "B5F4AAEB9C906522654D8B7FDDEBF0BF02099775C9F31D42F13C761598E0E9B0",
                    "trimpatcher", "2.1-mc26.2-fabric")),
            Map.entry("yaclReferenceJar", new ArtifactContract(
                    "829396C3B3E7D1801AE0E9E2921D0454C5A3078AFDB6C6DDA6B3D1819DFA0E3F",
                    "yet_another_config_lib_v3", "3.9.6+26.2-fabric")),
            Map.entry("macawsTrapdoorsReferenceJar", new ArtifactContract(
                    "6411CB0FF6C6CC4312DEED48C3CA69CDBCBAE72E80FF4ED0269F2D0A78ACD32D",
                    "mcwtrpdoors", "1.1.5")),
            Map.entry("macawsWindowsReferenceJar", new ArtifactContract(
                    "40BA9C55F191F8BD4758293683ED2BCF6D0C76C8F54F9FC8A0DCCABDBCFD36AD",
                    "mcwwindows", "2.4.2")),
            Map.entry("dramaticDoorsReferenceJar", new ArtifactContract(
                    "4E77603B3337EE2A2571900395B89273E7053440EA0C325223DA1556C1F14B34",
                    "dramaticdoors", "1.20.1-3.3.3+26.2-workbench-canary8")),
            Map.entry("nibaruReferenceJar", new ArtifactContract(
                    "0A979A75101076E987A35807F8EB293631FE5E664B252E5DB0AA263D4EEDF07F",
                    "more_slabs_stairs_and_walls", "4.2.0+26.2-port-canary43-native-directional-material-axis")),
            Map.entry("cnmIntegrationReferenceJar", new ArtifactContract(
                    "0E84FB7B8C69E31C3C22A592D0667DB66C2918C8FD9F461BD2C216D377722E69",
                    "cnm_terrain_slabs_compat", "0.5.49-nibaru-cnm-canary1.39-native-directional-material-axis")),
            Map.entry("qsnReferenceJar", new ArtifactContract(
                    "43F1130527F782A291231C682791B4FD3766A20916C691CBDB98F91FDCC47E53",
                    "quick-stack-nearby", "0.4.0")),
            Map.entry("qsnCompatReferenceJar", new ArtifactContract(
                    "C2F4AE3B02A5517AD184998C784C356D90132AEAEE91546C91D03A878BE6CE98",
                    "quick_stack_nearby_compat", "0.1.0-canary6"))
    );

    @Test
    void resolvedRuntimeReferencesMatchTheAuditedAndAcceptedBytes() throws Exception {
        for (Map.Entry<String, ArtifactContract> entry : CONTRACTS.entrySet()) {
            String property = entry.getKey();
            Path artifact = Path.of(requiredProperty(property));
            ArtifactContract contract = entry.getValue();

            assertTrue(Files.isRegularFile(artifact), property + " did not resolve to a file: " + artifact);
            assertEquals(contract.sha256(), sha256(artifact), property + " SHA-256 drifted");

            String metadata = zipText(artifact, "fabric.mod.json");
            assertTrue(metadata.contains("\"id\"") && metadata.contains("\"" + contract.modId() + "\""),
                    property + " has the wrong Fabric mod id");
            assertTrue(metadata.contains("\"version\"") && metadata.contains("\"" + contract.version() + "\""),
                    property + " has the wrong Fabric mod version");
        }
    }

    @Test
    void minecraftRegistryAuditUsesTheExactCurrent26_2MergedJar() throws Exception {
        Path artifact = Path.of(requiredProperty("minecraftReferenceJar"));
        assertTrue(Files.isRegularFile(artifact),
                "minecraftReferenceJar did not resolve to a file: " + artifact);
        assertEquals(MINECRAFT_26_2_MERGED_SHA256, sha256(artifact));

        String version = zipText(artifact, "version.json");
        assertTrue(version.contains("\"id\": \"26.2\"")
                        || version.contains("\"id\":\"26.2\""),
                "minecraftReferenceJar is not the Minecraft 26.2 registry input");
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new AssertionError("Missing Gradle-wired artifact property: " + name);
        }
        return value;
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }

    private static String zipText(Path path, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                throw new AssertionError(path + " does not contain " + entryName);
            }
            try (InputStream input = zip.getInputStream(entry)) {
                return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
    }

    private record ArtifactContract(String sha256, String modId, String version) {
    }
}

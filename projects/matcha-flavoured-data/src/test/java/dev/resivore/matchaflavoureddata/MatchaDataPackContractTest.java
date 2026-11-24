package dev.resivore.matchaflavoureddata;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

final class MatchaDataPackContractTest {
    @Test
    void jarContainsOneUnmodifiedPublisherPack() throws Exception {
        Path project = Path.of(System.getProperty("projectRoot"));
        Path archive = project.resolve("upstream/MF_1_12_5_26_2.zip");
        String prefix = "resourcepacks/matcha_flavoured_1_12_5/";
        try (ZipFile source = new ZipFile(archive.toFile());
             ZipFile jar = new ZipFile(System.getProperty("packagedJar"))) {
            Set<String> expected = new TreeSet<>();
            for (var member : source.stream().filter(e -> !e.isDirectory()).toList()) {
                String target = prefix + member.getName();
                expected.add(target);
                assertArrayEquals(entryBytes(source, member.getName()), entryBytes(jar, target), target);
            }
            assertEquals(expected, jar.stream().filter(e -> !e.isDirectory())
                    .map(ZipEntry::getName).filter(n -> n.startsWith("resourcepacks/"))
                    .collect(java.util.stream.Collectors.toSet()));
            assertFalse(jar.stream().anyMatch(e -> e.getName().startsWith("data/")));
        }
    }

    private static byte[] entryBytes(ZipFile archive, String name) throws IOException {
        ZipEntry entry = archive.getEntry(name);
        assertNotNull(entry, () -> "Missing archive entry " + name);
        try (InputStream input = archive.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

}

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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.impl.resource.ResourceLoaderImpl;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void fabricDiscoversOneRequiredServerPackAndRejectsLegacyWorldData(@TempDir Path worldPack) throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        new MatchaFlavouredData().onInitialize();
        List<Pack> packs = new ArrayList<>();
        ResourceLoaderImpl.registerBuiltinResourcePacks(PackType.SERVER_DATA, packs::add);
        var matches = packs.stream().filter(pack -> pack.getId().equals(
                MatchaFlavouredData.BUILTIN_PACK_ID.toString())).toList();
        assertEquals(1, matches.size());
        Pack provider = matches.getFirst();
        assertTrue(provider.isRequired());
        for (int reload = 0; reload < 2; reload++) {
            try (var manager = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(provider.open()))) {
                for (String path : List.of("matcha:function/setup/load.mcfunction",
                        "matcha:function/timers/tick.mcfunction", "minecraft:tags/function/tick.json")) {
                    assertEquals(provider.getId(), manager.getResource(Identifier.parse(path)).orElseThrow().sourcePackId());
                }
            }
        }
        var location = new PackLocationInfo("file/renamed.zip", Component.literal("world pack"), PackSource.DEFAULT,
                Optional.empty());
        Path hook = worldPack.resolve("data/main/function/setup/tick.mcfunction");
        Files.createDirectories(hook.getParent());
        Files.writeString(hook, "return 0");
        try (var current = provider.open(); var old = new PathPackResources(location, worldPack)) {
            assertThrows(IllegalStateException.class,
                    () -> new MultiPackResourceManager(PackType.SERVER_DATA, List.of(old, current)));
            assertThrows(IllegalStateException.class,
                    () -> new MultiPackResourceManager(PackType.SERVER_DATA, List.of(current, old)));
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

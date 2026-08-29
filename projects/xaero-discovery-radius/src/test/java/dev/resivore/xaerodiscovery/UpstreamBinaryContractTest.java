package dev.resivore.xaerodiscovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class UpstreamBinaryContractTest {
    private static final String MINIMAP_SHA256 =
            "69284892D2EB853C9AEFA85A4C9B74232C322DA00207994C67AB8AEED8A64048";
    private static final String WORLD_MAP_SHA256 =
            "D55EF45C559AE0ADCF66D894C022F61D9D921629B0C885D04AA00424546A2389";
    private static final String XAERO_LIB_SHA256 =
            "7F4A78DD7E046FEA0500FEF83B1481D85317C8348D47E947035D7A07EFE51065";

    private static final String MINIMAP_WRITE_TILE =
            "(Lxaero/common/minimap/MinimapProcessor;DDDLnet/minecraft/client/multiplayer/ClientLevel;"
                    + "Lxaero/common/minimap/region/MinimapChunk;Lxaero/common/minimap/region/MinimapChunk;"
                    + "Lxaero/common/minimap/region/MinimapChunk;Lxaero/common/minimap/region/MinimapChunk;"
                    + "Lxaero/common/minimap/region/MinimapChunk;IIIIZZ)Z";
    private static final String LIVE_CHUNK_GET =
            "(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)"
                    + "Lnet/minecraft/world/level/chunk/ChunkAccess;";
    private static final String WORLD_SAVE_BUILD_TILE =
            "(Lnet/minecraft/nbt/CompoundTag;Lxaero/map/region/MapTile;Lxaero/map/region/MapTileChunk;"
                    + "IIIIIIZZLnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/HolderLookup;"
                    + "Lnet/minecraft/core/Registry;Lnet/minecraft/core/Registry;Lnet/minecraft/core/Registry;ZII)Z";
    private static final String LEAF_CACHE_READ =
            "(IILjava/io/DataInputStream;[B[BLxaero/map/region/LeveledRegion;"
                    + "Lxaero/map/MapProcessor;IIZ)V";

    @Test
    void exactXaeroInputsAndEmbeddedLibraryArePinned() throws Exception {
        Path minimap = Path.of(System.getProperty("xaeroMinimapJar"));
        Path worldMap = Path.of(System.getProperty("xaeroWorldMapJar"));
        assertEquals(MINIMAP_SHA256, sha256(Files.readAllBytes(minimap)));
        assertEquals(WORLD_MAP_SHA256, sha256(Files.readAllBytes(worldMap)));

        try (ZipFile zip = new ZipFile(minimap.toFile())) {
            byte[] xaeroLib = entryBytes(zip, "META-INF/jars/xaerolib-fabric-26.2-1.7.1.jar");
            assertEquals(XAERO_LIB_SHA256, sha256(xaeroLib));
        }

        assertFabricIdentity(minimap, "xaerominimap", "26.4.2");
        assertFabricIdentity(worldMap, "xaeroworldmap", "1.44.2");
    }

    @Test
    void minimapTerrainFetchContractIsUniqueAndNullCompatible() throws Exception {
        ClassNode writer = readClass(Path.of(System.getProperty("xaeroMinimapJar")),
                "xaero/common/minimap/write/MinimapWriter");
        MethodNode writeTile = exactMethod(writer, "writeTile", MINIMAP_WRITE_TILE);
        assertEquals(1, invocationCount(writeTile, invocation ->
                invocation.owner.equals("net/minecraft/client/multiplayer/ClientLevel")
                        && invocation.name.equals("getChunk")
                        && invocation.desc.equals(LIVE_CHUNK_GET)));
        assertEquals(1, invocationCount(writeTile, invocation ->
                invocation.owner.equals("net/minecraft/client/multiplayer/ClientLevel")
                        && invocation.name.equals("getChunk")
                        && invocation.desc.equals("(II)Lnet/minecraft/world/level/chunk/LevelChunk;")));
        assertEquals(1, invocationCount(writeTile, invocation -> invocation.name.equals("loadBlockColor")));
        assertEquals(2, invocationCount(writeTile, invocation ->
                invocation.owner.equals("xaero/common/minimap/region/MinimapChunk")
                        && invocation.name.equals("setTile")));
    }

    @Test
    void worldMapWriteDistanceAndSingleplayerDiskGateContractsArePinned() throws Exception {
        Path worldMapJar = Path.of(System.getProperty("xaeroWorldMapJar"));
        ClassNode mapWriter = readClass(worldMapJar, "xaero/map/MapWriter");
        MethodNode getWriteDistance = exactMethod(mapWriter, "getWriteDistance", "()I");
        assertTrue((getWriteDistance.access & Opcodes.ACC_PRIVATE) != 0);
        assertEquals(1, opcodeCount(getWriteDistance, Opcodes.IRETURN));

        MethodNode dirty = exactMethod(
                mapWriter,
                "setDirtyInWriteDistance",
                "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;)V"
        );
        assertEquals(1, invocationCount(dirty, invocation ->
                invocation.owner.equals("xaero/map/MapWriter")
                        && invocation.name.equals("getWriteDistance")
                        && invocation.desc.equals("()I")));
        assertEquals(1, mapWriter.methods.stream()
                .filter(method -> method.name.equals("writeMap"))
                .mapToLong(method -> invocationCount(method, invocation ->
                        invocation.owner.equals("xaero/map/MapWriter")
                                && invocation.name.equals("getWriteDistance")
                                && invocation.desc.equals("()I")))
                .sum());

        ClassNode reader = readClass(worldMapJar, "xaero/map/file/worldsave/WorldDataReader");
        MethodNode buildTile = exactMethod(reader, "buildTile", WORLD_SAVE_BUILD_TILE);
        assertTrue((buildTile.access & Opcodes.ACC_PRIVATE) != 0);
        assertTrue(opcodeCount(buildTile, Opcodes.IRETURN) > 0);
    }

    @Test
    void legacyCachePredicateAndLoadBeforeRebuildOrderingArePinned() throws Exception {
        Path worldMapJar = Path.of(System.getProperty("xaeroWorldMapJar"));
        ClassNode leaf = readClass(worldMapJar, "xaero/map/region/texture/LeafRegionTexture");
        exactMethod(leaf, "readCacheData", LEAF_CACHE_READ);
        exactMethod(leaf, "getTileChunk", "()Lxaero/map/region/MapTileChunk;");
        MethodNode highlights = exactMethod(
                leaf,
                "applyHighlights",
                "(Lxaero/map/highlight/DimensionHighlighterHandler;Lxaero/map/pool/buffer/PoolTextureDirectBufferUnit;Z)"
                        + "Lxaero/map/pool/buffer/PoolTextureDirectBufferUnit;"
        );
        assertEquals(1, invocationCount(highlights, invocation ->
                invocation.name.equals("getHeight") && invocation.desc.equals("(II)I")));
        assertTrue(highlights.instructions.iterator().hasNext());
        assertTrue(containsSipush(highlights, Short.MAX_VALUE));

        ClassNode saveLoad = readClass(worldMapJar, "xaero/map/file/MapSaveLoad");
        MethodNode run = exactMethod(
                saveLoad,
                "run",
                "(Lnet/minecraft/core/HolderLookup;Lnet/minecraft/core/Registry;Lnet/minecraft/core/Registry;"
                        + "Lxaero/map/biome/BiomeGetter;Lnet/minecraft/core/Registry;)V"
        );
        int cacheLoadIndex = firstInvocationIndex(run, invocation -> invocation.name.equals("loadCacheTextures"));
        int regionLoadIndex = firstInvocationIndex(run, invocation ->
                invocation.owner.equals("xaero/map/file/MapSaveLoad") && invocation.name.equals("loadRegion"));
        assertTrue(cacheLoadIndex >= 0);
        assertTrue(regionLoadIndex > cacheLoadIndex);
    }

    private static void assertFabricIdentity(Path jar, String expectedId, String expectedVersion) throws Exception {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            JsonObject metadata = JsonParser.parseString(
                    new String(entryBytes(zip, "fabric.mod.json"), StandardCharsets.UTF_8)
            ).getAsJsonObject();
            assertEquals(expectedId, metadata.get("id").getAsString());
            assertEquals(expectedVersion, metadata.get("version").getAsString());
            assertEquals("All Rights Reserved", metadata.get("license").getAsString());
        }
    }

    private static ClassNode readClass(Path jar, String internalName) throws Exception {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            byte[] bytes = entryBytes(zip, internalName + ".class");
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            return node;
        }
    }

    private static byte[] entryBytes(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        assertNotNull(entry, "Missing JAR entry " + name);
        try (InputStream input = zip.getInputStream(entry); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private static MethodNode exactMethod(ClassNode owner, String name, String descriptor) {
        List<MethodNode> matches = owner.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .toList();
        assertEquals(1, matches.size(), owner.name + "." + name + descriptor);
        return matches.getFirst();
    }

    private static long invocationCount(MethodNode method, Predicate<MethodInsnNode> predicate) {
        long count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode invocation && predicate.test(invocation)) {
                count++;
            }
        }
        return count;
    }

    private static long opcodeCount(MethodNode method, int opcode) {
        long count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() == opcode) {
                count++;
            }
        }
        return count;
    }

    private static boolean containsSipush(MethodNode method, int operand) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof IntInsnNode intInstruction
                    && intInstruction.getOpcode() == Opcodes.SIPUSH
                    && intInstruction.operand == operand) {
                return true;
            }
        }
        return false;
    }

    private static int firstInvocationIndex(MethodNode method, Predicate<MethodInsnNode> predicate) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode invocation && predicate.test(invocation)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}

package com.yungnickyoung.minecraft.ribbits.world.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.SwampHutPiece;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwampHutPhaseCContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path PRODUCTION_MINECRAFT_JAR =
            Path.of(System.getProperty("productionMinecraftJar"));
    private static final String STRUCTURE_PIECE =
            "net/minecraft/world/level/levelgen/structure/StructurePiece";
    private static final String SCATTERED_FEATURE_PIECE =
            "net/minecraft/world/level/levelgen/structure/ScatteredFeaturePiece";
    private static final String SWAMP_HUT_PIECE =
            "net/minecraft/world/level/levelgen/structure/structures/SwampHutPiece";
    private static final String POST_PROCESS_DESC = "(Lnet/minecraft/world/level/WorldGenLevel;"
            + "Lnet/minecraft/world/level/StructureManager;"
            + "Lnet/minecraft/world/level/chunk/ChunkGenerator;"
            + "Lnet/minecraft/util/RandomSource;"
            + "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"
            + "Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/core/BlockPos;)V";
    private static final String SPAWN_CAT_DESC = "(Lnet/minecraft/world/level/ServerLevelAccessor;"
            + "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)V";
    private static final String NATURAL_SPAWN_DESC = "(Lnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/world/entity/MobCategory;"
            + "Lnet/minecraft/world/level/StructureManager;"
            + "Lnet/minecraft/world/level/chunk/ChunkGenerator;"
            + "Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;"
            + "Lnet/minecraft/core/BlockPos$MutableBlockPos;D)Z";

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void localLayoutAndVanillaTransformsAreExactInAllFourOrientations() {
        assertEquals(new BlockPos(3, 2, 5), SwampHutPhaseC.SORCERER_LOCAL);
        assertEquals(new BlockPos(2, 2, 4), SwampHutPhaseC.CAT_LOCAL);
        assertEquals(new BlockPos(2, 2, 6), SwampHutPhaseC.BARREL_LOCAL);

        InspectableSwampHutPiece piece = new InspectableSwampHutPiece();
        BlockState northFacingBarrel = Blocks.BARREL.defaultBlockState()
                .setValue(BarrelBlock.FACING, Direction.NORTH)
                .setValue(BarrelBlock.OPEN, false);

        for (Direction orientation : Direction.Plane.HORIZONTAL) {
            piece.setOrientation(orientation);
            BoundingBox box = piece.getBoundingBox();
            BlockPos sorcerer = piece.world(SwampHutPhaseC.SORCERER_LOCAL);
            BlockPos cat = piece.world(SwampHutPhaseC.CAT_LOCAL);
            BlockPos barrel = piece.world(SwampHutPhaseC.BARREL_LOCAL);
            BlockPos crafting = piece.world(SwampHutPhaseC.CRAFTING_TABLE_LOCAL);
            BlockPos cauldron = piece.world(SwampHutPhaseC.CAULDRON_LOCAL);

            assertEquals(expectedWorld(box, orientation, SwampHutPhaseC.SORCERER_LOCAL), sorcerer);
            assertEquals(expectedWorld(box, orientation, SwampHutPhaseC.CAT_LOCAL), cat);
            assertEquals(expectedWorld(box, orientation, SwampHutPhaseC.BARREL_LOCAL), barrel);
            assertEquals(expectedWorld(box, orientation, SwampHutPhaseC.CRAFTING_TABLE_LOCAL), crafting);
            assertEquals(expectedWorld(box, orientation, SwampHutPhaseC.CAULDRON_LOCAL), cauldron);
            assertEquals(1, manhattan(barrel, crafting),
                    "barrel remains directly beside the crafting table for " + orientation);
            assertEquals(1, manhattan(crafting, cauldron));
            assertEquals(1, manhattan(sorcerer, crafting));
            assertEquals(2, manhattan(cat, sorcerer));
            assertEquals(5, Set.of(sorcerer, cat, barrel, crafting, cauldron).size());

            BlockState transformed = northFacingBarrel
                    .mirror(piece.getMirror()).rotate(piece.getRotation());
            assertEquals(orientation, transformed.getValue(BarrelBlock.FACING));
            assertFalse(transformed.getValue(BarrelBlock.OPEN));
        }
    }

    @Test
    void exactPieceMembershipIncludesEveryFaceAndEmptyUpperLayerButNotSupportColumns() {
        BoundingBox box = new BoundingBox(
                SwampHutPhaseC.PIECE_MIN_X,
                SwampHutPhaseC.PIECE_MIN_Y,
                SwampHutPhaseC.PIECE_MIN_Z,
                SwampHutPhaseC.PIECE_MAX_X,
                SwampHutPhaseC.PIECE_MAX_Y,
                SwampHutPhaseC.PIECE_MAX_Z);

        List<BlockPos> inside = List.of(
                new BlockPos(0, 0, 0), new BlockPos(6, 6, 8),
                new BlockPos(0, 3, 4), new BlockPos(6, 3, 4),
                new BlockPos(3, 0, 4), new BlockPos(3, 6, 4),
                new BlockPos(3, 3, 0), new BlockPos(3, 3, 8),
                new BlockPos(0, 6, 0), new BlockPos(6, 6, 8));
        inside.forEach(pos -> assertTrue(SwampHutPhaseC.contains(box, pos), pos.toString()));

        List<BlockPos> outside = List.of(
                new BlockPos(-1, 3, 4), new BlockPos(7, 3, 4),
                new BlockPos(3, -1, 4), new BlockPos(3, 7, 4),
                new BlockPos(3, 3, -1), new BlockPos(3, 3, 9),
                new BlockPos(1, -1, 2), new BlockPos(5, -1, 2),
                new BlockPos(1, -1, 7), new BlockPos(5, -1, 7));
        outside.forEach(pos -> assertFalse(SwampHutPhaseC.contains(box, pos), pos.toString()));
    }

    @Test
    void exactPiecePredicateFailsClosedForInvalidMetadata() {
        InspectableSwampHutPiece piece = new InspectableSwampHutPiece();
        StructureStart start = new StructureStart(null, net.minecraft.world.level.ChunkPos.ZERO, 0,
                new PiecesContainer(List.of(piece)));
        assertTrue(SwampHutPhaseC.containsExactPiece(start, piece.getBoundingBox().getCenter()));
        assertFalse(SwampHutPhaseC.containsExactPiece(start,
                piece.getBoundingBox().getCenter().below(piece.getBoundingBox().getYSpan())));
        assertFalse(SwampHutPhaseC.containsExactPiece(StructureStart.INVALID_START,
                piece.getBoundingBox().getCenter()));
    }

    @Test
    void mixinsUseOnlyTheTwoAuditedServerPathsAndFailLoudTargets() throws IOException {
        JsonObject config = JsonParser.parseString(read(
                "common/src/main/resources/ribbits.mixins.json")).getAsJsonObject();
        JsonArray mixins = config.getAsJsonArray("mixins");
        assertTrue(mixins.asList().stream().anyMatch(value ->
                value.getAsString().equals("accessor.StructurePieceInvoker")));
        assertTrue(mixins.asList().stream().anyMatch(value ->
                value.getAsString().equals("world.SwampHutPieceMixin")));
        assertTrue(mixins.asList().stream().anyMatch(value ->
                value.getAsString().equals("world.NaturalSpawnerMixin")));
        assertEquals(1, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        assertFalse(config.has("refmap"));

        String hut = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/"
                + "mixin/mixins/world/SwampHutPieceMixin.java");
        assertEquals(4, occurrences(hut, "require = 1"));
        assertEquals(4, occurrences(hut, "allow = 1"));
        assertEquals(2, occurrences(hut, "getWorldPos(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;"));
        assertEquals(2, occurrences(hut, "ribbits$invokeGetWorldPos("));
        assertEquals(1, occurrences(hut, "ribbits$invokePlaceBlock("));
        assertFalse(hut.contains("@Shadow"));
        assertTrue(hut.contains("EntityType;create("));
        assertTrue(hut.contains("entityType != EntityTypes.WITCH"));
        assertTrue(hut.contains("spawnReason != EntitySpawnReason.STRUCTURE"));
        assertTrue(hut.contains("return null;"));
        assertTrue(hut.contains("ribbit.finalizeSpawn(generationLevel"));
        assertTrue(hut.contains("RibbitProfessionModule.SORCERER"));
        assertTrue(hut.contains("RibbitUmbrellaTypeModule.UMBRELLA_1"));
        assertTrue(hut.contains("RibbitInstrumentModule.NONE"));
        assertTrue(hut.contains("ribbit.reassessGoals();"));
        assertTrue(hut.contains("ribbit.setPersistenceRequired();"));
        assertTrue(hut.contains("if (!generationLevel.addFreshEntity(ribbit))"));
        assertTrue(hut.contains("if (ribbit == null)"));
        assertTrue(hut.contains("shift = At.Shift.BEFORE"));
        assertTrue(hut.contains("level.getBlockState(pos).is(Blocks.BARREL)"));
        assertTrue(hut.contains("barrel.setLootTable(MAP_BARREL_LOOT_TABLE);"));
        assertFalse(hut.contains("net.minecraft.client"));

        String invoker = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/"
                + "mixin/mixins/accessor/StructurePieceInvoker.java");
        assertTrue(invoker.contains("@Mixin(StructurePiece.class)"));
        assertTrue(invoker.contains("@Invoker(\"getWorldPos\")"));
        assertTrue(invoker.contains("@Invoker(\"placeBlock\")"));
        assertFalse(invoker.contains("java.lang.reflect"));

        String natural = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/"
                + "mixin/mixins/world/NaturalSpawnerMixin.java");
        assertEquals(1, occurrences(natural, "require = 1"));
        assertEquals(1, occurrences(natural, "allow = 1"));
        assertTrue(natural.contains("isValidSpawnPostitionForType("));
        assertTrue(natural.contains("at = @At(\"HEAD\")"));
        assertTrue(natural.contains("cancellable = true"));
        assertTrue(natural.contains("spawnerData.type() != EntityTypes.WITCH"));
        assertTrue(natural.contains("getValueOrThrow(SWAMP_HUT)"));
        assertTrue(natural.contains("getStructureWithPieceAt(pos, swampHut)"));
        assertTrue(natural.contains("SwampHutPhaseC.containsExactPiece(start, pos)"));
        assertFalse(natural.contains("net.minecraft.client"));
        assertFalse(natural.contains("StructureTags"));
    }

    @Test
    void auditedMinecraftBytecodeResolvesEveryExactMixinSelectorOnce() throws IOException {
        byte[] swampHutBytes = productionClassBytes(SWAMP_HUT_PIECE);
        ClassNode swampHutNode = classNode(swampHutBytes);
        ClassNode scatteredFeatureNode = classNode(productionClassBytes(SCATTERED_FEATURE_PIECE));
        assertEquals(SCATTERED_FEATURE_PIECE, swampHutNode.superName);
        assertEquals(STRUCTURE_PIECE, scatteredFeatureNode.superName);

        MethodNode postProcess = uniqueMethod(
                swampHutBytes, SWAMP_HUT_PIECE, "postProcess", POST_PROCESS_DESC);
        MethodNode spawnCat = uniqueMethod(
                swampHutBytes, SWAMP_HUT_PIECE, "spawnCat", SPAWN_CAT_DESC);
        uniqueMethod(productionClassBytes("net/minecraft/world/level/NaturalSpawner"),
                "net/minecraft/world/level/NaturalSpawner",
                "isValidSpawnPostitionForType", NATURAL_SPAWN_DESC);

        String worldPosDesc = "(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;";
        MethodNode worldPos = uniqueMethod(productionClassBytes(STRUCTURE_PIECE),
                STRUCTURE_PIECE, "getWorldPos", worldPosDesc);
        assertTrue((worldPos.access & Opcodes.ACC_PROTECTED) != 0);
        assertEquals(0, methods(productionClassBytes(SCATTERED_FEATURE_PIECE),
                "getWorldPos", worldPosDesc).size());
        assertEquals(0, methods(swampHutBytes, "getWorldPos", worldPosDesc).size());

        String placeBlockDesc = "(Lnet/minecraft/world/level/WorldGenLevel;"
                + "Lnet/minecraft/world/level/block/state/BlockState;III"
                + "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)V";
        MethodNode placeBlock = uniqueMethod(productionClassBytes(STRUCTURE_PIECE),
                STRUCTURE_PIECE, "placeBlock", placeBlockDesc);
        assertTrue((placeBlock.access & Opcodes.ACC_PROTECTED) != 0);
        assertEquals(0, methods(swampHutBytes, "placeBlock", placeBlockDesc).size());

        String createDesc = "(Lnet/minecraft/world/level/Level;"
                + "Lnet/minecraft/world/entity/EntitySpawnReason;)"
                + "Lnet/minecraft/world/entity/Entity;";
        List<MethodInsnNode> postProcessWorldPos = invocations(postProcess, SWAMP_HUT_PIECE,
                "getWorldPos", worldPosDesc);
        List<MethodInsnNode> spawnCatWorldPos = invocations(spawnCat, SWAMP_HUT_PIECE,
                "getWorldPos", worldPosDesc);
        assertEquals(1, postProcessWorldPos.size(), "initial Witch coordinate selector");
        assertEquals(Opcodes.INVOKEVIRTUAL, postProcessWorldPos.getFirst().getOpcode());
        assertEquals(1, invocations(postProcess, "net/minecraft/world/entity/EntityType",
                "create", createDesc).size(), "initial Witch creation selector");
        assertEquals(1, invocations(postProcess, SWAMP_HUT_PIECE,
                "spawnCat", SPAWN_CAT_DESC).size(), "barrel-before-Cat selector");
        assertEquals(1, spawnCatWorldPos.size(), "initial Cat coordinate selector");
        assertEquals(Opcodes.INVOKEVIRTUAL, spawnCatWorldPos.getFirst().getOpcode());
        assertEquals(1, invocations(spawnCat, "net/minecraft/world/entity/EntityType",
                "create", createDesc).size(), "vanilla Cat creation remains present and untargeted");

        List<FieldInsnNode> witchFlagWrites = fieldInstructions(
                postProcess, Opcodes.PUTFIELD, SWAMP_HUT_PIECE, "spawnedWitch", "Z");
        List<FieldInsnNode> catFlagWrites = fieldInstructions(
                spawnCat, Opcodes.PUTFIELD, SWAMP_HUT_PIECE, "spawnedCat", "Z");
        assertEquals(1, witchFlagWrites.size(), "vanilla persistent Witch one-shot write");
        assertEquals(1, catFlagWrites.size(), "independent vanilla persistent Cat one-shot write");

        int witchPosition = instructionIndex(postProcess,
                postProcessWorldPos.getFirst());
        int witchCreate = instructionIndex(postProcess,
                invocations(postProcess, "net/minecraft/world/entity/EntityType",
                        "create", createDesc).getFirst());
        int catCall = instructionIndex(postProcess,
                invocations(postProcess, SWAMP_HUT_PIECE, "spawnCat", SPAWN_CAT_DESC).getFirst());
        int witchFlagWrite = instructionIndex(postProcess, witchFlagWrites.getFirst());
        int catPosition = instructionIndex(spawnCat,
                spawnCatWorldPos.getFirst());
        int catFlagWrite = instructionIndex(spawnCat, catFlagWrites.getFirst());
        int catCreate = instructionIndex(spawnCat,
                invocations(spawnCat, "net/minecraft/world/entity/EntityType",
                        "create", createDesc).getFirst());
        assertTrue(witchPosition < witchFlagWrite && witchFlagWrite < witchCreate
                        && witchCreate < catCall,
                "the Witch flag is consumed before the redirected creation, then Cat remains later");
        assertTrue(catPosition < catFlagWrite && catFlagWrite < catCreate,
                "the independent Cat flag is consumed before vanilla Cat creation");
    }

    @Test
    void productionTargetClassesAndStructureDataRetainExactFingerprints()
            throws IOException, NoSuchAlgorithmException {
        assertEquals("548dcf9db4e4655403d987fb62db903e797a11601ae1ded402f864cba4ecd130",
                sha256(productionClassBytes(SWAMP_HUT_PIECE)));
        assertEquals("0f4052a7e66860f2fc3c57d7848a22cfa771d71f4cf9fc3218092578cc44c808",
                sha256(productionClassBytes(
                        "net/minecraft/world/level/levelgen/structure/structures/SwampHutStructure")));
        assertEquals("b548ce5367d49e87cfa95b81486a8483e38baf42beb5941c5adf8936c953b69c",
                sha256(productionClassBytes("net/minecraft/world/level/StructureManager")));
        assertEquals("b32ab0c0fcb21ebc2515345231ea1607eb4bd815d07156b55c9a080ffd394663",
                sha256(productionClassBytes(STRUCTURE_PIECE)));
        assertEquals("749a8a33d3897ccc5cf9e60fc2b32e0f435e6d01df5be62375a1965dbaa22329",
                sha256(productionClassBytes("net/minecraft/world/level/NaturalSpawner")));
        assertEquals("f3446923999ca537bdb60469d5a1ef6e1656d30ca9d345a664ee696411a17586",
                sha256(resourceBytes("/data/minecraft/worldgen/structure/swamp_hut.json")));
    }

    private static BlockPos expectedWorld(BoundingBox box, Direction orientation, BlockPos local) {
        int x = switch (orientation) {
            case NORTH, SOUTH -> box.minX() + local.getX();
            case WEST -> box.maxX() - local.getZ();
            case EAST -> box.minX() + local.getZ();
            default -> throw new IllegalArgumentException("not horizontal");
        };
        int z = switch (orientation) {
            case NORTH -> box.maxZ() - local.getZ();
            case SOUTH -> box.minZ() + local.getZ();
            case WEST, EAST -> box.minZ() + local.getX();
            default -> throw new IllegalArgumentException("not horizontal");
        };
        return new BlockPos(x, box.minY() + local.getY(), z);
    }

    private static int manhattan(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX())
                + Math.abs(first.getY() - second.getY())
                + Math.abs(first.getZ() - second.getZ());
    }

    private static String read(String relative) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relative)).replace("\r\n", "\n");
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int from = 0;
        while ((from = value.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static MethodNode uniqueMethod(byte[] ownerBytes, String owner, String name,
                                           String descriptor) {
        List<MethodNode> matches = methods(ownerBytes, name, descriptor);
        assertEquals(1, matches.size(), owner + "." + name + descriptor);
        return matches.getFirst();
    }

    private static List<MethodNode> methods(byte[] ownerBytes, String name, String descriptor) {
        return classNode(ownerBytes).methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .toList();
    }

    private static ClassNode classNode(byte[] ownerBytes) {
        ClassNode node = new ClassNode();
        new ClassReader(ownerBytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static List<MethodInsnNode> invocations(MethodNode method, String owner,
                                                     String name, String descriptor) {
        List<MethodInsnNode> matches = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)
                    && call.desc.equals(descriptor)) {
                matches.add(call);
            }
        }
        return matches;
    }

    private static List<FieldInsnNode> fieldInstructions(MethodNode method, int opcode,
                                                          String owner, String name,
                                                          String descriptor) {
        List<FieldInsnNode> matches = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == opcode
                    && field.owner.equals(owner)
                    && field.name.equals(name)
                    && field.desc.equals(descriptor)) {
                matches.add(field);
            }
        }
        return matches;
    }

    private static int instructionIndex(MethodNode method, AbstractInsnNode target) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction == target) {
                return index;
            }
            index++;
        }
        throw new IllegalArgumentException("instruction does not belong to method");
    }

    private static byte[] productionClassBytes(String internalName) throws IOException {
        try (JarFile archive = new JarFile(PRODUCTION_MINECRAFT_JAR.toFile())) {
            var entry = archive.getJarEntry(internalName + ".class");
            if (entry == null) {
                throw new IOException("missing production Minecraft class " + internalName);
            }
            try (InputStream input = archive.getInputStream(entry)) {
                return input.readAllBytes();
            }
        }
    }

    private static byte[] resourceBytes(String path) throws IOException {
        try (InputStream input = SwampHutPhaseCContractTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("missing classpath resource " + path);
            }
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] value) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static final class InspectableSwampHutPiece extends SwampHutPiece {
        private InspectableSwampHutPiece() {
            super(RandomSource.create(0x2A650718L), 100, 200);
        }

        private BlockPos world(BlockPos local) {
            return this.getWorldPos(local.getX(), local.getY(), local.getZ()).immutable();
        }
    }
}

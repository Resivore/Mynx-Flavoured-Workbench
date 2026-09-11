package dev.resivore.inventoryparticlesmatchacompat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Binds every C4 injection to the exact audited Inventory Particles 2.6.0 JAR. */
final class InventoryParticlesSeamContractTest {
    private static final String PARTICLE_SPAWNER = "net/lopymine/ip/element/mod/spawner/ParticleSpawner";
    private static final String PARTICLE_HOLDER = "net/lopymine/ip/config/particle/ParticleHolder";
    private static final String RENDERER = "net/lopymine/ip/renderer/InventoryParticlesRenderer";
    private static final String CONFIG_MANAGER = "net/lopymine/ip/resourcepack/manager/ParticlesConfigsManager";
    private static final String SPAWNER_INTERFACE = "net/lopymine/ip/element/mod/spawner/IParticleSpawner";
    private static final String SPAWNER_CONTEXT = "net/lopymine/ip/element/mod/spawner/context/ParticleSpawnContext";
    private static final String SHA256 = "b44d808e673805eea4949591abcb3325faa1abf8b9321266d06533ea5a34c802";

    @Test
    void exactAuditedJarExposesOnlyTheC4SpawningSeams() throws Exception {
        Path jar = Path.of(System.getProperty("inventoryParticlesReferenceJar"));
        assertEquals(1_783_277L, Files.size(jar));
        assertEquals(SHA256, sha256(jar));

        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ClassNode holder = classNode(zip, PARTICLE_HOLDER);
            assertMethod(holder, "createSpawner",
                    "(Ljava/util/function/Function;)Lnet/lopymine/ip/element/mod/spawner/ParticleSpawner;");

            ClassNode manager = classNode(zip, CONFIG_MANAGER);
            assertMethod(manager, "reload", "()V");

            ClassNode spawner = classNode(zip, PARTICLE_SPAWNER);
            assertMethod(spawner, "tickAndSpawn",
                    "(Lnet/lopymine/ip/element/mod/spawner/context/ParticleSpawnContext;)Ljava/util/List;");
            assertMethod(spawner, "spawn",
                    "(Lnet/lopymine/ip/element/mod/spawner/context/ParticleSpawnContext;)Ljava/util/List;");
            assertMethod(spawner, "spawnFromCursor",
                    "(Lnet/lopymine/ip/element/mod/InventoryCursor;)Ljava/util/List;");

            ClassNode renderer = classNode(zip, RENDERER);
            assertRoute(renderer, "spawnHoveredSlotParticles", "(II)V", "tickAndSpawn");
            assertRoute(renderer, "spawnAllSlotsParticles",
                    "(Lnet/minecraft/world/inventory/AbstractContainerMenu;II)V", "tickAndSpawn");
            assertRoute(renderer, "spawnCursorParticles", "()V", "tickAndSpawn", "spawnFromCursor");
            assertRoute(renderer, "lambda$onGuiAction$0",
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;II)V", "spawn");
        }
    }

    @Test
    void packagedMixinContractIsClientOnlyAndFailsSafeWhenTheAuditedSeamDrifts() throws IOException {
        Path root = Path.of(System.getProperty("projectRoot"));
        String mixins = Files.readString(root.resolve("src/main/resources/inventory_particles_matcha_compat.mixins.json"));
        assertTrue(mixins.contains("\"required\": false"));
        assertTrue(mixins.contains("\"defaultRequire\": 0"));
        for (String mixin : List.of(
                "ParticleHolderCompatMarkerMixin",
                "ParticleSpawnerExclusiveDispatchMixin",
                "ParticlesConfigsManagerReloadMixin")) {
            assertTrue(mixins.contains(mixin));
        }

        String dispatch = Files.readString(root.resolve(
                "src/main/java/dev/resivore/inventoryparticlesmatchacompat/mixin/ParticleSpawnerExclusiveDispatchMixin.java"));
        for (String method : List.of("tickAndSpawn", "spawn", "spawnFromCursor")) {
            assertTrue(dispatch.contains(method));
        }
        assertTrue(dispatch.contains("context.getStack()"));
        assertTrue(dispatch.contains("cursor.getCurrentStack()"));
    }

    @Test
    void c4ResourcesContainOnlyTheSixNamedExclusiveDefinitions() throws IOException {
        Path resources = Path.of(System.getProperty("projectRoot"))
                .resolve("src/main/resources/assets/inventory_particles/iparticles/mynx");
        List<String> names = List.of(
                "mynx_ipmc_green_curry", "mynx_ipmc_ramen", "mynx_ipmc_crystal_heart",
                "mynx_ipmc_glowcap", "mynx_ipmc_toadstool_heart", "mynx_ipmc_ribbit_village_map");
        String all = Files.list(resources)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .map(path -> {
                    try {
                        return Files.readString(path, StandardCharsets.UTF_8);
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                })
                .reduce("", String::concat);
        for (String name : names) {
            assertEquals(1, occurrences(all, name), name);
        }
        assertTrue(all.contains("minecraft:green_curry"));
        assertTrue(all.contains("minecraft:ramen"));
        assertTrue(all.contains("minecraft:heart_container"));
        assertTrue(all.contains("ribbits:ribbit_village_explorer_map"));
    }

    private static void assertRoute(ClassNode renderer, String name, String descriptor, String... spawnerMethods) {
        MethodNode method = assertMethod(renderer, name, descriptor);
        assertTrue(method.instructions.iterator().hasNext(), name + " has no bytecode");
        assertTrue(methodCalls(method, CONFIG_MANAGER, "getSpawnersForItem"),
                name + " no longer selects Inventory Particles spawners");
        for (String spawnerMethod : spawnerMethods) {
            assertTrue(methodCalls(method, SPAWNER_INTERFACE, spawnerMethod),
                    name + " no longer reaches IParticleSpawner." + spawnerMethod);
        }
    }

    private static boolean methodCalls(MethodNode method, String owner, String name) {
        for (var instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (instruction.getOpcode() == Opcodes.INVOKEINTERFACE || instruction.getOpcode() == Opcodes.INVOKESTATIC
                    || instruction.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (owner.equals(call.owner) && name.equals(call.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static MethodNode assertMethod(ClassNode node, String name, String descriptor) {
        MethodNode result = node.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst().orElse(null);
        assertNotNull(result, () -> node.name + " is missing " + name + descriptor);
        return result;
    }

    private static ClassNode classNode(ZipFile zip, String internalName) throws IOException {
        var entry = zip.getEntry(internalName + ".class");
        assertNotNull(entry, () -> "Missing " + internalName + ".class");
        ClassNode node = new ClassNode();
        try (var input = zip.getInputStream(entry)) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private static String sha256(Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static int occurrences(String source, String value) {
        return source.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}

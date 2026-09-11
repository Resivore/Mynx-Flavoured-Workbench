package dev.resivore.inventorysortercsrcompat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fails closed if the audited 3.0.0 snapshot/layout/writeback or fallback-planner seam changes. */
class InventorySorterArtifactContractTest {
    private static final String SHA256 = "935100251E9AA5BA3F279DC5AC02F4426F568838986C9EA86CD1B198393A8708";
    private static final long SIZE = 3_777_884L;

    @Test
    void exactInventorySorterArtifactAndBothMaskedSeamsRemainPresent() throws Exception {
        Path jar = Path.of(System.getProperty("inventorySorterReferenceJar"));
        assertEquals(SIZE, Files.size(jar));
        assertEquals(SHA256, HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));

        try (JarFile archive = new JarFile(jar.toFile())) {
            String metadata = new String(archive.getInputStream(archive.getEntry("fabric.mod.json")).readAllBytes());
            assertTrue(metadata.contains("\"id\": \"inventorysorter\"")
                    && metadata.contains("\"version\": \"3.0.0\""));

            MethodNode serverSort = method(archive, "net/kyrptonaught/inventorysorter/inventory/ContainerInventorySorter.class",
                    "sort", "(Lnet/minecraft/world/Container;IILnet/kyrptonaught/inventorysorter/sort/SortType;Ljava/lang/String;Ljava/util/List;Z)V");
            assertTrue(hasCall(serverSort, "net/kyrptonaught/inventorysorter/inventory/container/ContainerStacks", "get")
                    && hasCall(serverSort, "net/kyrptonaught/inventorysorter/sort/SortedInventoryLayout", "from")
                    && hasCall(serverSort, "net/kyrptonaught/inventorysorter/inventory/container/ContainerStacks", "set"));

            MethodNode playerSort = method(archive,
                    "net/kyrptonaught/inventorysorter/inventory/PlayerInventorySorter.class",
                    "sort", "(Lnet/minecraft/server/level/ServerPlayer;Lnet/kyrptonaught/inventorysorter/network/SortSettings;Ljava/lang/String;)V");
            assertTrue(hasCall(playerSort, "net/kyrptonaught/inventorysorter/sort/bundle/BundleInsertionLayoutPass", "apply")
                    && hasCall(playerSort, "net/kyrptonaught/inventorysorter/inventory/ContainerInventorySorter", "sort"));

            MethodNode fallback = method(archive,
                    "net/kyrptonaught/inventorysorter/client/sort/plan/ClientFallbackSortPlanBuilder.class",
                    "build", "(Lnet/kyrptonaught/inventorysorter/client/sort/ClientSortScope;Lnet/kyrptonaught/inventorysorter/sort/SortType;Ljava/lang/String;Ljava/util/List;ZZ)Ljava/util/Optional;");
            assertTrue(hasCall(fallback, "net/kyrptonaught/inventorysorter/client/sort/ClientSortScope", "slots")
                    && hasCall(fallback, "net/kyrptonaught/inventorysorter/client/sort/plan/ClientSortClickPlanner", "plan"));
        }
    }

    @Test
    void c2FixedSlotAndBundleBypassBoundariesStayNarrow() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        String fixedSlots = Files.readString(root.resolve(
                "src/main/java/dev/resivore/inventorysortercsrcompat/core/FixedSortSlots.java"));
        String playerMixin = Files.readString(root.resolve(
                "src/main/java/dev/resivore/inventorysortercsrcompat/mixin/PlayerInventorySorterMixin.java"));
        String clientFallback = Files.readString(root.resolve(
                "src/main/java/dev/resivore/inventorysortercsrcompat/core/MaskedClientFallbackSort.java"));

        assertTrue(fixedSlots.contains("return ContainerSlotReservationsApi.isReserved(container, localSlot);")
                && !fixedSlots.contains("ShulkerBoxBlock")
                && !fixedSlots.contains("||"));
        assertTrue(playerMixin.contains("FixedSortSlots.isBundle(inventory.getItem(slot))")
                && playerMixin.contains("settings.sortPriorityRules(), false)"));
        assertTrue(clientFallback.contains("if (!hasFixedSlot && !hasBundle) return null;")
                && clientFallback.contains("SortedInventoryLayout.from("));
    }

    private static MethodNode method(JarFile archive, String entry, String name, String descriptor) throws Exception {
        ClassNode node = new ClassNode();
        try (var input = archive.getInputStream(archive.getEntry(entry))) {
            new ClassReader(input).accept(node, 0);
        }
        MethodNode result = node.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst().orElse(null);
        assertNotNull(result, () -> "Missing audited method " + name + descriptor);
        return result;
    }

    private static boolean hasCall(MethodNode method, String owner, String name) {
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner) && call.name.equals(name)) return true;
        }
        return false;
    }
}

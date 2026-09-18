package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins the exact upstream cursor-held storage gesture that Canary 25 interposes. */
final class EasyShulkerBoxesReferenceContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String OUTER_FILE =
            "EasyShulkerBoxes-v26.2.3-mc26.2.x-Fabric.jar";
    private static final long OUTER_SIZE = 639_115L;
    private static final String OUTER_SHA256 =
            "66f803ae8796a0cdd7086e175fe8b96846515710b8b98b9aebc79d52ea2b0c38";
    private static final String NESTED_PATH =
            "META-INF/jars/iteminteractions-fabric-26.2.2.jar";
    private static final int NESTED_SIZE = 196_177;
    private static final String NESTED_SHA256 =
            "1745adf294134817986e46bf554a5a6613837602b660e15a15c50bdc2e0a293c";

    private static final String CLIENT =
            "fuzs/iteminteractions/common/impl/client/ItemInteractionsClient";
    private static final String HANDLER =
            "fuzs/iteminteractions/common/impl/client/handler/ItemSlotMouseActionHandler";
    private static final String ACTIONS =
            "fuzs/iteminteractions/common/impl/client/gui/ItemStorageMouseActions";
    private static final String CUSTOM_ACTION =
            "fuzs/iteminteractions/common/impl/client/gui/CustomItemSlotMouseAction";
    private static final String SCREEN =
            "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen";
    private static final String SCREEN_EVENTS =
            "fuzs/puzzleslib/common/api/client/event/v1/gui/ScreenMouseEvents";
    private static final String EVENT_INVOKER =
            "fuzs/puzzleslib/common/api/event/v1/core/EventInvoker";
    private static final String EVENT_PHASE =
            "fuzs/puzzleslib/common/api/event/v1/core/EventPhase";
    private static final String EVENT_RESULT =
            "fuzs/puzzleslib/common/api/event/v1/core/EventResult";
    private static final String SLOT = "net/minecraft/world/inventory/Slot";
    private static final String MENU = "net/minecraft/world/inventory/AbstractContainerMenu";
    private static final String CLICK_ACTION = "net/minecraft/world/inventory/ClickAction";

    private static final String HANDLER_CLICK_DESCRIPTOR =
            "(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;"
                    + "Lnet/minecraft/client/input/MouseButtonEvent;)"
                    + "Lfuzs/puzzleslib/common/api/event/v1/core/EventResult;";
    private static final String ACTION_CLICK_DESCRIPTOR =
            "(Lnet/minecraft/client/input/MouseButtonEvent;Ljava/util/OptionalInt;"
                    + "Lnet/minecraft/world/item/ItemStack;)Z";
    private static final String ACTION_DRAG_DESCRIPTOR =
            "(Lnet/minecraft/client/input/MouseButtonEvent;DDLjava/util/OptionalInt;"
                    + "Lnet/minecraft/world/item/ItemStack;)Z";

    @Test
    void exactEasyShulkerBoxesJarEmbedsTheExactItemInteractionsRuntime() throws Exception {
        Path outer = exactOuterJar();
        assertTrue(Files.isRegularFile(outer), "Missing exact Easy Shulker Boxes reference");
        assertEquals(OUTER_SIZE, Files.size(outer));
        assertEquals(OUTER_SHA256, sha256(Files.readAllBytes(outer)));

        try (JarFile jar = new JarFile(outer.toFile())) {
            String metadata = text(bytes(jar, "fabric.mod.json"));
            assertEquals("easyshulkerboxes", jsonString(metadata, "id"));
            assertEquals("26.2.3", jsonString(metadata, "version"));
            assertTrue(metadata.contains("\"iteminteractions\""));
            assertTrue(metadata.contains("\"file\": \"" + NESTED_PATH + "\""));

            byte[] nested = bytes(jar, NESTED_PATH);
            assertEquals(NESTED_SIZE, nested.length);
            assertEquals(NESTED_SHA256, sha256(nested));
            Map<String, byte[]> entries = entries(nested);
            String nestedMetadata = text(required(entries, "fabric.mod.json"));
            assertEquals("iteminteractions", jsonString(nestedMetadata, "id"));
            assertEquals("26.2.2", jsonString(nestedMetadata, "version"));

            String definition = text(bytes(
                    jar,
                    "data/easyshulkerboxes/datapacks/shulker_boxes/data/minecraft/"
                            + "iteminteractions/item_storage/shulker_box.json"
            ));
            assertTrue(definition.contains("\"type\": \"iteminteractions:container\""));
            assertTrue(definition.contains("\"inventory_height\": 3"));
            assertTrue(definition.contains("\"inventory_width\": 9"));
            assertTrue(definition.contains("\"supported_items\": \"minecraft:shulker_box\""));
        }
    }

    @Test
    void itemInteractionsClaimsTheInitialPressFromPuzzlesBeforeMouseClick() throws Exception {
        Map<String, byte[]> jar = nestedEntries();
        MethodCode registration = code(jar, CLIENT, "registerEventHandlers", "()V");
        assertOrdered(
                registration,
                call(SCREEN_EVENTS, "beforeMouseClick"),
                field(Opcodes.GETSTATIC, EVENT_PHASE, "BEFORE"),
                dynamicHandle(HANDLER, "onBeforeMouseClicked", HANDLER_CLICK_DESCRIPTOR),
                call(EVENT_INVOKER, "register",
                        "(Lfuzs/puzzleslib/common/api/event/v1/core/EventPhase;Ljava/lang/Object;)V")
        );

        MethodCode handler = code(jar, HANDLER, "onBeforeMouseClicked", HANDLER_CLICK_DESCRIPTOR);
        assertOrdered(
                handler,
                call(MENU, "getCarried"),
                call(CUSTOM_ACTION, "matches", "(Lnet/minecraft/world/item/ItemStack;)Z"),
                call(CUSTOM_ACTION, "onMouseClicked", ACTION_CLICK_DESCRIPTOR),
                field(Opcodes.GETSTATIC, EVENT_RESULT, "INTERRUPT")
        );
    }

    @Test
    void splitInputArmsPrimaryAndSecondaryWithoutMovingTheOriginOnPress() throws Exception {
        Map<String, byte[]> jar = nestedEntries();
        MethodCode click = code(jar, ACTIONS, "onMouseClicked", ACTION_CLICK_DESCRIPTOR);
        assertOrdered(
                click,
                call(ACTIONS, "clearDraggingSlots", "()V"),
                call(SCREEN, "getHoveredSlot", "(DD)Lnet/minecraft/world/inventory/Slot;"),
                call(ACTIONS, "getClickActionFromScheme"),
                field(Opcodes.PUTFIELD, ACTIONS, "clickAction")
        );
        assertFalse(click.calls().stream().anyMatch(value ->
                        value.owner().equals(SCREEN) && value.name().equals("slotClicked")),
                "The reference press only arms its drag; source transfer begins on drag");

        MethodCode buttons = code(
                jar,
                ACTIONS,
                "getClickActionFromButtonNum",
                "(I)Lnet/minecraft/world/inventory/ClickAction;"
        );
        assertTrue(buttons.events().contains(new LookupSwitch(List.of(0, 1))));
        assertOrdered(
                buttons,
                field(Opcodes.GETSTATIC, CLICK_ACTION, "PRIMARY"),
                field(Opcodes.GETSTATIC, CLICK_ACTION, "SECONDARY")
        );

        MethodCode secondary = code(
                jar,
                ACTIONS,
                "matchesSecondaryClickAction",
                "(Lnet/minecraft/world/inventory/Slot;"
                        + "Lfuzs/iteminteractions/common/api/v2/world/item/storage/ItemStorageHolder;"
                        + "Lnet/minecraft/world/item/ItemStack;"
                        + "Lnet/minecraft/world/entity/player/Player;)Z"
        );
        assertOrdered(
                secondary,
                call(SLOT, "hasItem", "()Z"),
                jump(Opcodes.IFNE),
                call("net/minecraft/world/Container", "isEmpty", "()Z")
        );
    }

    @Test
    void dragUsesActualSlotsVisitsEachIdentityOnceAndClearsOnlyOnRelease() throws Exception {
        Map<String, byte[]> jar = nestedEntries();
        MethodCode constructor = code(
                jar,
                ACTIONS,
                "<init>",
                "(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;)V"
        );
        assertEquals(2L, constructor.calls().stream().filter(value ->
                value.owner().equals("com/google/common/collect/Sets")
                        && value.name().equals("newIdentityHashSet")).count());

        MethodCode drag = code(jar, ACTIONS, "onMouseDragged", ACTION_DRAG_DESCRIPTOR);
        assertOrdered(
                drag,
                call(SCREEN, "getHoveredSlot", "(DD)Lnet/minecraft/world/inventory/Slot;"),
                call(MENU, "canDragTo", "(Lnet/minecraft/world/inventory/Slot;)Z"),
                field(Opcodes.GETFIELD, ACTIONS, "allDraggingSlots"),
                call("java/util/Set", "contains", "(Ljava/lang/Object;)Z"),
                call(ACTIONS, "shouldSlotBeClicked"),
                field(Opcodes.GETFIELD, ACTIONS, "clickedDraggingSlots"),
                call("java/util/Set", "add", "(Ljava/lang/Object;)Z"),
                call(SCREEN, "slotClicked",
                        "(Lnet/minecraft/world/inventory/Slot;II"
                                + "Lnet/minecraft/world/inventory/ContainerInput;)V"),
                field(Opcodes.GETFIELD, ACTIONS, "allDraggingSlots"),
                call("java/util/Set", "add", "(Ljava/lang/Object;)Z")
        );
        assertEquals(1L, drag.calls().stream().filter(value ->
                value.owner().equals(SCREEN) && value.name().equals("slotClicked")).count());

        int hovered = indexOf(drag.events(), call(
                SCREEN,
                "getHoveredSlot",
                "(DD)Lnet/minecraft/world/inventory/Slot;"
        ), 0);
        assertFalse(drag.events().subList(hovered, drag.events().size()).stream().anyMatch(
                        call(ACTIONS, "clearDraggingSlots", "()V")),
                "Leaving the slot surface does not clear identities already visited in the hold");

        MethodCode release = code(jar, ACTIONS, "onMouseReleased", ACTION_CLICK_DESCRIPTOR);
        assertTrue(release.calls().stream().anyMatch(
                call(ACTIONS, "clearDraggingSlots", "()V")));
    }

    private static Path exactOuterJar() throws IOException {
        Path repository = ROOT.resolve("../..").normalize();
        Path gitMarker = repository.resolve(".git");
        if (Files.isRegularFile(gitMarker)) {
            String marker = Files.readString(gitMarker).trim();
            assertTrue(marker.startsWith("gitdir: "), "Malformed worktree .git marker");
            Path gitDirectory = Path.of(marker.substring("gitdir: ".length()));
            if (!gitDirectory.isAbsolute()) gitDirectory = repository.resolve(gitDirectory).normalize();
            Path commonMarker = gitDirectory.resolve("commondir");
            Path commonDirectory = Files.isRegularFile(commonMarker)
                    ? gitDirectory.resolve(Files.readString(commonMarker).trim()).normalize()
                    : gitDirectory;
            repository = commonDirectory.getParent();
        }
        return repository.resolve("originals/mods").resolve(OUTER_FILE);
    }

    private static Map<String, byte[]> nestedEntries() throws Exception {
        try (JarFile outer = new JarFile(exactOuterJar().toFile())) {
            return entries(bytes(outer, NESTED_PATH));
        }
    }

    private static Map<String, byte[]> entries(byte[] archive) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (JarInputStream input = new JarInputStream(new ByteArrayInputStream(archive))) {
            JarEntry entry;
            while ((entry = input.getNextJarEntry()) != null) {
                if (!entry.isDirectory()) result.put(entry.getName(), input.readAllBytes());
            }
        }
        return Map.copyOf(result);
    }

    private static byte[] bytes(JarFile jar, String name) throws IOException {
        JarEntry entry = jar.getJarEntry(name);
        assertNotNull(entry, "Missing archive entry: " + name);
        try (var input = jar.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static byte[] required(Map<String, byte[]> entries, String name) {
        byte[] result = entries.get(name);
        assertNotNull(result, "Missing nested archive entry: " + name);
        return result;
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String jsonString(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        assertTrue(matcher.find(), "Missing JSON string: " + key);
        return matcher.group(1);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes)
        );
    }

    private static MethodCode code(Map<String, byte[]> entries, String owner,
                                   String methodName, String descriptor) {
        MethodCode result = new MethodCode();
        new ClassReader(required(entries, owner + ".class")).accept(
                new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String actualDescriptor,
                                                     String signature, String[] exceptions) {
                        if (!methodName.equals(name) || !descriptor.equals(actualDescriptor)) return null;
                        result.found = true;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String callOwner, String callName,
                                                        String callDescriptor, boolean isInterface) {
                                MethodCall call = new MethodCall(
                                        opcode, callOwner, callName, callDescriptor
                                );
                                result.calls.add(call);
                                result.events.add(call);
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String fieldOwner, String fieldName,
                                                       String fieldDescriptor) {
                                result.events.add(new FieldAccess(
                                        opcode, fieldOwner, fieldName, fieldDescriptor
                                ));
                            }

                            @Override
                            public void visitInvokeDynamicInsn(String name, String dynamicDescriptor,
                                                               Handle bootstrapMethodHandle,
                                                               Object... bootstrapMethodArguments) {
                                List<HandleReference> handles = Arrays.stream(bootstrapMethodArguments)
                                        .filter(Handle.class::isInstance)
                                        .map(Handle.class::cast)
                                        .map(value -> new HandleReference(
                                                value.getOwner(), value.getName(), value.getDesc()
                                        ))
                                        .toList();
                                result.events.add(new DynamicCall(name, dynamicDescriptor, handles));
                            }

                            @Override
                            public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                                result.events.add(new LookupSwitch(Arrays.stream(keys).boxed().toList()));
                            }

                            @Override
                            public void visitJumpInsn(int opcode, Label label) {
                                result.events.add(new Jump(opcode));
                            }
                        };
                    }
                },
                ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
        );
        assertTrue(result.found, "Missing method " + owner + "." + methodName + descriptor);
        return result;
    }

    @SafeVarargs
    private static void assertOrdered(MethodCode code, Predicate<Object>... expected) {
        int matched = 0;
        for (Object event : code.events()) {
            if (matched < expected.length && expected[matched].test(event)) matched++;
        }
        assertEquals(expected.length, matched,
                "Missing ordered bytecode contract at predicate " + matched);
    }

    private static int indexOf(List<Object> events, Predicate<Object> predicate, int start) {
        for (int index = start; index < events.size(); index++) {
            if (predicate.test(events.get(index))) return index;
        }
        throw new AssertionError("Missing bytecode event");
    }

    private static Predicate<Object> call(String owner, String name) {
        return event -> event instanceof MethodCall value
                && value.owner().equals(owner) && value.name().equals(name);
    }

    private static Predicate<Object> call(String owner, String name, String descriptor) {
        return event -> event instanceof MethodCall value
                && value.owner().equals(owner) && value.name().equals(name)
                && value.descriptor().equals(descriptor);
    }

    private static Predicate<Object> field(int opcode, String owner, String name) {
        return event -> event instanceof FieldAccess value
                && value.opcode() == opcode && value.owner().equals(owner)
                && value.name().equals(name);
    }

    private static Predicate<Object> dynamicHandle(String owner, String name, String descriptor) {
        return event -> event instanceof DynamicCall value
                && value.handles().contains(new HandleReference(owner, name, descriptor));
    }

    private static Predicate<Object> jump(int opcode) {
        return event -> event.equals(new Jump(opcode));
    }

    private record MethodCall(int opcode, String owner, String name, String descriptor) {
    }

    private record FieldAccess(int opcode, String owner, String name, String descriptor) {
    }

    private record HandleReference(String owner, String name, String descriptor) {
    }

    private record DynamicCall(String name, String descriptor, List<HandleReference> handles) {
    }

    private record LookupSwitch(List<Integer> keys) {
    }

    private record Jump(int opcode) {
    }

    private static final class MethodCode {
        private boolean found;
        private final List<Object> events = new ArrayList<>();
        private final List<MethodCall> calls = new ArrayList<>();

        private List<Object> events() {
            return List.copyOf(events);
        }

        private List<MethodCall> calls() {
            return List.copyOf(calls);
        }
    }
}

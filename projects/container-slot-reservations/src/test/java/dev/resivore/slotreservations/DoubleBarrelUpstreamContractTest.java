package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DoubleBarrelUpstreamContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path ARTIFACT = ROOT.resolve(
            "../double-barrels/artifacts/doublebarrels-fabric-26.2-1.0.1+26.2-canary2.jar"
    ).normalize();
    private static final String SHA256 =
            "b2119e05c508880c7977551091515f0076dd3cdc2733e51340ac813925b553c0";

    private static final String ACCESS = "com/mozko/doublebarrels/DoubleBarrelAccess";
    private static final String WRAPPER = "com/mozko/doublebarrels/DoubleBarrelInventory";
    private static final String BARREL_MIXIN =
            "com/mozko/doublebarrels/mixin/BarrelBlockEntityMixin";
    private static final String NON_NULL_LIST = "net/minecraft/core/NonNullList";
    private static final String CONTAINER = "net/minecraft/world/Container";
    private static final String ITEM_STACK = "net/minecraft/world/item/ItemStack";
    private static final String INVENTORY = "net/minecraft/world/entity/player/Inventory";
    private static final String CHEST_MENU = "net/minecraft/world/inventory/ChestMenu";
    private static final String SLOT = "net/minecraft/world/inventory/Slot";
    private static final String CALLBACK_RETURNABLE =
            "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";

    private static final String WRAPPER_CONSTRUCTOR =
            "(Lnet/minecraft/core/NonNullList;Lnet/minecraft/core/NonNullList;"
                    + "Ljava/lang/Runnable;Ljava/util/function/Consumer;"
                    + "Ljava/util/function/Consumer;)V";
    private static final String CREATE_MENU_HANDLER =
            "(ILnet/minecraft/world/entity/player/Inventory;"
                    + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";

    private static final Set<String> EXPECTED_CLASSES = Set.of(
            "com/mozko/doublebarrels/DoubleBarrelAccess.class",
            "com/mozko/doublebarrels/DoubleBarrelInventory.class",
            "com/mozko/doublebarrels/DoubleBarrelProperties.class",
            "com/mozko/doublebarrels/DoubleBarrelsMod.class",
            "com/mozko/doublebarrels/DoubleBarrelType.class",
            "com/mozko/doublebarrels/mixin/BarrelBlockEntityMixin.class",
            "com/mozko/doublebarrels/mixin/BarrelBlockMixin.class",
            "com/mozko/doublebarrels/mixin/BaseContainerBlockEntityMixin.class",
            "com/mozko/doublebarrels/mixin/BlockEntityMixin.class",
            "com/mozko/doublebarrels/mixin/BlockItemMixin.class",
            "com/mozko/doublebarrels/mixin/RandomizableContainerBlockEntityMixin.class"
    );

    @Test
    void retainedCanaryIsTheExactAuditedDoubleBarrelsBuild() throws IOException {
        assertTrue(Files.isRegularFile(ARTIFACT), "Missing retained Double Barrels artifact");
        assertEquals(35_682L, Files.size(ARTIFACT));
        assertEquals(SHA256, sha256(Files.readAllBytes(ARTIFACT)));

        try (ZipFile jar = new ZipFile(ARTIFACT.toFile())) {
            String metadata = text(jar, "fabric.mod.json");
            assertEquals("doublebarrels", jsonString(metadata, "id"));
            assertEquals("1.0.1+26.2-canary2", jsonString(metadata, "version"));
            assertEquals("com.mozko.doublebarrels.DoubleBarrelsMod",
                    jsonArrayValue(objectBody(metadata, "entrypoints"), "main"));
            assertEquals("doublebarrels.mixins.json", jsonArrayValue(metadata, "mixins"));
            assertEquals("~26.2", jsonString(objectBody(metadata, "depends"), "minecraft"));

            Set<String> classes = new TreeSet<>();
            jar.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith("com/mozko/doublebarrels/"))
                    .filter(name -> name.endsWith(".class"))
                    .forEach(classes::add);
            assertEquals(new TreeSet<>(EXPECTED_CLASSES), classes);
        }
    }

    @Test
    void accessAndWrapperPinTheAuditedPhysicalListContract() throws IOException {
        try (ZipFile jar = new ZipFile(ARTIFACT.toFile())) {
            ClassShape access = shape(bytes(jar, ACCESS + ".class"));
            assertTrue((access.access & Opcodes.ACC_INTERFACE) != 0);
            assertMember(access, "isConnected", "()Z", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT);
            assertMember(access, "isMainBarrel", "()Z", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT);
            assertMember(access, "getConnectionPos", "()Lnet/minecraft/core/BlockPos;",
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT);
            assertMember(access, "getCombinedInventory", "()Lnet/minecraft/world/Container;",
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT);
            assertMember(access, "doublebarrels$getItems", "()Lnet/minecraft/core/NonNullList;",
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT);

            byte[] wrapperBytes = bytes(jar, WRAPPER + ".class");
            ClassShape wrapper = shape(wrapperBytes);
            assertTrue((wrapper.access & Opcodes.ACC_FINAL) != 0);
            assertEquals(List.of(CONTAINER), wrapper.interfaces);
            assertMember(wrapper, "first", "Lnet/minecraft/core/NonNullList;",
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);
            assertMember(wrapper, "second", "Lnet/minecraft/core/NonNullList;",
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);
            assertMember(wrapper, "firstSize", "I", Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);
            assertMember(wrapper, "totalSize", "I", Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);
            assertMember(wrapper, "<init>", WRAPPER_CONSTRUCTOR, Opcodes.ACC_PUBLIC);
            assertMember(wrapper, "getList", "(I)Lnet/minecraft/core/NonNullList;",
                    Opcodes.ACC_PRIVATE);

            MethodCode constructor = code(wrapperBytes, "<init>", WRAPPER_CONSTRUCTOR);
            assertSubsequence(constructor.events,
                    variable(Opcodes.ALOAD, 0), variable(Opcodes.ALOAD, 1),
                    field(Opcodes.PUTFIELD, WRAPPER, "first", "Lnet/minecraft/core/NonNullList;"),
                    variable(Opcodes.ALOAD, 0), variable(Opcodes.ALOAD, 2),
                    field(Opcodes.PUTFIELD, WRAPPER, "second", "Lnet/minecraft/core/NonNullList;"),
                    variable(Opcodes.ALOAD, 0), variable(Opcodes.ALOAD, 1),
                    call(Opcodes.INVOKEVIRTUAL, NON_NULL_LIST, "size", "()I"),
                    field(Opcodes.PUTFIELD, WRAPPER, "firstSize", "I"),
                    variable(Opcodes.ALOAD, 0), variable(Opcodes.ALOAD, 0),
                    field(Opcodes.GETFIELD, WRAPPER, "firstSize", "I"),
                    variable(Opcodes.ALOAD, 2),
                    call(Opcodes.INVOKEVIRTUAL, NON_NULL_LIST, "size", "()I"),
                    instruction(Opcodes.IADD),
                    field(Opcodes.PUTFIELD, WRAPPER, "totalSize", "I"),
                    variable(Opcodes.ALOAD, 0), variable(Opcodes.ALOAD, 3),
                    field(Opcodes.PUTFIELD, WRAPPER, "onChanged", "Ljava/lang/Runnable;"),
                    variable(Opcodes.ALOAD, 0), variable(Opcodes.ALOAD, 4),
                    field(Opcodes.PUTFIELD, WRAPPER, "onOpen", "Ljava/util/function/Consumer;"),
                    variable(Opcodes.ALOAD, 0), variable(Opcodes.ALOAD, 5),
                    field(Opcodes.PUTFIELD, WRAPPER, "onClose", "Ljava/util/function/Consumer;")
            );

            MethodCode selector = code(wrapperBytes, "getList", "(I)Lnet/minecraft/core/NonNullList;");
            assertSubsequence(selector.events,
                    field(Opcodes.GETFIELD, WRAPPER, "firstSize", "I"),
                    field(Opcodes.GETFIELD, WRAPPER, "first", "Lnet/minecraft/core/NonNullList;"),
                    field(Opcodes.GETFIELD, WRAPPER, "second", "Lnet/minecraft/core/NonNullList;")
            );
            MethodCode getItem = code(wrapperBytes, "getItem", "(I)Lnet/minecraft/world/item/ItemStack;");
            assertSubsequence(getItem.events,
                    call(Opcodes.INVOKEVIRTUAL, WRAPPER, "getList",
                            "(I)Lnet/minecraft/core/NonNullList;"),
                    field(Opcodes.GETFIELD, WRAPPER, "firstSize", "I"),
                    instruction(Opcodes.IREM),
                    call(Opcodes.INVOKEVIRTUAL, NON_NULL_LIST, "get", "(I)Ljava/lang/Object;")
            );
        }
    }

    @Test
    void combinedInventoryAlwaysUsesMainThenFollowerAndFollowerDelegatesToMain()
            throws IOException {
        try (ZipFile jar = new ZipFile(ARTIFACT.toFile())) {
            byte[] mixinBytes = bytes(jar, BARREL_MIXIN + ".class");
            ClassShape mixin = shape(mixinBytes);
            assertEquals(List.of(ACCESS), mixin.interfaces);
            assertMember(mixin, "getCombinedInventory", "()Lnet/minecraft/world/Container;",
                    Opcodes.ACC_PUBLIC);

            MethodCode combined = code(
                    mixinBytes,
                    "getCombinedInventory",
                    "()Lnet/minecraft/world/Container;"
            );
            assertSubsequence(combined.events,
                    call(Opcodes.INVOKEVIRTUAL, BARREL_MIXIN, "doublebarrels$getPartner",
                            "()Lnet/minecraft/world/level/block/entity/BarrelBlockEntity;"),
                    type(Opcodes.CHECKCAST, ACCESS),
                    call(Opcodes.INVOKEINTERFACE, ACCESS, "doublebarrels$getItems",
                            "()Lnet/minecraft/core/NonNullList;"),
                    field(Opcodes.GETFIELD, BARREL_MIXIN, "doublebarrels$isMain", "Z"),
                    type(Opcodes.NEW, WRAPPER),
                    variable(Opcodes.ALOAD, 0),
                    field(Opcodes.GETFIELD, BARREL_MIXIN, "items",
                            "Lnet/minecraft/core/NonNullList;"),
                    variable(Opcodes.ALOAD, 2),
                    call(Opcodes.INVOKESPECIAL, WRAPPER, "<init>", WRAPPER_CONSTRUCTOR),
                    type(Opcodes.CHECKCAST, ACCESS),
                    call(Opcodes.INVOKEINTERFACE, ACCESS, "getCombinedInventory",
                            "()Lnet/minecraft/world/Container;")
            );
            assertEquals(1, combined.count(type(Opcodes.NEW, WRAPPER)));
            assertEquals(1, combined.count(call(
                    Opcodes.INVOKEINTERFACE,
                    ACCESS,
                    "getCombinedInventory",
                    "()Lnet/minecraft/world/Container;"
            )));
        }
    }

    @Test
    void doubleBarrelMenuUsesTheVanillaSixRowChestLayout() throws IOException {
        try (ZipFile jar = new ZipFile(ARTIFACT.toFile())) {
            byte[] mixinBytes = bytes(jar, BARREL_MIXIN + ".class");
            assertMember(shape(mixinBytes), "doublebarrels$createMenu", CREATE_MENU_HANDLER,
                    Opcodes.ACC_PRIVATE);
            MethodCode createMenu = code(
                    mixinBytes,
                    "doublebarrels$createMenu",
                    CREATE_MENU_HANDLER
            );
            assertSubsequence(createMenu.events,
                    call(Opcodes.INVOKEVIRTUAL, BARREL_MIXIN, "getCombinedInventory",
                            "()Lnet/minecraft/world/Container;"),
                    call(Opcodes.INVOKESTATIC, CHEST_MENU, "sixRows",
                            "(ILnet/minecraft/world/entity/player/Inventory;"
                                    + "Lnet/minecraft/world/Container;)"
                                    + "Lnet/minecraft/world/inventory/ChestMenu;"),
                    call(Opcodes.INVOKEVIRTUAL, CALLBACK_RETURNABLE, "setReturnValue",
                            "(Ljava/lang/Object;)V")
            );
        }

        byte[] chestMenuBytes = classpathEntry(CHEST_MENU + ".class");
        String sixRows = "(ILnet/minecraft/world/entity/player/Inventory;"
                + "Lnet/minecraft/world/Container;)Lnet/minecraft/world/inventory/ChestMenu;";
        String constructor = "(Lnet/minecraft/world/inventory/MenuType;I"
                + "Lnet/minecraft/world/entity/player/Inventory;"
                + "Lnet/minecraft/world/Container;I)V";
        assertMember(shape(chestMenuBytes), "sixRows", sixRows,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        assertMember(shape(chestMenuBytes), "<init>", constructor, Opcodes.ACC_PUBLIC);

        MethodCode factory = code(chestMenuBytes, "sixRows", sixRows);
        assertSubsequence(factory.events,
                type(Opcodes.NEW, CHEST_MENU),
                field(Opcodes.GETSTATIC, "net/minecraft/world/inventory/MenuType", "GENERIC_9x6",
                        "Lnet/minecraft/world/inventory/MenuType;"),
                integer(Opcodes.BIPUSH, 6),
                call(Opcodes.INVOKESPECIAL, CHEST_MENU, "<init>", constructor)
        );

        MethodCode grid = code(
                chestMenuBytes,
                "addChestGrid",
                "(Lnet/minecraft/world/Container;II)V"
        );
        assertEquals(1, grid.count(type(Opcodes.NEW, SLOT)));
        assertTrue(grid.events.contains(call(
                Opcodes.INVOKESPECIAL,
                SLOT,
                "<init>",
                "(Lnet/minecraft/world/Container;III)V"
        )));
    }

    @Test
    void csrBridgeIsOptionalStringLinkedAndHooksTheAuditedReturnSeam() throws IOException {
        byte[] bridge = classpathEntry(
                "dev/resivore/slotreservations/DoubleBarrelBridge.class"
        );
        Set<String> hardReferences = hardTypeReferences(bridge);
        assertTrue(hardReferences.stream().noneMatch(name -> name.startsWith("com/mozko/")),
                "The optional bridge must load without Double Barrels present: " + hardReferences);
        assertMember(shape(bridge), "resolve",
                "(Lnet/minecraft/world/Container;I)"
                        + "Ldev/resivore/slotreservations/DoubleBarrelBridge$Resolution;",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        assertMember(shape(bridge), "associate",
                "(Lnet/minecraft/world/level/block/entity/BarrelBlockEntity;"
                        + "Lnet/minecraft/world/Container;)V",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

        MethodCode inspect = code(
                bridge,
                "inspect",
                "(Lnet/minecraft/world/level/block/entity/BarrelBlockEntity;)"
                        + "Ldev/resivore/slotreservations/DoubleBarrelBridge$Inspection;"
        );
        assertTrue(inspect.constants.contains("com.mozko.doublebarrels.DoubleBarrelAccess"));
        assertTrue(inspect.constants.containsAll(Set.of(
                "isConnected",
                "isMainBarrel",
                "getConnectionPos",
                "doublebarrels$getItems"
        )));
        assertTrue(inspect.calls.stream().anyMatch(call ->
                call.owner.equals("java/lang/Class")
                        && call.name.equals("forName")
                        && call.descriptor.equals("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")));

        MethodCode wrapperMatch = code(
                bridge,
                "wrapperMatches",
                "(Lnet/minecraft/world/Container;"
                        + "Ldev/resivore/slotreservations/DoubleBarrelBridge$OwnerPair;)Z"
        );
        assertTrue(wrapperMatch.constants.containsAll(Set.of(
                "first", "second", "firstSize", "totalSize"
        )));

        byte[] integrationMixin = classpathEntry(
                "dev/resivore/slotreservations/mixin/DoubleBarrelIntegrationMixin.class"
        );
        assertTrue(hardTypeReferences(integrationMixin).stream()
                        .noneMatch(name -> name.startsWith("com/mozko/")),
                "The optional mixin must target only the vanilla BarrelBlockEntity class");
        MixinContract mixin = mixinContract(integrationMixin);
        assertEquals(900, mixin.priority);
        assertEquals(Set.of("net/minecraft/world/level/block/entity/BarrelBlockEntity"),
                mixin.targets);

        InjectionContract returnHook = injectionContract(
                integrationMixin,
                "containerSlotReservations$attachCombinedInventory",
                "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V"
        );
        assertEquals(Set.of("getCombinedInventory()Lnet/minecraft/world/Container;"),
                returnHook.targets);
        assertEquals("RETURN", returnHook.at);
        assertEquals(0, returnHook.require);
        assertEquals(Boolean.FALSE, returnHook.remap);
        MethodCode returnHookCode = code(
                integrationMixin,
                "containerSlotReservations$attachCombinedInventory",
                "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V"
        );
        assertTrue(returnHookCode.calls.contains(new Call(
                Opcodes.INVOKESTATIC,
                "dev/resivore/slotreservations/DoubleBarrelBridge",
                "associate",
                "(Lnet/minecraft/world/level/block/entity/BarrelBlockEntity;"
                        + "Lnet/minecraft/world/Container;)V"
        )));

        String fabricMetadata = Files.readString(ROOT.resolve("src/main/resources/fabric.mod.json"));
        assertEquals("*", jsonString(objectBody(fabricMetadata, "suggests"), "doublebarrels"));
        String mixinConfig = Files.readString(ROOT.resolve(
                "src/main/resources/container_slot_reservations.mixins.json"
        ));
        assertTrue(Pattern.compile("\\\"DoubleBarrelIntegrationMixin\\\"")
                .matcher(mixinConfig).find());
    }

    private static ClassShape shape(byte[] classBytes) {
        ClassShape result = new ClassShape();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                result.access = access;
                result.interfaces = interfaces == null ? List.of() : List.of(interfaces);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                result.members.add(new Member(name, descriptor, access));
                result.fieldConstants.put(name, value);
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                result.members.add(new Member(name, descriptor, access));
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return result;
    }

    private static void assertMember(
            ClassShape shape,
            String name,
            String descriptor,
            int requiredAccess
    ) {
        assertTrue(shape.members.stream().anyMatch(member ->
                        member.name.equals(name)
                                && member.descriptor.equals(descriptor)
                                && (member.access & requiredAccess) == requiredAccess),
                "Missing member " + name + descriptor + " with flags " + requiredAccess);
    }

    private static MethodCode code(byte[] classBytes, String methodName, String descriptor) {
        MethodCode result = new MethodCode();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String methodDescriptor,
                                             String signature, String[] exceptions) {
                if (!methodName.equals(name) || !descriptor.equals(methodDescriptor)) {
                    return null;
                }
                result.found = true;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitInsn(int opcode) {
                        result.events.add(instruction(opcode));
                    }

                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        result.events.add(integer(opcode, operand));
                    }

                    @Override
                    public void visitVarInsn(int opcode, int variable) {
                        result.events.add(variable(opcode, variable));
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        result.events.add(type(opcode, type));
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name,
                                               String fieldDescriptor) {
                        result.events.add(field(opcode, owner, name, fieldDescriptor));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                                                String callDescriptor, boolean isInterface) {
                        Call call = new Call(opcode, owner, name, callDescriptor);
                        result.calls.add(call);
                        result.events.add(call(call.opcode, call.owner, call.name, call.descriptor));
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        result.constants.add(value);
                        result.events.add("ldc:" + value);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertTrue(result.found, "Missing method " + methodName + descriptor);
        return result;
    }

    private static void assertSubsequence(List<String> actual, String... expected) {
        int index = 0;
        for (String event : actual) {
            if (index < expected.length && expected[index].equals(event)) {
                index++;
            }
        }
        assertEquals(expected.length, index,
                "Missing ordered bytecode contract at " + index + ": "
                        + (index < expected.length ? expected[index] : "<complete>"));
    }

    private static String instruction(int opcode) {
        return "insn:" + opcode;
    }

    private static String integer(int opcode, int operand) {
        return "int:" + opcode + ':' + operand;
    }

    private static String variable(int opcode, int variable) {
        return "var:" + opcode + ':' + variable;
    }

    private static String type(int opcode, String type) {
        return "type:" + opcode + ':' + type;
    }

    private static String field(int opcode, String owner, String name, String descriptor) {
        return "field:" + opcode + ':' + owner + '.' + name + descriptor;
    }

    private static String call(int opcode, String owner, String name, String descriptor) {
        return "call:" + opcode + ':' + owner + '.' + name + descriptor;
    }

    private static Set<String> hardTypeReferences(byte[] classBytes) {
        Set<String> references = new HashSet<>();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                addInternalName(references, superName);
                if (interfaces != null) {
                    for (String implemented : interfaces) {
                        addInternalName(references, implemented);
                    }
                }
                addDescriptor(references, signature);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                addDescriptor(references, descriptor);
                addDescriptor(references, signature);
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                addDescriptor(references, descriptor);
                addDescriptor(references, signature);
                if (exceptions != null) {
                    for (String exception : exceptions) {
                        addInternalName(references, exception);
                    }
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        addInternalName(references, type);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name,
                                               String fieldDescriptor) {
                        addInternalName(references, owner);
                        addDescriptor(references, fieldDescriptor);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                                                String callDescriptor, boolean isInterface) {
                        addInternalName(references, owner);
                        addDescriptor(references, callDescriptor);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return references;
    }

    private static void addDescriptor(Set<String> references, String descriptor) {
        if (descriptor == null || descriptor.isBlank()) {
            return;
        }
        if (descriptor.charAt(0) == '<') {
            if (descriptor.contains("com/mozko/")) {
                references.add(descriptor);
            }
            return;
        }
        try {
            addType(references, descriptor.charAt(0) == '('
                    ? Type.getMethodType(descriptor)
                    : Type.getType(descriptor));
        } catch (IllegalArgumentException ignoredSignature) {
            if (descriptor.contains("com/mozko/")) {
                references.add(descriptor);
            }
        }
    }

    private static void addType(Set<String> references, Type type) {
        switch (type.getSort()) {
            case Type.ARRAY -> addType(references, type.getElementType());
            case Type.OBJECT -> addInternalName(references, type.getInternalName());
            case Type.METHOD -> {
                addType(references, type.getReturnType());
                for (Type argument : type.getArgumentTypes()) {
                    addType(references, argument);
                }
            }
            default -> {
            }
        }
    }

    private static void addInternalName(Set<String> references, String name) {
        if (name != null) {
            references.add(name);
        }
    }

    private static MixinContract mixinContract(byte[] classBytes) {
        MixinContract result = new MixinContract();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) {
                    return null;
                }
                result.found = true;
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String name, Object value) {
                        if ("priority".equals(name)) {
                            result.priority = (Integer) value;
                        }
                    }

                    @Override
                    public AnnotationVisitor visitArray(String name) {
                        if (!"value".equals(name)) {
                            return null;
                        }
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override
                            public void visit(String ignored, Object value) {
                                if (value instanceof Type type) {
                                    result.targets.add(type.getInternalName());
                                }
                            }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertTrue(result.found, "Missing @Mixin annotation");
        return result;
    }

    private static InjectionContract injectionContract(
            byte[] classBytes,
            String methodName,
            String methodDescriptor
    ) {
        InjectionContract result = new InjectionContract();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!methodName.equals(name) || !methodDescriptor.equals(descriptor)) {
                    return null;
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(
                            String annotationDescriptor,
                            boolean visible
                    ) {
                        if (!"Lorg/spongepowered/asm/mixin/injection/Inject;"
                                .equals(annotationDescriptor)) {
                            return null;
                        }
                        result.found = true;
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override
                            public void visit(String name, Object value) {
                                switch (name) {
                                    case "require" -> result.require = (Integer) value;
                                    case "remap" -> result.remap = (Boolean) value;
                                    default -> {
                                    }
                                }
                            }

                            @Override
                            public AnnotationVisitor visitArray(String name) {
                                if ("method".equals(name)) {
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visit(String ignored, Object value) {
                                            result.targets.add((String) value);
                                        }
                                    };
                                }
                                if ("at".equals(name)) {
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override
                                        public AnnotationVisitor visitAnnotation(
                                                String ignored,
                                                String descriptor
                                        ) {
                                            return atVisitor(result);
                                        }
                                    };
                                }
                                return null;
                            }

                            @Override
                            public AnnotationVisitor visitAnnotation(
                                    String name,
                                    String descriptor
                            ) {
                                if (!"at".equals(name)) {
                                    return null;
                                }
                                return atVisitor(result);
                            }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertTrue(result.found, "Missing @Inject annotation on " + methodName);
        return result;
    }

    private static AnnotationVisitor atVisitor(InjectionContract result) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                if ("value".equals(name)) {
                    result.at = (String) value;
                }
            }
        };
    }

    private static byte[] classpathEntry(String name) throws IOException {
        try (InputStream input = DoubleBarrelUpstreamContractTest.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            assertNotNull(input, "Missing classpath entry: " + name);
            return input.readAllBytes();
        }
    }

    private static byte[] bytes(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        assertNotNull(entry, "Missing retained-artifact entry: " + name);
        try (InputStream input = zip.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static String text(ZipFile zip, String name) throws IOException {
        return new String(bytes(zip, name), StandardCharsets.UTF_8);
    }

    private static String objectBody(String json, String key) {
        Matcher matcher = Pattern.compile(
                "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\{(.*?)\\}",
                Pattern.DOTALL
        ).matcher(json);
        assertTrue(matcher.find(), "Missing JSON object: " + key);
        return matcher.group(1);
    }

    private static String jsonString(String json, String key) {
        Matcher matcher = Pattern.compile(
                "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
        ).matcher(json);
        assertTrue(matcher.find(), "Missing JSON string: " + key);
        return matcher.group(1);
    }

    private static String jsonArrayValue(String json, String key) {
        Matcher matcher = Pattern.compile(
                "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[\\s*\\\"([^\\\"]+)\\\""
        ).matcher(json);
        assertTrue(matcher.find(), "Missing JSON array value: " + key);
        return matcher.group(1);
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private record Member(String name, String descriptor, int access) {
    }

    private record Call(int opcode, String owner, String name, String descriptor) {
    }

    private static final class ClassShape {
        private int access;
        private List<String> interfaces = List.of();
        private final Set<Member> members = new HashSet<>();
        private final Map<String, Object> fieldConstants = new HashMap<>();
    }

    private static final class MethodCode {
        private boolean found;
        private final List<String> events = new ArrayList<>();
        private final List<Call> calls = new ArrayList<>();
        private final Set<Object> constants = new HashSet<>();

        private long count(String event) {
            return events.stream().filter(event::equals).count();
        }
    }

    private static final class MixinContract {
        private boolean found;
        private int priority = 1_000;
        private final Set<String> targets = new HashSet<>();
    }

    private static final class InjectionContract {
        private boolean found;
        private Integer require;
        private Boolean remap;
        private String at;
        private final Set<String> targets = new HashSet<>();
    }
}

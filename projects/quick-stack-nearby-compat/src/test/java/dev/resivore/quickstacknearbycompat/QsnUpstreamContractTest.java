package dev.resivore.quickstacknearbycompat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QsnUpstreamContractTest {
    private static final String EXPECTED_SHA256 =
            "43F1130527F782A291231C682791B4FD3766A20916C691CBDB98F91FDCC47E53";
    private static final long EXPECTED_SIZE = 159_918L;

    @Test
    void exactAuditedQsnArtifactIsUsed() throws Exception {
        Path jar = referenceJar();

        assertEquals(EXPECTED_SIZE, Files.size(jar));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Files.readAllBytes(jar));
        assertEquals(EXPECTED_SHA256, HexFormat.of().withUpperCase().formatHex(digest.digest()));

        try (JarFile zip = new JarFile(jar.toFile())) {
            String metadata = readText(zip, "fabric.mod.json");
            assertTrue(metadata.contains("\"id\": \"quick-stack-nearby\""));
            assertTrue(metadata.contains("\"version\": \"0.4.0\""));
        }
    }

    @Test
    void serverStillHasTheSingleVanillaBoundarySeam() throws Exception {
        try (JarFile jar = openReference()) {
            ClassNode service = readClass(jar, "tempeststudios/quickstacknearby/QuickStackService.class");
            MethodNode quickStack = method(
                    service,
                    "quickStack",
                    "(Lnet/minecraft/server/level/ServerPlayer;Ltempeststudios/quickstacknearby/QuickStackMoveEngine$SourceRules;)Ltempeststudios/quickstacknearby/QuickStackMoveEngine$Result;"
            );

            assertEquals(1, countIntegerConstant(quickStack, 36));
            assertTrue(hasCall(
                    quickStack,
                    "tempeststudios/quickstacknearby/QuickStackMoveEngine",
                    "moveMatchingItems"
            ));
        }
    }

    @Test
    void rulesScreenKeepsRealBackingSlotIdentityAndTwoNarrowLayoutSeams() throws Exception {
        try (JarFile jar = openReference()) {
            ClassNode screen = readClass(jar, "tempeststudios/quickstacknearby/QuickStackRulesScreen.class");
            MethodNode isTargetSlot = method(screen, "isTargetSlot", "(I)Z");
            MethodNode computeLayout = method(screen, "computeLayout", "()V");
            MethodNode targetEntries = method(screen, "targetEntries", "()Ljava/util/List;");

            assertEquals(1, countIntegerConstant(isTargetSlot, 36));
            assertEquals(1, countIntegerConstant(computeLayout, 3));
            assertTrue(hasCall(targetEntries, "net/minecraft/world/inventory/Slot", "getContainerSlot"));
            assertTrue(hasCall(targetEntries, "java/util/List", "sort"));
        }
    }

    @Test
    void inventoryActionButtonKeepsTheExactTwelvePixelPlacementSeam() throws Exception {
        try (JarFile jar = openReference()) {
            ClassNode screenMixin = readClass(
                    jar,
                    "tempeststudios/quickstacknearby/mixin/QuickStackInventoryScreenMixin.class"
            );
            MethodNode onInit = method(
                    screenMixin,
                    "quickStackNearby$onInit",
                    "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"
            );
            MethodNode update = method(screenMixin, "quickStackNearby$updateButtonPosition", "()V");
            String bridgeOwner = "tempeststudios/quickstacknearby/QuickStackButtonSlotBridge";

            assertTrue(hasTypeInstruction(
                    onInit,
                    Opcodes.INSTANCEOF,
                    "net/minecraft/client/gui/screens/inventory/InventoryScreen"
            ));
            assertTrue(hasTypeInstruction(
                    onInit,
                    Opcodes.NEW,
                    "tempeststudios/quickstacknearby/QuickStackIconButton"
            ));
            assertEquals(1, countCalls(onInit, bridgeOwner, "reservePlayerInventorySlot"));
            assertEquals(1, countCalls(update, bridgeOwner, "reservePlayerInventorySlot"));
            assertEquals(1, countCalls(update, "net/minecraft/client/gui/components/Button", "setX"));
            assertEquals(1, countCalls(update, "net/minecraft/client/gui/components/Button", "setY"));
            assertEquals(2, countCallsAcrossJar(jar, bridgeOwner, "reservePlayerInventorySlot"));

            ClassNode bridge = readClass(
                    jar,
                    "tempeststudios/quickstacknearby/QuickStackButtonSlotBridge.class"
            );
            method(
                    bridge,
                    "reservePlayerInventorySlot",
                    "(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Ljava/lang/String;Ljava/lang/String;)Ltempeststudios/quickstacknearby/QuickStackButtonSlotBridge$SlotPlacement;"
            );
            ClassNode sharedSlots = readClass(
                    jar,
                    "tempeststudios/quickstacknearby/QuickStackButtonSlotBridge$ExternalInventoryPlusSlots.class"
            );
            MethodNode createSharedSlots = method(
                    sharedSlots,
                    "create",
                    "()Ltempeststudios/quickstacknearby/QuickStackButtonSlotBridge$ExternalInventoryPlusSlots;"
            );
            MethodNode reserveSharedSlot = method(
                    sharedSlots,
                    "reservePlayerInventorySlot",
                    "(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Ljava/lang/String;Ljava/lang/String;)Ltempeststudios/quickstacknearby/QuickStackButtonSlotBridge$SlotPlacement;"
            );
            assertTrue(hasStringConstant(
                    createSharedSlots,
                    "tempeststudios.inventorysort.api.InventoryScreenButtonSlots"
            ));
            assertTrue(hasStringConstant(
                    createSharedSlots,
                    "tempeststudios.inventorysort.api.InventoryScreenButtonSlots$RightSlotGroup"
            ));
            assertTrue(hasStringConstant(createSharedSlots, "PLAYER_INVENTORY"));
            assertTrue(hasStringConstant(createSharedSlots, "THIRD_PARTY_DEFAULT_PRIORITY"));
            assertTrue(hasStringConstant(createSharedSlots, "reserveRightSlot"));
            assertEquals(1, countIntegerConstant(reserveSharedSlot, 12));
            assertTrue(hasCall(reserveSharedSlot, "java/lang/reflect/Method", "invoke"));

            ClassNode iconButton = readClass(
                    jar,
                    "tempeststudios/quickstacknearby/QuickStackIconButton.class"
            );
            MethodNode constructor = method(
                    iconButton,
                    "<init>",
                    "(IILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;Lnet/minecraft/client/gui/components/Button$OnPress;)V"
            );
            assertEquals(2, countIntegerConstant(constructor, 12));
            assertTrue(hasCall(
                    constructor,
                    "tempeststudios/quickstacknearby/QuickStackCustomButtonBase",
                    "<init>"
            ));
        }
    }

    @Test
    void sourceRuleProtocolRemainsCappedAtSixtyFour() throws Exception {
        try (JarFile jar = openReference()) {
            ClassNode payload = readClass(jar, "tempeststudios/quickstacknearby/QuickStackRequestPayload.class");
            MethodNode write = method(payload, "write", "(Lnet/minecraft/network/FriendlyByteBuf;)V");
            MethodNode read = method(
                    payload,
                    "read",
                    "(Lnet/minecraft/network/FriendlyByteBuf;)Ltempeststudios/quickstacknearby/QuickStackRequestPayload;"
            );

            assertTrue(countIntegerConstant(write, 64) >= 1);
            assertTrue(countIntegerConstant(read, 64) >= 1);
        }
    }

    @Test
    void exactIdentityAndActualInsertionRemainOwnedByQsn() throws Exception {
        try (JarFile jar = openReference()) {
            ClassNode target = readClass(
                    jar,
                    "tempeststudios/quickstacknearby/QuickStackMoveEngine$Target.class"
            );
            MethodNode accepts = method(
                    target,
                    "accepts",
                    "(Ltempeststudios/quickstacknearby/QuickStackMoveEngine$StackKey;)Z"
            );
            ClassNode engine = readClass(jar, "tempeststudios/quickstacknearby/QuickStackMoveEngine.class");
            MethodNode existing = method(
                    engine,
                    "insertIntoExistingStacks",
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;)I"
            );
            MethodNode empty = method(
                    engine,
                    "insertIntoEmptySlots",
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;)I"
            );
            assertTrue(hasCall(accepts, "java/util/Set", "contains"));
            assertTrue(hasCall(existing, "tempeststudios/quickstacknearby/InventoryCompat",
                    "sameItemAndComponents"));
            assertTrue(hasCall(empty, "net/minecraft/world/item/ItemStack", "copyWithCount"));
        }
    }

    @Test
    void reservationHookTargetsTheExactPostMergeEmptySlotSeam() throws Exception {
        try (JarFile jar = openReference()) {
            ClassNode engine = readClass(jar, "tempeststudios/quickstacknearby/QuickStackMoveEngine.class");
            MethodNode target = method(
                    engine,
                    "insertIntoTarget",
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;)I"
            );
            MethodNode empty = method(
                    engine,
                    "insertIntoEmptySlots",
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;)I"
            );
            MethodNode move = method(
                    engine,
                    "moveMatchingItems",
                    "(Lnet/minecraft/world/Container;IILjava/util/List;"
                            + "Ltempeststudios/quickstacknearby/QuickStackMoveEngine$SourceRules;)"
                            + "Ltempeststudios/quickstacknearby/QuickStackMoveEngine$Result;"
            );
            ClassNode inventoryCompat = readClass(
                    jar,
                    "tempeststudios/quickstacknearby/InventoryCompat.class"
            );
            MethodNode maxStackSize = method(
                    inventoryCompat,
                    "maxStackSize",
                    "(Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;)I"
            );

            assertEquals(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    empty.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE
                            | Opcodes.ACC_STATIC));
            List<String> insertionCalls = new ArrayList<>();
            for (AbstractInsnNode instruction : target.instructions) {
                if (instruction instanceof MethodInsnNode call
                        && call.owner.equals("tempeststudios/quickstacknearby/QuickStackMoveEngine")) {
                    insertionCalls.add(call.name + call.desc);
                }
            }
            assertEquals(List.of(
                    "insertIntoExistingStacks(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;)I",
                    "insertIntoEmptySlots(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;)I"
            ), insertionCalls);
            assertTrue(hasCall(empty, "net/minecraft/world/Container", "canPlaceItem"));
            assertTrue(hasCall(empty, "tempeststudios/quickstacknearby/InventoryCompat", "maxStackSize"));
            assertTrue(hasCall(empty, "net/minecraft/world/Container", "setItem"));
            assertTrue(hasCall(empty, "net/minecraft/world/item/ItemStack", "copyWithCount"));
            assertTrue(hasCall(maxStackSize, "net/minecraft/world/Container", "getMaxStackSize"));
            assertEquals(Opcodes.ACC_STATIC,
                    maxStackSize.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED
                            | Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC),
                    "The audited QSN capacity helper remains package-private static");
            assertEquals(2, countCalls(move, "net/minecraft/world/Container", "setChanged"),
                    "QSN must retain source and touched-target dirty tracking around the injected helper");
        }
    }

    @Test
    void accessAndValidityChecksRemainUpstreamOwned() throws Exception {
        try (JarFile jar = openReference()) {
            ClassNode service = readClass(jar, "tempeststudios/quickstacknearby/QuickStackService.class");
            MethodNode scan = method(
                    service,
                    "scanContainer",
                    "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ltempeststudios/quickstacknearby/QuickStackService$ScannedContainer;"
            );
            MethodNode canUse = method(
                    service,
                    "canUseContainer",
                    "(Lnet/minecraft/world/Container;Lnet/minecraft/server/level/ServerLevel;Ljava/util/List;Lnet/minecraft/server/level/ServerPlayer;)Z"
            );

            assertTrue(hasCall(scan, "tempeststudios/quickstacknearby/QuickStackService", "canUseContainer"));
            assertTrue(hasCall(canUse, "net/minecraft/world/level/block/entity/BaseContainerBlockEntity", "canOpen"));
            assertTrue(hasCall(canUse, "net/minecraft/world/Container", "stillValid"));
            assertTrue(hasCall(canUse, "net/minecraft/server/level/ServerPlayer", "mayInteract"));
        }
    }

    @Test
    void reservationDiscoveryHookTargetsTheExactAcceptedTypesPrefilterBeforeEmptyDrop() throws Exception {
        try (JarFile jar = openReference()) {
            ClassNode service = readClass(jar, "tempeststudios/quickstacknearby/QuickStackService.class");
            MethodNode scan = method(
                    service,
                    "scanContainer",
                    "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;"
                            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)"
                            + "Ltempeststudios/quickstacknearby/QuickStackService$ScannedContainer;"
            );
            MethodNode nearby = method(
                    service,
                    "nearbyTargets",
                    "(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/List;"
            );

            int canUse = firstCallIndex(
                    scan, "tempeststudios/quickstacknearby/QuickStackService", "canUseContainer");
            int acceptedTypes = firstCallIndex(
                    scan, "tempeststudios/quickstacknearby/QuickStackMoveEngine", "acceptedTypes");
            int scannedRecord = firstCallIndex(
                    scan, "tempeststudios/quickstacknearby/QuickStackService$ScannedContainer", "<init>");
            assertEquals(1, countCalls(
                    scan, "tempeststudios/quickstacknearby/QuickStackMoveEngine", "acceptedTypes"));
            assertTrue(canUse >= 0 && canUse < acceptedTypes && acceptedTypes < scannedRecord,
                    "QSN no longer derives accepted types after its access gate and before the scanned record");

            int scanCall = firstCallIndex(
                    nearby, "tempeststudios/quickstacknearby/QuickStackService", "scanContainer");
            List<Integer> recordAffinityReads = callIndexes(
                    nearby, "tempeststudios/quickstacknearby/QuickStackService$ScannedContainer", "acceptedTypes");
            int emptyDrop = firstCallIndex(nearby, "java/util/Set", "isEmpty");
            int rawAdd = firstCallIndex(nearby, "java/util/List", "add");
            int distanceSort = firstCallIndex(nearby, "java/util/List", "sort");
            int targetConstruction = firstCallIndex(
                    nearby, "tempeststudios/quickstacknearby/QuickStackMoveEngine$Target", "<init>");
            assertEquals(2, recordAffinityReads.size(),
                    "QSN must read the frozen affinity once for admission and once for Target construction");
            assertEquals(1, countCalls(nearby, "java/util/Set", "isEmpty"));
            assertTrue(scanCall >= 0
                            && scanCall < recordAffinityReads.getFirst()
                            && recordAffinityReads.getFirst() < emptyDrop
                            && emptyDrop < rawAdd
                            && rawAdd < distanceSort
                            && distanceSort < recordAffinityReads.get(1)
                            && recordAffinityReads.get(1) < targetConstruction,
                    "QSN's raw empty-affinity discard, distance ordering, or frozen Target seam changed");
        }
    }

    private static Path referenceJar() {
        String configured = System.getProperty("qsnReferenceJar");
        assertNotNull(configured, "Gradle must provide qsnReferenceJar");
        return Path.of(configured);
    }

    private static JarFile openReference() throws Exception {
        return new JarFile(referenceJar().toFile());
    }

    private static ClassNode readClass(JarFile jar, String entryName) throws Exception {
        var entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing upstream class " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static String readText(JarFile jar, String entryName) throws Exception {
        var entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing upstream entry " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing " + owner.name + "." + name + descriptor));
    }

    private static int countIntegerConstant(MethodNode method, int expected) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            Integer value = integerConstant(instruction);
            if (value != null && value == expected) {
                count++;
            }
        }
        return count;
    }

    private static Integer integerConstant(AbstractInsnNode instruction) {
        if (instruction instanceof IntInsnNode value) {
            return value.operand;
        }
        if (instruction instanceof LdcInsnNode value && value.cst instanceof Integer integer) {
            return integer;
        }
        int opcode = instruction.getOpcode();
        if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
            return opcode - Opcodes.ICONST_0;
        }
        return null;
    }

    private static boolean hasCall(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasStringConstant(MethodNode method, String expected) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode value && expected.equals(value.cst)) {
                return true;
            }
        }
        return false;
    }

    private static int countCalls(MethodNode method, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static int firstCallIndex(MethodNode method, String owner, String name) {
        List<Integer> indices = callIndexes(method, owner, name);
        return indices.isEmpty() ? -1 : indices.getFirst();
    }

    private static List<Integer> callIndexes(MethodNode method, String owner, String name) {
        List<Integer> indices = new ArrayList<>();
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)) {
                indices.add(index);
            }
            index++;
        }
        return indices;
    }

    private static int countCallsAcrossJar(JarFile jar, String owner, String name) throws Exception {
        int count = 0;
        var entries = jar.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            try (InputStream input = jar.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                for (MethodNode method : node.methods) {
                    count += countCalls(method, owner, name);
                }
            }
        }
        return count;
    }

    private static boolean hasTypeInstruction(MethodNode method, int opcode, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode type
                    && type.getOpcode() == opcode
                    && type.desc.equals(descriptor)) {
                return true;
            }
        }
        return false;
    }
}

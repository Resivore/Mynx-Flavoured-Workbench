package dev.resivore.slotreservations;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exact upstream and packaged-bridge contracts for the Canary 23 Mouse Tweaks correction. */
final class MouseTweaksCompatibilityContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String MAIN = "yalter/mousetweaks/Main";
    private static final String RMB_HELPER_DESCRIPTOR =
            "(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/item/ItemStack;)V";
    private static final String TARGET_SHA256 =
            "4592eff38a2e7af3487688e888a637a8cbd4767b46662df94d993a889102effe";

    @Test
    void exactMouseTweaks231RetainsTheObservedCompatibilityGateAndDragTraversal() throws Exception {
        String configured = System.getProperty("mouseTweaksReferenceJar");
        Assumptions.assumeTrue(configured != null && Files.isRegularFile(Path.of(configured)),
                "The local optional Mouse Tweaks reference is unavailable on this host");
        Path jar = Path.of(configured);
        assertEquals(TARGET_SHA256, sha256(jar));
        assertEquals(75_872L, Files.size(jar),
                "The canonical Mouse Tweaks identity includes its verified byte length");

        try (JarFile archive = new JarFile(jar.toFile())) {
            Set<String> methods = methods(archive, MAIN + ".class");
            Set<String> fields = fields(archive, MAIN + ".class");
            assertTrue(methods.contains("rmbTweakMaybeClickSlot" + RMB_HELPER_DESCRIPTOR));
            assertTrue(methods.contains("onMouseDrag(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z"));
            assertTrue(fields.contains("oldSelectedSlot:Lnet/minecraft/world/inventory/Slot;"));
            assertTrue(fields.contains("canDoRMBDrag:Z"));
            assertTrue(fields.contains("rmbTweakLeftOriginalSlot:Z"));

            assertOccupiedCompatibilityGatePrecedesClick(archive);
            assertSameIdentityReturnsBeforeAnyDragAction(archive);
            assertOriginThenCurrentHelperOrder(archive);
            assertRmbPressArmingContract(archive);

            List<String> fabricDragCalls = methodCalls(archive,
                    "yalter/mousetweaks/fabric/mixin/MixinMouseHandler.class", "onMouseDragged");
            int upstream = fabricDragCalls.indexOf(
                    "yalter/mousetweaks/Main.onMouseDrag(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z");
            int screen = fabricDragCalls.indexOf(
                    "com/llamalad7/mixinextras/injector/wrapoperation/Operation.call([Ljava/lang/Object;)Ljava/lang/Object;");
            assertTrue(upstream >= 0 && screen > upstream,
                    "Mouse Tweaks must run before the wrapped Screen.mouseDragged fallback");
        }
    }

    @Test
    void packagedOptionalMainMixinIsPseudoGatedAndShipsNoMouseTweaksClasses() throws IOException {
        Path artifact = Path.of(System.getProperty("canary12Artifact"));
        try (JarFile archive = new JarFile(artifact.toFile())) {
            String mixinEntry = "dev/resivore/slotreservations/mixin/client/MouseTweaksMainMixin.class";
            CompiledClassShape mixin = classShape(archive, mixinEntry);
            assertTrue(mixin.interfaces().isEmpty(),
                    "The helper injection must not implement or package Mouse Tweaks' provider API");
            assertTrue(mixin.annotations().contains("Lorg/spongepowered/asm/mixin/Pseudo;"));
            assertTrue(mixin.annotations().contains("Lorg/spongepowered/asm/mixin/Mixin;"));
            assertEquals(Set.of(MAIN), mixin.mixinTargets());

            String beforeDescriptor = RMB_HELPER_DESCRIPTOR.substring(0,
                    RMB_HELPER_DESCRIPTOR.length() - 2)
                    + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";
            assertTrue(mixin.methods().containsKey(
                    "containerSlotReservations$beforeNativeRmbSlotEntry" + beforeDescriptor));
            assertTrue(mixin.methods().containsKey(
                    "containerSlotReservations$afterNativeRmbSlotEntry" + beforeDescriptor));
            String dragObserverDescriptor =
                    "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";
            assertTrue(mixin.methods().containsKey(
                    "containerSlotReservations$observeNativeRmbSlotIdentity" + dragObserverDescriptor));

            List<Instruction> before = methodInstructions(archive, mixinEntry,
                    "containerSlotReservations$beforeNativeRmbSlotEntry", beforeDescriptor);
            int decision = methodIndex(before,
                    "dev/resivore/slotreservations/client/CarriedShulkerMouseTweaks",
                    "beforeMouseTweaksSlot", "(Lnet/minecraft/world/inventory/Slot;)Z", 0);
            int conditional = nextExecutable(before, decision + 1);
            int cancel = methodIndex(before,
                    "org/spongepowered/asm/mixin/injection/callback/CallbackInfo",
                    "cancel", "()V", conditional + 1);
            assertEquals(Opcodes.IFEQ, before.get(conditional).opcode());
            assertTrue(decision >= 0 && cancel > conditional,
                    "The HEAD hook must cancel only coordinator-owned helper entries");

            List<Instruction> after = methodInstructions(archive, mixinEntry,
                    "containerSlotReservations$afterNativeRmbSlotEntry", beforeDescriptor);
            assertTrue(methodIndex(after,
                    "dev/resivore/slotreservations/client/CarriedShulkerMouseTweaks",
                    "afterMouseTweaksSlot", "(Lnet/minecraft/world/inventory/Slot;)V", 0) >= 0,
                    "The RETURN hook must validate and advance the native outbound result");

            List<Instruction> dragObserver = methodInstructions(archive, mixinEntry,
                    "containerSlotReservations$observeNativeRmbSlotIdentity", dragObserverDescriptor);
            assertTrue(methodIndex(dragObserver,
                    "dev/resivore/slotreservations/client/CarriedShulkerMouseTweaks",
                    "observeMouseTweaksDrag", "(Lnet/minecraft/world/inventory/Slot;)V", 0) >= 0,
                    "The full drag RETURN hook must expose Mouse Tweaks' null-slot boundary");

            assertFalse(archive.stream().anyMatch(entry ->
                            entry.getName().startsWith("yalter/mousetweaks/")),
                    "The release must remain safe when Mouse Tweaks is absent");
            assertFalse(archive.stream().anyMatch(entry -> entry.getName().endsWith(
                            "MouseTweaksContainerScreenMixin.class")),
                    "The retired virtual-provider bridge must not ship");
        }

        String plugin = source("mixin/client/MouseTweaksMixinPlugin.java");
        String mixinJson = Files.readString(ROOT.resolve(
                "src/main/resources/container_slot_reservations.client.mixins.json"));
        assertTrue(plugin.contains(
                "dev.resivore.slotreservations.mixin.client.MouseTweaksMainMixin"));
        assertTrue(plugin.contains("isModLoaded(\"mousetweaks\")"));
        assertTrue(mixinJson.contains("\"MouseTweaksMainMixin\""));
        assertFalse(mixinJson.contains("MouseTweaksContainerScreenMixin"));
    }

    @Test
    void canary23LatchesOneOfThreeModesAndChainsInboundFingerprints() throws IOException {
        String gesture = source("client/CarriedShulkerRmbGesture.java");
        String coordinator = source("client/CarriedShulkerMouseTweaks.java");
        String compatibility = source("client/MouseTweaksCompatibility.java");
        String mixin = source("mixin/client/MouseTweaksMainMixin.java");
        String screen = source("mixin/client/AbstractContainerScreenMixin.java");
        String payload = source("network/CarriedShulkerInventoryActionPayload.java");
        String actions = source("CarriedShulkerInventoryActions.java");
        String networking = source("ReservationNetworking.java");
        String allMainJava = allMainJava();

        String modes = between(gesture, "enum Mode {", "}");
        assertTrue(modes.contains("INACTIVE"));
        assertTrue(modes.contains("SHULKER_TO_INVENTORY"));
        assertTrue(modes.contains("INVENTORY_TO_SHULKER"));
        assertEquals(3, modes.split(",").length,
                "The gesture state must remain the explicit inactive/two-direction model");
        assertTrue(gesture.contains("initialSlotOccupied ? Mode.INVENTORY_TO_SHULKER : Mode.SHULKER_TO_INVENTORY"));
        assertTrue(gesture.contains("slot.equals(currentSlot)"),
                "Consecutive identity repeats must be deduplicated without suppressing later re-entry");
        assertTrue(gesture.contains("creativePhysicalPlayerSlot"));
        assertTrue(gesture.contains("creativeCoordinate >= 36 && creativeCoordinate <= 44"));
        assertTrue(gesture.contains("validLiveFingerprints"));

        assertTrue(coordinator.contains("ShulkerTransferPlanner.planInsertion("),
                "Inbound actions must project the whole-shulker planner, never one-item clicks");
        assertTrue(coordinator.contains("!MouseTweaksCompatibility.ownsRightDrag()"));
        assertTrue(coordinator.contains("carried.getCount() != 1"));
        assertTrue(coordinator.contains("!SupportedContainerResolver.isSupportedShulkerItem(carried)"),
                "An ordinary non-shulker cursor must never activate CSR's special gesture");
        assertTrue(coordinator.contains("new CarriedShulkerInventoryActionPayload("));
        assertTrue(coordinator.contains("GESTURE.projectedFingerprint()"));
        assertTrue(coordinator.contains("GESTURE.projectedSource("),
                "Repeated rapid entries must plan from each slot's projected predecessor");
        assertTrue(coordinator.contains("sourceFingerprint"));
        assertTrue(coordinator.contains("GESTURE.advance("));
        assertTrue(coordinator.contains("GESTURE.advanceSource("));
        assertTrue(coordinator.contains("target.container != player.getInventory()"));
        assertTrue(coordinator.contains("physical >= ORDINARY_PLAYER_SLOT_COUNT"));
        assertTrue(coordinator.contains("candidateScreen instanceof CreativeModeInventoryScreen;"),
                "Every Creative tab must use physical-slot authority, not only the inventory tab");
        assertTrue(coordinator.contains("GESTURE.observe("),
                "Mouse Tweaks' blank-area transition must update consecutive slot identity");
        assertTrue(coordinator.contains("if (target.hasItem()) dispatchInventoryToShulker"));
        assertTrue(coordinator.contains("if (target.hasItem() || !ItemStack.matches("),
                "The outbound latch must preserve only the proven empty-slot native path");
        String begin = between(coordinator, "public static boolean begin(",
                "/**\n     * Runs at HEAD of Mouse Tweaks");
        assertTrue(begin.contains("nativeOutboundSlot = target;"));
        assertTrue(begin.contains("planExpectedNativeResult(client.player, target, carried);"),
                "The native empty-origin press must project its synchronous F0 -> F1 result");

        assertTrue(payload.contains("int menuId"));
        assertTrue(payload.contains("int menuSlot"));
        assertTrue(payload.contains("int physicalPlayerSlot"));
        assertTrue(payload.contains("String carriedFingerprint"));
        assertTrue(payload.contains("String sourceFingerprint"));
        assertFalse(payload.contains("ItemStack"));
        assertFalse(payload.contains("ItemStackTemplate"));

        assertTrue(actions.contains("physicalPlayerSlot < 0"));
        assertTrue(actions.contains("physicalPlayerSlot >= Inventory.INVENTORY_SIZE"));
        assertTrue(actions.contains("menuSlot != -1 || menu != player.inventoryMenu"),
                "Only the Creative client facade may resolve by physical slot alone");
        assertTrue(actions.contains("action.carriedFingerprint()"));
        assertTrue(actions.contains("action.sourceFingerprint()"));
        assertTrue(actions.contains("ShulkerContextualTransfers.insertFromSlot("));
        assertTrue(networking.contains("CarriedShulkerInventoryActionPayload.TYPE"));
        assertTrue(networking.contains("CarriedShulkerInventoryActions.handle("));
        assertTrue(compatibility.contains("isModLoaded(MOD_ID)"));
        assertTrue(compatibility.contains("getField(\"rmbTweak\").getBoolean(config)"),
                "A disabled Mouse Tweaks RMB tweak must leave the optional gesture inactive");

        assertTrue(mixin.contains("method = \"rmbTweakMaybeClickSlot\""));
        assertTrue(mixin.contains("method = \"onMouseDrag\""));
        assertTrue(mixin.contains("oldSelectedSlot"));
        assertTrue(mixin.contains("at = @At(\"HEAD\")"));
        assertTrue(mixin.contains("at = @At(\"RETURN\")"));
        assertTrue(screen.contains("CarriedShulkerMouseTweaks.begin("));
        assertTrue(screen.contains("CarriedShulkerMouseTweaks.afterInitialNativePress()"),
                "The screen-click RETURN hook must accept the verified native origin result");
        assertTrue(screen.contains("CarriedShulkerMouseTweaks.reset()"));

        for (String retired : List.of(
                "MouseTweaksRmbGesture",
                "MouseTweaksContainerScreenMixin",
                "IMTModGuiContainer3Ex",
                "MOUSE_TWEAKS_VIRTUAL_CONTAINER",
                "mouseTweaksShadowFingerprint",
                "SECONDARY_DEPOSIT",
                "ShulkerPanelMenuQuickMovePayload")) {
            assertFalse(allMainJava.contains(retired),
                    "Canary 23 must remove the retired C20-C22 bridge: " + retired);
        }
    }

    private static void assertOccupiedCompatibilityGatePrecedesClick(JarFile archive)
            throws IOException {
        List<Instruction> code = methodInstructions(
                archive, MAIN + ".class", "rmbTweakMaybeClickSlot", RMB_HELPER_DESCRIPTOR);
        int bundle = typeIndex(code, Opcodes.INSTANCEOF,
                "net/minecraft/world/item/BundleItem", 0);
        int bundleBypass = nextExecutable(code, bundle + 1);
        int targetStack = methodIndex(code, "net/minecraft/world/inventory/Slot",
                "getItem", "()Lnet/minecraft/world/item/ItemStack;", bundleBypass + 1);
        int compatible = methodIndex(code, MAIN, "areStacksCompatible",
                "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
                targetStack + 1);
        int compatibilityBranch = nextExecutable(code, compatible + 1);
        int incompatibleReturn = nextExecutable(code, compatibilityBranch + 1);
        int count = methodIndex(code, "net/minecraft/world/item/ItemStack",
                "getCount", "()I", jumpTargetIndex(code, compatibilityBranch));
        int capacity = methodIndex(code, "net/minecraft/world/inventory/Slot",
                "getMaxStackSize", "(Lnet/minecraft/world/item/ItemStack;)I", count + 1);
        int click = methodIndex(code, "yalter/mousetweaks/IGuiScreenHandler",
                "clickSlot",
                "(Lnet/minecraft/world/inventory/Slot;Lyalter/mousetweaks/MouseButton;Z)V",
                capacity + 1);

        assertEquals(Opcodes.IFNE, code.get(bundleBypass).opcode(),
                "Bundles alone bypass the ordinary stack compatibility gate");
        assertEquals(Opcodes.IFNE, code.get(compatibilityBranch).opcode());
        assertEquals(Opcodes.RETURN, code.get(incompatibleReturn).opcode(),
                "An incompatible occupied target returns before Mouse Tweaks invokes a click");
        assertTrue(bundle >= 0 && targetStack > bundleBypass && compatible > targetStack
                        && count > incompatibleReturn && capacity > count && click > capacity,
                "The exact helper must retain compatibility and capacity guards before clickSlot");
    }

    private static void assertSameIdentityReturnsBeforeAnyDragAction(JarFile archive)
            throws IOException {
        List<Instruction> code = dragInstructions(archive);
        int lookup = methodIndex(code, "yalter/mousetweaks/IGuiScreenHandler",
                "getSlotUnderMouse", "(DD)Lnet/minecraft/world/inventory/Slot;", 0);
        int sameSlotLoad = fieldIndex(code, Opcodes.GETSTATIC, MAIN, "oldSelectedSlot",
                "Lnet/minecraft/world/inventory/Slot;", lookup + 1);
        int identityBranch = nextExecutable(code, sameSlotLoad + 1);
        int falseConstant = nextExecutable(code, identityBranch + 1);
        int falseReturn = nextExecutable(code, falseConstant + 1);
        int carried = methodIndex(code, "net/minecraft/world/inventory/AbstractContainerMenu",
                "getCarried", "()Lnet/minecraft/world/item/ItemStack;",
                jumpTargetIndex(code, identityBranch));

        assertEquals(Opcodes.IF_ACMPNE, code.get(identityBranch).opcode(),
                "Mouse Tweaks must deduplicate by Slot object identity");
        assertEquals(Opcodes.ICONST_0, code.get(falseConstant).opcode());
        assertEquals(Opcodes.IRETURN, code.get(falseReturn).opcode());
        assertTrue(lookup >= 0 && sameSlotLoad > lookup && carried > falseReturn,
                "Same-slot identity must return before cursor capture or helper execution");
    }

    private static void assertOriginThenCurrentHelperOrder(JarFile archive) throws IOException {
        List<Instruction> code = dragInstructions(archive);
        List<Integer> helperCalls = methodIndices(code, MAIN, "rmbTweakMaybeClickSlot",
                RMB_HELPER_DESCRIPTOR);
        assertEquals(2, helperCalls.size(),
                "Exact 2.31 must expose one origin replay and one current-slot helper call");
        int origin = helperCalls.get(0);
        int current = helperCalls.get(1);
        int oldSlotLoad = fieldIndex(code, Opcodes.GETSTATIC, MAIN, "oldSelectedSlot",
                "Lnet/minecraft/world/inventory/Slot;", 0);
        int enteredSlotStore = fieldIndex(code, Opcodes.PUTSTATIC, MAIN, "oldSelectedSlot",
                "Lnet/minecraft/world/inventory/Slot;", origin + 1);
        int leftOriginStore = fieldIndex(code, Opcodes.PUTSTATIC, MAIN,
                "rmbTweakLeftOriginalSlot", "Z", 0);
        int disableVanillaDrag = methodIndex(code, "yalter/mousetweaks/IGuiScreenHandler",
                "disableRMBDraggingFunctionality", "()Z", leftOriginStore + 1);

        assertTrue(oldSlotLoad >= 0 && leftOriginStore > oldSlotLoad
                        && disableVanillaDrag > leftOriginStore && origin > disableVanillaDrag
                        && enteredSlotStore > origin && current > enteredSlotStore,
                "The first transition must replay the origin, store the entered slot, then visit it");
    }

    private static void assertRmbPressArmingContract(JarFile archive) throws IOException {
        List<Instruction> code = methodInstructions(
                archive, MAIN + ".class", "onMouseClicked",
                "(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z");
        int slotLookup = methodIndex(code, "yalter/mousetweaks/IGuiScreenHandler",
                "getSlotUnderMouse", "(DD)Lnet/minecraft/world/inventory/Slot;", 0);
        int slotStore = fieldIndex(code, Opcodes.PUTSTATIC, MAIN, "oldSelectedSlot",
                "Lnet/minecraft/world/inventory/Slot;", slotLookup + 1);
        int carried = methodIndex(code, "net/minecraft/world/inventory/AbstractContainerMenu",
                "getCarried", "()Lnet/minecraft/world/item/ItemStack;", slotStore + 1);
        int empty = methodIndex(code, "net/minecraft/world/item/ItemStack",
                "isEmpty", "()Z", carried + 1);
        int enabled = fieldIndex(code, Opcodes.GETFIELD,
                "yalter/mousetweaks/Config", "rmbTweak", "Z", empty + 1);
        int arm = fieldIndex(code, Opcodes.PUTSTATIC, MAIN, "canDoRMBDrag", "Z", enabled + 1);
        int leaveOriginal = fieldIndex(code, Opcodes.PUTSTATIC, MAIN,
                "rmbTweakLeftOriginalSlot", "Z", arm + 1);
        assertTrue(slotLookup >= 0 && slotStore > slotLookup && carried > slotStore
                        && empty > carried && enabled > empty && arm > enabled
                        && leaveOriginal > arm,
                "A nonempty cursor with RMB tweak enabled must arm from the captured origin slot");
        assertEquals(Opcodes.ICONST_1, code.get(previousExecutable(code, arm - 1)).opcode());
        assertEquals(Opcodes.ICONST_0,
                code.get(previousExecutable(code, leaveOriginal - 1)).opcode());
    }

    private static List<Instruction> dragInstructions(JarFile archive) throws IOException {
        return methodInstructions(archive, MAIN + ".class", "onMouseDrag",
                "(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z");
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/dev/resivore/slotreservations")
                .resolve(relative));
    }

    private static String allMainJava() throws IOException {
        Path root = ROOT.resolve("src/main/java");
        StringBuilder combined = new StringBuilder();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                combined.append(Files.readString(path)).append('\n');
            }
        }
        return combined.toString();
    }

    private static String between(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue(start >= 0, "Missing source-contract marker: " + startMarker);
        int end = text.indexOf(endMarker, start + startMarker.length());
        assertTrue(end >= 0, "Missing source-contract marker: " + endMarker);
        return text.substring(start + startMarker.length(), end);
    }

    private static Set<String> methods(JarFile archive, String entryName) throws IOException {
        Set<String> methods = new HashSet<>();
        try (var stream = archive.getInputStream(archive.getJarEntry(entryName))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
                    methods.add(name + descriptor);
                    return null;
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
        }
        return methods;
    }

    private static Set<String> fields(JarFile archive, String entryName) throws IOException {
        Set<String> fields = new HashSet<>();
        try (var stream = archive.getInputStream(archive.getJarEntry(entryName))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public org.objectweb.asm.FieldVisitor visitField(
                        int access, String name, String descriptor, String signature, Object value) {
                    fields.add(name + ":" + descriptor);
                    return null;
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
        }
        return fields;
    }

    private static List<String> methodCalls(JarFile archive, String entryName,
                                            String selectedMethod) throws IOException {
        List<String> calls = new ArrayList<>();
        try (var stream = archive.getInputStream(archive.getJarEntry(entryName))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
                    if (!name.equals(selectedMethod)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode, String owner, String name,
                                                              String descriptor, boolean isInterface) {
                            calls.add(owner + "." + name + descriptor);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return List.copyOf(calls);
    }

    private static CompiledClassShape classShape(JarFile archive, String entryName)
            throws IOException {
        var entry = archive.getJarEntry(entryName);
        assertTrue(entry != null, "Missing packaged class " + entryName);
        Set<String> interfaces = new HashSet<>();
        Set<String> annotations = new HashSet<>();
        Set<String> mixinTargets = new HashSet<>();
        Map<String, Integer> methods = new LinkedHashMap<>();
        try (var stream = archive.getInputStream(entry)) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public void visit(int version, int access, String name, String signature,
                                            String superName, String[] implementedInterfaces) {
                    if (implementedInterfaces != null) interfaces.addAll(List.of(implementedInterfaces));
                }

                @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    annotations.add(descriptor);
                    if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) return null;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override public AnnotationVisitor visitArray(String name) {
                            if (!"value".equals(name) && !"targets".equals(name)) return null;
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override public void visit(String ignored, Object value) {
                                    if (value instanceof Type type) {
                                        mixinTargets.add(type.getInternalName());
                                    } else if (value instanceof String target) {
                                        mixinTargets.add(target.replace('.', '/'));
                                    }
                                }
                            };
                        }
                    };
                }

                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
                    methods.put(name + descriptor, access);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return new CompiledClassShape(Set.copyOf(interfaces), Set.copyOf(annotations),
                Set.copyOf(mixinTargets), Map.copyOf(methods));
    }

    private static List<Instruction> methodInstructions(
            JarFile archive, String entryName, String selectedMethod, String selectedDescriptor)
            throws IOException {
        var entry = archive.getJarEntry(entryName);
        assertTrue(entry != null, "Missing class " + entryName);
        List<Instruction> instructions = new ArrayList<>();
        boolean[] found = {false};
        try (var stream = archive.getInputStream(entry)) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
                    if (!name.equals(selectedMethod) || !descriptor.equals(selectedDescriptor)) return null;
                    found[0] = true;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitInsn(int opcode) {
                            instructions.add(new Instruction(InstructionKind.INSN, opcode,
                                    null, null, null, null));
                        }

                        @Override public void visitIntInsn(int opcode, int operand) {
                            instructions.add(new Instruction(InstructionKind.INT, opcode,
                                    null, null, null, operand));
                        }

                        @Override public void visitVarInsn(int opcode, int varIndex) {
                            instructions.add(new Instruction(InstructionKind.VAR, opcode,
                                    null, null, null, varIndex));
                        }

                        @Override public void visitTypeInsn(int opcode, String type) {
                            instructions.add(new Instruction(InstructionKind.TYPE, opcode,
                                    type, null, null, null));
                        }

                        @Override public void visitFieldInsn(int opcode, String owner, String name,
                                                             String descriptor) {
                            instructions.add(new Instruction(InstructionKind.FIELD, opcode,
                                    owner, name, descriptor, null));
                        }

                        @Override public void visitMethodInsn(int opcode, String owner, String name,
                                                              String descriptor, boolean isInterface) {
                            instructions.add(new Instruction(InstructionKind.METHOD, opcode,
                                    owner, name, descriptor, isInterface));
                        }

                        @Override public void visitJumpInsn(int opcode, Label label) {
                            instructions.add(new Instruction(InstructionKind.JUMP, opcode,
                                    null, null, null, label));
                        }

                        @Override public void visitLabel(Label label) {
                            instructions.add(new Instruction(InstructionKind.LABEL, -1,
                                    null, null, null, label));
                        }

                        @Override public void visitLdcInsn(Object value) {
                            instructions.add(new Instruction(InstructionKind.LDC, Opcodes.LDC,
                                    null, null, null, value));
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertTrue(found[0], "Missing method " + selectedMethod + selectedDescriptor
                + " in " + entryName);
        return List.copyOf(instructions);
    }

    private static List<Integer> methodIndices(List<Instruction> code, String owner, String name,
                                               String descriptor) {
        List<Integer> indices = new ArrayList<>();
        for (int index = 0; index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.METHOD
                    && owner.equals(instruction.owner()) && name.equals(instruction.name())
                    && descriptor.equals(instruction.descriptor())) indices.add(index);
        }
        return List.copyOf(indices);
    }

    private static int methodIndex(List<Instruction> code, String owner, String name,
                                   String descriptor, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.METHOD
                    && owner.equals(instruction.owner()) && name.equals(instruction.name())
                    && descriptor.equals(instruction.descriptor())) return index;
        }
        return -1;
    }

    private static int fieldIndex(List<Instruction> code, int opcode, String owner, String name,
                                  String descriptor, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.FIELD && instruction.opcode() == opcode
                    && owner.equals(instruction.owner()) && name.equals(instruction.name())
                    && descriptor.equals(instruction.descriptor())) return index;
        }
        return -1;
    }

    private static int typeIndex(List<Instruction> code, int opcode, String type, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.TYPE && instruction.opcode() == opcode
                    && type.equals(instruction.owner())) return index;
        }
        return -1;
    }

    private static int nextExecutable(List<Instruction> code, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            if (code.get(index).kind() != InstructionKind.LABEL) return index;
        }
        throw new AssertionError("No executable instruction at or after index " + start);
    }

    private static int previousExecutable(List<Instruction> code, int start) {
        for (int index = Math.min(start, code.size() - 1); index >= 0; index--) {
            if (code.get(index).kind() != InstructionKind.LABEL) return index;
        }
        throw new AssertionError("No executable instruction at or before index " + start);
    }

    private static int jumpTargetIndex(List<Instruction> code, int jumpIndex) {
        Object target = code.get(jumpIndex).operand();
        assertTrue(target instanceof Label, "Expected a jump at instruction " + jumpIndex);
        for (int index = 0; index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.LABEL && instruction.operand() == target) return index;
        }
        throw new AssertionError("Missing jump target label for instruction " + jumpIndex);
    }

    private enum InstructionKind { INSN, INT, VAR, TYPE, FIELD, METHOD, JUMP, LABEL, LDC }

    private record Instruction(InstructionKind kind, int opcode, String owner, String name,
                               String descriptor, Object operand) {}

    private record CompiledClassShape(Set<String> interfaces, Set<String> annotations,
                                      Set<String> mixinTargets, Map<String, Integer> methods) {}

    private static String sha256(Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}

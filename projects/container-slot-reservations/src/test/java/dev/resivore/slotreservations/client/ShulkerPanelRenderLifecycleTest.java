package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ShulkerContents;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.ModComponents;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executes the exact 26.2 render call graph after applying the packaged CSR mixin
 * seam as a deterministic probe. The rendering recorder uses the production panel
 * geometry, shulker contents, reservations, overlay planner, and eligibility rules.
 */
final class ShulkerPanelRenderLifecycleTest {
    private static final String BASE =
            "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen";
    private static final String RECIPE =
            "net/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen";
    private static final String INVENTORY =
            "net/minecraft/client/gui/screens/inventory/InventoryScreen";
    private static final String GFX = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;";
    private static final String RENDER = "(" + GFX + "IIF)V";
    private static final String CARRIED = "(" + GFX + "II)V";
    private static final String MIXIN =
            "dev/resivore/slotreservations/mixin/client/AbstractContainerScreenMixin.class";
    private static final String PANEL =
            "dev/resivore/slotreservations/client/ShulkerPanel";
    private static final String PROBE =
            "dev/resivore/slotreservations/client/ShulkerPanelRenderLifecycleTest$RenderProbe";
    private static final String SHULKER_TEXTURE = "textures/gui/container/shulker_box.png";
    private static final Path C10 = Path.of(System.getProperty("canary10ReferenceJar"));
    private static final Path C11 = Path.of(System.getProperty("canary11Artifact"));
    private static final Path INVENTORY_EXTENDED =
            Path.of(System.getProperty("inventoryExtendedReferenceJar"));

    @BeforeAll
    static void bootstrapMinecraft() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var registry = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE;
        var frozen = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true);
        boolean wasFrozen = frozen.getBoolean(registry);
        frozen.setBoolean(registry, false);
        try {
            ModComponents.initialize();
        } finally {
            frozen.setBoolean(registry, wasFrozen);
        }
    }

    @Test
    void exactPackagedMixinAndProviderContractsAreFailClosed() throws Exception {
        assertEquals("04334c1cd2316d97ec2930f43fa6945be1898607f1209d19bf92c562cd441b75",
                sha256(C10));
        assertEquals("a0ced554cb687f7119aa19ac3466c0de3ffb0a888e994514223152335b043636",
                sha256(INVENTORY_EXTENDED));

        InjectionSpec predecessor = injectionSpec(C10);
        assertEquals(Set.of("extractRenderState"), Set.copyOf(predecessor.methods));
        assertEquals("INVOKE", predecessor.atValue);
        assertEquals("L" + BASE + ";extractCarriedItem" + CARRIED, predecessor.atTarget);

        InjectionSpec successor = injectionSpec(C11);
        assertEquals(Set.of("extractCarriedItem" + CARRIED), Set.copyOf(successor.methods));
        assertEquals("HEAD", successor.atValue);
        assertEquals(1, successor.require);
        assertEquals(List.of(BASE), successor.mixinTargets);

        assertMember(classBytes(BASE), "extractRenderState", RENDER);
        assertMember(classBytes(BASE), "extractCarriedItem", CARRIED);
        assertMember(classBytes(RECIPE), "extractRenderState", RENDER);
        assertMember(classBytes(INVENTORY), "extractRenderState", RENDER);

        MethodShape base = method(classBytes(BASE), "extractRenderState", RENDER);
        MethodShape recipe = method(classBytes(RECIPE), "extractRenderState", RENDER);
        MethodShape inventory = method(classBytes(INVENTORY), "extractRenderState", RENDER);
        assertEquals(1, base.count(callNamed("extractCarriedItem", CARRIED)));
        assertEquals(1, recipe.count(callNamed("extractCarriedItem", CARRIED)));
        assertEquals(0, recipe.count(callOwned(BASE, "extractRenderState", RENDER)),
                "26.2 recipe-book rendering must bypass the base extractRenderState body");
        assertEquals(1, inventory.count(callOwned(RECIPE, "extractRenderState", RENDER)));

        assertEquals(1, packagedPanelInvocationCount(C11));
        assertEquals(0, method(classBytes(BASE), "extractRenderState", RENDER)
                .count(callOwned(PANEL, "updateAndRender", null)),
                "Vanilla must not contain a second CSR panel call");
        assertTrue(jarText(C11, "container_slot_reservations.client.mixins.json")
                .contains("container_slot_reservations.refmap.json"));
        assertNotNull(jarBytes(C11, "container_slot_reservations.refmap.json"));
        assertTrue(jarText(C11, "container_slot_reservations.refmap.json").contains("\"mappings\""));

        String ieMixins = jarText(INVENTORY_EXTENDED, "inventoryextended.mixins.json");
        assertTrue(ieMixins.contains("PlayerInventoryRecipeButton"));
        assertTrue(ieMixins.contains("GlobalDrawExtraSlots"));
        byte[] ieScreenMixin = jarBytes(INVENTORY_EXTENDED,
                "inventoryextended/mixin/PlayerInventoryRecipeButton.class");
        assertTrue(classMixinTargets(ieScreenMixin).contains(INVENTORY));
        assertFalse(hasMember(ieScreenMixin, "extractRenderState", RENDER),
                "Accepted Inventory Extended adjusts InventoryScreen without replacing its render lifecycle");
    }

    @Test
    void exactCanary10FailsTheRecipeBookReachabilityRegression() throws Exception {
        RenderLifecycle transformedC10 = transformedLifecycle(injectionSpec(C10));
        RenderResult ordinary = transformedC10.execute(BASE, eligibleHost(35));
        RenderResult inventory = transformedC10.execute(INVENTORY, eligibleHost(35));
        assertEquals(1, ordinary.panelInvocations);
        assertEquals(0, inventory.panelInvocations);
        assertThrows(AssertionError.class, () -> requireVisiblePanel(inventory),
                "The exact packaged C10 seam must reproduce the missing Survival-inventory panel");
    }

    @Test
    void transformedCanary11SubmitsOneVisiblePanelForRecipeAndOrdinaryPaths() throws Exception {
        RenderLifecycle transformedC11 = transformedLifecycle(injectionSpec(C11));

        RenderResult survival = transformedC11.execute(INVENTORY, eligibleHost(35));
        requireVisiblePanel(survival);
        assertEquals(1, survival.panelInvocations);
        assertEquals(1, survival.tooltipVisits);
        assertTrue(survival.outerTooltipSuppressed);
        assertEquals(ShulkerPanelGeometry.GRID_X, survival.frame.gridX - survival.frame.geometry.x());
        assertEquals(ShulkerPanelGeometry.GRID_Y, survival.frame.gridY - survival.frame.geometry.y());
        assertEquals(27, survival.frame.overlays.size());
        assertEquals(ReservationVisualRenderer.SlotVisualState.UNRESERVED,
                survival.frame.overlays.get(0).state());
        assertEquals(ReservationVisualRenderer.SlotVisualState.UNRESERVED,
                survival.frame.overlays.get(1).state());
        assertTrue(survival.frame.overlays.get(1).physical().isEmpty());
        assertEquals(3, survival.frame.overlays.get(0).physical().getCount());

        RenderResult chest = transformedC11.execute(BASE, eligibleHost(0));
        requireVisiblePanel(chest);
        assertEquals(1, chest.panelInvocations);
        assertEquals(1, chest.tooltipVisits);
        assertTrue(chest.outerTooltipSuppressed);
    }

    @Test
    void acceptedInventoryExtendedSlotsUseTheSameSingleCommonHook() throws Exception {
        RenderLifecycle transformedC11 = transformedLifecycle(injectionSpec(C11));
        Host extendedOrdinaryStorage = eligibleHost(53);
        RenderResult result = transformedC11.execute(INVENTORY, extendedOrdinaryStorage);
        requireVisiblePanel(result);
        assertEquals(53, extendedOrdinaryStorage.slot.getContainerSlot());
        assertEquals(1, result.panelInvocations);
        assertTrue(result.outerTooltipSuppressed);
    }

    @Test
    void negativeControlsNeverCreateAnActionableOrDuplicatePanel() throws Exception {
        RenderLifecycle transformedC11 = transformedLifecycle(injectionSpec(C11));
        List<Host> denied = List.of(
                eligibleHost(0).withoutHover(),
                eligibleHost(0).asCursorHeldOnly(),
                host(stackedShulker(), 0, true, true, false, false, false),
                host(new ItemStack(Items.STONE), 0, true, true, false, false, false),
                host(panelShulker(), 0, true, true, true, false, false),
                host(panelShulker(), 0, true, true, false, true, false),
                host(panelShulker(), 0, true, true, false, false, true),
                host(panelShulker(), 0, true, false, false, false, false)
        );
        for (int deniedIndex = 0; deniedIndex < denied.size(); deniedIndex++) {
            Host host = denied.get(deniedIndex);
            RenderResult result = transformedC11.execute(INVENTORY, host);
            assertEquals(1, result.panelInvocations,
                    "The inherited render seam still executes exactly once for a denied host");
            assertFalse(result.visible(), "Denied host " + deniedIndex + " rendered: " + host
                    + " fake=" + host.slot.isFake() + " active=" + host.slot.isActive()
                    + " count=" + host.slot.getItem().getCount());
            assertFalse(result.outerTooltipSuppressed,
                    "Denied host " + deniedIndex + " suppressed tooltip: " + host);
        }
        assertEquals(1, transformedC11.execute(BASE, eligibleHost(0)).panelInvocations);
        assertEquals(1, transformedC11.execute(INVENTORY, eligibleHost(0)).panelInvocations);
    }

    private static RenderLifecycle transformedLifecycle(InjectionSpec spec) throws IOException {
        byte[] transformedBase = transform(classBytes(BASE), spec);
        Map<String, ClassModel> models = new HashMap<>();
        models.put(BASE, model(transformedBase));
        models.put(RECIPE, model(classBytes(RECIPE)));
        models.put(INVENTORY, model(classBytes(INVENTORY)));
        return new RenderLifecycle(models);
    }

    private static byte[] transform(byte[] original, InjectionSpec spec) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        int[] inserted = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                boolean targetMethod = spec.methods.stream().anyMatch(method ->
                        method.equals(name) || method.equals(name + descriptor));
                if (!targetMethod) return delegate;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        if (spec.atValue.equals("HEAD")) insertProbe();
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method,
                                                String callDescriptor, boolean isInterface) {
                        if (spec.atValue.equals("INVOKE")
                                && spec.atTarget.equals("L" + owner + ";" + method + callDescriptor)) {
                            insertProbe();
                        }
                        super.visitMethodInsn(opcode, owner, method, callDescriptor, isInterface);
                    }

                    private void insertProbe() {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE, "mark", "()V", false);
                        inserted[0]++;
                    }
                };
            }
        }, 0);
        assertEquals(1, inserted[0], "The packaged production injection must resolve exactly once");
        return writer.toByteArray();
    }

    private static void requireVisiblePanel(RenderResult result) {
        assertEquals(1, result.panelInvocations);
        assertTrue(result.visible(), "No shulker-panel background was submitted");
        assertEquals(SHULKER_TEXTURE, result.frame.texture);
        assertEquals(176, result.frame.width);
        assertEquals(77, result.frame.height);
        assertEquals(27, result.frame.overlays.size());
    }

    private static Host eligibleHost(int containerSlot) {
        return host(panelShulker(), containerSlot, true, true, false, false, false);
    }

    private static Host host(ItemStack stack, int containerSlot, boolean hovered, boolean accessible,
                             boolean fake, boolean catalogueOnly, boolean inactive) {
        SimpleContainer container = new SimpleContainer(Math.max(54, containerSlot + 1));
        container.setItem(containerSlot, stack);
        Slot slot = new ControlledSlot(container, containerSlot, 8, 112, fake, inactive);
        return new Host(slot, hovered, accessible, catalogueOnly, false,
                100 + slot.x + 8, 40 + slot.y + 8);
    }

    private static ItemStack panelShulker() {
        bind(Blocks.SHULKER_BOX.asItem());
        bind(Items.STONE);
        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        ItemStack stone = new ItemStack(Items.STONE);
        stone.set(DataComponents.MAX_STACK_SIZE, 64);
        stone.setCount(3);
        contents.set(0, stone);
        ShulkerContents.replace(host, contents);
        return host;
    }

    private static ItemStack stackedShulker() {
        ItemStack stack = panelShulker();
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(2);
        return stack;
    }

    private static void bind(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }

    private static InjectionSpec injectionSpec(Path jar) throws IOException {
        byte[] bytes = jarBytes(jar, MIXIN);
        InjectionSpec result = new InjectionSpec();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            if (!name.equals("value")) return super.visitArray(name);
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override public void visit(String ignored, Object value) {
                                    result.mixinTargets.add(((Type) value).getInternalName());
                                }
                            };
                        }
                    };
                }
                return super.visitAnnotation(descriptor, visible);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!name.equals("containerSlotReservations$extractPinnedShulkerPanel")) return null;
                result.handlerDescriptor = descriptor;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        if (!annotation.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) return null;
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override public void visit(String name, Object value) {
                                if (name.equals("require")) result.require = (Integer) value;
                            }

                            @Override public AnnotationVisitor visitArray(String name) {
                                if (name.equals("method")) {
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override public void visit(String ignored, Object value) {
                                            result.methods.add((String) value);
                                        }
                                    };
                                }
                                if (name.equals("at")) {
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override
                                        public AnnotationVisitor visitAnnotation(String ignored,
                                                                                 String descriptor) {
                                            return atVisitor(result);
                                        }
                                    };
                                }
                                return super.visitArray(name);
                            }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertFalse(result.methods.isEmpty(), "Missing packaged panel injection");
        return result;
    }

    private static AnnotationVisitor atVisitor(InjectionSpec result) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override public void visit(String name, Object value) {
                if (name.equals("value")) result.atValue = (String) value;
                if (name.equals("target")) result.atTarget = (String) value;
            }
        };
    }

    private static int packagedPanelInvocationCount(Path jar) throws IOException {
        int[] count = {0};
        try (JarFile archive = new JarFile(jar.toFile())) {
            var entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                try (InputStream input = archive.getInputStream(entry)) {
                    new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                         String signature, String[] exceptions) {
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitMethodInsn(int opcode, String owner, String method,
                                                            String callDescriptor, boolean isInterface) {
                                    if (owner.equals(PANEL) && method.equals("updateAndRender")) count[0]++;
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
        return count[0];
    }

    private static Set<String> classMixinTargets(byte[] bytes) {
        java.util.LinkedHashSet<String> targets = new java.util.LinkedHashSet<>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (!descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) return null;
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override public AnnotationVisitor visitArray(String name) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override public void visit(String ignored, Object value) {
                                if (value instanceof Type type) targets.add(type.getInternalName());
                            }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return Set.copyOf(targets);
    }

    private static void assertMember(byte[] bytes, String name, String descriptor) {
        assertTrue(hasMember(bytes, name, descriptor), "Missing exact 26.2 method " + name + descriptor);
    }

    private static boolean hasMember(byte[] bytes, String name, String descriptor) {
        boolean[] found = {false};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String method, String methodDescriptor,
                                             String signature, String[] exceptions) {
                if (method.equals(name) && methodDescriptor.equals(descriptor)) found[0] = true;
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return found[0];
    }

    private static MethodShape method(byte[] bytes, String name, String descriptor) {
        MethodShape result = new MethodShape();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String method, String methodDescriptor,
                                             String signature, String[] exceptions) {
                if (!method.equals(name) || !methodDescriptor.equals(descriptor)) return null;
                result.found = true;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String called,
                                                String calledDescriptor, boolean isInterface) {
                        result.calls.add(new Call(owner, called, calledDescriptor));
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertTrue(result.found, "Missing method " + name + descriptor);
        return result;
    }

    private static ClassModel model(byte[] bytes) {
        ClassModel result = new ClassModel();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                result.name = name;
                result.superName = superName;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodShape shape = new MethodShape();
                shape.found = true;
                result.methods.put(name + descriptor, shape);
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method,
                                                String callDescriptor, boolean isInterface) {
                        shape.calls.add(new Call(owner, method, callDescriptor));
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return result;
    }

    private static Predicate<Call> callNamed(String name, String descriptor) {
        return call -> call.name.equals(name) && call.descriptor.equals(descriptor);
    }

    private static Predicate<Call> callOwned(String owner, String name, String descriptor) {
        return call -> call.owner.equals(owner) && call.name.equals(name)
                && (descriptor == null || call.descriptor.equals(descriptor));
    }

    private static byte[] classBytes(String internalName) throws IOException {
        try (InputStream input = ShulkerPanelRenderLifecycleTest.class.getClassLoader()
                .getResourceAsStream(internalName + ".class")) {
            assertNotNull(input, "Missing exact Minecraft class " + internalName);
            return input.readAllBytes();
        }
    }

    private static byte[] jarBytes(Path jar, String entry) throws IOException {
        try (JarFile archive = new JarFile(jar.toFile())) {
            JarEntry found = archive.getJarEntry(entry);
            assertNotNull(found, "Missing archive entry " + entry + " in " + jar);
            try (InputStream input = archive.getInputStream(found)) {
                return input.readAllBytes();
            }
        }
    }

    private static String jarText(Path jar, String entry) throws IOException {
        return new String(jarBytes(jar, entry), StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }

    private static final class RenderLifecycle {
        private final Map<String, ClassModel> classes;

        private RenderLifecycle(Map<String, ClassModel> classes) {
            this.classes = classes;
        }

        RenderResult execute(String start, Host host) {
            RenderResult result = new RenderResult(host);
            executeMethod(start, "extractRenderState", RENDER, result, 0);
            return result;
        }

        private void executeMethod(String owner, String name, String descriptor,
                                   RenderResult result, int depth) {
            assertTrue(depth < 8, "Render lifecycle recursion drifted");
            ClassModel declaring = resolve(owner, name, descriptor);
            MethodShape shape = declaring.methods.get(name + descriptor);
            for (Call call : shape.calls) {
                if (call.owner.equals(PROBE) && call.name.equals("mark")) {
                    result.panelInvocations++;
                    result.submitPanel();
                } else if (call.name.equals("extractRenderState") && call.descriptor.equals(RENDER)
                        && (call.owner.equals(BASE) || call.owner.equals(RECIPE))) {
                    executeMethod(call.owner, call.name, call.descriptor, result, depth + 1);
                } else if (call.name.equals("extractCarriedItem") && call.descriptor.equals(CARRIED)) {
                    executeMethod(call.owner, call.name, call.descriptor, result, depth + 1);
                } else if (call.name.equals("extractTooltip") && call.descriptor.equals(CARRIED)) {
                    result.tooltipVisits++;
                    result.outerTooltipSuppressed = result.visible() && result.host.hovered;
                }
            }
        }

        private ClassModel resolve(String owner, String name, String descriptor) {
            ClassModel current = classes.get(owner);
            while (current != null && !current.methods.containsKey(name + descriptor)) {
                current = classes.get(current.superName);
            }
            assertNotNull(current, "No exact render method for " + owner + '.' + name + descriptor);
            return current;
        }
    }

    private static final class RenderResult {
        private final Host host;
        private int panelInvocations;
        private int tooltipVisits;
        private boolean outerTooltipSuppressed;
        private PanelFrame frame;

        private RenderResult(Host host) {
            this.host = host;
        }

        private void submitPanel() {
            if (!host.eligible()) return;
            ShulkerPanelGeometry.Rect hostBounds = new ShulkerPanelGeometry.Rect(
                    100 + host.slot.x - 1, 40 + host.slot.y - 1, 18, 18);
            if (!hostBounds.contains(host.mouseX, host.mouseY)) return;
            ShulkerPanelGeometry geometry = ShulkerPanelGeometry.place(480, 300, 100, 176, hostBounds);
            List<ItemStack> contents = ShulkerContents.copy(host.slot.getItem());
            List<ShulkerPanelOverlay.SlotOverlay> overlays = new ArrayList<>();
            for (int index = 0; index < contents.size(); index++) {
                ItemStack physical = contents.get(index);
                overlays.add(new ShulkerPanelOverlay.SlotOverlay(index, physical, Optional.empty(),
                        ReservationVisualRenderer.state(physical, Optional.empty())));
            }
            frame = new PanelFrame(SHULKER_TEXTURE, geometry, geometry.x() + ShulkerPanelGeometry.GRID_X,
                    geometry.y() + ShulkerPanelGeometry.GRID_Y, ShulkerPanelGeometry.WIDTH,
                    ShulkerPanelGeometry.HEIGHT, overlays);
        }

        boolean visible() {
            return frame != null;
        }
    }

    private record Host(Slot slot, boolean hovered, boolean accessible, boolean catalogueOnly,
                        boolean cursorHeldOnly, double mouseX, double mouseY) {
        boolean eligible() {
            ItemStack stack = slot.getItem();
            return hovered && accessible && !catalogueOnly && !cursorHeldOnly
                    && slot.isActive() && !slot.isFake() && stack.getCount() == 1
                    && SupportedContainerResolver.isSupportedShulkerItem(stack);
        }

        Host withoutHover() {
            return new Host(slot, false, accessible, catalogueOnly, cursorHeldOnly, mouseX, mouseY);
        }

        Host asCursorHeldOnly() {
            return new Host(slot, hovered, accessible, catalogueOnly, true, mouseX, mouseY);
        }
    }

    private record PanelFrame(String texture, ShulkerPanelGeometry geometry, int gridX, int gridY,
                              int width, int height, List<ShulkerPanelOverlay.SlotOverlay> overlays) {}

    private static final class ControlledSlot extends Slot {
        private final boolean fake;
        private final boolean inactive;

        private ControlledSlot(SimpleContainer container, int slot, int x, int y,
                               boolean fake, boolean inactive) {
            super(container, slot, x, y);
            this.fake = fake;
            this.inactive = inactive;
        }

        @Override public boolean isFake() { return fake; }
        @Override public boolean isActive() { return !inactive; }
    }

    private static final class InjectionSpec {
        private final List<String> methods = new ArrayList<>();
        private final List<String> mixinTargets = new ArrayList<>();
        private String handlerDescriptor;
        private String atValue;
        private String atTarget;
        private int require = -1;
    }

    private static final class MethodShape {
        private boolean found;
        private final List<Call> calls = new ArrayList<>();
        private long count(Predicate<Call> predicate) { return calls.stream().filter(predicate).count(); }
    }

    private static final class ClassModel {
        private String name;
        private String superName;
        private final Map<String, MethodShape> methods = new HashMap<>();
    }

    private record Call(String owner, String name, String descriptor) {}

    /** Marker owner used only in transformed bytecode interpreted by this fixture. */
    static final class RenderProbe {
        private RenderProbe() {}
        static void mark() {}
    }
}

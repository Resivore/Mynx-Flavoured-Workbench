package dev.resivore.slotreservations;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Minecraft26ContainerSeamContractTest {
    private static final String CONTAINER = "net/minecraft/world/Container";
    private static final String ITEM_STACK = "net/minecraft/world/item/ItemStack";
    private static final String SLOT = "net/minecraft/world/inventory/Slot";
    private static final String CAN_PLACE = "(ILnet/minecraft/world/item/ItemStack;)Z";
    private static final String SIDED_CAN_PLACE =
            "(ILnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Z";
    private static final String MENU_CONTAINER_CONSTRUCTOR =
            "(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V";

    private static final Set<String> COPPER_CHEST_IDS = Set.of(
            "minecraft:copper_chest",
            "minecraft:exposed_copper_chest",
            "minecraft:weathered_copper_chest",
            "minecraft:oxidized_copper_chest",
            "minecraft:waxed_copper_chest",
            "minecraft:waxed_exposed_copper_chest",
            "minecraft:waxed_weathered_copper_chest",
            "minecraft:waxed_oxidized_copper_chest"
    );

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void copperChestCollectionAndChestBlockEntityTypeHaveTheExact26Point2ValidSet()
            throws IOException {
        List<Block> copperChests = Blocks.COPPER_CHEST.asList();
        assertEquals(8, copperChests.size());
        assertEquals(COPPER_CHEST_IDS, copperChests.stream()
                .map(BuiltInRegistries.BLOCK::getKey)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        assertTrue(BlockEntityTypes.CHEST.isValid(Blocks.CHEST.defaultBlockState()));
        for (Block copperChest : copperChests) {
            assertTrue(BlockEntityTypes.CHEST.isValid(copperChest.defaultBlockState()),
                    () -> "CHEST must accept " + BuiltInRegistries.BLOCK.getKey(copperChest));
        }
        assertFalse(BlockEntityTypes.CHEST.isValid(Blocks.TRAPPED_CHEST.defaultBlockState()));
        assertTrue(BlockEntityTypes.TRAPPED_CHEST.isValid(
                Blocks.TRAPPED_CHEST.defaultBlockState()));
        for (Block copperChest : copperChests) {
            assertFalse(BlockEntityTypes.TRAPPED_CHEST.isValid(copperChest.defaultBlockState()));
        }

        byte[] blocks = classpathEntry("net/minecraft/world/level/block/Blocks.class");
        assertMember(shape(blocks), "COPPER_CHEST",
                "Lnet/minecraft/world/level/block/WeatheringCopperCollection;",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);

        MethodCode typeRegistration = code(
                classpathEntry("net/minecraft/world/level/block/entity/BlockEntityTypes.class"),
                "<clinit>",
                "()V"
        );
        assertOrdered(typeRegistration,
                event -> event.equals(new FieldAccess(
                        Opcodes.GETSTATIC,
                        "net/minecraft/world/level/block/Blocks",
                        "COPPER_CHEST",
                        "Lnet/minecraft/world/level/block/WeatheringCopperCollection;"
                )),
                callNamed("net/minecraft/world/level/block/WeatheringCopperCollection", "asList"),
                event -> event.equals(new FieldAccess(
                        Opcodes.GETSTATIC,
                        "net/minecraft/world/level/block/Blocks",
                        "CHEST",
                        "Lnet/minecraft/world/level/block/Block;"
                )),
                callNamed("net/minecraft/util/Util", "copyAndAdd"),
                callNamed("net/minecraft/world/level/block/entity/BlockEntityTypes", "register"),
                event -> event.equals(new FieldAccess(
                        Opcodes.PUTSTATIC,
                        "net/minecraft/world/level/block/entity/BlockEntityTypes",
                        "CHEST",
                        "Lnet/minecraft/world/level/block/entity/BlockEntityType;"
                ))
        );

        MethodCode validity = code(
                classpathEntry("net/minecraft/world/level/block/entity/BlockEntityType.class"),
                "isValid",
                "(Lnet/minecraft/world/level/block/state/BlockState;)Z"
        );
        assertOrdered(validity,
                event -> event.equals(new FieldAccess(
                        Opcodes.GETFIELD,
                        "net/minecraft/world/level/block/entity/BlockEntityType",
                        "validBlocks",
                        "Ljava/util/Set;"
                )),
                callNamed("net/minecraft/world/level/block/state/BlockState", "getBlock"),
                callNamed("java/util/Set", "contains")
        );

        byte[] copperChest = classpathEntry(
                "net/minecraft/world/level/block/CopperChestBlock.class"
        );
        MethodCode suppliedType = code(
                copperChest,
                "lambda$new$0",
                "()Lnet/minecraft/world/level/block/entity/BlockEntityType;"
        );
        assertTrue(suppliedType.events.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "net/minecraft/world/level/block/entity/BlockEntityTypes",
                "CHEST",
                "Lnet/minecraft/world/level/block/entity/BlockEntityType;"
        )));
        MethodCode preservedState = code(
                copperChest,
                "shouldChangedStateKeepBlockEntity",
                "(Lnet/minecraft/world/level/block/state/BlockState;)Z"
        );
        assertTrue(preservedState.events.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "net/minecraft/tags/BlockTags",
                "COPPER_CHESTS",
                "Lnet/minecraft/tags/TagKey;"
        )));
        assertTrue(preservedState.calls.stream().anyMatch(call ->
                call.owner.equals("net/minecraft/world/level/block/state/BlockState")
                        && call.name.equals("is")
                        && call.descriptor.equals("(Lnet/minecraft/tags/TagKey;)Z")));
    }

    @Test
    void menusKeepTheirExactSpecializedMachineSlotClasses() throws IOException {
        String furnaceConstructor = "(Lnet/minecraft/world/inventory/MenuType;"
                + "Lnet/minecraft/resources/ResourceKey;"
                + "Lnet/minecraft/world/inventory/RecipeBookType;I"
                + "Lnet/minecraft/world/entity/player/Inventory;"
                + "Lnet/minecraft/world/Container;"
                + "Lnet/minecraft/world/inventory/ContainerData;)V";
        MethodCode furnace = code(
                classpathEntry("net/minecraft/world/inventory/AbstractFurnaceMenu.class"),
                "<init>",
                furnaceConstructor
        );
        assertEquals(List.of(
                SLOT,
                "net/minecraft/world/inventory/FurnaceFuelSlot",
                "net/minecraft/world/inventory/FurnaceResultSlot"
        ), slotAllocations(furnace));
        assertTrue(furnace.calls.contains(new Call(
                Opcodes.INVOKESPECIAL,
                "net/minecraft/world/inventory/FurnaceFuelSlot",
                "<init>",
                "(Lnet/minecraft/world/inventory/AbstractFurnaceMenu;"
                        + "Lnet/minecraft/world/Container;III)V"
        )));
        assertTrue(furnace.calls.contains(new Call(
                Opcodes.INVOKESPECIAL,
                "net/minecraft/world/inventory/FurnaceResultSlot",
                "<init>",
                "(Lnet/minecraft/world/entity/player/Player;"
                        + "Lnet/minecraft/world/Container;III)V"
        )));

        MethodCode brewing = code(
                classpathEntry("net/minecraft/world/inventory/BrewingStandMenu.class"),
                "<init>",
                "(ILnet/minecraft/world/entity/player/Inventory;"
                        + "Lnet/minecraft/world/Container;"
                        + "Lnet/minecraft/world/inventory/ContainerData;)V"
        );
        assertEquals(List.of(
                "net/minecraft/world/inventory/BrewingStandMenu$PotionSlot",
                "net/minecraft/world/inventory/BrewingStandMenu$PotionSlot",
                "net/minecraft/world/inventory/BrewingStandMenu$PotionSlot",
                "net/minecraft/world/inventory/BrewingStandMenu$IngredientsSlot",
                "net/minecraft/world/inventory/BrewingStandMenu$FuelSlot"
        ), slotAllocations(brewing));

        MethodCode crafter = code(
                classpathEntry("net/minecraft/world/inventory/CrafterMenu.class"),
                "addSlots",
                "(Lnet/minecraft/world/entity/player/Inventory;)V"
        );
        assertEquals(List.of(
                "net/minecraft/world/inventory/CrafterSlot",
                "net/minecraft/world/inventory/NonInteractiveResultSlot"
        ), slotAllocations(crafter));
        assertTrue(crafter.calls.contains(new Call(
                Opcodes.INVOKESPECIAL,
                "net/minecraft/world/inventory/CrafterSlot",
                "<init>",
                "(Lnet/minecraft/world/Container;III"
                        + "Lnet/minecraft/world/inventory/CrafterMenu;)V"
        )));

        MethodCode shulker = code(
                classpathEntry("net/minecraft/world/inventory/ShulkerBoxMenu.class"),
                "<init>",
                MENU_CONTAINER_CONSTRUCTOR
        );
        assertEquals(List.of("net/minecraft/world/inventory/ShulkerBoxSlot"),
                slotAllocations(shulker));

        MethodCode dispenser = code(
                classpathEntry("net/minecraft/world/inventory/DispenserMenu.class"),
                "add3x3GridSlots",
                "(Lnet/minecraft/world/Container;II)V"
        );
        assertEquals(List.of(SLOT), slotAllocations(dispenser));

        MethodCode hopper = code(
                classpathEntry("net/minecraft/world/inventory/HopperMenu.class"),
                "<init>",
                MENU_CONTAINER_CONSTRUCTOR
        );
        assertEquals(List.of(SLOT), slotAllocations(hopper));

        MethodCode furnaceFuel = code(
                classpathEntry("net/minecraft/world/inventory/FurnaceFuelSlot.class"),
                "mayPlace",
                "(Lnet/minecraft/world/item/ItemStack;)Z"
        );
        assertTrue(furnaceFuel.calls.stream().anyMatch(call ->
                call.owner.equals("net/minecraft/world/inventory/AbstractFurnaceMenu")
                        && call.name.equals("isFuel")
                        && call.descriptor.equals("(Lnet/minecraft/world/item/ItemStack;)Z")));

        MethodCode crafterSlot = code(
                classpathEntry("net/minecraft/world/inventory/CrafterSlot.class"),
                "mayPlace",
                "(Lnet/minecraft/world/item/ItemStack;)Z"
        );
        assertTrue(crafterSlot.calls.stream().anyMatch(call ->
                call.owner.equals("net/minecraft/world/inventory/CrafterMenu")
                        && call.name.equals("isSlotDisabled")
                        && call.descriptor.equals("(I)Z")));

        MethodCode shulkerSlot = code(
                classpathEntry("net/minecraft/world/inventory/ShulkerBoxSlot.class"),
                "mayPlace",
                "(Lnet/minecraft/world/item/ItemStack;)Z"
        );
        assertTrue(shulkerSlot.calls.stream().anyMatch(call ->
                call.owner.equals("net/minecraft/world/item/Item")
                        && call.name.equals("canFitInsideContainerItems")
                        && call.descriptor.equals("()Z")));
    }

    @Test
    void machineBlockEntitiesRetainTheirNativeAdmissionMethods() throws IOException {
        byte[] container = classpathEntry(CONTAINER + ".class");
        assertMember(shape(container), "canPlaceItem", CAN_PLACE,
                Opcodes.ACC_PUBLIC);
        MethodCode defaultAdmission = code(container, "canPlaceItem", CAN_PLACE);
        assertEquals(List.of(Opcodes.ICONST_1, Opcodes.IRETURN), defaultAdmission.opcodes);

        String furnaceOwner =
                "net/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity";
        byte[] furnace = classpathEntry(furnaceOwner + ".class");
        assertMember(shape(furnace), "canPlaceItem", CAN_PLACE, Opcodes.ACC_PUBLIC);
        assertMember(shape(furnace), "canPlaceItemThroughFace", SIDED_CAN_PLACE,
                Opcodes.ACC_PUBLIC);
        MethodCode furnaceAdmission = code(furnace, "canPlaceItem", CAN_PLACE);
        assertTrue(furnaceAdmission.calls.stream().anyMatch(call ->
                call.owner.equals("net/minecraft/world/level/block/entity/FuelValues")
                        && call.name.equals("isFuel")
                        && call.descriptor.equals("(Lnet/minecraft/world/item/ItemStack;)Z")));
        assertTrue(furnaceAdmission.events.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "net/minecraft/world/item/Items",
                "BUCKET",
                "Lnet/minecraft/world/item/Item;"
        )));
        assertDelegatesSidedAdmissionToNative(furnace, furnaceOwner);

        String brewingOwner = "net/minecraft/world/level/block/entity/BrewingStandBlockEntity";
        byte[] brewing = classpathEntry(brewingOwner + ".class");
        assertMember(shape(brewing), "canPlaceItem", CAN_PLACE, Opcodes.ACC_PUBLIC);
        assertMember(shape(brewing), "canPlaceItemThroughFace", SIDED_CAN_PLACE,
                Opcodes.ACC_PUBLIC);
        MethodCode brewingAdmission = code(brewing, "canPlaceItem", CAN_PLACE);
        assertTrue(brewingAdmission.calls.stream().anyMatch(call ->
                call.owner.equals("net/minecraft/world/item/alchemy/PotionBrewing")
                        && call.name.equals("isIngredient")
                        && call.descriptor.equals("(Lnet/minecraft/world/item/ItemStack;)Z")));
        assertTrue(brewingAdmission.events.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "net/minecraft/tags/ItemTags",
                "BREWING_FUEL",
                "Lnet/minecraft/tags/TagKey;"
        )));
        assertDelegatesSidedAdmissionToNative(brewing, brewingOwner);

        String crafterOwner = "net/minecraft/world/level/block/entity/CrafterBlockEntity";
        byte[] crafter = classpathEntry(crafterOwner + ".class");
        assertMember(shape(crafter), "canPlaceItem", CAN_PLACE, Opcodes.ACC_PUBLIC);
        MethodCode crafterAdmission = code(crafter, "canPlaceItem", CAN_PLACE);
        assertOrdered(crafterAdmission,
                event -> event.equals(new FieldAccess(
                        Opcodes.GETFIELD,
                        crafterOwner,
                        "containerData",
                        "Lnet/minecraft/world/inventory/ContainerData;"
                )),
                callNamed("net/minecraft/world/inventory/ContainerData", "get")
        );

        String shulkerOwner = "net/minecraft/world/level/block/entity/ShulkerBoxBlockEntity";
        byte[] shulker = classpathEntry(shulkerOwner + ".class");
        assertFalse(hasMember(shape(shulker), "canPlaceItem", CAN_PLACE),
                "Shulker native admission is sided-only; menu admission is a separate seam");
        assertMember(shape(shulker), "canPlaceItemThroughFace", SIDED_CAN_PLACE,
                Opcodes.ACC_PUBLIC);
        MethodCode shulkerAdmission = code(shulker, "canPlaceItemThroughFace", SIDED_CAN_PLACE);
        assertTrue(shulkerAdmission.events.contains(new TypeUse(
                Opcodes.INSTANCEOF,
                "net/minecraft/world/level/block/ShulkerBoxBlock"
        )));
    }

    @Test
    void playerEnderItemsPersistenceAndRespawnAssignmentRemainExact() throws IOException {
        String enderDescriptor =
                "Lnet/minecraft/world/inventory/PlayerEnderChestContainer;";
        byte[] player = classpathEntry("net/minecraft/world/entity/player/Player.class");
        ClassShape playerShape = shape(player);
        assertMember(playerShape, "enderChestInventory", enderDescriptor, Opcodes.ACC_PROTECTED);

        MethodCode read = code(
                player,
                "readAdditionalSaveData",
                "(Lnet/minecraft/world/level/storage/ValueInput;)V"
        );
        assertOrdered(read,
                event -> event instanceof FieldAccess field
                        && field.opcode == Opcodes.GETFIELD
                        && field.name.equals("enderChestInventory")
                        && field.descriptor.equals(enderDescriptor),
                event -> event.equals(new Constant("EnderItems")),
                event -> event.equals(new Call(
                        Opcodes.INVOKEINTERFACE,
                        "net/minecraft/world/level/storage/ValueInput",
                        "listOrEmpty",
                        "(Ljava/lang/String;Lcom/mojang/serialization/Codec;)"
                                + "Lnet/minecraft/world/level/storage/ValueInput$TypedInputList;"
                )),
                event -> event.equals(new Call(
                        Opcodes.INVOKEVIRTUAL,
                        "net/minecraft/world/inventory/PlayerEnderChestContainer",
                        "fromSlots",
                        "(Lnet/minecraft/world/level/storage/ValueInput$TypedInputList;)V"
                ))
        );

        MethodCode write = code(
                player,
                "addAdditionalSaveData",
                "(Lnet/minecraft/world/level/storage/ValueOutput;)V"
        );
        assertOrdered(write,
                event -> event instanceof FieldAccess field
                        && field.opcode == Opcodes.GETFIELD
                        && field.name.equals("enderChestInventory")
                        && field.descriptor.equals(enderDescriptor),
                event -> event.equals(new Constant("EnderItems")),
                event -> event.equals(new Call(
                        Opcodes.INVOKEINTERFACE,
                        "net/minecraft/world/level/storage/ValueOutput",
                        "list",
                        "(Ljava/lang/String;Lcom/mojang/serialization/Codec;)"
                                + "Lnet/minecraft/world/level/storage/ValueOutput$TypedOutputList;"
                )),
                event -> event.equals(new Call(
                        Opcodes.INVOKEVIRTUAL,
                        "net/minecraft/world/inventory/PlayerEnderChestContainer",
                        "storeAsSlots",
                        "(Lnet/minecraft/world/level/storage/ValueOutput$TypedOutputList;)V"
                ))
        );

        byte[] enderContainer = classpathEntry(
                "net/minecraft/world/inventory/PlayerEnderChestContainer.class"
        );
        assertMember(shape(enderContainer), "<init>", "()V", Opcodes.ACC_PUBLIC);
        assertMember(shape(enderContainer), "fromSlots",
                "(Lnet/minecraft/world/level/storage/ValueInput$TypedInputList;)V",
                Opcodes.ACC_PUBLIC);
        assertMember(shape(enderContainer), "storeAsSlots",
                "(Lnet/minecraft/world/level/storage/ValueOutput$TypedOutputList;)V",
                Opcodes.ACC_PUBLIC);

        MethodCode restore = code(
                classpathEntry("net/minecraft/server/level/ServerPlayer.class"),
                "restoreFrom",
                "(Lnet/minecraft/server/level/ServerPlayer;Z)V"
        );
        assertOrdered(restore,
                event -> event.equals(new VariableUse(Opcodes.ALOAD, 0)),
                event -> event.equals(new VariableUse(Opcodes.ALOAD, 1)),
                enderField(Opcodes.GETFIELD, enderDescriptor),
                enderField(Opcodes.PUTFIELD, enderDescriptor)
        );
    }

    private static void assertDelegatesSidedAdmissionToNative(byte[] classBytes, String owner) {
        MethodCode sided = code(classBytes, "canPlaceItemThroughFace", SIDED_CAN_PLACE);
        assertTrue(sided.calls.contains(new Call(
                Opcodes.INVOKEVIRTUAL,
                owner,
                "canPlaceItem",
                CAN_PLACE
        )));
    }

    private static Predicate<Object> enderField(int opcode, String descriptor) {
        return event -> event instanceof FieldAccess field
                && field.opcode == opcode
                && field.name.equals("enderChestInventory")
                && field.descriptor.equals(descriptor);
    }

    private static Predicate<Object> callNamed(String owner, String name) {
        return event -> event instanceof Call call
                && call.owner.equals(owner)
                && call.name.equals(name);
    }

    @SafeVarargs
    private static void assertOrdered(MethodCode code, Predicate<Object>... expected) {
        int matched = 0;
        for (Object event : code.events) {
            if (matched < expected.length && expected[matched].test(event)) {
                matched++;
            }
        }
        assertEquals(expected.length, matched,
                "Missing ordered bytecode contract at predicate " + matched);
    }

    private static List<String> slotAllocations(MethodCode code) {
        return code.types.stream()
                .filter(type -> type.opcode == Opcodes.NEW)
                .map(TypeUse::type)
                .filter(type -> type.endsWith("Slot"))
                .toList();
    }

    private static ClassShape shape(byte[] classBytes) {
        ClassShape result = new ClassShape();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                result.members.add(new Member(name, descriptor, access));
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

    private static boolean hasMember(ClassShape shape, String name, String descriptor) {
        return shape.members.stream().anyMatch(member ->
                member.name.equals(name) && member.descriptor.equals(descriptor));
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
                        result.opcodes.add(opcode);
                        result.events.add(new Instruction(opcode));
                    }

                    @Override
                    public void visitVarInsn(int opcode, int variable) {
                        result.events.add(new VariableUse(opcode, variable));
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        TypeUse use = new TypeUse(opcode, type);
                        result.types.add(use);
                        result.events.add(use);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name,
                                               String fieldDescriptor) {
                        result.events.add(new FieldAccess(
                                opcode, owner, name, fieldDescriptor
                        ));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                                                String callDescriptor, boolean isInterface) {
                        Call call = new Call(opcode, owner, name, callDescriptor);
                        result.calls.add(call);
                        result.events.add(call);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        result.events.add(new Constant(value));
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertTrue(result.found, "Missing method " + methodName + descriptor);
        return result;
    }

    private static byte[] classpathEntry(String name) throws IOException {
        try (InputStream input = Minecraft26ContainerSeamContractTest.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            assertNotNull(input, "Missing classpath entry: " + name);
            return input.readAllBytes();
        }
    }

    private record Member(String name, String descriptor, int access) {
    }

    private record Call(int opcode, String owner, String name, String descriptor) {
    }

    private record FieldAccess(int opcode, String owner, String name, String descriptor) {
    }

    private record TypeUse(int opcode, String type) {
    }

    private record VariableUse(int opcode, int variable) {
    }

    private record Instruction(int opcode) {
    }

    private record Constant(Object value) {
    }

    private static final class ClassShape {
        private final Set<Member> members = new HashSet<>();
    }

    private static final class MethodCode {
        private boolean found;
        private final List<Object> events = new ArrayList<>();
        private final List<Integer> opcodes = new ArrayList<>();
        private final List<TypeUse> types = new ArrayList<>();
        private final List<Call> calls = new ArrayList<>();
    }
}

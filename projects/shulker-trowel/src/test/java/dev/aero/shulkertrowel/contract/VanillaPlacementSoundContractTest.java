package dev.aero.shulkertrowel.contract;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaPlacementSoundContractTest {
    private static final String PLAY_SOUND_DESCRIPTOR =
            "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;" +
                    "Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V";

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void minecraft26_2BlockItemOwnsExactlyOneCanonicalSuccessfulSoundCall() throws IOException {
        MethodNode place = blockItemPlaceMethod();
        List<AbstractInsnNode> instructions = instructionList(place);
        List<MethodInsnNode> soundCalls = instructions.stream()
                .filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast)
                .filter(call -> call.owner.equals("net/minecraft/world/level/Level"))
                .filter(call -> call.name.equals("playSound"))
                .filter(call -> call.desc.equals(PLAY_SOUND_DESCRIPTOR))
                .toList();

        assertEquals(1, soundCalls.size());
        int soundIndex = instructions.indexOf(soundCalls.getFirst());
        assertTrue(indexOfCall(instructions, "net/minecraft/world/level/Level", "getBlockState") < soundIndex);
        assertTrue(indexOfCall(instructions, "net/minecraft/world/level/block/state/BlockState", "getSoundType") < soundIndex);
        assertTrue(indexOfCall(instructions, "net/minecraft/world/item/BlockItem", "getPlaceSound") < soundIndex);
        assertTrue(indexOfCall(instructions, "net/minecraft/world/level/block/SoundType", "getVolume") < soundIndex);
        assertTrue(indexOfCall(instructions, "net/minecraft/world/level/block/SoundType", "getPitch") < soundIndex);
        assertTrue(hasFieldBefore(instructions, soundIndex, "net/minecraft/sounds/SoundSource", "BLOCKS"));
        assertTrue(hasOpcodeBefore(instructions, soundIndex, Opcodes.FADD));
        assertTrue(hasOpcodeBefore(instructions, soundIndex, Opcodes.FDIV));
        assertTrue(hasOpcodeBefore(instructions, soundIndex, Opcodes.FMUL));
        assertTrue(hasFloatBefore(instructions, soundIndex, 0.8F));
    }

    @Test
    void failedVanillaPlacementReturnsBeforeTheOnlySoundCall() throws IOException {
        List<AbstractInsnNode> instructions = instructionList(blockItemPlaceMethod());
        int soundIndex = indexOfCall(
                instructions,
                "net/minecraft/world/level/Level",
                "playSound",
                PLAY_SOUND_DESCRIPTOR
        );
        List<Integer> failReturns = new ArrayList<>();

        for (int index = 0; index < instructions.size(); index++) {
            AbstractInsnNode instruction = instructions.get(index);
            if (instruction instanceof FieldInsnNode field &&
                    field.owner.equals("net/minecraft/world/InteractionResult") &&
                    field.name.equals("FAIL")) {
                failReturns.add(index);
            }
        }

        assertTrue(failReturns.size() >= 4);
        assertTrue(failReturns.stream().allMatch(index -> index < soundIndex));
        assertTrue(indexOfFieldAfter(instructions, soundIndex, "net/minecraft/world/InteractionResult", "SUCCESS") > soundIndex);
    }

    @Test
    void stoneAndOakPlanksExposeDistinctActualStatePlacementSounds() {
        SoundType stone = Blocks.STONE.defaultBlockState().getSoundType();
        SoundType wood = Blocks.OAK_PLANKS.defaultBlockState().getSoundType();

        assertSame(SoundType.STONE.getPlaceSound(), stone.getPlaceSound());
        assertSame(SoundType.WOOD.getPlaceSound(), wood.getPlaceSound());
        assertNotEquals(stone.getPlaceSound(), wood.getPlaceSound());
        assertEquals((stone.getVolume() + 1.0F) / 2.0F, canonicalVolume(stone));
        assertEquals(stone.getPitch() * 0.8F, canonicalPitch(stone));
        assertEquals((wood.getVolume() + 1.0F) / 2.0F, canonicalVolume(wood));
        assertEquals(wood.getPitch() * 0.8F, canonicalPitch(wood));
        assertEquals(SoundSource.BLOCKS, SoundSource.valueOf("BLOCKS"));
    }

    private static float canonicalVolume(SoundType soundType) {
        return (soundType.getVolume() + 1.0F) / 2.0F;
    }

    private static float canonicalPitch(SoundType soundType) {
        return soundType.getPitch() * 0.8F;
    }

    private static MethodNode blockItemPlaceMethod() throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream input = BlockItem.class.getResourceAsStream("BlockItem.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node.methods.stream()
                .filter(method -> method.name.equals("place"))
                .filter(method -> method.desc.equals(
                        "(Lnet/minecraft/world/item/context/BlockPlaceContext;)" +
                                "Lnet/minecraft/world/InteractionResult;"
                ))
                .findFirst()
                .orElseThrow();
    }

    private static List<AbstractInsnNode> instructionList(MethodNode method) {
        List<AbstractInsnNode> result = new ArrayList<>();
        method.instructions.forEach(result::add);
        return result;
    }

    private static int indexOfCall(List<AbstractInsnNode> instructions, String owner, String name) {
        for (int index = 0; index < instructions.size(); index++) {
            if (instructions.get(index) instanceof MethodInsnNode call &&
                    call.owner.equals(owner) && call.name.equals(name)) {
                return index;
            }
        }
        return -1;
    }

    private static int indexOfCall(
            List<AbstractInsnNode> instructions,
            String owner,
            String name,
            String descriptor
    ) {
        for (int index = 0; index < instructions.size(); index++) {
            if (instructions.get(index) instanceof MethodInsnNode call &&
                    call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(descriptor)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean hasFieldBefore(
            List<AbstractInsnNode> instructions,
            int limit,
            String owner,
            String name
    ) {
        for (int index = 0; index < limit; index++) {
            if (instructions.get(index) instanceof FieldInsnNode field &&
                    field.owner.equals(owner) && field.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static int indexOfFieldAfter(
            List<AbstractInsnNode> instructions,
            int start,
            String owner,
            String name
    ) {
        for (int index = start + 1; index < instructions.size(); index++) {
            if (instructions.get(index) instanceof FieldInsnNode field &&
                    field.owner.equals(owner) && field.name.equals(name)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean hasOpcodeBefore(List<AbstractInsnNode> instructions, int limit, int opcode) {
        for (int index = 0; index < limit; index++) {
            if (instructions.get(index) instanceof InsnNode instruction && instruction.getOpcode() == opcode) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFloatBefore(List<AbstractInsnNode> instructions, int limit, float expected) {
        for (int index = 0; index < limit; index++) {
            if (instructions.get(index) instanceof LdcInsnNode instruction &&
                    instruction.cst instanceof Float value && Float.compare(value, expected) == 0) {
                return true;
            }
        }
        return false;
    }
}

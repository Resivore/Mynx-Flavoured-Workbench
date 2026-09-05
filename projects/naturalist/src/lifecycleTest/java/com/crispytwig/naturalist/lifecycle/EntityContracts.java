package com.crispytwig.naturalist.lifecycle;

import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DynamicTest;
import org.objectweb.asm.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

final class EntityContracts {
    static Stream<DynamicTest> roster(RegistryAccess.Frozen registries) {
        var types = BuiltInRegistries.ENTITY_TYPE.stream().filter(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals("naturalist")).toList();
        assertEquals(51, types.size());
        return types.stream().map(type -> DynamicTest.dynamicTest(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), () -> inspect(type, registries)));
    }

    private static void inspect(EntityType<?> type, RegistryAccess.Frozen registries) throws Exception {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getServer()).thenReturn(mock(net.minecraft.server.MinecraftServer.class, RETURNS_DEEP_STUBS));
        when(level.registryAccess()).thenReturn(registries);
        when(level.enabledFeatures()).thenReturn(FeatureFlags.VANILLA_SET);
        when(level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
        when(level.getFluidState(any())).thenReturn(Fluids.EMPTY.defaultFluidState());
        when(level.getHeight()).thenReturn(384);
        when(level.getMinY()).thenReturn(-64);
        when(level.getRandom()).thenReturn(net.minecraft.util.RandomSource.create(1234));
        var entity = assertDoesNotThrow(() -> type.create(level, EntitySpawnReason.LOAD));
        assertNotNull(entity);
        entity.setId(1);
        if (!(entity instanceof LivingEntity living)) {
            assertTrue(Set.of("duck_egg", "dirt_trail", "carried_food").contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath()));
            return;
        }
        assertTrue(DefaultAttributes.hasSupplier(type), "No registered supplier");
        Set<String> required = new TreeSet<>();
        auditFamily(entity.getClass(), required, true);
        if (entity instanceof Mob mob) {
            for (String selectorName : List.of("goalSelector", "targetSelector")) {
                var field = Mob.class.getDeclaredField(selectorName);
                field.setAccessible(true);
                GoalSelector selector = (GoalSelector) field.get(mob);
                for (var wrapped : selector.getAvailableGoals()) {
                    Goal goal = wrapped.getGoal();
                    auditFamily(goal.getClass(), required, false);
                    if (goal instanceof MeleeAttackGoal) required.add("ATTACK_DAMAGE");
                    if (goal instanceof MeleeAttackGoal || goal instanceof net.minecraft.world.entity.ai.goal.target.TargetGoal
                            || goal instanceof FloatGoal || goal instanceof BreedGoal || goal instanceof FollowParentGoal) {
                        if (assertDoesNotThrow(goal::canUse)) {
                            goal.start();
                            assertDoesNotThrow(goal::canContinueToUse);
                            goal.stop();
                        }
                    }
                    if (goal instanceof TemptGoal) {
                        required.add("TEMPT_RANGE");
                        assertTrue(living.getAttributes().hasAttribute(Attributes.TEMPT_RANGE), "Missing TEMPT_RANGE for " + goal.getClass());
                        assertEquals(10.0, living.getAttributeValue(Attributes.TEMPT_RANGE));
                        assertFalse(goal.canUse()); // No player; still executes the actual 26.2 range query.
                        var player = mock(net.minecraft.world.entity.player.Player.class);
                        when(player.position()).thenReturn(net.minecraft.world.phys.Vec3.ZERO);
                        when(level.getNearestPlayer(any(net.minecraft.world.entity.ai.targeting.TargetingConditions.class), same(mob))).thenReturn(player);
                        if (goal.canUse()) {
                            goal.start();
                            assertTrue(goal.canContinueToUse());
                            goal.stop();
                        }
                        when(level.getNearestPlayer(any(net.minecraft.world.entity.ai.targeting.TargetingConditions.class), same(mob))).thenReturn(null);
                    }
                }
            }
            auditFamily(mob.getMoveControl().getClass(), required, false);
            if (mob.getMoveControl() instanceof FlyingMoveControl) required.add("FLYING_SPEED");
        }
        for (String name : required) {
            @SuppressWarnings("unchecked") Holder<Attribute> attribute = (Holder<Attribute>) Attributes.class.getField(name).get(null);
            assertTrue(living.getAttributes().hasAttribute(attribute), "Missing " + name + " queried by installed goal/entity/control");
        }
        System.out.println(BuiltInRegistries.ENTITY_TYPE.getKey(type) + " constructor/goals OK; required=" + required);
    }

    private static void auditFamily(Class<?> type, Set<String> attributes, boolean nested) throws Exception {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            // Vanilla Mob/LivingEntity expose optional capabilities (e.g. attack damage),
            // so audit installed goal/control hierarchies and Naturalist's own family only.
            if (nested && !current.getName().startsWith("com.crispytwig.naturalist.")) break;
            auditBytecode(current, attributes);
            if (nested) for (Class<?> child : current.getDeclaredClasses()) auditFamily(child, attributes, false);
        }
    }

    private static void auditBytecode(Class<?> type, Set<String> attributes) throws Exception {
        try (var input = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            if (input == null) return;
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        String attribute;
                        @Override public void visitFieldInsn(int opcode, String owner, String field, String descriptor) {
                            if (opcode == Opcodes.GETSTATIC && owner.equals("net/minecraft/world/entity/ai/attributes/Attributes")) attribute = field;
                        }
                        @Override public void visitMethodInsn(int opcode, String owner, String method, String descriptor, boolean iface) {
                            if (attribute != null && Set.of("getAttribute", "getAttributeValue", "getAttributeBaseValue").contains(method)
                                    && !descriptor.contains(";D)")) attributes.add(attribute);
                            if (method.equals("doHurtTarget")) attributes.add("ATTACK_DAMAGE");
                            attribute = null;
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
    }
}

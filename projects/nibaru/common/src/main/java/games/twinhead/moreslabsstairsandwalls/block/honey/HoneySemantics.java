package games.twinhead.moreslabsstairsandwalls.block.honey;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Geometry-neutral Honey callbacks with side contact resolved against the supplied collision shape. */
public final class HoneySemantics {
    private static final double CONTACT_EPSILON = 1.0E-4;

    private HoneySemantics() {}

    public static void fallOn(Level level, Entity entity, double fallDistance, SoundType soundType) {
        entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        if (!level.isClientSide()) level.broadcastEntityEvent(entity, EntityEvent.HONEY_JUMP);
        if (entity.causeFallDamage(fallDistance, 0.2F, level.damageSources().fall())) {
            entity.playSound(soundType.getFallSound(), soundType.getVolume() * 0.5F,
                    soundType.getPitch() * 0.75F);
        }
    }

    public static boolean applySlideIfEligible(BlockState state, Level level, BlockPos pos,
            Entity entity, VoxelShape collisionShape) {
        if (!isSliding(pos, entity, collisionShape)) return false;
        triggerAdvancement(state, entity);
        updateSlidingVelocity(entity);
        addCollisionEffects(level, entity);
        return true;
    }

    public static boolean isSliding(BlockPos pos, Entity entity, VoxelShape collisionShape) {
        if (entity.onGround() || entity.getDeltaMovement().y >= -0.08) return false;
        AABB entityBox = entity.getBoundingBox();
        for (AABB local : collisionShape.toAabbs()) {
            AABB occupied = local.move(pos);
            if (entity.getY() > occupied.maxY - CONTACT_EPSILON) continue;
            boolean overlapsX = entityBox.maxX > occupied.minX + CONTACT_EPSILON
                    && entityBox.minX < occupied.maxX - CONTACT_EPSILON;
            boolean overlapsZ = entityBox.maxZ > occupied.minZ + CONTACT_EPSILON
                    && entityBox.minZ < occupied.maxZ - CONTACT_EPSILON;
            boolean besideX = Math.abs(entityBox.maxX - occupied.minX) <= CONTACT_EPSILON
                    || Math.abs(entityBox.minX - occupied.maxX) <= CONTACT_EPSILON;
            boolean besideZ = Math.abs(entityBox.maxZ - occupied.minZ) <= CONTACT_EPSILON
                    || Math.abs(entityBox.minZ - occupied.maxZ) <= CONTACT_EPSILON;
            if ((besideX && overlapsZ) || (besideZ && overlapsX)) return true;
        }
        return false;
    }

    private static void triggerAdvancement(BlockState state, Entity entity) {
        if (entity instanceof ServerPlayer player && entity.level().getGameTime() % 20L == 0L)
            CriteriaTriggers.HONEY_BLOCK_SLIDE.trigger(player, state);
    }

    public static void updateSlidingVelocity(Entity entity) {
        Vec3 movement = entity.getDeltaMovement();
        if (movement.y < -0.13) {
            double factor = -0.05 / movement.y;
            entity.setDeltaMovement(new Vec3(movement.x * factor, -0.05, movement.z * factor));
        } else {
            entity.setDeltaMovement(new Vec3(movement.x, -0.05, movement.z));
        }
        entity.resetFallDistance();
    }

    private static void addCollisionEffects(Level level, Entity entity) {
        if (!(entity instanceof LivingEntity || entity instanceof AbstractMinecart
                || entity instanceof PrimedTnt || entity instanceof Boat)) return;
        if (level.getRandom().nextInt(5) == 0)
            entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        if (!level.isClientSide() && level.getRandom().nextInt(5) == 0)
            level.broadcastEntityEvent(entity, EntityEvent.HONEY_SLIDE);
    }
}

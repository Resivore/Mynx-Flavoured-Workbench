package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Visual-only float. It never hooks entities, rolls loot, grants XP, drops items, or saves. */
public final class FishingFloat extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(FishingFloat.class, EntityDataSerializers.INT);

    public FishingFloat(EntityType<? extends FishingFloat> type, Level level) { super(type, level); }

    public FishingFloat(ServerLevel level, Villager owner, BlockPos water) {
        this(VillagerWorkRoutines.FISHING_FLOAT, level);
        this.entityData.set(OWNER_ID, owner.getId());
        setPos(water.getX() + 0.5, water.getY() + 0.78, water.getZ() + 0.5);
    }

    public Entity owner() { return level().getEntity(entityData.get(OWNER_ID)); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(OWNER_ID, -1); }

    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            Entity owner = owner();
            BlockPos water = blockPosition();
            if (!(owner instanceof Villager) || !owner.isAlive() || owner.distanceToSqr(this) > 18 * 18
                    || tickCount > 800 || !server.hasChunkAt(water)
                    || !server.getFluidState(water).is(FluidTags.WATER)) discard();
        }
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}

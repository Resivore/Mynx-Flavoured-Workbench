package dev.resivore.villagerwork;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A synchronized, non-persistent ownership marker for the Shepherd's custom shears layer. */
public final class ShearingToolMarker extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(ShearingToolMarker.class, EntityDataSerializers.INT);

    public ShearingToolMarker(EntityType<? extends ShearingToolMarker> type, Level level) { super(type, level); }

    public ShearingToolMarker(ServerLevel level, Villager owner) {
        this(VillagerWorkRoutines.SHEARING_TOOL_MARKER, level);
        entityData.set(OWNER_ID, owner.getId());
        setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    public Entity owner() { return level().getEntity(entityData.get(OWNER_ID)); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(OWNER_ID, -1); }

    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel && (!(owner() instanceof Villager owner) || !owner.isAlive() || tickCount > 400))
            discard();
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}

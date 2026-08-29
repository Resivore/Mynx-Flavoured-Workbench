package com.crispytwig.naturalist.server.entity.misc;

import com.crispytwig.naturalist.server.entity.mob.Duck;
import com.crispytwig.naturalist.registry.NaturalistEntityTypes;
import com.crispytwig.naturalist.registry.NaturalistRegistry;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class ThrownDuckEgg extends ThrowableItemProjectile {
    public ThrownDuckEgg(EntityType<ThrownDuckEgg> thrownDuckEggEntityType, Level level) {
        super(thrownDuckEggEntityType, level);
    }

    public ThrownDuckEgg(Level level, LivingEntity livingEntity) {
        super(NaturalistEntityTypes.DUCK_EGG.get(), livingEntity, level, new ItemStack(NaturalistRegistry.DUCK_EGG.get()));
    }

    public ThrownDuckEgg(@NotNull Level level, double d, double e, double f) {
        super(NaturalistEntityTypes.DUCK_EGG.get(), d, e, f, level, new ItemStack(NaturalistRegistry.DUCK_EGG.get()));
    }

    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ItemStack particleStack = this.getItem();
            if (particleStack.isEmpty()) {
                return;
            }
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(particleStack));
            for(int i = 0; i < 8; ++i) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), ((double)this.random.nextFloat() - 0.5) * 0.08, ((double)this.random.nextFloat() - 0.5) * 0.08, ((double)this.random.nextFloat() - 0.5) * 0.08);
            }
        }

    }

    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(result.getEntity().damageSources().thrown(this, this.getOwner()), 0.0F);
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            if (this.random.nextInt(8) == 0) {
                int i = 1;
                if (this.random.nextInt(32) == 0) {
                    i = 4;
                }

                for (int j = 0; j < i; ++j) {
                    Duck duck = NaturalistEntityTypes.DUCK.get().create(this.level(), EntitySpawnReason.TRIGGERED);
                    assert duck != null;
                    duck.setAge(-24000);
                    duck.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                    this.level().addFreshEntity(duck);
                }
            }
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return NaturalistRegistry.DUCK_EGG.get();
    }
}

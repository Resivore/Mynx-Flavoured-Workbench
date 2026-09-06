package com.yungnickyoung.minecraft.ribbits.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.yungnickyoung.minecraft.ribbits.entity.goal.WanderingRibbitMoveToTargetGoal;
import com.yungnickyoung.minecraft.ribbits.entity.trade.WanderingRibbitTradeProviders;
import com.yungnickyoung.minecraft.ribbits.entity.trade.WanderingRibbitTradeSnapshot;
import com.yungnickyoung.minecraft.ribbits.module.SoundModule;
import com.yungnickyoung.minecraft.ribbits.world.spawn.NaturalistSnailCompanions;
import com.yungnickyoung.minecraft.ribbits.world.spawn.WanderingRibbitScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** A dedicated profession-free, non-restocking Ribbit merchant. */
public final class WanderingRibbitEntity extends AbstractVillager implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenPlay("walk");
    private static final long UNINITIALIZED_TRADE_SEED = Long.MIN_VALUE;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private long tradeSeed = UNINITIALIZED_TRADE_SEED;
    @Nullable
    private WanderingRibbitTradeSnapshot tradeSnapshot;
    @Nullable
    private BlockPos wanderTarget;

    private boolean schedulerManaged;
    private long leaseGeneration = -1L;
    private long leaseExpiry = -1L;
    @Nullable
    private net.minecraft.resources.ResourceKey<Level> leaseDimension;
    private boolean naturalistCompanionsInitialized;
    private List<UUID> naturalistCompanionUuids = List.of();

    public WanderingRibbitEntity(EntityType<? extends WanderingRibbitEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createWanderingRibbitAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.125D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(3, new WanderingRibbitMoveToTargetGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public boolean canRestock() {
        return false;
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // Wandering Ribbit offers intentionally grant no merchant or player XP.
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        if (tradeSeed == UNINITIALIZED_TRADE_SEED) {
            tradeSeed = this.getRandom().nextLong();
        }
        WanderingRibbitTradeProviders.Materialization materialization =
                WanderingRibbitTradeProviders.materialize(level, this.blockPosition(), tradeSeed);
        this.offers.addAll(materialization.offers());
        this.tradeSnapshot = materialization.snapshot();
    }

    /** Forces server-only one-time materialization before a scheduler lease is committed. */
    public void materializeOffers(ServerLevel level) {
        if (level != this.level()) {
            throw new IllegalArgumentException("Offers must be materialized in the entity's current level");
        }
        if (this.getOffers().isEmpty() || tradeSnapshot == null) {
            throw new IllegalStateException("Wandering Ribbit native offers did not materialize");
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).is(Items.VILLAGER_SPAWN_EGG)
                && this.isAlive()
                && !this.isTrading()
                && !this.isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) {
                player.awardStat(Stats.TALKED_TO_VILLAGER);
            }
            if (!this.level().isClientSide()) {
                if (this.getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                this.setTradingPlayer(player);
                Component title = this.hasCustomName()
                        ? this.getDisplayName()
                        : Component.translatable("entity.ribbits.wandering_ribbit");
                this.openTradingScreen(player, title, 0);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (schedulerManaged && this.level() instanceof ServerLevel serverLevel) {
            WanderingRibbitScheduler.validateManagedEntity(serverLevel, this);
        }
    }

    /** Marks this exact inserted entity as the scheduler's tentative lease holder. */
    public void initializeSchedulerLease(
            long generation,
            long expiry,
            net.minecraft.resources.ResourceKey<Level> dimension,
            BlockPos target,
            long seed
    ) {
        if (this.schedulerManaged) {
            throw new IllegalStateException("Wandering Ribbit is already scheduler-managed");
        }
        this.schedulerManaged = true;
        this.leaseGeneration = generation;
        this.leaseExpiry = expiry;
        this.leaseDimension = dimension;
        this.wanderTarget = target.immutable();
        this.tradeSeed = seed;
    }

    public boolean hasValidTradingSession() {
        Player player = this.getTradingPlayer();
        return player != null
                && player.isAlive()
                && !player.isRemoved()
                && player.level() == this.level()
                && player.containerMenu instanceof MerchantMenu menu
                && menu.stillValid(player);
    }

    public boolean isSchedulerManaged() {
        return schedulerManaged;
    }

    public long getLeaseGeneration() {
        return leaseGeneration;
    }

    public long getLeaseExpiry() {
        return leaseExpiry;
    }

    @Nullable
    public net.minecraft.resources.ResourceKey<Level> getLeaseDimension() {
        return leaseDimension;
    }

    public void synchronizeLeaseExpiry(long authoritativeExpiry) {
        this.leaseExpiry = authoritativeExpiry;
    }

    /** Scheduler-only, one-time optional companion initialization. */
    public void initializeNaturalistCompanions(ServerLevel level) {
        if (!schedulerManaged || naturalistCompanionsInitialized) {
            return;
        }
        naturalistCompanionUuids = NaturalistSnailCompanions.createPair(level, this);
        naturalistCompanionsInitialized = true;
    }

    public void discardNaturalistCompanions(ServerLevel level) {
        NaturalistSnailCompanions.discardLoadedManagedCompanions(level, this.getUUID());
    }

    @Nullable
    public BlockPos getWanderTarget() {
        return wanderTarget;
    }

    public void setWanderTarget(@Nullable BlockPos target) {
        this.wanderTarget = target == null ? null : target.immutable();
    }

    public long getTradeSeed() {
        return tradeSeed;
    }

    @Nullable
    public WanderingRibbitTradeSnapshot getTradeSnapshot() {
        return tradeSnapshot;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (!this.level().isClientSide()) {
            this.getOffers();
        }
        super.addAdditionalSaveData(output);
        output.putLong("WanderingTradeSeed", tradeSeed);
        output.storeNullable("WanderingTradeSnapshot", WanderingRibbitTradeSnapshot.CODEC, tradeSnapshot);
        output.storeNullable("WanderTarget", BlockPos.CODEC, wanderTarget);
        output.putBoolean("SchedulerManaged", schedulerManaged);
        if (schedulerManaged) {
            output.putLong("LeaseGeneration", leaseGeneration);
            output.putLong("LeaseExpiry", leaseExpiry);
            output.storeNullable("LeaseDimension", Level.RESOURCE_KEY_CODEC, leaseDimension);
            output.putBoolean("NaturalistCompanionsInitialized", naturalistCompanionsInitialized);
            output.store("NaturalistCompanionUuids", UUIDUtil.CODEC.listOf(), naturalistCompanionUuids);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        tradeSeed = input.getLongOr("WanderingTradeSeed", UNINITIALIZED_TRADE_SEED);
        tradeSnapshot = input.read("WanderingTradeSnapshot", WanderingRibbitTradeSnapshot.CODEC).orElse(null);
        wanderTarget = input.read("WanderTarget", BlockPos.CODEC).orElse(null);
        schedulerManaged = input.getBooleanOr("SchedulerManaged", false);
        if (schedulerManaged) {
            leaseGeneration = input.getLongOr("LeaseGeneration", -1L);
            leaseExpiry = input.getLongOr("LeaseExpiry", -1L);
            leaseDimension = input.read("LeaseDimension", Level.RESOURCE_KEY_CODEC).orElse(null);
            naturalistCompanionsInitialized = input.getBooleanOr("NaturalistCompanionsInitialized", false);
            naturalistCompanionUuids = List.copyOf(input.read("NaturalistCompanionUuids",
                    UUIDUtil.CODEC.listOf()).orElse(List.of()));
        } else {
            leaseGeneration = -1L;
            leaseExpiry = -1L;
            leaseDimension = null;
            naturalistCompanionsInitialized = false;
            naturalistCompanionUuids = List.of();
        }
        this.setAge(Math.max(0, this.getAge()));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return !schedulerManaged && super.canUsePortal(allowPassengers);
    }

    @Nullable
    @Override
    public Entity teleport(TeleportTransition transition) {
        return schedulerManaged ? null : super.teleport(transition);
    }

    @Override
    public void die(DamageSource source) {
        if (schedulerManaged && this.level() instanceof ServerLevel serverLevel) {
            WanderingRibbitScheduler.releaseDestroyedLease(serverLevel, this);
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        boolean release = schedulerManaged && reason.shouldDestroy() && !this.isRemoved();
        super.remove(reason);
        if (release && this.level() instanceof ServerLevel serverLevel) {
            WanderingRibbitScheduler.releaseDestroyedLease(serverLevel, this);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundModule.ENTITY_RIBBIT_AMBIENT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundModule.ENTITY_RIBBIT_HURT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundModule.ENTITY_RIBBIT_DEATH.get();
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundModule.ENTITY_RIBBIT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean successful) {
        return SoundModule.ENTITY_RIBBIT_AMBIENT.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        super.playStepSound(pos, state);
        this.playSound(SoundModule.ENTITY_RIBBIT_STEP.get(), 1.0F, 1.0F);
    }

    private <E extends GeoAnimatable> PlayState animationPredicate(AnimationTest<E> state) {
        state.controller().setAnimation(state.isMoving() && !this.isInWater() ? WALK : IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<WanderingRibbitEntity>("controller", 5, this::animationPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}

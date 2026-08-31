package com.starfish_studios.bbb.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BrazierBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public BrazierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, true).setValue(WATERLOGGED, false));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ItemTags.SHOVELS) && state.getValue(LIT)) {
            dowse(player, level, pos, state);
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL_IMMEDIATE);
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(Items.FLINT_AND_STEEL) && !state.getValue(LIT)) {
            stack.hurtAndBreak(1, player, hand);
            light(level, pos, state, SoundEvents.FLINTANDSTEEL_USE);
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(Items.FIRE_CHARGE) && !state.getValue(LIT)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            light(level, pos, state, SoundEvents.FIRECHARGE_USE);
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static void light(Level level, BlockPos pos, BlockState state, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS,
                1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
        level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL_IMMEDIATE);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effects, boolean intersects) {
        if (state.getValue(LIT) && entity instanceof LivingEntity living
                && EnchantmentHelper.getEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FROST_WALKER), living) == 0) {
            entity.hurt(level.damageSources().inFire(), 3.0F);
        }
        super.entityInside(state, level, pos, entity, effects, intersects);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
        return defaultBlockState().setValue(WATERLOGGED, water).setValue(LIT, !water);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
        }
        if (random.nextInt(10) != 0) {
            double x = pos.getX() + 0.5;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
                    pos.getY() + 1.35,
                    z + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
                    0.0, 0.02, 0.0);
        }
    }

    public static void dowse(Entity entity, LevelAccessor level, BlockPos pos, BlockState state) {
        if (level.isClientSide() && level instanceof Level clientLevel) {
            for (int i = 0; i < 20; i++) {
                makeParticles(clientLevel, pos, state.getValue(LIT), true);
            }
        } else if (state.getValue(LIT)) {
            level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        level.gameEvent(entity, GameEvent.BLOCK_CHANGE, pos);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.getValue(WATERLOGGED) && fluidState.is(Fluids.WATER)) {
            if (state.getValue(LIT)) {
                if (!level.isClientSide()) {
                    level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                dowse(null, level, pos, state);
            }
            level.setBlock(pos, state.setValue(WATERLOGGED, true).setValue(LIT, false), Block.UPDATE_ALL);
            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
            return true;
        }
        return false;
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos pos = hit.getBlockPos();
        if (projectile.isOnFire() && projectile.mayInteract(server, pos)
                && !state.getValue(LIT) && !state.getValue(WATERLOGGED)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL_IMMEDIATE);
        } else if (projectile instanceof AbstractThrownPotion && projectile.mayInteract(server, pos)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL_IMMEDIATE);
            dowse(null, level, pos, state);
        }
    }

    public static void makeParticles(Level level, BlockPos pos, boolean signal, boolean extraSmoke) {
        RandomSource random = level.getRandom();
        SimpleParticleType smoke = signal ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
        level.addAlwaysVisibleParticle(smoke, true,
                pos.getX() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
                pos.getY() + random.nextDouble() + random.nextDouble() + 1,
                pos.getZ() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
                0.0, 0.07, 0.0);
        if (extraSmoke) {
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
                    pos.getY() + 0.4,
                    pos.getZ() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
                    0.0, 0.005, 0.0);
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED);
    }
}

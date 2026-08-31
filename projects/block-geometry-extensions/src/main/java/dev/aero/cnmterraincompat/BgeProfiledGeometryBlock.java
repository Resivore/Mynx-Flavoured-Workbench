package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.concretepowder.ConcretePowderSemantics;
import games.twinhead.moreslabsstairsandwalls.block.coral.CoralSemantics;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathSemantics;
import games.twinhead.moreslabsstairsandwalls.block.falling.FallingSemantics;
import games.twinhead.moreslabsstairsandwalls.block.honey.HoneySemantics;
import games.twinhead.moreslabsstairsandwalls.block.magma.MagmaSemantics;
import games.twinhead.moreslabsstairsandwalls.block.redstone.RedstoneSemantics;
import games.twinhead.moreslabsstairsandwalls.block.slime.SlimeSemantics;
import games.twinhead.moreslabsstairsandwalls.block.soulsand.SoulSandSemantics;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import games.twinhead.moreslabsstairsandwalls.block.translucent.IceGeometryBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Objects;

/** Shared canonical-material and water state for BGE-owned geometry blocks. */
public abstract class BgeProfiledGeometryBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final NibaruMaterialProfile materialProfile;

    protected BgeProfiledGeometryBlock(NibaruMaterialProfile materialProfile,
            BlockBehaviour.Properties properties) {
        super(properties);
        this.materialProfile = Objects.requireNonNull(materialProfile, "materialProfile");
    }

    public final NibaruMaterialProfile materialProfile() {
        return materialProfile;
    }

    protected abstract BgeGeometryRole geometryRole();

    /** Material-specific collision shell supplied by the concrete geometry. */
    protected abstract VoxelShape materialCollisionShape(BlockState state, boolean honey);

    /** Whether an equal neighboring state completely covers this state's boundary face. */
    protected boolean equalStateFaceCanCull(BlockState state, Direction direction) {
        return false;
    }

    /** Completes profile-owned placement lifecycle after geometry state selection. */
    protected final BlockState applyProfilePlacement(BlockState state, BlockPlaceContext context) {
        if (has(BehaviorCapability.PATH_CONVERSION)) {
            state = PathSemantics.placementState(state,
                    transition(MaterialTransition.Type.PATH_REVERSION).defaultBlockState(),
                    context.getLevel(), context.getClickedPos());
        }
        if (has(BehaviorCapability.CORAL_DEATH)) {
            CoralSemantics.scheduleIfDry(context.getLevel(), context.getLevel(),
                    context.getClickedPos(), this, context.getLevel().getRandom());
        }
        return state;
    }

    protected final boolean has(BehaviorCapability capability) {
        return materialProfile.capabilities().contains(capability);
    }

    protected static boolean isSpreadableSurface(NibaruMaterialProfile profile) {
        return profile.transitions().stream().anyMatch(transition ->
                transition.type() == MaterialTransition.Type.SPREADABLE_BASE);
    }

    protected final Block transition(MaterialTransition.Type type) {
        NibaruMaterialProfile target = materialProfile.transition(type)
                .flatMap(transition -> NibaruMaterialProfiles.fromFamily(transition.target()))
                .orElseThrow(() -> new IllegalStateException("Missing " + type + " target for "
                        + materialProfile.canonicalParentId()));
        return NibaruProviderAdapter.derived(target, geometryRole())
                .orElseThrow(() -> new IllegalStateException("Missing " + geometryRole()
                        + " transition target for " + materialProfile.canonicalParentId()));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
            RandomSource random) {
        if (has(BehaviorCapability.CONCRETE_HARDENING)
                && ConcretePowderSemantics.shouldHarden(level, pos, state)) {
            return ConcretePowderSemantics.hardenedState(state,
                    NibaruProviderAdapter.concreteHardening(this));
        }
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (has(BehaviorCapability.PATH_CONVERSION)) {
            PathSemantics.scheduleConversionIfNeeded(state, level, ticks, pos, direction);
        }
        if (has(BehaviorCapability.CORAL_DEATH)) {
            CoralSemantics.scheduleIfDry(level, ticks, pos, this, random);
        }
        if (has(BehaviorCapability.FALLING)) FallingSemantics.schedule(ticks, pos, this);
        if (has(BehaviorCapability.MAGMA_DAMAGE)) {
            MagmaSemantics.scheduleForWaterAbove(ticks, pos, this, direction, neighborState);
        }
        if (has(BehaviorCapability.SOUL_SAND_INTERACTION)) {
            SoulSandSemantics.scheduleIfNeeded(state, ticks, pos, this, direction, neighborState);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, level, pos, oldState, notify);
        if (has(BehaviorCapability.FALLING)) FallingSemantics.schedule(level, pos, this);
        if (has(BehaviorCapability.MAGMA_DAMAGE)) MagmaSemantics.schedule(level, pos, this);
        if (has(BehaviorCapability.SOUL_SAND_INTERACTION)) SoulSandSemantics.schedule(level, pos, this);
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level,
            BlockPos pos, RandomSource random) {
        if (has(BehaviorCapability.CONCRETE_HARDENING)
                && ConcretePowderSemantics.shouldHarden(level, pos, state)) {
            level.setBlock(pos, ConcretePowderSemantics.hardenedState(state,
                    NibaruProviderAdapter.concreteHardening(this)), Block.UPDATE_ALL);
            return;
        }
        if (has(BehaviorCapability.FALLING) && FallingSemantics.shouldFall(level, pos)) {
            FallingBlockEntity.fall(level, pos, state);
            return;
        }
        if (has(BehaviorCapability.CORAL_DEATH) && !CoralSemantics.survives(state, level, pos)) {
            level.setBlock(pos, CoralSemantics.deadState(state,
                    NibaruProviderAdapter.coralDeath(this)), Block.UPDATE_CLIENTS);
            return;
        }
        if (has(BehaviorCapability.PATH_CONVERSION)) {
            PathSemantics.revertIfObstructed(level, pos, state,
                    transition(MaterialTransition.Type.PATH_REVERSION).defaultBlockState());
        }
        if (has(BehaviorCapability.MAGMA_DAMAGE)) MagmaSemantics.updateBubbleColumn(level, pos, state);
        if (has(BehaviorCapability.SOUL_SAND_INTERACTION)) {
            SoulSandSemantics.updateBubbleColumn(state, level, pos);
        }
    }

    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level,
            BlockPos pos, RandomSource random) {
        if (isSpreadableSurface(materialProfile)) {
            GeometrySpreadableBehavior.randomTick(state, level, pos, random);
        }
        if (has(BehaviorCapability.ICE_MELTING)) {
            IceGeometryBehavior.randomTick(ModBlocks.ICE, state, level, pos, random);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (has(BehaviorCapability.FALLING)) FallingSemantics.animateDust(state, level, pos, random);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (has(BehaviorCapability.ICE_MELTING)) {
            IceGeometryBehavior.afterPlayerDestroy(ModBlocks.ICE, level, player, pos, tool);
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return has(BehaviorCapability.REDSTONE_POWER)
                ? RedstoneSemantics.isSignalSource() : super.isSignalSource(state);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return has(BehaviorCapability.REDSTONE_POWER)
                ? RedstoneSemantics.signal() : super.getSignal(state, level, pos, direction);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        if (has(BehaviorCapability.SOUL_SAND_INTERACTION)) return materialCollisionShape(state, false);
        if (has(BehaviorCapability.HONEY_INTERACTION)) return materialCollisionShape(state, true);
        return super.getCollisionShape(state, level, pos, context);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
        if (has(BehaviorCapability.HONEY_INTERACTION)) {
            HoneySemantics.fallOn(level, entity, distance, soundType);
            return;
        }
        if (has(BehaviorCapability.SLIME_INTERACTION) && !level.isClientSide()) {
            if (SlimeSemantics.handlesFall(entity)) {
                SlimeSemantics.suppressFallDamage(level, entity, distance);
                return;
            }
        }
        super.fallOn(level, state, pos, entity, distance);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (has(BehaviorCapability.MAGMA_DAMAGE)) MagmaSemantics.hurtIfNeeded(level, entity);
        if (has(BehaviorCapability.SLIME_INTERACTION)) SlimeSemantics.modifyHorizontalMovement(entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean intersects) {
        if (has(BehaviorCapability.HONEY_INTERACTION)) {
            HoneySemantics.applySlideIfEligible(state, level, pos, entity,
                    materialCollisionShape(state, true));
        }
        super.entityInside(state, level, pos, entity, effects, intersects);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        boolean specializedAdjacency = has(BehaviorCapability.TRANSLUCENT_ADJACENCY)
                || has(BehaviorCapability.ICE_MELTING)
                || has(BehaviorCapability.HONEY_INTERACTION)
                || has(BehaviorCapability.SLIME_INTERACTION);
        if (specializedAdjacency
                && NibaruProviderAdapter.cullsBoundTranslucent(this, state, neighbor)
                && (neighbor.getBlock() != this || !state.equals(neighbor)
                        || equalStateFaceCanCull(state, direction))) {
            return true;
        }
        return super.skipRendering(state, neighbor, direction);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return NibaruProviderAdapter.useComposedCapabilities(this, stack, state, level, pos, player, hand)
                .orElseGet(() -> super.useItemOn(stack, state, level, pos, player, hand, hit));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }
}

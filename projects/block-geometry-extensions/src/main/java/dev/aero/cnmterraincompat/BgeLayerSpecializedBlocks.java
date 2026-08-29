package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.concretepowder.ConcretePowderSemantics;
import games.twinhead.moreslabsstairsandwalls.block.coral.CoralSemantics;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathGeometry;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathSemantics;
import games.twinhead.moreslabsstairsandwalls.block.falling.FallingSemantics;
import games.twinhead.moreslabsstairsandwalls.block.honey.HoneySemantics;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafDistanceCarrier;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafSemantics;
import games.twinhead.moreslabsstairsandwalls.block.magma.MagmaSemantics;
import games.twinhead.moreslabsstairsandwalls.block.redstone.RedstoneSemantics;
import games.twinhead.moreslabsstairsandwalls.block.slime.SlimeSemantics;
import games.twinhead.moreslabsstairsandwalls.block.soulsand.SoulSandSemantics;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableGeometry;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import games.twinhead.moreslabsstairsandwalls.block.translucent.IceGeometryBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.function.Supplier;

/** Layer-only composition of provider-declared specialized material semantics. */
final class BgeLayerSpecializedBlocks {
    private BgeLayerSpecializedBlocks() {}

    static BgeLayerBlock create(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
        var capabilities = profile.capabilities();
        if (capabilities.contains(BehaviorCapability.LEAF_LIFECYCLE))
            return new Leaves(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.PATH_CONVERSION))
            return new Path(profile, properties, () -> transition(profile, MaterialTransition.Type.PATH_REVERSION));
        if (isSpreadableSurface(profile)) return new Spreadable(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.OXIDIZABLE)) return new Copper(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.CORAL_DEATH)) return new Coral(profile, properties);
        if (capabilities.contains(BehaviorCapability.CONCRETE_HARDENING)) return new ConcretePowder(profile, properties);
        if (capabilities.contains(BehaviorCapability.REDSTONE_POWER)) return new Redstone(profile, properties);
        if (capabilities.contains(BehaviorCapability.ICE_MELTING)) return new Ice(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.MAGMA_DAMAGE)) return new Magma(profile, properties);
        if (capabilities.contains(BehaviorCapability.SOUL_SAND_INTERACTION)) return new SoulSand(profile, properties);
        if (capabilities.contains(BehaviorCapability.HONEY_INTERACTION)) return new Honey(profile, properties);
        if (capabilities.contains(BehaviorCapability.SLIME_INTERACTION)) return new Slime(profile, properties);
        if (capabilities.contains(BehaviorCapability.FALLING)) return new Falling(profile, properties);
        if (capabilities.contains(BehaviorCapability.TRANSLUCENT_ADJACENCY))
            return new Translucent(profile, properties);
        return BgeLayerBlock.create(profile, properties);
    }

    private static boolean isSpreadableSurface(NibaruMaterialProfile profile) {
        return profile.transitions().stream().anyMatch(transition ->
                transition.type() == MaterialTransition.Type.SPREADABLE_BASE);
    }

    private static Block transition(NibaruMaterialProfile profile, MaterialTransition.Type type) {
        NibaruMaterialProfile target = profile.transition(type)
                .flatMap(transition -> NibaruMaterialProfiles.fromFamily(transition.target()))
                .orElseThrow(() -> new IllegalStateException("Missing " + type + " target for "
                        + profile.canonicalParentId()));
        return NibaruProviderAdapter.derived(target, DerivedGeometrySupport.Geometry.LAYER)
                .orElseThrow(() -> new IllegalStateException("Missing Layer transition target for "
                        + profile.canonicalParentId()));
    }

    @SuppressWarnings("deprecation")
    private static final class Leaves extends BgeLayerBlock implements LeafDistanceCarrier {
        private static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
        private static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;

        Leaves(NibaruMaterialProfile profile, Properties properties) {
            super(profile, properties);
            registerDefaultState(LeafSemantics.applyDefaultState(defaultBlockState()));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(DISTANCE, PERSISTENT);
        }

        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : LeafSemantics.applyPlayerPlacementState(state);
        }

        @Override public boolean isRandomlyTicking(BlockState state) {
            return LeafSemantics.isRandomlyTicking(state);
        }

        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            LeafSemantics.decayIfNeeded(state, level, pos);
        }

        @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            level.setBlock(pos, LeafSemantics.updateDistanceFromLogs(state, level, pos), 3);
        }

        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            LeafSemantics.scheduleDistanceUpdate(state, level, ticks, pos, this, neighbor);
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 1; }

        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            LeafSemantics.animateRainDrip(state, level, pos, random);
        }
    }

    private static final class Spreadable extends BgeLayerBlock implements SpreadableGeometry {
        Spreadable(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }

        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            SpreadableSemantics.randomTick(state, level, pos, random);
        }

        @Override public Exposure spreadableExposure(BlockState state, LevelReader level, BlockPos pos) {
            if (state.getValue(WATERLOGGED)) return Exposure.BLOCKED;
            if (state.getValue(FACING) == Direction.UP && state.getValue(LAYERS) < 4)
                return Exposure.EXPOSED;
            return Exposure.DEFAULT;
        }
    }

    private static final class Path extends BgeLayerBlock implements PathGeometry {
        private final Supplier<Block> dirtLayer;

        Path(NibaruMaterialProfile profile, Properties properties, Supplier<Block> dirtLayer) {
            super(profile, properties);
            this.dirtLayer = dirtLayer;
        }

        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState placed = super.getStateForPlacement(context);
            return placed == null ? null : PathSemantics.placementState(placed,
                    dirtLayer.get().defaultBlockState(), context.getLevel(), context.getClickedPos());
        }

        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                CollisionContext context) {
            int depth = state.getValue(LAYERS) * 4;
            return switch (state.getValue(FACING)) {
                case UP -> Block.box(0, 0, 0, 16, Math.min(depth, 15), 16);
                case DOWN -> Block.box(0, 16 - depth, 0, 16, 15, 16);
                case NORTH -> Block.box(0, 0, 16 - depth, 16, 15, 16);
                case SOUTH -> Block.box(0, 0, 0, 16, 15, depth);
                case EAST -> Block.box(0, 0, 0, depth, 15, 16);
                case WEST -> Block.box(16 - depth, 0, 0, 16, 15, 16);
            };
        }

        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            PathSemantics.scheduleConversionIfNeeded(state, level, ticks, pos, direction);
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            PathSemantics.revertIfObstructed(level, pos, state, dirtLayer.get().defaultBlockState());
        }

        @Override public boolean pathSurfaceRequiresClearAbove(BlockState state) {
            return state.getValue(FACING) != Direction.UP || state.getValue(LAYERS) == 4;
        }
    }

    private static final class Copper extends BgeLayerBlock
            implements ChangeOverTimeBlock<WeatheringCopper.WeatherState> {
        Copper(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override public WeatheringCopper.WeatherState getAge() { return NibaruProviderAdapter.copperAge(this); }
        @Override public Optional<BlockState> getNext(BlockState state) {
            return NibaruProviderAdapter.nextOxidation(this, state);
        }
        @Override public float getChanceModifier() {
            return getAge() == WeatheringCopper.WeatherState.UNAFFECTED ? 0.75F : 1.0F;
        }
        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            changeOverTime(state, level, pos, random);
        }
        @Override public boolean isRandomlyTicking(BlockState state) {
            return getAge() != WeatheringCopper.WeatherState.OXIDIZED;
        }
    }

    private static final class Coral extends BgeLayerBlock {
        Coral(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!CoralSemantics.survives(state, level, pos)) level.setBlock(pos,
                    CoralSemantics.deadState(state, NibaruProviderAdapter.coralDeath(this)), Block.UPDATE_CLIENTS);
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            CoralSemantics.scheduleIfDry(level, ticks, pos, this, random);
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            CoralSemantics.scheduleIfDry(context.getLevel(), context.getLevel(), context.getClickedPos(), this,
                    context.getLevel().getRandom());
            return super.getStateForPlacement(context);
        }
    }

    private static class Falling extends BgeLayerBlock implements Fallable {
        Falling(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean notify) {
            FallingSemantics.schedule(level, pos, this);
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            FallingSemantics.schedule(ticks, pos, this);
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
        @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (FallingSemantics.shouldFall(level, pos)) FallingBlockEntity.fall(level, pos, state);
        }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            FallingSemantics.animateDust(state, level, pos, random);
        }
    }

    private static final class ConcretePowder extends Falling {
        ConcretePowder(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        private Block hardened() { return NibaruProviderAdapter.concreteHardening(this); }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return ConcretePowderSemantics.shouldHarden(level, pos, state)
                    ? ConcretePowderSemantics.hardenedState(state, hardened())
                    : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
        @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (ConcretePowderSemantics.shouldHarden(level, pos, state)) {
                level.setBlock(pos, ConcretePowderSemantics.hardenedState(state, hardened()), Block.UPDATE_ALL);
                return;
            }
            super.tick(state, level, pos, random);
        }
        @Override public void onLand(Level level, BlockPos pos, BlockState falling, BlockState current,
                FallingBlockEntity entity) {
            if (ConcretePowderSemantics.shouldHarden(level, pos, current))
                level.setBlock(pos, ConcretePowderSemantics.hardenedState(falling, hardened()), Block.UPDATE_ALL);
        }
    }

    private static final class Redstone extends BgeLayerBlock {
        Redstone(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override public boolean isSignalSource(BlockState state) { return RedstoneSemantics.isSignalSource(); }
        @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return RedstoneSemantics.signal();
        }
    }

    private static final class Ice extends BgeLayerBlock {
        Ice(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            IceGeometryBehavior.randomTick(ModBlocks.ICE, state, level, pos, random);
        }
        @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                BlockEntity blockEntity, ItemStack tool) {
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
            IceGeometryBehavior.afterPlayerDestroy(ModBlocks.ICE, level, player, pos, tool);
        }
        @Override protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
            return cullsBoundTranslucent(state, neighbor, direction)
                    || super.skipRendering(state, neighbor, direction);
        }
    }

    @SuppressWarnings("deprecation")
    private static final class Magma extends BgeLayerBlock {
        Magma(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            MagmaSemantics.hurtIfNeeded(level, entity);
            super.stepOn(level, pos, state, entity);
        }
        @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean notify) {
            MagmaSemantics.schedule(level, pos, this);
            super.onPlace(state, level, pos, old, notify);
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            MagmaSemantics.scheduleForWaterAbove(ticks, pos, this, direction, neighbor);
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
        @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            MagmaSemantics.updateBubbleColumn(level, pos, state);
        }
    }

    private static final class SoulSand extends BgeLayerBlock {
        SoulSand(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                CollisionContext context) {
            int depth = state.getValue(LAYERS) * 4;
            return switch (state.getValue(FACING)) {
                case UP -> Block.box(0, 0, 0, 16, Math.min(depth, 14), 16);
                case DOWN -> Block.box(0, 16 - depth, 0, 16, 14, 16);
                case NORTH -> Block.box(0, 0, 16 - depth, 16, 14, 16);
                case SOUTH -> Block.box(0, 0, 0, 16, 14, depth);
                case EAST -> Block.box(0, 0, 0, depth, 14, 16);
                case WEST -> Block.box(16 - depth, 0, 0, 16, 14, 16);
            };
        }
        @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean notify) {
            SoulSandSemantics.schedule(level, pos, this);
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            SoulSandSemantics.scheduleIfNeeded(state, ticks, pos, this, direction, neighbor);
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
        @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            SoulSandSemantics.updateBubbleColumn(state, level, pos);
        }
    }

    @SuppressWarnings("deprecation")
    private static final class Honey extends BgeLayerBlock {
        Honey(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                CollisionContext context) {
            int depth = state.getValue(LAYERS) * 4;
            return switch (state.getValue(FACING)) {
                case UP -> Block.box(1, 0, 1, 15, Math.min(depth, 15), 15);
                case DOWN -> Block.box(1, 16 - depth, 1, 15, 15, 15);
                case NORTH -> Block.box(1, 0, Math.max(16 - depth, 1), 15, 15, 15);
                case SOUTH -> Block.box(1, 0, 1, 15, 15, Math.min(depth, 15));
                case EAST -> Block.box(1, 0, 1, Math.min(depth, 15), 15, 15);
                case WEST -> Block.box(Math.max(16 - depth, 1), 0, 1, 15, 15, 15);
            };
        }
        @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
            HoneySemantics.fallOn(level, entity, distance, soundType);
        }
        @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                InsideBlockEffectApplier effects, boolean intersects) {
            HoneySemantics.applySlideIfEligible(state, level, pos, entity,
                    getCollisionShape(state, level, pos, CollisionContext.empty()));
            super.entityInside(state, level, pos, entity, effects, intersects);
        }
        @Override protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
            return cullsBoundTranslucent(state, neighbor, direction)
                    || super.skipRendering(state, neighbor, direction);
        }
    }

    @SuppressWarnings("deprecation")
    private static final class Slime extends BgeLayerBlock {
        Slime(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
            if (!level.isClientSide()) {
                if (SlimeSemantics.handlesFall(entity)) SlimeSemantics.suppressFallDamage(level, entity, distance);
                else super.fallOn(level, state, pos, entity, distance);
            }
        }
        @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            SlimeSemantics.modifyHorizontalMovement(entity);
            super.stepOn(level, pos, state, entity);
        }
        @Override protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
            return cullsBoundTranslucent(state, neighbor, direction)
                    || super.skipRendering(state, neighbor, direction);
        }
    }

    private static final class Translucent extends BgeLayerBlock {
        Translucent(NibaruMaterialProfile profile, Properties properties) { super(profile, properties); }
        @Override protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
            return cullsBoundTranslucent(state, neighbor, direction)
                    || super.skipRendering(state, neighbor, direction);
        }
    }
}

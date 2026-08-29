package games.twinhead.moreslabsstairsandwalls.block.spreadable;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.dirt.DirtSlab;
import games.twinhead.moreslabsstairsandwalls.registry.ModTags;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class SpreadableSlab extends DirtSlab implements SimpleWaterloggedBlock, BonemealableBlock, SpreadableGeometry {

    public static final BooleanProperty SNOWY;
    public static final EnumProperty<SlabType> TYPE;
    public static final BooleanProperty WATERLOGGED;

    public SpreadableSlab(ModBlocks block, Properties settings) {
        super(block,settings);
    }

    public static boolean spreadCanSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos blockPos = pos.above();
        BlockState blockState = world.getBlockState(blockPos);

        if (state.getBlock() instanceof DirtSlab && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
            return !state.getValue(SlabBlock.WATERLOGGED);
        }

        if (blockState.is(Blocks.SNOW) && blockState.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }

        if (blockState.getFluidState().getAmount() == 8) {
            return false;
        }

        if (state.is(BlockTags.WALLS) && blockState.is(BlockTags.WALLS) && blockState.canOcclude()) {
            return false;
        }

        if (blockState.getBlock() instanceof SlabBlock && blockState.getValue(SlabBlock.TYPE) == SlabType.TOP){
            return true;
        }

        if (blockState.getBlock() instanceof StairBlock && blockState.getValue(StairBlock.HALF) == Half.TOP){
            return true;
        }

        int lightDampening = LightEngine.getLightDampeningInto(
                ModBlocks.GRASS_BLOCK.parentBlock.defaultBlockState(),
                blockState,
                Direction.UP,
                blockState.getLightDampening());
        return lightDampening < 15;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        if(state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) return false;
        return world.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.is(ModTags.GRASS_BLOCKS)) {
            growBoneMeal(world,random,pos);
        }
    }


    public static void growBoneMeal(ServerLevel world, RandomSource random, BlockPos pos) {
        BlockPos blockPos = pos.above();
        BlockState blockState = Blocks.SHORT_GRASS.defaultBlockState();
        Optional<Holder.Reference<PlacedFeature>> optional = world.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(VegetationPlacements.GRASS_BONEMEAL);

        label49:
        for(int i = 0; i < 128; ++i) {
            BlockPos blockPos2 = blockPos;

            for(int j = 0; j < i / 16; ++j) {
                blockPos2 = blockPos2.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                if (!world.getBlockState(blockPos2.below()).is(ModTags.GRASS_BLOCKS) || world.getBlockState(blockPos2).isCollisionShapeFullBlock(world, blockPos2)) {
                    continue label49;
                }
            }

            BlockState blockState2 = world.getBlockState(blockPos2);
            if (blockState2.is(blockState.getBlock()) && random.nextInt(10) == 0) {
                ((BonemealableBlock)blockState.getBlock()).performBonemeal(world, random, blockPos2, blockState2);
            }

            if (blockState2.isAir()) {
                if (random.nextInt(8) == 0) {
                    List<ConfiguredFeature<?, ?>> list = world.getBiome(blockPos2).value().getGenerationSettings().getBoneMealFeatures();
                    if (list.isEmpty()) {
                        continue;
                    }
                    list.get(0).place(world, world.getChunkSource().getGenerator(), random, blockPos2);
                } else {
                    if (optional.isEmpty()) {
                        continue;
                    }
                    optional.get().value().place(world, world.getChunkSource().getGenerator(), random, blockPos2);
                }
            }
        }

    }

    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        SpreadableSemantics.randomTick(state, world, pos, random);
    }

    @Override
    public Exposure spreadableExposure(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(SlabBlock.TYPE) != SlabType.BOTTOM) return Exposure.DEFAULT;
        return state.getValue(SlabBlock.WATERLOGGED) ? Exposure.BLOCKED : Exposure.EXPOSED;
    }


    public static void trySpread(ServerLevel world, Block parentBlock, BlockPos spreadPos) {
        BlockState newState = null;
        BlockState oldState = world.getBlockState(spreadPos);
        ModBlocks[] fromDirt = {ModBlocks.GRASS_BLOCK, ModBlocks.MYCELIUM};




        if (oldState.is(Blocks.DIRT)) {
            // target is a full dirt block
            for (ModBlocks modBlock : fromDirt) {
                if (parentBlock.equals(modBlock.parentBlock)) {
                    newState = modBlock.parentBlock.defaultBlockState().setValue(SNOWY, world.getBlockState(spreadPos.above()).is(Blocks.SNOW));
                }
            }
        } else {
            // luckily all properties except "SNOWY" can be copied using getStateWithProperties(...)
            for (ModBlocks.BlockType blockType :ModBlocks.BlockType.values()) {
                if (oldState.is(ModBlocks.DIRT.getBlock(blockType))) {
                    // target is dirt slab/stairs/wall
                    for (ModBlocks modBlock : fromDirt) {
                        if (parentBlock.equals(modBlock.parentBlock)) {
                            newState = modBlock.getBlock(blockType)
                                    .withPropertiesOf(world.getBlockState(spreadPos));
                            if (newState.hasProperty(BlockStateProperties.SNOWY))
                                newState = newState.setValue(BlockStateProperties.SNOWY, world.getBlockState(spreadPos.above()).is(Blocks.SNOW));
                        }
                    }
                }
            }
        }
        if (newState != null && spreadCanSurvive(newState, world, spreadPos) && !world.getFluidState(spreadPos.above()).is(FluidTags.WATER))
            world.setBlockAndUpdate(spreadPos, newState);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, WATERLOGGED, SNOWY);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos blockPos = ctx.getClickedPos();
        BlockState blockState = ctx.getLevel().getBlockState(blockPos);
        if (blockState.is(this)) {
            return blockState.setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false).setValue(SNOWY, ctx.getLevel().getBlockState(ctx.getClickedPos().above()).is(BlockTags.SNOW));
        } else {
            FluidState fluidState = ctx.getLevel().getFluidState(blockPos);
            BlockState blockState2 = this.defaultBlockState().setValue(TYPE, SlabType.BOTTOM).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER).setValue(SNOWY, ctx.getLevel().getBlockState(ctx.getClickedPos().above()).is(BlockTags.SNOW));
            Direction direction = ctx.getClickedFace();
            return direction != Direction.DOWN && (direction == Direction.UP || !(ctx.getClickLocation().y - (double)blockPos.getY() > 0.5)) ? blockState2 : blockState2.setValue(TYPE, SlabType.TOP);
        }
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        state = state.setValue(SNOWY, world.getBlockState(pos.above()).is(BlockTags.SNOW));


        return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    static {
        TYPE = BlockStateProperties.SLAB_TYPE;
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        SNOWY = BlockStateProperties.SNOWY;
    }
}

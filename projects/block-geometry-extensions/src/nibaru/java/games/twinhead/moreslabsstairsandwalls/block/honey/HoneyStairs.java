package games.twinhead.moreslabsstairsandwalls.block.honey;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.slime.SlimeSlab;
import games.twinhead.moreslabsstairsandwalls.block.translucent.TranslucentStairs;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class HoneyStairs extends TranslucentStairs {

    protected static final VoxelShape TOP_SHAPE = HoneySlab.TOP_SHAPE;
    protected static final VoxelShape BOTTOM_SHAPE = HoneySlab.BOTTOM_SHAPE;
    protected static final VoxelShape BOTTOM_NORTH_WEST_CORNER_SHAPE = Block.box(1.0, 0.0, 1.0, 8.0, 8.0, 8.0);
    protected static final VoxelShape BOTTOM_SOUTH_WEST_CORNER_SHAPE = Block.box(1.0, 0.0, 8.0, 8.0, 8.0, 15.0);
    protected static final VoxelShape TOP_NORTH_WEST_CORNER_SHAPE = Block.box(1.0, 7.0, 1.0, 8.0, 15.0, 8.0);
    protected static final VoxelShape TOP_SOUTH_WEST_CORNER_SHAPE = Block.box(1.0, 7.0, 8.0, 8.0, 15.0, 15.0);
    protected static final VoxelShape BOTTOM_NORTH_EAST_CORNER_SHAPE = Block.box(8.0, 0.0, 1.0, 15.0, 8.0, 8.0);
    protected static final VoxelShape BOTTOM_SOUTH_EAST_CORNER_SHAPE = Block.box(8.0, 0.0, 8.0, 15.0, 8.0, 15.0);
    protected static final VoxelShape TOP_NORTH_EAST_CORNER_SHAPE = Block.box(8.0, 7.0, 1.0, 15.0, 15.0, 8.0);
    protected static final VoxelShape TOP_SOUTH_EAST_CORNER_SHAPE = Block.box(8.0, 7.0, 8.0, 15.0, 15.0, 15.0);
    protected static final VoxelShape[] TOP_SHAPES = makeShapes(TOP_SHAPE, BOTTOM_NORTH_WEST_CORNER_SHAPE, BOTTOM_NORTH_EAST_CORNER_SHAPE, BOTTOM_SOUTH_WEST_CORNER_SHAPE, BOTTOM_SOUTH_EAST_CORNER_SHAPE);
    protected static final VoxelShape[] BOTTOM_SHAPES = makeShapes(BOTTOM_SHAPE, TOP_NORTH_WEST_CORNER_SHAPE, TOP_NORTH_EAST_CORNER_SHAPE, TOP_SOUTH_WEST_CORNER_SHAPE, TOP_SOUTH_EAST_CORNER_SHAPE);
    private static final int[] SHAPE_INDICES = new int[]{12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    public HoneyStairs(ModBlocks modBlocks,BlockState state, Properties settings) {
        super(modBlocks, state, settings);
    }

    @Override

    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape[] shapes;
        if (state.getValue(HALF) == Half.TOP) {
            shapes = TOP_SHAPES;
        } else {
            shapes = BOTTOM_SHAPES;
        }
        return shapes[SHAPE_INDICES[this.getShapeIndex(state)]];
    }

    private int getShapeIndex(BlockState state) {
        return state.getValue(SHAPE).ordinal() * 4 + state.getValue(FACING).get2DDataValue();
    }

    public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        HoneySemantics.fallOn(world, entity, fallDistance, this.soundType);
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean intersects) {
        HoneySemantics.applySlideIfEligible(state, world, pos, entity,
                getCollisionShape(state, world, pos, CollisionContext.empty()));
        super.entityInside(state, world, pos, entity, effectApplier, intersects);
    }

    /**
     * @return true if the block is sticky block which used for pull or push adjacent blocks (use by piston)
     */
    public boolean isStickyBlock(BlockState state) {
        return SlimeSlab.isStateHoney(state) || SlimeSlab.isStateSlime(state);
    }

    /**
     * Determines if this block can stick to another block when pushed by a piston.
     *
     * @param other Other block
     * @return True to link blocks
     */
    public boolean canStickTo(BlockState state, BlockState other) {
        if (SlimeSlab.isStateSlime(state) && SlimeSlab.isStateHoney(other)) return false;
        if (SlimeSlab.isStateSlime(other) && SlimeSlab.isStateHoney(state)) return false;
        return isStickyBlock(state) || isStickyBlock(other);
    }

}

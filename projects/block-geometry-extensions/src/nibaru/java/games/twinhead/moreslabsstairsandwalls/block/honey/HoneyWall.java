package games.twinhead.moreslabsstairsandwalls.block.honey;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseWall;
import games.twinhead.moreslabsstairsandwalls.block.slime.SlimeSlab;
import games.twinhead.moreslabsstairsandwalls.block.translucent.TranslucentWall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
@SuppressWarnings("deprecation")
public class HoneyWall extends TranslucentWall {

    public HoneyWall(ModBlocks modBlocks, Properties settings) {
        super(modBlocks, settings);
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

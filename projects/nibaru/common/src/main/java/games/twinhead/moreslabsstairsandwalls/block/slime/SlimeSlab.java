package games.twinhead.moreslabsstairsandwalls.block.slime;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.translucent.TranslucentSlab;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SlimeSlab extends TranslucentSlab {


    public SlimeSlab(ModBlocks block,Properties settings) {
        super(block, settings);
    }



    public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (!world.isClientSide()) {
            if (!SlimeSemantics.handlesFall(entity)) {
                super.fallOn(world, state, pos, entity, fallDistance);
            } else {
                SlimeSemantics.suppressFallDamage(world, entity, fallDistance);
            }
        }
    }

    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        SlimeSemantics.modifyHorizontalMovement(entity);
        super.stepOn(world, pos, state, entity);
    }


    public boolean isSlimeBlock(BlockState state) {
        return isStateSlime(state);
    }

    /**
     * @return true if the block is sticky block which used for pull or push adjacent blocks (use by piston)
     */
    public boolean isStickyBlock(BlockState state) {
        return isStateHoney(state) || isStateSlime(state);
    }

    /**
     * Determines if this block can stick to another block when pushed by a piston.
     *
     * @param other Other block
     * @return True to link blocks
     */
    public boolean canStickTo(BlockState state, BlockState other) {
        if (isStateSlime(state) && isStateHoney(other)) return false;
        if (isStateSlime(other) && isStateHoney(state)) return false;
        return isStickyBlock(state) || isStickyBlock(other);
    }


    public static boolean isStateHoney(BlockState state){
        return StickyMaterialSemantics.family(state) == StickyMaterialSemantics.Family.HONEY;
    }

    public static boolean isStateSlime(BlockState state){
        return StickyMaterialSemantics.family(state) == StickyMaterialSemantics.Family.SLIME;
    }


}

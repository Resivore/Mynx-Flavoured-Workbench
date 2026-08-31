package games.twinhead.moreslabsstairsandwalls.block.honey;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.slime.SlimeSlab;
import games.twinhead.moreslabsstairsandwalls.block.translucent.TranslucentSlab;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
@SuppressWarnings("deprecation")
public class HoneySlab extends TranslucentSlab {

    protected static final VoxelShape FULL_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);
    public static final VoxelShape BOTTOM_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 7.0, 15.0);
    public static final VoxelShape TOP_SHAPE = Block.box(1.0, 8.0, 1.0, 15.0, 15.0, 15.0);

    public HoneySlab(ModBlocks modBlocks, Properties settings) {
        super(modBlocks, settings);
    }

    public static boolean hasHoneyBlockEffects(Entity entity) {
        return entity instanceof LivingEntity || entity instanceof AbstractMinecart || entity instanceof PrimedTnt || entity instanceof Boat;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        SlabType slabType = state.getValue(TYPE);
        return switch (slabType) {
            case DOUBLE -> FULL_SHAPE;
            case TOP -> TOP_SHAPE;
            default -> BOTTOM_SHAPE;
        };
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
     * used on neoForge to determine if the block is sticky block which used for pull or push adjacent blocks (use by piston)
     * @return true if the block is sticky block which used for pull or push adjacent blocks (use by piston)
     */
    public boolean isStickyBlock(BlockState state) {
        return SlimeSlab.isStateHoney(state) || SlimeSlab.isStateSlime(state);
    }

    /**
     * Used on NeoForge to determine if this block can stick to another block when pushed by a piston.
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

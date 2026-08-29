package games.twinhead.moreslabsstairsandwalls.block.entity;

import games.twinhead.moreslabsstairsandwalls.registry.ModRegistry;
import games.twinhead.moreslabsstairsandwalls.mixin.FallingBlockEntityAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class FallingSlabBlockEntity extends FallingBlockEntity {

    public FallingSlabBlockEntity(EntityType<? extends FallingSlabBlockEntity> entityType, Level world) {
        super(entityType, world);
    }

    private FallingSlabBlockEntity(Level world, double x, double y, double z, BlockState state) {
        super(ModRegistry.getFallingSlabEntityType(), world);
        ((FallingBlockEntityAccessor) this).moreSlabsStairsAndWalls$setBlockState(state);
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.entityData.set(FallingBlockEntity.DATA_START_POS, this.blockPosition());
    }

    public static FallingSlabBlockEntity fall(Level world, BlockPos pos, BlockState state) {
        FallingSlabBlockEntity fallingBlockEntity = new FallingSlabBlockEntity(world, (double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5, state.hasProperty(BlockStateProperties.WATERLOGGED) ? (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, false) : state);
        world.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
        world.addFreshEntity(fallingBlockEntity);
        return fallingBlockEntity;
    }

    public void tick() {
        BlockState fallingState = this.getBlockState();
        if (fallingState.isAir()) {
            this.discard();
        } else {
            Block slabBlock = fallingState.getBlock();
            ++this.time;
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
            }

            this.move(MoverType.SELF, this.getDeltaMovement());
            if (!this.level().isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) this.level();
                BlockPos blockPos = this.blockPosition();
                boolean bl = fallingState.getBlock() instanceof ConcretePowderBlock;
                boolean bl2 = bl && this.level().getFluidState(blockPos).is(FluidTags.WATER);
                double d = this.getDeltaMovement().lengthSqr();
                if (bl && d > 1.0) {
                    BlockHitResult blockHitResult = this.level().clip(new ClipContext(new Vec3(this.xo, this.yo, this.zo), this.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, this));
                    if (blockHitResult.getType() != HitResult.Type.MISS && this.level().getFluidState(blockHitResult.getBlockPos()).is(FluidTags.WATER)) {
                        blockPos = blockHitResult.getBlockPos();
                        bl2 = true;
                    }
                }

                if (!this.onGround() && !bl2) {
                    if (this.time > 100 && (blockPos.getY() <= serverLevel.getMinY() || blockPos.getY() > serverLevel.getMaxY()) || this.time > 600) {
                        if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                            this.spawnAtLocation(serverLevel, slabBlock);
                        }

                        this.discard();
                    }
                } else {
                    BlockState blockState = this.level().getBlockState(blockPos);
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
                    if (!blockState.is(Blocks.MOVING_PISTON)) {
                        boolean bl3 = blockState.canBeReplaced(new DirectionalPlaceContext(this.level(), blockPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
                        boolean bl4 = FallingBlock.isFree(this.level().getBlockState(blockPos.below())) && (!bl || !bl2);
                        boolean bl5 = fallingState.canSurvive(this.level(), blockPos) && !bl4;
                        if (bl3 && bl5) {
                            if (fallingState.hasProperty(BlockStateProperties.WATERLOGGED) && this.level().getFluidState(blockPos).getType() == Fluids.WATER) {
                                fallingState = fallingState.setValue(BlockStateProperties.WATERLOGGED, true);
                                ((FallingBlockEntityAccessor) this).moreSlabsStairsAndWalls$setBlockState(fallingState);
                            }

                            if (this.level().setBlock(blockPos, fallingState, 3)) {
                                this.discard();

                                if (slabBlock instanceof LandingSlabBlock) {
                                    ((LandingSlabBlock)slabBlock).onLanding(this.level(), blockPos, fallingState, blockState, this);
                                }

                                if (this.blockData != null && fallingState.hasBlockEntity()) {
                                    BlockEntity blockEntity = this.level().getBlockEntity(blockPos);
                                    if (blockEntity != null) {
                                        CompoundTag nbtCompound = blockEntity.saveWithoutMetadata(this.level().registryAccess());
                                        for (String string : this.blockData.keySet()) {
                                            nbtCompound.put(string, this.blockData.get(string).copy());
                                        }
                                        blockEntity.loadCustomOnly(TagValueInput.create(
                                                ProblemReporter.DISCARDING,
                                                this.level().registryAccess(),
                                                nbtCompound));
                                        blockEntity.setChanged();
                                    }
                                }
                            } else if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                                this.discard();
                                this.callOnBrokenAfterFall(slabBlock, blockPos);
                            }
                        } else {
                            this.discard();
                            if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                                this.callOnBrokenAfterFall(slabBlock, blockPos);
                            }
                        }
                    }
                }
            }

            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }
    }
    public void callOnBrokenAfterFall(Block block, BlockPos pos) {
        if (block instanceof LandingSlabBlock) {
            ((LandingSlabBlock) block).onDestroyedOnLanding(this.level(), pos, this);
        }
    }

}

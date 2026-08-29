package dev.resivore.dragonbound.block;

import com.mojang.serialization.MapCodec;
import dev.resivore.dragonbound.DragonboundContent;
import dev.resivore.dragonbound.anchor.DragonboundAnchors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Fixed 16x3x16 return anchor. This deliberately is not a {@code SlabBlock} and therefore has no
 * slab type, merge, or waterlogging state.
 */
public final class DragonboundWaystoneBlock extends BaseEntityBlock {
    public static final float EXPLOSION_RESISTANCE = 3_600_000.0F;
    public static final double HEIGHT_PIXELS = 3.0D;
    public static final double HEIGHT_BLOCKS = HEIGHT_PIXELS / 16.0D;
    public static final VoxelShape SHAPE = box(0.0D, 0.0D, 0.0D, 16.0D, HEIGHT_PIXELS, 16.0D);
    public static final MapCodec<DragonboundWaystoneBlock> CODEC = simpleCodec(properties ->
            new DragonboundWaystoneBlock(properties, () -> DragonboundContent.WAYSTONE_BLOCK_ENTITY));

    private final Supplier<BlockEntityType<DragonboundWaystoneBlockEntity>> blockEntityType;

    public DragonboundWaystoneBlock(
            BlockBehaviour.Properties properties,
            Supplier<BlockEntityType<DragonboundWaystoneBlockEntity>> blockEntityType) {
        super(properties
                .sound(SoundType.STONE)
                .strength(3.0F, EXPLOSION_RESISTANCE)
                .pushReaction(PushReaction.BLOCK)
                .noLootTable());
        this.blockEntityType = Objects.requireNonNull(blockEntityType, "blockEntityType");
    }

    @Override
    protected MapCodec<? extends DragonboundWaystoneBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityType.get().create(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            DragonboundAnchors.bind(serverLevel, pos);
        }
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DragonboundWaystoneBlockEntity waystone) {
            ItemStack exactStack = stack.is(this.asItem()) ? stack : new ItemStack(this);
            waystone.setPlacedStack(exactStack);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston) {
        DragonboundAnchors.clearIfMatching(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            ItemStack tool) {
        // noLootTable() makes the vanilla path stats/exhaustion-only, preventing a second drop.
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (player.preventsBlockDrops()) {
            return;
        }

        ItemStack returned = blockEntity instanceof DragonboundWaystoneBlockEntity waystone
                ? waystone.copyPlacedStack()
                : ItemStack.EMPTY;
        if (returned.isEmpty() || !returned.is(this.asItem())) {
            returned = new ItemStack(this);
        }

        WaystoneLossProtection.returnToPlayerOrSpawnProtected(serverLevel, player, pos, returned);
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> dropConsumer) {
        // Deliberately immune: no removal and no explosion loot path.
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }
}

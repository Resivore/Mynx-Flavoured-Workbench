package com.crispytwig.naturalist.server.block;

import com.crispytwig.naturalist.server.entity.mob.Tortoise;
import com.crispytwig.naturalist.server.entity.variant.DataDrivenVariantAnimal;
import com.crispytwig.naturalist.registry.NaturalistEntityTypes;
import com.crispytwig.naturalist.registry.NaturalistSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class TortoiseEggBlock extends NaturalistEggBlock {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public TortoiseEggBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HATCH, 0)
                .setValue(EGGS, 1)
                .setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos,
                                                @NotNull BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        CompoundTag tag = new CompoundTag();
        tag.putInt(DataDrivenVariantAnimal.VARIANT_TAG, state.getValue(VARIANT));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    protected SoundEvent breakSound() {
        return NaturalistSoundEvents.TORTOISE_EGG_BREAK.get();
    }

    @Override
    protected SoundEvent crackSound() {
        return NaturalistSoundEvents.TORTOISE_EGG_CRACK.get();
    }

    @Override
    protected SoundEvent hatchSound() {
        return NaturalistSoundEvents.TORTOISE_EGG_HATCH.get();
    }

    @Override
    protected boolean isParentSpecies(Entity entity) {
        return entity instanceof Tortoise;
    }

    @Override
    protected boolean shouldUpdateHatchLevel(ServerLevel level, BlockPos pos, RandomSource random) {
        return shouldUpdateAtNaturalistDawn(level, pos);
    }

    @Override
    protected void spawnHatchlings(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int variant = state.getValue(VARIANT);
        for (int index = 0; index < state.getValue(EGGS); ++index) {
            level.levelEvent(2001, pos, Block.getId(state));
            spawnBaby(level, NaturalistEntityTypes.TORTOISE.get(), pos,
                    0.3D + index * 0.2D, 0.3D, 0.0F,
                    baby -> baby.setVariantByLegacyIndex(variant));
        }
    }

    @Override
    public void playerDestroy(@NotNull Level level, @NotNull Player player, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity blockEntity, @NotNull ItemStack stack) {
        super.playerDestroy(level, player, pos, state, blockEntity, stack);

        if (level instanceof ServerLevel serverLevel && stack.getItem() == Items.COMMAND_BLOCK) {
            int variant = state.getValue(VARIANT);
            int eggCount = state.getValue(EGGS);

            serverLevel.playSound(null, pos, NaturalistSoundEvents.TORTOISE_EGG_HATCH.get(),
                    SoundSource.BLOCKS, 0.7f, 0.9f + serverLevel.getRandom().nextFloat() * 0.2f);
            serverLevel.levelEvent(2001, pos, Block.getId(state));

            serverLevel.removeBlock(pos, false);

            for (int i = 0; i < eggCount; i++) {
                spawnBaby(serverLevel, NaturalistEntityTypes.TORTOISE.get(), pos,
                        0.3D + i * 0.2D, 0.3D, 0.0F,
                        baby -> baby.setVariantByLegacyIndex(variant));
            }
        }
    }
}

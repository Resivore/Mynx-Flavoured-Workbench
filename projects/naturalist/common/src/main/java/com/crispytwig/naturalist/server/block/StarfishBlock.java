package com.crispytwig.naturalist.server.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.MultifaceBlock;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class StarfishBlock extends MultifaceBlock {
    public static final MapCodec<StarfishBlock> CODEC = simpleCodec(StarfishBlock::new);

    public StarfishBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull MapCodec<StarfishBlock> codec() {
        return CODEC;
    }
}

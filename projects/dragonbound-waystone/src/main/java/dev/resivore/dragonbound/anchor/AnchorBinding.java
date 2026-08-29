package dev.resivore.dragonbound.anchor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Immutable identity for the one authoritative Dragonbound Waystone destination.
 */
public record AnchorBinding(ResourceKey<Level> dimension, BlockPos pos, long generation) {
    private static final Codec<Long> POSITIVE_GENERATION = Codec.LONG.validate(value -> value > 0L
            ? DataResult.success(value)
            : DataResult.error(() -> "Dragonbound anchor generation must be positive"));

    public static final Codec<AnchorBinding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(AnchorBinding::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(AnchorBinding::pos),
            POSITIVE_GENERATION.fieldOf("generation").forGetter(AnchorBinding::generation)
    ).apply(instance, AnchorBinding::new));

    public AnchorBinding {
        Objects.requireNonNull(dimension, "dimension");
        pos = Objects.requireNonNull(pos, "pos").immutable();
        if (generation <= 0L) {
            throw new IllegalArgumentException("generation must be positive");
        }
    }
}

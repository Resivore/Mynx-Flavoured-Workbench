package dev.resivore.matchabeacon.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

/** The two durable phases of one Beacon Kindling summon. */
public enum SummonPhase {
    APPROACH,
    VISIT;

    public static final Codec<SummonPhase> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(valueOf(value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown Beacon Kindling summon phase: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT));
}

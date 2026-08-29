package dev.resivore.dragonbound.anchor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.resivore.dragonbound.DragonboundWaystone;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Objects;
import java.util.Optional;

/**
 * Server-global persistent state for Dragonbound's single destination.
 *
 * <p>The generation counter never moves backwards and is intentionally retained when the
 * Waystone is carried as an item. A later placement therefore cannot accidentally validate a
 * channel captured from an older placement, even if it returns to the same coordinates.</p>
 */
public final class DragonboundAnchorData extends SavedData {
    private static final Codec<Long> NON_NEGATIVE_GENERATION = Codec.LONG.validate(value -> value >= 0L
            ? DataResult.success(value)
            : DataResult.error(() -> "Dragonbound last generation cannot be negative"));

    public static final Codec<DragonboundAnchorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NON_NEGATIVE_GENERATION.optionalFieldOf("last_generation", 0L)
                    .forGetter(DragonboundAnchorData::lastGeneration),
            AnchorBinding.CODEC.optionalFieldOf("active_anchor")
                    .forGetter(DragonboundAnchorData::active)
    ).apply(instance, DragonboundAnchorData::new));

    // Fabric patches SavedDataStorage to accept null for mod-owned data without a vanilla DFU schema.
    @SuppressWarnings("DataFlowIssue")
    public static final SavedDataType<DragonboundAnchorData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DragonboundWaystone.MOD_ID, "waystone_anchor"),
            DragonboundAnchorData::new,
            CODEC,
            null);

    private long lastGeneration;
    private Optional<AnchorBinding> active;

    public DragonboundAnchorData() {
        this(0L, Optional.empty());
    }

    private DragonboundAnchorData(long lastGeneration, Optional<AnchorBinding> active) {
        if (lastGeneration < 0L) {
            throw new IllegalArgumentException("lastGeneration cannot be negative");
        }
        this.active = Objects.requireNonNull(active, "active");
        this.lastGeneration = active
                .map(binding -> Math.max(lastGeneration, binding.generation()))
                .orElse(lastGeneration);
    }

    public Optional<AnchorBinding> active() {
        return active;
    }

    public long lastGeneration() {
        return lastGeneration;
    }

    /**
     * Replaces the sole authoritative binding and always assigns a fresh identity.
     */
    public AnchorBinding bind(ResourceKey<Level> dimension, BlockPos pos) {
        if (lastGeneration == Long.MAX_VALUE) {
            throw new IllegalStateException("Dragonbound anchor generation space is exhausted");
        }

        AnchorBinding replacement = new AnchorBinding(dimension, pos, ++lastGeneration);
        active = Optional.of(replacement);
        setDirty();
        return replacement;
    }

    /**
     * Clears only the physical location that is authoritative at the time of removal.
     */
    public boolean clearIfMatching(ResourceKey<Level> dimension, BlockPos pos) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");

        if (active.isEmpty()) {
            return false;
        }

        AnchorBinding current = active.orElseThrow();
        if (!current.dimension().equals(dimension) || !current.pos().equals(pos)) {
            return false;
        }

        active = Optional.empty();
        setDirty();
        return true;
    }

    /**
     * Clears only the exact captured identity, including its immutable generation.
     */
    public boolean clearIfMatching(AnchorBinding expected) {
        Objects.requireNonNull(expected, "expected");
        if (active.filter(expected::equals).isEmpty()) {
            return false;
        }

        active = Optional.empty();
        setDirty();
        return true;
    }

    public boolean matches(AnchorBinding expected) {
        Objects.requireNonNull(expected, "expected");
        return active.filter(expected::equals).isPresent();
    }
}

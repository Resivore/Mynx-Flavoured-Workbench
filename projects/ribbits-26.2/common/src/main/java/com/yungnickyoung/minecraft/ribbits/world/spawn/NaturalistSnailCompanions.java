package com.yungnickyoung.minecraft.ribbits.world.spawn;

import com.yungnickyoung.minecraft.ribbits.entity.WanderingRibbitEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Optional Naturalist bridge with no Naturalist classes in Ribbits' linkage graph. */
public final class NaturalistSnailCompanions {
    static final String TAG_PREFIX = "ribbits:wandering_ribbit_companion:";
    private static final Identifier SNAIL = Identifier.parse("naturalist:snail");

    private NaturalistSnailCompanions() {
    }

    public static List<UUID> createPair(ServerLevel level, WanderingRibbitEntity merchant) {
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(SNAIL).orElse(null);
        if (type == null) return List.of();
        List<Entity> created = new ArrayList<>(2);
        try {
            RandomSource random = level.getRandom();
            for (int index = 0; index < 2; index++) {
                int x = merchant.blockPosition().getX() + (index == 0 ? -2 : 2);
                int z = merchant.blockPosition().getZ() + (random.nextBoolean() ? 1 : -1);
                Entity snail = type.spawn(level, new net.minecraft.core.BlockPos(x,
                        merchant.blockPosition().getY(), z), EntitySpawnReason.EVENT);
                if (snail == null || snail.isRemoved()) throw new IllegalStateException("Naturalist Snail spawn failed");
                // Reflection keeps Naturalist out of Ribbits' linkage graph and makes API drift
                // an all-or-nothing optional-provider failure.
                snail.getClass().getMethod("setColor", DyeColor.class).invoke(snail, DyeColor.BROWN);
                if (!(snail instanceof Leashable leashable)) throw new IllegalStateException("Naturalist Snail is not leashable");
                snail.addTag(TAG_PREFIX + merchant.getUUID());
                leashable.setLeashedTo(merchant, true);
                created.add(snail);
            }
            return created.stream().map(Entity::getUUID).toList();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            created.forEach(Entity::discard);
            throw new IllegalStateException("Unable to initialize Naturalist Snail companions", exception);
        }
    }

    public static void discardLoadedManagedCompanions(ServerLevel level, UUID merchantUuid) {
        String tag = TAG_PREFIX + merchantUuid;
        for (Entity entity : level.getAllEntities()) {
            if (entity.getType().builtInRegistryHolder().is(SNAIL) && entity.entityTags().contains(tag)) entity.discard();
        }
    }

    /** Cleans only explicitly tagged, loaded former companions; never force-loads chunks. */
    public static void discardLoadedStaleCompanions(ServerLevel level, UUID activeMerchant) {
        for (Entity entity : level.getAllEntities()) {
            if (!entity.getType().builtInRegistryHolder().is(SNAIL)) continue;
            boolean managed = entity.entityTags().stream().anyMatch(tag -> tag.startsWith(TAG_PREFIX));
            if (managed && (activeMerchant == null || !entity.entityTags().contains(TAG_PREFIX + activeMerchant))) entity.discard();
        }
    }
}

package dev.resivore.inventoryparticlesmatchacompat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.lopymine.ip.element.mod.spawner.ParticleSpawner;
import net.minecraft.world.item.ItemStack;

/**
 * Marks only the C4 data-defined spawners, then gives exact target stacks an exclusive route.
 * Non-target stacks always preserve the unmodified Inventory Particles spawner behavior.
 */
public final class ExclusiveParticleDispatch {
    private static final Map<ParticleSpawner, ExclusiveParticleTarget> COMPAT_SPAWNERS = new ConcurrentHashMap<>();

    private ExclusiveParticleDispatch() {
    }

    public static void reset() {
        COMPAT_SPAWNERS.clear();
    }

    public static void register(ParticleSpawner spawner, String holderName) {
        for (ExclusiveParticleTarget target : ExclusiveParticleTarget.values()) {
            if (target.compatHolderName().equals(holderName)) {
                COMPAT_SPAWNERS.put(spawner, target);
                return;
            }
        }
    }

    public static boolean permits(ParticleSpawner spawner, ItemStack stack) {
        return ExclusiveParticleTarget.find(stack)
                .map(target -> target.equals(COMPAT_SPAWNERS.get(spawner)))
                .orElse(true);
    }

    static boolean permits(ExclusiveParticleTarget target, String holderName) {
        return target.compatHolderName().equals(holderName);
    }
}

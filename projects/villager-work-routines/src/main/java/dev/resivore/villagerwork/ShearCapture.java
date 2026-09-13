package dev.resivore.villagerwork;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;

/** Thread-local scope binds only the output callback of our exact synchronous sheep.shear call. */
public final class ShearCapture {
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();

    private ShearCapture() {}

    public static void shear(Sheep sheep, ServerLevel level, SimpleContainer ownedOutput) {
        Scope previous = ACTIVE.get();
        ACTIVE.set(new Scope(sheep, ownedOutput));
        try { sheep.shear(level, net.minecraft.sounds.SoundSource.NEUTRAL, new ItemStack(net.minecraft.world.item.Items.SHEARS)); }
        finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }

    public static BiConsumer<ServerLevel, ItemStack> wrap(Sheep sheep, BiConsumer<ServerLevel, ItemStack> vanilla) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.sheep != sheep) return vanilla;
        return (level, stack) -> {
            if (stack.is(ItemTags.WOOL) && OutputStorage.fits(scope.output, stack, stack.getCount())) {
                OutputStorage.insert(scope.output, stack, stack.getCount());
            } else {
                // A datapack may increase or replace the vanilla 1–3 wool output. Preserve every unexpected output.
                vanilla.accept(level, stack);
            }
        };
    }

    private record Scope(Sheep sheep, SimpleContainer output) {}
}

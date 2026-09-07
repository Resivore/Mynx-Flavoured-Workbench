package tempeststudios.quickstacknearby;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Uses CNM's own ShapeMap only for routing affinity; it never affects search identity. */
final class ShapeMapRoutingCompat {
    private ShapeMapRoutingCompat() {}

    static List<QuickStackMoveEngine.Target> augmentTargets(Container source, int first, int end,
            QuickStackMoveEngine.SourceRules rules, List<QuickStackMoveEngine.Target> targets) {
        if (!FabricLoader.getInstance().isModLoaded("clutternomore") || targets.isEmpty()) return targets;
        List<ItemStack> sources = new ArrayList<>();
        for (int i = Math.max(0, first); i < Math.min(end, source.getContainerSize()); i++) {
            ItemStack stack = source.getItem(i);
            if (!stack.isEmpty() && !rules.isLocked(i) && rules.movableCount(i, stack.getCount()) > 0) sources.add(stack);
        }
        if (sources.isEmpty()) return targets;
        List<QuickStackMoveEngine.Target> result = new ArrayList<>(targets.size());
        boolean changed = false;
        for (QuickStackMoveEngine.Target target : targets) {
            LinkedHashSet<QuickStackMoveEngine.StackKey> keys = new LinkedHashSet<>(target.acceptedTypes());
            for (int slot = 0; slot < target.container().getContainerSize(); slot++) {
                ItemStack stored = target.container().getItem(slot);
                if (stored.isEmpty()) continue;
                for (ItemStack candidate : sources) if (sameShape(candidate, stored)) keys.add(QuickStackMoveEngine.StackKey.of(candidate));
            }
            if (keys.equals(target.acceptedTypes())) result.add(target);
            else { changed = true; result.add(new QuickStackMoveEngine.Target(target.container(), Set.copyOf(keys))); }
        }
        return changed ? List.copyOf(result) : targets;
    }

    private static boolean sameShape(ItemStack left, ItemStack right) {
        try {
            Class<?> shapeMap = Class.forName("dev.tazer.clutternomore.common.shape_map.ShapeMap");
            Method method = shapeMap.getMethod("inSameShapeSet", net.minecraft.world.item.Item.class, net.minecraft.world.item.Item.class);
            return (boolean) method.invoke(null, left.getItem(), right.getItem());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Loaded Clutter No More lacks its audited ShapeMap API", e);
        }
    }
}

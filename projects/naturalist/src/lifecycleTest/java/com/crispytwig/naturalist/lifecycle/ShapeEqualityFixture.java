package com.crispytwig.naturalist.lifecycle;
import net.minecraft.world.item.Item;
import java.util.Set;
public final class ShapeEqualityFixture {
    static Set<Item> family = Set.of();
    public static boolean sameFamily(Item first, Item second) { return family.contains(first) && family.contains(second); }
}

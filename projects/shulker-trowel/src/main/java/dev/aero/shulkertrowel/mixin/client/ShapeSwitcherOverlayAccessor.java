package dev.aero.shulkertrowel.mixin.client;

import dev.tazer.clutternomore.client.ShapeSwitcherOverlay;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = ShapeSwitcherOverlay.class, remap = false)
public interface ShapeSwitcherOverlayAccessor {
    @Mutable
    @Accessor("shapes")
    void shulkerTrowel$setShapes(List<Item> shapes);
}

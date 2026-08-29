package dev.resivore.mapmarkerextension.mixin;

import dev.resivore.mapmarkerextension.core.MapMarkerNormalizer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapItemSavedData.class)
abstract class MapItemSavedDataMixin {
    @Shadow
    private void addDecoration(
        Holder<MapDecorationType> type,
        LevelAccessor level,
        String key,
        double x,
        double z,
        double rotation,
        Component name
    ) {
        throw new AssertionError();
    }

    @Inject(method = "tickCarriedBy", at = @At("HEAD"))
    private void mapMarkerExtension$normalizeTarget(
        Player player,
        ItemStack stack,
        ItemFrame frame,
        CallbackInfo callbackInfo
    ) {
        MapMarkerNormalizer.normalize(stack).ifPresent(target -> addDecoration(
            target.type(),
            player.level(),
            MapMarkerNormalizer.EXPLORATION_TARGET_KEY,
            target.x(),
            target.z(),
            target.rotation(),
            null
        ));
    }
}

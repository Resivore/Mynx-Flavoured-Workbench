package dev.resivore.mapmarkerextension.client;

import dev.resivore.mapmarkerextension.core.DecorationObservation;
import dev.resivore.mapmarkerextension.core.MapMarkerIdentity;
import dev.resivore.mapmarkerextension.core.MapMarkerItemIdentity;
import dev.resivore.mapmarkerextension.core.MapMarkerTargetResolver;
import dev.resivore.mapmarkerextension.core.MapObservation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;

public final class CarriedMapScanner {
    private final MapMarkerTargetRepository targets;

    public CarriedMapScanner(MapMarkerTargetRepository targets) {
        this.targets = targets;
    }

    public void refresh(Minecraft client) {
        if (client.player == null || client.level == null) {
            targets.clear();
            return;
        }

        Inventory inventory = client.player.getInventory();
        List<MapObservation> observations = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(Items.FILLED_MAP)) {
                continue;
            }

            MapId mapId = stack.get(DataComponents.MAP_ID);
            if (mapId == null) {
                continue;
            }
            Optional<MapMarkerIdentity> identity = MapMarkerItemIdentity.find(stack);
            MapDecorations decorations = stack.getOrDefault(
                DataComponents.MAP_DECORATIONS,
                MapDecorations.EMPTY
            );
            List<DecorationObservation> decorationObservations = new ArrayList<>();
            for (MapDecorations.Entry entry : decorations.decorations().values()) {
                entry.type().unwrapKey().ifPresent(typeKey ->
                    decorationObservations.add(new DecorationObservation(
                        typeKey.identifier().toString(),
                        entry.type().value().assetId().toString(),
                        entry.x(),
                        entry.z()
                    ))
                );
            }
            observations.add(new MapObservation(
                mapId.id(),
                true,
                Level.OVERWORLD.identifier().toString(),
                identity,
                decorationObservations
            ));
        }
        targets.replace(MapMarkerTargetResolver.resolve(observations));
    }
}

package dev.resivore.mynxtrees;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.boat.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.DispenserBlock;
import java.util.ArrayList;
import java.util.List;

final class WoodBoats {
    record Vessel(EntityType<? extends AbstractBoat> type, Identifier texture, boolean chest) {}
    static final List<Vessel> VESSELS = new ArrayList<>();

    static void register(String family) {
        for (boolean chest : new boolean[]{false, true}) {
            String kind = chest ? "chest_boat" : "boat";
            Identifier id = Identifier.fromNamespaceAndPath("mynx_trees", family + "_" + kind);
            var itemKey = ResourceKey.create(Registries.ITEM, id);
            java.util.function.Supplier<Item> drop = () -> BuiltInRegistries.ITEM.getValue(id);
            EntityType.Builder<? extends AbstractBoat> builder = chest
                    ? EntityType.Builder.<ChestBoat>of((type, level) -> new ChestBoat(type, level, drop), MobCategory.MISC)
                    : EntityType.Builder.<Boat>of((type, level) -> new Boat(type, level, drop), MobCategory.MISC);
            var key = ResourceKey.create(Registries.ENTITY_TYPE, id);
            var entity = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                    builder.noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10).build(key));
            var item = Registry.register(BuiltInRegistries.ITEM, itemKey,
                    new BoatItem(entity, new Item.Properties().setId(itemKey).stacksTo(1)));
            DispenserBlock.registerBehavior(item, new BoatDispenseItemBehavior(entity));
            CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> out.accept(item));
            VESSELS.add(new Vessel(entity, Identifier.fromNamespaceAndPath("mynx_trees", kind + "/" + family), chest));
        }
    }
}

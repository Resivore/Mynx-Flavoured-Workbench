package dev.resivore.villagerwork;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.core.Registry;

public final class VillagerWorkRoutines implements ModInitializer {
    public static final Identifier FLOAT_ID = Identifier.fromNamespaceAndPath("villager_work_routines", "fishing_float");
    public static final EntityType<FishingFloat> FISHING_FLOAT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, FLOAT_ID,
            EntityType.Builder.<FishingFloat>of(FishingFloat::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).noSummon().clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, FLOAT_ID)));

    @Override public void onInitialize() {
        ServerLevelEvents.UNLOAD.register((server, level) ->
                LivestockGateBlocker.clearLevel(level, "world-unload"));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Villager villager) WorkCoordinator.onRemoval(villager);
        });
    }
}

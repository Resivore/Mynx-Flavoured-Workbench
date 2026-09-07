package dev.resivore.mynxfloratrades;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/** Bootstrap-only registrations plus the narrow dynamic FlowerPotBlock matching rule. */
public final class FloristRegistry {
    public static final Identifier FLORIST_ID = MynxFloraTrades.id("florist");
    public static final ResourceKey<PoiType> FLORIST_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, FLORIST_ID);
    public static final ResourceKey<VillagerProfession> FLORIST = ResourceKey.create(Registries.VILLAGER_PROFESSION, FLORIST_ID);
    public static final ResourceKey<TradeSet> FLORIST_TRADES = ResourceKey.create(Registries.TRADE_SET,
            MynxFloraTrades.id("florist/level_1"));

    private FloristRegistry() { }

    public static void registerPoi(Registry<PoiType> registry) {
        Registry.register(registry, FLORIST_POI, new PoiType(Set.copyOf(
                Blocks.FLOWER_POT.getStateDefinition().getPossibleStates()), 1, 1));
    }

    public static void registerProfession(Registry<VillagerProfession> registry) {
        var tradeSets = new Int2ObjectOpenHashMap<ResourceKey<TradeSet>>();
        tradeSets.put(1, FLORIST_TRADES);
        Registry.register(registry, FLORIST, new VillagerProfession(
                Component.translatable("entity.minecraft.villager.florist"),
                holder -> holder.is(FLORIST_POI), holder -> holder.is(FLORIST_POI),
                ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_FARMER, tradeSets));
    }

    public static boolean isFloristPoi(PoiType type) {
        return BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOptional(FLORIST_POI)
                .map(found -> found == type).orElse(false);
    }

    public static boolean isFlowerPot(BlockState state) { return state.getBlock() instanceof FlowerPotBlock; }

    public static Holder<PoiType> floristHolder() {
        return BuiltInRegistries.POINT_OF_INTEREST_TYPE.get(FLORIST_ID)
                .orElseThrow(() -> new IllegalStateException("Florist POI is not registered"));
    }
}

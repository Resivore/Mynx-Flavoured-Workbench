package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegister;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegisterCreativeTab;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@AutoRegister(RibbitsCommon.MOD_ID)
public class CreativeTabModule {

    private static final List<CreativeEntry> ENTRIES = List.of(
            CreativeEntry.of("red_toadstool", BlockModule.RED_TOADSTOOL::get),
            CreativeEntry.of("brown_toadstool", BlockModule.BROWN_TOADSTOOL::get),
            CreativeEntry.of("toadstool_stem", BlockModule.TOADSTOOL_STEM::get),
            CreativeEntry.of("swamp_lantern", BlockModule.SWAMP_LANTERN::get),
            CreativeEntry.of("giant_lilypad", ItemModule.GIANT_LILYPAD::get),
            CreativeEntry.of("swamp_daisy", BlockModule.SWAMP_DAISY::get),
            CreativeEntry.of("toadstool", BlockModule.TOADSTOOL::get),
            CreativeEntry.of("glowcap", ItemModule.GLOWCAP::get),
            CreativeEntry.of("toadstool_heart", ItemModule.TOADSTOOL_HEART::get),
            CreativeEntry.of("chute_leaf", ItemModule.CHUTE_LEAF::get),
            CreativeEntry.of("umbrella_leaf", BlockModule.UMBRELLA_LEAF::get),
            CreativeEntry.of("mossy_oak_planks", BlockModule.MOSSY_OAK_PLANKS::get),
            CreativeEntry.of("mossy_oak_planks_stairs", BlockModule.MOSSY_OAK_PLANKS::getStairs),
            CreativeEntry.of("mossy_oak_planks_slab", BlockModule.MOSSY_OAK_PLANKS::getSlab),
            CreativeEntry.of("mossy_oak_planks_fence", BlockModule.MOSSY_OAK_PLANKS::getFence),
            CreativeEntry.of("mossy_oak_planks_fence_gate", BlockModule.MOSSY_OAK_PLANKS::getFenceGate),
            CreativeEntry.of("mossy_oak_door", BlockModule.MOSSY_OAK_DOOR::get),
            CreativeEntry.of("maraca", ItemModule.MARACA::get),
            CreativeEntry.of("ribbit_nitwit_spawn_egg", ItemModule.RIBBIT_NITWIT_SPAWN_EGG::get),
            CreativeEntry.of("ribbit_fisherman_spawn_egg", ItemModule.RIBBIT_FISHERMAN_SPAWN_EGG::get),
            CreativeEntry.of("ribbit_gardener_spawn_egg", ItemModule.RIBBIT_GARDENER_SPAWN_EGG::get),
            CreativeEntry.of("ribbit_merchant_spawn_egg", ItemModule.RIBBIT_MERCHANT_SPAWN_EGG::get),
            CreativeEntry.of("ribbit_sorcerer_spawn_egg", ItemModule.RIBBIT_SORCERER_SPAWN_EGG::get),
            CreativeEntry.of("ribbit_chef_spawn_egg", ItemModule.RIBBIT_CHEF_SPAWN_EGG::get),
            CreativeEntry.of("ribbit_farmer_spawn_egg", ItemModule.RIBBIT_FARMER_SPAWN_EGG::get),
            CreativeEntry.of("ribbit_prospector_spawn_egg", ItemModule.RIBBIT_PROSPECTOR_SPAWN_EGG::get),
            CreativeEntry.of("ribbit_guard_spawn_egg", ItemModule.RIBBIT_GUARD_SPAWN_EGG::get),
            CreativeEntry.of("wandering_ribbit_spawn_egg", ItemModule.WANDERING_RIBBIT_SPAWN_EGG::get)
    );

    @AutoRegister("general")
    public static AutoRegisterCreativeTab TAB = AutoRegisterCreativeTab.builder()
            .title(Component.translatable("itemGroup.ribbits.general"))
            .iconItem(() -> new ItemStack(BlockModule.RED_TOADSTOOL.get()))
            .entries((params, output) -> {
                Set<Item> seenItems = Collections.newSetFromMap(new IdentityHashMap<>());
                Set<Identifier> seenIds = new HashSet<>();

                for (CreativeEntry entry : ENTRIES) {
                    ItemLike value = entry.value().get();
                    Item item = value.asItem();
                    Identifier actualId = BuiltInRegistries.ITEM.getKey(item);
                    if (!entry.expectedId().equals(actualId)) {
                        throw new IllegalStateException("Ribbits creative entry " + entry.expectedId()
                                + " resolved to " + actualId);
                    }
                    if (!seenItems.add(item) || !seenIds.add(actualId)) {
                        throw new IllegalStateException("Duplicate Ribbits creative entry: " + actualId);
                    }
                    output.accept(value);
                }
            })
            .build();

    private record CreativeEntry(Identifier expectedId, Supplier<? extends ItemLike> value) {
        private static CreativeEntry of(String path, Supplier<? extends ItemLike> value) {
            return new CreativeEntry(Identifier.fromNamespaceAndPath(RibbitsCommon.MOD_ID, path), value);
        }
    }
}

package com.fizzware.dramaticdoors.fabric;

import com.fizzware.dramaticdoors.DramaticDoors;
import com.fizzware.dramaticdoors.blocks.TallDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallSlidingDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallStableDoorBlock;
import com.fizzware.dramaticdoors.compat.DDCompatAdvancement;
import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.fizzware.dramaticdoors.registry.DDRegistry;
import oshi.util.tuples.Pair;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.List;
import java.util.Set;

/** AMC publishes each block immediately before its BlockItem, from either provider's init hook. */
final class AmcMacawDoors {
    private record Entry(String wood, Identifier source, Identifier tall, String kind) {}

    static void initialize() {
        var loader = FabricLoader.getInstance();
        if (!loader.isModLoaded("architectural_material_closure") || !loader.isModLoaded("mcwdoors")) return;
        var pending = new LinkedHashMap<Identifier, Entry>();
        var packs = new HashSet<String>();
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                AmcMacawDoors.class.getResourceAsStream("/dramaticdoors/amc_macaw_doors.tsv")), StandardCharsets.UTF_8))) {
            reader.readLine();
            for (String line; (line = reader.readLine()) != null;) {
                var fields = line.split("\t");
                if (!loader.isModLoaded(fields[0].equals("mossy_oak") ? "ribbits" : "enderscape")) continue;
                var entry = new Entry(fields[0], Identifier.parse(fields[1]), Identifier.parse(fields[2]), fields[4]);
                if (pending.put(entry.source(), entry) != null) throw new IllegalStateException(line);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read AMC door mappings", e);
        }
        // Listen before inspecting the allowlist. Removing a matched entry makes both paths idempotent.
        RegistryEntryAddedCallback.event(BuiltInRegistries.ITEM).register((rawId, id, item) -> {
            var entry = pending.remove(id);
            if (entry != null) register(entry, item, packs);
        });
        for (var id : List.copyOf(pending.keySet())) {
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> register(pending.remove(id), item, packs));
        }
    }

    private static void register(Entry entry, Item item, Set<String> packs) {
        if (!(item instanceof BlockItem original) || !(original.getBlock() instanceof DoorBlock source)
                || BuiltInRegistries.BLOCK.getOptional(entry.source()).orElse(null) != source) {
            DramaticDoors.LOGGER.warn("Skipping AMC tall door: {} has no matching DoorBlock and BlockItem", entry.source());
            return;
        }
        String expectedClass = switch (entry.kind()) {
            case "ordinary" -> DoorBlock.class.getName();
            case "sliding" -> "net.kikoz.mcwdoors.objects.JapaneseDoors";
            case "stable" -> "net.kikoz.mcwdoors.objects.StableDoor";
            default -> throw new IllegalStateException("Unknown AMC door class " + entry.kind());
        };
        if (!source.getClass().getName().equals(expectedClass)) {
            DramaticDoors.LOGGER.warn("Skipping AMC tall door: {} uses {}, expected {}", entry.source(), source.getClass().getName(), expectedClass);
            return;
        }
        if (packs.add(entry.wood())) {
            var pack = Identifier.fromNamespaceAndPath("dramaticdoors", "amc_" + entry.wood() + "_doors");
            if (!ResourceLoader.registerBuiltinPack(pack, FabricLoader.getInstance().getModContainer("dramaticdoors").orElseThrow(),
                    PackActivationType.ALWAYS_ENABLED)) throw new IllegalStateException("Missing " + pack);
        }
        var blockKey = ResourceKey.create(Registries.BLOCK, entry.tall());
        var properties = BlockBehaviour.Properties.ofFullCopy(source).setId(blockKey);
        var block = switch (entry.kind()) {
            case "sliding" -> new TallSlidingDoorBlock(source.type(), properties);
            case "stable" -> new TallStableDoorBlock(source.type(), properties);
            default -> new TallDoorBlock(source.type(), properties);
        };
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        var itemKey = ResourceKey.create(Registries.ITEM, entry.tall());
        var output = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, output);
        String name = entry.tall().getPath();
        DDRegistry.DOOR_BLOCKS.add(new Pair<>(name, block));
        DDRegistry.DOOR_ITEMS.add(new Pair<>(name, output));
        String design = entry.source().getPath().substring(entry.wood().length() + 1);
        String group = "tall_macaw_" + (design.equals("stem_glass_door") ? "bark_glass_door" : design);
        DDCompatRecipe.createTallDoorRecipe(name, entry.source(), group);
        DDCompatAdvancement.createRecipeAdvancement(name, entry.source());
    }
}

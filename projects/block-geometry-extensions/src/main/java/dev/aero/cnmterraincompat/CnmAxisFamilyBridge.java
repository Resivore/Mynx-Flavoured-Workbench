package dev.aero.cnmterraincompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.mixin.AxeItemAccessor;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Adopts CNM-family pillar sources that can prove their material contract from their own
 * registered state and packaged resource data.  This deliberately has no provider-name or
 * registry-path allowlist: an ambiguous, overridden, or non-column source is left to its owner.
 */
public final class CnmAxisFamilyBridge {
    public static final String PROFILE_VERSION = "bge-c79-cnm-axis-family-bridge-v1";
    private static boolean registered;

    private CnmAxisFamilyBridge() {}

    /**
     * Runs before CNM scans slabs and stairs.  Every accepted source receives BGE's typed
     * standard roles first, allowing CNM to derive the compatible Vertical Slab and Step once.
     */
    public static synchronized int registerEligibleFamilies() {
        if (registered) return 0;
        List<Candidate> candidates = discover();
        Map<Block, Block> strippables = AxeItemAccessor.bge$getStrippables();
        List<ExternalMaterialCatalog.Spec> specs = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (strippables.containsKey(candidate.source())) {
                // MaterialTransition is intentionally typed to a native ModBlocks target.  A
                // generic provider target would lose that identity, so preserve its owner rather
                // than manufacture a partial stripping contract.
                continue;
            }
            specs.add(new ExternalMaterialCatalog.Spec(candidate.id(), candidate.id().getNamespace(),
                    candidate.id(), candidate.id(), Map.of(), VisualProfile.PILLAR,
                    NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED,
                    candidate.visual().side(), candidate.visual().end(), candidate.visual().end(), "",
                    TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                    Set.of(), Set.of(), List.of()));
        }
        specs.sort(Comparator.comparing(ExternalMaterialCatalog.Spec::id));
        for (ExternalMaterialCatalog.Spec spec : specs) ExternalMaterialFamilies.register(spec);
        registered = true;
        return specs.size();
    }

    /** Visible regression seam: only a unique exact resource contract is eligible. */
    public static Optional<ColumnVisual> inspect(Identifier source) {
        List<ColumnVisual> contracts = new ArrayList<>();
        String statePath = "assets/" + source.getNamespace() + "/blockstates/"
                + source.getPath() + ".json";
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            Optional<Path> state = container.findPath(statePath);
            if (state.isEmpty()) continue;
            decode(source, container, state.get()).ifPresent(contracts::add);
        }
        return contracts.size() == 1 ? Optional.of(contracts.getFirst()) : Optional.empty();
    }

    private static List<Candidate> discover() {
        List<Candidate> result = new ArrayList<>();
        for (Block source : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(source);
            if (id == null || id.getNamespace().equals("minecraft")
                    || !(source instanceof RotatedPillarBlock)
                    || !(source.asItem() instanceof BlockItem)
                    || NibaruMaterialProfiles.fromBlock(source).isPresent()) continue;
            inspect(id).ifPresent(visual -> result.add(new Candidate(id, source, visual)));
        }
        result.sort(Comparator.comparing(Candidate::id));
        return List.copyOf(result);
    }

    private static Optional<ColumnVisual> decode(Identifier source, ModContainer container, Path statePath) {
        try {
            JsonObject blockState = readObject(statePath);
            NativeAxisModelContract.uvPolicy(source, blockState);
            NativeAxisModelContract.AxisSelector axisY = NativeAxisModelContract.axisSelector(source,
                    blockState, net.minecraft.core.Direction.Axis.Y);
            if (axisY.models().size() != 1) return Optional.empty();
            Identifier model = Identifier.parse(axisY.models().iterator().next());
            if (!model.getPath().startsWith("block/")) return Optional.empty();
            Optional<Path> modelPath = container.findPath("assets/" + model.getNamespace() + "/models/"
                    + model.getPath() + ".json");
            if (modelPath.isEmpty()) return Optional.empty();
            JsonObject root = readObject(modelPath.get());
            if (!root.has("parent") || !root.get("parent").isJsonPrimitive()
                    || !root.get("parent").getAsString().equals("minecraft:block/cube_column")
                    || !root.has("textures") || !root.get("textures").isJsonObject()) return Optional.empty();
            JsonObject textures = root.getAsJsonObject("textures");
            String side = texture(textures, "side");
            String end = texture(textures, "end");
            return Optional.of(new ColumnVisual(side, end));
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return Optional.empty();
        }
    }

    private static JsonObject readObject(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var value = JsonParser.parseReader(reader);
            if (!value.isJsonObject()) throw new IllegalArgumentException("Expected JSON object: " + path);
            return value.getAsJsonObject();
        }
    }

    private static String texture(JsonObject textures, String key) {
        if (!textures.has(key) || !textures.get(key).isJsonPrimitive()
                || !textures.get(key).getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing column texture role " + key);
        }
        String value = textures.get(key).getAsString();
        if (value.startsWith("#") || !value.contains(":")) {
            throw new IllegalArgumentException("Unresolved column texture role " + key + ": " + value);
        }
        Identifier.parse(value);
        return value;
    }

    private record Candidate(Identifier id, Block source, ColumnVisual visual) {}

    /** Exact bark/end-grain visual contract extracted from the provider's own column model. */
    public record ColumnVisual(String side, String end) {}
}

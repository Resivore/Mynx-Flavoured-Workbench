package games.twinhead.moreslabsstairsandwalls.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract.AxisUvPolicy;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract.GeneratedBlockResources;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/** Build-time writer for provider-owned native slab/stair material-axis resources. */
public final class NativeAxisResourceGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int EXPECTED_AXIS_FAMILIES = 56;
    private static final int EXPECTED_SLAB_SELECTORS = 9;
    private static final int EXPECTED_STAIR_SELECTORS = 120;

    private NativeAxisResourceGenerator() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected generated-resource root argument");
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path generatedRoot = Path.of(args[0]).toAbsolutePath().normalize();
        Path providerAssets = generatedRoot.resolve("assets")
                .resolve(NativeAxisModelContract.PROVIDER_NAMESPACE).normalize();
        if (!providerAssets.startsWith(generatedRoot)
                || !Files.isDirectory(providerAssets.resolve("blockstates"))
                || !Files.isDirectory(providerAssets.resolve("models").resolve("block"))) {
            throw new IllegalStateException("Run tools/Generate-CurrentResources.ps1 before the "
                    + "native-axis writer; provider resource tree is missing: " + providerAssets);
        }

        int families = 0;
        int slabs = 0;
        int stairs = 0;
        int slabSelectors = 0;
        int stairSelectors = 0;
        EnumMap<AxisUvPolicy, Integer> policies = new EnumMap<>(AxisUvPolicy.class);

        for (ModBlocks family : ModBlocks.values()) {
            if (!NativeAxisModelContract.applies(family)) continue;
            families++;
            Identifier canonicalParent = BuiltInRegistries.BLOCK.getKey(family.parentBlock);
            JsonObject canonicalBlockState = canonicalBlockState(canonicalParent);
            AxisUvPolicy policy = NativeAxisModelContract.uvPolicy(
                    canonicalParent, canonicalBlockState);
            policies.merge(policy, 1, Integer::sum);

            if (!family.hasBlock(ModBlocks.BlockType.SLAB)
                    || !family.hasBlock(ModBlocks.BlockType.STAIRS)) {
                throw new IllegalStateException("Applicable canonical parent lacks native slab/stair: "
                        + canonicalParent);
            }

            GeneratedBlockResources slab = NativeAxisModelContract.slab(family, policy);
            requireSelectors(family, "slab", slab, EXPECTED_SLAB_SELECTORS);
            write(providerAssets, family.getId(ModBlocks.BlockType.SLAB), slab);
            slabs++;
            slabSelectors += slab.selectors().size();

            GeneratedBlockResources stair = NativeAxisModelContract.stairs(family, policy);
            requireSelectors(family, "stair", stair, EXPECTED_STAIR_SELECTORS);
            write(providerAssets, family.getId(ModBlocks.BlockType.STAIRS), stair);
            stairs++;
            stairSelectors += stair.selectors().size();
        }

        if (families != EXPECTED_AXIS_FAMILIES || slabs != EXPECTED_AXIS_FAMILIES
                || stairs != EXPECTED_AXIS_FAMILIES || policies.size() != AxisUvPolicy.values().length) {
            throw new IllegalStateException("Native-axis inventory drift: families=" + families
                    + " slabs=" + slabs + " stairs=" + stairs + " policies=" + policies);
        }
        System.out.println("NATIVE_AXIS_STATIC_RESOURCES families=" + families
                + " slabs=" + slabs + " stairs=" + stairs
                + " slabSelectors=" + slabSelectors + " stairSelectors=" + stairSelectors
                + " policies=" + policies);
    }

    private static JsonObject canonicalBlockState(Identifier canonicalParent) throws IOException {
        String resource = "assets/" + canonicalParent.getNamespace() + "/blockstates/"
                + canonicalParent.getPath() + ".json";
        InputStream stream = NativeAxisResourceGenerator.class.getClassLoader()
                .getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("Canonical blockstate is absent from the Minecraft "
                    + "runtime classpath: " + resource);
        }
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void requireSelectors(ModBlocks family, String geometry,
            GeneratedBlockResources resources, int expected) {
        if (resources.selectors().size() != expected) {
            throw new IllegalStateException("Wrong " + geometry + " selector count for " + family
                    + ": " + resources.selectors().size() + " != " + expected);
        }
        for (NativeAxisModelContract.VariantSelection selector : resources.selectors().values()) {
            if (!resources.models().containsKey(selector.model())) {
                throw new IllegalStateException("Missing generated model " + selector.model()
                        + " for " + family + " " + geometry);
            }
        }
    }

    private static void write(Path providerAssets, Identifier blockId,
            GeneratedBlockResources resources) throws IOException {
        if (!blockId.getNamespace().equals(NativeAxisModelContract.PROVIDER_NAMESPACE)) {
            throw new IllegalArgumentException("Native geometry is outside provider namespace: " + blockId);
        }
        Path blockState = providerAssets.resolve("blockstates")
                .resolve(blockId.getPath() + ".json").normalize();
        requireContained(providerAssets, blockState);

        Path modelDirectory = providerAssets.resolve("models").resolve("block");
        try (DirectoryStream<Path> stale = Files.newDirectoryStream(
                modelDirectory, blockId.getPath() + "_material_*.json")) {
            for (Path path : stale) Files.delete(path);
        }

        writeJson(blockState, resources.blockState());
        for (Map.Entry<String, JsonObject> model : resources.models().entrySet()) {
            Identifier modelId = Identifier.parse(model.getKey());
            if (!modelId.getNamespace().equals(NativeAxisModelContract.PROVIDER_NAMESPACE)
                    || !modelId.getPath().startsWith("block/")) {
                throw new IllegalStateException("Generated model escaped provider block models: "
                        + modelId);
            }
            Path target = providerAssets.resolve("models")
                    .resolve(modelId.getPath() + ".json").normalize();
            requireContained(providerAssets, target);
            writeJson(target, model.getValue());
        }
    }

    private static void writeJson(Path target, JsonObject json) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, GSON.toJson(json) + System.lineSeparator(),
                StandardCharsets.UTF_8);
    }

    private static void requireContained(Path root, Path target) {
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Refusing to write outside generated provider assets: "
                    + target);
        }
    }
}

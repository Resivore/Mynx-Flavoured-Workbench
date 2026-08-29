package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.AxisModelContract;
import dev.aero.cnmterraincompat.AxisModelContract.AxisUvPolicy;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;

/** Extends CNM's normal resource pass only when the canonical material parent actually has axis. */
public final class AxisGeneratedResources {
    private AxisGeneratedResources() {}

    public static void extendVertical(
            ResourceManager manager, Identifier parent, Identifier shape) {
        NibaruMaterialProfile profile = axisProfile(parent);
        if (profile == null) return;
        AxisUvPolicy policy = policy(manager, profile.canonicalParentId());
        AxisModelContract.verticalGeneratedModels(profile, shape, policy).forEach((modelId, model) ->
                AssetGenerator.write(AxisModelContract.generatedModelPath(modelId), model));
        AssetGenerator.write("blockstates/%s.json".formatted(shape.getPath()),
                AxisModelContract.verticalBlockState(shape, policy));
    }

    public static void extendStep(
            ResourceManager manager, Identifier parent, Identifier shape) {
        NibaruMaterialProfile profile = axisProfile(parent);
        if (profile == null) return;
        AxisUvPolicy policy = policy(manager, profile.canonicalParentId());
        AxisModelContract.stepGeneratedModels(profile, shape, policy).forEach((modelId, model) ->
                AssetGenerator.write(AxisModelContract.generatedModelPath(modelId), model));
        AssetGenerator.write("blockstates/%s.json".formatted(shape.getPath()),
                AxisModelContract.stepBlockState(shape, policy));
    }

    /** Public predicate seam for catalog-level generated-resource coverage. */
    public static boolean applies(Identifier parent) {
        return NibaruMaterialProfiles.fromId(parent).filter(AxisModelContract::applies).isPresent();
    }

    /** Reads the canonical parent's actual axis selectors; missing/unknown data is a hard failure. */
    public static AxisUvPolicy policy(ResourceManager manager, Identifier canonicalParent) {
        Identifier blockStateId = Identifier.fromNamespaceAndPath(canonicalParent.getNamespace(),
                "blockstates/" + canonicalParent.getPath() + ".json");
        Resource resource = manager.getResource(blockStateId).orElseThrow(() ->
                new IllegalStateException("Missing canonical axis blockstate: " + blockStateId));
        try (var reader = resource.openAsReader()) {
            var parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException("Canonical axis blockstate is not an object: "
                        + blockStateId);
            }
            JsonObject blockState = parsed.getAsJsonObject();
            return AxisModelContract.uvPolicy(canonicalParent, blockState);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot inspect canonical axis blockstate: "
                    + blockStateId, exception);
        }
    }

    private static NibaruMaterialProfile axisProfile(Identifier parent) {
        return NibaruMaterialProfiles.fromId(parent).filter(AxisModelContract::applies).orElse(null);
    }
}

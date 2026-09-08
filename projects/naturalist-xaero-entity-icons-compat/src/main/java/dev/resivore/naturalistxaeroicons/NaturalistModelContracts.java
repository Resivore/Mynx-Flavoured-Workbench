package dev.resivore.naturalistxaeroicons;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.ModelPartUtil;

/**
 * Closed Naturalist-only contracts.  A model is eligible only when both its exact entity id and
 * exact known model class agree.  These paths are source-audited Naturalist 2.0.3 C8 contracts,
 * not a name-search heuristic over arbitrary modded ModelPart trees.
 */
public final class NaturalistModelContracts {
    private static final Map<String, List<Contract>> TARGETS = targets();
    private static final List<String> NATIVE_CONTROLS = List.of(
            "bear", "bird", "butterfly", "catfish", "caterpillar", "crab", "deer", "firefly", "snake", "snail");
    private static final Map<String, Presentation> NATIVE_PRESENTATION_OVERRIDES =
            Map.of("bear", new Presentation(0.45F, 0.0F, 0.0F, 0.0F));

    private NaturalistModelContracts() {}

    public static boolean owns(Entity entity) {
        return EntityType.getKey(entity.getType()).getNamespace().equals("naturalist")
                && TARGETS.containsKey(EntityType.getKey(entity.getType()).getPath());
    }
    public static boolean isTargetId(String id) { return TARGETS.containsKey(id); }
    public static List<String> targetIds() { return TARGETS.keySet().stream().sorted().toList(); }
    static List<Contract> contractsForId(String id) { return TARGETS.getOrDefault(id, List.of()); }

    public static boolean isNativeControl(Entity entity) {
        return EntityType.getKey(entity.getType()).getNamespace().equals("naturalist")
                && NATIVE_CONTROLS.contains(EntityType.getKey(entity.getType()).getPath());
    }
    public static boolean isNativeControlId(String id) { return NATIVE_CONTROLS.contains(id); }
    public static boolean isNativePresentationOverrideId(String id) {
        return NATIVE_PRESENTATION_OVERRIDES.containsKey(id);
    }
    public static boolean isNativePresentationOverride(Entity entity) {
        return EntityType.getKey(entity.getType()).getNamespace().equals("naturalist")
                && isNativePresentationOverrideId(EntityType.getKey(entity.getType()).getPath());
    }
    public static Presentation nativePresentation(Entity entity) {
        return NATIVE_PRESENTATION_OVERRIDES.get(EntityType.getKey(entity.getType()).getPath());
    }
    static Presentation nativePresentationForId(String id) { return NATIVE_PRESENTATION_OVERRIDES.get(id); }

    public static Optional<ResolvedContract> resolve(Entity entity, Model model) {
        if (!owns(entity) || model == null) return Optional.empty();
        String modelName = model.getClass().getName();
        for (Contract contract : TARGETS.get(EntityType.getKey(entity.getType()).getPath())) {
            if (!contract.modelClass().equals(modelName)) continue;
            ModelPart source = follow(model.root(), contract.path());
            ModelPart trace = follow(model.root(), contract.tracePath());
            if (source == null || trace == null || (contract.requiresVisible() && !source.visible)) continue;
            ModelPart selected = contract.cubeIndexes().isEmpty() ? source : copyDirectCubes(source, contract.cubeIndexes());
            if (selected != null) return Optional.of(new ResolvedContract(contract, source, selected, trace));
        }
        return Optional.empty();
    }

    private static ModelPart follow(ModelPart root, List<String> path) {
        ModelPart current = root;
        for (String segment : path) {
            if (!current.hasChild(segment)) return null;
            current = current.getChild(segment);
        }
        return current;
    }

    /**
     * Zebra's adult face shares its authored part with the long neck.  Copying only the two
     * source-audited face cuboids is the narrow alternative to capturing the whole neck/body.
     */
    private static ModelPart copyDirectCubes(ModelPart source, List<Integer> indexes) {
        List<ModelPart.Cube> cubes = ModelPartUtil.getCubes(source);
        if (cubes == null || indexes.stream().anyMatch(index -> index < 0 || index >= cubes.size())) return null;
        ModelPart copy = new ModelPart(indexes.stream().map(cubes::get).toList(), Map.of());
        copy.x = source.x; copy.y = source.y; copy.z = source.z;
        copy.xRot = source.xRot; copy.yRot = source.yRot; copy.zRot = source.zRot;
        copy.xScale = source.xScale; copy.yScale = source.yScale; copy.zScale = source.zScale;
        copy.visible = source.visible; copy.skipDraw = source.skipDraw;
        copy.setInitialPose(copy.storePose());
        return copy;
    }

    private static Map<String, List<Contract>> targets() {
        Map<String, List<Contract>> map = new LinkedHashMap<>();
        // ordinary headed animals: capture only the model's explicit head/neck subtree
        add(map, "rhino", c("RhinoModel", "root/body/attack/skullRot/neck", p(.44F)));
        add(map, "lion", c("LionModel", "body/neck", p(.68F)), c("LionBabyModel", "body/neck", p(.78F)));
        add(map, "elephant", c("ElephantModel", "body/skullRot/attack/neck", p(.36F)), c("ElephantBabyModel", "body/neck", p(.70F)));
        add(map, "mammoth", c("MammothModel", "body/skullRot/attack/neck", p(.36F)), c("MammothBabyModel", "body/neck", p(.70F)));
        add(map, "zebra", c("ZebraModel", "body/neck/neck_r1", "body/neck/neck_r1", p(.50F), List.of(2, 3)), c("ZebraBabyModel", "body/neck/skull2", p(.70F)));
        add(map, "giraffe", c("GiraffeModel", "hips/shoulders/body/neck/head", p(.55F)), c("GiraffeBabyModel", "body/neck", p(.75F)));
        add(map, "hippo", c("HippoModel", "body/bone/neck", p(.48F)), c("HippoBabyModel", "body/neck", p(.70F)));
        add(map, "vulture", c("VultureModel", "neck"), c("VultureBabyModel", "body/neck"));
        add(map, "boar", c("BoarModel", "body/neck", p(.72F)), c("BoarBabyModel", "body/neck", p(.80F)));
        add(map, "alligator", c("AlligatorModel", "body/neck", p(.58F)), c("AlligatorBabyModel", "body/neck", p(.76F)));
        add(map, "lizard", c("LizardModel", "body/skullRot/neck", "body/skullRot/neck/neck_r1", p(1.0F)));
        add(map, "tortoise", c("TortoiseModel", "body/skullRot/neck"), c("TortoiseBabyModel", "body/skullRot/neck"));
        add(map, "duck", c("DuckModel", "body/neck", p(.72F, 0.0F, 1.5708F, 0.0F)), c("DuckBabyModel", "body/neck", p(.80F, 0.0F, 1.5708F, 0.0F)));
        add(map, "mole", c("MoleModel", "root/body/skull"));
        add(map, "rat", c("RatModel", "body/skull", p(.74F, 0.0F, 1.5708F, 0.0F)));
        add(map, "black_bear", c("BlackBearModel", "body/skullRot/skull"), c("BlackBearBabyModel", "body/skull"));
        add(map, "tiger", c("TigerModel", "body/skullRot/skull", p(.76F)), c("TigerBabyModel", "body/skull", p(.82F)));
        add(map, "komodo_dragon", c("KomodoDragonModel", "body/neck", p(.68F, 0.0F, 1.5708F, 0.0F)));
        add(map, "ostrich", c("OstrichModel", "root/body/skull", p(.60F)), c("OstrichBabyModel", "body/skull", p(.76F)));
        add(map, "turkey", c("TurkeyModel", "body/skull"));
        add(map, "capybara", c("CapybaraModel", "body/skull"), c("CapybaraBabyModel", "body/skull"));
        add(map, "hedgehog", cVisible("HedgehogModel", "rolled", p(.95F)), c("HedgehogModel", "unrolled/body"));
        // compact or non-headed anatomies: capture the explicit compact body subtree, never a sibling search
        add(map, "dragonfly", c("DragonflyModel", "", p(.62F)));
        add(map, "anglerfish", c("AnglerfishModel", "root/body", p(.78F, 0.0F, 1.5708F, 0.0F)));
        add(map, "ray", c("RayModel", "body"));
        add(map, "blobfish", c("BlobfishPinkModel", "", p(.72F)), c("BlobfishGrayModel", "", p(.72F)));
        add(map, "piranha", c("PiranhaModel", "body", p(.88F, 0.0F, 1.5708F, 0.0F)));
        add(map, "bass", c("BassModel", "body"), c("MediumBassModel", "body"), c("LargeBassModel", "", "body", p(1.0F)));
        add(map, "lizard_tail", c("LizardTailModel", ""));
        add(map, "starfish", c("StarfishModel", "", "body", p(.58F, 1.5708F, 0.0F, 0.0F)));
        add(map, "clam", c("ClamModel", "", "top", p(.32F, 1.5708F, 0.0F, 0.0F)));
        add(map, "giant_isopod", c("GiantIsopodModel", "body"));
        add(map, "jellyfish", c("JellyfishModel", "body"));
        add(map, "whale", c("WhaleModel", "body/skullRot", p(.30F)), c("WhaleBabyModel", "body/skull", p(.60F)));
        add(map, "desert_scorpion", c("DesertScorpionModel", "", "body", p(.46F)));
        add(map, "jungle_scorpion", c("JungleScorpionModel", "", "body", p(.50F)));
        add(map, "great_white_shark", cNeutralRoot("GreatWhiteSharkModel", "body", p(.28F, 0.0F, 1.5708F, 0.0F)));
        return Map.copyOf(map);
    }

    private static Contract c(String simpleName, String slashPath) { return c(simpleName, slashPath, p(1.0F)); }
    private static Contract c(String simpleName, String slashPath, Presentation presentation) {
        return c(simpleName, slashPath, slashPath, presentation);
    }
    private static Contract c(String simpleName, String slashPath, String slashTracePath, Presentation presentation) {
        return c(simpleName, slashPath, slashTracePath, presentation, List.of());
    }
    private static Contract c(String simpleName, String slashPath, String slashTracePath, Presentation presentation, List<Integer> cubeIndexes) {
        return new Contract("com.crispytwig.naturalist.client.model." + simpleName,
                path(slashPath), path(slashTracePath), presentation, cubeIndexes, false, false);
    }
    private static Contract cVisible(String simpleName, String slashPath, Presentation presentation) {
        Contract base = c(simpleName, slashPath, presentation);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), true, false);
    }
    private static Contract cNeutralRoot(String simpleName, String slashPath, Presentation presentation) {
        Contract base = c(simpleName, slashPath, presentation);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), false, true);
    }
    private static List<String> path(String slashPath) { return slashPath.isEmpty() ? List.of() : List.of(slashPath.split("/")); }
    private static Presentation p(float scale) { return p(scale, 0.0F, 0.0F, 0.0F); }
    private static Presentation p(float scale, float xRotation, float yRotation, float zRotation) {
        return new Presentation(scale, xRotation, yRotation, zRotation);
    }
    private static void add(Map<String, List<Contract>> map, String id, Contract... contracts) { map.put(id, List.of(contracts)); }

    public record Contract(String modelClass, List<String> path, List<String> tracePath, Presentation presentation,
                           List<Integer> cubeIndexes, boolean requiresVisible, boolean neutralizeRootRotation) {}
    /** Explicit icon-only framing metadata; it never offsets a live renderer model. */
    public record Presentation(float scale, float xRotation, float yRotation, float zRotation) {}
    public record ResolvedContract(Contract contract, ModelPart source, ModelPart selected, ModelPart trace) {}
}

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
    public static Optional<ResolvedContract> resolve(Entity entity, Model model) {
        if (!owns(entity) || model == null) return Optional.empty();
        String modelName = model.getClass().getName();
        for (Contract contract : TARGETS.get(EntityType.getKey(entity.getType()).getPath())) {
            if (!contract.modelClass().equals(modelName)) continue;
            ModelPart source = follow(model.root(), contract.path());
            ModelPart trace = follow(model.root(), contract.tracePath());
            if (source == null || trace == null || (contract.requiresVisible() && !source.visible)) continue;
            ModelPart selected = select(source, contract);
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
    private static ModelPart select(ModelPart source, Contract contract) {
        if (contract.cubeIndexes().isEmpty() && contract.drawableChildren().isEmpty()) return source;
        List<ModelPart.Cube> cubes = ModelPartUtil.getCubes(source);
        if (cubes == null) return null;
        List<Integer> indexes = contract.cubeIndexes();
        if (!indexes.isEmpty() && indexes.stream().anyMatch(index -> index < 0 || index >= cubes.size())) return null;
        Map<String, ModelPart> children = new LinkedHashMap<>();
        for (String childPath : contract.drawableChildren()) {
            List<String> segments = path(childPath);
            if (segments.size() != 1 || !source.hasChild(segments.getFirst())) return null;
            children.put(segments.getFirst(), copySubtree(source.getChild(segments.getFirst()), 0));
        }
        ModelPart copy = new ModelPart(indexes.isEmpty() ? List.copyOf(cubes) : indexes.stream().map(cubes::get).toList(), children);
        copy.x = source.x; copy.y = source.y; copy.z = source.z;
        copy.xRot = source.xRot; copy.yRot = source.yRot; copy.zRot = source.zRot;
        copy.xScale = source.xScale; copy.yScale = source.yScale; copy.zScale = source.zScale;
        copy.visible = source.visible; copy.skipDraw = source.skipDraw;
        copy.setInitialPose(copy.storePose());
        return copy;
    }

    private static ModelPart copySubtree(ModelPart source, int depth) {
        if (depth > 16) throw new IllegalArgumentException("Naturalist contract subtree is too deep");
        Map<String, ModelPart> children = new LinkedHashMap<>();
        Map<String, ModelPart> sourceChildren = ModelPartUtil.getChildren(source);
        if (sourceChildren != null) sourceChildren.forEach((name, child) -> children.put(name, copySubtree(child, depth + 1)));
        List<ModelPart.Cube> cubes = ModelPartUtil.getCubes(source);
        ModelPart copy = new ModelPart(cubes == null ? List.of() : List.copyOf(cubes), children);
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
        // Adult Zebra keeps the authored face/muzzle, ears, and only the upper neck branch.  Its
        // face cuboids and ears are siblings, so the exact neck contract is intentionally not a
        // face-only cube subset.  Baby Zebra has a distinct skull-and-ears tree.
        add(map, "zebra", cDetachedChildren("ZebraModel", "body/neck", "body/neck/neck_r1",
                p(.42F, 0.0F, 1.5708F, 0.0F), List.of("neck_r1", "leftEar", "rightEar")),
                cDetached("ZebraBabyModel", "body/neck/skull2", "body/neck/skull2", p(.62F, 0.0F, 1.5708F, 0.0F)));
        add(map, "giraffe", c("GiraffeModel", "hips/shoulders/body/neck/head", p(.55F)), c("GiraffeBabyModel", "body/neck", p(.75F)));
        add(map, "hippo", c("HippoModel", "body/bone/neck", p(.48F)), c("HippoBabyModel", "body/neck", p(.70F)));
        add(map, "vulture", cDetached("VultureModel", "neck"), cDetached("VultureBabyModel", "body/neck"));
        add(map, "boar", c("BoarModel", "body/neck", p(.72F)), c("BoarBabyModel", "body/neck", p(.80F)));
        add(map, "alligator", c("AlligatorModel", "body/neck", p(.58F)), c("AlligatorBabyModel", "body/neck", p(.76F)));
        add(map, "lizard", cDetached("LizardModel", "body/skullRot/neck", "body/skullRot/neck/neck_r1", p(1.0F)));
        add(map, "tortoise", cDetached("TortoiseModel", "body/skullRot/neck"), cDetached("TortoiseBabyModel", "body/skullRot/neck"));
        add(map, "duck", c("DuckModel", "body/neck", p(.72F, 0.0F, 1.5708F, 0.0F)), c("DuckBabyModel", "body/neck", p(.80F, 0.0F, 1.5708F, 0.0F)));
        add(map, "mole", cDetached("MoleModel", "root/body/skull"));
        add(map, "rat", c("RatModel", "body/skull", p(.74F, 0.0F, 1.5708F, 0.0F)));
        add(map, "black_bear", c("BlackBearModel", "body/skullRot/skull"), c("BlackBearBabyModel", "body/skull"));
        add(map, "tiger", c("TigerModel", "body/skullRot/skull", p(.76F)), c("TigerBabyModel", "body/skull", p(.82F)));
        add(map, "komodo_dragon", c("KomodoDragonModel", "body/neck", p(.68F, 0.0F, 1.5708F, 0.0F)));
        add(map, "ostrich", c("OstrichModel", "root/body/skull", p(.60F)), c("OstrichBabyModel", "body/skull", p(.76F)));
        add(map, "turkey", c("TurkeyModel", "body/skull"));
        add(map, "capybara", c("CapybaraModel", "body/skull"), c("CapybaraBabyModel", "body/skull"));
        // Both states use the C6-recognizable authored geometry; only their icon scale changes.
        add(map, "hedgehog", cVisibleDetached("HedgehogModel", "rolled", p(.68F)),
                cDetached("HedgehogModel", "unrolled/body", p(.70F)));
        // compact or non-headed anatomies: capture the explicit compact body subtree, never a sibling search
        add(map, "dragonfly", c("DragonflyModel", "", p(.62F)));
        add(map, "anglerfish", cDetachedChildren("AnglerfishModel", "root/body", p(.38F, 0.0F, 1.5708F, 0.0F), List.of("jaw", "dangly")));
        add(map, "ray", cDetached("RayModel", "body", "body", p(.38F)));
        add(map, "blobfish", c("BlobfishPinkModel", "", p(.72F)), c("BlobfishGrayModel", "", p(.72F)));
        add(map, "piranha", cDetached("PiranhaModel", "body", p(.60F, 0.0F, 1.5708F, 0.0F)));
        // All three authored Bass models run along Z.  A Y-quarter turn gives Xaero a compact
        // fish profile; Large Bass must retain head, body, fins, and tail rather than head-only.
        add(map, "bass", cDetached("BassModel", "body", "body", p(.82F, 0.0F, 1.5708F, 0.0F)),
                cDetached("MediumBassModel", "body", "body", p(.76F, 0.0F, 1.5708F, 0.0F)),
                cDetached("LargeBassModel", "", "body", p(.58F, 0.0F, 1.5708F, 0.0F)));
        add(map, "lizard_tail", c("LizardTailModel", ""));
        // Xaero 26.4.2's captured-success contract is ModelPart-trace based and has no item-sprite
        // output seam.  The installed Naturalist item is the generated
        // naturalist:orange_starfish -> naturalist:block/orange_starfish sprite, but it cannot
        // participate in Xaero's bounded rendered-part detector.  Keep a narrow entity fallback
        // with the real drawable body trace instead of caching a false 2-D success.
        // These models place visible anatomy beneath the model wrapper's authored `root` child.
        // Copy only the source-audited drawable siblings from that child, then normalize the
        // copied gameplay root so its y=21/24 placement cannot move the icon outside Xaero's
        // capture target.  The outer model wrapper itself has no drawable body/legs children.
        add(map, "starfish", cNormalizedDetachedChildren("StarfishModel", "root", "root/body",
                p(.58F, 1.5708F, 0.0F, 0.0F), List.of("body", "legs")));
        // C18 proves Xaero reaches this fallback route but records no rendered parts at 0.20F.
        // C19 isolates the first post-C7 scale experiment: retain the model-root / `bottom` trace,
        // normalized detached top/bottom/hinge shell assembly, and top-down orientation at 0.30F.
        add(map, "clam", cNormalizedDetachedChildren("ClamModel", "", "bottom",
                p(.30F, 1.5708F, 0.0F, 0.0F), List.of("top", "bottom", "hinge")));
        add(map, "giant_isopod", cVisibleDetached("GiantIsopodModel", "rolled", p(.45F)), cDetached("GiantIsopodModel", "body", p(.45F)));
        add(map, "jellyfish", cDetached("JellyfishModel", "body", p(.45F)));
        add(map, "whale", c("WhaleModel", "body/skullRot", p(.30F)), c("WhaleBabyModel", "body/skull", p(.60F)));
        // The authored trees differ.  Desert's body owns claws and tail while legs are a root
        // sibling; Jungle's body owns arms/claws and tail while legs are also a root sibling.
        // Keep separate, source-backed compact assemblies and normalize only their copied root
        // frame.  The drawable body remains the exact trace anchor for Xaero's success detector.
        add(map, "desert_scorpion", cNormalizedDetachedChildren("DesertScorpionModel", "root", "root/body",
                p(.34F, 1.5708F, 0.0F, 0.0F), List.of("body", "legs")));
        add(map, "jungle_scorpion", cNormalizedDetachedChildren("JungleScorpionModel", "root", "root/body",
                p(.28F, 1.5708F, 0.0F, 0.0F), List.of("body", "legs")));
        // C5's profile scale is retained.  The negative model-space frame Y correction exposes
        // the lower silhouette without changing the shark's size or side presentation.
        add(map, "great_white_shark", cNeutralRoot("GreatWhiteSharkModel", "body", p(.21F, 0.0F, 1.5708F, 0.0F, -4.0F)));
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
                path(slashPath), path(slashTracePath), presentation, cubeIndexes, List.of(), false, false, false, true);
    }
    private static Contract cVisible(String simpleName, String slashPath, Presentation presentation) {
        Contract base = c(simpleName, slashPath, presentation);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), true, false, false, true);
    }
    private static Contract cNeutralRoot(String simpleName, String slashPath, Presentation presentation) {
        Contract base = c(simpleName, slashPath, presentation);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), false, true, false, true);
    }
    private static Contract cDetached(String simpleName, String slashPath) { return detach(c(simpleName, slashPath)); }
    private static Contract cDetached(String simpleName, String slashPath, Presentation presentation) { return detach(c(simpleName, slashPath, presentation)); }
    private static Contract cDetached(String simpleName, String slashPath, String slashTracePath, Presentation presentation) { return detach(c(simpleName, slashPath, slashTracePath, presentation)); }
    private static Contract cDetached(String simpleName, String slashPath, String slashTracePath, Presentation presentation, List<Integer> cubeIndexes) { return detach(c(simpleName, slashPath, slashTracePath, presentation, cubeIndexes)); }
    private static Contract cVisibleDetached(String simpleName, String slashPath, Presentation presentation) { return detach(cVisible(simpleName, slashPath, presentation)); }
    private static Contract cDetachedChildren(String simpleName, String slashPath, Presentation presentation, List<String> drawableChildren) {
        return cDetachedChildren(simpleName, slashPath, slashPath, presentation, drawableChildren);
    }
    private static Contract cDetachedChildren(String simpleName, String slashPath, String slashTracePath, Presentation presentation, List<String> drawableChildren) {
        Contract base = c(simpleName, slashPath, slashTracePath, presentation);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), drawableChildren, false, false, false, false);
    }
    private static Contract cNormalizedDetachedChildren(String simpleName, String slashPath, String slashTracePath, Presentation presentation, List<String> drawableChildren) {
        Contract base = cDetachedChildren(simpleName, slashPath, slashTracePath, presentation, drawableChildren);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), base.requiresVisible(), base.neutralizeRootRotation(), true, base.preserveAncestorTransforms());
    }
    private static Contract detach(Contract base) {
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), base.requiresVisible(), base.neutralizeRootRotation(), base.normalizeSelectedRootTransform(), false);
    }
    private static List<String> path(String slashPath) { return slashPath.isEmpty() ? List.of() : List.of(slashPath.split("/")); }
    private static Presentation p(float scale) { return p(scale, 0.0F, 0.0F, 0.0F, 0.0F); }
    private static Presentation p(float scale, float xRotation, float yRotation, float zRotation) {
        return p(scale, xRotation, yRotation, zRotation, 0.0F);
    }
    private static Presentation p(float scale, float xRotation, float yRotation, float zRotation, float frameYOffset) {
        return new Presentation(scale, xRotation, yRotation, zRotation, frameYOffset);
    }
    private static void add(Map<String, List<Contract>> map, String id, Contract... contracts) { map.put(id, List.of(contracts)); }

    public record Contract(String modelClass, List<String> path, List<String> tracePath, Presentation presentation,
                           List<Integer> cubeIndexes, List<String> drawableChildren, boolean requiresVisible,
                           boolean neutralizeRootRotation, boolean normalizeSelectedRootTransform,
                           boolean preserveAncestorTransforms) {}
    /** Explicit icon-only presentation metadata; frameYOffset applies only to the copied adapter. */
    public record Presentation(float scale, float xRotation, float yRotation, float zRotation, float frameYOffset) {}
    public record ResolvedContract(Contract contract, ModelPart source, ModelPart selected, ModelPart trace) {}
}

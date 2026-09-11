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
        // C26 retains C25's exact head route and gives only the adult the requested 1.25x size increase.
        add(map, "hippo", c("HippoModel", "body/bone/neck", p(.75F)), c("HippoBabyModel", "body/neck", p(.70F)));
        add(map, "vulture", cDetached("VultureModel", "neck"), cDetached("VultureBabyModel", "body/neck"));
        add(map, "boar", c("BoarModel", "body/neck", p(.72F)), c("BoarBabyModel", "body/neck", p(.80F)));
        // Adult neck owns the skull plane plus the snout child.  A restrained source-model yaw
        // exposes its eye plane without changing its compact head geometry; baby remains proven.
        add(map, "alligator", c("AlligatorModel", "body/neck", p(.58F, 0.0F, .7854F, 0.0F)), c("AlligatorBabyModel", "body/neck", p(.76F)));
        // The exact neck subtree is retained; its authored forward axis is rotated into profile.
        add(map, "lizard", cDetached("LizardModel", "body/skullRot/neck", "body/skullRot/neck/neck_r1", p(1.0F, 0.0F, 1.5708F, 0.0F)));
        // Preserve both compact head subtrees and use only a model-space profile presentation.
        add(map, "tortoise", cDetached("TortoiseModel", "body/skullRot/neck", p(1.0F, 0.0F, 1.5708F, 0.0F)), cDetached("TortoiseBabyModel", "body/skullRot/neck", p(1.0F, 0.0F, 1.5708F, 0.0F)));
        add(map, "duck", c("DuckModel", "body/neck", p(.72F, 0.0F, 1.5708F, 0.0F)), c("DuckBabyModel", "body/neck", p(.80F, 0.0F, 1.5708F, 0.0F)));
        add(map, "mole", cDetached("MoleModel", "root/body/skull"));
        add(map, "rat", c("RatModel", "body/skull", p(.74F, 0.0F, 1.5708F, 0.0F)));
        // The adult skull contains the lower jaw/snout.  C25 moves only its copied icon frame
        // down one additional model unit to expose slightly more chin; no live model or baby change.
        add(map, "black_bear", c("BlackBearModel", "body/skullRot/skull", p(1.0F, 0.0F, 0.0F, 0.0F, -2.0F)), c("BlackBearBabyModel", "body/skull"));
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
        // StarfishModel passes its authored `root` child to EntityModel, therefore model.root()
        // already is that gameplay-positioning wrapper.  Its drawable body holds the disk and
        // fifth arm; the four other arms are under its legs sibling.  Copy both exact authored
        // children, normalize only that copied wrapper's y=24/yRot=pi placement, and trace its
        // drawable body rather than making a second, nonexistent `root` traversal.
        add(map, "starfish", cNormalizedDetachedChildrenWithTraceCenter("StarfishModel", "", "body",
                p(.52F, 1.5708F, 0.0F, 0.0F), List.of("body", "legs")));
        // C19 proves that the 0.30F top-down presentation can produce a real icon, but its
        // top/bottom/hinge assembly captures only part of the shell.  Naturalist's authored
        // `top` child contains the complete upper shell surface, so C20 renders and traces only
        // that exact child instead of asking Xaero to frame the lower shell or hinge as well.
        add(map, "clam", cNormalizedDetached("ClamModel", "top", "top",
                p(.30F, 1.5708F, 0.0F, 0.0F)));
        add(map, "giant_isopod", cVisibleDetached("GiantIsopodModel", "rolled", p(.45F)), cDetached("GiantIsopodModel", "body", p(.45F)));
        add(map, "jellyfish", cDetached("JellyfishModel", "body", p(.45F)));
        // C31 retains C30's complete adult skullRot face (cranium plus topJaw and bottomJaw), and
        // adds only body's direct torso cuboid.  The distant tail/tail2/fluke and both root-level
        // fins are deliberately omitted: this is the smallest audited head-plus-body silhouette,
        // rather than a tiny full Whale.  The copied selection is detached, while the exact live
        // drawable body remains the trace and Xaero frame center.  Its approximately 113-unit
        // head-to-torso extent stays at .20F. Its C31 67.5-degree model-space yaw is the
        // narrow intermediate step toward profile: only yaw changes, preserving C30 readability.
        // Baby Whale stays on the C29 safe fallback unchanged.
        add(map, "whale", cDetachedChildrenWithTraceCenter("WhaleModel", "body", "body",
                p(.20F, 0.0F, 1.1781F, 0.0F), List.of("skullRot")),
                c("WhaleBabyModel", "body/skull", p(.60F, 0.0F, .7854F, 0.0F)));
        // Both constructors pass root.getChild("root") to EntityModel, so model.root() already
        // is the authored root: a second `root` hop is invalid. Desert body owns claws/tail and
        // Jungle body owns arms/claws/tail; legs is a sibling in each. Keep the compact copied
        // assembly but nominate the live drawable body as both trace and Xaero center.
        add(map, "desert_scorpion", cNormalizedDetachedChildrenWithTraceCenter("DesertScorpionModel", "", "body",
                p(.34F, 1.5708F, 0.0F, 0.0F), List.of("body", "legs")));
        add(map, "jungle_scorpion", cNormalizedDetachedChildrenWithTraceCenter("JungleScorpionModel", "", "body",
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
                path(slashPath), path(slashTracePath), presentation, cubeIndexes, List.of(), false, false, false, false, true);
    }
    /** Keeps the complete source subtree while using an exact live drawable trace as Xaero's center. */
    private static Contract cWithTraceCenter(String simpleName, String slashPath, String slashTracePath, Presentation presentation) {
        Contract base = c(simpleName, slashPath, slashTracePath, presentation);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(),
                base.requiresVisible(), base.neutralizeRootRotation(), base.normalizeSelectedRootTransform(), true, base.preserveAncestorTransforms());
    }
    private static Contract cVisible(String simpleName, String slashPath, Presentation presentation) {
        Contract base = c(simpleName, slashPath, presentation);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), true, false, false, false, true);
    }
    private static Contract cNeutralRoot(String simpleName, String slashPath, Presentation presentation) {
        Contract base = c(simpleName, slashPath, presentation);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), false, true, false, false, true);
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
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), drawableChildren, false, false, false, false, false);
    }
    /** A detached direct-cube-plus-child assembly still needs a live cubed trace/frame center. */
    private static Contract cDetachedChildrenWithTraceCenter(String simpleName, String slashPath, String slashTracePath,
                                                               Presentation presentation, List<String> drawableChildren) {
        Contract base = cDetachedChildren(simpleName, slashPath, slashTracePath, presentation, drawableChildren);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(),
                base.requiresVisible(), base.neutralizeRootRotation(), base.normalizeSelectedRootTransform(), true, base.preserveAncestorTransforms());
    }
    private static Contract cNormalizedDetachedChildren(String simpleName, String slashPath, String slashTracePath, Presentation presentation, List<String> drawableChildren) {
        Contract base = cDetachedChildren(simpleName, slashPath, slashTracePath, presentation, drawableChildren);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), base.requiresVisible(), base.neutralizeRootRotation(), true, base.useTraceAsRenderCenter(), base.preserveAncestorTransforms());
    }
    /** A copied sibling assembly needs an authored live part for Xaero's direct-cuboid centering. */
    private static Contract cNormalizedDetachedChildrenWithTraceCenter(String simpleName, String slashPath, String slashTracePath, Presentation presentation, List<String> drawableChildren) {
        Contract base = cNormalizedDetachedChildren(simpleName, slashPath, slashTracePath, presentation, drawableChildren);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), base.requiresVisible(), base.neutralizeRootRotation(), base.normalizeSelectedRootTransform(), true, base.preserveAncestorTransforms());
    }
    private static Contract cNormalizedDetached(String simpleName, String slashPath, String slashTracePath, Presentation presentation) {
        Contract base = cDetached(simpleName, slashPath, slashTracePath, presentation);
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), base.requiresVisible(), base.neutralizeRootRotation(), true, base.useTraceAsRenderCenter(), base.preserveAncestorTransforms());
    }
    private static Contract detach(Contract base) {
        return new Contract(base.modelClass(), base.path(), base.tracePath(), base.presentation(), base.cubeIndexes(), base.drawableChildren(), base.requiresVisible(), base.neutralizeRootRotation(), base.normalizeSelectedRootTransform(), base.useTraceAsRenderCenter(), false);
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
                           boolean useTraceAsRenderCenter, boolean preserveAncestorTransforms) {}
    /** Explicit icon-only presentation metadata; frameYOffset applies only to the copied adapter. */
    public record Presentation(float scale, float xRotation, float yRotation, float zRotation, float frameYOffset) {}
    public record ResolvedContract(Contract contract, ModelPart source, ModelPart selected, ModelPart trace) {
        /** Xaero uses this part's direct cuboid as the render frame center. */
        public ModelPart renderCenter() { return contract.useTraceAsRenderCenter() ? trace : selected; }
    }
}

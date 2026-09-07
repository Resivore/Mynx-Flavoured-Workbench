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
            ModelPart selected = follow(model.root(), contract.path());
            if (selected != null) return Optional.of(new ResolvedContract(contract, selected));
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

    private static Map<String, List<Contract>> targets() {
        Map<String, List<Contract>> map = new LinkedHashMap<>();
        // ordinary headed animals: capture only the model's explicit head/neck subtree
        add(map, "rhino", c("RhinoModel", "root/body/attack/skullRot/neck"));
        add(map, "lion", c("LionModel", "body/neck"), c("LionBabyModel", "body/neck"));
        add(map, "elephant", c("ElephantModel", "body/skullRot/attack/neck"), c("ElephantBabyModel", "body/neck"));
        add(map, "mammoth", c("MammothModel", "body/skullRot/attack/neck"), c("MammothBabyModel", "body/neck"));
        add(map, "zebra", c("ZebraModel", "body/neck"), c("ZebraBabyModel", "body/neck"));
        add(map, "giraffe", c("GiraffeModel", "shoulders/body/neck/head"), c("GiraffeBabyModel", "body/neck"));
        add(map, "hippo", c("HippoModel", "body/bone/neck"), c("HippoBabyModel", "body/neck"));
        add(map, "vulture", c("VultureModel", "neck"), c("VultureBabyModel", "body/neck"));
        add(map, "boar", c("BoarModel", "body/neck"), c("BoarBabyModel", "body/neck"));
        add(map, "alligator", c("AlligatorModel", "body/neck"), c("AlligatorBabyModel", "body/neck"));
        add(map, "lizard", c("LizardModel", "body/skullRot/neck"));
        add(map, "tortoise", c("TortoiseModel", "body/skullRot/neck"), c("TortoiseBabyModel", "body/skullRot/neck"));
        add(map, "duck", c("DuckModel", "body/neck"), c("DuckBabyModel", "body/neck"));
        add(map, "mole", c("MoleModel", "root/body/skull"));
        add(map, "rat", c("RatModel", "body/skull"));
        add(map, "black_bear", c("BlackBearModel", "body/skullRot/skull"), c("BlackBearBabyModel", "body/skull"));
        add(map, "tiger", c("TigerModel", "body/skullRot/skull"), c("TigerBabyModel", "body/skull"));
        add(map, "komodo_dragon", c("KomodoDragonModel", "body/neck/skull"));
        add(map, "ostrich", c("OstrichModel", "root/body/skull"), c("OstrichBabyModel", "body/skull"));
        add(map, "turkey", c("TurkeyModel", "body/skull"));
        add(map, "capybara", c("CapybaraModel", "body/skull"), c("CapybaraBabyModel", "body/skull"));
        add(map, "hedgehog", c("HedgehogModel", "body"));
        // compact or non-headed anatomies: capture the explicit compact body subtree, never a sibling search
        add(map, "dragonfly", c("DragonflyModel", ""));
        add(map, "anglerfish", c("AnglerfishModel", "root/body"));
        add(map, "ray", c("RayModel", "body"));
        add(map, "blobfish", c("BlobfishPinkModel", ""), c("BlobfishGrayModel", ""));
        add(map, "piranha", c("PiranhaModel", "body"));
        add(map, "bass", c("BassModel", "body"), c("MediumBassModel", "body"), c("LargeBassModel", "body"));
        add(map, "lizard_tail", c("LizardTailModel", ""));
        add(map, "starfish", c("StarfishModel", ""));
        add(map, "clam", c("ClamModel", ""));
        add(map, "giant_isopod", c("GiantIsopodModel", "body"));
        add(map, "jellyfish", c("JellyfishModel", "body"));
        add(map, "whale", c("WhaleModel", "body/skullRot"), c("WhaleBabyModel", "body/skull"));
        add(map, "desert_scorpion", c("DesertScorpionModel", ""));
        add(map, "jungle_scorpion", c("JungleScorpionModel", ""));
        add(map, "great_white_shark", c("GreatWhiteSharkModel", "body/skullRot"));
        return Map.copyOf(map);
    }

    private static Contract c(String simpleName, String slashPath) {
        return new Contract("com.crispytwig.naturalist.client.model." + simpleName,
                slashPath.isEmpty() ? List.of() : List.of(slashPath.split("/")));
    }
    private static void add(Map<String, List<Contract>> map, String id, Contract... contracts) { map.put(id, List.of(contracts)); }

    public record Contract(String modelClass, List<String> path) {}
    public record ResolvedContract(Contract contract, ModelPart selected) {}
}

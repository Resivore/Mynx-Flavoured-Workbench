package dev.resivore.villagerwork.client;

import dev.resivore.villagerwork.mixin.client.ModelPartChildrenAccessor;
import dev.resivore.villagerwork.mixin.client.VillagerModelArmsAccessor;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Throttled, observation-only diagnostics for the temporary C22 evidence canary. */
final class VwrRodDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_work_routines/rod_diagnostic");
    private static final int MAX_TRACKED_CASTS = 64;
    private static final int MAX_TRACKED_STATES = 16;
    private static final int MAX_HIERARCHY_DEPTH = 6;
    private static final int MAX_HIERARCHY_PARTS = 128;

    private static final Map<Integer, RenderObservation> RENDERS_BY_VILLAGER = new LinkedHashMap<>();
    private static final Map<Integer, LineObservation> LINES_BY_VILLAGER = new LinkedHashMap<>();
    private static final Map<Long, Boolean> REPORTED_CASTS = new LinkedHashMap<>();
    private static final Set<String> REPORTED_RESOURCE_STATES = new LinkedHashSet<>();
    private static final Set<String> REPORTED_HIERARCHIES = new LinkedHashSet<>();
    private static final IdentityHashMap<VillagerModel, HierarchyObservation> HIERARCHY_CACHE =
            new IdentityHashMap<>();
    private static Object observedLevel;

    private VwrRodDiagnostics() {
    }

    static synchronized void observeRenderPath(Villager villager, int floatId, VillagerModel model,
                                                boolean submitted, Matrix4f incomingEntityLayer,
                                                Matrix4f afterTranslateToArms,
                                                Matrix4f afterAuthoredGrip,
                                                RibbitsFishermanRodRenderer.Inspection inspection) {
        resetForLevelChange();
        String resourceState = inspection.resourceSignature()
                + "|exactLiveCapture=" + inspection.exactLiveCapture();
        if (addBounded(REPORTED_RESOURCE_STATES, resourceState)) {
            LOGGER.info("[VWR C22 ROD DIAGNOSTIC: RESOURCE/MODEL]\n{}\n[/VWR C22 ROD DIAGNOSTIC]",
                    inspection.resourceReport());
        }

        HierarchyObservation hierarchy = hierarchy(model);
        if (addBounded(REPORTED_HIERARCHIES, hierarchy.signature())) {
            LOGGER.info("[VWR C22 ROD DIAGNOSTIC: LIVE VILLAGER MODEL]\n{}\n[/VWR C22 ROD DIAGNOSTIC]",
                    hierarchy.report());
        }

        putBounded(RENDERS_BY_VILLAGER, villager.getId(), new RenderObservation(
                villager.getId(), floatId, model.getClass().getName(), submitted,
                new Matrix4f(incomingEntityLayer), new Matrix4f(afterTranslateToArms),
                new Matrix4f(afterAuthoredGrip), inspection, currentArmsPartState(model)));
        reportCastIfComplete(villager.getId());
    }

    /** Called from the restored C20 float renderer at the exact line-start assignment. */
    static synchronized void observeLinePath(int floatId, int villagerId, Vec3 c20LineStartWorld,
                                             Vec3 floatWorld, Vec3 floatLocalLineVector) {
        resetForLevelChange();
        putBounded(LINES_BY_VILLAGER, villagerId, new LineObservation(floatId, villagerId,
                c20LineStartWorld, floatWorld, floatLocalLineVector));
        reportCastIfComplete(villagerId);
    }

    private static void reportCastIfComplete(int villagerId) {
        RenderObservation render = RENDERS_BY_VILLAGER.get(villagerId);
        LineObservation line = LINES_BY_VILLAGER.get(villagerId);
        if (render == null || line == null || render.floatId() != line.floatId()) return;

        long castKey = ((long) villagerId << 32) ^ Integer.toUnsignedLong(line.floatId());
        boolean complete = render.rod().exactLiveCapture();
        Boolean previousComplete = REPORTED_CASTS.get(castKey);
        if (previousComplete != null && (previousComplete || !complete)) return;
        putBounded(REPORTED_CASTS, castKey, complete);

        Vec3 cameraWorld = Minecraft.getInstance().gameRenderer.gameRenderState()
                .levelRenderState.cameraRenderState.pos;
        RibbitsFishermanRodRenderer.Inspection rod = render.rod();
        Vec3 incomingOrigin = transform(render.incomingEntityLayer(), 0.0F, 0.0F, 0.0F);
        Vec3 armsOrigin = transform(render.afterTranslateToArms(), 0.0F, 0.0F, 0.0F);
        Vec3 authoredGripOrigin = transform(render.afterAuthoredGrip(), 0.0F, 0.0F, 0.0F);
        Vec3 lineStartRender = line.c20LineStartWorld().subtract(cameraWorld);

        StringBuilder report = new StringBuilder(4096);
        report.append("villager_id=").append(villagerId)
                .append(" float_id=").append(line.floatId())
                .append(" runtime_model_class=").append(render.runtimeModelClass()).append('\n');
        report.append("spaces: matrix checkpoints map their named local space into camera-relative render space; ")
                .append("*_world values are derived only by adding camera_world; model pixels are explicitly labeled.\n");
        report.append("camera_world=").append(vector(cameraWorld)).append('\n');
        report.append("arms_part_at_submission=").append(render.armsPartState()).append('\n');

        appendCheckpoint(report, "incoming_entity_layer (entity-local -> camera-relative)",
                render.incomingEntityLayer(), incomingOrigin, cameraWorld);
        appendCheckpoint(report, "after_C20_translateToArms (arms-local -> camera-relative)",
                render.afterTranslateToArms(), armsOrigin, cameraWorld);
        appendCheckpoint(report, "after_C20_authored_grip_[0,-7,-6]px (rod-group-local -> camera-relative)",
                render.afterAuthoredGrip(), authoredGripOrigin, cameraWorld);

        report.append("rod_submission_succeeded=").append(render.submitted())
                .append(" exact_live_GeckoLib_matrix_capture=").append(rod.exactLiveCapture()).append('\n');
        if (rod.failure() != null) report.append("rod_submission_or_probe_failure=").append(rod.failure()).append('\n');
        if (rod.rawGripPosition() != null) {
            report.append("standalone_raw_grip_before_rebase (Geo object render space)=")
                    .append(vector(rod.rawGripPosition())).append('\n');
        }
        if (rod.geometryRootMatrix() != null) {
            appendCheckpoint(report,
                    "after_C20_standalone_rebase_and_GeckoLib_geometry_root (geo-local -> camera-relative)",
                    rod.geometryRootMatrix(), rod.geometryRootOrigin(), cameraWorld);
        }
        if (rod.rodPivotMatrix() != null) {
            appendCheckpoint(report,
                    "live_fishing_rod_pivot (rod-local -> camera-relative; BoneSnapshots applied)",
                    rod.rodPivotMatrix(), rod.physicalGrip(), cameraWorld);
        }
        if (rod.physicalOuterTip() != null) {
            report.append("physical_outer_shaft_tip model_local_from_pivot=(0, 0, -9.5px)\n")
                    .append("  camera_relative_render=").append(vector(rod.physicalOuterTip())).append('\n')
                    .append("  derived_world=").append(vector(rod.physicalOuterTip().add(cameraWorld))).append('\n');
        }

        report.append("C20_line_start_currently_used_world=").append(vector(line.c20LineStartWorld())).append('\n')
                .append("C20_line_start_currently_used_camera_relative=").append(vector(lineStartRender)).append('\n')
                .append("float_world=").append(vector(line.floatWorld())).append('\n')
                .append("C20_line_vector_float_local=").append(vector(line.floatLocalLineVector())).append('\n');
        if (rod.physicalOuterTip() != null) {
            Vec3 discrepancy = lineStartRender.subtract(rod.physicalOuterTip());
            report.append("diagnostic_only_line_start_minus_visible_tip_camera_relative=")
                    .append(vector(discrepancy)).append(" distance=")
                    .append(format(discrepancy.length())).append('\n');
        }
        report.append("IMPORTANT: the measured physical tip is diagnostic-only; C22 still submits the line from C20's analytical endpoint.");

        LOGGER.info("[VWR C22 ROD DIAGNOSTIC: LIVE CAST TRANSFORMS]\n{}\n[/VWR C22 ROD DIAGNOSTIC]",
                report);
    }

    private static HierarchyObservation hierarchy(VillagerModel model) {
        HierarchyObservation cached = HIERARCHY_CACHE.get(model);
        if (cached != null) return cached;
        if (HIERARCHY_CACHE.size() >= MAX_TRACKED_STATES) HIERARCHY_CACHE.clear();
        HierarchyObservation inspected = inspectHierarchy(model);
        HIERARCHY_CACHE.put(model, inspected);
        return inspected;
    }

    private static HierarchyObservation inspectHierarchy(VillagerModel model) {
        try {
            ModelPart arms = ((VillagerModelArmsAccessor) model).villagerWork$getArms();
            Map<String, ModelPart> directChildren = children(arms);
            List<String> names = directChildren.keySet().stream().sorted().toList();
            StringBuilder report = new StringBuilder(2048);
            report.append("runtime_model_class=").append(model.getClass().getName()).append('\n')
                    .append("arms_part=").append(partState(arms)).append('\n')
                    .append("arms_direct_children_exact_names=").append(names).append('\n')
                    .append("arms_rotation_direct_child_exists=").append(directChildren.containsKey("arms_rotation"))
                    .append('\n').append("arms_descendants (max_depth=").append(MAX_HIERARCHY_DEPTH)
                    .append(", max_parts=").append(MAX_HIERARCHY_PARTS).append("):");

            IdentityHashMap<ModelPart, Boolean> visited = new IdentityHashMap<>();
            int[] count = {0};
            appendPartTree(report, "arms", arms, 0, visited, count);
            String signature = model.getClass().getName() + '/' + treeNames(arms);
            return new HierarchyObservation(signature, report.toString(), partState(arms));
        } catch (RuntimeException | LinkageError error) {
            String failure = error.getClass().getName() + ": " + String.valueOf(error.getMessage());
            return new HierarchyObservation(model.getClass().getName() + "/failure/" + failure,
                    "runtime_model_class=" + model.getClass().getName()
                            + "\narms_part=<unavailable>\narms_direct_children_exact_names=<unavailable>"
                            + "\narms_rotation_direct_child_exists=<unavailable>"
                            + "\nhierarchy_failure=" + failure,
                    "<unavailable: " + failure + '>' );
        }
    }

    private static void appendPartTree(StringBuilder report, String path, ModelPart part, int depth,
                                       IdentityHashMap<ModelPart, Boolean> visited, int[] count) {
        if (count[0] >= MAX_HIERARCHY_PARTS) return;
        report.append('\n').append("  ".repeat(Math.min(depth + 1, MAX_HIERARCHY_DEPTH + 1)))
                .append(path).append(' ').append(partState(part));
        count[0]++;
        if (depth >= MAX_HIERARCHY_DEPTH || visited.put(part, Boolean.TRUE) != null) return;
        List<Map.Entry<String, ModelPart>> descendants = new ArrayList<>(children(part).entrySet());
        descendants.sort(Map.Entry.comparingByKey());
        for (Map.Entry<String, ModelPart> child : descendants) {
            if (count[0] >= MAX_HIERARCHY_PARTS) break;
            appendPartTree(report, path + '/' + child.getKey(), child.getValue(), depth + 1, visited, count);
        }
    }

    private static String treeNames(ModelPart root) {
        StringBuilder result = new StringBuilder();
        appendTreeNames(result, "arms", root, 0, new IdentityHashMap<>(), new int[]{0});
        return result.toString();
    }

    private static void appendTreeNames(StringBuilder result, String path, ModelPart part, int depth,
                                        IdentityHashMap<ModelPart, Boolean> visited, int[] count) {
        if (depth > MAX_HIERARCHY_DEPTH || count[0] >= MAX_HIERARCHY_PARTS
                || visited.put(part, Boolean.TRUE) != null) return;
        result.append(path).append(';');
        count[0]++;
        children(part).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                appendTreeNames(result, path + '/' + entry.getKey(), entry.getValue(), depth + 1,
                        visited, count));
    }

    private static Map<String, ModelPart> children(ModelPart part) {
        return ((ModelPartChildrenAccessor) (Object) part).villagerWork$getChildren();
    }

    private static String partState(ModelPart part) {
        return String.format(Locale.ROOT,
                "pivot_px=(%.6f, %.6f, %.6f) rotation_rad=(%.6f, %.6f, %.6f) "
                        + "rotation_deg=(%.3f, %.3f, %.3f) scale=(%.6f, %.6f, %.6f) visible=%s skipDraw=%s",
                part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                Math.toDegrees(part.xRot), Math.toDegrees(part.yRot), Math.toDegrees(part.zRot),
                part.xScale, part.yScale, part.zScale, part.visible, part.skipDraw);
    }

    private static String currentArmsPartState(VillagerModel model) {
        try {
            return partState(((VillagerModelArmsAccessor) model).villagerWork$getArms());
        } catch (RuntimeException | LinkageError error) {
            return "<unavailable: " + error.getClass().getName() + ": "
                    + String.valueOf(error.getMessage()) + '>';
        }
    }

    private static void resetForLevelChange() {
        Object currentLevel = Minecraft.getInstance().level;
        if (currentLevel == observedLevel) return;
        observedLevel = currentLevel;
        RENDERS_BY_VILLAGER.clear();
        LINES_BY_VILLAGER.clear();
        REPORTED_CASTS.clear();
        REPORTED_RESOURCE_STATES.clear();
        REPORTED_HIERARCHIES.clear();
        HIERARCHY_CACHE.clear();
    }

    private static void appendCheckpoint(StringBuilder report, String name, Matrix4fc matrix,
                                         Vec3 renderOrigin, Vec3 cameraWorld) {
        report.append(name).append('\n')
                .append("  matrix_rows=").append(matrix(matrix)).append('\n')
                .append("  origin_camera_relative_render=").append(vector(renderOrigin)).append('\n')
                .append("  origin_derived_world=").append(vector(renderOrigin.add(cameraWorld))).append('\n');
    }

    private static Vec3 transform(Matrix4fc matrix, float x, float y, float z) {
        Vector4f transformed = matrix.transform(new Vector4f(x, y, z, 1.0F));
        return new Vec3(transformed.x(), transformed.y(), transformed.z());
    }

    private static String matrix(Matrix4fc m) {
        return String.format(Locale.ROOT,
                "[[%.6f, %.6f, %.6f, %.6f], [%.6f, %.6f, %.6f, %.6f], "
                        + "[%.6f, %.6f, %.6f, %.6f], [%.6f, %.6f, %.6f, %.6f]]",
                m.m00(), m.m10(), m.m20(), m.m30(),
                m.m01(), m.m11(), m.m21(), m.m31(),
                m.m02(), m.m12(), m.m22(), m.m32(),
                m.m03(), m.m13(), m.m23(), m.m33());
    }

    private static String vector(Vec3 vector) {
        return String.format(Locale.ROOT, "(%.6f, %.6f, %.6f)", vector.x, vector.y, vector.z);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static <K, V> void putBounded(Map<K, V> map, K key, V value) {
        map.put(key, value);
        while (map.size() > MAX_TRACKED_CASTS) map.remove(map.keySet().iterator().next());
    }

    private static <T> boolean addBounded(Set<T> set, T value) {
        if (!set.add(value)) return false;
        while (set.size() > MAX_TRACKED_STATES) set.remove(set.iterator().next());
        return true;
    }

    private record HierarchyObservation(String signature, String report, String armsPartState) {
    }

    private record RenderObservation(int villagerId, int floatId, String runtimeModelClass,
                                     boolean submitted,
                                     Matrix4f incomingEntityLayer, Matrix4f afterTranslateToArms,
                                     Matrix4f afterAuthoredGrip,
                                     RibbitsFishermanRodRenderer.Inspection rod,
                                     String armsPartState) {
    }

    private record LineObservation(int floatId, int villagerId, Vec3 c20LineStartWorld,
                                   Vec3 floatWorld, Vec3 floatLocalLineVector) {
    }
}

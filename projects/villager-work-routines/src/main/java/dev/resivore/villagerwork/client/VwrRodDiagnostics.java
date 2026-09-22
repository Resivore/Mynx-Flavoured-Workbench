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

/** Throttled diagnostics retained for direct verification of the focused C25 correction. */
final class VwrRodDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_work_routines/rod_diagnostic");
    private static final int MAX_TRACKED_CASTS = 64;
    private static final int MAX_TRACKED_STATES = 16;
    private static final int MAX_HIERARCHY_DEPTH = 6;
    private static final int MAX_HIERARCHY_PARTS = 128;

    private static final Map<Integer, RenderObservation> RENDERS_BY_VILLAGER = new LinkedHashMap<>();
    private static final Map<Long, Boolean> REPORTED_CASTS = new LinkedHashMap<>();
    private static final Set<String> REPORTED_RESOURCE_STATES = new LinkedHashSet<>();
    private static final Set<String> REPORTED_HIERARCHIES = new LinkedHashSet<>();
    private static final IdentityHashMap<VillagerModel, HierarchyObservation> HIERARCHY_CACHE =
            new IdentityHashMap<>();
    private static Object observedLevel;

    private VwrRodDiagnostics() {
    }

    static synchronized void observeAttachmentPath(VillagerModel model,
                                                    FoldedArmRenderPath.Attachment attachment) {
        resetForLevelChange();
        reportHierarchy(model, attachment);
    }

    static synchronized void observeRenderPath(Villager villager, int floatId, VillagerModel model,
                                                boolean submitted, Matrix4f incomingEntityLayer,
                                                FoldedArmRenderPath.Attachment attachment,
                                                Matrix4f afterAuthoredGrip,
                                                RibbitsFishermanRodRenderer.Inspection inspection,
                                                FishingFloatRenderer.LineSubmission line) {
        resetForLevelChange();
        String resourceState = inspection.resourceSignature()
                + "|exactLiveCapture=" + inspection.exactLiveCapture();
        if (addBounded(REPORTED_RESOURCE_STATES, resourceState)) {
            LOGGER.info("[VWR C24 ROD DIAGNOSTIC: RESOURCE/MODEL]\n{}\n[/VWR C24 ROD DIAGNOSTIC]",
                    inspection.resourceReport());
        }
        reportHierarchy(model, attachment);

        putBounded(RENDERS_BY_VILLAGER, villager.getId(), new RenderObservation(
                floatId, model.getClass().getName(), submitted,
                new Matrix4f(incomingEntityLayer), attachment,
                new Matrix4f(afterAuthoredGrip), inspection, line,
                currentArmsPartState(model)));
        reportCast(villager.getId());
    }

    private static void reportHierarchy(VillagerModel model,
                                        FoldedArmRenderPath.Attachment attachment) {
        HierarchyObservation hierarchy = hierarchy(model);
        String signature = hierarchy.signature() + "|applied=" + attachment.applied()
                + "|path=" + attachment.path() + "|failure=" + attachment.failure();
        if (!addBounded(REPORTED_HIERARCHIES, signature)) return;

        StringBuilder report = new StringBuilder(hierarchy.report());
        report.append('\n').append("effective_folded_arm_selection_rule=")
                .append(FoldedArmRenderPath.SELECTION_RULE).append('\n')
                .append("effective_folded_arm_path_applied=").append(attachment.applied()).append('\n')
                .append("effective_folded_arm_path=").append(attachment.path()).append('\n');
        if (attachment.failure() != null) {
            report.append("effective_folded_arm_path_failure=")
                    .append(attachment.failure()).append('\n');
        }
        if (!attachment.steps().isEmpty()) {
            report.append("effective_folded_arm_path_live_parts:");
            StringBuilder path = new StringBuilder();
            for (FoldedArmRenderPath.PartStep step : attachment.steps()) {
                if (!path.isEmpty()) path.append('/');
                path.append(step.name());
                report.append('\n').append("  ").append(path).append(' ')
                        .append("authored_id=").append(step.authoredId()).append(' ')
                        .append("part_to_be_attached=").append(step.attachedPart()).append(' ')
                        .append(partState(step.part()));
            }
        }
        LOGGER.info("[VWR C24 ROD DIAGNOSTIC: LIVE VILLAGER MODEL]\n{}\n[/VWR C24 ROD DIAGNOSTIC]",
                report);
    }

    private static void reportCast(int villagerId) {
        RenderObservation render = RENDERS_BY_VILLAGER.get(villagerId);
        if (render == null) return;

        long castKey = ((long) villagerId << 32) ^ Integer.toUnsignedLong(render.floatId());
        boolean complete = render.rod().exactLiveCapture()
                && render.line() != null && render.line().submitted();
        Boolean previousComplete = REPORTED_CASTS.get(castKey);
        if (previousComplete != null && (previousComplete || !complete)) return;
        putBounded(REPORTED_CASTS, castKey, complete);

        Vec3 cameraWorld = Minecraft.getInstance().gameRenderer.gameRenderState()
                .levelRenderState.cameraRenderState.pos;
        RibbitsFishermanRodRenderer.Inspection rod = render.rod();
        Vec3 incomingOrigin = transform(render.incomingEntityLayer(), 0.0F, 0.0F, 0.0F);
        Vec3 outerArmsOrigin = transform(render.attachment().afterTranslateToArms(),
                0.0F, 0.0F, 0.0F);
        Vec3 effectiveArmsOrigin = transform(render.attachment().afterEffectiveFoldedArms(),
                0.0F, 0.0F, 0.0F);
        Vec3 authoredGripOrigin = transform(render.afterAuthoredGrip(), 0.0F, 0.0F, 0.0F);

        StringBuilder report = new StringBuilder(4096);
        report.append("villager_id=").append(villagerId)
                .append(" float_id=").append(render.floatId())
                .append(" runtime_model_class=").append(render.runtimeModelClass()).append('\n');
        report.append("spaces: matrix checkpoints map their named local space into camera-relative render space; ")
                .append("*_world values are derived only by adding camera_world; model pixels are explicitly labeled.\n");
        report.append("camera_world=").append(vector(cameraWorld)).append('\n')
                .append("arms_part_at_submission=").append(render.armsPartState()).append('\n')
                .append("effective_folded_arm_selection_rule=")
                .append(FoldedArmRenderPath.SELECTION_RULE).append('\n')
                .append("effective_folded_arm_path_actually_used=")
                .append(render.attachment().path()).append('\n');

        appendCheckpoint(report, "incoming_entity_layer (entity-local -> camera-relative)",
                render.incomingEntityLayer(), incomingOrigin, cameraWorld);
        appendCheckpoint(report, "after_translateToArms (outer-arms-local -> camera-relative)",
                render.attachment().afterTranslateToArms(), outerArmsOrigin, cameraWorld);
        appendCheckpoint(report,
                "after_effective_folded_arm_path (authored-parent-local -> camera-relative)",
                render.attachment().afterEffectiveFoldedArms(), effectiveArmsOrigin, cameraWorld);
        appendCheckpoint(report,
                "after_C25_authored_grip_[0,-7,-6]px_EMF_mapped_live_[0,+7,-6]px"
                        + "_then_local_[0,+2,+1]px "
                        + "(rod-group-local -> camera-relative)",
                render.afterAuthoredGrip(), authoredGripOrigin, cameraWorld);

        report.append("rod_submission_succeeded=").append(render.submitted())
                .append(" exact_live_GeckoLib_matrix_capture=")
                .append(rod.exactLiveCapture()).append('\n');
        if (rod.failure() != null) {
            report.append("rod_submission_or_probe_failure=").append(rod.failure()).append('\n');
        }
        if (rod.rawGripPosition() != null) {
            report.append("standalone_raw_grip_before_rebase (Geo object render space)=")
                    .append(vector(rod.rawGripPosition())).append('\n');
        }
        if (rod.geometryRootMatrix() != null) {
            appendCheckpoint(report,
                    "after_standalone_rebase_and_GeckoLib_geometry_root (geo-local -> camera-relative)",
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
                    .append("  derived_world=").append(vector(rod.physicalOuterTip().add(cameraWorld)))
                    .append('\n');
        }

        FishingFloatRenderer.LineSubmission line = render.line();
        if (line == null) {
            report.append("C25_line_submitted=false line_authority=<unavailable live physical tip>\n");
        } else {
            report.append("C25_line_submitted=").append(line.submitted())
                    .append(" segment_count=").append(line.segmentCount()).append('\n')
                    .append("C25_line_start_authority=live_fishing_rod_physical_outer_tip\n")
                    .append("C25_line_first_vertex_camera_relative=")
                    .append(vector(line.physicalTipRender())).append('\n')
                    .append("C25_line_start_world=").append(vector(line.physicalTipWorld())).append('\n')
                    .append("float_world=").append(vector(line.floatWorld())).append('\n')
                    .append("float_camera_relative=").append(vector(line.floatRender())).append('\n');
            if (rod.physicalOuterTip() != null) {
                Vec3 discrepancy = line.physicalTipRender().subtract(rod.physicalOuterTip());
                report.append("C25_line_start_minus_visible_tip_camera_relative=")
                        .append(vector(discrepancy)).append(" distance=")
                        .append(format(discrepancy.length())).append('\n');
            }
        }
        report.append("IMPORTANT: C26 submits the sole line from the current C25 physical tip; "
                + "the C24 comparison ghost has no line.");

        LOGGER.info("[VWR C26 ROD DIAGNOSTIC: LIVE CAST TRANSFORMS]\n{}\n[/VWR C26 ROD DIAGNOSTIC]",
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
            List<String> names = children(arms).keySet().stream().sorted().toList();
            StringBuilder report = new StringBuilder(2048);
            report.append("runtime_model_class=").append(model.getClass().getName()).append('\n')
                    .append("arms_part=").append(partState(arms)).append('\n')
                    .append("arms_direct_children_exact_names=").append(names).append('\n')
                    .append("arms_descendants (max_depth=").append(MAX_HIERARCHY_DEPTH)
                    .append(", max_parts=").append(MAX_HIERARCHY_PARTS).append("):");

            IdentityHashMap<ModelPart, Boolean> visited = new IdentityHashMap<>();
            int[] count = {0};
            appendPartTree(report, "arms", arms, 0, visited, count);
            String signature = model.getClass().getName() + '/' + treeNames(arms);
            return new HierarchyObservation(signature, report.toString());
        } catch (RuntimeException | LinkageError error) {
            String failure = error.getClass().getName() + ": " + String.valueOf(error.getMessage());
            return new HierarchyObservation(model.getClass().getName() + "/failure/" + failure,
                    "runtime_model_class=" + model.getClass().getName()
                            + "\narms_part=<unavailable>"
                            + "\narms_direct_children_exact_names=<unavailable>"
                            + "\nhierarchy_failure=" + failure);
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
            appendPartTree(report, path + '/' + child.getKey(), child.getValue(), depth + 1,
                    visited, count);
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
                        + "rotation_deg=(%.3f, %.3f, %.3f) scale=(%.6f, %.6f, %.6f) "
                        + "visible=%s skipDraw=%s direct_cubes=%s",
                part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                Math.toDegrees(part.xRot), Math.toDegrees(part.yRot), Math.toDegrees(part.zRot),
                part.xScale, part.yScale, part.zScale, part.visible, part.skipDraw, !part.isEmpty());
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
        if (vector == null) return "<unavailable>";
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

    private record HierarchyObservation(String signature, String report) {
    }

    private record RenderObservation(int floatId, String runtimeModelClass, boolean submitted,
                                     Matrix4f incomingEntityLayer,
                                     FoldedArmRenderPath.Attachment attachment,
                                     Matrix4f afterAuthoredGrip,
                                     RibbitsFishermanRodRenderer.Inspection rod,
                                     FishingFloatRenderer.LineSubmission line,
                                     String armsPartState) {
    }
}

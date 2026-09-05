package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

class UpstreamCaptureContractTest {
    private static MethodNode method(String owner, String name) throws Exception {
        try (var zip = new ZipFile(System.getProperty("xaeroJar"))) {
            var node = new ClassNode();
            new ClassReader(zip.getInputStream(zip.getEntry(owner + ".class"))).accept(node, 0);
            return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
        }
    }
    private static List<String> calls(MethodNode method) {
        List<String> result = new ArrayList<>();
        for (var insn : method.instructions) if (insn instanceof MethodInsnNode call) result.add(call.name);
        return result;
    }
    @Test void creatorResetsInheritedPoseBeforeCallingTheReplacement() throws Exception {
        var owner = "xaero/hud/minimap/radar/icon/creator/RadarIconCreator";
        var setup = calls(method(owner, "setupMatrices"));
        assertTrue(setup.contains("setIdentity"));
        assertTrue(setup.contains("identity"), "model-view stack is also reset");
        assertFalse(setup.contains("mulPose"));
        var create = calls(method(owner, "create"));
        assertTrue(create.indexOf("setupMatrices") < create.indexOf("prerender"));
    }
    @Test void captureAndDisplaySplitTheUserScaleAtOne() throws Exception {
        var form = method("xaero/hud/minimap/radar/icon/creator/render/form/model/RadarIconModelFormPrerenderer", "prerender");
        boolean factor32 = false, parameterScale = false, clampBranch = false;
        for (var insn : form.instructions) {
            if (insn instanceof IntInsnNode n && n.operand == 32) factor32 = true;
            if (insn instanceof FieldInsnNode n && n.owner.endsWith("RadarIconCreator$Parameters") && n.name.equals("scale")) parameterScale = true;
            if (parameterScale && insn.getOpcode() == Opcodes.IFGE) clampBranch = true;
        }
        assertTrue(factor32 && parameterScale && clampBranch);
        var display = method("xaero/hud/minimap/radar/render/element/RadarRenderer", "renderIcon");
        assertTrue(calls(display).contains("max"));
        assertTrue(calls(display).indexOf("max") < calls(display).indexOf("scale"));
        assertFalse(calls(display).contains("min"));
        var flipped = method("xaero/hud/minimap/radar/icon/creator/render/form/model/RadarIconModelFormPrerenderer", "isFlipped");
        assertTrue(java.util.stream.StreamSupport.stream(flipped.instructions.spliterator(), false)
                .anyMatch(i -> i.getOpcode() == Opcodes.ICONST_0));
    }
}

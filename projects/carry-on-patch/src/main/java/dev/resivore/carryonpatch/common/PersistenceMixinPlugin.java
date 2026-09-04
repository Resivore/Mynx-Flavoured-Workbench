package dev.resivore.carryonpatch.common;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Replaces only the two upstream injected persistence callbacks, not vanilla player saving.
 * Injector handler names are renamed by Mixin; match the audited suffix and descriptor,
 * fail closed unless each callback is unique. This also removes upstream's call to the
 * nonexistent 26.2 ValueInput.contains method. No upstream classes are redistributed. */
public final class PersistenceMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String pkg) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) { return true; }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (!mixin.endsWith(".PlayerCarryStateMixin")) return;
        replace(node, "grabandgo$readAdditionalSaveData", "ValueInput", "load");
        replace(node, "grabandgo$addAdditionalSaveData", "ValueOutput", "save");
    }

    private static void replace(ClassNode node, String suffix, String valueType, String hook) {
        String value = "Lnet/minecraft/world/level/storage/" + valueType + ";";
        var matches = node.methods.stream().filter(m -> m.name.endsWith(suffix)
                && m.desc.equals("(" + value + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V")).toList();
        if (matches.size() != 1) throw new IllegalStateException("Unsupported GrabAndGo persistence callback: " + suffix);
        MethodNode method = matches.getFirst();
        method.instructions.clear(); method.tryCatchBlocks.clear();
        if (method.localVariables != null) method.localVariables.clear();
        method.visibleLocalVariableAnnotations = null; method.invisibleLocalVariableAnnotations = null;
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "dev/resivore/carryonpatch/common/CarryPersistence", hook,
                "(Lnet/minecraft/world/entity/player/Player;" + value + ")V", false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 2; method.maxLocals = 3;
    }
}

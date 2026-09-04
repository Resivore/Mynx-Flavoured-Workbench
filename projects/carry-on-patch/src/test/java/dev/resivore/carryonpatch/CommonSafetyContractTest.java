package dev.resivore.carryonpatch;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

class CommonSafetyContractTest {
    @Test void commonClassesCannotResolveClientRenderingOrAssignSyntheticIds() throws Exception {
        try(var zip=new ZipFile(Path.of(System.getProperty("patchJar")).toFile())) {
            var entries=zip.stream().filter(e -> e.getName().contains("/common/") && e.getName().endsWith(".class")).toList();
            assertTrue(entries.size()>=8);
            for(var entry:entries) {
                byte[] bytes=zip.getInputStream(entry).readAllBytes();
                String constants=new String(bytes,StandardCharsets.ISO_8859_1);
                assertFalse(constants.contains("net/minecraft/client/"),entry.getName());
                assertFalse(constants.contains("RenderOnlyEntityIds"),entry.getName());
                assertFalse(constants.contains("dummyEntityCache"),entry.getName());
                ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);
                for(var method:node.methods)for(var insn:method.instructions)
                    if(insn instanceof MethodInsnNode call)assertFalse(call.name.equals("setId"),entry.getName());
            }
        }
    }

    @Test void upstreamAndMinecraftSynchronizationContractsRemainAudited() throws Exception {
        try(var zip=new ZipFile(Path.of(System.getProperty("grabAndGoReferenceJar")).toFile())) {
            ClassNode node=read(zip,"org/chermew/grabandgo/mixin/PlayerEntityMixin.class");
            assertEquals(1,node.methods.stream().filter(m->m.name.equals("grabandgo$readAdditionalSaveData")).count());
            assertEquals(1,node.methods.stream().filter(m->m.name.equals("grabandgo$addAdditionalSaveData")).count());
        }
        try(var zip=new ZipFile(Path.of(System.getProperty("productionMinecraftJar")).toFile())) {
            ClassNode node=read(zip,"net/minecraft/server/level/ServerEntity.class");
            assertTrue(node.methods.stream().anyMatch(m->calls(m,"packDirty") && calls(m,"sendToTrackingPlayersAndSelf")),
                    "dirty carry metadata must be sent to the owning player as well as observers");
            assertTrue(node.methods.stream().anyMatch(m->calls(m,"getNonDefaultValues")),"initial tracking snapshot missing");
        }
    }
    private static boolean calls(MethodNode method,String name) {
        for(var i:method.instructions)if(i instanceof MethodInsnNode c && c.name.equals(name))return true;
        return false;
    }
    private static ClassNode read(ZipFile zip,String entry)throws Exception {
        ClassNode node=new ClassNode();new ClassReader(zip.getInputStream(zip.getEntry(entry))).accept(node,0);return node;
    }
}

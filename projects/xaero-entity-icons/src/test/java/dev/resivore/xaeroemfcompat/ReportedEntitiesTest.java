package dev.resivore.xaeroemfcompat;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.ModelPartUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.*;
/** Exact local JEM structural fixtures; these do not claim observed runtime traces. */
class ReportedEntitiesTest {
    static final String[][] MODELS = {
        {"wolf","animal.wolf.AdultWolf"}, {"bat","ambient.Bat"},
        {"axolotl","animal.axolotl.AdultAxolotl"}, {"parrot","animal.parrot.Parrot"},
        {"frog","animal.frog.Frog"}, {"allay","animal.allay.Allay"},
        {"sniffer","animal.sniffer.Sniffer"}, {"iron_golem","animal.golem.IronGolem"},
        {"vex","monster.vex.Vex"}, {"ravager","monster.ravager.Ravager"},
        {"witch","monster.witch.Witch"}};
    @Test void inspectAllReportedBaseModelsAgainstRetainedC3() throws Exception {
        for (String[] row : MODELS) {
            Class<?> type = Class.forName("net.minecraft.client.model."+row[1]+"Model");
            ModelPart vanilla;
            if (row[0].equals("wolf")) {
                MeshDefinition mesh=(MeshDefinition)type.getMethod("createBodyLayer",CubeDeformation.class).invoke(null,CubeDeformation.NONE);
                vanilla=LayerDefinition.create(mesh,64,32).bakeRoot();
            } else vanilla=((LayerDefinition)type.getMethod("createBodyLayer").invoke(null)).bakeRoot();
            List<String> headPath = path(vanilla,"head");
            assertNotNull(headPath,row[0]);
            ModelPart retainedHead=vanilla;
            for(String key:headPath)retainedHead=ModelPartUtil.getChildren(retainedHead).get(key);
            ModelPart canonical = transformedEmpty(retainedHead,Map.of());
            ModelPart customBody=customPart(topLevelPart(jem(row[0]),"body"));
            ModelPart root=replace(vanilla,headPath,0,canonical,customBody);
            ModelPart head=find(customBody,"EMF_head2");
            assertNotNull(head,row[0]);
            var trace=traced(head,0xFFFFFFFF);
            var result=runRetainedC3(root,canonical,vanilla,trace);
            var successor=EmfIconPartResolver.resolveRelocatedHead(root,canonical,vanilla,trace,true);
            assertTrue(successor.isPresent(),row[0]+":"+IconDiagnostics.lastReason());
            var resolved=successor.orElseThrow();
            assertSame(head,resolved.geometryRoot(),row[0]+" must exclude body and siblings");
            assertEquals(cubeCount(head),cubeCount(resolved.renderAdapter()),row[0]);
            var first=centeredBounds(resolved.renderAdapter(),resolved.centeringPart());
            var second=centeredBounds(resolved.renderAdapter(),resolved.centeringPart());
            assertBoundsClose(first,second,0.0001F);
            assertTrue(Float.isFinite(first.spanX()) && Float.isFinite(first.spanY()) && Float.isFinite(first.spanZ()),row[0]);
            boolean priorExpected=Set.of("bat","parrot","iron_golem","witch").contains(row[0]);
            assertEquals(priorExpected,result.isPresent(),row[0]+" C3 baseline");
            System.out.println("C4_STRUCTURAL " + row[0] + " resolved=" + successor.isPresent() + " reason=" + IconDiagnostics.lastReason());
            System.out.println("C3_STRUCTURAL "+row[0]+" vanilla="+row[1]+" canonical="+headPath+" resolved="+result.isPresent()+" direct="+ModelPartUtil.getCubes(head).size()+" total="+cubeCount(head));
        }
    }
    static Optional<?> runRetainedC3(ModelPart root,ModelPart canonical,ModelPart vanilla,
            xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace trace) throws Exception {
        var jar=java.nio.file.Path.of(System.getProperty("projectRoot"),"artifacts",
                "xaero-emf-entity-icon-compat-0.1.0-canary3.jar");
        assertEquals("4f34d743f5fffd8e938c8f5157c630fd85f3b263ac1ae9f96c432cfe51668df2",
                java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                        .digest(java.nio.file.Files.readAllBytes(jar))));
        String name="dev.resivore.xaeroemfcompat.EmfIconPartResolver";
        // Load only the retained resolver and its nested types child-first; share exact MC/Xaero types.
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},ReportedEntitiesTest.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String requested,boolean resolve) throws ClassNotFoundException {
                if(!requested.startsWith(name))return super.loadClass(requested,resolve);
                Class<?> c=findLoadedClass(requested);if(c==null)c=findClass(requested);
                if(resolve)resolveClass(c);return c;
            }
        }) {
            var method=loader.loadClass(name).getDeclaredMethod("resolveRelocatedHead",ModelPart.class,
                    ModelPart.class,ModelPart.class,trace.getClass(),boolean.class);
            method.setAccessible(true);
            return (Optional<?>)method.invoke(null,root,canonical,vanilla,trace,true);
        }
    }
    static List<String> path(ModelPart part,String target) {
        var children=ModelPartUtil.getChildren(part);
        if(children==null)return null;
        if(children.containsKey(target))return List.of(target);
        for(var e:children.entrySet()) {var sub=path(e.getValue(),target);if(sub!=null){var out=new ArrayList<String>();out.add(e.getKey());out.addAll(sub);return out;}}
        return null;
    }
    static ModelPart find(ModelPart part,String name) {var path=path(part,name);if(path==null)return null;for(String key:path)part=ModelPartUtil.getChildren(part).get(key);return part;}
    static ModelPart replace(ModelPart vanilla,List<String> headPath,int depth,ModelPart canonical,ModelPart customBody) {
        Map<String,ModelPart> children=new LinkedHashMap<>();
        for(var e:ModelPartUtil.getChildren(vanilla).entrySet()) {
            ModelPart child=e.getValue();
            if(e.getKey().equals(headPath.get(depth))) {
                child=depth==headPath.size()-1?canonical:replace(child,headPath,depth+1,canonical,customBody);
            } else if(e.getKey().equals("body")) child=transformedEmpty(child,Map.of("EMF_body",customBody));
            children.put(e.getKey(),child);
        }
        // If the canonical head is nested below body, retain its identity path beside the custom subtree.
        if(depth>0 && headPath.get(depth-1).equals("body"))children.put("EMF_body",customBody);
        return transformedEmpty(vanilla,children);
    }
}

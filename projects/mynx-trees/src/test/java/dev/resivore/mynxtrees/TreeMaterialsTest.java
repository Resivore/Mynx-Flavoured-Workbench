package dev.resivore.mynxtrees;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.zip.ZipFile;

class TreeMaterialsTest {
    private JsonObject vanilla(String name) throws Exception {
        try(var zip = new ZipFile(Path.of(System.getProperty("user.home"), ".gradle/caches/fabric-loom/26.2/minecraft-merged.jar").toFile())) {
            return JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("data/minecraft/worldgen/configured_feature/"+name+".json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private void assertOnlyMaterialsChanged(String name, String species, String old) throws Exception {
        var source=vanilla(name); var changed=source.deepCopy(); TreeMaterials.transform(changed,species);
        var expected=JsonParser.parseString(source.toString().replace("minecraft:"+old+"_log", "mynx_trees:"+species+"_log").replace("minecraft:"+old+"_leaves", "mynx_trees:"+species+"_leaves"));
        assertEquals(expected,changed,"Tree parameters and all decorations must survive: "+name);
        assertEquals(vanilla(name),source,"Do not mutate source configuration");
    }
    @Test void allNaturalBirchBranchesPreserveShapeAndDecorators() throws Exception {
        for(String name:new String[]{"birch_bees_0002","super_birch_bees_0002","fallen_birch_tree","fallen_super_birch_tree"}) assertOnlyMaterialsChanged(name,"silver_birch","birch");
    }
    @Test void plantedSilverIsAlwaysSuperBirchAndKeepsBeeVariant() throws Exception {
        assertOnlyMaterialsChanged("super_birch_bees","silver_birch","birch");
        assertEquals(6,vanilla("super_birch_bees_0002").getAsJsonObject("config").getAsJsonObject("trunk_placer").get("height_rand_b").getAsInt());
    }
    @Test void wisteriaPreservesCherryBranchesCanopyAndBees() throws Exception {
        for(String name:new String[]{"cherry","cherry_bees_005"}) {
            var source=vanilla(name);var actual=source.deepCopy();TreeMaterials.transform(actual,"wisteria");
            var decorations=actual.getAsJsonObject("config").getAsJsonArray("decorators");
            var added=decorations.remove(decorations.size()-1).getAsJsonObject();
            assertEquals("mynx_trees:grove_flowers",added.get("type").getAsString());assertTrue(added.get("wisteria").getAsBoolean());
            assertEquals(JsonParser.parseString(source.toString().replace("minecraft:cherry_log","mynx_trees:wisteria_log").replace("minecraft:cherry_leaves","mynx_trees:wisteria_leaves")),actual);
        }
    }
    private JsonObject nemoCherry() throws Exception {
        var f=vanilla("cherry_bees_005");f.getAsJsonObject("config").getAsJsonArray("decorators").add(JsonParser.parseString("{\"type\":\"nemos-blooming-blossom:cherry_tree_decorator\"}"));return f;
    }
    @Test void nemoIsRemovedOnlyFromWisteriaAndOwnFlowersAddedOnce() throws Exception {
        var actual=nemoCherry();TreeMaterials.transform(actual,"wisteria");TreeMaterials.transform(actual,"wisteria");
        String text=actual.toString();assertFalse(text.contains("nemos-blooming-blossom:cherry_tree_decorator"));assertFalse(text.contains("minecraft:pink_petals"));
        assertEquals(2,actual.getAsJsonObject("config").getAsJsonArray("decorators").size());
        assertTrue(text.contains("minecraft:beehive"));
    }
    @Test void nemoCherryIsExactlyPreserved() throws Exception { var actual=nemoCherry();var expected=actual.deepCopy();TreeMaterials.transform(actual,"cherry");assertEquals(expected,actual); }
    @Test void absentNemoCherryHasOnePinkFlowerDecorator() throws Exception {
        var actual=vanilla("cherry");TreeMaterials.transform(actual,"cherry");TreeMaterials.transform(actual,"cherry");
        var d=actual.getAsJsonObject("config").getAsJsonArray("decorators");assertEquals(1,d.size());assertFalse(d.get(0).getAsJsonObject().get("wisteria").getAsBoolean());
    }
    @Test void externalParametersAndUnrelatedDecoratorsAreRetained() throws Exception {
        var original=nemoCherry();original.getAsJsonObject("config").getAsJsonObject("trunk_placer").addProperty("base_height",19);
        original.getAsJsonObject("config").getAsJsonArray("decorators").add(JsonParser.parseString("{\"type\":\"example:custom_decorator\",\"arbitrary_parameter\":42}"));
        TreeMaterials.transform(original,"wisteria");assertEquals(19,original.getAsJsonObject("config").getAsJsonObject("trunk_placer").get("base_height").getAsInt());assertTrue(original.toString().contains("example:custom_decorator"));
    }
    @Test void unsupportedFeatureFailsClosedInsteadOfGrowingAnOrdinaryTree() {
        var source=JsonParser.parseString("{\"type\":\"example:unknown_tree\",\"config\":{}}").getAsJsonObject();
        assertThrows(IllegalStateException.class,()->TreeMaterials.transform(source,"wisteria"));
    }
}

package dev.resivore.mynxtrees;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.IntStream;

class LeafShaderAliasesTest {
    private Object2IntLinkedOpenHashMap<String> ids() {
        var map = new Object2IntLinkedOpenHashMap<String>(); map.defaultReturnValue(-1); return map;
    }
    @Test void silverBirchInheritsCorrespondingBirchLeavesStateForState() {
        var map=ids();var states=IntStream.range(0,28).mapToObj(i->"custom:"+i).toList();
        for(int i=0;i<28;i++) map.put("birch:"+i,40+i);
        LeafShaderAliases.inheritUnmapped(map,states,s->s.replace("custom:","birch:"));
        for(int i=0;i<28;i++) assertEquals(40+i,map.getInt("custom:"+i));
    }
    @Test void preservesExplicitSilverBirchMappingsIncludingDisabledOrZero() {
        var map=ids();map.put("birch",47);map.put("custom1",42);map.put("custom2",-1);map.put("custom3",0);
        var before=new Object2IntLinkedOpenHashMap<>(map);
        LeafShaderAliases.inheritUnmapped(map,List.of("custom1","custom2","custom3"),s->"birch");assertEquals(before,map);
    }
    @Test void missingBirchLeavesAssignmentDoesNotInventAnId() {
        var map=ids();map.put("sunflower-upper",31);
        LeafShaderAliases.inheritUnmapped(map,List.of("silver"),s->"birch");
        assertFalse(map.containsKey("silver"));assertEquals(31,map.getInt("sunflower-upper"));
    }
    @Test void wisteriaStillInheritsCherryLeavesStateForState() {
        var map=ids();var states=IntStream.range(0,28).mapToObj(i->"wisteria:"+i).toList();
        for(int i=0;i<28;i++) map.put("cherry:"+i,40+i);
        LeafShaderAliases.inheritUnmapped(map,states,s->s.replace("wisteria:","cherry:"));
        for(int i=0;i<28;i++) assertEquals(40+i,map.getInt("wisteria:"+i));
    }
    @Test void shaderReloadUsesOnlyTheNewlyLoadedBirchLeavesMap() {
        for(int chosen:List.of(47,17,0)) {
            var map=ids();map.put("birch",chosen);LeafShaderAliases.inheritUnmapped(map,List.of("silver"),s->"birch");assertEquals(chosen,map.getInt("silver"));
        }
    }
    @Test void noUnrelatedOrVanillaAssignmentChangesAndReplaysAreIdempotent() {
        var map=ids();map.put("birch",47);map.put("cherry",123);map.put("stone",7);
        LeafShaderAliases.inheritUnmapped(map,List.of("silver"),s->"birch");LeafShaderAliases.inheritUnmapped(map,List.of("wisteria"),s->"cherry");
        var expected=new Object2IntLinkedOpenHashMap<>(map);
        LeafShaderAliases.inheritUnmapped(map,List.of("silver"),s->"cherry");assertEquals(expected,map);assertEquals(7,map.getInt("stone"));assertEquals(47,map.getInt("birch"));assertEquals(123,map.getInt("cherry"));assertEquals(5,map.size());
    }
    @Test void inventoryGoldenTintMatchesWorldDefaultOnce() {
        assertEquals(0xFF857A1B,MynxTreesClient.golden(0,0));
    }
}

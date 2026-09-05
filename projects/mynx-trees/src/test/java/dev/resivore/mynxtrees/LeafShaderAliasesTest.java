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
    @Test void inheritsPackSelectedIdsForEveryLeafState() {
        var map=ids();var states=IntStream.range(0,28).mapToObj(i->"custom:"+i).toList();
        for(int i=0;i<28;i++) map.put("vanilla:"+i,10009+i);
        LeafShaderAliases.inheritUnmapped(map,states,s->s.replace("custom:","vanilla:"));
        for(int i=0;i<28;i++) assertEquals(10009+i,map.getInt("custom:"+i));
    }
    @Test void preservesExplicitCustomMappingsIncludingDisabledOrZero() {
        var map=ids();map.put("birch",10009);map.put("custom1",42);map.put("custom2",-1);map.put("custom3",0);
        var before=new Object2IntLinkedOpenHashMap<>(map);
        LeafShaderAliases.inheritUnmapped(map,List.of("custom1","custom2","custom3"),s->"birch");assertEquals(before,map);
    }
    @Test void unmappedVanillaDoesNotInventShaderId() {
        var map=ids();LeafShaderAliases.inheritUnmapped(map,List.of("custom"),s->"missing");assertTrue(map.isEmpty());
    }
    @Test void shaderReloadUsesNewMapWithoutStaleIds() {
        for(int chosen:List.of(10009,17,0)) {
            var map=ids();map.put("birch",chosen);LeafShaderAliases.inheritUnmapped(map,List.of("silver"),s->"birch");assertEquals(chosen,map.getInt("silver"));
        }
    }
    @Test void noUnrelatedOrVanillaAssignmentChangesAndReplaysAreIdempotent() {
        var map=ids();map.put("birch",10009);map.put("cherry",123);map.put("stone",7);
        LeafShaderAliases.inheritUnmapped(map,List.of("silver"),s->"birch");LeafShaderAliases.inheritUnmapped(map,List.of("wisteria"),s->"cherry");
        var expected=new Object2IntLinkedOpenHashMap<>(map);
        LeafShaderAliases.inheritUnmapped(map,List.of("silver"),s->"cherry");assertEquals(expected,map);assertEquals(7,map.getInt("stone"));assertEquals(10009,map.getInt("birch"));assertEquals(123,map.getInt("cherry"));assertEquals(5,map.size());
    }
    @Test void inventoryGoldenTintMatchesWorldDefaultOnce() {
        assertEquals(0xFFFFEB33,MynxTreesClient.golden(0,0));
    }
}

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
    @Test void silverBirchUsesTheFirstMappedUpperFoliageRepresentativeForEveryLeafState() {
        var map=ids();var states=IntStream.range(0,28).mapToObj(i->"custom:"+i).toList();
        map.put("sunflower-upper",47);map.put("lilac-upper",12);
        LeafShaderAliases.inheritUnmappedFromFirstPresent(map,states,List.of("sunflower-upper","lilac-upper","rose-bush-upper","peony-upper"));
        for(int i=0;i<28;i++) assertEquals(47,map.getInt("custom:"+i));
    }
    @Test void silverBirchSkipsMissingRepresentativesInOrder() {
        var map=ids();map.put("rose-bush-upper",19);map.put("peony-upper",23);
        LeafShaderAliases.inheritUnmappedFromFirstPresent(map,List.of("silver"),List.of("sunflower-upper","lilac-upper","rose-bush-upper","peony-upper"));
        assertEquals(19,map.getInt("silver"));
    }
    @Test void preservesExplicitSilverBirchMappingsIncludingDisabledOrZero() {
        var map=ids();map.put("sunflower-upper",47);map.put("custom1",42);map.put("custom2",-1);map.put("custom3",0);
        var before=new Object2IntLinkedOpenHashMap<>(map);
        LeafShaderAliases.inheritUnmappedFromFirstPresent(map,List.of("custom1","custom2","custom3"),List.of("sunflower-upper"));assertEquals(before,map);
    }
    @Test void missingUpperFoliageDoesNotFallBackToBirchOrInventAnId() {
        var map=ids();map.put("birch",31);
        LeafShaderAliases.inheritUnmappedFromFirstPresent(map,List.of("silver"),List.of("sunflower-upper","lilac-upper","rose-bush-upper","peony-upper"));
        assertFalse(map.containsKey("silver"));assertEquals(31,map.getInt("birch"));
    }
    @Test void wisteriaStillInheritsCherryLeavesStateForState() {
        var map=ids();var states=IntStream.range(0,28).mapToObj(i->"wisteria:"+i).toList();
        for(int i=0;i<28;i++) map.put("cherry:"+i,40+i);
        LeafShaderAliases.inheritUnmapped(map,states,s->s.replace("wisteria:","cherry:"));
        for(int i=0;i<28;i++) assertEquals(40+i,map.getInt("wisteria:"+i));
    }
    @Test void shaderReloadUsesOnlyTheNewlyLoadedUpperFoliageMap() {
        for(int chosen:List.of(47,17,0)) {
            var map=ids();map.put("sunflower-upper",chosen);LeafShaderAliases.inheritUnmappedFromFirstPresent(map,List.of("silver"),List.of("sunflower-upper"));assertEquals(chosen,map.getInt("silver"));
        }
    }
    @Test void noUnrelatedOrVanillaAssignmentChangesAndReplaysAreIdempotent() {
        var map=ids();map.put("sunflower-upper",47);map.put("cherry",123);map.put("stone",7);
        LeafShaderAliases.inheritUnmappedFromFirstPresent(map,List.of("silver"),List.of("sunflower-upper"));LeafShaderAliases.inheritUnmapped(map,List.of("wisteria"),s->"cherry");
        var expected=new Object2IntLinkedOpenHashMap<>(map);
        LeafShaderAliases.inheritUnmappedFromFirstPresent(map,List.of("silver"),List.of("cherry"));assertEquals(expected,map);assertEquals(7,map.getInt("stone"));assertEquals(47,map.getInt("sunflower-upper"));assertEquals(123,map.getInt("cherry"));assertEquals(5,map.size());
    }
    @Test void inventoryGoldenTintMatchesWorldDefaultOnce() {
        assertEquals(0xFFFFEB33,MynxTreesClient.golden(0,0));
    }
}

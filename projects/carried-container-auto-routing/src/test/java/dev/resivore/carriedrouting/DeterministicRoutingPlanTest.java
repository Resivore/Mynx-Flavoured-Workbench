package dev.resivore.carriedrouting;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DeterministicRoutingPlanTest {
    private static DeterministicRoutingPlan.Candidate c(String kind, String id, boolean match, boolean locked, int merge, int free, boolean top) {
        return new DeterministicRoutingPlan.Candidate(kind,id,match,locked,merge,free,top);
    }
    private static DeterministicRoutingPlan.Result plan(int count, DeterministicRoutingPlan.Candidate... c) {
        return DeterministicRoutingPlan.plan("stone{}", count, List.of(c));
    }
    @Test void matchingUnlockedShulker() { assertEquals(0, plan(12,c("shulker","stone{}",true,false,12,0,true)).remainder()); }
    @Test void emptyShulkerDoesNotQualify() { assertEquals(12, plan(12,c("shulker","stone{}",false,false,0,1728,true)).remainder()); }
    @Test void unrelatedShulkerDoesNotQualify() { assertEquals(12, plan(12,c("shulker","dirt{}",true,false,64,0,true)).remainder()); }
    @Test void lockedShulkerIgnored() { assertEquals(12, plan(12,c("shulker","stone{}",true,true,64,0,true)).remainder()); }
    @Test void partialStack() { assertEquals(7, plan(12,c("shulker","stone{}",true,false,5,0,true)).remainder()); }
    @Test void matchThenFreeSlots() { assertEquals(0, plan(70,c("shulker","stone{}",true,false,4,128,true)).remainder()); }
    @Test void fullShulker() { assertEquals(12, plan(12,c("shulker","stone{}",true,false,0,0,true)).remainder()); }
    @Test void twoMatchingShulkers() { assertEquals(2, plan(12,c("shulker","stone{}",true,false,5,0,true),c("shulker","stone{}",true,false,5,0,true)).remainder()); }
    @Test void secondAcceptsRemainder() { assertEquals(List.of(3,9), plan(12,c("shulker","stone{}",true,false,3,0,true),c("shulker","stone{}",true,false,9,0,true)).allocations().stream().map(DeterministicRoutingPlan.Allocation::count).toList()); }
    @Test void playerFallbackIsExactRemainder() { assertEquals(7, plan(12,c("shulker","stone{}",true,false,5,0,true)).remainder()); }
    @Test void totalCapacityInsufficient() { assertEquals(90, plan(100,c("shulker","stone{}",true,false,10,0,true)).remainder()); }
    @Test void componentDistinctRejected() { assertEquals(12, plan(12,c("shulker","stone{custom_name:x}",true,false,64,0,true)).remainder()); }
    @Test void componentHeavyExactAccepted() { assertEquals(0, DeterministicRoutingPlan.plan("potion{name:x,potion:y}",1,List.of(c("bundle","potion{name:x,potion:y}",true,false,1,0,true))).remainder()); }
    @Test void matchingBundle() { assertEquals(0, plan(12,c("bundle","stone{}",true,false,12,0,true)).remainder()); }
    @Test void lockedBundle() { assertEquals(12, plan(12,c("bundle","stone{}",true,true,12,0,true)).remainder()); }
    @Test void partialBundle() { assertEquals(7, plan(12,c("bundle","stone{}",true,false,5,0,true)).remainder()); }
    @Test void noSupportedContainer() { assertEquals(12, plan(12,c("backpack","stone{}",true,false,64,0,true)).remainder()); }
    @Test void deterministicOrdering() { assertEquals(List.of(0,1), plan(10,c("bundle","stone{}",true,false,5,0,true),c("shulker","stone{}",true,false,5,0,true)).allocations().stream().map(DeterministicRoutingPlan.Allocation::candidateIndex).toList()); }
    @Test void lockStateDoesNotChangeCapacityModel() { var locked=c("shulker","stone{}",true,true,4,64,true); assertEquals(68, locked.existingMergeCapacity()+locked.qualifiedFreeCapacity()); assertEquals(12,plan(12,locked).remainder()); }
    @Test void nestedContainerIgnored() { assertEquals(12, plan(12,c("shulker","stone{}",true,false,64,0,false)).remainder()); }
}

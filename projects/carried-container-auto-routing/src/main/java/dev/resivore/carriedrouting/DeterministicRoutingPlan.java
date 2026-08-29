package dev.resivore.carriedrouting;

import java.util.ArrayList;
import java.util.List;

/** Pure fixture-facing model for ordering, qualification, and exact remainder arithmetic. */
public final class DeterministicRoutingPlan {
    public record Candidate(String kind, String componentIdentity, boolean hasMatch, boolean locked,
                            int existingMergeCapacity, int qualifiedFreeCapacity, boolean topLevel) {}
    public record Allocation(int candidateIndex, int count) {}
    public record Result(List<Allocation> allocations, int remainder) {}
    private DeterministicRoutingPlan() {}
    public static Result plan(String incomingIdentity, int count, List<Candidate> candidates) {
        int remainder = count;
        List<Allocation> allocations = new ArrayList<>();
        for (int i = 0; i < candidates.size() && remainder > 0; i++) {
            Candidate c = candidates.get(i);
            if (!c.topLevel || c.locked || !c.hasMatch || !c.componentIdentity.equals(incomingIdentity)
                    || !(c.kind.equals("shulker") || c.kind.equals("bundle"))) continue;
            int capacity = Math.max(0, c.existingMergeCapacity) + Math.max(0, c.qualifiedFreeCapacity);
            int moved = Math.min(remainder, capacity);
            if (moved > 0) { allocations.add(new Allocation(i, moved)); remainder -= moved; }
        }
        return new Result(List.copyOf(allocations), remainder);
    }
}

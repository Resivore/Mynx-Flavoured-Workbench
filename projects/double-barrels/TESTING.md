# Testing

## Retained runtime evidence

Exact Canary 2, `doublebarrels-fabric-26.2-1.0.1+26.2-canary2.jar`
(SHA-256
`b2119e05c508880c7977551091515f0076dd3cdc2733e51340ac813925b553c0`),
has the frozen classification **PARTIAL RUNTIME PASS / ACCEPTED**. The user
reported that it appeared to function properly during normal gameplay, but no
individual matrix row was reported and none is inferred. A later normal-quit
watchdog was traced to a pre-existing Stack to Nearby Chests executor leak and
is not a Double Barrels blocker.

This migration performed no deployment or Minecraft runtime test. Run the
procedure below only under separate runtime ownership.

## Current regression procedure

1. Record the exact Double Barrels JAR filename and SHA-256, Minecraft, Fabric
   Loader and Fabric API versions, complete test stack, world type, and matching
   log identity.
2. Launch Minecraft, enter a world, and confirm there are no Double Barrels
   mixin, registry, model, blockstate, or resource errors.
3. Test an ordinary unpaired barrel for facing, lid behavior, 27 slots, drops,
   hopper transfer, and comparator output.
4. For horizontal-facing barrels, exercise left, right, above, below, front, and
   back partners. For upward-facing barrels, exercise both horizontal axes and
   vertical end-to-end pairing. For downward-facing barrels, confirm vertical
   pairing works and horizontal pairing does not.
5. Confirm barrels with different facings, already-paired neighbors, and
   secondary-use placement against a non-barrel do not merge.
6. Open both halves of a pair and confirm both expose the same six-row,
   54-slot inventory. Exercise boundary slots 26, 27, and 53 through both
   halves.
7. Save and reload, return to the title screen and reopen, then restart the
   client. Confirm orientation, partner/main relationship, contents, and lid
   visuals persist.
8. With items in both halves, break the non-main half and then repeat with the
   main half. Only the removed half's contents should drop; the survivor should
   return to a single barrel with its own contents intact. Also exercise
   explosion removal and, where possible, piston behavior.
9. Insert and extract through hoppers on representative faces of both halves,
   covering both 27-slot ranges. Measure comparator output from each half when
   empty, partially filled, and full; both should reflect the combined
   inventory.
10. Test a pair across a chunk border through save/reload and one-side
    unload/reload. With both chunks loaded, the pair must remain consistent. If
    one half is queried while its partner is unavailable, preserve the upstream
    disconnect-to-single behavior without item loss, stale linkage, or later
    resynchronization errors.
11. Throughout, check for ghost items, duplication or deletion, stale
    `double_barrel` state, client/server disagreement, incorrect opening or
    rendering, and relevant errors in the matching log. Record each passed or
    failed step explicitly rather than upgrading unchecked rows.

## Stop conditions

Stop and preserve the exact world/log evidence on any startup mixin failure,
crash, item duplication or loss, stale pairing, persistence failure, or
client/server disagreement. Do not promote or replace the accepted C2 artifact
as part of diagnosis.

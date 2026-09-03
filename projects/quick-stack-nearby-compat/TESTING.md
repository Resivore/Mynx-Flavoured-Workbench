# Testing

C7 (`0.1.0-canary7`) is the current deployed candidate. Its exact artifact is
`quick-stack-nearby-compat-0.1.0-canary7.jar`, 31,568 bytes, SHA-256
`9e2ef78a5f40f5bcdeed0e8c6af8cd7cfb15a35d541360017c4e46e94521995e`,
from implementation checkpoint `3f124a44086e52845e35805d5abb3049379150b8`.
It has `CONTROLLED_VALIDATION_PASS`, `READY_TO_TEST_VERIFIED`, and
`RUNTIME_UNTESTED`.

Manager revision 71 physically verified Stack v15 with C7 in Test Slot B.
Deployment `2c2ca107-d652-45bc-a7d6-23c0238d00f7` binds slot artifact
`918d23dd-6268-47c9-baa0-c49daf85e05d` to those exact file, hash, and source
identities. The verified state digest is
`bf3b0cafa400620a9755f9d2cd3a3080eb726691922553da3c945f76a25e86f9`.
C7's Slot B result is independently `UNTESTED`. C6's earlier accepted aggregate
`RUNTIME_PASS` is separate historical evidence and does not apply to C7. Exact
C6 remains accepted and exact C4 remains the passing rollback.

## Preconditions

- Require manager revision 71, or a later canonical revision that preserves the
  exact C7 Slot B deployment and artifact identities above, reports
  `READY_TO_TEST_VERIFIED`, and still shows C7 as `UNTESTED`. Revision 71 must
  match the recorded Stack v15 digest. Stop rather than redeploying if it does
  not.
- Require official `quick-stack-nearby-0.4.0.jar`, 159,918 bytes, SHA-256
  `43f1130527f782a291231c682791b4fd3766a20916c691cbdb98f91fdcc47e53`.
  Its accepted deployment `66b8b293-d9ae-41c4-a969-d43baf79c2ff` and artifact
  `cb31d144-b6c4-41fb-ba44-35d896b228f6` must remain active through the verified
  accepted-companion passthrough, not as a second slot member.
- For the CSR-enabled pass, require exact C2 in Slot A: deployment
  `bee6248a-0cb8-4953-bd55-33ac3f6e5111`, artifact
  `b92b3cd6-a3c2-4882-b279-4a39194fbdc9`, file
  `container-slot-reservations-0.1.0-canary2.jar`, 81,962 bytes, SHA-256
  `bb4f269758d43469f2a9c0a36d01dcecb8704054ee8bc01327c46ce614d4a02f`,
  source `48468ad99ba1353ac5455be39dedef6ca143f7ae`, and result `UNTESTED`.
- Before testing, record the manager revision, complete enabled stack, world,
  and log. Confirm exactly one enabled owner for `quick_stack_nearby_compat`,
  `quick-stack-nearby`, and `container_slot_reservations`; stop on any drift.

## C7 runtime procedure

1. Establish a nearby supported container with a physically empty slot reserved
   through CSR for an exact item-and-component stack. Keep that item absent from
   the target's physical stacks. Quick-stack the matching source and confirm the
   reservation alone gives the container target affinity and receives the item.
2. Repeat with the same item ID but different components. Confirm the mismatched
   source gains no reservation affinity and does not enter the reserved slot.
3. Give one target a partial matching physical stack, then a matching reserved
   empty, an ordinary empty, and a mismatched reserved empty. Confirm QSN merges
   the physical stack first, visits matching reserved empties in physical order,
   then ordinary empties in physical order, and leaves the mismatch untouched.
4. Admit a target through an existing physical match while its only remaining
   empty slot is reserved for another stack. Confirm the physical merge remains
   native and the mismatched reserved empty cannot be filled by QSN's fallback.
5. Exercise an otherwise identical wholly unreserved supported container and a
   CSR-unsupported container. Confirm C7 delegates their empty-slot work and
   ordinary QSN behavior remains unchanged.
6. Exercise a low-capacity target and a source larger than the available legal
   capacity. Reconcile source, target, moved count, remainder, container change,
   rule ceiling, packet/result, and user feedback with native QSN behavior.
7. Restart the world after creating reservations and repeat a matching and a
   mismatching case. Confirm CSR still owns persistence and C7 only consumes the
   public classification result.
8. Put a matching source in Inventory Extended row 4, 5, or 6. Confirm QSN uses
   it while hotbar, equipment, offhand, crafting/result, Trinkets, trash, and
   foreign menu slots remain outside C7's source-window expansion. Exercise one
   lock/keep-count rule and confirm its ceiling remains QSN-owned.
9. Merge one Clutter No More ShapeMap-equivalent geometry into a partial nearby
   destination and repeat in reverse. Confirm the existing destination geometry
   wins, exact source variants remain distinct, and QSN retains target order,
   capacity, and remainder ownership.
10. Put a matching destination in a vanilla shelf and another in an ordinary
    chest or barrel. Confirm the shelf remains excluded and the ordinary target
    remains eligible without changing access or validity behavior.
11. Open the normal Survival inventory with Inventory Search and Inventory
    Extended enabled. Confirm Inventory Search owns the base right-edge button,
    QSN appears immediately below without overlap, both actions work, and the
    layout remains correct after reopening and toggling the recipe book.
12. Repeat representative native cases without each optional C6 provider where
    an authorized runtime configuration already exists. A gameplay no-CSR pass
    requires its own explicit manager transition; do not treat C7's successful
    CSR-absent unit launch as runtime evidence.

Throughout, reconcile exact item and component totals. Stop and preserve the
world and log if a reservation alone fails to create affinity, a component
mismatch is admitted, a mismatched reserved slot receives an item, slot order or
capacity diverges, an unsupported target loses native behavior, a C6 regression
appears, counts diverge, a ghost stack or conversion occurs, or a relevant
Mixin/client/server error appears. Record only the rows actually observed and
never infer C7 runtime evidence from C6 or from controlled GameTests.

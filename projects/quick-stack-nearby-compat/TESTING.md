# Testing

C7 (`0.1.0-canary7`) is the current predeployment candidate. Its exact artifact
is `quick-stack-nearby-compat-0.1.0-canary7.jar`, 31,568 bytes, SHA-256
`9e2ef78a5f40f5bcdeed0e8c6af8cd7cfb15a35d541360017c4e46e94521995e`,
from implementation checkpoint `3f124a44086e52845e35805d5abb3049379150b8`.
It has `CONTROLLED_VALIDATION_PASS`, is `NOT_DEPLOYED`, and is
`RUNTIME_UNTESTED`.

The project lifecycle remains `TESTING` only because exact accepted C6 still
occupies Test Slot B at manager revision 70. Slot B deployment
`34dcf31b-99c9-4c64-9978-f1f94782ed9a` uses slot artifact
`7293a2b5-ea16-4929-af1b-a6c15f0aef32`, exact C6 source
`3c8cc5917da9fd016e57a969955fa7c6e3b08661`, and reports `UNTESTED`.
C6's earlier accepted aggregate `RUNTIME_PASS` is separate historical evidence.
Neither result applies to C7. Exact C4 remains the passing rollback.

## Preconditions

- Use a serialized Test Instance Manager transition before gameplay testing.
  Replace the same-project C6 member in Slot B with exact C7; preserve Slot A,
  every unrelated accepted artifact, and the accepted upstream QSN companion.
  Do not launch until the manager returns a physically verified C7 deployment.
- Require official `quick-stack-nearby-0.4.0.jar`, 159,918 bytes, SHA-256
  `43f1130527f782a291231c682791b4fd3766a20916c691cbdb98f91fdcc47e53`.
  Its accepted deployment `66b8b293-d9ae-41c4-a969-d43baf79c2ff` and artifact
  `cb31d144-b6c4-41fb-ba44-35d896b228f6` remain baseline infrastructure, not a
  second slot member.
- For the CSR-enabled pass, require the accepted C1 provider
  `container-slot-reservations-0.1.0-canary1.jar`, 63,388 bytes, SHA-256
  `4e7f0a470a387be76189d0a5e1ae8c614d17ec8b24a6774b400838cc234c4532`,
  or record the exact later compatible provider deliberately substituted by an
  authorized manager transition. C2 pairing has controlled GameTest evidence,
  not gameplay evidence.
- Before testing, record the manager revision, C7 deployment and artifact IDs,
  complete enabled stack, world, log, filenames, versions, and SHA-256 values.
  Stop if either QSN or C7 has zero or multiple enabled Fabric-ID owners.

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

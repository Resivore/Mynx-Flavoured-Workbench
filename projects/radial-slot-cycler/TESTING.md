# Testing

Exact Private Canary 2 is the current and accepted Workbench release. It is a
private, non-redistributable artifact and is not tracked in this repository.
The user supplied one aggregate focused runtime **PASS** for that exact binary;
the numbered checks below were not reported separately and must not be
retroactively classified one by one.

This migration did not deploy an artifact, launch Minecraft, change a Test
Slot or the accepted baseline, touch either gameplay profile, or access the
protected gameplay instance. The preserved deployment and runtime
classifications are prior evidence for the exact accepted binary.

## Preconditions

1. Use only `radial-slot-cycler-0.1.0-canary2-private.jar`, 36,436 bytes,
   SHA-256
   `3FE7E539EAAFDA97AB5A5A301544B25563F896FEE354B729607074F404BDBC21`.
2. Keep the artifact's dedicated key unbound by default and assign `M` only in
   the Workbench controls. Keep `interactionMode` at the accepted `hold`
   value.
3. Keep upstream Slot Cycler disabled. Keep Inventory Extended and exact
   Traveler Tool Belt `1.0.2+26.2-trinkets-canary1` installed as separate,
   unchanged mods; Radial must not use Traveler storage or behavior.
4. Use the dedicated Minecraft 26.2 Workbench only in a separately authorized
   runtime task. Do not substitute clean Canary 1 for current acceptance; it
   remains the distributable focused-runtime-pass rollback.

## Current focused procedure

1. Launch successfully and confirm Radial Slot Cycler and Traveler Tool Belt
   remain independently functional on their separate keys and storage domains.
2. Hold `M` and confirm the Radial wheel uses the private black annulus and
   dotted 42-pixel ring, starts at twelve o'clock, shows seven evenly spaced
   entries with Inventory Extended, and opens without dimming or blurring the
   world.
3. Check physical hotbar columns 1, 5, and 9. Each must show the current
   hotbar anchor plus the six same-column ordinary storage cells, including
   selectable empty cells.
4. Release on occupied and empty storage entries and confirm only the complete
   selected hotbar and target storage stacks exchange. Confirm identical
   partial, damaged, renamed, enchanted, and component-bearing stacks never
   merge, consolidate, quick-move, or route elsewhere.
5. Release in the 20-pixel center dead zone and on the hotbar anchor; neither
   action may mutate inventory. Rapid gestures must produce at most one swap.
6. Confirm the highlighted item treatment, count and durability decorations,
   centered action text, tooltip text, and empty-entry treatment remain
   readable while Traveler's own radial appearance and behavior remain
   unchanged.
7. Confirm Radial cannot open in inventory, chest, Creative, or other GUI
   screens, cancels on a stale hotbar selection or inventory snapshot, and
   remains disabled for spectators.
8. Save and reload, disconnect and rejoin, and inspect the log. Confirm no
   loss, duplication, ghost stack, desynchronization, payload rejection,
   mixin, resource, or inventory-routing error.

Stop and record **FAIL** or **INCONCLUSIVE** for a crash, wrong-column entry,
missing empty entry, merge or routing behavior, multiple swaps from one
gesture, screen-suppression failure, stale-selection mutation, Traveler
regression, missing private visual, loss, duplication, or relevant log error.
No dedicated-server result is currently established; record any future server
coverage independently rather than extending the preserved aggregate pass.

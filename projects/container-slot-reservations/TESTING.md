# Container Slot Reservations Canary 10 runtime procedure

**Current Slot A: READY_TO_TEST_VERIFIED / RUNTIME_UNTESTED. Do not promote from controlled evidence.**

Use only the dedicated **Matcha Flavoured 26.2 Workbench**. Do not use the protected 26.1.2 gameplay profile. Minecraft was not launched during deployment, so every row below still needs real gameplay observation.

Exact candidate:

- Version: `0.1.0-canary10`
- File: `container-slot-reservations-0.1.0-canary10.jar`
- Size: 167546 bytes
- SHA-256: `04334c1cd2316d97ec2930f43fa6945be1898607f1209d19bf92c562cd441b75`
- Source: `12eec63fcb08a3f47c6041d1b4961ca231879fe4`
- Deployment: `0c8732a8-34a8-423e-a3b6-be7abb8b310c`
- Artifact UUID: `6fda1dd5-bcb7-4902-a098-20329934d937`
- Manager revision/state/physical digest: `98` / `0547fd5c82c3e1a03707eecdf1f26cef9630faa848509b778cfd1cdc028aebb7` / `ced07e98678166d812c735c5c83fcb772473a22fa20353c7ee8367e67133bbe7`

Slot B remains exact QSN C9 and independently `INCONCLUSIVE`. Its sole existing positive observation is: “Nested shulker routing using an internal CSR reservation worked.” Do not copy CSR results to QSN or infer unobserved QSN rows.

## Stop immediately if

- the title does not show `Baseline: Stack v19`, `Slot A: Container Slot Reservations - Canary 10`, and `Slot B: Quick Stack Nearby Compatibility - Canary 9`;
- the candidate identity differs, a mixin fails, the client crashes, or the complete log reports a CSR error;
- any action loses, duplicates, changes, or moves more items than described;
- a stale, moved, replaced, multi-count, fake, inactive, foreign, catalog-only Creative, or closed host still accepts a panel action; or
- either slot or an unrelated mod changes outside the observations below.

Preserve the complete `logs/latest.log` after any stop. Do not modify mods or configuration to continue.

## Setup

Create several distinguishable count-one vanilla shulker boxes. In one test shulker prepare:

- one partially filled occupied stack;
- one empty cell reserved for the same item/components;
- one empty cell reserved for a different item/components;
- one unreserved empty cell; and
- at least two other occupied cells so selection cycling is visible.

Keep a multi-count shulker stack as a denial control. Use the existing **Toggle Slot Reservation** key. Record actual results for CSR and QSN separately.

## 1. Panel visibility, placement, and ownership

In Survival inventory, hover a count-one shulker in the main inventory and then in the hotbar.

- A fixed 176x77 shulker panel should open beside the screen, preferring the right, falling back left, and clamping on-screen.
- All 27 cells must align with the vanilla shulker background and show live contents, counts, accepted ghost/zero/marker reservation visuals, ordinary item tooltips, and the standard hovered-cell highlight.
- The outer shulker tooltip must not cover the panel or fight for pointer input.
- Move from the host through the narrow corridor into the panel, then leave all owned areas. The panel should remain stable during the crossing and one grace frame, then close promptly without flicker.
- Replace, move, resize to multiple copies, or remove the host while the panel is open. Stale identity must close or reject; it must never jump to an equal stack.
- A multi-count shulker and an unsupported/fake/catalog slot must never open an actionable panel.

Repeat the visible-host checks in an ordinary chest menu, an eligible machine/container menu, Inventory Extended, and Creative’s actual player-inventory tab. Confirm Creative catalog entries and foreign slots are excluded.

## 2. Reservation editing

With the pointer on exact internal cells:

- Press the reservation key over an occupied eligible stack: its exact count-one component identity becomes the reservation.
- With a valid item on the cursor, press the key over an empty cell: that exact identity becomes the reservation without moving the cursor stack.
- With an empty cursor, press the key over an empty reserved cell: the reservation clears.
- Try an ineligible template, a mismatching occupied cell, stale host identity, and an unsupported host. Each must reject without changing contents or reservations.
- Close/reopen and move the shulker between hotbar, inventory, chest, and another legitimate viewer. Reservations and the established visuals must persist and synchronize.

## 3. Exact-cell panel transfers

Use ordinary clicks inside panel cells; double-clicks, modifier clicks, hotbar swaps, drag/release gestures, and scrolling must not leak into vanilla slot actions.

- Empty cursor + primary click on an occupied cell extracts the whole stack to the cursor.
- Empty cursor + secondary click extracts the ceiling half.
- Nonempty cursor + primary click inserts as much as the exact target can accept.
- Nonempty cursor + secondary click inserts exactly one.
- Matching merges, matching reservations, and unreserved empties accept only within effective maximum capacity.
- Mismatching reservations, nested shulkers, full cells, nonwritable cells, fake slots, multi-count hosts, and stale payloads make no change.
- Verify counts and all unrelated components before and after every action; close/reopen to confirm the physical shulker owns the result.

## 4. Selection and contextual transfers

- Hover occupied internal cells and scroll over the host, corridor, and panel. The blue selection should follow the hovered occupied cell and cycle only through occupied cells.
- Move the pointer away and back without changing the host. Selection should persist independently of hover.
- Pick up the exact host from its original menu slot to the cursor. Selection should migrate to that exact cursor-held shulker only.
- With the selected cursor-held shulker, secondary-click an empty or compatible target slot. The selected internal stack should extract into that target within its capacity; an emptied selection advances deterministically to the next occupied cell.
- Secondary-click a normal source stack onto a count-one shulker slot, and a cursor source stack onto the hovered count-one shulker slot. Insertion must prefer occupied compatible stacks, then matching reserved empty cells, then unreserved empty cells.
- Verify both insertion directions with partial capacity, effective non-64 maxima, exact component matches/mismatches, nested-shulker denial, full shulker, nonwritable source/target, multi-count host, and no eligible destination.
- Confirm primary clicks and unrelated secondary clicks retain vanilla behavior. Close the menu or disconnect and confirm transient selection clears.

## 5. Existing CSR and integrations

Run representative accepted C4 regressions on chest/trapped chest/copper chest halves, barrel, shulker, dispenser/dropper/hopper, furnace family, brewing stand, crafter, Ender Chest inventory, and Double Barrels:

- manual placement, vanilla QUICK_MOVE, hopper/sided automation and native machine admission obey exact reservations;
- extraction, callbacks, result/XP behavior, processing, routing order, comparator/drop/open-close behavior, stack limits, and unrelated components remain vanilla or owner-controlled;
- unsupported entities, temporary containers, player-inventory slots, fake/inactive/result slots, and foreign menus remain excluded.

For QSN C9, verify matching reservation affinity, fallback ordering, nested shulker routing, zero-quantity handling, rejection paths, and no item loss/duplication. For CCAR C12, if present in the runtime profile, verify its published CSR API behavior independently; its controlled suite alone is not runtime evidence.

## Report

For every attempted section, report only what was actually observed, including exact counts for any transfer and whether restart/reopen/reconnect cases were performed. Classify CSR Canary 10 and QSN Canary 9 independently as `PASS`, `FAIL`, or `INCONCLUSIVE`; omitted rows remain unobserved. Preserve the complete log for any failure. Do not promote either slot without an explicit exact runtime `PASS` for that member.

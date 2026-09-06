# Container Slot Reservations Canary 13 runtime procedure

**Candidate: `0.1.0-canary13`; controlled validation is not a runtime pass. Do not promote.**

Use only the dedicated **Matcha Flavoured 26.2 Workbench**. Never use the protected 26.1.2 gameplay profile. Preserve `logs/latest.log` and stop immediately for a crash, absent or duplicated panel, competing outer tooltip, wrong host binding, click-through, loss, duplication, or unexpected mod/profile change.

Exact candidate: `container-slot-reservations-0.1.0-canary13.jar`, 170400 bytes, SHA-256 `61d24c69f12da4f0ca1c68407d929cc8f21e83ef29e32ac2b5922d5a650bf23e`, source `6d8258f74e7b80625db81cd61c7fec0e4a3a49b0`. It is `NOT_DEPLOYED` / `RUNTIME_UNTESTED`; do not use the intentionally present manually deployed C12 JAR as managed-deployment provenance. Slot A remains exact C11 deployment `eb9e9dc9-b213-42f1-b47f-e6530cb5802e`, artifact `2184b05b-e1f6-41dc-9632-bfd682d78546`, `READY_TO_TEST_VERIFIED` / `FAIL` at manager revision 101. That C11 failure is limited to the supplied missing-PacketContext crash; no other C11 row is inferred.

## Supplied manual C12 evidence — do not expand it

The user manually placed exact C12 in the dedicated 26.2 Workbench. It is not a Test Instance Manager deployment. The supplied observations are: the PacketContext/Polymer hover crash is resolved; the pinned panel generally opens/functions; right-click insertion into the shulker works; an unwanted transient blue selected-cell visual appears; secondary-click extraction from a carried shulker to an empty writable inventory slot is broken; and the standalone panel has no bottom frame. This is not a complete C12 PASS.

## Canary 13 focused gate

In Survival inventory and hotbar, hover a count-one vanilla shulker with at least two occupied internal cells, then move through the host-to-panel corridor.

- The panel opens once as a 176x83 shulker panel. Its 9x3 grid, title, item, reservation, ghost/zero/marker, tooltip, and ordinary vanilla pointer-hover visual remain aligned.
- No blue CSR selected-cell fill, border, outline, flash, or first-frame highlight appears. Scroll selection still changes the selected internal item for transfer purposes; this selection is intentionally invisible.
- The six-pixel lower frame is present immediately under the unchanged 176x77 upper panel. No player-inventory background or slots appear between them. Repeat with a resource pack that replaces `minecraft:textures/gui/container/shulker_box.png` when available.

With the same count-one supported shulker on the cursor, select a known occupied internal cell and secondary-click an empty writable player-inventory slot.

- The selected physical stack enters the target and the mutated shulker stays on the cursor.
- Repeat with a capacity-limited target: only its permitted amount moves, the remainder stays in the shulker, and total item count is conserved.
- Repeat with a reservation mismatch: neither target nor shulker changes.
- Remove the selected stack completely: selection advances to the next occupied internal cell, or clears when none remain.
- Confirm existing secondary-click insertion into a shulker remains unchanged.

Only if this gate passes, repeat the existing C10 matrix: reachability in accepted Inventory Extended ordinary storage, an ordinary chest, and one other non-recipe container; reservation editing, exact-cell movement, QSN, CCAR, capacities, persistence, and synchronization. Record CSR and QSN evidence independently; infer no unobserved row.

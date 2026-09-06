# Container Slot Reservations Canary 12 runtime procedure

**Candidate: `0.1.0-canary12`; controlled validation is not a runtime pass. Do not promote.**

Use only the dedicated **Matcha Flavoured 26.2 Workbench**. Never use the protected 26.1.2 gameplay profile. Preserve `logs/latest.log` and stop immediately for a crash, absent or duplicated panel, competing outer tooltip, wrong host binding, click-through, loss, duplication, or unexpected mod/profile change.

## First gate — context-free host fingerprint

In Survival inventory and hotbar, put one count-one vanilla shulker in a real normal slot, leave the cursor empty, and hover it without clicking.

- The CSR 176x77 panel and visible 9x3 grid must appear once without a PacketContext/Polymer crash.
- The ordinary outer shulker tooltip must not compete with it.
- Cross the host-to-panel corridor: the panel must remain visible and cells must highlight; move away to close it.

Only if this gate passes, continue the existing C10 matrix: repeat the reachability gate in accepted Inventory Extended ordinary storage, an ordinary chest, and one other non-recipe container; then test reservation editing, exact-cell movement, contextual right-click transfers, selection, QSN, CCAR, capacities, persistence, and synchronization. Record CSR and QSN evidence independently; infer no unobserved row.

Exact candidate: `container-slot-reservations-0.1.0-canary12.jar`, 170247 bytes, SHA-256 `8b3c75a9dc3f5b51fd9ab4fb730e56539b2fc6463acb92c4a21a95605383aa55`, source `992eaf9328c85656a7d758e4bcd385c8a8b5fbf8`. It is `NOT_DEPLOYED` / `RUNTIME_UNTESTED`; no runtime launch occurred for Canary 12. Slot A remains exact C11 deployment `eb9e9dc9-b213-42f1-b47f-e6530cb5802e`, artifact `2184b05b-e1f6-41dc-9632-bfd682d78546`, `READY_TO_TEST_VERIFIED` / `FAIL` at manager revision 101. That C11 failure is limited to the supplied missing-PacketContext crash; no other C11 row is inferred.

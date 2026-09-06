# Container Slot Reservations Canary 11 runtime procedure

**Candidate: `0.1.0-canary11`; controlled validation is not a runtime pass. Do not promote.**

Use only the dedicated **Matcha Flavoured 26.2 Workbench**. Never use the protected 26.1.2 gameplay profile. Preserve `logs/latest.log` and stop immediately for a Mixin error, absent or duplicated panel, cursor drawn behind it, competing outer tooltip, wrong host binding, click-through, crash, loss, duplication, or unexpected mod/profile change.

## First gate — panel reachability

In Survival inventory and hotbar, put one count-one vanilla shulker in a real normal slot, leave the cursor empty, and hover it without clicking.

- The CSR 176x77 panel and visible 9x3 grid must appear once.
- The ordinary outer shulker tooltip must not compete with it.
- Cross the host-to-panel corridor: the panel must remain visible and cells must highlight; move away to close it.

Repeat the same check in accepted Inventory Extended ordinary storage, an ordinary chest, and one other non-recipe container. Then proceed to the existing C10 matrix only if this gate passes: reservation editing, exact-cell movement, contextual right-click transfers, selection, QSN, CCAR, capacities, persistence, and synchronization. Record CSR and QSN evidence independently; infer no unobserved row.

Exact active candidate: `container-slot-reservations-0.1.0-canary11.jar`, 167741 bytes, SHA-256 `b56a40a7ed504e07c68b8d037e99c1fe5aa8a822352dfbdf351977dd963189c1`, source `c38897a5bfdea34c54b4ade519a44b31fc1cc150`, Slot A deployment `eb9e9dc9-b213-42f1-b47f-e6530cb5802e`, artifact `2184b05b-e1f6-41dc-9632-bfd682d78546`; manager revision 100, Stack v19, state `831cca7a8afbd59889cd81cb764cacbcfd3d155fde21192cbc22b4fbe7b43e09`, physical `aa989e9fe69513f92f319b3a4f58a58846bc8da0d8391f0ac0cf4370cd86aeb0`. It is READY_TO_TEST_VERIFIED / RUNTIME_UNTESTED. Do not launch Minecraft during deployment administration; launch only to perform this runtime procedure.

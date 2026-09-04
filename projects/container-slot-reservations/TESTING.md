# Testing

## Current gate

**READY TO TEST VERIFIED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Test only `artifacts/container-slot-reservations-0.1.0-canary6.jar`, 117,505 bytes, SHA-256 `93616BD2794A936EB6485614C3215AD2F42B149053BCB8DCD80E6A7C668AED40`, embedded version `0.1.0-canary6`, source `6a1a284b0410bcce77467756e5046ed76117aa80`.

Test Instance Manager revision 88 at `2026-09-04T20:19:06Z` physically verified accepted Stack v18, state digest `DEE397C4FFEBACABF41028A0FAFB22D674EE695A937BA22F9A6122771577C9A4`, and physical inventory digest `C6FD9345DD5DA38D5FD07ED46628D09BAF0C3B6B2014C3BE24A1848647A73F2`. Slot A contains exact C6 deployment `9783fefa-2d3f-4f4b-acdc-766d6b5ac2ab` / artifact `1ef9a92a-47ef-4ea5-9f9f-b0166d042dd1` as `READY_TO_TEST_VERIFIED / UNTESTED`; Slot B is empty. Accepted CSR C4 remains the baseline and C1 remains rollback.

Exact C5 deployment `f12e434a-51c1-4e84-a487-3ebcadbe3d64` / artifact `c9551ad6-7a35-4a0e-ac25-af3efeccd86a` was recorded `FAIL` at manager revision 86 at `2026-09-04T20:15:53Z` only from the user's report that an Easy Shulker Boxes carried-shulker insertion entered a mismatched reserved slot and the reservation remained visible after the item was removed. No other C5 row passed or failed. Reservation persistence after removal remains intentional; C6 must prevent the mismatched insertion without deleting or rewriting the reservation.

C6 filters Item Interactions' exact occupied and empty candidate arrays before its temporary `SimpleContainer` mutates shulker contents. Expected policy is native shulker nesting first, then exact item/components reservation admission; matching reserved candidates precede unreserved candidates, mismatches are unavailable, and the filter itself changes neither reservations nor contents. The optional string-targeted integration must remain safe when Easy Shulker Boxes / Item Interactions are absent and must fail closed if the audited seam changes.

C6 also uses only the exact supplied 1,723-byte 3×3 RGBA occupied-reservation marker, SHA-256 `D12D0BE850A742C69795259FDE9A5D9D52E822DF529196445948241B4B199F02`, at the same `itemY` and `itemX + 12`—one pixel left of C5. It renders only for `OCCUPIED_RESERVED`.

## Preflight and startup

Use only the dedicated Minecraft 26.2 Workbench. Never access the protected 26.1.2 gameplay profile.

1. Before launch, run the Test Instance Manager read-only verification and require `PHYSICAL_STATE_VERIFIED`, manager revision 88, the exact state and inventory digests above, Stack v18, exact Slot A identity with independent `UNTESTED`, and empty Slot B. Stop on any drift.
2. Launch to the title screen and inspect the complete startup log for mixin target, injection, linkage, optional-classloading, or seam-drift errors. Do not change the verified profile to manufacture an optional-mod absence test; controlled absence checks and exact upstream bytecode audits do not count as runtime evidence.
3. Enter a disposable world only after startup succeeds. Use the Easy Shulker Boxes / Item Interactions carried-shulker action that transfers a source stack into a shulker item; ordinary placed-menu insertion alone does not exercise the repaired seam.

## Carried-shulker regression

Prepare a distinctively named shulker, preserving at least one unrelated item component, and create reservations while it is placed. Include an empty exact-component reservation, an empty reservation for a same-item component mismatch, an occupied exact reservation that can still merge, and at least one unreserved empty slot. Break and carry the shulker, then exercise these cases through the carried interaction:

1. Arrange an unreserved candidate before a later matching reserved candidate. Insert the exact matching item/components and confirm the matching reserved slot is chosen first.
2. Insert the same base item with different components, then an unrelated item. Confirm each mismatched reserved candidate is skipped and an unreserved fallback is used without changing the reservation.
3. Repeat a mismatch with every unreserved fallback unavailable. Confirm insertion is denied, the source and all shulker contents remain unchanged, and no reservation changes.
4. Insert into the occupied exact reservation and confirm the native merge, effective maximum, and remainder are unchanged. A mismatched occupied reservation must not grow and must fall back or deny by the same policy.
5. Attempt to insert a shulker into the carried shulker and confirm native nesting denial is preserved.
6. Remove an item that was admitted to its matching reservation, close the carried view, reopen it, move the shulker through inventory/hotbar, and reopen again. Confirm the physical item stays removed, the reservation remains visible and component-exact, the next mismatch is still denied, and the next exact match is still preferred.
7. Across every insertion/remove/reopen cycle, confirm the shulker's name and other unrelated components remain exact and no unrelated container slot changes. Inspect the full log for duplicate writes, component loss, stale previews, rejected writeback, or reservation/content mutation errors.

## Marker and representative regressions

1. In a normal supported menu and the carried-shulker preview, confirm the new 3×3 marker is one pixel left of C5 at `itemX + 12, itemY`, is nearest-sampled and unscaled, and appears only on an occupied reserved slot. Empty reservations retain the approved ghost, literal zero, and tooltip without the marker; unreserved slots remain unchanged.
2. Recheck representative accepted C4 behavior: manual placement, vanilla `QUICK_MOVE`, hopper insertion where native sided rules permit, accepted QSN insertion, one machine writable/non-writable case, one copper chest, one Double Barrels physical half, one Ender Chest, and an ordinary placed shulker. Confirm native ordering, extraction, persistence, counts, callbacks, and reservation-free behavior remain unchanged.
3. Close and reopen the world once, then inspect the complete log through normal shutdown. Do not infer a result for an unobserved owner, route, visual, or optional-mod configuration.

## Stopping and recording

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE`, as supported by what was actually observed, if exact identity/readiness drifts; startup or optional linkage fails; a mismatched reserved slot is mutated; a matching reservation loses priority; no-fallback denial mutates source, contents, or reservation; nesting rules change; remove/writeback/reopen loses contents, reservation, or unrelated components; the marker asset/state/anchor differs; an accepted C4 behavior regresses; or a CSR-attributable log error appears.

Record only C6 behavior actually observed. The two clean 71/71 JUnit and 21/21 GameTest runs, identical JAR result, dependency audits, deployment, and physical verification establish controlled readiness only; they are not Minecraft runtime evidence and do not authorize promotion.

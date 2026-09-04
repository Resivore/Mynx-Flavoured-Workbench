# Testing

**READY TO TEST VERIFIED — RUNTIME UNTESTED — NOT PROMOTED**

Slot B: deployment 98fe1cc9-eeaa-4c07-8b72-17678bc07581, artifact 2eb193ee-0975-4a2c-a725-c97a721c35b4, carried-container-auto-routing-0.3.7-csr-reservation-affinity-canary1.jar, 42,805 bytes, SHA-256 f00b1e6bc64a0e63ac1199d89c08f7510a20d5bbb9a486b40bd97833df522a2f, source c373b663c99d7a217fc9147db5007d3f1057678f.

Manager revision 90 / accepted Stack v18; state SHA-256 3cc3a9d83c34aa763263478fb7d54c1fb6aa1aee384ae48af3e815130f4a43b5; physical inventory SHA-256 1d4138fc6648f9181daaecde5374f99810072e778da9c513910fdcee96284d9e. Both exact current releases are READY_TO_TEST_VERIFIED with independent UNTESTED results.

The paired providers are CSR C7 and CCAR C12. Accepted CSR C4 and CCAR C11 remain exact predecessors; CSR C1 and CCAR C10 remain exact rollback artifacts. The manager receipt records deployment/readiness timestamp 2026-09-04T23:23:00Z. Physical verification is not a gameplay pass.

## Before testing

1. Use only the dedicated Matcha Flavoured 26.2 Workbench. Verify the canonical manager reports PHYSICAL_STATE_VERIFIED, revision 90, Stack v18, the digests above, exact current C7 in A and C12 in B, and independent UNTESTED results. Stop on drift.
2. Launch only when performing this user runtime procedure. Inspect startup logs for injection, linkage, registry/component, or dependency errors before entering a disposable world. No dedicated client was launched during this task.
3. Preserve accepted QSN C8, SAS Container Fixes C2 with Stacks Are Stacks 2.1.2-1.26.2, accepted Offhand Shift-Click QoL, ESB 26.2.3 and nested Item Interactions 26.2.2. Do not change the protected 26.1.2 gameplay profile.

## Reservation affinity in all acquisition paths

1. Carry an otherwise physically empty unlocked shulker with an exact matching CSR reservation. Acquire the item through ground pickup, external-container QUICK_MOVE and player-origin QUICK_MOVE in separate cases. Require the item to enter that reserved physical index.
2. Put an earlier-index unreserved empty before a later matching reserved empty; the reservation must win. Use several matching reservations and require physical order across all of them before any ordinary empty.
3. Test same-item/component-different and unrelated reservations: neither creates affinity. An entirely unreserved empty shulker remains ineligible. Do not expect a generic backpack.
4. Combine a physical matching partial, a matching reserved empty and an unreserved empty. Require occupied merge, then matching reserved empty, then ordinary empty; reconcile every source, destination and remainder count.
5. A full physical matching stack must still establish affinity and permit an unreserved fallback. A conflicting reservation on an occupied stack must neither allow growth nor create affinity through that conflict.
6. Deny mismatched reservations when no permitted fallback exists, without changing the incoming stack, reservation or carrier. Deny shulker nesting. Remove an inserted item and require the original reservation to remain.

## Priority, composition and persistence

1. A locked shulker must not qualify; unlocking restores affinity. Test ordinary inventory storage and offhand carriers, multiple carriers in stable scan order, excluded source slots and explicit offhand exclusion. Do not prioritize reservation carriers over earlier physical-match carriers.
2. Preserve selected occupied main hand, carried-container tier, occupied hotbar/offhand, ordinary-storage partials, empty hotbar and empty ordinary-storage fallbacks. Include Inventory Extended storage at indices 9, 36 and 62 where present, and accepted Offhand Shift-Click QoL composition.
3. Repeat real acquisition/menu cases in Survival and Creative. Test counts below, equal to and above one slot capacity; reconcile totals and exact remainder ownership.
4. Repeat with an eligible normally nonstackable SAS item whose effective maximum exceeds one, such as a configured saddle. Require the effective maximum and exact total count to survive. Do not add a MAX_STACK_SIZE reservation patch.
5. Recheck bundle routing and QSN C8 without changing their behavior. Confirm custom name, lore, damage, lock, CSR reservations and unrelated carrier components survive insertion, removal, close/reopen, save/reload and reconnect.
6. Run [CSR's empty-reservation tooltip procedure](../container-slot-reservations/TESTING.md). Ghost reservations must remain visual metadata; the shulker's physical fullness must reflect only real items.

## Evidence and stopping conditions

Accepted C11 retains its aggregate focused RUNTIME_PASS; requiring physical matching contents was its deliberate design. C12 adds reservation affinity and remains independently UNTESTED. CSR absence was controlled-tested with no CSR runtime classes, and incompatible API failure is fail-closed; do not alter the dedicated paired stack just to repeat those controlled cases.

Record only actual PASS, FAIL or INCONCLUSIVE observations for C12. Do not transfer a CSR result to CCAR. Stop on identity drift, startup/linkage errors, wrong priority, reservation mismatch insertion, locked-carrier admission, component/reservation loss, desynchronization, count/remainder discrepancy or item duplication/loss. Neither successor is ready for promotion without its own runtime evidence.

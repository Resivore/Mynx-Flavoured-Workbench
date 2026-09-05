# CSR C8 runtime procedure

**CONTROLLED VALIDATION PASS - RUNTIME UNTESTED**

Exact candidate: `container-slot-reservations-0.1.0-canary8.jar`, 140230 bytes, SHA-256 `e1a21bf322384e23ab60dc30cd3dc7fabe4d4a580d708b439751e1bf70b84708`, source `a333a4b5fa417cd9d35f6fc2608727828c23f6ea`. Verified Slot A; deployment `fcbab307-141b-49da-9e91-0ae1c6327205`, artifact `4d1dc3c0-e081-431b-82d9-65be1d012ca5`. Verify canonical manager readiness before the user launches the dedicated 26.2 Workbench.

1. Hover occupied, empty reserved and empty unreserved cells. Check native highlight and indices 0, 8, 9, 17, 18 and 26; repeat all edges at two GUI scales and with native scroll/collapse settings.
2. With the existing CSR key, set/replace an occupied template and toggle an exact match off. Set/replace an empty cell from the cursor, clear with an empty cursor, and verify an empty unreserved cell is a non-consuming no-op. A valid tooltip cell takes priority over the outer slot.
3. Test hosts in player inventory and an open chest, including identical duplicate shulkers. Only the selected host changes. Move/change the host or menu before an action; stale actions must do nothing.
4. Keep a second viewer on the shared chest. Both viewers must update immediately without reopening. Contents, name, lore, lock and all unrelated components must survive.
5. A physically empty reserved shulker has its native 9x3 tooltip with 27 actual empty contents, ghosts and zeroes. Clear the final reservation and verify native no-tooltip behavior returns.
6. Create a reservation from an empty named/lore-bearing undyed and dyed shulker in a normal menu. Exact identity must match; filled or internally reserved variants must be rejected. Eligible multi-count template identity normalizes to one; multi-count host editing is rejected.
7. Filled, reservation-bearing and combined shulker templates are rejected from occupied/cursor creation paths. Historical invalid templates remain visible and explicitly clearable, with no migration.
8. Recheck carried insertion filtering, ghost alpha, literal zero, exact occupied marker, copper chest and Double Barrels ownership, Ender persistence, machine permissions and QSN read-only API behavior. Shulker nesting remains denied.
9. Inspect the full client/server log. Stop on mixin/network errors, stale edits, visual geometry drift, lost components, mismatched insertion, desynchronization or count discrepancy. Record only observed rows and the exact release identity.

Controlled evidence: 87 JUnit tests; 24 GameTests; clean Java 25 / Gradle 9.5.1 / Loom 1.17.19 build with accepted Double Barrels; absence baseline and native ESB 26.2.3 / ItemInteractions 26.2.2 bytecode audit. GUI geometry/state and native empty-highlight seams have controlled coverage; actual interactive client rendering/scroll/collapse remains unobserved.

Deployment verification: Manager revision 93, accepted Stack 19, timestamp 2026-09-05T04:27:20Z; state SHA-256 6a9e9cd01e01b210b239d50416483cd4eb426c3b57a57f25a3f9f914f39eb2e6; physical inventory SHA-256 d50a36599630947d42b95bf9e2a185acad04709f4705a32359deed8e8b274639. One atomic DEPLOY_PROFILE replaced C7 in A and placed C9 in B. Both exact current releases are READY_TO_TEST_VERIFIED with independent UNTESTED results. Accepted CCAR C12, SAS C2, Stacks Are Stacks, Offhand Shift-Click QoL, Double Barrels and all unrelated accepted units remain unchanged. Exact upstream QSN 0.4.0 remains accepted passthrough; accepted C8 is disabled as the reversible predecessor. No dedicated client was launched.

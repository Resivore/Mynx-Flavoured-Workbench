# Testing

**READY TO TEST VERIFIED — RUNTIME UNTESTED — NOT PROMOTED**

Slot A: deployment 2764b293-5c8a-41c2-a4a5-66b2913b1642, artifact 9cbbc908-966c-4cd5-99d7-7244de6bf6d9, container-slot-reservations-0.1.0-canary7.jar, 119,442 bytes, SHA-256 fafbdbc4b5f3c470f291183baa6fd161ec56604ad6a06fd64bf59556d037e239, source c373b663c99d7a217fc9147db5007d3f1057678f.

Manager revision 90 / accepted Stack v18; state SHA-256 3cc3a9d83c34aa763263478fb7d54c1fb6aa1aee384ae48af3e815130f4a43b5; physical inventory SHA-256 1d4138fc6648f9181daaecde5374f99810072e778da9c513910fdcee96284d9e. Both exact current releases are READY_TO_TEST_VERIFIED with independent UNTESTED results.

The paired providers are CSR C7 and CCAR C12. Accepted CSR C4 and CCAR C11 remain exact predecessors; CSR C1 and CCAR C10 remain exact rollback artifacts. The manager receipt records deployment/readiness timestamp 2026-09-04T23:23:00Z. Physical verification is not a gameplay pass.

## Before testing

1. Use only the dedicated Matcha Flavoured 26.2 Workbench. Verify the canonical manager reports PHYSICAL_STATE_VERIFIED, revision 90, Stack v18, the digests above, exact current C7 in A and C12 in B, and independent UNTESTED results. Stop on drift.
2. Launch only when performing this user runtime procedure. Inspect startup logs for injection, linkage, registry/component, or dependency errors before entering a disposable world. No dedicated client was launched during this task.
3. Preserve accepted QSN C8, SAS Container Fixes C2 with Stacks Are Stacks 2.1.2-1.26.2, accepted Offhand Shift-Click QoL, ESB 26.2.3 and nested Item Interactions 26.2.2. Do not change the protected 26.1.2 gameplay profile.

## Empty reservation tooltip

1. Place a named vanilla shulker and reserve distinguishable items at physical indices 0, 8, 9, 17, 18 and 26. Use component-distinct named items where useful. Remove every physical item, break the shulker and carry it.
2. Hover through ESB with the normal configured modifier/visibility conditions. Require exactly one native 9×3 contents grid with approved ghosts and literal zeroes at the six exact physical indices. Unreserved slots stay visually empty; fullness/storage bar stays physically empty.
3. Confirm native dimensions, background, selected-slot/highlight behavior, item names, scrolling, collapse/expand, mouse behavior and modifier configuration. Disable the upstream tooltip setting and require its normal hidden behavior; restore the setting. Do not manufacture a changed mod stack for optional-absence testing.
4. Clear the final reservation from a physically empty shulker. Require the exact native empty-shulker no-preview behavior; test another never-reserved empty shulker as the control.
5. Recheck an undyed and dyed shulker and a physically nonempty shulker. Require the established tooltip without duplication, correct slot-local occupied marker at itemX+12,itemY, approved ghost alpha/zero and unchanged native interactions.

## Preserve C6 insertion and established CSR behavior

1. Through ESB carried-shulker insertion, put an earlier unreserved candidate before a later exact matching reservation. Matching reservations must lead, while random/unrelated and same-item/component-distinct items must skip mismatched reservations.
2. Repeat without any permitted fallback; require denial with unchanged source, contents and reservations. Merge a compatible occupied stack, then remove it and confirm its original reservation reappears. Shulker nesting remains denied.
3. Recheck placed chests including copper, Double Barrels physical halves, Ender Chest ownership/persistence, machine native/sided insertion rules, hoppers and QSN C8 public-API insertion. Keep exact component matching and removal/persistence behavior.
4. Use an eligible normally nonstackable item whose SAS effective maximum exceeds one (for example a configured saddle). Confirm counts above one survive container, tooltip, reservation and insertion/remove/reconnect paths without an explicit MAX_STACK_SIZE reservation patch.
5. Save/reconnect and repeat the empty-reserved preview and mismatch protection. Preserve shulker name, lore, lock and unrelated components.

## Paired CCAR check and evidence

Run [CCAR's reservation-affinity procedure](../carried-container-auto-routing/TESTING.md) in the same exact stack. Judge the projects independently.

C6 was reported FAIL only because the physically empty reserved tooltip was missing; its successful insertion observation remains preserved. C7 is untested. Record only actually observed PASS, FAIL or INCONCLUSIVE findings for C7; do not infer a C7 pass from controlled tests or a CCAR result.

Stop on incorrect artifact identity, startup errors, missing/duplicate/incorrect tooltip slots, nonzero physical fullness for reservations alone, altered approved visuals, mismatched insertion, lost reservation/components, or inventory loss/duplication. Do not promote until independently reported runtime evidence supports it.

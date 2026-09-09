# BGE C66 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.10-bge.canary66.quarter-column-continuation+26.2.jar`, SHA-256 `02436dd9744fc2dbcc723205b12501cf7c49e2dfde72343480685ccf9498cb9e`, embedded version `4.2.10-bge.canary66.quarter-column-continuation+26.2`, source checkpoint `fd9d66032d271cf2c419600858985f6380819323`. Exact C58 remains accepted; C62 and C64 remain external runtime-failed provenance.

C66 is `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; its clean build, archive validation, and 104/104 GameTests are not Minecraft runtime evidence. The project remains `TESTING` only because its immutable UUID is assigned to an older Slot A cohort; that does not deploy or ready C66. The earlier serialized C65 deployment dry run failed closed before profile access because an unrelated project control log has an invalid unknown C3 implementation entry, so do not retry until repository control validation is repaired. Use only the dedicated Matcha Flavoured 26.2 Workbench. Never use or modify the protected Matcha Flavoured 26.1.2 profile.

## Manual checks

1. Place one compatible Quarter Column. Hold a second of that exact type, aim at an empty portion of the same occupied cell without intersecting its geometry, and place. Confirm the matching terminal quarter is added in that same cell with no second source-block debit.
2. Repeat from representative initial quadrants and attempt another placement after the cell reaches its allowed terminal occupancy. Confirm existing quadrant selection remains authoritative and a full cell does not change or consume an item.
3. Attempt the empty-cell continuation with an incompatible Quarter Column. Confirm it does not merge or consume an item. Confirm normal placement into an empty/replacement target is unchanged.
4. Recheck ordinary compatible Step continuation through an empty portion of its occupied cell; it must still combine exactly as before.
5. With BBB present, confirm a representative BGE Step and Quarter Column retain the C65 axis texture/orientation behavior, and the client reaches a title/world screen without a `bbb:blockstates/warped_beam.json` resource-reload failure. Record `PASS`, `FAIL`, or `INCONCLUSIVE` only from actual Minecraft observation.

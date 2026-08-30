# Testing

## Candidate and evidence boundary

Exact BGE C54 is installed and readiness-verified in Slot A together with the exact Nibaru C46 contract artifact:

- `artifacts/cnm-nibaru-integration-0.6.1-bge-canary54-layer-economy.jar`, 258,564 bytes, SHA-256 `7CD96C8153B2DDCA863A96BCF0BBB04680CA6CE92A63A2086A7155441EC9812A`;
- `../nibaru/artifacts/more-slabs-stairs-and-walls-4.2.0+26.2-port-canary46-bge-layer-contract.jar`, 5,652,769 bytes, SHA-256 `3281D110F062DB62E721D838A35CE915CA73DD41AF098A013B52952561CEAE7D`.

BGE C54 was built from source `f7f61a0c50aa0fb96a8fd4d2cb79ce7ef1a7e62e`. The Java 25 clean build, 52 focused architecture/resource checks, and 67/67 controlled GameTests passed. Manager revision 37 returned `READY_TO_TEST_VERIFIED / UNTESTED`; Minecraft was not launched for C54. Static checks, GameTests, artifact identity, and manager readiness are not a runtime pass.

Predecessor C53 is `FAIL`: compatible same-block Layer growth consumed extra source items, and its generated Layer item presentation was reported off-center. Preserve only the user's other reported evidence: one-full-source-block return, gravity/falling-material behavior, grass lifecycle, and glass/translucency were correct. No unreported row is inferred. Accepted/rollback BGE C52 remains unchanged, and neither C53 nor C54 is promoted.

## C54 Layer runtime matrix

1. Confirm Slot A is `READY_TO_TEST_VERIFIED / UNTESTED` and contains exact C54 and C46 identities above. Reach the title screen and a disposable test world only under explicit runtime ownership; stop on a Loader rejection, startup crash, registry/mixin error, ShapeMap error, missing resource, or other relevant log error.
2. Compare representative ordinary, pillar/axis, and Glazed generated Layer items in inventory and the hotbar. Confirm their shared item projection is visually centered and consistent without changing any placed-world model, material pattern, or Glazed appearance.
3. Put exactly two generated Oak Planks Layer items in survival inventory and use that Layer `BlockItem` directly. Confirm the first placement consumes one Layer item; compatible same-block growth from thickness 1 through 4 consumes none; a fifth, wrong-face, and incompatible attempt each consume none; and normal survival breaking returns exactly one Oak Planks. Block adjacent valid placement while testing each intended failure.
4. Exercise representative Layers in all six directions. Confirm opposite-face anchoring, 4/8/12/16-pixel visible and collision thickness, exposed-face-only compatible growth, waterlogging for partial Layers, dry/full behavior, deterministic ShapeMap order after Step, and no generated-family recursion.
5. Smoke-check one falling material, grass lifecycle and typed base drops, glass/translucency, and unchanged Step and Vertical behavior. Also sample ordinary, pillar/axis, and Glazed rotation, mirroring, persistence, and material identity without expanding the test into unrelated behavior.
6. Stop and leave C54 unaccepted on any incorrect identity, centering, direction, anchoring, thickness, collision, water state, economy, one-full-source return, material transform, ShapeMap order, duplicate/recursion, specialized smoke behavior, or relevant log error. Record only behavior actually observed in Minecraft.

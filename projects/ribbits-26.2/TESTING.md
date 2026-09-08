# Testing

## Canary 21 — pending dedicated-slot runtime test

Supplied external Canary 20 evidence is deliberately recorded as mixed: the Naturalist snail leash is visually **GOOD**—both the snail and Wandering Ribbit endpoints are correctly placed and the leash visibly connects them. The closed/undeployed Chute Leaf is also correct. The deployed/open authored Chute Leaf remains a black/magenta missing-texture checkerboard, so C20 is not an overall runtime pass.

Canary 21 changes only the private open-Chute conversion: verified Blockbench texture-pixel UVs are normalized from the exact source PNG dimensions into Minecraft's fixed 0–16 model UV space. The exact 32×32 authored inputs, cube bounds, rotations/pivots where represented, shade, display transforms, ambient occlusion, `#layer0` bindings, open PNG bytes, closed resource chain, C20 leash correction, and C19 optional-Naturalist startup safeguards are preserved. Static evidence does not prove the visual repair.

Both canonical Test Instance Manager slots are occupied by unrelated ready-to-test cohorts at manager revision 117. Do not evict either cohort. Canary 21 is `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED` until a slot is deliberately assigned.

When a serialized Test Instance Manager slot is deliberately available, use only the dedicated Matcha Flavoured 26.2 Workbench. Never access or modify the protected Matcha Flavoured 26.1.2 gameplay instance. Rehash retained `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary21.jar` before deployment and replace, never duplicate, the existing `ribbits` JAR.

1. Launch the ordinary managed stack with Ribbits C21, Naturalist, Simple Copper Pipes, Sodium, and the usual Matcha Flavoured 26.2 dependencies. Confirm normal startup with no `MixinTargetAlreadyLoadedException`, early-Entity loading, Simple Copper Pipes preparation failure, Sodium preLaunch failure, or Ribbits/Naturalist mixin failure. Repeat without Naturalist when the managed stack permits it; the optional target must remain harmless.
2. Confirm the closed Chute is correct on the player's back.
3. Deploy the Chute in third person. Confirm there is no black/magenta checkerboard, the exact authored open texture is visible, and the edited authored canopy geometry is visible.
4. Inspect the deployed Chute from front, back, sides, above, and below where practical, then inspect first-person rendering.
5. With Naturalist installed, reconfirm the Wandering Ribbit ↔ scheduler-created Naturalist snail leash: both endpoints meet their entities and remain correctly connected while stationary and moving. Snails held by other entities remain unchanged.

Record only observed behavior. If texturing succeeds but a separate pose/orientation defect remains, record it independently; do not alter transforms preemptively. Stop with `FAIL` on any startup, closed-Chute, texture, geometry, leash, migration, menu, or persistence regression. Do not record runtime `PASS` from build, static tests, resource validation, archive inspection, or GameTests.

# Testing

C2 (`0.1.0-canary2`) is the current candidate and remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`. Test only exact `slab-decorations-0.1.0-canary2.jar`, 36,138 bytes, SHA-256 `D405D5A699ABA2C266B4AC227D7418CC2B07982C93D17755012F95087ECFA390`, produced by legacy source `bf1af349a0e43a49f12279e8e49794a2e1423aac`.

The frozen C2 provenance records a Java 25 / Gradle 9.5.1 offline clean build, 1/1 JUnit artifact-contract test, and all 16 required Fabric GameTests passing before the retained JAR was copied and independently matched. Migration verification used Temurin Java 25.0.4.1+1, Gradle 9.5.1, and Loom 1.17.19: `clean test build -x runGameTest --offline --no-daemon` passed 1/1 JUnit test, `compileGameTestJava` passed, and the fresh 36,138-byte deployable reproduced the retained C2 SHA-256 byte-for-byte. No GameTest or Minecraft process was launched during migration. These are source/build/static results, not C2 Minecraft runtime evidence.

## Runtime preconditions

1. Obtain explicit Test Slot ownership and use a disposable Minecraft 26.2 Fabric test world. Do not mutate an accepted release or substitute C1 in place of C2.
2. Verify the exact C2 artifact above before deployment. Preserve its exact Nibaru C41 material-profile/native-slab dependency, SHA-256 `8DFB6E4F55ACF021118D022D99EC9A78CB2C74C13AD469BAB58A68734969C7B6`; later Nibaru or Block Geometry Extensions artifacts require a separate compatibility reconciliation and are not inferred equivalent by this migration.
3. Use Fabric Loader 0.19.3 or later, Fabric API 0.157.0+26.2 or later, and the documented optional CNM 2.0.7 / isolated Integration 1.36 fixture identities where applicable. Record only behavior actually observed.

## Focused C2 runtime matrix

1. In Survival, bonemeal exact dry bottom native Grass Block, Moss Block, and Pale Moss Block slabs. Each valid activation must consume exactly one bonemeal and leave the clicked exact native bottom slab intact.
2. For Grass, compare with a nearby full Grass Block in the same biome. Confirm representative vegetation and biome flower/petal generation, with generated decorations represented on the real `+0.5` slab surface.
3. For Moss, compare with a full Moss Block. Confirm representative spread and vegetation, and require every eligible canonical replacement to reconcile to the exact corresponding bottom native moss slab.
4. For Pale Moss, compare with a full Pale Moss Block. Confirm representative spread, growth, and carpet behavior, with eligible canonical replacements reconciled to exact bottom native pale-moss slabs.
5. Check generated short/tall grass, flowers, pink petals, wildflowers, azalea/flowering azalea, moss carpet, and pale-moss carpet. Verify model position, outline/collision where applicable, targeting, Survival break/drop, support-removal cleanup, and save/reload at the half-height surface. Bonemealing a lowered generated azalea must not consume the item or grow a floating tree.
6. Confirm unsupported materials and waterlogged bottom slabs do not activate or consume bonemeal. Confirm top/double slabs, vertical slabs, CNM Steps, stairs, walls, and unrelated geometry gain no C2 substrate spread, reconciliation, or offset behavior.
7. Recheck C1 behavior: short-grass and fern bonemeal must still produce coherently lowered tall grass/large fern and clean up after support removal; representative flowers, dead bush, and nether wart must preserve placement, survival, growth/harvest, targeting, and non-bonemeal behavior.
8. Save and reload representative C2 results, then inspect logs for Mixin application, registry, projection-restoration, reconciliation, rendering, targeting, collision, or dependency errors. Confirm unrelated Nibaru/BGE/CNM geometry and accepted-stack behavior remain unchanged.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for any wrong item consumption, lost or changed slab identity, canonical full-block residue, spread into unsupported geometry/material, waterlogged activation, floating tree, invalid generated decoration, stale representation after reload, C1 regression, dependency mismatch, crash, or relevant error. Do not promote C2 from static evidence.

## Preserved C1 evidence

Exact C1 is retained at `artifacts/slab-decorations-0.1.0-canary1.jar`, 19,926 bytes, SHA-256 `B8B586D87E9EFCD6E14A652F36D09EEF5F3A5179DE72086704FF4E53DFD9D88D`, source `3d21d4db9cedae4690f8de7fdb1f34ea5a8887e3`. At `2026-08-26T05:07:26Z`, the legacy manager deployed that exact retained binary without rebuilding, preserved all 23 accepted artifacts, made C1 the sole experimental overlay, recorded no temporary diagnostic, and returned `READY_TO_TEST_VERIFIED` at `2026-08-26T05:07:26.3564075Z`. The user later reported an aggregate focused runtime pass; no individual checklist row was reported or inferred. The frozen lifecycle record subsequently classified C1 as released without promotion. C1 is not accepted, and the V2 release schema therefore does not place it in the accepted-baseline rollback slot.

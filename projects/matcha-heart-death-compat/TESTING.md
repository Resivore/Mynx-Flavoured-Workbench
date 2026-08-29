# Testing

## Current gate

**NOT READY FOR PROMOTION**

Exact Canary 7 remains `TESTING`, `READY_TO_TEST_VERIFIED`, and
`PARTIAL_RUNTIME_PASS` at the frozen legacy checkpoint. Its Crystal Heart,
Sculk Sensor, and Sculk Shrieker recipe-precedence contracts are known to fail
because Matcha still wins those three effective recipes. Do not promote or
reclassify unchanged C7, and do not infer results for unreported checks.

This migration did not build a release artifact, deploy, rerun promotion,
change either Test Slot, or access a Minecraft instance. The deployment field
preserves prior evidence for the exact retained C7 binary; it is not a new
deployment claim.

## Retained current evidence

- Current C7 is `matcha-heart-death-compat-0.1.6-canary7.jar`, 30,407 bytes,
  SHA-256 `3B1F608C6F4B3B92F764A3781A9652DC11F089590A9173444C067975D01CA2DD`,
  from source `3dbd78201104686a061df7a94d3be5162072c877`.
- Frozen static evidence records a Java 25 clean build and 31 passing tests in
  six suites. Migration verification reproduced all 31 tests with zero
  failures, errors, or skips; the fresh build output was not substituted for
  the retained artifact.
- The reported C7 runtime pass confirmed that Sculk Sensor and Sculk Shrieker
  no longer dropped Echo Shards and that the Reinforced Crystal Heart recipe
  was correct.
- The reported C7 runtime failure confirmed that Crystal Heart still used
  Divine Fragment and that the Sensor and Shrieker recipes were not the
  intended compatibility recipes. No C8 exists.
- The frozen Heart C7 + Mossy Stone C2 manager profile was
  `READY_TO_TEST_VERIFIED` with 26 accepted artifacts, exactly two ordered
  overlays, and zero diagnostics. That readiness result does not upgrade C7's
  runtime classification.

## Accepted rollback evidence

Exact Canary 5 is both the accepted release and rollback:
`matcha-heart-death-compat-0.1.4-canary5.jar`, 22,986 bytes, SHA-256
`21E24D37B1A3B575B738F7CA38B0614C9D0285C4F0723DEFE4DB85297B171C04`,
from source `5477ca770e58a8b4bed8f1bf737b04765a9d12a7`.

The user's 2026-08-21 dedicated-Workbench pass on that exact binary confirmed
Reinforced crafting, appearance, recipe-book visibility, and JEI exposure;
ordinary poisonous-potato behavior; no automatic Crystal Heart use; Crystal
refusal below ten hearts; Reinforced restoration of exactly one maximum heart
below ten and refusal at ten; one-heart loss on every death above and below ten
to the five-heart floor; Matcha keepInventory; and no crash or visible project
failure. The matching log recorded exact mod/version identity, successful
recipe and advancement enforcement, one Heart JEI runtime ingredient, normal
shutdown, and no relevant Heart, Mixin, codec, duplicate-death, or guarded-
function error. Dedicated-server validation remains unperformed.

## Next controlled runtime matrix

Run this only after a separately authorized source fix produces and deploys a
new exact candidate. Retest the changed recipe-precedence boundary and the
accepted Heart/death baseline together:

1. Confirm exactly one Heart candidate is enabled and the retired standalone
   scarcity mod is absent; launch and run `/reload` without a relevant codec,
   Mixin, or compatibility error.
2. Craft Crystal Heart as `ddd/ded/ddd`, with eight Diamonds and one center Echo
   Shard, and verify the exact component-qualified Matcha output.
3. Craft Sculk Sensor as `v v/sss` and Sculk Shrieker as `b b/v v/sss`; confirm
   neither recipe accepts an Echo Shard.
4. Break Sculk Sensor, Calibrated Sculk Sensor, and Sculk Shrieker with and
   without Silk Touch; confirm Silk-Touch-only block drops, no Echo Shards, and
   unchanged applicable XP behavior.
5. Sanity-check Deep Dark fishing for no obvious Echo Shard result without
   treating a small sample as statistical proof; retain Ancient City as the
   unchanged Echo Shard loot source.
6. Craft Reinforced Heart as `EEE/EHE/ E ` using six Echo Shards and one
   semantic Matcha Crystal Heart; confirm its accepted sprite, output identity,
   recipe-book entry, and optional JEI exposure.
7. Move, craft, shift-click, and store a Crystal Heart without applying it.
   Main-hand use must add exactly one maximum heart only from ten through
   twenty-nine hearts and refuse without consumption below ten or at thirty.
8. Main-hand Reinforced use below ten hearts must restore exactly one maximum
   heart and refuse without consumption at or above ten.
9. Verify deaths at representative values above, at, and below ten hearts each
   remove exactly one maximum heart and repeated deaths stop at five hearts.
10. Verify the maximum-health state persists through save/reload or reconnect,
    and confirm Matcha keepInventory and unrelated Matcha behavior remain
    unchanged.

Stop and record a failure or inconclusive result for any known recipe still
losing precedence, reload failure, incorrect semantic item match, inventory
auto-use, wrong health delta or bound, persistence loss, unexpected shard
source, broad loot/function interception, duplicate mod ownership, or unrelated
Matcha regression.

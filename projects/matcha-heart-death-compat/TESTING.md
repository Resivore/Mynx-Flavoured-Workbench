# Testing

## Successor-only gate

Exact Canary 7 (`matcha-heart-death-compat-0.1.6-canary7.jar`, SHA-256
`3B1F608C6F4B3B92F764A3781A9652DC11F089590A9173444C067975D01CA2DD`)
is a closed failed candidate in runtime Slot A. Its Sculk Sensor and Sculk
Shrieker Echo Shard drop removals and Reinforced Crystal Heart recipe passed,
but Matcha still won the Crystal Heart, Sculk Sensor, and Sculk Shrieker
recipes. Do not redeploy, promote, or reclassify unchanged C7.

Exact Canary 5 remains the accepted disabled rollback. No C8 exists. Run this
procedure only after a separately authorized narrow recipe-precedence fix
produces a new exact successor artifact with a new version, source commit, and
SHA-256. Update Slot A only; preserve Slot B and the accepted baseline.

## Successor preflight

1. Confirm the successor replaces C7 in Slot A and that C5 remains the single
   disabled accepted rollback; no retired scarcity mod or second enabled Heart
   candidate may be present.
2. Use the V2 physical manager's dry-run and apply paths, then require its
   focused readiness verification before launching Minecraft.
3. Record the exact successor identity and only behavior actually observed.
   Build, static tests, deployment, and readiness are not runtime results.

## Focused successor matrix

1. Launch Minecraft Java 26.2 with the cumulative accepted stack and unchanged
   Mossy Slot B, then run `/reload` without a relevant recipe, codec, Mixin,
   loot, or compatibility error.
2. Craft Crystal Heart as `ddd/ded/ddd`, with eight Diamonds and one center
   Echo Shard, and verify the exact component-qualified Matcha output.
3. Craft Sculk Sensor as `v v/sss` and Sculk Shrieker as `b b/v v/sss`;
   confirm neither recipe accepts an Echo Shard and Matcha no longer wins either
   effective recipe.
4. Break Sculk Sensor, Calibrated Sculk Sensor, and Sculk Shrieker with and
   without Silk Touch; confirm Silk-Touch-only block drops, no Echo Shards, and
   unchanged applicable XP behavior.
5. Craft Reinforced Heart as `EEE/EHE/ E ` using six Echo Shards and one
   semantic Matcha Crystal Heart; confirm its accepted sprite, output identity,
   recipe-book entry, and optional JEI exposure.
6. Move, craft, shift-click, and store a Crystal Heart without applying it.
   Main-hand use must add exactly one maximum heart only from ten through
   twenty-nine hearts and refuse without consumption below ten or at thirty.
7. Main-hand Reinforced use below ten hearts must restore exactly one maximum
   heart and refuse without consumption at or above ten.
8. Verify deaths at representative values above, at, and below ten hearts each
   remove exactly one maximum heart; repeated deaths must stop at five hearts.
9. Save/reload or reconnect and confirm maximum-health persistence, Matcha
   keepInventory, Ancient City Echo Shard availability, and unrelated Matcha
   behavior remain unchanged.

Stop and record `FAIL` or `INCONCLUSIVE` for any recipe still losing
precedence, reload failure, wrong semantic item match, inventory auto-use,
incorrect health delta or bound, persistence loss, unexpected shard source,
broad interception, duplicate mod ownership, or unrelated Matcha regression.

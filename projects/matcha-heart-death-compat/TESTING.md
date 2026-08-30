# Testing

## Canary 8 combined-session gate

Test only exact Canary 8:

- version `0.1.7-canary8`
- file `matcha-heart-death-compat-0.1.7-canary8.jar`
- SHA-256 `CB36D6917DC61B17F2D8B5CEA0D09CB4C1455E04AACA2C186ECAEBE5E9960AA2`
- source `4707b35150cc4169b2eee68b8683f9dfb27c158e`

Canary 7 is a closed failed candidate. Its Heart/death mechanics, Sensor and
Shrieker Echo Shard drop removal, and Reinforced Crystal Heart recipe passed,
but Matcha still won the Crystal Heart, Sculk Sensor, and Sculk Shrieker
recipes. Do not redeploy, promote, or reclassify C7. Exact Canary 5 remains the
disabled accepted rollback.

Exact C8 is installed in Slot A alongside exact Mossy Stone Canary 3 in Slot B,
and final V2 manager re-verification passed. If managed state changes before
launch, run a live physical reverify and require it to pass again. Preserve the
accepted baseline and do not enable a second Heart or retired Echo-scarcity
artifact. Build, static tests, deployment, and readiness are not Minecraft
runtime results.

## Focused Canary 8 matrix

1. Launch Minecraft Java 26.2 with the final combined state and run `/reload`.
   Confirm there is no relevant recipe, codec, Mixin, loot, or compatibility
   error and that accepted Dramatic Doors recipes remain available.
2. Craft Crystal Heart as `ddd/ded/ddd` using eight Diamonds and one center
   Echo Shard. Confirm the output is Matcha's exact semantic/component-qualified
   Crystal Heart, not an ordinary Poisonous Potato.
3. Craft Sculk Sensor as `v v/sss` and Sculk Shrieker as `b b/v v/sss`.
   Confirm neither recipe accepts an Echo Shard and Matcha no longer wins either
   effective recipe.
4. Craft Reinforced Heart as `EEE/EHE/ E ` using six Echo Shards and one
   semantic Matcha Crystal Heart. Confirm its accepted sprite, output identity,
   recipe-book entry, and optional JEI exposure.
5. Break Sculk Sensor, Calibrated Sculk Sensor, and Sculk Shrieker with and
   without Silk Touch. Confirm Silk-Touch-only block drops, no Echo Shards, and
   unchanged applicable XP behavior.
6. Move, craft, shift-click, and store a Crystal Heart without applying it.
   Main-hand use must add exactly one maximum heart only from ten through
   twenty-nine hearts and refuse without consumption below ten or at thirty.
   Main-hand Reinforced use below ten hearts must restore exactly one maximum
   heart and refuse without consumption at or above ten.
7. Verify deaths at representative values above, at, and below ten hearts each
   remove exactly one maximum heart and stop at five hearts. Save/reload or
   reconnect and confirm persistence, Matcha keepInventory, Ancient City Echo
   Shard availability, and unrelated Matcha behavior remain unchanged.

Record Slot A independently as `PASS`, `FAIL`, or `INCONCLUSIVE`, even if Mossy
Stone Canary 3 in Slot B has a different result during the same launch. Stop
and record `FAIL` or `INCONCLUSIVE` for any recipe still losing precedence,
reload failure, wrong semantic item, inventory auto-use, incorrect health delta
or bound, persistence loss, unexpected shard source, missing Dramatic Doors
recipes, broad interception, duplicate ownership, or unrelated Matcha
regression.

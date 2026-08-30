# Testing

## Accepted Canary 10 regression baseline

The physically verified Stack v3 baseline contains only this exact Matcha Death
Rebalance release:

- version `0.1.9-canary10`
- file `matcha-heart-death-compat-0.1.9-canary10.jar`
- size `34,751` bytes
- SHA-256 `F86442EC69ED8AFB86D52C97DB2E899223C25A659EF7C19F245E51AC642BBC90`
- release source `f9c877d14956616bc3f46f3239ac3b512e1ee9c4`

The user explicitly authorized `USER_APPROVED_UNTESTED_PROMOTION` after the
focused Java 25 / Gradle 9.5.1 offline clean test/build passed 5/5. The broader
suite compiled and passed 32 tests, but its eight overlay-reference tests could
not run because the untracked Matcha ZIP was absent; this is not a 40/40 pass.
Exact C10 remains `RUNTIME_UNTESTED`. Baseline acceptance and physical
verification are not Minecraft runtime evidence.

Exact Canary 8 (`0.1.7-canary8`, 31,176 bytes, SHA-256
`CB36D6917DC61B17F2D8B5CEA0D09CB4C1455E04AACA2C186ECAEBE5E9960AA2`,
source `4707b35150cc4169b2eee68b8683f9dfb27c158e`) retains the user's
aggregate PASS for all intended behavior and is the closest exact runtime-passed
rollback. That historical PASS must never be inherited by C10.

C10 changes C9 only by setting
`DataComponents.ENCHANTMENT_GLINT_OVERRIDE=true` on Resonant Favour's default
`Item.Properties`. It adds no enchantment and changes no recipe, model, texture,
Heart, death/recovery, scarcity, loot, advancement, function, or compatibility
behavior.

## Focused Canary 10 regression matrix

1. Launch Minecraft Java 26.2 with the final combined state and run `/reload`.
   Confirm there is no relevant recipe, codec, Mixin, loot, or compatibility
   error and accepted Dramatic Doors recipes remain available.
2. In Creative Ingredients/search, confirm one ordinary item named Resonant
   Favour with the supplied cyan sprite and ID
   `matcha_heart_death_compat:resonant_favour`. Confirm both the default stack
   and a crafted stack show the enchantment glint while remaining unenchanted.
3. Craft Resonant Favour as ` e /ede/ e ` using four Echo Shards around Divine
   Favour (`minecraft:nether_star`). Then craft Reinforced Crystal Heart as
   `e e/ere/ e ` using five Echo Shards and the center Resonant Favour.
4. Craft Crystal Heart as `f f/fdf/ f ` using five Divine Fragments
   (`minecraft:turtle_scute`) and center Divine Favour. Confirm each relevant
   output has exactly one effective recipe and none of the superseded C8
   Crystal/Reinforced layouts remains available.
5. Craft Sculk Sensor as `v v/sss` and Sculk Shrieker as `b b/v v/sss`.
   Confirm neither accepts an Echo Shard and Matcha does not win either recipe.
6. Break Sculk Sensor, Calibrated Sculk Sensor, and Sculk Shrieker with and
   without Silk Touch. Confirm Silk-Touch-only block drops, no Echo Shards, and
   unchanged applicable XP behavior.
7. Move, craft, shift-click, and store a Crystal Heart without applying it.
   Main-hand Crystal use must add exactly one maximum heart only from ten
   through twenty-nine hearts and refuse without consumption below ten or at
   thirty. Main-hand Reinforced use below ten hearts must restore exactly one
   maximum heart and refuse without consumption at or above ten.
8. Verify representative deaths above, at, and below ten hearts each remove
   exactly one maximum heart and stop at five. Save/reload or reconnect and
   confirm persistence, Matcha keepInventory, Ancient City Echo Shard
   availability, and unrelated Matcha behavior remain unchanged.

Use this matrix for the next C10 regression session. Record C10 independently as
`PASS`, `FAIL`, or `INCONCLUSIVE`; do not infer its result from C8, Stack v3
acceptance, static checks, or another project tested in the same launch.

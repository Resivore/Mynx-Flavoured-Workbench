# Testing

## Canary 9 combined-session gate

Test only exact Canary 9:

- version `0.1.8-canary9`
- file `matcha-heart-death-compat-0.1.8-canary9.jar`
- SHA-256 `099E7E1D0F8EB5B5A060D029BA89D7328A9CB9D0AF50487B2352BCBCA45A49E8`
- source `f789b290f72c508a563797b0c543125bc9c468c1`

Exact Canary 8 passed all intended Matcha Death Rebalance runtime behavior and
is preserved as historical evidence; it was not promoted. Exact Canary 5
remains the disabled accepted rollback in Stack v1. C9 changes only the new
Resonant Favour component and requested Crystal/Reinforced recipe progression.

Exact C9 is installed in Slot A alongside exact Mossy Stone Canary 4 in Slot B,
and final manager physical/title re-verification passed. If managed state
changes before launch, require the normal live verifier to pass again. Build,
static tests, deployment, and readiness are not Minecraft runtime results.

## Focused Canary 9 matrix

1. Launch Minecraft Java 26.2 with the final combined state and run `/reload`.
   Confirm there is no relevant recipe, codec, Mixin, loot, or compatibility
   error and accepted Dramatic Doors recipes remain available.
2. In Creative Ingredients/search, confirm one ordinary item named Resonant
   Favour with the supplied cyan sprite and ID
   `matcha_heart_death_compat:resonant_favour`.
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

Record Slot A independently as `PASS`, `FAIL`, or `INCONCLUSIVE`, even if Mossy
Stone Canary 4 has a different result during the same launch. Do not promote C9
without its explicit runtime result.

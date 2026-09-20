# Testing — Canary 2

Test only the exact release below with the unchanged upstream Enderscape input.

| Field | Value |
|---|---|
| Companion artifact | `enderscape-pruning-0.1.0-canary2.jar` |
| Version | `0.1.0-canary2` |
| Companion SHA-256 | `1da6a13cbd5a0027159a45d66cff89261aa5dd53cd9c438a5c3a1b17f4a0039b` |
| Built at | `2026-09-20T01:52:05.0944502Z` |
| Implementation checkpoint | `cb0707e9af08f230280dd7f73994153443233924` |
| Required upstream input | `enderscape-fabric-3.0.2+mc26.2.jar` — SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b` |
| Required Matcha provider | `matcha-flavoured-data-0.1.0-canary2.jar` — SHA-256 `0850c7ff0680cf42436cd2b72055af77970075cf438ff429bdb55723af444639` |

Canary 2 has controlled build/static evidence only. It is `RUNTIME_UNTESTED`.
Do not use a protected or retired gameplay profile. The intended target world has
never loaded the End, so this procedure validates the approved new-game design;
it does not make historical End inventories or generated chunks a release gate.

## 1. Startup and retained systems

1. Start a disposable Minecraft 26.2 Fabric instance with the exact upstream
   Enderscape jar, Matcha Flavoured Data, and C1. Confirm server/world startup
   and later reconnect produce no missing registry, data-pack, loot-table,
   recipe, advancement, or mixin errors.
2. Enter and generate the End. Visit representative retained Enderscape biomes
   and End City, Stronghold, End Haven, and Mirestone Ruins structures. Confirm
   their ordinary blocks, entities, structures, and world generation remain.
3. Verify Magnia Fields, Alluring/Repulsive Magnia, sprouts, field behavior,
   polarity/power behavior, and Magnia Radio remain upstream. The Radio remains
   Creative/command-only; it gains no C1 survival route.
4. Brew and use representative Low Gravity, Void Purification, Void Resistance,
   and Void Corruption normal/splash/lingering/tipped-arrow forms. Check the
   Void effect loop and totem behavior are unchanged.

## 2. Suppression and discovery

1. Check crafting, recipe-book unlocks, Enderscape tabs, vanilla tabs, Creative
   Search, and JEI when installed. Shadoline Helmet/Chestplate/Leggings/Boots,
   Dagger, all four Rubble Shield variants, and default plus all dyed Mirror
   variants must not appear through ordinary discovery.
2. Confirm the Shadoline armor recycling recipes, Dagger recipe, four Shield
   recipes, and Mirror dyeing recipe are unavailable. Confirm the candidate
   recipe unlocks, `stun_attack`, `rubble_shield_dash`, and the Mirror
   teleport/long-distance/Transdimensional advancement chain do not surface.
3. Open representative Mirestone Ruins, End City, Stronghold, and supplement
   loot. Dagger and Mirror must never result from their former entries; the
   remaining rewards must retain their original apparent roll frequency/count
   behavior rather than receiving the removed entries' probability.
4. Confirm Bundling, Stun Burst, and Transdimensional are absent from normal
   enchanting, enchanted-book loot, Creative Search, and JEI. Confirm Rebound
   remains ordinary and Resonance can be newly applied to a Magnia Attractor
   but not to a newly acquired Mirror or Dagger.

## 3. Canary 2 pruning and crafting

1. Kill representative Rubblemites, including with Looting where practical. The existing 65% player-kill reward chance and count/Looting behavior must yield `enderscape:nebulite_shards`; Rubble Chitin must never drop. Confirm unrelated Rubblemite behavior and rewards remain upstream.
2. Check Enderscape tabs, vanilla tabs, Creative Search, JEI when installed, recipe-book unlocks, normal loot, and normal acquisition routes. Rubble Chitin must be absent. Existing legacy stacks may continue to resolve; do not treat their retained registry identity or void-immunity tag as a new acquisition path.
3. With the exact Matcha C2 Kindling (the component-bearing Beacon Kindling item), craft a Void Campfire using exactly one Kindling and one `enderscape:void_shale` in either shapeless arrangement. The output must be exactly one `enderscape:void_campfire`.
4. Verify that a vanilla `minecraft:stick` plus Void Shale, ordinary chicken spawn eggs plus Void Shale, extra ingredients, and the former shaped logs/sticks/Void Shale recipe do not craft a Void Campfire.
5. Let natural Veiled Leaves decay repeatedly and break representative leaves without shears or Silk Touch. Veiled Saplings must never result from either passive leaf path. Break leaves with shears and Silk Touch to confirm direct Veiled Leaves acquisition remains available.
6. Craft exactly one Veiled Leaves into exactly one Veiled Sapling in either shapeless arrangement. Confirm no four-to-one or alternate passive acquisition route is introduced.

## 4. Matcha food behavior

1. In the audited Enderscape loot routes, inspect complete component maps and
   quantities for Bread, Carrot, Golden Carrot, Golden Apple, Enchanted Golden
   Apple, and Chorus Fruit. They must use current Matcha canonical identities
   without altered pool rolls, weights, conditions, or count ranges.
2. Confirm structure-loot Cod remains its Enderscape form unless a later
   Matcha authority explicitly changes that rule. Potato, Poisonous Potato,
   Honey Bottle, and exact Suspicious Stew variants remain unaltered.
3. Consume Drift Jelly Bottle, Puruberry, and Murublight Bracket at full hunger.
   Each must provide exactly one Matcha heart with no vanilla hunger benefit,
   while retaining its Low Gravity/Void effect and its upstream remainder,
   placement, plant/drop, Fortune, mob-food, or Rustle behavior as applicable.
4. Eat each of the seven Chorus Cake Roll bites at full hunger. Each bite must
   provide exactly two Matcha hearts and no vanilla hunger; bite state,
   comparator behavior, sounds/particles, final teleport, and final removal
   must remain upstream.

## 5. Reload and evidence

1. Run `/reload`, disconnect/reconnect, and reopen Creative Search and JEI.
   Require no duplicate or stale Matcha food identities and no hidden candidate
   variant leak.
2. Repeat the C2 Rubblemite, Void Campfire, and Veiled Leaves checks after reload. Preserve logs and the exact release identifiers above with every observation.
   Record only observed behavior. Any registry/data/loot/advancement error,
   changed loot math, wrong Matcha component identity, unexpected hunger,
   lost Void/Magnia behavior, or discovery leak is a failure.

No Minecraft runtime observation has been supplied for C1 or C2 yet.

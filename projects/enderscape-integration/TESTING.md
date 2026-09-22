# Testing — Enderscape Integration Canary 4

Test only the exact release below with the unchanged upstream Enderscape input.

| Field | Value |
|---|---|
| Companion artifact | `enderscape-integration-0.1.0-canary4.jar` |
| Version | `0.1.0-canary4` |
| Companion SHA-256 | `20ac7360850f6b62335d333e9328acc2475b248f74c0b4826ed8aadf283bab30` |
| Built at | `2026-09-22T01:23:24.3593451Z` |
| Implementation checkpoint | `7fd96db9504606ca65ee8b3ed21583987646b175` |
| Required upstream input | `enderscape-fabric-3.0.2+mc26.2.jar` — SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b` |
| Required Matcha provider | `matcha-flavoured-data-0.1.0-canary2.jar` — SHA-256 `0850c7ff0680cf42436cd2b72055af77970075cf438ff429bdb55723af444639` |

Canary 4 has controlled build/static evidence only. It is `RUNTIME_UNTESTED`.
Do not use a protected or retired gameplay profile. The intended target world has
never loaded the End, so this procedure validates the approved new-game design;
it does not make historical End inventories or generated chunks a release gate.

## 1. Startup and retained systems

1. Start a disposable Minecraft 26.2 Fabric instance with the exact upstream
   Enderscape jar, Matcha Flavoured Data, and this exact Canary 4 companion. Confirm server/world startup
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

## 3. Retained Canary 2 pruning and crafting

1. Kill representative Rubblemites, including with Looting where practical. The existing 65% player-kill reward chance and count/Looting behavior must yield `enderscape:nebulite_shards`; Rubble Chitin must never drop. Confirm unrelated Rubblemite behavior and rewards remain upstream.
2. Check Enderscape tabs, vanilla tabs, Creative Search, JEI when installed, recipe-book unlocks, normal loot, and normal acquisition routes. Rubble Chitin must be absent. Existing legacy stacks may continue to resolve; do not treat their retained registry identity or void-immunity tag as a new acquisition path.
3. With the exact Matcha C2 Kindling (the component-bearing Beacon Kindling item), craft a Void Campfire using exactly one Kindling and one `enderscape:void_shale` in either shapeless arrangement. The output must be exactly one `enderscape:void_campfire`.
4. Verify that a vanilla `minecraft:stick` plus Void Shale, ordinary chicken spawn eggs plus Void Shale, extra ingredients, and the former shaped logs/sticks/Void Shale recipe do not craft a Void Campfire.
5. Let natural Veiled Leaves decay repeatedly and break representative leaves without shears or Silk Touch. Veiled Saplings must never result from either passive leaf path. Break leaves with shears and Silk Touch to confirm direct Veiled Leaves acquisition remains available.
6. Craft exactly one Veiled Leaves into exactly one Veiled Sapling in either shapeless arrangement. Confirm no four-to-one or alternate passive acquisition route is introduced.

## 4. Retained Canary 3 JEI and English presentation

1. With JEI installed, open the normal crafting presentation for Void Campfire. It must appear exactly once with one component-bearing Matcha Beacon Kindling (`minecraft:chicken_spawn_egg` with `minecraft:item_model=minecraft:beacon_kindling`) plus one `enderscape:void_shale`, producing exactly one `enderscape:void_campfire`.
2. Confirm JEI does not expose a plain Chicken Spawn Egg or `minecraft:stick` as an equivalent input, the former upstream shaped Void Campfire recipe, or any second compatibility-created Void Campfire recipe.
3. Confirm the ordinary one-to-one Veiled Leaves to Veiled Sapling shapeless recipe appears exactly once. Recheck all C1/C2 blocked recipes and suppressed item/enchantment variants; none may reappear.
4. Run `/reload`, wait for recipe synchronization to complete, and reopen JEI. Repeat the preceding checks with no stale or duplicate recipe presentation. Disconnect/reconnect and repeat once more.
5. Repeat startup and crafting checks with JEI removed. Common/server initialization, pruning, and both effective recipes must remain functional; a missing JEI warning or hard dependency is a failure.
6. In English, verify `Veiled End Stone`, `Celestial Overgrowth`, and `Corrupt Overgrowth` now display as `Veiled Nullium`, `Celestial Nullium`, and `Corrupted Nullium`. Confirm `Celestial Path` and `Corrupt Path` remain unchanged, and sample the blocks to ensure behavior, world generation, models, textures, CTM, and BGE relationships remain upstream.

## 5. Optional Canary 4 Veiled Leaves shader-wind matrix

1. In the same disposable world, place canonical `enderscape:veiled_leaves` beside ordinary vanilla Oak Leaves. With Iris absent or shaders disabled, confirm both blocks retain their normal non-shader rendering and that C1-C3 discovery, loot, recipe, food, JEI, localization, and ordinary Veiled Leaves behavior remain unaffected.
2. Enable the exact retained Complementary Unbound r5.8.1 setup. Use the vanilla Oak Leaves beside canonical Veiled Leaves as the animation reference: Veiled Leaves must receive the same appropriate foliage/leaf wind treatment. Confirm their existing texture, UV layout, biome/model tint, cutout/render layer, waterlogging, distance, persistent, and decay behavior remain unchanged.
3. With BGE × Complementary installed, compare canonical Veiled Leaves with representative BGE geometries whose authoritative canonical material is Veiled Leaves. Include a full-height or solid-looking role and, where the current BGE catalog exposes them, Layer, Slab, Wall, Corner, and Quarter Column roles. Each geometry must retain its actual shape, texture/UVs, tint, state projection, and render behavior while receiving the same foliage animation through canonical inheritance; do not accept an individual derived-block override as evidence.
4. If the active shader pack supplies an explicit physical/canonical Veiled Leaves mapping, repeat the comparison and confirm that pack-owned assignment wins rather than this fallback. Check that only unmapped Veiled Leaves states use the vanilla-leaf reference.
5. Reload shaders, then run `/reload`, leave/rejoin the world, and fully restart the client. Recheck canonical and representative BGE geometry animation, tint, texture, state-specific geometry, and absence of duplicate/stale material behavior after each boundary.
6. Remove Iris and all shaders for the no-Iris control. Start, join, reload, leave/rejoin, and restart once. Common/server Enderscape Integration behavior—including the retained recipes and pruning/adjustment rules—must work normally with no Iris requirement, linkage error, or changed ordinary Veiled Leaves behavior.

## 6. Matcha food behavior

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

## 7. Reload and evidence

1. Run `/reload`, disconnect/reconnect, and reopen Creative Search and JEI.
   Require no duplicate or stale Matcha food identities and no hidden candidate
   variant leak.
2. Repeat the retained C2 Rubblemite, Void Campfire, and Veiled Leaves checks plus every retained C3 JEI/localization and C4 shader-wind check after reload. Preserve logs and the exact release identifiers above with every observation.
   Record only observed behavior. Any registry/data/loot/advancement error,
   changed loot math, wrong Matcha component identity, unexpected hunger,
   lost Void/Magnia behavior, or discovery leak is a failure.

No Minecraft, Iris, Complementary, BGE, JEI, or gameplay runtime observation has been supplied for C1-C4 yet.

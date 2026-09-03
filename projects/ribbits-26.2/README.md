# Mynx Ribbits 26.2

This directory maintains the legally trackable source, build, test, and private-assembly tooling for the private Minecraft Java 26.2 downstream Ribbits build used by the fixed Mynx/Matcha stack. Upstream behavior remains the default except for the intentional Mynx changes recorded in `MYNX_DEVIATIONS.md`.

Upstream code is covered by LGPL-3.0. Ribbits resources and the approved Guard Ribbits and Useful Ribbits donor visuals retain their original rights status. Protected resources, donor-derived outputs, private manifests/reports, pristine or donor JARs, and the complete private candidate are ignored and must not be published or redistributed.

## Current candidate

- Internal version: `4.1.6+26.2-mynx-canary4`
- Private artifact: `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary4.jar`, 3,219,246 bytes, SHA-256 `DA7A37E0AEECE4D7633F0C5104298D76E066CD38DD5255FAE616DF95C5FBC712`
- Source-only artifact: `ribbits-source-only-4.1.6+26.2-mynx-canary4.jar`, 1,159,347 bytes, SHA-256 `FE9E03AA72964CEB3EB3851EF485BAD50444AED00F6B8F1924DFFD0287CE01E4`
- State: `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`
- The exact implementation checkpoint is recorded in `WORKBENCH_STATUS.json` by the administrative checkpoint after the implementation commit is frozen.

Canary 4 succeeds, but does not overwrite or relabel, Canary 3: version `4.1.6+26.2-mynx-canary3`, private artifact `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary3.jar`, 3,204,738 bytes, SHA-256 `69AD105B44E3E9131D540E26887EB09CE6BE1DA89EE9C4B3C49E4E95341F3E19`, implementation checkpoint `fc983c0534ac2a73308111d2052530a202156777`. Canary 2 and the exact faithful-port baseline also remain historical at their recorded identities.

No Minecraft client or server was launched and no candidate was deployed for this task. Static compilation, tests, deterministic assembly, archive inspection, and codec validation are not runtime evidence. The current useful future runtime procedure is in `TESTING.md`.

## Permanent items and temporary visuals

Canary 3 introduced, and Canary 4 preserves, two permanent, mechanically distinct Ribbits items:

- `ribbits:glowcap` is the stack-64, noncraftable currency used by all monetary Ribbit offers and the intended private village-chest currency entries. Its item definition temporarily renders through `minecraft:item/warped_fungus`. It is not `minecraft:warped_fungus`, is not a block item, and receives none of Warped Fungus's placement, composting, tags, recipes, or other behavior.
- `ribbits:toadstool_heart` is a stack-64 ordinary item, the exact recipe output, and the only home-setting item. Its item definition temporarily renders through `minecraft:item/heart_container`, the model used by the original Crystal Heart. It receives none of the Crystal Heart's components, health effects, consumption behavior, attributes, rarity, glint, or progression behavior.

Both items appear exactly once in the Ribbits creative tab and translate to `Glowcap` and `Toadstool Heart`. The model references are temporary visuals, not the approved final art. A later asset-only successor can replace them without changing registry IDs, recipes, trades, loot, saved state, or behavior; no migration-only item ID or recipe is introduced here. No vanilla or protected Matcha texture PNG is copied into tracked source.

The shaped Toadstool Heart recipe is `T T` / `THT` / ` T ` and yields exactly one `ribbits:toadstool_heart`. `T` is exactly five `ribbits:toadstool` small foliage mushrooms, never `ribbits:red_toadstool`, `ribbits:brown_toadstool`, `ribbits:toadstool_stem`, or another large building block. `H` is the original component-bearing Crystal Heart: base `minecraft:poisonous_potato`, item model `minecraft:heart_container`, translated item-name component `item.kleispack.crystal_heart`, rare rarity, enchantment glint override, and no consumable component. The narrow component-aware recipe rejects an ordinary poisonous potato and the epic, differently modeled Reinforced Crystal Heart.

Crouch/secondary-use of a Toadstool Heart on any Ribbit sets its saved `HomePosX/Y/Z` to that Ribbit's current block position, emits the existing heart-particle confirmation, and consumes exactly one item outside Creative instabuild. Amethyst shards no longer invoke or consume through the old home-setting path. The existing home-navigation goals remain otherwise unchanged and home is not made dimension-aware.

## Tiered Ribbit economy

Ribbits remain normal `Merchant` entities using `MerchantOffer`, `MerchantOffers`, the existing screen, serialized offers, use limits, and merchant progress. They are not converted into vanilla Villagers and gain no POIs, workstations, gossip, villager brains, schedules, profession acquisition, or breeding changes.

Tier thresholds are 0, 10, 30, 60, and 100 profession XP. Ordinary Tier 1–4 offers award 1, 2, 3, and 4 XP respectively; offers at a profession's maximum tier award zero. Lower unlocked tiers remain cumulative and keep their tier reward until maximum rank. All designed barter offers use a zero price multiplier, so demand does not change approved rates. Normal tier unlocks add their offers immediately. Tiered professions show merchant progress; Musician is unranked and does not.

The saved rank and XP always use those exact Mynx thresholds. The merchant packet maps progress proportionally onto Minecraft's fixed Villager progress-bar breakpoints, and maps five-rank professions' final display to the fourth visible bar segment so the normal merchant screen continues to show a full progress bar instead of hiding it at vanilla level 5; this is a display-only adapter and does not alter progression or saved XP.

| Profession | Maximum | Exact displayed ranks | Maximum materialized offers |
| --- | ---: | --- | ---: |
| Gardener | 2 | Sprout Tender; Toadstool Keeper | 5 |
| Farmer | 3 | Vine Puller; Root Wrangler; Mudfield Steward | 5 |
| Fisherman | 5 | Pond Forager; Coral Keeper; Amphibian Attendant; Opal Angler; Monument Mariner | 8 |
| Merchant | 3 | Moss Peddler; Lantern Trader; Glowgoods Baron | 5 |
| Chef | 5 | Tadpole Cook; Pond Cook; Swamp Chef; Grand Chef; Master of the Feast | 7 |
| Sorcerer | 4 | Wart Whisperer; Gatecaller; Flask Sage; Deep-Pond Oracle | 20 |
| Prospector | 3 | Pebble Picker; Vein-Seeker; Deep Delver | 16 |
| Guard | 4 | Pond Sentry; Lily Warden; Marsh Marshal; Bulwark of the Bog | 11 weapons / 12 mount / 15 armor |
| Musician | unranked | Musician | 1 |

Player-assigned custom entity names remain intact. Rank, XP, progression-gate completion, profession choices, Chef menu state, and daily restock state are saved with safe defaults when absent. There is deliberately no generalized migration of old serialized offer schemas; Canary 4 recognizes only the exact saved Canary 3 Sorcerer offer shapes needed to insert its one new service without resetting existing offers or uses.

Persistent equal-weight choices are one linked Gardener Tier-1 pair; Farmer Tier-2 and Tier-3 choices; Fisherman aquatic material and one linked coral family; Chef Master specialty; Sorcerer Blessing; Prospector bullion; and one Guard branch. These choices do not reroll on menu open, restock, rank advancement, chunk reload, save/reload, restart, or dimension travel. Fisherman's first exact Opal buyback gates promotion from Opal Angler to Monument Mariner even at 100 ordinary XP. Sorcerer's first exact Benzene buyback immediately gates promotion to Gatecaller. Both gate offers remain available afterward.

Chef permanently offers Glow Berry Crumble and Honey Ginger Tea. For every unlocked Tier 1–4, it materializes exactly one deterministic daily special for that Chef, tier, and overworld day. The selection is stable through ordinary restocks, menu reopen, chunk reload, and restart and cycles through the tier pool before repeating where practical. A tier unlocked mid-day receives one special immediately without changing lower-tier choices. A day boundary rotates all eligible tiers; if a trading session is open, replacement waits until that session closes. The Tier-5 Master specialty is one permanent equal-weight choice among Japanese Curry, Green Curry, and Tonkotsu Ramen and never rotates.

The strict stock system persists the current overworld day, restocks used that day, and last restock game time. A new day supplies fresh initial stock and resets the allowance. An exhausted offer is required for either ordinary restock; at most two ordinary restocks occur per day and at least 2,400 ticks separate them. Restocking refreshes current offers without rerolling choices or Chef menus. Unload, relog, restart, dimension changes, and backward time changes cannot reset or bypass the allowance. The intended maximum is initial stock plus two restocks, with no workstation, POI, schedule, home, or work window.

## Phase C swamp huts

Every newly generated vanilla `minecraft:swamp_hut`, including `/place structure`, uses its normal procedural `SwampHutPiece` and persisted one-shot flags but creates one persistent Sorcerer instead of the initial Witch. The Sorcerer is transformed from local `(3,2,5)`, finalized with spawn reason `STRUCTURE`, assigned `ribbits:sorcerer`, starts as Wart Whisperer with normal Phase B data and buff goal, and uses its spawn position as home. The independent initial Cat remains a black hut Cat and moves only from local `(2,2,5)` to `(2,2,4)`. A locally north-facing closed barrel is added at `(2,2,6)`, immediately left of the unchanged crafting table `(3,2,6)` and cauldron `(4,2,6)`; normal piece transforms handle every orientation.

Natural Witch candidates are rejected only when the exact registered `minecraft:swamp_hut` has a valid containing `SwampHutPiece` whose inclusive bounding box contains the candidate. The procedural local boundary is `x=0..6`, `y=0..6`, `z=0..8`, including empty upper layers but excluding support columns below it and every position immediately outside. Other monsters, existing Witches, and structure-, command-, egg-, dispenser-, spawner-, trial-spawner-, raid-, conversion-, and event-created Witches remain untouched. Missing or malformed structure metadata does not suppress broadly.

Existing huts are not retrofitted: no Sorcerer, barrel, Cat relocation, or saved-Witch conversion occurs. Killing the generated Sorcerer does not recreate it. Because the normal vanilla `Witch` one-shot flag remains consumed, removing Ribbits later can leave a Phase C-generated hut without its initial Witch.

## Explorer-map barrel and redemption

The barrel receives `ribbits:chests/swamp_hut_map` once and lazily resolves exactly one result. That loot table invokes shared item modifier `ribbits:ribbit_village_explorer_result`, whose dedicated destination tag `#ribbits:on_ribbit_village_explorer_maps` contains exactly `ribbits:ribbit_village`. Minecraft 26.2 serializes that tag key without the conceptual leading `#` in JSON, like its vanilla cartographer data. From one vanilla empty map, the vanilla exploration-map function uses plains-village decoration, raw structure-placement radius `100`, zoom `2`, and `skip_existing_chunks=false`.

A hit is exactly one filled map with a valid map ID and translated name `Ribbit Village Explorer Map`. The modifier's registered common-side finalizer `ribbits:finalize_ribbit_village_explorer_result` replaces every non-success shape with a fresh canonical failure, including malformed input carrying transient map-processing or unrelated components. A normal miss is therefore exactly one vanilla empty map with no map ID, translated name `Uncharted Ribbit Map`, lore `No Ribbit village could be charted.`, and exact private custom-data boolean `ribbits:failed_ribbit_village_map=true`. The stored result never reruns after inventory resolution.

Every Sorcerer at every rank has an independent `1 exact marked failed map -> 1 ribbits:toadstool_heart` service with `maxUses=16`, profession XP `0`, and price multiplier `0`. Generic or renamed maps, filled maps, and unrelated explorer maps fail because matching uses the exact marker rather than display text. Redemption does not complete or bypass the Benzene gate and cannot promote the Sorcerer. This feature uses vanilla map data plus existing Ribbits content and adds no Matcha dependency. The future Wandering Ribbit and its map sale are not implemented.

## Exact fixed-stack boundary

Cross-mod stacks are resolved server-authoritatively from the fixed Minecraft 26.2 recipe/registry contracts, not translated names. Component-bearing inputs require their exact components; component-bearing outputs copy the exact current result stack. Ordinary equipment buybacks intentionally match the underlying registered item while allowing damage, enchantments, curses, repair cost, and custom names.

Notable exact inputs include Opal as the component-bearing `minecraft:fermented_spider_eye` stack with `minecraft:opal` model and its exact colored/translatable name; Benzene from `crafting:benzene`; and Divine Fragment from `crafting:divine_fragment`. Divine Fragment is a `minecraft:turtle_scute` with rare rarity and enchantment glint. The Deep Delver Electrum offer is exactly 16 Glowcaps plus one exact Divine Fragment to one `crafting:electrum_alloy` result; a plain Turtle Scute does not match.

Prepared foods are copied from `food:glow_berry_crumble`, `food:honey_ginger_tea`, `food:pickled_carrots`, `food:rind_jam`, `food:gimmari`, `food:bokguk`, `food:golden_pickled_carrots`, `food:melon_sorbet`, `food:pumpkin_empanada`, `food:warped_pizza`, `food:warped_stroganoff`, `food:sweet_berry_danish`, `food:golden_carrot_cupcake`, `food:japanese_curry`, `food:green_curry`, and `food:ramen` (Tonkotsu Ramen). Other component/result contracts are `crafting:estus_flask`, `blessings:reach`, `blessings:silk_touch`, `crafting:carbon_rich_iron`, `crafting:bronze_alloy` (Hepatizon), `crafting:shakudo_alloy`, and `crafting:electrum_alloy`. Lush Lily Pad is `ribbits:giant_lilypad`. All sixteen catalysts are `customportals:<color>_portal_catalyst` for white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, and black.

Runtime dependencies include the coordinated YUNG's API `26.2-Fabric-6.1.1-compat.2` artifact (1,260,939 bytes, SHA-256 `FF22A6B509BA559988D7A9352DC94AC612C4B099517ACAC7DA7C81322D797ED7`) plus the fixed Custom Portals and Matcha Heart Death compatibility providers declared by the mod metadata. Where Matcha content is recipe/component-defined rather than owned by one mod ID, startup validation checks the exact required recipe results and components.

## Glowcap village loot

The private assembler changes only the five established village-currency entries from `minecraft:amethyst_shard` to `ribbits:glowcap`: `fisherman_storage`, `gardener`, `merchant`, `nitwit`, and `sorcerer`. Each entry keeps its pool, position, weight, count range, rolls, functions, conditions, and surrounding JSON structure. No quantity is rebalanced; no unrelated amethyst use changes. The merchant's unrelated amethyst-block entry remains, and the repaired `minecraft:empty` sentinels in `fisherman_main` and `sorcerer` remain valid. Protected loot tables stay exclusively inside the ignored private assembly boundary.

## Future Ribbit village utility cleanup

The private assembler now performs one exact hash-guarded deterministic transform across six protected templates at their verified pristine paths under `data/ribbits/structure/houses/`. It replaces each Sorcerer-house Brewing Stand at `(6,2,5)` with air, each `small_house_*_3` Damaged Anvil at `(3,1,5)` with air, the brown-house Smoker at `(5,1,6)` with ordinary Stone Bricks, and the red-house Blast Furnace at `(4,1,2)` with ordinary Stone Bricks. Incompatible block-entity NBT and brewing contents are removed. All other block records remain unchanged.

The complete 29-template validation requires zero remaining Brewing Stands, Damaged Anvils, Smokers, or Blast Furnaces while preserving every Barrel, Chest, private loot binding, Crafting Table, Water Cauldron, rooftop Campfire, bed, cake, counter, floor, chimney surround, and raw processor sentinel. Only future village placements change; existing generated villages are not edited.

## Preserved Phase A behavior and exclusions

Canary 4 preserves all nine registered professions, the equal-weight eight-profession natural village pool (Musician, Gardener, Fisherman, Merchant, Chef, Farmer, Prospector, Guard), natural-village exclusion of Sorcerer, every Phase B price/rank/menu/gate/restock contract, profession textures/models and umbrella variants, typed eggs, pick-block/dispenser mappings, creative-tab duplicate protection, generic behavior for the four added professions, cosmetic-only Guard equipment, private donor isolation, repaired private loot tables, and the coordinated YUNG's API concurrency fix. It adds no donor AI or Guard combat AI.

This candidate deliberately excludes a Wandering Ribbit and its map sale, automatic Sorcerer death replacement, existing-hut retrofits, conversion of saved Witches, global Witch suppression, whole-hut replacement, asynchronous map search, a new map item or GUI, direct Toadstool Heart barrel fallback, Matcha cartographer integration or Witch Hut map changes, donor profession behavior, generalized old-offer migration, dimension-aware homes, Fortune Blessing, Silver Bullion, Adamant/Netherite bullion or equipment additions, Netherite tool recycling, and unrelated Matcha or Custom Portals behavior changes.

## External Phase A report

The user reports that the Phase A profession candidate's tested functionality appeared operational: Ribbit villages generated, the expanded profession roster appeared to work, the Prospector typed spawn egg existed, and the Prospector model rendered correctly. The user did not directly observe a naturally generated Prospector in the sample. That row is therefore unobserved—not a failure and not a confirmed natural-spawn pass—and the equal-weight village pool remains unchanged. No complete runtime matrix or exact log evidence was supplied, and the available evidence does not uniquely bind the report to exact Ribbits and YUNG's API artifact identities, so it is retained as partial/unbound external evidence only.

## Bound Phase B report

The retained canonical Canary 3 and YUNG's API Compat.2 binaries rehashed exactly against authoritative `main`. The user's Phase B report therefore binds to that exact pair as a practical gameplay `PASS`, with practical use sufficient to continue. The exhaustive profession-by-profession and tier-by-tier trade matrix was intentionally not executed, so no unexecuted trade, component rejection, daily rotation, specialization, or restock edge case is marked observed. This report does not promote Canary 3 or assign a managed-slot result. Canary 4 has no runtime evidence and remains `RUNTIME_UNTESTED`.

## Build and private assembly

The public build requires the exact YUNG's API compatibility JAR via `YUNGS_API_26_2_JAR`. Leave private-resource variables unset for the deliberately non-runnable source-only archive:

```powershell
$env:YUNGS_API_26_2_JAR = 'C:\path\to\YungsApi-26.2-Fabric-6.1.1-compat.2.jar'
Remove-Item Env:RIBBITS_PRIVATE_RESOURCES_DIR -ErrorAction SilentlyContinue
Remove-Item Env:RIBBITS_PRIVATE_MANIFEST -ErrorAction SilentlyContinue
.\gradlew.bat clean test build --offline --no-daemon
```

`tools/private_resource_tools.py` remains the only tracked private assembler. Its pristine Ribbits, Minecraft, Guard Ribbits, and Useful Ribbits inputs remain immutable and external. The exact donor allowlist and ownership boundary are retained in `MYNX_DEVIATIONS.md`. For a private build, assemble and validate an ignored `test-builds/private` tree, set `RIBBITS_PRIVATE_RESOURCES_DIR` and `RIBBITS_PRIVATE_MANIFEST` to that exact eligible tree/manifest, then run the same clean offline build and validate the resulting JAR. The complete private artifact, donor-derived resources, manifests, reports, and previews remain ignored and untracked.

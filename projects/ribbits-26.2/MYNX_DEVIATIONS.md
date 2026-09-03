# Mynx Deviations

This file records intentional departures from the faithful Minecraft Java 26.2 Ribbits port. The exact faithful-port baseline is private Canary 2: version `4.1.6+26.2-port-canary2`, artifact `ribbits-private-reconstruction-4.1.6+26.2-port-canary2.jar`, 3,124,301 bytes, SHA-256 `0AD73B7B61C6EE792EC1745056563641767AFE6811C0FDF2D3C99123C3F289DC`, implementation checkpoint `efe1970d2447aea4913e67f55c0c6b83cc36c5bb`. It remains historical provenance, not an accepted or rollback release.

The direct predecessor is Mynx Canary 2: version `4.1.6+26.2-mynx-canary2`, artifact `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary2.jar`, 3,166,148 bytes, SHA-256 `587B200A52300FF57C919ED5656E357DE5840D0D05C321D6D055405382D299BD`, source checkpoint `f0be1f9c6c1f3e843a0e44791a650df237836537`. Canary 3 preserves it exactly and does not rebuild, overwrite, rename, promote, or misrepresent it.

## Canary 1 and Canary 2 deviations retained

- The private `ribbits:chests/fisherman_main` table's invalid weight-15 `minecraft:air` no-loot sentinel remains repaired as a weight-15 `minecraft:empty` entry.
- The private `ribbits:chests/sorcerer` table's invalid weight-1 `minecraft:air` sentinel remains repaired as a weight-1 `minecraft:empty` entry. Its established strong-leaping potion migration remains unchanged.
- Four visual professions remain registered without separate entity types: `ribbits:chef`, `ribbits:farmer`, `ribbits:prospector`, and `ribbits:guard`.
- Internal profession `ribbits:nitwit` retains its registry/save identity and appears to users as Musician.
- Natural villages retain one equal-weight pool of Musician, Gardener, Fisherman, Merchant, Chef, Farmer, Prospector, and Guard. `ribbits:sorcerer` remains registered and accessible through its typed egg/commands but excluded from natural villages.
- Profession-aware private models, textures, composite rain atlases, and all three umbrella variants remain. Chef, Farmer, Prospector, and Guard keep generic Ribbit behavior and `RibbitInstrumentModule.NONE`; Guard's spear/shield remain cosmetic. No donor behavior is imported.
- Typed eggs, pick-block mappings, dispenser mappings, private donor transforms, and the duplicate-checked ordered creative-tab path remain.
- The coordinated YUNG's API `26.2-Fabric-6.1.1-compat.2` keeps enhanced-beardifier pieces/junctions as reusable lists and creates fresh call-local iterators for each density evaluation, preventing shared exhausted cursor state without disabling terrain adaptation.

## Permanent currency and home item

- `ribbits:glowcap` is a permanent distinct stack-64 Ribbits item and the sole money in this candidate's Ribbit trade profiles. It has no crafting recipe and appears once in the creative tab.
- `ribbits:toadstool_heart` is a permanent distinct stack-64 Ribbits item, appears once in the creative tab, and is produced only by the approved shaped recipe.
- The two final user-authored sprites were unavailable to this remote task. Glowcap therefore temporarily references `minecraft:item/warped_fungus`; Toadstool Heart temporarily references `minecraft:item/heart_container`, the original Crystal Heart visual. No vanilla or protected texture bytes are committed.
- These references are visual-only. Glowcap does not inherit Warped Fungus placement, composting, tags, food, recipes, or block-item behavior. Toadstool Heart does not inherit Crystal Heart effects, food/consumable components, attributes, enchantments, rarity, glint, or progression behavior.
- The placeholders are not approved final art. Replacing them later is intentionally an asset-only change that must preserve both registry IDs, the recipe, every trade/loot reference, saved state, and behavior.
- The recipe is `T T` / `THT` / ` T ` and returns one Toadstool Heart. `T` is exactly five `ribbits:toadstool` foliage items and excludes red/brown block caps, stems, and other huge-mushroom building blocks.
- `H` is exact original Crystal Heart: `minecraft:poisonous_potato` with item model `minecraft:heart_container`, item-name translation `item.kleispack.crystal_heart`, rare rarity, glint override, and no consumable component. The component-aware recipe rejects ordinary poisonous potatoes and the epic Reinforced Crystal Heart.
- Crouch/secondary-use of a Toadstool Heart on any Ribbit writes the existing `HomePosX/Y/Z` at the entity's current block position, keeps the existing heart-particle confirmation, and consumes one outside Creative. Amethyst shards no longer set home or consume through that path. Navigation goals and non-dimension-aware home storage are not redesigned.

## Tiered merchant framework

Ribbits remain `Merchant` implementations and retain normal `MerchantOffer`/`MerchantOffers`, serialized offers, use limits, screen, and XP/progress display. They do not become Villagers and receive no POI, workstation, gossip, brain, schedule, profession-acquisition, or breeding systems.

The cumulative thresholds are Tier 1 at 0 XP, Tier 2 at 10, Tier 3 at 30, Tier 4 at 60, and Tier 5 at 100. Ordinary Tier 1–4 offers award 1, 2, 3, and 4 profession XP; a profession's maximum-tier offers award zero. Lower-tier offers remain and retain their tier XP until maximum rank. New tiers materialize immediately. Every designed barter has zero price multiplier. Tiered professions show progress; Musician remains unranked with no bar.

Saved progression uses those exact thresholds. A display-only adapter proportionally maps it to Minecraft's fixed merchant-bar breakpoints and keeps five-tier professions on the fourth visible segment at maximum so the vanilla screen renders a full bar rather than suppressing it at vanilla level 5; it does not change rank, earned XP, or persistence.

The exact titles and maxima are:

- Gardener (2): Sprout Tender; Toadstool Keeper.
- Farmer (3): Vine Puller; Root Wrangler; Mudfield Steward.
- Fisherman (5): Pond Forager; Coral Keeper; Amphibian Attendant; Opal Angler; Monument Mariner.
- Merchant (3): Moss Peddler; Lantern Trader; Glowgoods Baron.
- Chef (5): Tadpole Cook; Pond Cook; Swamp Chef; Grand Chef; Master of the Feast.
- Sorcerer (4): Wart Whisperer; Gatecaller; Flask Sage; Deep-Pond Oracle.
- Prospector (3): Pebble Picker; Vein-Seeker; Deep Delver.
- Guard (4): Pond Sentry; Lily Warden; Marsh Marshal; Bulwark of the Bog.
- Musician: unranked title Musician.

Custom entity names take precedence over rank titles. Saved state includes rank, XP, gate completion, all permanent choices, Chef menu/day data, current overworld restock day, restocks used, and last restock game time. Absent fields receive safe defaults. No general migration of old serialized offers is added because the retained world has no generated Ribbit villages.

## Exact trade profiles

The active maximum-rank concrete offer counts are Gardener 5, Farmer 5, Fisherman 8, Merchant 5, Musician 1, Chef 7, Sorcerer 19, Prospector 16, and Guard 11/12/15 for Weapons/Mount/Armor respectively. Each listed row has its own use counter.

### Gardener (maximum 2)

Tier 1 permanently selects one equal-weight linked pair: Red Mushroom to 4 small `ribbits:toadstool` plus Oxeye Daisy to 4 Swamp Daisies, or Lily Pad to 4 `ribbits:giant_lilypad` plus Small Dripleaf to 4 Umbrella Leaves. Tier 2 always sells 16 Red Toadstool Blocks, 16 Brown Toadstool Blocks, and 16 Toadstool Stems for one Glowcap each. Every offer has `maxUses=16`.

### Farmer (maximum 3)

Tier 1 always sells 32 Vines and 32 Hanging Roots for one Glowcap each. Tier 2 permanently selects 16 Muddy Mangrove Roots or 16 Rooted Dirt for one Glowcap. Tier 3 permanently selects 16 Coarse Dirt or 16 Mud for one Glowcap and always sells 8 Packed Mud for one Glowcap. Every offer has `maxUses=16`.

### Fisherman (maximum 5)

Tier 1 permanently selects 16 Sea Pickles, Kelp, or Seagrass for one Glowcap (`maxUses=16`). Tier 2 permanently selects one equal-weight Tube/Brain/Bubble/Fire/Horn family and sells its matching living block (8), fan (16), and coral (16) for one Glowcap each (`maxUses=16` each), never mixing colors. Tier 3 always sells one Tadpole Bucket and one Axolotl Bucket for one Glowcap (`maxUses=4` each). Tier 4 buys one exact component-bearing Opal for 16 Glowcaps (`maxUses=2`); its first success immediately and persistently gates promotion from Opal Angler to Monument Mariner, and 100 ordinary XP cannot bypass it. Tier 5 sells 4 Dry Sponges for one Glowcap (`maxUses=1`). Obsolete Tropical Fish Bucket, cooked fish, cod/salmon buybacks, and enchanted rod offers are removed.

### Merchant and Musician

Merchant Tier 1 sells 16 Mossy Oak Planks for one Glowcap; Tier 2 sells 8 Swamp Lanterns for one; Tier 3 sells 8 each of Ochre, Verdant, and Pearlescent Froglights for one. Every offer has `maxUses=16`. Old Toadstool Block/Stem and Maraca sales are removed.

Unranked Musician (`ribbits:nitwit`) has one fixed `8 Glowcaps -> 1 Maraca` offer with `maxUses=4`; existing Musician behavior is preserved.

### Chef (maximum 5)

Every Chef always offers `2 Glowcaps -> 1 exact Glow Berry Crumble` and `1 Glowcap -> 1 exact Honey Ginger Tea`, each with `maxUses=8`.

For every unlocked Tier 1–4, exactly one deterministic daily special is present. Tier 1 costs two Glowcaps and selects Pickled Carrots (5-minute Night Vision) or Rind Jam (5-minute Fire Resistance), `maxUses=8`. Tier 2 costs four and selects Gimmari (8-minute Water Breathing) or Bokguk (8-minute Conduit Power), `maxUses=6`. Tier 3 costs six and selects Golden Pickled Carrots (10-minute Night Vision), Melon Sorbet (10-minute Fire Resistance), Pumpkin Empanada (8-minute Resistance), Warped Pizza (5-minute Invisibility plus 3-minute Strength), or Warped Stroganoff (10-minute Invisibility), `maxUses=4`. Tier 4 costs twelve and selects Sweet Berry Danish (8-minute Health Boost II/four-heart behavior) or Golden Carrot Cupcake (20-minute Night Vision), `maxUses=2`. Every purchase returns exactly one prepared food.

At Master of the Feast, one permanent equal-weight specialty is selected: `20 Glowcaps -> 1 Japanese Curry` with 30-minute Strength, Green Curry with 30-minute Speed, or Tonkotsu Ramen with 30-minute Haste (`maxUses=1`). It never rotates.

The daily menu is deterministic for entity, tier, and overworld day; each pool cycles before repetition where practical. Same-day menu open, normal restock, chunk reload, and restart do not change it. A newly unlocked tier receives its one selection immediately without perturbing lower tiers. Next-day replacement covers all eligible tiers but is deferred while a trading session remains open.

### Sorcerer (maximum 4)

Tier 1 buys 4 exact Benzene for one Glowcap (`maxUses=16`). Its first success immediately and persistently gates promotion to Gatecaller; this is item-gated, not advancement-gated, and the trade remains afterward. Tier 2 includes all sixteen `4 Ender Pearls + 1 matching Dye -> 2 matching Portal Catalysts` offers (`maxUses=8` each). Tier 3 sells one exact Estus Flask for two Glowcaps (`maxUses=1`). Tier 4 permanently selects `16 Glowcaps -> 1 exact Prayer of Will/Reach` or `1 exact Prayer of Eros/Silk Touch` (`maxUses=1`). Fortune and other Blessings are excluded.

### Prospector (maximum 3)

Tier 1 buys each Golden/Iron Shovel for 2 Glowcaps, Hoe for 4, and Axe/Pickaxe for 8 (`maxUses=4` each), and sells 16 Glow Lichen for one Glowcap (`maxUses=16`). Tier 2 buys Diamond Shovel for 10, Hoe for 12, and Pickaxe/Axe for 16 (`maxUses=2` each), and sells 4 Pointed Dripstone for one Glowcap (`maxUses=16`). Tier 3 always sells exact Carbon-Rich Iron for 2 Glowcaps (`maxUses=8`) and permanently selects one equal-weight bullion specialty (`maxUses=1`): 8 Glowcaps to exact Hepatizon, 16 to exact Shakudo, or 16 Glowcaps plus one exact Divine Fragment to exact Electrum.

The Divine Fragment is exactly the `crafting:divine_fragment` result: base `minecraft:turtle_scute`, rare rarity, and enchantment-glint override. Plain Turtle Scutes do not match. All equipment buybacks match the intended underlying item while allowing arbitrary durability, enchantments, curses, repair cost, and custom name. Silver, Netherite tool recycling, and Adamant/Netherite sales are excluded.

### Guard (maximum 4)

One equal-weight permanent branch is chosen and never mixed. Weapons Tier 1 buys Golden/Iron Spear for 2 Glowcaps, Golden/Iron Sword for 4, and Bow for 1 (`maxUses=4` each); Tier 3 buys Diamond Spear for 4, Diamond Sword for 8, and Crossbow for 2 (`maxUses=2` each). Mount Tier 1 buys Saddle and Golden/Iron/Copper Nautilus Armor and Horse Armor for 4 Glowcaps (`maxUses=4` each); Tier 3 buys Diamond Nautilus and Horse Armor for 8 (`maxUses=2` each). Armor Tier 1 buys Golden/Iron Helmet for 6, Chestplate for 10, Leggings for 8, and Boots for 4 (`maxUses=4` each); Tier 3 buys Diamond Helmet for 12, Chestplate for 20, Leggings for 16, and Boots for 8 (`maxUses=2` each).

Every branch receives Tier 2 sales of 16 exact Iron Chains and 16 exact Copper Chains for one Glowcap (`maxUses=16` each), and Tier 4 `16 Glowcaps -> 1 Anvil` (`maxUses=1`). Equipment matching deliberately allows damage and additional equipment components. Adamant/Netherite mount rows and combat AI are excluded.

## Exact component and registry contracts

- `ribbits:giant_lilypad` is Lush Lily Pad; `ribbits:toadstool` is the small foliage mushroom.
- Opal is `minecraft:fermented_spider_eye` with exact `minecraft:opal` model and colored/translatable item name.
- Crystal Heart is the exact component-bearing `minecraft:poisonous_potato` contract described above.
- Foods resolve from `food:glow_berry_crumble`, `food:honey_ginger_tea`, `food:pickled_carrots`, `food:rind_jam`, `food:gimmari`, `food:bokguk`, `food:golden_pickled_carrots`, `food:melon_sorbet`, `food:pumpkin_empanada`, `food:warped_pizza`, `food:warped_stroganoff`, `food:sweet_berry_danish`, `food:golden_carrot_cupcake`, `food:japanese_curry`, `food:green_curry`, and `food:ramen`.
- Other exact recipe-result contracts are `crafting:benzene`, `crafting:estus_flask`, `blessings:reach`, `blessings:silk_touch`, `crafting:carbon_rich_iron`, `crafting:bronze_alloy` (Hepatizon), `crafting:shakudo_alloy`, `crafting:electrum_alloy`, and `crafting:divine_fragment`.
- Exact equipment registry IDs are vanilla 26.2 `minecraft:golden_spear`, `iron_spear`, `diamond_spear`; `iron_chain`, `copper_chain`; `{golden,iron,copper,diamond}_horse_armor`; and `{golden,iron,copper,diamond}_nautilus_armor`.
- Portal catalysts use `customportals:<color>_portal_catalyst` for all sixteen dye colors: white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, and black.

Component-bearing inputs require the exact intended components, including required absence when identity depends on it; generic stacks sharing a base item are rejected. Component-bearing outputs copy the exact fixed recipe result, preserving model, components, name, effects, duration, lore, enchantments, and behavior. The fixed-stack contract is validated server-authoritatively at startup rather than inferred from display names.

## Persistent daily stock

The prior loose restock cycle is replaced with saved overworld-day, restocks-used, and last-restock-game-time state. A new day starts with fresh initial stock and zero used ordinary restocks. A restock requires at least one exhausted offer, at most two ordinary restocks occur that day, and at least 2,400 ticks separate them. Restock refreshes the current materialized offers without rerolling profession choices, gates, or Chef menu. No workstation, POI, schedule, home, or time-of-day window is required.

Chunk unload, relog, server restart, and dimension changes retain the allowance. Backward or unusual time changes cannot repeatedly manufacture fresh days. If the Chef day changes during an active trading session, only daily menu replacement is deferred until the screen closes. Designed prices remain exact because all offers use zero demand multiplier. The maximum is exactly three stock batches per day: initial plus two restocks.

## Private Glowcap loot transform

The protected loot tables remain private. The deterministic assembler performs a one-for-one item-ID substitution at exactly these pre-existing amethyst-currency entries:

| Private table | Pool/entry | Preserved weight | Preserved count |
| --- | --- | ---: | --- |
| `ribbits:chests/fisherman_storage` | pool 0, entry 7 | 5 | 1–3 |
| `ribbits:chests/gardener` | pool 0, entry 3 | 3 | 1–2 |
| `ribbits:chests/merchant` | pool 0, entry 0 | 8 | 2–4 |
| `ribbits:chests/nitwit` | pool 0, entry 3 | 3 | 1–3 |
| `ribbits:chests/sorcerer` | pool 0, entry 4 | 5 | 2–6 |

Only `minecraft:amethyst_shard` becomes `ribbits:glowcap`; each pool, rolls, entry order, functions, conditions, weight, and count range remains unchanged. `fisherman_main` contains no currency substitution. The unrelated merchant `minecraft:amethyst_block` entry is retained. Existing repaired empty sentinels remain `minecraft:empty` and all private tables continue through exact Minecraft 26.2 codec/registry validation.

## Private donor boundary

The immutable donor references remain:

- `GuardRibbits-1.20.1-Fabric-1.0.4.jar` — 166,896 bytes; SHA-256 `52F1E184DC12CF1E29BC224AB5A640B8EA5875AA9F46067C0907C6A45B7D1869`.
- `useful_ribbits-1.0.2-forge-1.20.1.jar` — 416,439 bytes; SHA-256 `2B56007A985B162477113BB2EA1776D9CE2CE602886EA21A88D8B2500D2DED5D`.

The exact allowlisted archive members remain:

- `assets/guardribbits/geo/guard_ribbit.geo.json`
- `assets/guardribbits/textures/entity/guard_ribbit.png`
- `assets/useful_ribbits/geo/chef_ribbit.geo.json`
- `assets/useful_ribbits/textures/entities/chef_ribbit.png`
- `assets/useful_ribbits/geo/farmer_ribbit.geo.json`
- `assets/useful_ribbits/textures/entities/farmer_ribbit.png`
- `assets/useful_ribbits/geo/miner_ribbit.geo.json`
- `assets/useful_ribbits/textures/entities/miner_ribbit.png`

No donor Java/classes, AI, procedures, combat, structures, generation, sounds, animations, spawn-egg graphics, workstations, inventories, GUIs, or unrelated assets enter the build. Derived outputs remain private-use-only and no relicensing claim is made. Source-only output remains truthful and contains no protected/donor-private material.

## Historical Canary 2 to Mynx Canary 1 archive audit

The retained predecessor audit changed the complete uncompressed payload inventory from 434 to 462 entries: 28 additions, zero removals, and 22 changed payloads. The 28 additions were the four profession models, twelve profession-specific umbrella models, four typed-egg item definitions, four typed-egg conventional models, and four composite profession textures for Chef, Farmer, Guard, and Prospector. The 22 changed payloads were `assets/ribbits/lang/en_us.json`, the affected Ribbit model/data/profession/entity/pick-result/spawn-egg/creative-tab/umbrella class payloads, `fabric.mod.json`, the five profession structure NBTs, and the repaired `fisherman_main` and `sorcerer` loot tables. Nothing was removed and no unrelated protected-resource drift was found. This historical boundary remains provenance; Canary 3's separate predecessor-to-successor audit is recorded from its final deterministic artifact.

## Explicit deferrals

Canary 3 does not implement natural Sorcerers, witch-hut Sorcerer placement, witch suppression, hut markers/cats/structure modification, witch-hut or cartographer Ribbit Village Explorer Maps, Sorcerer replacement after death, donor Guard/Chef/Farmer/Miner behavior, generalized old-offer migration, dimension-aware homes, Fortune Blessing, Silver Bullion, Adamant/Netherite bullion or mount equipment, Netherite tool recycling, unrelated Matcha trades, or Custom Portals recipe/behavior changes.

## Runtime evidence boundary

The user reports the Phase A profession functionality they tested appeared operational: villages generated, the expanded roster appeared to work, the Prospector typed egg existed, and its model rendered correctly. A naturally generated Prospector was not directly observed in that sample, so that observation is neither a failure nor a natural-spawn pass. No complete runtime matrix or exact logs were supplied, and the evidence does not uniquely bind exact Ribbits/YUNG's API artifact identities; it is retained only as partial/unbound external evidence.

The Canary 3 economy, progression, restocking, loot currency, recipe, home interaction, and placeholders have not been runtime tested. Static validation must not be reported as runtime correctness.

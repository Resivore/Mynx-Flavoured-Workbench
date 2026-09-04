# Mynx Deviations

This file records intentional departures from the faithful Minecraft Java 26.2 Ribbits port. The exact faithful-port baseline is private Canary 2: version `4.1.6+26.2-port-canary2`, artifact `ribbits-private-reconstruction-4.1.6+26.2-port-canary2.jar`, 3,124,301 bytes, SHA-256 `0AD73B7B61C6EE792EC1745056563641767AFE6811C0FDF2D3C99123C3F289DC`, implementation checkpoint `efe1970d2447aea4913e67f55c0c6b83cc36c5bb`. It remains historical provenance, not an accepted or rollback release.

The direct predecessor is Mynx Canary 4: version `4.1.6+26.2-mynx-canary4`, artifact `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary4.jar`, 3,219,246 bytes, SHA-256 `DA7A37E0AEECE4D7633F0C5104298D76E066CD38DD5255FAE616DF95C5FBC712`, source checkpoint `cbc247054ecba937ad009dd651cde31fac504c22`. That exact identity has a bound user-reported external startup `FAIL`, described below. Canary 5 preserves Canary 4 exactly and does not rebuild, overwrite, rename, promote, or misrepresent it. Canary 3 and Canary 2 also remain historical and untouched at their recorded identities.

The current narrow Phase C startup-repair successor is `4.1.6+26.2-mynx-canary5`: private artifact `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary5.jar`, 3,220,021 bytes, SHA-256 `0DDE14CAAF1EF1E4B52CA118D61E0ED0706BB0EAD4B718471CA3CC5B5A70BE4F`; source-only artifact `ribbits-source-only-4.1.6+26.2-mynx-canary5.jar`, 1,160,122 bytes, SHA-256 `7386A91EEB45E6628222092E3A9ABDD8306932B05EC6F87A792D0E77204FB040`. It is `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. Its implementation checkpoint is assigned only after this content and the reproducible artifacts are frozen.

## Phase A and Phase B deviations retained

- The private `ribbits:chests/fisherman_main` table's invalid weight-15 `minecraft:air` no-loot sentinel remains repaired as a weight-15 `minecraft:empty` entry.
- The private `ribbits:chests/sorcerer` table's invalid weight-1 `minecraft:air` sentinel remains repaired as a weight-1 `minecraft:empty` entry. Its established strong-leaping potion migration remains unchanged.
- Four visual professions remain registered without separate entity types: `ribbits:chef`, `ribbits:farmer`, `ribbits:prospector`, and `ribbits:guard`.
- Internal profession `ribbits:nitwit` retains its registry/save identity and appears to users as Musician.
- Natural villages retain one equal-weight pool of Musician, Gardener, Fisherman, Merchant, Chef, Farmer, Prospector, and Guard. `ribbits:sorcerer` remains registered and accessible through its typed egg/commands but excluded from natural villages.
- Profession-aware private models, textures, composite rain atlases, and all three umbrella variants remain. Chef, Farmer, Prospector, and Guard keep generic Ribbit behavior and `RibbitInstrumentModule.NONE`; Guard's spear/shield remain cosmetic. No donor behavior is imported.
- Typed eggs, pick-block mappings, dispenser mappings, private donor transforms, and the duplicate-checked ordered creative-tab path remain.
- The coordinated YUNG's API `26.2-Fabric-6.1.1-compat.2` keeps enhanced-beardifier pieces/junctions as reusable lists and creates fresh call-local iterators for each density evaluation, preventing shared exhausted cursor state without disabling terrain adaptation.

## Phase C witch-hut resident

- Sorcerer remains excluded from the equal-weight natural Ribbit village pool and is not added to any natural entity spawn pool. Commands and the typed Sorcerer egg remain unchanged.
- A common/server mixin narrowly intercepts the one `EntityTypes.WITCH.create(Level, EntitySpawnReason)` invocation in `SwampHutPiece.postProcess`, verifies exact Witch plus `STRUCTURE`, lets vanilla persist its one-shot `Witch` flag, creates one canonical Ribbit instead, and returns `null` so vanilla never adds the initial Witch. The Sorcerer is positioned with the piece's normal transform from local `(3,2,5)`, finalized with spawn reason `STRUCTURE`, assigned exact profession `ribbits:sorcerer`, initialized as Wart Whisperer with normal Sorcerer umbrella/instrument data, reassesses goals so the Sorcerer buff goal is installed, takes that position as home, is marked persistent, and is added server-side once.
- The independent initial Cat path is preserved. Only the exact `spawnCat` local coordinate changes from `(2,2,5)` to `(2,2,4)`; its `Cat` one-shot flag, black-cat behavior, structure creature override, `CatSpawner`, and Cats elsewhere remain untouched.
- A north-facing closed barrel is placed through the structure piece's orientation-aware block method at local `(2,2,6)`, immediately left of the crafting table `(3,2,6)` and beside the cauldron `(4,2,6)`. The transformed coordinate must lie inside the supplied generation bounding box. Existing barrels are left intact so re-entry cannot reset a resolved inventory or restore a cleared loot key. A new valid Barrel block entity receives `ribbits:chests/swamp_hut_map`; no map search occurs during chunk generation.
- This is new-generation behavior, including `/place structure minecraft:swamp_hut`. Existing huts receive no retrofit Sorcerer, barrel, or Cat relocation, and saved Witches are never converted or removed. Killing the generated Sorcerer cannot reopen vanilla's consumed one-shot path.
- Removing Ribbits after generating a Phase C hut can therefore leave that hut without its initial Witch: the vanilla `Witch` flag is deliberately consumed even though the created resident is a Ribbit. This downgrade limitation is accepted and explicit.

## Exact natural-Witch boundary

A common/server head injection targets only the exact Minecraft 26.2 natural-candidate helper `NaturalSpawner.isValidSpawnPostitionForType(...)` (including Mojang's mapped `Postition` misspelling). It first requires the candidate entity type to be exactly `EntityTypes.WITCH`, resolves the exact registered `minecraft:swamp_hut` structure, calls `StructureManager.getStructureWithPieceAt`, and additionally requires the candidate position to be inside the bounding box of an actual `SwampHutPiece` in that valid start.

The inclusive procedural piece spans local `x=0..6`, `y=0..6`, and `z=0..8`, including empty upper layers. Support columns below the piece and positions immediately outside any face do not qualify. Missing, stripped, malformed, or otherwise unrecognized structure metadata fails closed as “not a recognized hut” and does not suppress broadly. No tag, biome, radius, locate search, chunk scan, or whole-structure replacement is used. Other monsters and initial-structure, command, egg, dispenser, spawner, trial-spawner, raid, conversion, event, and saved-Witch paths do not pass through this rejection.

The targeted audit used the 37,399,549-byte Loom merged/deobfuscated Minecraft 26.2 JAR at SHA-256 `F26880408F8C2404630293322FE5F5984E0A66409E31374EC51D29490660C1B4`. Exact unchanged members were `swamp_hut.json` `F3446923999CA537BDB60469D5A1EF6E1656D30CA9D345A664EE696411A17586`, `SwampHutStructure.class` `0F4052A7E66860F2FC3C57D7848A22CFA771D71F4CF9FC3218092578CC44C808`, `SwampHutPiece.class` `548DCF9DB4E4655403D987FB62DB903E797A11601AE1DED402F864CBA4ECD130`, `StructureManager.class` `B548CE5367D49E87CFA95B81486A8483E38BAF42BEB5941C5ADF8936C953B69C`, and `NaturalSpawner.class` `749A8A33D3897CCC5CF9E60FC2B32E0F435E6D01DF5BE62375A1965DBAA22329`.

The production-remapped Minecraft 26.2 hierarchy is `SwampHutPiece` → `ScatteredFeaturePiece` → `StructurePiece`. Both `getWorldPos` and the mixin's directly needed `placeBlock` are protected declarations on `net/minecraft/world/level/levelgen/structure/StructurePiece`; neither subclass redeclares them. The exact runtime name and return type of the coordinate transform are `getWorldPos` and `net/minecraft/core/BlockPos$MutableBlockPos`, with descriptor `(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;`. Despite that declaration owner, the actual `INVOKEVIRTUAL` instructions in both `SwampHutPiece.postProcess` and `SwampHutPiece.spawnCat` use owner `net/minecraft/world/level/levelgen/structure/structures/SwampHutPiece`, name `getWorldPos`, and the same descriptor.

Canary 4 incorrectly placed an `@Shadow getWorldPos` on the `SwampHutPiece` target, so Mixin searched that target for a declaration that exists only on `StructurePiece` and aborted startup with `InvalidMixinException`. Canary 5 removes the invalid shadow and registers `com.yungnickyoung.minecraft.ribbits.mixin.mixins.accessor.StructurePieceInvoker`, an `@Mixin(StructurePiece.class)` interface with remapped `@Invoker` bridges for `getWorldPos` and `placeBlock`. Both direct coordinate-transform calls and the oriented barrel placement use those non-reflective bridges; no transform math is copied. Both `@ModifyArgs` selectors correctly retain the actual `SwampHutPiece` invocation owner and exact coordinate-transform descriptor.

`SwampHutPiece.postProcess(WorldGenLevel, StructureManager, ChunkGenerator, RandomSource, BoundingBox, ChunkPos, BlockPos)` retains four fail-loud hooks, each with `require=1` and `allow=1`: ordinal-0 `@ModifyArgs` on exact `SwampHutPiece.getWorldPos(III)BlockPos$MutableBlockPos` for the resident, ordinal-0 `@Redirect` on exact `EntityType.create(Level,EntitySpawnReason)Entity`, `@Inject` immediately before ordinal-0 exact `spawnCat(ServerLevelAccessor,BoundingBox)V` for the barrel, and the Cat method's ordinal-0 `@ModifyArgs` on exact `SwampHutPiece.getWorldPos`. `NaturalSpawner.isValidSpawnPostitionForType(ServerLevel,MobCategory,StructureManager,ChunkGenerator,MobSpawnSettings$SpawnerData,BlockPos$MutableBlockPos,double)boolean` retains one cancellable head injection with `require=1` and `allow=1`.

The official-namespace production mapping setup requires no refmap for these selectors, and `ribbits.mixins.json` therefore intentionally has no `refmap` entry. The Canary 4 diagnostic “No refMap loaded” did not contribute to the invalid-shadow failure; a refmap would not make an inherited method into a direct `SwampHutPiece` declaration. A production-equivalent Fabric Loader Knot/Mixin harness against the built/remapped Minecraft 26.2 classes applied `StructurePieceInvoker`, `SwampHutPieceMixin`, and `NaturalSpawnerMixin` successfully, confirmed no invalid shadow remained, and resolved all five required injections exactly once. No Minecraft client or server was launched for that static validation.

## Ribbit Village Explorer Map result

- The dedicated base structure tag `#ribbits:on_ribbit_village_explorer_maps` contains exactly `ribbits:ribbit_village` and is distinct from the biome tag `#ribbits:has_structure/ribbit_village`.
- The shared item modifier `ribbits:ribbit_village_explorer_result` is the canonical result factory for this barrel and is ready for a future Phase D consumer without implementing one. Starting from exactly one vanilla empty map, its vanilla exploration-map function uses destination `#ribbits:on_ribbit_village_explorer_maps`, decoration `minecraft:village_plains`, raw structure-placement search radius `100`, zoom `2`, and `skip_existing_chunks=false`. Minecraft 26.2's `TagKey` JSON codec serializes that conceptual `#` tag as bare `ribbits:on_ribbit_village_explorer_maps`, matching vanilla cartographer data. Resolution remains synchronous and server-authoritative on first legitimate inventory unpack/access.
- Success is exactly one `minecraft:filled_map` with a valid vanilla `minecraft:map_id`, normal tracking and plains-village marker, named by translation key `item.ribbits.ribbit_village_explorer_map` as `Ribbit Village Explorer Map`.
- A normal no-target miss remains inside the same modifier and becomes exactly one vanilla `minecraft:map`, with no map ID or successful-map component, translated name `Uncharted Ribbit Map`, translated lore `No Ribbit village could be charted.`, and exact `minecraft:custom_data` boolean marker `{ribbits:failed_ribbit_village_map:1b}`. The modifier's common-side final step is registered as `ribbits:finalize_ribbit_village_explorer_result`; it preserves only a filled map with a valid map ID and otherwise constructs a fresh canonical failure, so persistent, transient (`minecraft:map_post_processing`), modded, and stale success components cannot leak. The marker—not display text—identifies the genuine failed search. The lazy loot result is fixed permanently once the barrel resolves.
- This map path uses only vanilla map data plus the existing Ribbits structure and introduces no new Matcha dependency. It does not alter Matcha Witch Hut maps or add a Matcha cartographer offer.

## Failed-map Sorcerer service

Every Sorcerer receives a rank-independent `1 exact marked Uncharted Ribbit Map -> 1 ribbits:toadstool_heart` offer at all four ranks, including command-, egg-, and hut-generated Sorcerers. It accepts only `minecraft:map` with the exact private custom-data marker and no map ID; generic or merely renamed empty maps, successful filled maps, and unrelated explorer maps fail. The service has `maxUses=16`, profession XP `0`, and price multiplier `0`. It neither completes nor bypasses the Benzene gate and cannot promote the Sorcerer; existing ordinary restocking may replenish it. A narrow Canary 3 save upgrade recognizes the exact component-aware Sorcerer offer shape at each rank and inserts only this service while preserving every existing serialized Phase B offer object, use count, demand, price adjustment, gate, rank, XP, specialization, and menu state; unexpected legacy or drifted shapes fail closed.

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

Custom entity names take precedence over rank titles. Saved state includes rank, XP, gate completion, all permanent choices, Chef menu/day data, current overworld restock day, restocks used, and last restock game time. Absent fields receive safe defaults. The only serialized-offer migration is the exact Canary 3-to-4 Sorcerer insertion described above; no general migration is added.

## Exact trade profiles

The active maximum-rank concrete offer counts are Gardener 5, Farmer 5, Fisherman 8, Merchant 5, Musician 1, Chef 7, Sorcerer 20, Prospector 16, and Guard 11/12/15 for Weapons/Mount/Armor respectively. Each listed row has its own use counter.

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

At every rank, the independent failed-map service exchanges one exact marked Uncharted Ribbit Map for one Toadstool Heart (`maxUses=16`, zero profession XP, zero multiplier) without affecting progression or the Benzene gate. Tier 1 buys 4 exact Benzene for one Glowcap (`maxUses=16`). Its first success immediately and persistently gates promotion to Gatecaller; this is item-gated, not advancement-gated, and the trade remains afterward. Tier 2 includes all sixteen `4 Ender Pearls + 1 matching Dye -> 2 matching Portal Catalysts` offers (`maxUses=8` each). Tier 3 sells one exact Estus Flask for two Glowcaps (`maxUses=1`). Tier 4 permanently selects `16 Glowcaps -> 1 exact Prayer of Will/Reach` or `1 exact Prayer of Eros/Silk Touch` (`maxUses=1`). Fortune and other Blessings are excluded.

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

## Future-only private village utility transform

The deterministic private assembler hash-pins and rewrites six protected templates under their verified current archive paths `data/ribbits/structure/houses/` (the audited pristine JAR does not store these six under the prompt's expected `structure/ribbits/` path). It requires each exact source hash, coordinate, palette state, and block-entity shape exactly once, preserves all non-target block records, reuses an exact existing air or Stone Bricks palette state, removes incompatible block-entity NBT, writes deterministic compressed NBT, and verifies the complete 29-template village tree.

| Private template | Source SHA-256 | Output SHA-256 | Coordinate | Exact change |
| --- | --- | --- | --- | --- |
| `brown_sorcerer_house.nbt` | `502DC904D293D411A5EBAFED2C7F71B8EED8E36AB2123553AE8AE3295A56AA76` | `D6878D280EC391A7FFD48FCD442FBDD33F6B341031E7EFE753F98812270D1B63` | `(6,2,5)` | Brewing Stand to air; remove `minecraft:brewing_stand` block-entity NBT and processor-supplied contents |
| `red_sorcerer_house.nbt` | `EF66D580570C81657500F714B76EB761DE910285571FF0CE36442B14E4BC8948` | `57DCF47CDECE4E459522CEA74B69215269A45C81028A2C42F2F3EBFAEE43D516` | `(6,2,5)` | Brewing Stand to air; remove `minecraft:brewing_stand` block-entity NBT and processor-supplied contents |
| `small_house_brown_3.nbt` | `329DD885FD3A26FBF809CC37B74696799BEA2E5397A6DD645810261BFBB1055A` | `B54530FFEB4C2284AB4397796B8BBAD411193FB318E3DFCF93F61558B78327B87` | `(3,1,5)` | Damaged Anvil to air; no target block-entity NBT existed |
| `small_house_red_3.nbt` | `4652D7C9FA1481B9D210A32140EEDC751A797C0D2DEB6B6E12F53D4C60955E70` | `2C5A76CF50F5993ED0F6F089AEFABE3B04FEAB2E75962E80B8CFA6F5788CCB3E` | `(3,1,5)` | Damaged Anvil to air; no target block-entity NBT existed |
| `small_house_brown_2.nbt` | `7E7CA64FE02C9953B6E3CCF3BF2A2393C3274BBBB3848AE874B4FF8C9C6B1676` | `BC65457EA8C0B6AAAF3902B04840FF8F1BA04EE5818B85E5C11C70CB5683B695` | `(5,1,6)` | Smoker to ordinary `minecraft:stone_bricks`; remove `minecraft:smoker` block-entity NBT |
| `small_house_red_2.nbt` | `A91945113B28214F5BE8935EFDBB4C42F6EC469BF9CA9BAE5074A0579023D20A` | `7CEB960251B893A75C82FA08D2268A307676A55D00A2CD5BE897CEB0B08D272D` | `(4,1,2)` | Blast Furnace to ordinary `minecraft:stone_bricks`; remove `minecraft:blast_furnace` block-entity NBT |

The canonical 29-template tree changes deterministically from SHA-256 `9C2F704975BAF2FE7E1C530C85A82CC1A69116BE609EE769615C422CFB8D0499` to `9D321D21A57F0800E564E05432E564341D3836696831624383EABF1BE48DD827`. Across all 29 templates the future-placement counts of Brewing Stands, Damaged Anvils, Smokers, and Blast Furnaces become zero. Existing generated villages are not edited. All Barrels, Chests, private loot bindings, Crafting Tables, Water Cauldrons, rooftop Campfires, beds, cakes, counters, floors, chimney surroundings, and every unrelated block remain. Raw lapis, end-stone, glass, Nether, and other processor sentinels are deliberately preserved in source templates and continue to be checked against final-world leakage.

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

## Mynx Canary 4 to Canary 5 archive audit

The final private archive advances from 605 to 606 ZIP records: one addition, zero removals, four changed records, and 601 byte-identical common records. The sole addition is `com/yungnickyoung/minecraft/ribbits/mixin/mixins/accessor/StructurePieceInvoker.class`. The four changed records are exactly:

- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/world/NaturalSpawnerMixin.class`
- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/world/SwampHutPieceMixin.class`
- `fabric.mod.json`
- `ribbits.mixins.json`

Every common entry retains its relative order, every Canary 5 record uses the fixed ZIP timestamp `1980-01-01T00:00:00-06:00`, and no duplicate path exists. Clean builds A and B are byte-identical: each private JAR is 3,220,021 bytes with SHA-256 `0DDE14CAAF1EF1E4B52CA118D61E0ED0706BB0EAD4B718471CA3CC5B5A70BE4F`, and each source-only JAR is 1,160,122 bytes with SHA-256 `7386A91EEB45E6628222092E3A9ABDD8306932B05EC6F87A792D0E77204FB040`.

The packaged private resources are exactly byte-identical to Canary 4: 336 files, including 245 strict JSON files, totaling 2,714,803 bytes, with inventory digest `885518CA13DB69F30A3BBDEB219B80516BB7DA7652CCA271823A996AC57E8106`. No protected NBT, model, texture, umbrella, egg, loot, trade, profession, donor-boundary, or other private-resource payload drifted.

## Mynx Canary 3 to Canary 4 archive audit

The final private archive advances from 591 to 605 ZIP records: 14 additions, zero removals, 19 changed payloads, and 572 byte-identical payloads. Every common entry retains its relative order, every Canary 4 record uses the same fixed ZIP timestamp `1980-01-01T00:00:00-06:00`, no duplicate path exists, and both clean builds have identical names, order, uncompressed sizes, compressed sizes, timestamps, and complete bytes.

The fourteen additions are exactly five directory records (`com/yungnickyoung/minecraft/ribbits/mixin/mixins/world/`, `com/yungnickyoung/minecraft/ribbits/world/loot/`, `com/yungnickyoung/minecraft/ribbits/world/structure/`, `data/ribbits/item_modifier/`, and `data/ribbits/tags/worldgen/structure/`) plus these nine payloads:

- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/world/NaturalSpawnerMixin.class`
- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/world/SwampHutPieceMixin.class`
- `com/yungnickyoung/minecraft/ribbits/module/LootFunctionModule.class`
- `com/yungnickyoung/minecraft/ribbits/world/loot/RibbitVillageExplorerMap.class`
- `com/yungnickyoung/minecraft/ribbits/world/loot/RibbitVillageExplorerResultFunction.class`
- `com/yungnickyoung/minecraft/ribbits/world/structure/SwampHutPhaseC.class`
- `data/ribbits/item_modifier/ribbit_village_explorer_result.json`
- `data/ribbits/loot_table/chests/swamp_hut_map.json`
- `data/ribbits/tags/worldgen/structure/on_ribbit_village_explorer_maps.json`

The nineteen changed payloads are exactly `assets/ribbits/lang/en_us.json`, `fabric.mod.json`, `ribbits.mixins.json`, `RibbitsCommon.class`, `RibbitEntity.class`, `RibbitEntity$1.class`, `StrictMerchantOffer.class`, `RibbitTradeModule.class`, `RibbitTradeModule$CostSpec.class`, `RibbitTradeModule$Gate.class`, `RibbitTradeModule$StackRef.class`, `RibbitTradeModule$TradeOfferSpec.class`, `RibbitTradeModule$TradeProfile.class`, and the six transformed NBT templates listed above. Bytecode inspection confirms the four apparent collateral changes in `RibbitEntity$1`, `RibbitTradeModule$Gate`, `RibbitTradeModule$StackRef`, and `RibbitTradeModule$TradeProfile` preserve their fields, method signatures, and instructions; only compiler debug/line metadata moved with the Phase C source. The three new data payloads are respectively 315 bytes/SHA-256 `F70EFB5D1B341AD05CFA24F12F07F6C1A7EA1E13EDDE20723E3BD3AA39CECCA0`, 375 bytes/SHA-256 `BFAB627241EDF908FB5337E9AB903E56D60FEBA4EFAAE4301259F163B197F967`, and 71 bytes/SHA-256 `13CA89B1F04F4FFBCFC053486A3F09A179CC75C5EA5FDE7E4EC1F97A50E8047E` in the order shown above. No unrelated protected-resource drift was found.

## Historical Canary 2 to Mynx Canary 1 archive audit

The retained predecessor audit changed the complete uncompressed payload inventory from 434 to 462 entries: 28 additions, zero removals, and 22 changed payloads. The 28 additions were the four profession models, twelve profession-specific umbrella models, four typed-egg item definitions, four typed-egg conventional models, and four composite profession textures for Chef, Farmer, Guard, and Prospector. The 22 changed payloads were `assets/ribbits/lang/en_us.json`, the affected Ribbit model/data/profession/entity/pick-result/spawn-egg/creative-tab/umbrella class payloads, `fabric.mod.json`, the five profession structure NBTs, and the repaired `fisherman_main` and `sorcerer` loot tables. Nothing was removed and no unrelated protected-resource drift was found. This historical boundary remains provenance; Canary 3's separate predecessor-to-successor audit is recorded from its final deterministic artifact.

## Explicit deferrals

Canary 5 does not implement a Wandering Ribbit, Wandering Ribbit map sale, automatic Sorcerer replacement after death, retrofit injection into existing huts, conversion of saved Witches, global Witch suppression, a whole swamp-hut replacement, asynchronous map search, a custom map GUI, a new registered map item, direct Toadstool Heart barrel fallback, Matcha cartographer Ribbit Village maps, changes to Matcha Witch Hut maps, donor Guard/Chef/Farmer/Miner behavior, generalized old-offer migration, dimension-aware homes, Fortune Blessing, Silver Bullion, Adamant/Netherite bullion or mount equipment, Netherite tool recycling, unrelated Matcha trades, or Custom Portals recipe/behavior changes.

## Runtime evidence boundary

The user reports the Phase A profession functionality they tested appeared operational: villages generated, the expanded roster appeared to work, the Prospector typed egg existed, and its model rendered correctly. A naturally generated Prospector was not directly observed in that sample, so that observation is neither a failure nor a natural-spawn pass. No complete runtime matrix or exact logs were supplied, and the evidence does not uniquely bind exact Ribbits/YUNG's API artifact identities; it remains partial/unbound external evidence.

The retained canonical Canary 3 and YUNG's API Compat.2 binaries rehashed exactly against authoritative `main`, so the user's Phase B report is bound to that pair as a practical overall `PASS`; practical use looked sufficient to continue. The exhaustive profession/tier trade matrix was intentionally not executed, so no unexecuted trade row, component rejection, menu rotation, specialization, or restock edge case is marked observed. Canary 3 is not promoted and receives no managed-slot result from that report.

The reported failing artifact is unambiguously the canonical Canary 4 private artifact: version `4.1.6+26.2-mynx-canary4`, filename `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary4.jar`, 3,219,246 bytes, SHA-256 `DA7A37E0AEECE4D7633F0C5104298D76E066CD38DD5255FAE616DF95C5FBC712`, source checkpoint `cbc247054ecba937ad009dd651cde31fac504c22`. The user's supplied observation is therefore bound only to that exact identity as an external runtime `FAIL`: startup aborted at the invalid `@Shadow getWorldPos` before Phase C gameplay could run. The task made no Test Instance Manager transition, so the report supplies no managed deployment or slot result and no unreported gameplay row is inferred.

Canary 5 was not deployed or launched and remains `RUNTIME_UNTESTED`. Its successful production-equivalent Knot/Mixin application and the rest of its static validation must not be reported as Minecraft gameplay runtime correctness.

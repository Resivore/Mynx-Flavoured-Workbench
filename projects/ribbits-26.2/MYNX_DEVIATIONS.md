# Mynx Deviations

This file records intentional departures from the faithful Minecraft Java 26.2 Ribbits port. The exact faithful-port baseline is private Canary 2: version `4.1.6+26.2-port-canary2`, artifact `ribbits-private-reconstruction-4.1.6+26.2-port-canary2.jar`, 3,124,301 bytes, SHA-256 `0AD73B7B61C6EE792EC1745056563641767AFE6811C0FDF2D3C99123C3F289DC`, implementation checkpoint `efe1970d2447aea4913e67f55c0c6b83cc36c5bb`. It remains historical provenance, not an accepted or rollback release.

The direct predecessor is runtime-failed Mynx Canary 11: `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary11.jar`, 3,334,222 bytes, SHA-256 `258CA17B5D61C825AFBAF852413C1F44183D2533446BFF297C4B8453AD9CECAA`, source `784b745d6480e076fb94598a3b466cfb98bbec04`. It and earlier private artifacts remain unchanged and unaccepted.

## Canary 21 authored open-Chute UV repair

- The exact user-authored `originals/assets/chute_leaf_open.bbmodel` and `chute_leaf_open.png` remain immutable inputs: 5,953 bytes / SHA-256 `1D2332100DAEF279FD9BD1EA442714FE82B572A1662E05F40360CDEAA7680444` and 743 bytes / SHA-256 `C9DCC9DB447C84E69306810AAEF1818B52525E8DF5205432C8EB454743E059B2`. The BBModel declares 32×32 and its embedded PNG equals the standalone bytes.
- Direct conversion retains every authored cube, face direction, display transform, ambient-occlusion value, supported rotation/pivot, shade value, and exact output PNG bytes. It now maps each face UV from source texture pixels into Minecraft Java item-model 0–16 space using the actual verified PNG width and height. UV ordering is never sorted: the authored canopy `[20, 30, 11, 19]` becomes `[10.0, 15.0, 5.5, 9.5]`, while its reversed counterpart remains reversed as `[10.0, 9.5, 5.5, 15.0]`.
- The generated `ribbits:item/chute_leaf_open` resource chain remains item definition → `assets/ribbits/models/item/chute_leaf_open.json` → exact `assets/ribbits/textures/item/chute_leaf_open.png`, with every face using `#layer0`. This is a conversion correction only: no geometry redesign, renderer-pose compensation, donor umbrella fallback, closed Chute change, or Chute mechanics change is introduced.

Current successor `4.1.6+26.2-mynx-canary16` changes only the Wandering Ribbit scheduler's heightmap-result-to-feet conversion. Mapped Minecraft Java 26.2 `ChunkAccess.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)` returns `Heightmap.getFirstAvailable(x & 15, z & 15) - 1`, the occupied top surface. Canary 16 keeps that result as `groundPos`, uses `groundPos.above()` as feet, and builds the unchanged AABB and `EVENT` spawn at those feet; body/head, hazards, fluids, biome, collision and bounds therefore use their actual positions. Canary 15's bounded diagnostics remain unchanged. Its exact artifact/source identity is in `WORKBENCH_STATUS.json` and the revision-21 log entry. It remains `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`.

## Canary 12 item-atlas correction

- The user's Canary 11 Minecraft screenshot/report is bound to the exact canonical Canary 11 identity above as an external runtime `FAIL`: the new rain geometry and back attachment appeared, but its entire zero-thickness canopy rendered with Minecraft's magenta/black missing-texture sprite. The task makes no managed deployment or slot transition and infers no unreported runtime behavior.
- Minecraft 26.2's exact `assets/minecraft/atlases/items.json` automatically stitches directory source `item` with prefix `item/`; it does not include `textures/entity`. Canary 11's item-baked open model therefore could not resolve `ribbits:entity/wandering_ribbit`, even though the PNG existed in the JAR. No atlas override is necessary.
- Canary 12 copies the exact donor `assets/wandering_ribbit/textures/entity/wandering_ribbit.png` bytes to assembled `assets/ribbits/textures/item/chute_leaf_open.png`: 6,233 bytes, 128×128, SHA-256 `8E481FA8B4E4458ADB52D60AB3123801E612835B65CD362264C00801D84D681A`. Both model texture keys bind `ribbits:item/chute_leaf_open`; the 128×128 UV conversion is unchanged. The unrelated 548-byte/32×32 donor `umbrella_leaf_texture.png` remains an immutable approved input but no longer supplies that output.
- `ChuteLeafRenderer`, `applyDeployedPose`, both model elements, every bound, UV, face and cull choice, inactive/closed/first-person presentation, mechanics, identities and Wandering behavior are byte-identical to Canary 11. The packaged model is 1,790 bytes/SHA-256 `32D66B6B30B89F3C4F51784A58D0C7A5CC9476DE32730331B0615CB7B3E9D55B`.

## Canary 11 deployed Drop Leaf rain visual

- Donor bytecode establishes the selection path: `WanderingRibbitEntity.m_20285_()` delegates to the level rain-at-position predicate, and the animation predicate selects `idle_holding` or `walk_holding` while it is true. Those animations expose `umbrella_leaf`, `grip`, and `leaf2` from `assets/wandering_ribbit/geo/wandering_ribbit.geo.json`; they do not render the separate player-held item model.
- The actual rain shelter is the `grip` cube (0.5×15×0.5) plus the `leaf2` zero-thickness 17×17 canopy plane. Both use the exact 128×128 `assets/wandering_ribbit/textures/entity/wandering_ribbit.png`; the populated canopy box-UV region is retained on both item-model faces so expected above/below views cannot be lost to face culling. The geometry is centered and vertically positioned at the established deployed canopy height without changing `ChuteLeafRenderer`.
- Before Canary 11, private assembly sourced `assets/ribbits/models/item/chute_leaf_open.json` from `assets/wandering_ribbit/models/custom/umbrella_leaf.json` and bound it to `ribbits:item/chute_leaf_open`. That file is the donor's separately registered hand-held umbrella item model, not the rain-held entity bones. A binary edit that omits or misplaces its texture binding exposes Minecraft's missing-texture magenta across the model's full zero-thickness square; Canary 11 instead generates the canonical model and resource reference together from hash-guarded private inputs.
- `ribbits:chute_leaf_open` still resolves through the existing item-definition and temporary `DataComponents.ITEM_MODEL` renderer contract. Canary 11's packaged model was 1,796 bytes/SHA-256 `FFD3A9529B6B3067C4908353FDB3E25E19788F8B6CBA95BBB0F9F021EDF5FAB1`; its entity-namespace texture reference failed in the item atlas at runtime. It contains no held-item `display`, `groups`, `texture_size`, or legacy custom-model contract.
- Inactive inventory art, `chute_leaf_closed`, closed chest/cape attachment, first-person callback/placement, `ChuteLeafRenderer`, registry/save ID, stack size, slot eligibility, input/acknowledgement/state, descent physics, Elytra conflicts, acquisition, durability and every Wandering/profession resource or behavior are unchanged. Canary 11 is runtime-failed; Canary 12 runtime validation remains pending.

## Canary 10 inventory and naming follow-up

- The displayed name is now **Drop Leaf** in both public and assembled localization. Registry/save identity `ribbits:chute_leaf`, tooltip key, trades, equipment and mechanics remain unchanged; no migration is needed.
- Latest attachment `codex-clipboard-891633e9-1445-4435-8f6c-2a6fd99e5413.png` is copied byte-for-byte to `tools/assets/drop_leaf_inventory.png`: 249 bytes, 16x16, SHA-256 `5ACBE4AFC118B2EC1A04EC5A2DCFD91F7DB05BD61937019A75300756CC257A30`. Assembly copies it to `assets/ribbits/textures/item/drop_leaf_inventory.png`; the existing `chute_leaf` item/model selects it for inventory and ordinary item presentation. No generation, redraw, resampling or conversion occurs.
- A separate auxiliary `chute_leaf_closed` item definition/model selects the unchanged earlier `chute_leaf.png` sprite. Only the closed-back renderer selects that model on a temporary stack copy. Its FIXED context, chest attachment, translation, rotation and scale remain unchanged. The equipped stack is not mutated. Canary 11 later replaces only the open private model resource while retaining the same renderer paths.
- Canary 9 already implements the requested horizontal canopy, empty-handed Wandering model, eight-Glowcap map cost, native 8x8 mushroom marker and one-pixel-down Glowcap. These are preserved, not applied twice. This follow-up independently confirms the local Minecraft 26.2 atlas has 35 decoration PNGs, all 8x8, and Compass Ribbon 2.9.0+26.2 resolves `MAP_DECORATIONS` by `MapDecorationType.assetId()` and blits full sprite UVs. The marker is independent of the filled-map inventory model. Existing asset and pose tests retain the exact crop/shift and deployed NONE-context contracts.
- All nine focused runtime checks in `TESTING.md` remain pending. Builds and resource/pose tests are static evidence only.

## Historical Canary 9 polish

- User attachment `codex-clipboard-52834f3e-016e-42ab-a25a-a71fe0524e40.png` is retained byte-for-byte as `tools/assets/chute_leaf.png`: 293 bytes, 16x16 RGBA, SHA-256 `816E4D4EDC23542AFEB2F2F90A5AF8A2076AE61829F1ACB016711E05FEC0191D`. Private assembly copies it to `assets/ribbits/textures/item/chute_leaf.png`; the existing generated model uses that path for inventory and closed back rendering. Open texture, geometry and display settings remain separate and unchanged. No generative imaging was used.
- The donor canopy is already an XZ surface at Y=22. Its `display.fixed.rotation=[0,0,42.75]` is an item-frame presentation incorrectly inherited by the old deployed renderer. Deployment now uses `ItemDisplayContext.NONE`, bypasses animated chest attachment, and applies X=180 degrees to convert item Y-up into entity model Y-down. Translation `(0,-0.10,0.18)` and uniform scale 1.2 position the horizontal canopy above its grip. Closed chest attachment and first-person transforms remain unchanged; no distorted geometry compensation is used.
- Exact local Compass Ribbon 2.9.0+26.2 `CompassRibbonOverlay.renderMarkers/renderMarker` gets `AtlasIds.MAP_DECORATIONS`, resolves `RibbonMapMarker.getAssetId()` via `MapDecorationType.assetId()`, and blits full sprite UV bounds into its configured marker area. Minecraft 26.2 `assets/minecraft/atlases/map_decorations.json` loads `map/decorations`; all 35 native decoration PNGs are 8x8, including plains village, woodland mansion and ocean monument. Ribbits uses the ordinary native `ribbits:ribbit_village` decoration, independently of its successful-map inventory item model. Crop `[4,12) x [4,12)` preserves every pixel of the old 16x16 sprite's 6x7 mushroom, changing visible bounds `(5,4)..(10,10)` to `(1,0)..(6,6)` on native 8x8. There is no resampling, blur, palette change or Compass Ribbon edit. Output: 122 bytes, SHA-256 `BD05CCFBE6ADE532D20FC42F05C665074A0A733A9398069D8D766B515ABA861D`.
- Only the Wandering model's held `umbrella_leaf` bone and `grip`/`leaf2` children are removed during private generation. Its separate body `leaf`, arms, clothing, texture and all other geometry remain intact; ordinary Ribbit renderers are untouched. Output model: 9,102 bytes, SHA-256 `F7EE351AE54CB90C86E36A5FAF991F22A25A11A7D755E4B4234D096C2D71A920`. Serialization whitespace changes do not alter surviving geometry.
- Native trade schema 3 makes newly materialized map offers cost eight Glowcaps with no second input. Map modifier/output, once-only materialization, index, uses, XP, pricing and all other offers/buybacks remain unchanged. Existing saved merchants retain their serialized offers/use counts; there is no old-menu migration.
- Glowcap RGBA content moves exactly one pixel down on the same 16x16 canvas, from bounds `(1,3)..(14,11)` to `(1,4)..(14,12)`. No visible content is clipped or resampled; all 78 opaque pixels and transparency remain. Reversing the shift recovers the original full RGBA digest. Output: 221 bytes, SHA-256 `9F79B36007A4A4E5E0B5683264C335D76C0F7E00572116B74A33B9E26C0D5ABE`.
- To meet the repository runtime dependency policy for a new release, Trinkets now requires minimum `>=4.1.0-beta.3`, and Matcha Heart Death uses release floor `>=0.1.10-0` instead of a Canary-specific predicate; the exact accepted C5 build JAR/hash remains pinned separately. Release-scoped capability/provider attestation has no exceptions. Immutable donor hashes are preserved, generated private assets/JAR remain ignored, and no runtime profile is touched.

## Phase A and Phase B deviations retained

- The private `ribbits:chests/fisherman_main` table's invalid weight-15 `minecraft:air` no-loot sentinel remains repaired as a weight-15 `minecraft:empty` entry.
- The private `ribbits:chests/sorcerer` table's invalid weight-1 `minecraft:air` sentinel remains repaired as a weight-1 `minecraft:empty` entry. Canary 6 separately replaces its weight-5 Glass Bottle entry and weight-1 strong-leaping Potion entry with same-weight empty entries as detailed below.
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
- The shared item modifier `ribbits:ribbit_village_explorer_result` is the sole canonical result factory for the Phase C barrel and Phase D Wandering Ribbit sale. Starting from exactly one vanilla empty map, its vanilla exploration-map function uses destination `#ribbits:on_ribbit_village_explorer_maps`, native decoration `ribbits:ribbit_village`, raw structure-placement search radius `100`, zoom `2`, and `skip_existing_chunks=false`. Minecraft 26.2's `TagKey` JSON codec serializes that conceptual `#` tag as bare `ribbits:on_ribbit_village_explorer_maps`, matching vanilla cartographer data. Resolution remains synchronous and server-authoritative on first legitimate barrel access or one-time merchant materialization.
- `ribbits:ribbit_village` is one Ribbits-owned registered `MapDecorationType` with its own asset identity and the same map behavior flags as `minecraft:village_plains`. Its tracked user-approved marker `assets/ribbits/textures/map/decorations/ribbit_village.png` is copied byte-for-byte from `ribbit_village_marker_16x16.png`: 168 bytes, 16×16 8-bit RGBA, SHA-256 `DF63EB91EDA13E91E3B984B11CDFFC3D2F3E8C8482D090D4EE1C744EC428B2BA`.
- Success is exactly one `minecraft:filled_map` with valid vanilla `minecraft:map_id`, native `ribbits:ribbit_village` decoration, exact custom-data boolean `{ribbits:ribbit_village_explorer_map:1b}`, and translation key `item.ribbits.ribbit_village_explorer_map` (`Ribbit Village Explorer Map`). A chain-preserving wrapper for `minecraft:filled_map` claims only that exact mechanical identity and otherwise delegates. Its tracked conventional generated-item presentation uses the supplied `610cbaa3-e4e8-4d55-abb9-377f0545e672.png` unchanged as `assets/ribbits/textures/item/ribbit_village_explorer_map.png`: 506 bytes, 16×16 8-bit RGBA, SHA-256 `6065E126DA4D3D70725CC3ADCA725E2CE2812BA8A0155A07C1E510B373AA38F5`.
- A normal no-target miss remains inside the same modifier and becomes exactly one vanilla `minecraft:map`, with no map ID or successful-map component, translated name `Uncharted Ribbit Map`, translated lore `No Ribbit village could be charted.`, and exact `minecraft:custom_data` boolean marker `{ribbits:failed_ribbit_village_map:1b}`. The modifier's common-side final step is registered as `ribbits:finalize_ribbit_village_explorer_result`; it preserves only a filled map with a valid map ID and otherwise constructs a fresh canonical failure, so persistent, transient (`minecraft:map_post_processing`), modded, and stale success components cannot leak. The marker—not display text—identifies the genuine failed search. The lazy loot result is fixed permanently once the barrel resolves.
- This map path uses only vanilla map data plus the existing Ribbits structure and introduces no new Matcha dependency. It does not alter Matcha Witch Hut maps or add a Matcha cartographer offer.

## Failed-map Sorcerer service

Every Sorcerer receives a rank-independent `1 exact marked Uncharted Ribbit Map -> 1 ribbits:toadstool_heart` offer at all four ranks, including command-, egg-, and hut-generated Sorcerers. It accepts only `minecraft:map` with the exact private custom-data marker and no map ID; generic or merely renamed empty maps, successful filled maps, and unrelated explorer maps fail. The service has `maxUses=16`, profession XP `0`, and price multiplier `0`. It neither completes nor bypasses the Benzene gate and cannot promote the Sorcerer; existing ordinary restocking may replenish it. A narrow Canary 3 save upgrade recognizes the exact component-aware Sorcerer offer shape at each rank and inserts only this service while preserving every existing serialized Phase B offer object, use count, demand, price adjustment, gate, rank, XP, specialization, and menu state; unexpected legacy or drifted shapes fail closed.

## Dedicated Wandering Ribbit entity

- `ribbits:wandering_ribbit` is a dedicated final `AbstractVillager`, not `RibbitEntity`, not a tenth profession, and not a subclass or relabeling of vanilla `WanderingTrader`. It has a 0.5×0.75 bounding box, 15 maximum health, 0.125 movement speed, its own entity type and renderer, retained Ribbits idle/walk animations, and ordinary Ribbits ambient, step, hurt, death, and trade sounds.
- It cannot breed, shows no profession progress, awards no merchant or player XP, never restocks, and receives no rank, POI, workstation, gossip, brain, daily schedule, llama, invisibility potion, milk, camp, structure, world generation, or vanilla Wandering Trader state.
- Command- and egg-created entities remain non-scheduler-managed, persistent ordinary merchants and may use portals normally. They receive normal offers but no automatic lease, generation, visit timer, departure, cap occupancy, or cooldown authority. Scheduler-managed entities are event-spawned, persistent, bound to one generation/dimension/expiry/target, and cannot portal away from the lease.
- The entity persists its independent trade seed, ordinary serialized `MerchantOffers` and use counts, provider snapshot, wander target, scheduler-managed flag, lease generation, expiry, and dimension. Expiry pauses while a valid merchant session remains open and resumes immediately when that session ceases to be valid.
- `ribbits:wandering_ribbit_spawn_egg` is a plain Minecraft 26.2 `SpawnEggItem` whose entity-type component binds it to this separate entity. It is translated as `Wandering Ribbit Spawn Egg`, appears once in the Ribbits tab, once in vanilla Spawn Eggs, and once in Creative search, and supports ordinary hand, dispenser, and pick-block paths. The vanilla-tab entry is parent-tab-only so the Ribbits tab is the sole search contributor. The ignored private item definition and model use the existing approved shared green Ribbits egg texture; the egg has no recipe, loot source, or trade.

## Independent scheduler and lease

- One server-global scheduler executes once per server tick and never reads, mutates, delays, caps, replaces, or reuses vanilla Wandering Trader state. SavedData type `ribbits:wandering_ribbit_spawner` persists exactly `next_attempt_time`, `active_lease_generation`, optional active entity UUID, optional active dimension, `visit_expiry`, `fair_player_cursor`, and `last_observed_time`.
- First initialization delays 48,000–72,000 ticks. A successful spawn delays the next attempt 120,000–168,000 ticks; any failed attempt retries after 1,200 ticks; a scheduled visit lasts 48,000 ticks. Backward game-time movement rebases absolute deadlines rather than manufacturing retries or expiration.
- The cap is one generation-stamped active lease across the server. The lease commits only after the exact `EVENT`-spawned entity is inserted, alive, addressable by UUID, and has successfully materialized all native offers. An unloaded holder retains the lease without a chunk ticket or force-load. Missing dimensions, wrong entity types, exact destruction, or expiry clear only the owned lease; a stale/replayed generation self-discards and cannot coexist with a successor.
- Only living, connected, non-spectating Survival or Adventure players in the Overworld are candidates. Stable UUID sorting and the persisted round-robin cursor prevent permanent first-player bias. The scheduler examines at most 16 random positions per selected player and 64 total, within a 24–48-block annulus and at least 16 blocks from every eligible player.
- Placement uses only already-loaded, entity-ready chunks and the existing motion-blocking-no-leaves heightmap. It evaluates the biome at the candidate feet, requires sturdy dry ground, dry body/head space, collision clearance, and world-border containment, and rejects fire, cactus, magma, powder snow, lit campfires, invalid heights, fluids, and denied biomes.
- The exact vanilla baseline is `swamp`, `mangrove_swamp`; `forest`, `flower_forest`, `birch_forest`, `old_growth_birch_forest`, `dark_forest`, `pale_garden`; `plains`, `sunflower_plains`, `meadow`, `cherry_grove`; `jungle`, `sparse_jungle`, `bamboo_jungle`; `taiga`, `old_growth_pine_taiga`, `old_growth_spruce_taiga`; `mushroom_fields`; and `river`. Meadow and Cherry Grove remain explicit exceptions to the barren-highland exclusion, and non-snowy taigas remain allowed. Wetland/river admission never bypasses dry placement.
- Reloadable biome tag `ribbits:without_wandering_ribbit_spawns` always wins over `ribbits:allows_wandering_ribbit_spawns`. An installed allow tag is authoritative even when intentionally empty. If it is absent, the exact vanilla baseline plus audited Minecraft forest, jungle, taiga, and river family tags supplies the fallback behind hard exclusions for ocean, badlands, savanna, windswept-hill, snow/frozen, desert, and stony-peak families. This gives accurately tagged modded equivalents the same policy without automatically admitting every Overworld biome or depending on a biome mod.

## Stable Wandering trade providers and exact offers

- The provider registry bootstraps permanent native provider `ribbits:native`, rejects duplicate IDs or replacement of the native provider, returns providers in stable ID order, and derives a provider-isolated seed from the entity seed plus provider ID. Adding, removing, or failing an optional provider cannot perturb another provider's choices.
- Provider ID, positive schema version, first-offer index, and offer count are persisted as sorted contiguous ranges. Serialized offers and uses remain authoritative on reload, so existing schema-1/five-offer merchants are not upgraded, rerolled, or reset. A failing optional provider contributes a persisted zero-offer range; native-provider failure rejects entity materialization and prevents lease commit.
- Native schema 2 preserves the original five offers at indexes 0–4: `20 ribbits:glowcap -> 1 ribbits:chute_leaf` (`maxUses=1`); `8 ribbits:glowcap + 1 minecraft:compass ->` one canonical shared map result (`maxUses=1`); and three distinct `1 ribbits:glowcap -> curiosity` offers (`maxUses=2`) sampled without replacement from the unchanged eight-member pool. It appends at index 5 one ordinary Compass → 4 Glowcaps buyback.
- Optional provider `ribbits:optional_matcha_compasses`, schema 1, resolves the exact Copper, Golden, and Titanium Compass contracts before staging any result. It contributes all three buybacks or none: Copper → 2 Glowcaps, Golden → 4, and Titanium → 8. The exact intended Matcha stack therefore gives each newly materialized merchant nine offers. Each buyback has `maxUses=2`, zero XP, zero price multiplier, and no restock. The unchanged map modifier still runs exactly once during materialization.
- All four compass variants are base `minecraft:compass`. Identity matching requires only the exact `minecraft:item_model` plus `minecraft:item_name`: ordinary is `minecraft:compass`/`item.minecraft.compass`; Copper is the `crafting:copper_compass` result `minecraft:copper_compass`/`item.kleispack.copper_compass`; Golden is the `crafting:golden_compass` result `minecraft:golden_compass`/`item.kleispack.golden_compass`; Titanium is the first `minecraft:loot_table/chests/equipment/special_compass.json` entry `minecraft:titanium_compass`/`item.kleispack.titanium_compass`. Custom names and legitimate lodestone targeting are accepted as mutable extras; changing display text cannot impersonate another tier, and specialized variants cannot enter the ordinary offer.

## Chute Leaf equipment, authority, movement, and rendering

- `ribbits:chute_leaf` is a stack-one, noncraftable, nondurable, unlimited-use Ribbits item. Its sole Trinkets eligibility file is `data/trinkets/tags/item/chest/cape.json`. Both tag and runtime logic require exact slot ID `chest/cape`, index 0, in the already accepted amount-one slot. There is no `chest/back` or broad all-slot tag, slot/group definition, second inventory, equippable component, glider component, recipe, or spawn egg.
- The exact accepted dependency is `trinkets-4.1.0-beta.3+26.2-inventory-compat-canary5.jar`, 560,208 bytes, SHA-256 `4C1FA6AC36C0457483FD0D395B99BBD94C9334AAD6DEFECE7633BBF0552D1724`. Canary 8 preserves the Chute's item eligibility and does not alter that artifact, Inventory Extended, the Trinkets companion, Traveler's Toolbelt, the accepted amount-one `chest/cape` definition, or disabled Elytra-support policy.
- Equip callbacks and vanilla-equipment insertion/swap checks reject the Chute outside exact `chest/cape`, reject Chute insertion beside any equipped vanilla or Trinkets glider, and reject glider insertion beside an active Chute. A broken glider still conflicts with insertion; movement validity independently follows usable-glider rules.
- The client samples only physical jump-key rising edges and queues a monotonically increasing sequence plus current dimension. The server advances highest-sequence authority before every other check, rejects replays and wrong dimensions, accepts at most one request per two ticks, deploys immediately at nonpositive vertical velocity, or keeps an ascending request pending for at most 60 ticks until descent begins. A second accepted press never closes a deployed Chute. Owner-only acknowledgements are exactly `REJECTED`, `PENDING`, and `DEPLOYED`.
- Pending/deployed state is server-owned and transient. Only the deployed boolean is synchronized to clients, never saved. Death, disconnect, missing/invalid Chute, ground, vehicle, swimming/crawling, water/lava, climbing, sleep, levitation, flight, fall-flying, usable glider, dimension mismatch, or another invalid state clears motion state; stale acknowledgements cannot reopen it.
- Valid deployment clamps only vertical velocity below `-0.10` blocks per tick before and after travel. It preserves horizontal velocity, ascent, and already-slower descent and resets fall distance only while the server-authoritative state remains valid. It adds no lift, steering, horizontal drag, Elytra pose, flight, durability, firework, or portal behavior.
- One item-specific Trinkets renderer owns presentation: a closed leaf sits on the back, an open canopy follows ordinary player transforms while deployed, invisible render states draw nothing, and first person renders through exactly the physical main-arm callback. No Elytra wing renderer or second generic layer is registered.

## Optional Map Marker Extensions and Compass Ribbon boundary

Map Marker Extensions C8 remains a separate optional project: `map-marker-extension-0.4.0-canary8.jar`, 58,720 bytes, SHA-256 `C3DA533B207050855C691584DB6BC3C85B383A9EC8E338737F7986D426668CA2`, and `map-marker-extension-icons-0.4.0-canary8.zip`, 34,938 bytes, SHA-256 `E88B18402275139A28A5FC9C1CE860937138D0E665CBD842EDEC5DE6EDA2521C`. Its icon pack is byte-identical to C7. C8 recognizes only an externally owned `minecraft:filled_map` with `minecraft:map_id` and exact `ribbits:ribbit_village_explorer_map=true`, admits only the native `ribbits:ribbit_village` holder, and passes the live holder, ordinary coordinates, and asset through its existing Xaero projection. It contains only inert Ribbits identifier strings, no Ribbits class/resource dependency, and never registers, supplies, or rewrites the Ribbits marker or item art. Nested wrapper tests passed in both registration orders. C8 remains unaccepted, not deployed, and runtime untested.

Static inspection of official Compass Ribbon 2.9.0 for Minecraft 26.2, CurseForge file 8261473, exact 469,546-byte SHA-256 `0F3A03C4ECC8B78420ECDCB4CC126810EA10581694AB808141D7F81B16D59905`, confirmed that its standard map-decoration path is generic over the live decoration holder/asset and contains no vanilla-only type whitelist. That establishes only static architectural compatibility for `ribbits:ribbit_village`; neither Compass Ribbon nor the combined native marker presentation has runtime evidence.

## Permanent currency and home item

- `ribbits:glowcap` is a permanent distinct stack-64 Ribbits item and the sole money in this candidate's Ribbit trade profiles. It has no crafting recipe and appears once in the creative tab.
- `ribbits:toadstool_heart` is a permanent distinct stack-64 Ribbits item, appears once in the creative tab, and is produced only by the approved shaped recipe.
- Canary 6 tracks the attached final user-approved sprites byte-for-byte. Glowcap source `glowcap_16x16_final.png` is 323 bytes, 16×16, 8-bit RGBA with real binary transparency, SHA-256 `414BA9042F4BF97278927CB8D65C78AE076B14824A4F34F87F6C7C729543D5DF`; it is installed unchanged as `assets/ribbits/textures/item/glowcap.png`. Toadstool Heart source `4e8e067e-3969-49ea-be28-fb8d91ea932b.png` is 881 bytes, 16×16, 8-bit RGBA with real binary transparency, SHA-256 `024773D1CCCFBE15BA4378B53B09D8522E6157C6EF7CB6E693A99AC8AE36ECB0`; it is installed unchanged as `assets/ribbits/textures/item/toadstool_heart.png`. Neither sprite was generated, redrawn, recolored, smoothed, resized, optimized, or otherwise reinterpreted.
- The item definitions `assets/ribbits/items/glowcap.json` and `assets/ribbits/items/toadstool_heart.json` now select `ribbits:item/glowcap` and `ribbits:item/toadstool_heart`. Their conventional models are respectively `assets/ribbits/models/item/glowcap.json` and `assets/ribbits/models/item/toadstool_heart.json`, each with parent `minecraft:item/generated` and its matching Ribbits `layer0`. The former `minecraft:item/warped_fungus` and `minecraft:item/heart_container` placeholder references are absent.
- These user-approved textures, models, and item definitions are trackable public resources and are admitted to the source-only artifact. Both registry IDs, names, stack sizes, the recipe, every trade and loot reference, saved state, and all mechanics remain unchanged. Glowcap gains no Warped Fungus placement, composting, tags, food, recipe, or block-item behavior; Toadstool Heart gains no Crystal Heart effect, consumable component, attribute, enchantment, rarity, glint, or progression behavior.
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

## Private village loot transform

The protected loot tables remain private. The deterministic assembler performs a one-for-one item-ID substitution at exactly these pre-existing amethyst-currency entries:

| Private table | Pool/entry | Preserved weight | Preserved count |
| --- | --- | ---: | --- |
| `ribbits:chests/fisherman_storage` | pool 0, entry 7 | 5 | 1–3 |
| `ribbits:chests/gardener` | pool 0, entry 3 | 3 | 1–2 |
| `ribbits:chests/merchant` | pool 0, entry 0 | 8 | 2–4 |
| `ribbits:chests/nitwit` | pool 0, entry 3 | 3 | 1–3 |
| `ribbits:chests/sorcerer` | pool 0, entry 4 | 5 | 2–6 |

Only `minecraft:amethyst_shard` becomes `ribbits:glowcap`; each pool, rolls, entry order, functions, conditions, weight, and count range remains unchanged. `fisherman_main` contains no currency substitution. The unrelated merchant `minecraft:amethyst_block` entry is retained.

Canary 6 makes exactly two additional replacements in `data/ribbits/loot_table/chests/sorcerer.json`. Pool 0 entry 2 changes from the weight-5 `minecraft:glass_bottle` item with uniform 2–4 `minecraft:set_count` to `{type: minecraft:empty, weight: 5}`. Pool 1 entry 1 changes from the weight-1 strong-leaping `minecraft:potion` item to `{type: minecraft:empty, weight: 1}`; its item-specific potion function is removed. The exact pristine assembler input expresses that potion through legacy `minecraft:set_components` with `potion_contents=minecraft:strong_leaping`, while Canary 5's packaged table expresses it through `minecraft:set_potion`; the Canary 6 final table contains neither form. The previously repaired, distinct pool 1 entry 0 weight-1 empty sentinel remains. The canonical final table is 2,563 bytes with SHA-256 `5B06E06502BF11F661161E89BF34E329D8F23268B7B0104371038C38AD9B378D`. It contains zero Glass Bottle, Potion, Splash Potion, or Lingering Potion item entries, zero `minecraft:set_potion` functions, and no legacy potion-contents component function. Every unrelated entry, probability, pool roll, condition, weight, function, Glowcap substitution, and all six existing Sorcerer-container bindings remain unchanged. Already-resolved inventories in existing worlds are not retroactively changed; an unresolved container that still references this table may use Canary 6 when vanilla lazily resolves its loot.

## Future-only private village utility transform

The deterministic private assembler hash-pins and rewrites four protected templates under their verified current archive paths `data/ribbits/structure/houses/`. It requires each exact source hash, coordinate, palette state, and block-entity shape exactly once, preserves all non-target block records, reuses an exact existing air palette state, removes only incompatible Brewing Stand block-entity NBT, writes deterministic compressed NBT, and verifies the complete 29-template village tree.

| Private template | Source SHA-256 | Output SHA-256 | Coordinate | Exact change |
| --- | --- | --- | --- | --- |
| `brown_sorcerer_house.nbt` | `502DC904D293D411A5EBAFED2C7F71B8EED8E36AB2123553AE8AE3295A56AA76` | `D6878D280EC391A7FFD48FCD442FBDD33F6B341031E7EFE753F98812270D1B63` | `(6,2,5)` | Brewing Stand to air; remove `minecraft:brewing_stand` block-entity NBT and processor-supplied contents |
| `red_sorcerer_house.nbt` | `EF66D580570C81657500F714B76EB761DE910285571FF0CE36442B14E4BC8948` | `57DCF47CDECE4E459522CEA74B69215269A45C81028A2C42F2F3EBFAEE43D516` | `(6,2,5)` | Brewing Stand to air; remove `minecraft:brewing_stand` block-entity NBT and processor-supplied contents |
| `small_house_brown_3.nbt` | `329DD885FD3A26FBF809CC37B74696799BEA2E5397A6DD645810261BFBB1055A` | `B54530FFEB4C2284AB4397796B8BBAD411193FB318E3DFCF93F61558B78327B87` | `(3,1,5)` | Damaged Anvil to air; no target block-entity NBT existed |
| `small_house_red_3.nbt` | `4652D7C9FA1481B9D210A32140EEDC751A797C0D2DEB6B6E12F53D4C60955E70` | `2C5A76CF50F5993ED0F6F089AEFABE3B04FEAB2E75962E80B8CFA6F5788CCB3E` | `(3,1,5)` | Damaged Anvil to air; no target block-entity NBT existed |

The two former processor transformations are removed, not reversed against a Canary 5 stage. Fresh assembly from the exact pristine input preserves these complete compressed template bytes:

| Guarded pristine pass-through | Bytes / SHA-256 | Coordinate | Exact retained state |
| --- | --- | --- | --- |
| `small_house_brown_2.nbt` | 2,810 / `7E7CA64FE02C9953B6E3CCF3BF2A2393C3274BBBB3848AE874B4FF8C9C6B1676` | `(5,1,6)` | north-facing unlit `minecraft:smoker`; original `minecraft:smoker` block entity, exact block-entity-NBT digest `65C78F8C0D18FBE8DE274ADF10D4D3D7E5365E37E7F7A8F95295719D0265B536` |
| `small_house_red_2.nbt` | 2,881 / `A91945113B28214F5BE8935EFDBB4C42F6EC469BF9CA9BAE5074A0579023D20A` | `(4,1,2)` | south-facing unlit `minecraft:blast_furnace`; original `minecraft:blast_furnace` block entity, exact block-entity-NBT digest `F0F5C4E4E4987767031407CD66B704E739A8C092BB08DF7269888F172440AD25` |

The canonical 29-template tree changes deterministically from SHA-256 `9C2F704975BAF2FE7E1C530C85A82CC1A69116BE609EE769615C422CFB8D0499` to `CB1CAC748727CC4387092CEE0D77426816204D0986866D44E9995D6948468DE0`. Across all templates the future-placement counts are zero Brewing Stands, zero Damaged Anvils, one Smoker, one Blast Furnace, and zero ordinary Furnaces. Existing generated villages are not edited. All Barrels, Chests, private loot bindings, Crafting Tables, Water Cauldrons, rooftop Campfires, beds, cakes, counters, floors, chimney surroundings, and every unrelated block remain. Raw lapis, end-stone, glass, Nether, and other processor sentinels are deliberately preserved in source templates and continue to be checked against final-world leakage.

## Private donor boundary

The immutable donor references remain:

- `GuardRibbits-1.20.1-Fabric-1.0.4.jar` — 166,896 bytes; SHA-256 `52F1E184DC12CF1E29BC224AB5A640B8EA5875AA9F46067C0907C6A45B7D1869`.
- `useful_ribbits-1.0.2-forge-1.20.1.jar` — 416,439 bytes; SHA-256 `2B56007A985B162477113BB2EA1776D9CE2CE602886EA21A88D8B2500D2DED5D`.
- `wandering_ribbit-4.0-forge.jar` — 98,047 bytes; SHA-256 `5BFD24A88C84D6948F72DC19153155CE99CA5BB4BAEF90ACADDA0AB0CA529E4A`.

The exact allowlisted archive members remain:

- `assets/guardribbits/geo/guard_ribbit.geo.json`
- `assets/guardribbits/textures/entity/guard_ribbit.png`
- `assets/useful_ribbits/geo/chef_ribbit.geo.json`
- `assets/useful_ribbits/textures/entities/chef_ribbit.png`
- `assets/useful_ribbits/geo/farmer_ribbit.geo.json`
- `assets/useful_ribbits/textures/entities/farmer_ribbit.png`
- `assets/useful_ribbits/geo/miner_ribbit.geo.json`
- `assets/useful_ribbits/textures/entities/miner_ribbit.png`
- `assets/wandering_ribbit/geo/wandering_ribbit.geo.json` — 3,277 bytes; SHA-256 `C86EA5F798F12C4AF172A2DF1939E318900B349463D6CA9E26A250134F6C8D08`
- `assets/wandering_ribbit/textures/entity/wandering_ribbit.png` — 6,233 bytes; SHA-256 `8E481FA8B4E4458ADB52D60AB3123801E612835B65CD362264C00801D84D681A`
- `assets/wandering_ribbit/models/custom/umbrella_leaf.json` — 3,841 bytes; SHA-256 `59506A4A35E71F5630A22F45332FB0813DF1BFF4A985096328E206DF9A5F5027`
- `assets/wandering_ribbit/textures/item/umbrella_leaf_item.png` — 305 bytes; SHA-256 `D595D40E69A838C7FF3BDC5D59DE92BC9C84B39A4A3A433D84590EC4FD1C04AD`
- `assets/wandering_ribbit/textures/item/umbrella_leaf_texture.png` — 548 bytes; SHA-256 `3505383EE2DA238B600C3A445F676209066307F5A2A690252179B767B50F99FF`

The Wandering donor contributes only the allowlisted entity model/texture and Chute custom model plus closed/open textures. Deterministic private assembly copies the entity visual, derives only the two Chute conventional models and item definitions needed by Minecraft 26.2, and leaves all outputs inside the ignored private boundary. No donor Java/classes, AI, procedures, combat, structures, camps, generation, sounds, animations, spawn-egg graphics, workstations, inventories, GUIs, or unrelated assets enter the build. Canary 8 derives the Wandering egg from the already authorized shared green Ribbits egg artwork, not from donor material. Derived outputs remain private-use-only and no relicensing claim is made. Source-only output remains truthful and contains no protected/donor-private material.

## Mynx Canary 7 to Canary 8 archive audit

The final private archive advances from 677 ZIP records (544 files and 133 directories) to 684 records (551 files and 133 directories): seven added files, zero removals, 15 changed common files, and 662 byte-identical common records. Uncompressed member payload advances from 4,511,621 to 4,539,607 bytes; compressed member bytes advance from 3,185,554 to 3,196,699. Two independent clean private builds are byte-identical at 3,333,563 bytes/SHA-256 `3A8E4FF06378265D949F01067672EB6BAEC56FCD0AD0AB57260B6F32F630067B`; two clean source-only builds are byte-identical at 1,263,483 bytes/SHA-256 `180A614AADA36ADF156628CCD9FFB7920806D78091CC26A31FD905B4BF094D23`.

The seven additions are exactly `assets/ribbits/items/wandering_ribbit_spawn_egg.json`, `assets/ribbits/models/item/wandering_ribbit_spawn_egg.json`, `MatchaCompassCatalog.class`, `MatchaCompassCatalog$RecipeCompass.class`, `WanderingRibbitCompassTrades.class`, `WanderingRibbitMatchaCompassTradeProvider.class`, and `WanderingRibbitBiomePolicy.class`. The 15 changed payloads are the English translation; `MatchaStackCatalog`, native-provider/curiosity, provider/materialization, Fabric bootstrap, Creative-entry, item-module, scheduler/spawn-site classes; both Wandering biome tags; and `fabric.mod.json`. Nothing else changed across the private C7→C8 boundary.

Both private trees contain exactly 346 files including 252 strict JSON files and total 2,729,540 bytes, up from 344/250/2,729,248. The two new resources are only the Wandering egg definition and conventional model; the shared green texture is byte-identical to the already authorized output. All archive paths remain unique and canonical, all resource/JAR validation passes, and the exact donor identities and five-member Wandering allowlist remain unchanged.

## Mynx Canary 6 to Canary 7 archive audit

The final private archive advances from 610 ZIP records (488 files and 122 directories) to 677 records (544 files and 133 directories): 56 added files, 11 added directories, zero removed files or directories, 18 changed common files, and 470 byte-identical common files. Its uncompressed member payload advances from 4,307,032 to 4,511,621 bytes; added payload contributes 198,035 bytes, the changed set advances from 80,561 to 87,115 bytes, and total uncompressed growth is 204,589 bytes. Compressed member bytes advance from 3,102,063 to 3,185,554, a growth of 83,491.

The final source-only archive advances from 226 records (153 files and 73 directories) to 286 records (201 files and 85 directories): 48 added files, 12 added directories, zero removed files or directories, the same 18 changed common files, and 135 byte-identical common files. Its uncompressed member payload advances from 1,594,711 to 1,784,695 bytes; added payload contributes 183,430 bytes, the changed set advances from 76,044 to 82,598 bytes, and total uncompressed growth is 189,984 bytes. Compressed member bytes advance from 1,115,037 to 1,189,798, a growth of 74,761.

These 48 added files are the complete source-only addition set and are also present in the private archive:

- `assets/ribbits/items/ribbit_village_explorer_map.json`
- `assets/ribbits/models/item/ribbit_village_explorer_map.json`
- `assets/ribbits/textures/item/ribbit_village_explorer_map.png`
- `assets/ribbits/textures/map/decorations/ribbit_village.png`
- `com/yungnickyoung/minecraft/ribbits/chute/ChuteAckState.class`
- `com/yungnickyoung/minecraft/ribbits/chute/ChuteEquipment.class`
- `com/yungnickyoung/minecraft/ribbits/chute/ChuteInputState.class`
- `com/yungnickyoung/minecraft/ribbits/chute/ChutePhysics.class`
- `com/yungnickyoung/minecraft/ribbits/chute/ChutePlayerAccess.class`
- `com/yungnickyoung/minecraft/ribbits/chute/ChuteServerController$PlayerState.class`
- `com/yungnickyoung/minecraft/ribbits/chute/ChuteServerController.class`
- `com/yungnickyoung/minecraft/ribbits/chute/ChuteStateMachine$Acknowledgement.class`
- `com/yungnickyoung/minecraft/ribbits/chute/ChuteStateMachine.class`
- `com/yungnickyoung/minecraft/ribbits/client/chute/ChuteClientController.class`
- `com/yungnickyoung/minecraft/ribbits/client/model/WanderingRibbitModel.class`
- `com/yungnickyoung/minecraft/ribbits/client/render/ChuteLeafRenderer.class`
- `com/yungnickyoung/minecraft/ribbits/client/render/RibbitVillageMapItemModel.class`
- `com/yungnickyoung/minecraft/ribbits/client/render/WanderingRibbitRenderer.class`
- `com/yungnickyoung/minecraft/ribbits/entity/WanderingRibbitEntity.class`
- `com/yungnickyoung/minecraft/ribbits/entity/goal/WanderingRibbitMoveToTargetGoal.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitMapOffer.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitNativeTradeProvider$Curiosity.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitNativeTradeProvider.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitTradeContext.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitTradeProvider$OfferCollector.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitTradeProvider.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitTradeProviders$Materialization.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitTradeProviders.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitTradeSnapshot$ProviderRange.class`
- `com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitTradeSnapshot.class`
- `com/yungnickyoung/minecraft/ribbits/fabric/client/WanderingRibbitClientHooks.class`
- `com/yungnickyoung/minecraft/ribbits/item/ChuteLeafItem.class`
- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/chute/ArmorSlotChuteMixin.class`
- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/chute/EquippableChuteMixin.class`
- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/chute/PlayerChuteDataMixin.class`
- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/chute/PlayerChuteMovementMixin.class`
- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/client/chute/ClientPlayerChuteMovementMixin.class`
- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/client/chute/KeyboardInputChuteMixin.class`
- `com/yungnickyoung/minecraft/ribbits/mixin/mixins/client/chute/LocalPlayerChuteTickMixin.class`
- `com/yungnickyoung/minecraft/ribbits/module/MapDecorationTypeModule.class`
- `com/yungnickyoung/minecraft/ribbits/network/payload/ChuteAckS2C.class`
- `com/yungnickyoung/minecraft/ribbits/network/payload/ChutePressC2S.class`
- `com/yungnickyoung/minecraft/ribbits/world/spawn/WanderingRibbitScheduler$SpawnSite.class`
- `com/yungnickyoung/minecraft/ribbits/world/spawn/WanderingRibbitScheduler.class`
- `com/yungnickyoung/minecraft/ribbits/world/spawn/WanderingRibbitSpawnerData.class`
- `data/ribbits/tags/worldgen/biome/allows_wandering_ribbit_spawns.json`
- `data/ribbits/tags/worldgen/biome/without_wandering_ribbit_spawns.json`
- `data/trinkets/tags/item/chest/cape.json`

The private archive adds exactly these eight further private-only files, and none enters source-only output:

- `assets/ribbits/geckolib/models/wandering_ribbit.geo.json`
- `assets/ribbits/items/chute_leaf.json`
- `assets/ribbits/items/chute_leaf_open.json`
- `assets/ribbits/models/item/chute_leaf.json`
- `assets/ribbits/models/item/chute_leaf_open.json`
- `assets/ribbits/textures/entity/wandering_ribbit.png`
- `assets/ribbits/textures/item/chute_leaf.png`
- `assets/ribbits/textures/item/chute_leaf_open.png`

The complete changed-file set is identical in both lanes:

- `assets/ribbits/lang/en_us.json`
- `com/yungnickyoung/minecraft/ribbits/RibbitsCommon.class`
- `com/yungnickyoung/minecraft/ribbits/client/RibbitsCommonClient.class`
- `com/yungnickyoung/minecraft/ribbits/fabric/RibbitsFabric.class`
- `com/yungnickyoung/minecraft/ribbits/fabric/client/ClientNetworkModuleFabric.class`
- `com/yungnickyoung/minecraft/ribbits/fabric/client/RibbitsFabricClient.class`
- `com/yungnickyoung/minecraft/ribbits/fabric/module/NetworkModuleFabric.class`
- `com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule$CreativeEntry.class`
- `com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule.class`
- `com/yungnickyoung/minecraft/ribbits/module/EntityTypeModule.class`
- `com/yungnickyoung/minecraft/ribbits/module/ItemModule.class`
- `com/yungnickyoung/minecraft/ribbits/module/NetworkModule.class`
- `com/yungnickyoung/minecraft/ribbits/network/ClientNetworkHandler.class`
- `com/yungnickyoung/minecraft/ribbits/network/ServerNetworkHandler.class`
- `com/yungnickyoung/minecraft/ribbits/world/loot/RibbitVillageExplorerMap.class`
- `data/ribbits/item_modifier/ribbit_village_explorer_result.json`
- `fabric.mod.json`
- `ribbits.mixins.json`

The private archive adds exactly these 11 directory records: `assets/ribbits/textures/map/`, `assets/ribbits/textures/map/decorations/`, `com/yungnickyoung/minecraft/ribbits/chute/`, `com/yungnickyoung/minecraft/ribbits/client/chute/`, `com/yungnickyoung/minecraft/ribbits/mixin/mixins/chute/`, `com/yungnickyoung/minecraft/ribbits/mixin/mixins/client/chute/`, `com/yungnickyoung/minecraft/ribbits/world/spawn/`, `data/trinkets/`, `data/trinkets/tags/`, `data/trinkets/tags/item/`, and `data/trinkets/tags/item/chest/`. Source-only adds those same 11 plus `data/ribbits/tags/worldgen/biome/`, which already existed as a directory record in private Canary 6. There are no removed member records of either kind.

All 335 Canary 6 private-only files remain present and byte-identical in Canary 7; none changes or disappears. Every one of the 201 Canary 7 source-only files is present in private output, 200 are byte-identical between lanes, and only the intentionally private-complete `assets/ribbits/lang/en_us.json` differs. The two public map PNG members retain their exact supplied identities recorded above. No unexpected private boundary was found.

Every archive has unique canonical forward-slash paths, validates every recorded CRC, and has no member extra field, member comment, or archive comment. Every record uses DEFLATE method 8, flag bits 2056, and timestamp `1980-01-01T00:00:00`; `META-INF/MANIFEST.MF` is first, every remaining record is lexicographically ordered, physical local-header offsets increase monotonically, every common file retains relative order, and no common member has metadata-only drift. Clean A and B outputs are byte-identical: each private Canary 7 JAR is 3,320,708 bytes with SHA-256 `6B18658C5A68D66623B9A388CC644E2F7A1B864E490B6F8B35D57FCD73A5BF74`, and each source-only Canary 7 JAR is 1,251,192 bytes with SHA-256 `1194083C64B3409D4752EBA292516E092C4A68E26A2114F5272FB4A4EC792061`.

## Mynx Canary 5 to Canary 6 archive audit

The final private archive advances from 606 to 610 ZIP records: 484 to 488 file records with 122 directory records unchanged, four added files, zero removals, six changed files, and 478 byte-identical common files. The four additions are exactly:

- `assets/ribbits/models/item/glowcap.json`
- `assets/ribbits/models/item/toadstool_heart.json`
- `assets/ribbits/textures/item/glowcap.png`
- `assets/ribbits/textures/item/toadstool_heart.png`

The six changed files are exactly:

- `assets/ribbits/items/glowcap.json`
- `assets/ribbits/items/toadstool_heart.json`
- `data/ribbits/loot_table/chests/sorcerer.json`
- `data/ribbits/structure/houses/small_house_brown_2.nbt`
- `data/ribbits/structure/houses/small_house_red_2.nbt`
- `fabric.mod.json`

The source-only archive advances from 218 to 226 ZIP records: 149 to 153 file records and 69 to 73 directory records. Its four added files are the same two generated-item models and two exact PNGs above; the four accompanying model/texture directory records are also new. It has zero removals, exactly three changed files (`assets/ribbits/items/glowcap.json`, `assets/ribbits/items/toadstool_heart.json`, and `fabric.mod.json`), and 146 byte-identical common files. In both archive classes every common entry retains its relative order and metadata, every Canary 6 record uses the fixed ZIP timestamp `1980-01-01T00:00:00-06:00`, every CRC validates, and no duplicate path exists.

Clean builds A and B are byte-identical: each private JAR is 3,222,195 bytes with SHA-256 `CBEEDD06BAB0D6809FD528662E4C7E4F11E308F241D60FCE0D6AD0F57ADD0759`, and each source-only JAR is 1,162,715 bytes with SHA-256 `0EF6117836F9AE70354C9859236F1E38216411842A65941597C274CC008515F5`. Both fresh private assembly trees contain exactly 336 files, including 245 strict JSON files, totaling 2,714,466 bytes; both tree and JAR validations pass. Focused validation passed all 102 JUnit tests across 19 suites and all 40 Python tests with no failures, errors, or skips. No unrelated private resource, class, model, umbrella, spawn egg, donor asset, trade, profession, or behavior payload changed.

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

Canary 8 does not implement a Naturalist or Regions Unexplored Wandering trade provider, rescued-baby capsule, Chute recipe or spawn egg, a Survival recipe/loot/trade for the Wandering egg, `chest/back` or broad Trinkets eligibility, new slot/group definition, vanilla glider component, automatic Sorcerer replacement after death, retrofit injection into existing huts, conversion of saved Witches, global Witch suppression, a whole swamp-hut replacement, asynchronous map search, a custom map GUI, a new registered map item, direct Toadstool Heart barrel fallback, Matcha cartographer Ribbit Village maps, changes to Matcha Witch Hut maps, donor behavior, generalized old-offer migration, dimension-aware homes, Fortune Blessing, Silver Bullion, Adamant/Netherite bullion or mount equipment, Netherite tool recycling, any Matcha trade beyond the three authorized optional compass buybacks, or Custom Portals recipe/behavior changes.

## Runtime evidence boundary

The user reports the Phase A profession functionality they tested appeared operational: villages generated, the expanded roster appeared to work, the Prospector typed egg existed, and its model rendered correctly. A naturally generated Prospector was not directly observed in that sample, so that observation is neither a failure nor a natural-spawn pass. No complete runtime matrix or exact logs were supplied, and the evidence does not uniquely bind exact Ribbits/YUNG's API artifact identities; it remains partial/unbound external evidence.

The retained canonical Canary 3 and YUNG's API Compat.2 binaries rehashed exactly against authoritative `main`, so the user's Phase B report is bound to that pair as a practical overall `PASS`; practical use looked sufficient to continue. The exhaustive profession/tier trade matrix was intentionally not executed, so no unexecuted trade row, component rejection, menu rotation, specialization, or restock edge case is marked observed. Canary 3 is not promoted and receives no managed-slot result from that report.

The reported failing artifact is unambiguously the canonical Canary 4 private artifact: version `4.1.6+26.2-mynx-canary4`, filename `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary4.jar`, 3,219,246 bytes, SHA-256 `DA7A37E0AEECE4D7633F0C5104298D76E066CD38DD5255FAE616DF95C5FBC712`, source checkpoint `cbc247054ecba937ad009dd651cde31fac504c22`. The user's supplied observation is therefore bound only to that exact identity as an external runtime `FAIL`: startup aborted at the invalid `@Shadow getWorldPos` before Phase C gameplay could run. The task made no Test Instance Manager transition, so the report supplies no managed deployment or slot result and no unreported gameplay row is inferred.

The user's statement that Phase C is good binds to the exact canonical Canary 6 cohort independently rehashed from authoritative records: private Ribbits Canary 6 `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary6.jar`, 3,222,195 bytes, SHA-256 `CBEEDD06BAB0D6809FD528662E4C7E4F11E308F241D60FCE0D6AD0F57ADD0759`, source `fe8f08bf128791f1d64f227524b9a0585ef333d5`; YUNG's API Compat.2 `YungsApi-26.2-Fabric-6.1.1-compat.2.jar`, 1,260,939 bytes, SHA-256 `FF22A6B509BA559988D7A9352DC94AC612C4B099517ACAC7DA7C81322D797ED7`; accepted Custom Portals C2 `custom-portals-26.2-4.0.0+26.2-port-canary2.jar`, 910,246 bytes, SHA-256 `13FD0E76748FCC3EF963BCC2F4A820D5FC2D473A90AD5D5988CBE0129C2BE148`; and Matcha Heart Death C11 `matcha-heart-death-compat-0.1.10-canary11.jar`, 39,105 bytes, SHA-256 `A0570179F85D32EC6740D9136AD50890B9C797500661CD2ED2325DA5B6B1E4A9`. It is recorded only as a scoped external practical `PASS`; no individual Phase C or retained regression row is inferred, no managed deployment or Test Slot result is assigned, and no release is accepted or promoted from it.

Canary 7 was not deployed or launched and remains `RUNTIME_UNTESTED`. Its successful build, tests, deterministic assembly, archive/codec/dependency/donor checks, production-equivalent Knot/Mixin application, MME wrapper-order tests, and static Compass Ribbon audit must not be reported as Minecraft gameplay runtime correctness.

Canary 8 was not deployed or launched and remains independently `RUNTIME_UNTESTED`. Its successful build, 153-test Java suite, 41-test private-resource suite, deterministic source/private reconstruction, archive/resource/component/tag checks, and production-equivalent Knot/Mixin application must not be reported as Minecraft gameplay correctness. Its exact visible and instrumented runtime procedure is in `TESTING.md`.

# Mynx Deviations

This file records intentional departures from the faithful Minecraft Java 26.2 Ribbits port. The exact faithful-port baseline is private Canary 2: version `4.1.6+26.2-port-canary2`, artifact `ribbits-private-reconstruction-4.1.6+26.2-port-canary2.jar`, 3,124,301 bytes, SHA-256 `0AD73B7B61C6EE792EC1745056563641767AFE6811C0FDF2D3C99123C3F289DC`, implementation checkpoint `efe1970d2447aea4913e67f55c0c6b83cc36c5bb`. It remains historical provenance, not an accepted or rollback release.

## Canary 1 deviations retained by Canary 2

- The private `ribbits:chests/fisherman_main` table's weight-15 no-loot sentinel used the item entry `minecraft:air`, which Minecraft 26.2 decodes as invalid. It is faithfully represented as a weight-15 `minecraft:empty` loot entry.
- The private `ribbits:chests/sorcerer` table's weight-1 no-loot sentinel used the same invalid item entry. It is faithfully represented as a weight-1 `minecraft:empty` loot entry. The existing `minecraft:set_potion` migration to `minecraft:strong_leaping` is unchanged.
- Four visual professions are registered without separate entity types: `ribbits:chef`, `ribbits:farmer`, `ribbits:prospector`, and `ribbits:guard`.
- Internal profession `ribbits:nitwit` keeps its registry and save identity and is displayed to users as Musician.
- Natural Ribbit villages use one explicit equal-weight ordered pool: `ribbits:nitwit`, `ribbits:gardener`, `ribbits:fisherman`, `ribbits:merchant`, `ribbits:chef`, `ribbits:farmer`, `ribbits:prospector`, and `ribbits:guard`. `ribbits:sorcerer` remains registered and command/spawn-egg accessible but is excluded from natural Ribbit-village assignment.
- Profession records now provide a model and texture identity. The original five professions retain their exact models and shared `ribbits:textures/entity/ribbit.png`; the four new professions use private assembled composite textures.
- Rain rendering uses deterministic 256×128 composite atlases and profession-specific umbrella models. Each donor model stays in the first atlas half; the pristine Ribbits shared texture and each of the three existing Musician umbrella variants occupy the second half. This preserves each profession's accessories and all three umbrella appearances without depending on a new GeckoLib render-layer API.
- Chef, Farmer, Prospector, and Guard receive generic Ribbit movement/idle behavior, initialize with `RibbitInstrumentModule.NONE`, do not enter the shared instrument-model path, and receive no donor behavior. Guard's spear and shield are cosmetic.
- Four typed spawn eggs use the existing authorized shared green private Ribbit egg texture and generated-item model. No donor egg graphic is imported.
- Merchant and Fisherman offers, saved offers, interaction behavior, and the existing restock implementation remain unchanged. Gardener, Sorcerer, Musician, Chef, Farmer, Prospector, and Guard have no newly added trades.

Canary 2 deliberately does not implement redesigned trades, Gardener conversions, Farmer field clearing, Prospector exchanges, Guard or Quartermaster trades, Chef foods/levels/XP, strict two-restocks-per-day persistence, Sorcerer progression, Benzene, Portal Catalysts, Estus Flasks, Blessings, witch-hut Sorcerers, witch suppression, witch-hut or Ribbit Village maps, Matcha integration, or Custom Portals integration.

## Canary 2 runtime hardening

- Creative-tab contents now come from one ordered 24-entry table. Before emitting any stack, the generator verifies every resolved item against its intended `ribbits` registry ID and rejects duplicate item identities or registry keys. Mossy Oak Slab remains registered, keeps its position between Mossy Oak Stairs and Fence, and has one contribution.
- The coordinated YUNG's API `6.1.1-compat.2` dependency carries upstream's narrow enhanced-beardifier concurrency fix: Beardifier state retains piece and junction lists, while each density call obtains fresh local iterators. No terrain adaptation is disabled, and nonempty contribution formulas remain unchanged.
- The duplicate-stack report came from an unavailable runtime pair whose hashes differ from authoritative Canary 1. The one canonical Canary 1 creative contribution and retained artifact were not rewritten or misclassified; Canary 2 adds a fail-closed invariant around the reconstructed path.

## Private donor boundary

The two donor JARs are immutable local references beneath `originals/mods` and are read externally from the authoritative checkout; they are never copied into the task worktree or private output:

- `GuardRibbits-1.20.1-Fabric-1.0.4.jar` — 166,896 bytes; SHA-256 `52F1E184DC12CF1E29BC224AB5A640B8EA5875AA9F46067C0907C6A45B7D1869`.
- `useful_ribbits-1.0.2-forge-1.20.1.jar` — 416,439 bytes; SHA-256 `2B56007A985B162477113BB2EA1776D9CE2CE602886EA21A88D8B2500D2DED5D`.

The exact allowlisted archive members are:

- `assets/guardribbits/geo/guard_ribbit.geo.json`
- `assets/guardribbits/textures/entity/guard_ribbit.png`
- `assets/useful_ribbits/geo/chef_ribbit.geo.json`
- `assets/useful_ribbits/textures/entities/chef_ribbit.png`
- `assets/useful_ribbits/geo/farmer_ribbit.geo.json`
- `assets/useful_ribbits/textures/entities/farmer_ribbit.png`
- `assets/useful_ribbits/geo/miner_ribbit.geo.json`
- `assets/useful_ribbits/textures/entities/miner_ribbit.png`

No donor Java or class files, AI, procedures, combat behavior, targeting, alerts, attacks, structures, world generation, sounds, animations, spawn-egg graphics, workstations, inventories, beds, chests, GUIs, mines, hoists, or unrelated assets are imported. Donor-derived outputs are private-use-only and nonredistributable; this work makes no relicensing claim.

## Canary 2 to Mynx Canary 1 archive audit

The complete uncompressed payload inventory changed from 434 to 462 entries: 28 additions, zero removals, and 22 changed payloads.

Added payloads:

- `assets/ribbits/geckolib/models/chef_ribbit.geo.json`
- `assets/ribbits/geckolib/models/farmer_ribbit.geo.json`
- `assets/ribbits/geckolib/models/guard_ribbit.geo.json`
- `assets/ribbits/geckolib/models/prospector_ribbit.geo.json`
- `assets/ribbits/geckolib/models/umbrella/chef/umbrella_1.geo.json`
- `assets/ribbits/geckolib/models/umbrella/chef/umbrella_2.geo.json`
- `assets/ribbits/geckolib/models/umbrella/chef/umbrella_3.geo.json`
- `assets/ribbits/geckolib/models/umbrella/farmer/umbrella_1.geo.json`
- `assets/ribbits/geckolib/models/umbrella/farmer/umbrella_2.geo.json`
- `assets/ribbits/geckolib/models/umbrella/farmer/umbrella_3.geo.json`
- `assets/ribbits/geckolib/models/umbrella/guard/umbrella_1.geo.json`
- `assets/ribbits/geckolib/models/umbrella/guard/umbrella_2.geo.json`
- `assets/ribbits/geckolib/models/umbrella/guard/umbrella_3.geo.json`
- `assets/ribbits/geckolib/models/umbrella/prospector/umbrella_1.geo.json`
- `assets/ribbits/geckolib/models/umbrella/prospector/umbrella_2.geo.json`
- `assets/ribbits/geckolib/models/umbrella/prospector/umbrella_3.geo.json`
- `assets/ribbits/items/ribbit_chef_spawn_egg.json`
- `assets/ribbits/items/ribbit_farmer_spawn_egg.json`
- `assets/ribbits/items/ribbit_guard_spawn_egg.json`
- `assets/ribbits/items/ribbit_prospector_spawn_egg.json`
- `assets/ribbits/models/item/ribbit_chef_spawn_egg.json`
- `assets/ribbits/models/item/ribbit_farmer_spawn_egg.json`
- `assets/ribbits/models/item/ribbit_guard_spawn_egg.json`
- `assets/ribbits/models/item/ribbit_prospector_spawn_egg.json`
- `assets/ribbits/textures/entity/chef_ribbit.png`
- `assets/ribbits/textures/entity/farmer_ribbit.png`
- `assets/ribbits/textures/entity/guard_ribbit.png`
- `assets/ribbits/textures/entity/prospector_ribbit.png`

Changed payloads:

- `assets/ribbits/lang/en_us.json`
- `com/yungnickyoung/minecraft/ribbits/client/model/RibbitModel.class`
- `com/yungnickyoung/minecraft/ribbits/data/RibbitData.class`
- `com/yungnickyoung/minecraft/ribbits/data/RibbitProfession.class`
- `com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity.class`
- `com/yungnickyoung/minecraft/ribbits/entity/RibbitEntity$1.class`
- `com/yungnickyoung/minecraft/ribbits/entity/RibbitPickResult.class`
- `com/yungnickyoung/minecraft/ribbits/entity/RibbitPickResult$Egg.class`
- `com/yungnickyoung/minecraft/ribbits/item/RibbitSpawnEggDispenseItemBehavior.class`
- `com/yungnickyoung/minecraft/ribbits/item/RibbitSpawnEggItem.class`
- `com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule.class`
- `com/yungnickyoung/minecraft/ribbits/module/ItemModule.class`
- `com/yungnickyoung/minecraft/ribbits/module/RibbitProfessionModule.class`
- `com/yungnickyoung/minecraft/ribbits/module/RibbitUmbrellaTypeModule.class`
- `data/ribbits/loot_table/chests/fisherman_main.json`
- `data/ribbits/loot_table/chests/sorcerer.json`
- `data/ribbits/structure/ribbits/ribbit_fisherman.nbt`
- `data/ribbits/structure/ribbits/ribbit_gardener.nbt`
- `data/ribbits/structure/ribbits/ribbit_merchant.nbt`
- `data/ribbits/structure/ribbits/ribbit_nitwit.nbt`
- `data/ribbits/structure/ribbits/ribbit_sorcerer.nbt`
- `fabric.mod.json`

No payload was removed. No unrelated protected-resource drift was found.

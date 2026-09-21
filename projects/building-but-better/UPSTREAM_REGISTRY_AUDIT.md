# Upstream Registry Audit

This audit records the active registrations in the exact official `bbb-fabric-2.0pre4.jar` (SHA-256 `1e7ae114aaec53475133e11c607fc65dce493bba5897eaf0044d53959b508fc0`) and the deliberate Minecraft 26.2 preservation subset. Counts come from the packaged runtime registries, not a naïve regex over source comments or dormant declarations.

## Count reconciliation

| Registry | Upstream active | Standalone retained | Optional Enderscape extension | Upstream active removed |
|---|---:|---:|---:|---:|
| Blocks | 247 | 160 historical baseline + 11 Pale Oak extension | 33 provider-derived | 87 |
| Items | 243 | 161 historical baseline + 11 Pale Oak extension | 33 provider-derived | 82 |

The retained block set is the exact Cartesian product below plus four standalone blocks:

- 12 port wood materials: `oak`, `spruce`, `birch`, `jungle`, `acacia`, `dark_oak`, `crimson`, `warped`, `mangrove`, `bamboo`, `cherry`, `pale_oak`.
- 11 forms for every wood: `balustrade`, `lattice`, `wall`, `beam`, `beam_stairs`, `beam_slab`, `support`, `pallet`, `frame`, `lantern`, `trim`.
- 7 stone materials: `stone`, `blackstone`, `deepslate`, `nether_brick`, `sandstone`, `red_sandstone`, `quartz`.
- 5 forms for every stone: `column`, `urn`, `moulding`, `fence`, `frame`.
- Standalone blocks: `brazier`, `soul_brazier`, `rope`, `iron_fence`.
- Every retained block has a retained block item; `hammer` is the one additional item.

The historical reconciliation remains `(11 × 11) + (7 × 5) + 4 = 160` blocks and `160 + hammer = 161` items. The explicit 26.2-native Pale Oak extension adds eleven blocks and their block items, producing `(12 × 11) + (7 × 5) + 4 = 171` blocks and `171 + hammer = 172` items. When the complete explicit Enderscape provider set is present, three more eleven-form families append 33 blocks and block items, producing 204 blocks and 205 items. `src/porting/curated-registry.json` is the machine-readable authority for both the standalone and provider-present formulas.

Pale Oak is absent from the pristine `2.0pre4` registry and remains absent from the historical count. It is now a separately identified complete port-native extension; no claim is made that its eleven blocks appeared in the upstream artifact. Pale Oak Layers and Ladders remain excluded. The retained `bbb:rope` registry/item identity remains unchanged; C4 adds a narrow BBB-owned state-aware vertical climbing and one-segment pay-out/reel-in mechanic without changing the curated registry count.

The optional Enderscape rows in the table are also extensions, not recovered `2.0pre4` registrations, so they do not alter the historical 87-block/82-item removal accounting.

## Explicit Enderscape provider audit

The audited provider is the exact `enderscape-fabric-3.0.2+mc26.2.jar`, SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`. Its registrations and packaged models/textures establish these literal bindings:

| Material | Source planks | Axial/beam material |
|---|---|---|
| `veiled` | `enderscape:veiled_planks` | `enderscape:stripped_veiled_log` |
| `celestial` | `enderscape:celestial_planks` | `enderscape:stripped_celestial_stem` |
| `murublight` | `enderscape:murublight_planks` | `enderscape:stripped_murublight_stem` |

For each material BBB explicitly registers balustrade, lattice, wall, beam, beam stairs, beam slab, support, pallet, frame, lantern, and trim under `bbb:<material>_<form>`. There is no registry-name scanning or inferred family discovery. The standalone 171-block/172-item registry is established first. With the literal `enderscape` provider loaded, an immediate six-ID lookup handles provider-first ordering and a callback filtered to those same six IDs handles late provider registration; the 33/33 extension is committed only after the complete set is available. The always-enabled built-in pack `bbb:enderscape_wood_families` is registered only in that provider-present path. If the provider is absent, BBB retains the accepted standalone registry and does not register invalid families or their pack.

This task makes no IBF changes. Integration of these three new BBB families into IBF remains a separate successor.

## Removed block IDs (87)

Wood and mosaic layers (12):

```text
oak_layer
spruce_layer
birch_layer
jungle_layer
acacia_layer
dark_oak_layer
crimson_layer
warped_layer
mangrove_layer
bamboo_layer
bamboo_mosaic_layer
cherry_layer
```

Ladders (11):

```text
oak_ladder
spruce_ladder
birch_ladder
jungle_ladder
acacia_ladder
dark_oak_ladder
crimson_ladder
warped_ladder
mangrove_ladder
bamboo_ladder
cherry_ladder
```

Small stone blocks and their internal wall companions (14):

```text
stone_block
wall_stone_block
blackstone_block
wall_blackstone_block
deepslate_block
wall_deepslate_block
nether_brick_block
wall_nether_brick_block
sandstone_block
wall_sandstone_block
red_sandstone_block
wall_red_sandstone_block
quartz_block
wall_quartz_block
```

General material layers (44):

```text
moss_layer
stone_layer
cobblestone_layer
mossy_cobblestone_layer
smooth_stone_layer
polished_stone_layer
stone_tile_layer
stone_brick_layer
mossy_stone_brick_layer
granite_layer
polished_granite_layer
diorite_layer
polished_diorite_layer
andesite_layer
polished_andesite_layer
cobbled_deepslate_layer
polished_deepslate_layer
deepslate_brick_layer
deepslate_tile_layer
brick_layer
mud_brick_layer
sandstone_layer
smooth_sandstone_layer
red_sandstone_layer
smooth_red_sandstone_layer
prismarine_layer
prismarine_brick_layer
dark_prismarine_layer
nether_brick_layer
red_nether_brick_layer
blackstone_layer
polished_blackstone_layer
polished_blackstone_brick_layer
end_stone_brick_layer
purpur_layer
quartz_layer
cut_copper_layer
exposed_cut_copper_layer
weathered_cut_copper_layer
oxidized_cut_copper_layer
waxed_cut_copper_layer
waxed_exposed_cut_copper_layer
waxed_weathered_cut_copper_layer
waxed_oxidized_cut_copper_layer
```

Additional stone derivatives (6):

```text
polished_stone
polished_stone_stairs
polished_stone_slab
stone_tiles
stone_tile_stairs
stone_tile_slab
```

Source remnants such as `tall_oak_door`, `copper_gateway`, and `roofing` are not part of the exact JAR's active registry and therefore are neither retained nor counted among the 87 active block removals.

## Removed item IDs (82)

The removed item set contains every removed block ID except the seven internal `wall_*_block` implementations, which did not have active block items. It also contains the two item-only IDs `bbb` and `chisel`.

```text
oak_layer
spruce_layer
birch_layer
jungle_layer
acacia_layer
dark_oak_layer
crimson_layer
warped_layer
mangrove_layer
bamboo_layer
bamboo_mosaic_layer
cherry_layer
oak_ladder
spruce_ladder
birch_ladder
jungle_ladder
acacia_ladder
dark_oak_ladder
crimson_ladder
warped_ladder
mangrove_ladder
bamboo_ladder
cherry_ladder
stone_block
blackstone_block
deepslate_block
nether_brick_block
sandstone_block
red_sandstone_block
quartz_block
moss_layer
stone_layer
cobblestone_layer
mossy_cobblestone_layer
smooth_stone_layer
polished_stone_layer
stone_tile_layer
stone_brick_layer
mossy_stone_brick_layer
granite_layer
polished_granite_layer
diorite_layer
polished_diorite_layer
andesite_layer
polished_andesite_layer
cobbled_deepslate_layer
polished_deepslate_layer
deepslate_brick_layer
deepslate_tile_layer
brick_layer
mud_brick_layer
sandstone_layer
smooth_sandstone_layer
red_sandstone_layer
smooth_red_sandstone_layer
prismarine_layer
prismarine_brick_layer
dark_prismarine_layer
nether_brick_layer
red_nether_brick_layer
blackstone_layer
polished_blackstone_layer
polished_blackstone_brick_layer
end_stone_brick_layer
purpur_layer
quartz_layer
cut_copper_layer
exposed_cut_copper_layer
weathered_cut_copper_layer
oxidized_cut_copper_layer
waxed_cut_copper_layer
waxed_exposed_cut_copper_layer
waxed_weathered_cut_copper_layer
waxed_oxidized_cut_copper_layer
polished_stone
polished_stone_stairs
polished_stone_slab
stone_tiles
stone_tile_stairs
stone_tile_slab
bbb
chisel
```

## Hammer interaction matrix

All retained Hammer actions preserve the published zero-durability-cost behavior. In `2.0pre4`, only the removed Layer action damages the Hammer.

| Target | Retained behavior | Input distinction | Hammer durability |
|---|---|---|---:|
| Balustrade | Toggle the struck upper or lower section; toggle tilted state | normal use selects top/bottom by hit; sneak-use toggles tilt | 0 |
| Frame | Cycle center-stick direction; reset center | Hammer use cycles; ordinary attack/punch resets and is not Hammer-gated | 0 |
| Column | Toggle one of four quarter-shell bands selected by hit position around its axis | use | 0 |
| Pallet | Toggle one of the two board layers selected by hit position/orientation | use | 0 |
| Support | Toggle the support brace | use | 0 |
| Wooden lantern | Toggle hanging state | use | 0 |
| Moulding | Toggle dentil state | use | 0 |
| Stone fence | Toggle side fill; toggle pillar for straight north/south or east/west runs | normal use toggles side fill; sneak-use toggles eligible pillar | 0 |

The removed Hammer branches are:

- BBB ladder style cycling;
- BBB layer subtraction, dropped layer item, and one point of Hammer damage.

There is no Hammer interaction in `2.0pre4` for rope, either brazier, iron fence, lattice, trim, beams, beam slabs, beam stairs, wooden walls, or urns. Their non-Hammer interactions remain separate behavior: lattices handle plant/shears behavior, and urns handle dirt/shovel behavior.

The 26.2 port routes Hammer callbacks only through the retained Hammer-capable block contract. It does not retain a BBB Layer or BBB Ladder implementation and does not branch on removed registry IDs.

## Resource closure

The accepted standalone resource closure in `bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.6.jar` (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`) contains:

- 171 retained/generated blockstates;
- 172 generated 26.2 item definitions;
- 171 retained/generated block loot tables;
- 224 recipes that reference retained BBB IDs and no removed BBB IDs;
- 140 migrated recipe-unlock advancements whose rewards resolve to staged recipes;
- 1,132 transitive model JSON files;
- 248 transitive/generated textures/metadata files;
- 244 PNG textures, all binary-alpha, of which exactly 112 contain transparent pixels;
- 2 filtered language files;
- 44 filtered/generated tag files, including vanilla block/item slab, stair, and wall integration tags;
- 2,305 staged files before `bbb-resource-staging.json`, or 2,306 including the report.

The historical selected ARR dependency closure remains exactly 1,467 paths. The current blockstates, models, and textures/sidecars total 1,551 files after adding 84 port-native Pale Oak resources: eleven blockstates, fifty-nine model JSON files, and fourteen generated authored texture sheets. Those sheets map only colors proven to belong to the actual Minecraft 26.2 Cherry plank palette onto the corresponding Pale Oak palette; transparent pixels and unmatched material-independent details—including the lantern's 36 opaque glow/metal pixels—remain unchanged. The Beam side/end sheets preserve BBB's original Beam layout and are palette-recolored like the other authored Pale Oak sheets. Pale Oak Lattice preserves its canonical shared `cherry_leaves` multipart state/model rather than inventing the nonexistent `pale_oak_leaves` state. The official `dark_oak_lattice` blockstate mistakenly points at Oak left/middle/right geometry; ignored staging output corrects just those three references to the authored Dark Oak models so the material variant remains distinct. Minecraft 26.2 derives section layers from sprite transparency, so the verifier rejects unsupported/inert model `render_type` metadata, malformed or unresolved model/texture references, and partial alpha. These are static asset checks, not runtime visual validation.

One apparent removed-ID residue is intentional: retained Stone Column models reference `textures/block/polished_stone.png` as their shared particle texture. No `polished_stone` block or item is retained. Likewise, model paths containing `_layer1` through `_layer4` for columns and `_layer1`/`_layer2` for pallets are internal model-composition parts, not the removed BBB Layer block family.

For the Enderscape candidate, the exact accepted dev.6 JAR above is the hash-guarded base input, and all accepted base resource bytes are verified unchanged. The optional built-in pack adds exactly 33 blockstates, 177 model JSON files, 33 item definitions, 39 recipes, 27 recipe-unlock advancements, 33 block loot tables, and 42 generated PNG textures. Inspection found all three provider plank sheets to be opaque 16-by-16 textures: Celestial has the same seven-color palette cardinality as the audited Cherry mask, while Veiled and Murublight each have ten colors. The generator therefore maps the seven verified Cherry wood ranks to seven luminance-ranked colors sampled across each real provider palette; it does not claim to reproduce the provider plank grain or all ten Veiled/Murublight variants. It preserves BBB geometry, alpha, and unmatched material-independent detail, including the 36 Lantern glow/metal pixels. Provider planks and the unstripped log/stem textures used by the accepted lattice visual template remain literal external references where that model contract calls for them; the actual block-property and recipe axial bindings use the requested stripped log/stems. Static closure checks resolve every Minecraft, BBB, and Enderscape model/texture reference without a missing-resource fallback and verify the recipe, loot, tag, and explicit material bindings.

Enderscape's audited license marks its packaged `assets/` resources All Rights Reserved. The staging task therefore verifies the exact provider hash and license before reading those resources, writes generated derivatives only to ignored build output and the retained local candidate, and copies no raw `assets/enderscape` or `data/enderscape` tree. The production boundary check also rejects bundled `net/penumbra/enderscape` classes and nested JARs. `originals/` remains untouched, and neither the generated resources nor the local-only artifact are tracked or redistributable source.

Observed validation comprises the passing static/unit/resource/JAR boundary checks, six required provider-present Fabric GameTests, and one required provider-absent Fabric GameTest. The provider-present tests cover exact family and ID counts, source identity, form/state/Hammer/waterlogging behavior contracts, lattice/wall/beam/lantern specializations, fuel, recipes, loot, and tags; the absent-provider test covers clean 171/172 startup and accepted data closure. These are controlled test observations, not gameplay validation.

Generated resource staging remains ignored build output under `build/`; only the finalized ignored local artifact is retained, and neither is tracked or redistributable source.

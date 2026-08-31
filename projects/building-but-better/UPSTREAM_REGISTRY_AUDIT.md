# Upstream Registry Audit

This audit records the active registrations in the exact official `bbb-fabric-2.0pre4.jar` (SHA-256 `1e7ae114aaec53475133e11c607fc65dce493bba5897eaf0044d53959b508fc0`) and the deliberate Minecraft 26.2 preservation subset. Counts come from the packaged runtime registries, not a naïve regex over source comments or dormant declarations.

## Count reconciliation

| Registry | Upstream active | Retained | Removed |
|---|---:|---:|---:|
| Blocks | 247 | 160 | 87 |
| Items | 243 | 161 | 82 |

The retained block set is the exact Cartesian product below plus four standalone blocks:

- 11 wood materials: `oak`, `spruce`, `birch`, `jungle`, `acacia`, `dark_oak`, `crimson`, `warped`, `mangrove`, `bamboo`, `cherry`.
- 11 forms for every wood: `balustrade`, `lattice`, `wall`, `beam`, `beam_stairs`, `beam_slab`, `support`, `pallet`, `frame`, `lantern`, `trim`.
- 7 stone materials: `stone`, `blackstone`, `deepslate`, `nether_brick`, `sandstone`, `red_sandstone`, `quartz`.
- 5 forms for every stone: `column`, `urn`, `moulding`, `fence`, `frame`.
- Standalone blocks: `brazier`, `soul_brazier`, `rope`, `iron_fence`.
- Every retained block has a retained block item; `hammer` is the one additional item.

Thus `(11 × 11) + (7 × 5) + 4 = 160` blocks, and `160 + hammer = 161` items. `src/porting/curated-registry.json` is the machine-readable authority for this formula.

Pale Oak is absent because it is not part of the `2.0pre4` registry. It is a future extension only. Rope is retained at its stock BBB baseline without a new behavior system.

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

The hash-guarded staging pipeline starts from the retained registry rather than copying the JAR wholesale. Its current verified closure contains:

- 160 retained blockstates;
- 161 generated 26.2 item definitions backed by retained upstream item models;
- 160 retained block loot tables;
- 211 recipes that reference retained BBB IDs and no removed BBB IDs;
- 131 migrated recipe-unlock advancements whose rewards resolve to staged recipes;
- 1,073 transitive model JSON files;
- 234 transitive textures/metadata files;
- 230 PNG textures, all binary-alpha, of which exactly 106 contain transparent pixels;
- 2 filtered language files;
- 44 filtered/generated tag files, including vanilla block/item slab, stair, and wall integration tags;
- 2,177 staged files before `bbb-resource-staging.json`, or 2,178 including the report.

The blockstates, models, and textures/sidecars total exactly 1,467 selected ARR dependency paths. The official `dark_oak_lattice` blockstate mistakenly points at Oak left/middle/right geometry; ignored staging output corrects just those three references to the authored Dark Oak models so the material variant remains distinct. Minecraft 26.2 derives section layers from sprite transparency, so the verifier rejects unsupported/inert model `render_type` metadata and instead audits the PNG alpha values. These are static asset checks, not runtime visual validation.

One apparent removed-ID residue is intentional: retained Stone Column models reference `textures/block/polished_stone.png` as their shared particle texture. No `polished_stone` block or item is retained. Likewise, model paths containing `_layer1` through `_layer4` for columns and `_layer1`/`_layer2` for pallets are internal model-composition parts, not the removed BBB Layer block family. All generated content remains build output under `build/`, not tracked or redistributable source.

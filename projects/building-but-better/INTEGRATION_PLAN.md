# Integration Plan

This file defines the compatibility seam established by the BBB 26.2 port, including BBB's explicit optional Enderscape provider integration. It does not implement downstream integration and does not authorize changes to Inventory Bridge Framework (IBF), Clutter No More (CNM), or the Workbench runtime profile.

## Fabric-only port boundary

The 26.2 project registers curated BBB content directly with Fabric/Minecraft APIs. Upstream's common/fabric/forge Architectury split and embedded MidnightLib are intentionally not retained. The port keeps upstream registry IDs for every selected block/item so worlds, recipes, tags, and resources use the stable `bbb:<id>` namespace within the curated scope.

`BBBContent` is the source of truth for family membership:

- `WoodFamily` maps one literal wood material, source plank block, and axial/beam material to its 11 retained forms.
- `StoneFamily` maps one literal stone material and its foundation/masonry source blocks to its 5 retained forms.
- `BeamFamily` explicitly binds material, source planks, axial/beam material, beam, directional beam slab, beam stairs, and wooden wall.

The Beam contract exposes the canonical beam axis from the beam's `RotatedPillarBlock.AXIS` state and the slab material axis from the directional slab's `FACING` state. Consumers must use these records/state accessors. They must not derive material or geometry relationships from registry-name prefixes, suffixes, `split`, `substring`, or broad ID switches.

## Separate IBF/CNM follow-up

Inventory Bridge Framework is intended to consolidate intuitive inventory and storage variants. It does not require exact block-behavior equivalence or exact recipe-value equivalence, and every material family must remain literal and audited. The planned ShapeMap families are:

- stone, per retained stone material: `Column ↔ Frame ↔ Urn ↔ Fence ↔ Moulding`;
- wood family A, per retained wood material: `vanilla Fence ↔ vanilla Fence Gate ↔ BBB Lattice ↔ BBB Frame`;
- wood family B, per retained wood material: `BBB Trim ↔ BBB Balustrade ↔ BBB Support ↔ BBB Pallet`;
- metal: `Iron Bars ↔ Chain ↔ BBB Iron Fence`.

The intentional recipe-value imbalance in the metal family is not something the BBB baseline should normalize.

For CNM × Nibaru, Beam is planned as a directional material family. Its native BBB Beam, Beam Slab, Beam Stairs, and authored BBB Wooden Wall remain the canonical members. A later integration may generate CNM Vertical Slab and Step geometries, followed eventually by a universal CNM-owned Layer geometry conceptually derived from BBB's quarter-thickness, six-direction Layer design. Original `bbb:*_layer` blocks/items stay removed, CNM owns the eventual material quantity/drop semantics, and the Hammer has no Layer interaction.

A future task may connect the BBB metadata seam to IBF and CNM. That task should:

1. start from the accepted BBB artifact and its exact source commit;
2. inspect the current IBF/CNM extension points rather than assuming their APIs;
3. consume `BBBContent.BEAM_FAMILIES` or a narrow public adapter built over it;
4. preserve all 12 standalone material variants exactly and treat Veiled, Celestial, and Murublight as three explicit optional families only when Enderscape is present;
5. use blockstate axis/facing metadata for orientation-sensitive storage/crafting behavior;
6. add focused compatibility tests for beam, slab, stairs, and wall relationships;
7. build a new canary and obtain separate runtime validation.

This BBB port makes **no** IBF or CNM source changes. It also does not claim that either mod already recognizes BBB beams.

## Pale Oak extension

Pale Oak is a Minecraft 26.2-native extension, not historical `2.0pre4` content. It is a complete eleven-form wood family—balustrade, lattice, wall, beam, beam stairs, beam slab, support, pallet, frame, lantern, and trim—represented by the same explicit metadata as every existing family. Layers and ladders remain excluded. Its ignored generated resource closure uses the retained Cherry geometry/state layouts, direct Minecraft 26.2 Pale Oak log textures where the model contract permits, and fourteen Pale Oak-specific authored sheets generated from the actual vanilla Pale Oak plank palette while preserving BBB transparency and non-wood details, including BBB's authored Beam side/end artwork.

## Explicit optional Enderscape extension

Enderscape `3.0.2+mc26.2` is an optional, explicitly named provider. BBB does not scan registry names or infer materials. The integration accepts exactly these six provider blocks:

| BBB family | Source planks | Axial/beam material |
|---|---|---|
| `veiled` | `enderscape:veiled_planks` | `enderscape:stripped_veiled_log` |
| `celestial` | `enderscape:celestial_planks` | `enderscape:stripped_celestial_stem` |
| `murublight` | `enderscape:murublight_planks` | `enderscape:stripped_murublight_stem` |

Each provider material receives exactly the same eleven forms as Pale Oak: balustrade, lattice, wall, beam, beam stairs, beam slab, support, pallet, frame, lantern, and trim. IDs follow the stable `bbb:<material>_<form>` convention. This adds 33 blocks and 33 block items only in the provider-present case: standalone BBB remains exactly 171 blocks/172 items, while BBB with the complete provider set is exactly 204 blocks/205 items.

Initialization first checks Fabric Loader for the literal `enderscape` mod ID. If it is absent, no provider lookup, resource-pack registration, or optional BBB registration occurs. If it is present, BBB registers the always-enabled built-in pack `bbb:enderscape_wood_families`, installs a block-registry callback filtered to the six identifiers above, and immediately performs the same six-block lookup. The immediate lookup covers a provider that registered first; the callback covers a provider that registers later. Family registration begins only after all six exact blocks resolve, and server/client readiness checks fail closed if Enderscape claims to be loaded but its required block set is incomplete. This ordering mechanism remains an explicit provider adapter, not discovery by registry-name scanning.

The resource pipeline is equally explicit. Its standalone input is the accepted `bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.6.jar` (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`), whose staged base resource bytes remain unchanged and which remains the accepted rollback baseline pending an explicit owner decision. Its provider input is the exact `enderscape-fabric-3.0.2+mc26.2.jar` (SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`). The build verifies both files and Enderscape's audited code/resource license boundary before opening protected resources. It then derives the three narrow BBB closures from retained BBB templates and the real provider materials. No raw `assets/enderscape` or `data/enderscape` namespace is copied, no Enderscape class or nested provider JAR is bundled, and no generated/provider asset is added to tracked source or `originals/`. Because the generated texture derivatives remain within the upstream ARR boundary, the candidate JAR and its staging output are retained locally and are not a redistributable artifact.

Focused static verification observed the unchanged 171/172 accepted base closure, the exact 33/33 optional registry closure, complete models/textures/recipes/loot/tags, resolvable Minecraft/BBB/Enderscape references, and the production-JAR code/resource boundary. Controlled Fabric GameTest runs observed all six required provider-present tests pass and the separate one-test provider-absent run pass. Those tests exercise registry identity, source bindings, form/state behavior contracts, fuel, recipes, loot, tags, and clean standalone startup; they are controlled test evidence, not owner-supplied gameplay validation.

### Wooden wall policy

The current BBB source registers every wooden wall, creates its block item, and includes the items in BBB's creative-tab iteration; it has no separate runtime gate. Pale Oak Wall follows that exact existing treatment. The planned later absorption of BBB wooden walls into Block Geometry Extensions is not implemented here and remains out of scope.

## Rope mechanic

`bbb:rope` retains its original registry ID, item, models, recipes, loot, ChainBlock X/Y/Z axis states, waterlogging, shape, and ordinary placement behavior. Its BBB-owned C4 mechanic is deliberately narrow: only a vertical (`axis=y`) rope is climbable. A non-sneaking main-hand use with a rope pays out one vertical segment at the bottom of the clicked contiguous vertical column through Minecraft's normal block-placement path; an empty main hand reels in that same bottom segment. Sneaking bypasses the mechanic and retains ordinary axis-aware rope placement. The feature does not add entities, block entities, ticking, column ownership, cross-mod behavior, or cascade breaking.

## Compatibility invariants

- Never collapse material variants or infer them by lossy name parsing.
- Do not reintroduce removed Layer/Ladder behavior as a side effect of integration.
- Keep BBB and Enderscape ARR resources in the hash-guarded, ignored, local-only staging pipeline.
- Preserve the 171-block/172-item standalone registry exactly; append exactly 33 blocks/items only when all six explicit Enderscape sources are available, yielding 204 blocks/205 items.
- Treat compilation, static fixtures, GameTests, and JAR generation as controlled evidence rather than gameplay/runtime acceptance.

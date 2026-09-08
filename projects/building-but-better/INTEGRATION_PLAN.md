# Integration Plan

This file defines the compatibility seam established by the BBB 26.2 port. It does not implement downstream integration and does not authorize changes to Inventory Bridge Framework (IBF), Clutter No More (CNM), or the Workbench runtime profile.

## Fabric-only port boundary

The 26.2 project registers curated BBB content directly with Fabric/Minecraft APIs. Upstream's common/fabric/forge Architectury split and embedded MidnightLib are intentionally not retained. The port keeps upstream registry IDs for every selected block/item so worlds, recipes, tags, and resources use the stable `bbb:<id>` namespace within the curated scope.

`BBBContent` is the source of truth for family membership:

- `WoodFamily` maps one literal wood material and source plank block to its 11 retained forms.
- `StoneFamily` maps one literal stone material and its foundation/masonry source blocks to its 5 retained forms.
- `BeamFamily` explicitly binds material, source planks, beam, directional beam slab, beam stairs, and wooden wall.

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
4. preserve all 11 material variants exactly, including bamboo, crimson, warped, mangrove, and cherry;
5. use blockstate axis/facing metadata for orientation-sensitive storage/crafting behavior;
6. add focused compatibility tests for beam, slab, stairs, and wall relationships;
7. build a new canary and obtain separate runtime validation.

This BBB port makes **no** IBF or CNM source changes. It also does not claim that either mod already recognizes BBB beams.

## Pale Oak extension

Pale Oak is a Minecraft 26.2-native extension, not historical `2.0pre4` content. It is a complete eleven-form wood family—balustrade, lattice, wall, beam, beam stairs, beam slab, support, pallet, frame, lantern, and trim—represented by the same explicit metadata as every existing family. Layers and ladders remain excluded. Its ignored generated resource closure uses the retained Cherry geometry/state layouts, direct Minecraft 26.2 Pale Oak log textures where the model contract permits, and fourteen Pale Oak-specific authored sheets generated from the actual vanilla Pale Oak plank palette while preserving BBB transparency and non-wood details, including BBB's authored Beam side/end artwork.

### Wooden wall policy

The current BBB source registers every wooden wall, creates its block item, and includes the items in BBB's creative-tab iteration; it has no separate runtime gate. Pale Oak Wall follows that exact existing treatment. The planned later absorption of BBB wooden walls into Block Geometry Extensions is not implemented here and remains out of scope.

## Rope baseline

`bbb:rope` remains the stock retained baseline. Any climbing, chaining, placement, physics, connectivity, or cross-mod behavior enhancement is a separate feature proposal with its own compatibility and runtime analysis.

## Compatibility invariants

- Never collapse material variants or infer them by lossy name parsing.
- Do not reintroduce removed Layer/Ladder behavior as a side effect of integration.
- Keep upstream ARR resources in the hash-guarded ignored staging pipeline.
- Preserve the 171-block/172-item current curated registry: the historical 160/161 port baseline plus exactly eleven port-native Pale Oak blocks and block items.
- Treat compilation, fixtures, and JAR generation as non-runtime evidence.

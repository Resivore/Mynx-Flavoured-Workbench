# BGE × Complementary architecture audit

**Audit checkpoint:** authoritative main at 0dae5a8ba550e43886f7d74e23567697c466b1df (2026-09-19)
**Result:** rescope complete; no supported Workbench-only implementation exists in the supplied stack
**Runtime evidence:** none — this is source/archive/configuration inspection, not a Minecraft result

## Decision

BGE already has the right authority for parent identity. Its current canonical
binding identifies every derived block's canonical material without registry-name
parsing or a second BGE-block inventory. That information reaches Continuity
through the separate BGE × CTM Fabric appearance bridge, but Iris does not use
that appearance state to choose shader material IDs.

Complementary Unbound r5.8.1 assigns those IDs in its own
shaders/block.properties. The exact supplied file uses explicit block IDs; it
declares neither a %tag selector nor an IRIS_TAG_SUPPORT branch. Thus the
necessary parent relationship cannot be expressed by BGE alone today. A normal
resource pack cannot merge an extra block.properties mapping into an active
shader pack, and no documented Iris API in this audited stack lets a mod replace
that pack-owned mapping at runtime.

The clean future seam is **pack-declared Iris tags backed by
BGE-generated canonical-parent tags**, but it requires Complementary maintainer
agreement and a supported tag contract. Do not generate an inert tag namespace,
patch Iris, or patch/redistribute the supplied Complementary archive before that
agreement.

## Exact reference inputs

| Input | Read-only identity | Relevant finding |
| --- | --- | --- |
| Complementary Unbound | originals/shaderpacks/ComplementaryUnbound_r5.8.1.zip; SHA-256 bb89b1fc54687d4147a837fb2e3c3f7261a13bee51819761e9b6a91cb7915965 | Owns the material-ID map and material programs. |
| Saved shader options | ComplementaryUnbound_r5.8.1.zip.txt; SHA-256 931dd0fc5aa4cea6f0f8fb1fa8b6c056492930258bcb52a586f5439a622a130b | FANCY_GLASS=true, BLOCK_REFLECT_QUALITY=1, COATED_TEXTURES=true, generated normals enabled, and the recorded water/shadow settings are active audit inputs. |
| BGE | C78 / 4.2.22-bge.canary78.stair-wall-surface-authority+26.2 | Owns typed canonical bindings and derived surface topology. |
| BGE × CTM | Canary 12 / 0.12.0-canary12 | Owns canonical Fabric appearance for Continuity and its physical contact filter; it does not select Iris material IDs. |
| Iris | audited stack 1.11.2+mc26.2 | Reads shader-pack block.properties mappings; its documented tag syntax is pack-side. |

The archive's License.txt defines both redistribution and modification of pack
code as outside normal usage. No permission to create, distribute, or install a
modified archive was found in the exact input. originals/ was inspected only;
nothing in it was changed or copied into this project.

## Current BGE parent authority

The current BGE architecture is materially stronger than the former project's
C54-era snapshot:

- **BgeMaterialBindings** is the authoritative binding for every BGE-owned
  physical block. A binding holds the physical block, canonical material,
  optional NibaruMaterialProfile, role, explicit state projection, topology,
  and exact rendered-surface provider. Normal catalog materials bind all nine
  roles; retained aliases and deliberately limited forms are explicit rather
  than inferred.
- **NibaruProviderAdapter** still captures the provider-profile relationship as
  RuntimeBinding(profile, role) and registers legacy geometry through
  DerivedMaterialTraits. These are useful compatibility/runtime records, while
  BgeMaterialBindings supplies the complete current C78 cross-role authority.
- The binding projects only material-equivalent state to the canonical parent:
  axis, leaf distance/persistence, snowy, and glazed pattern state. Geometry
  facts such as slab type, placement-facing, layer count, double state, and
  water volume are not silently treated as parent material state.
- BGE's generated models retain canonical texture roles and its surface model
  describes exact exposed faces for slabs, stairs, walls, Vertical Slabs,
  Steps, Layers, Corners, and Quarter Columns. That is geometry authority, not
  shader classification.

BGE also writes ordinary block tags from profile-derived tags for appropriate
geometry. They are gameplay/data tags, not a shader-material API. The exact
Complementary block.properties never references them, so adding a BGE block to
one cannot alter its current Iris material ID.

## What Complementary and Iris actually classify

block.properties maps a registry block/state to a numerical shader material ID.
Representative exact entries are:

| Parent class | Exact mapping in supplied pack | Consequence |
| --- | --- | --- |
| Clear glass / pane | glass → 32008; glass_pane → 32012 | layer.translucent also lists canonical glass/pane. Material 32008 enters the glass path. |
| Stained glass | white 31000, then separate full-block and pane IDs through black 31030/31031 | Color identity is not one generic stained-glass ID; a future mapping must preserve the parent's precise class. |
| Metals / reflective solids | iron 10264, copper family 10292, gold 10312, diamond 10316, emerald 10336 | Parent material IDs route into distinct terrain handling; canonical texture/specular inputs remain separately relevant. |
| Light-emitting examples | glowstone 10412, sea lantern 10448, shroomlight 10648, froglights 10680–10688 | Shader treatment can include class-specific lighting/emission behavior, not merely vanilla light level. |
| Leaves and foliage | leaves 10009; lower foliage 10005; vine 10013; upper foliage 10021 | These IDs participate in material setup and vertex waving. |
| Other specialized translucents | tinted glass 30008, slime 30012, honey 30016, portal 30020, water 32000, ice 32004, beacon 32016 | They are separate classes and are not evidence that an arbitrary partial solid may inherit them. |

The programs show that an ID is important but not the entire visual contract:

- specificMaterials/translucents/glass.glsl assigns glass smoothness and
  reflection. With the supplied FANCY_GLASS=true, it raises alpha to the pack's
  glass opacity floor and applies the Fancy Glass translucent treatment.
  stainedGlass.glsl is a distinct path.
- customMaterials.glsl additionally samples texture-provided normal/specular
  and emission information. BGE's canonical sprite reuse is helpful, but an ID
  match alone cannot repair a wrong texture, UV/projection issue, or missing
  auxiliary map.
- wavingBlocks.glsl moves vertices for foliage, leaves, vines, water and other
  selected classes. This is deliberately geometry-sensitive behavior, not a
  generic material color property.
- Render-pass selection is a separate layer.* mechanism. In particular, the
  pack's layer.translucent=glass glass_pane beacon does not automatically
  include BGE IDs. BGE model/render-layer correctness remains BGE-owned and
  must be proved independently of shader-ID parity.

## Iris seam and its limits

Iris documents block.properties as a shader-pack file that assigns IDs by
Minecraft block ID/state. It supports data-pack tags since Iris 1.7 using
%namespace:tag; Iris 1.8+ gives explicit block entries priority over tags when
IRIS_TAG_SUPPORT >= 2 is available. See the official
[block.properties reference](https://github.com/IrisShaders/docs/blob/main/src/content/docs/current/Reference/Miscellaneous/block_properties.mdx)
and [IRIS_TAG_SUPPORT reference](https://shaders.properties/current/reference/macros/iris_tag_support/).

That makes a tag-supported shader pack a viable scalable integration seam:
Iris expands the selected shader pack's declared tags when constructing its
block-state material mapping. It does **not** mean that Fabric
BlockState#getAppearance, BGE's RuntimeBinding, or an arbitrary ordinary
resource pack can substitute a different registry block for shader rendering.
The BGE × CTM mixin is correctly limited to Fabric appearance consumed by
Continuity. Iris still sees the physical BGE block unless its own pack mapping
matches that block or an included tag. This conclusion follows from Iris's
documented ID/tag mapping model and BGE × CTM's source-level use of Fabric
appearance; it is not a runtime-rendering claim.

No current pack-side tag entry exists in the supplied archive, and no documented
mod-contributed mapping hook was identified. A private Iris mixin would be a
version-coupled replacement of pack policy, would turn Iris into a hard
implementation dependency, and is excluded rather than treated as an
integration solution.

## Inheritance policy

| Category | Parent-ID inheritance policy | Additional condition |
| --- | --- | --- |
| Clear and stained glass | Candidate for parity | Preserve exact clear/stained color ID; BGE must retain the required translucent layer, texture inputs, and physically valid face-culling. Translucent Glass remains a clear-glass texture/CTM test case. |
| Static solid special materials, including reflective/smooth/specular and ordinary emissive categories | Candidate for parity | The derived model must preserve canonical texture roles and no shader path may depend on an absent full-cube-only semantic. Test each representative family. |
| Cutout leaves, foliage, vines, crops, lily-pad/water-like classes | Withheld by default | Waving, upper/lower state meaning, and plant-specific geometry can move or shade partial solids incorrectly. Admit only a demonstrated parent-class/topology pair; never force a full-block interpretation. |
| Fluids, portals, beacon-like and block-entity-specific paths | Excluded unless a supported BGE geometry and exact semantic equivalence are demonstrated | Their pass, vertex metadata, fluid/block-entity behavior, or full-block assumptions are outside a generic geometry derivation. |
| Full occupancy forms canonicalized by BGE placement | Already canonical at the physical block level | No derived shader mapping should be necessary after BGE has replaced completed geometry with its canonical parent. |

This policy is intentionally based on material class plus BGE surface/topology
evidence, not a hand-maintained mapping of every derived registry ID. It also
keeps internal-face suppression and continuity separate: a shader ID cannot
remove a bad translucent face, and BGE × CTM does not grant an Iris ID.

## Supported implementation plan

1. Obtain Complementary maintainer approval for an Iris-tag extension and for
   the proposed BGE namespace/compatibility contract. Do not submit or
   distribute a local patched r5.8.1 archive.
2. Once accepted, add pack-side entries alongside the existing parent entries,
   guarded by IRIS_TAG_SUPPORT. Prefer one stable BGE tag per canonical parent,
   so the pack author maps minecraft:glass and its BGE descendants to 32008
   without enumerating slabs, stairs, walls, or future BGE IDs. Keep each
   stained parent in its exact existing color class.
3. In BGE, only after the namespace/eligibility contract is accepted, generate
   those tags from BgeMaterialBindings and the binding's canonical parent.
   Include only supported, explicitly admitted derived physical blocks;
   canonical blocks remain covered by Complementary's existing explicit IDs.
   This is a narrow metadata addition, not a second material registry.
4. Add static fixtures proving tag membership is derived from the binding,
   excludes unbound/special/withheld geometry, and has no name-parsing path.
   Build a BGE candidate only if the BGE project owns that implementation.
5. Run the manual matrix in TESTING.md with shaders off first, then under the
   exact supplied Complementary configuration. Promote no category merely
   because its data tag or generated model exists.

There is deliberately no implementation in this task. BGE-only tag generation
would have no effect on the supplied archive, while a local shader-pack change
would violate the stated boundary. The next meaningful external action is an
upstream-supported contract decision, not a workaround.

## Translucent Glass continuity

The renamed project preserves the former audit's useful scope:

- Translucent Glass 0.3.0 is a resource-driven clear-glass treatment; it is not
  a shader material provider. Its clear texture and Continuity rules remain a
  high-signal representative control for BGE glass geometry.
- BGE × CTM owns Continuity rule selection, canonical appearance, connection,
  overlays, and contact filtering. This project consumes those results and does
  not duplicate CTM rules or claim its work.
- The former C54 audit's observations about missing explicit model material and
  incomplete translucent face coverage are retained as historical provenance,
  not asserted as current C78 behavior. Current BGE model/render-layer and
  face-culling observations require their own exact candidate/runtime evidence.
- No prior visual observation establishes current Complementary parity. This
  project remains PLANNED and RUNTIME_UNTESTED until the exact candidate, stack,
  and in-game observations are recorded.

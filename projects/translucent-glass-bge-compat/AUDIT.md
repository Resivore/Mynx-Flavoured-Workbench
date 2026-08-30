# Translucent Glass × BGE architecture audit

**Checkpoint:** `main` at `777d9ece7f38bc6c323f57d12ff83d7cd31ebc59`  
**Reconciled dependency:** BGE × CTM audit on `main` at `8e8e40a3773ea954e94fe22c1af174d7fe633229`
**Result:** static audit complete; compatibility implementation not started  
**Runtime evidence:** none for this project; BGE C54 is runtime untested

## Finding

The clean ownership seam already exists: Nibaru describes each canonical
material once, and BGE binds every derived geometry back to that profile. The
eventual implementation should extend that typed material/geometry seam so a
new BGE geometry inherits glass behavior when it is registered. It should not
maintain a list of Translucent Glass patches per block ID.

Three different contracts currently stop short of that seam:

1. BGE copies block settings and the canonical texture path, but it does not
   preserve Minecraft 26.2's per-texture `force_translucent` model material.
2. Translucent face suppression is incomplete and asymmetric across canonical,
   native Nibaru, Step, Vertical, and Layer shapes; some generated model faces
   are not marked `cullface`, so block-side culling alone cannot hide them.
3. Continuity and Complementary start from registry-based material rules. BGE
   does not currently expose its in-memory canonical binding through the
   appearance API Continuity consumes, while Complementary has no corresponding
   BGE classification in the selected shader pack.

The first two are primarily BGE/Nibaru architecture. Continuity's half of the
third belongs to BGE × CTM's shared appearance bridge; shader classification
needs an upstream or otherwise authorized integration path.

## Current architecture

### Translucent Glass 0.3.0

The exact dependency is `translucent-glass-0.3.0.jar`, mod ID
`translucent-glass`, SHA-256
`27590CC73ACC1285CD6EAC837CB2AB27A0DF69E50EC5EEDD5AD6D5E7166CAD77`,
from `JustinTimeCuber/translucent-glass` under GPL-3.0-only.

Its client initializer only registers the built-in
`translucent-glass:translucent-glass` pack as default-enabled. The JAR has no
mixins, custom blocks, block/render-layer registration, model loader, renderer
hook, Sodium/Iris hook, or shader code. Its clear-glass behavior is resource
driven:

- it replaces `minecraft:textures/block/glass.png` and
  `glass_pane_top.png` with partially transparent textures;
- it provides 47 clear-glass CTM tiles;
- its Continuity rules use `matchBlocks=glass` and
  `matchBlocks=glass_pane` (pane sides only);
- it supplies no model/blockstate override and no BGE IDs;
- it does not directly alter stained glass. Stained families remain useful
  BGE/Continuity/Complementary parity controls for this project.

Thus alpha comes from the resolved texture. Neighbor-face suppression comes
from Minecraft/Nibaru/BGE block and model behavior, not from Translucent Glass.
Connected borders require Continuity and the matching CTM rules. Shader
presentation is external to the mod.

### BGE and Nibaru material derivation

`NibaruMaterialProfile` is the material authority. For the clear and sixteen
stained-glass families it records the canonical parent, native slab/stair/wall,
`TRANSLUCENT_ADJACENCY`, `GLASS_EDGE`, translucent intent, canonical texture
roles, and supported derived geometries.

BGE redirects CNM's Vertical and Step creation through
`NibaruProviderAdapter.createVertical` / `createStep` and registers one Layer
for every supported profile. Representative clear-glass ownership is:

| Geometry | Representative owner/ID |
| --- | --- |
| full block | `minecraft:glass` |
| slab/stair/wall | Nibaru `more_slabs_stairs_and_walls:glass_*` |
| Vertical/Step | BGE-mediated CNM `clutternomore:more_slabs_stairs_and_walls/...` |
| Layer | BGE `cnm_terrain_slabs_compat:minecraft/glass_layer` |

Step and Vertical receive `BlockBehaviour.Properties.ofFullCopy` from their
native source geometry; Layer copies the canonical parent. This preserves
ordinary settings such as strength, sound, no-occlusion predicates, light and
movement properties. It does not copy Java overrides/interfaces, state
properties, tags, model materials, CTM rules, shader IDs, or renderer hooks.

Every created geometry is captured once by `NibaruProviderAdapter` in
`DERIVED` / `RUNTIME_BINDINGS` and registered in `DerivedMaterialTraits` with
its canonical parent and geometry. This is already the scalable derived-to-
source relationship the compatibility work needs.

Models reuse the canonical glass sprite and BGE's `GLASS_EDGE` topology. Layer
also generates a corresponding item projection. This is why the Translucent
Glass texture replacement reaches BGE geometry automatically.

## Exact compatibility gaps

### Model material and translucency

Minecraft 26.2's canonical `glass.json` represents its texture as a material
object with `"force_translucent": true`. The 26.2 `Material` codec parses that
field, and `FaceBakery` selects translucent material behavior immediately when
it is true. The vanilla `CuboidModel` parser does not consume BGE/Nibaru's
top-level `"render_type": "translucent"` as an equivalent contract.

BGE's `ProviderVisualAdapter`, Layer projection, and Glass Step/Vertical
templates currently emit string texture references plus that top-level key.
They therefore preserve the sprite but drop the canonical model-material
guarantee. The partially transparent Translucent Glass PNG should cause sprite
alpha inference to choose translucency in the current resolved stack, which is
consistent with the narrow C53 report that exterior glass/translucency looked
correct. That is not deterministic parity, does not establish C54 behavior,
and does not rule out state, item, reload, or pack-order differences.

No static evidence shows a deliberate opaque-rendering path. The supported
finding is a missing explicit 26.2 contract with a current alpha-inference
fallback.

### Face culling and adjacency

The typed capability exists, but the present implementations do not compose it
across all geometry:

- `createVertical` and `createStep` have no `TRANSLUCENT_ADJACENCY` branch, so
  plain/stained glass falls through to raw CNM geometry without BGE's
  profile-aware `skipRendering` behavior.
- Layer selects a translucent subclass, but `cullsBoundTranslucent` returns
  true only for the canonical parent or the exact same derived block in an
  equal state. It does not cover Layer ↔ Step, Layer ↔ Vertical, or native
  slab/stair/wall seams.
- Nibaru's full-block mixin recognizes its native translucent slab/stair types,
  not BGE Step/Vertical/Layer. Full glass → BGE suppression is therefore not
  inherited, even where the reverse direction has a rule.
- BGE's Glass Step/Vertical templates contain no `cullface` declarations.
  Their boundary quads cannot be removed by neighbor `skipRendering` alone.
- Partial geometry cannot safely cull on material equality alone. The caller
  must prove that both shapes cover the relevant shared face. Layer already
  contains a narrow equal-state safeguard; the general case remains to be
  designed and runtime checked.

The best-supported likely result is visible internal faces or seams for some
self, mixed-geometry, and canonical/derived adjacencies, with translucent
overdraw amplifying their appearance. This is a static coverage gap, not a
claim that every adjacency visibly fails in Minecraft.

### Continuity and resource matching

Translucent Glass's clear CTM rule explicitly matches canonical
`minecraft:glass`. Continuity 3.0.1's `matchBlocks` parser resolves explicit
registry IDs/state predicates, not BGE runtime profiles or data tags. However,
Continuity evaluates the current and neighboring states through Fabric's block
appearance API before applying those predicates and `connect=block` /
`connect=state` comparisons.

BGE currently returns the derived state as its appearance, so the active clear
rule does not select BGE glass and different geometry IDs do not connect. The
separate BGE × CTM audit now establishes the scalable fix: BGE should project an
eligible derived state to a deliberately mapped canonical appearance state via
`getAppearance`, backed by `RuntimeBinding` / `DerivedMaterialTraits`. Then the
unchanged Translucent Glass rule can see `minecraft:glass` for selection and
connection without a generated ID inventory.

State projection and partial-shape eligibility remain important: geometry-only
properties must not leak into canonical state, and block-neighbor adjacency
does not prove that two partial rendered planes touch. Those policies belong to
BGE × CTM, with a narrow Continuity-specific contact filter only if runtime
evidence shows that the renderer-neutral appearance bridge is too permissive.
No Translucent-Glass-specific CTM assets are currently justified here.

### Complementary and Iris

Complementary Unbound r5.8.1's `shaders/block.properties` maps canonical clear
glass to material `32008` and canonical stained families to the `31000` series.
It contains no Nibaru/BGE/CNM geometry IDs. With the current
`FANCY_GLASS=true`, canonical clear glass receives the shader's opacity floor,
smoothness, highlight, and reflection path; an unclassified derived block is
ordinary translucent terrain. The likely shader mismatch is therefore derived
geometry looking too transparent/differently reflective, not an opaque BGE
fallback. Extra internal faces can make either mismatch more conspicuous.

Iris supports `%namespace:tag` entries in a shader pack's
[`block.properties`](https://github.com/IrisShaders/docs/blob/main/src/content/docs/current/Reference/Miscellaneous/block_properties.mdx),
so a stable per-canonical-material BGE tag is a scalable upstream
classification seam. Iris still reads the selected shader pack's mapping; a
normal resource pack does not overlay it. The exact Complementary r5.8.1
license does not authorize treating block-property additions as an ordinary
modpack modification. Do not revive the historical local ZIP patch or
redistribute a modified pack without upstream permission. Preferred outcomes
are upstream Complementary support for BGE's stable tag or a documented,
licensed external Iris extension point.

### Inventory versus placed blocks

Generated item models reuse the canonical sprite and, for Layer, the projected
`GLASS_EDGE` geometry. They do not exercise placed-block neighbor culling,
Continuity adjacency, or the same terrain block-ID shader classification.
Inventory parity is therefore a separate focused runtime check. There is no
current evidence of an item-only failure.

## Ownership decision

1. **BGE, using Nibaru's typed profile:** primary owner of inherited model
   material, derived-to-canonical binding, geometry/face coverage, and
   symmetric translucent adjacency for Step, Vertical, Layer, and future
   geometries.
2. **Nibaru:** material-profile/API owner and, only where necessary, the narrow
   canonical/native-side bridge. It should not learn BGE registry-name lists or
   shader-pack policy.
3. **BGE × CTM:** owner of the canonical appearance-state bridge, explicit
   state projection, partial-geometry eligibility/contact policy, and any
   evidence-supported Continuity-specific fallback.
4. **Translucent Glass × BGE:** thin composition owner for the exact upstream
   pack, resolved-resource checks, shader coordination, and acceptance testing.
   No copied textures, per-geometry CTM rules, or competing material registry
   are currently justified.
5. **Complementary upstream or a supported Iris extension:** owner of shader
   material classification. This repository should not modify/distribute the
   current shader ZIP without authorization.

## Expected implementation components

Primary BGE/Nibaru work is likely to touch:

- `projects/nibaru/common/.../api/material/NibaruMaterialProfile.java` and
  `NibaruMaterialProfiles.java` for an explicit 26.2 model-material contract;
- `projects/block-geometry-extensions/.../NibaruProviderAdapter.java` and a new
  shared geometry-aware translucent adjacency service/interface;
- common Step/Vertical/Layer factories or base classes, including
  `BgeLayerBlock.java` and `BgeLayerSpecializedBlocks.java`;
- `ProviderVisualAdapter.java`, `client/LayerModelProjection.java`, the
  Step/Vertical model mixins, and `glass_*.json` templates to emit material
  objects and boundary-safe `cullface` metadata;
- BGE data/resource generation to expose stable per-canonical-family
  membership, plus focused static/GameTest coverage.

The separate BGE × CTM implementation is expected to add a canonical
appearance resolver in `NibaruProviderAdapter`, a `BgeLayerBlock` delegation,
and a common CNM Step/Vertical appearance mixin. This project should consume and
test that shared behavior rather than duplicate it. Complementary's mapping is
an external upstream component, not a repository file to patch under the
current license.

## Version and API constraints

| Component | Audited constraint/identity |
| --- | --- |
| Minecraft / Java | Java Edition 26.2 / Java 25 model-material contract |
| Fabric | Loader `>=0.19.3`; BGE builds against Fabric API `0.156.0+26.2`; current profile has `0.157.0+26.2` |
| Translucent Glass | 0.3.0; Minecraft `>=26.1`; Fabric API required; Continuity suggested |
| BGE | C54 (`0.6.1` line), SHA-256 `7CD96C8153B2DDCA863A96BCF0BBB04680CA6CE92A63A2086A7155441EC9812A` |
| Nibaru | exact C46, SHA-256 `3281D110F062DB62E721D838A35CE915CA73DD41AF098A013B52952561CEAE7D` |
| CNM | `>=2.0.7`; current 2.0.7 |
| Continuity | 3.0.1+26.2, client-side, Minecraft `>=26.2` |
| Iris / Complementary | Iris 1.11.2+mc26.2; Complementary Unbound r5.8.1, Fancy Glass enabled |
| Matcha | `Matcha_Flavoured_1_10.zip`; targeted scan found no vanilla glass/stained-glass texture override |

Sodium, Continuity, Iris, and Complementary consume the baked model/quad,
culling, resource, and material-ID results. Compilation, generated models,
fixtures, GameTests, and screenshots cannot establish their runtime behavior.

## Focused implementation plan

1. Extend the typed visual/material contract once, then emit Minecraft 26.2
   material objects (`force_translucent` for applicable texture roles) for
   native and BGE block/item models. Add a static assertion against the actual
   parsed structure, not the ignored top-level key.
2. Replace geometry-specific material checks with one runtime-binding-aware
   translucent adjacency service. Give each geometry a face-coverage adapter
   and make both sides of canonical/native/BGE adjacency call the same policy.
3. Add `cullface` only to quads on true unit boundaries; keep partial exposed
   faces visible. Cover same-shape, cross-shape, and canonical/derived decisions
   in focused tests.
4. Integrate with BGE × CTM's canonical `getAppearance` bridge, explicit state
   projection, and conservative geometry eligibility. Keep the active
   Translucent Glass rule unchanged and add no ID inventory unless focused
   evidence disproves the shared bridge.
5. Expose a stable per-canonical-family data tag only if useful for shader
   classification or debugging; Continuity does not consume that tag. Seek
   upstream Complementary tag support or confirm a licensed Iris extension
   mechanism. Treat full shader parity as unresolved until that path exists.
6. Build a candidate only after these ownership contracts are settled, then run
   the focused runtime procedure in `TESTING.md` under explicit slot ownership.

## Unresolved questions and blockers

- The topology-safe face-coverage rules for every mixed partial-shape pair need
  implementation design and runtime confirmation.
- BGE × CTM has selected the canonical appearance bridge, but its safe state
  projection and partial-geometry eligibility/contact rules still need
  implementation and runtime evidence.
- Full Complementary parity is blocked on upstream support, written permission,
  or a documented licensed external mapping mechanism.
- Native Nibaru slab/stair/wall model outputs use the same top-level
  `render_type` convention and must be included in the implementation preflight,
  even though the primary new geometry work belongs to BGE.

The project is ready for an implementation task that begins with the BGE/Nibaru
intrinsic contract. Shader parity should be a separable track and must not block
the model-material and culling corrections that can be implemented lawfully
inside the Workbench.

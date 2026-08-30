# BGE × CTM architecture audit

Audit checkpoint: `777d9ece7f38bc6c323f57d12ff83d7cd31ebc59` (`main`, 2026-08-30)

This is a source, generated-resource, installed-artifact, and active-resource-stack audit. It did not add a compatibility implementation, build or deploy an artifact, launch Minecraft, or establish any runtime CTM result.

## Result

- **CTM in scope is Continuity 3.0.1+26.2 interpreting OptiFine-style `.properties` connected-texture definitions plus Continuity's documented extensions.** There is no separate tag-driven native Continuity format in the installed stack. Continuity's built-in pack, Translucent Glass, and Matcha Overlays use the same properties family in different namespaces/pack layouts.
- **BGE already has the scalable material-family relationship.** Every admitted Step, Vertical Slab, and Layer is bound to a typed Nibaru material profile and canonical parent block. Generated models preserve canonical material sprite IDs and emit ordinary model quads.
- **The principal compatibility gap is a missing Fabric block-appearance bridge.** Continuity selects block predicates and evaluates neighbors against `BlockState#getAppearance(...)`. BGE does not currently make a derived block appear as its canonical parent, so ID/property rules and `connect=block`/`connect=state` see unrelated derived blocks. Tags are not consulted by this CTM format.
- **The preferred implementation is to project each eligible derived state to a canonical material appearance state through Fabric's appearance API, using BGE's existing typed binding.** This is renderer-neutral, does not rewrite packs, does not enumerate geometry IDs in CTM files, and automatically covers future packs/materials plus future geometry bases that implement the common appearance contract once. A Continuity-specific adapter is a fallback only for geometry-contact cases the appearance API cannot express.
- **BGE is ready to move into a bounded implementation phase.** The registry does not need redesign. Partial-shape topology, canonical property projection, overlays, and multi-element translucent models remain explicit runtime-test gates.

## What “CTM” means here

The dedicated Workbench contains exact `continuity-3.0.1+26.2.jar` with SHA-256 `2094a52b98e247f664ef2ecdbb41d7dcad7779cc7a8fb87f0ed8a4f8196c0446`. Its metadata describes OptiFine connected-texture and emissive formats. Its built-in default pack contains 83 `.properties` definitions under `assets/continuity/optifine/ctm/`.

The active stack also enables the built-in pack from exact `translucent-glass-0.3.0.jar` (SHA-256 `27590cc73acc1285cd6eac837cb2ab27a0df69e50ec5eedd5ad6d5e7166cad77`). Its `assets/minecraft/optifine/ctm/glass/glass.properties` supplies the current clear-glass CTM tiles and a `matchBlocks=glass` rule. Because that pack owns the active glass texture, Continuity's own default glass definition fails its `resourceCondition`; Translucent Glass is the live clear-glass rule in this stack.

`Overlays_Matcha_STRICT_darkoak-bugfix` contributes 119 rules using the legacy-compatible `assets/minecraft/optifine/ctm/overlays/` layout. `Matcha_Flavoured_1_10.zip` (SHA-256 `b36faa3c0247c3b96dedcb1131c77989c231c828a8b332f1bbaa16ccf8ef776d`) supplies the visible texture stack but contains no CTM `.properties`; Matcha's CTM behavior comes from the separate Overlays pack. Direct scans found no CTM `.properties` in the generated `clutternomore` pack or the active ClutterNoMore, BGE C54, and Nibaru C46 JARs.

Pack conditions are materially active. Matcha overrides ordinary sandstone and bookshelf textures, so those Continuity built-ins are disabled. Matcha does not override `chiseled_sandstone.png`; Continuity's built-in chiseled-sandstone `horizontal` rule remains an active solid-material representative.

The relevant enabled low→high priority chain (unrelated packs omitted) is `continuity:default` → generated `file/clutternomore` → `translucent-glass:translucent-glass` → `file/Matcha_Flavoured_1_10.zip` → `file/Overlays_Matcha_STRICT_darkoak-bugfix`. Inspected pack metadata is compatible with the 26.2 resource system: Continuity default declares min 88/max 999, Translucent Glass min 85/max 88, Matcha Flavoured min 88.0/max 107.1, and Matcha Overlays min 69/max 999. Metadata admits and orders packs; CTM family behavior still comes from the loaded rules/resources.

Continuity's format relationship is:

| Concern | Installed Continuity behavior |
| --- | --- |
| Resource discovery | Reads OptiFine-style `optifine/ctm/**/*.properties`; Continuity additionally permits arbitrary resource namespaces. |
| Rule selection | `matchBlocks` parses exact block IDs with optional block-state property filters. `matchTiles` matches the sprite on the emitted model quad. If both are present, both predicates must pass. Filename inference is also supported. |
| Tags | No block-tag selector is parsed. `matchBlocks` resolves IDs through the block registry, so adding BGE blocks to data tags cannot make an existing CTM rule discover them. |
| Connections | `connect=block` compares appearance block identity; `connect=state` compares exact appearance state; `connect=tile` compares the rendered sprite against sprites resolved for the neighbor's appearance state. Overlay `connectBlocks` and `matchBlocks` predicates also test appearance states. |
| Quads/models | Continuity wraps block-state models, obtains the current appearance state, and runs processors on emitted quads using their sprite and light face. BGE's ordinary JSON quads therefore enter the expected path. |
| Pack precedence | Pack priority, file ordering, and Continuity's `prioritize` behavior select competing processors. `resourceCondition` can restrict a definition to the pack that currently owns a resource. |
| Metadata | `pack.mcmeta` controls pack compatibility/loading; it does not express material-family inheritance. |

The Continuity extensions relevant to this audit include arbitrary namespaces, `orient` (with `TEXTURE` the default for connecting methods), `prioritize`, `resourceCondition`, and additional overlay methods. They extend rather than replace the OptiFine properties model. This distinction matters because generating a second set of rules would also have to preserve active-pack priority, extension properties, and the original rules' combined block/sprite predicates.

## Current BGE material × geometry architecture

Nibaru's `NibaruMaterialProfiles` is the authoritative immutable material catalog. Each `NibaruMaterialProfile` carries the canonical parent block/ID, native and effective source geometries, derived tags, behavior capabilities, semantic texture roles, tint/render layer, orientation/surface policy, and material transitions.

BGE consumes that catalog rather than inferring families from names:

- `NibaruProviderAdapter` keeps `profile + geometry -> derived block` and `derived block -> RuntimeBinding(profile, geometry)` identity maps.
- Every capture also registers `DerivedMaterialTraits.Entry(derived, canonicalParent, geometry, fuelDivisor)`.
- `CanonicalGeometryRegistry` prevents a derived geometry from becoming another material root.
- The current controlled population is 311 canonical material profiles across `VERTICAL_SLAB`, `STEP`, and `LAYER`: 933 derived blocks.

Current ID formation is deterministic but is not the inheritance mechanism:

- CNM Vertical Slab: `clutternomore:<source namespace>/vertical_<source slab path>`, for example `clutternomore:more_slabs_stairs_and_walls/vertical_glass_slab`.
- CNM Step: `clutternomore:<source namespace>/<source stair path with stairs→step>`, for example `clutternomore:more_slabs_stairs_and_walls/glass_step`.
- BGE Layer: `cnm_terrain_slabs_compat:<canonical namespace>/<canonical path>_layer`, for example `cnm_terrain_slabs_compat:minecraft/glass_layer`.

Step and Vertical Slab asset generation is redirected to the material profile's semantic texture roles. Layer models are projected from those same roles. Representative generated glass models bind `minecraft:block/glass`, and solid models similarly retain their canonical sprites. BGE does not install a custom baked-model or quad-renderer path; the models are ordinary JSON and Fabric/vanilla-rendered quads.

`BlockBehaviour.Properties.ofFullCopy(canonicalParent)` copies settings, not material identity, data tags, or canonical state. Generated Layers copy `derivedBlockTags`; Step and Vertical Slab have selected authored behavioral tags rather than general tag propagation. Neither tag path affects Continuity rule selection. BGE currently contains no `getAppearance` override or equivalent CTM-family hook.

## Demonstrated compatibility gap

The gap is rule-dependent rather than a blanket inability to process BGE models.

| Representative installed/current-stack rule | Current static outcome | Outcome from canonical appearance |
| --- | --- | --- |
| Active Translucent Glass clear glass: `method=ctm`, `matchBlocks=glass`, `tiles=0-46` (implicit `connect=block`) | The definition has no sprite selector that could rescue a derived ID. A BGE glass state is not selected, and different BGE geometry IDs/base glass are not block-equal. | The rule sees `minecraft:glass` for selection and for both connection endpoints, so base↔BGE and eligible BGE↔BGE family connections become automatic. |
| Active Continuity chiseled sandstone: `method=horizontal`, `matchTiles=chiseled_sandstone`, `matchBlocks=chiseled_sandstone`, `faces=sides`, `connect=block` | BGE retains the canonical sprite, but both selectors must pass and its derived appearance fails the block predicate. | Canonical state plus the already-canonical sprite satisfies both predicates; block connection uses the shared canonical parent. This is the solid-material control. |
| Matcha `bricks.properties`: `method=overlay`, explicit receiver `matchBlocks`, `connectBlocks=bricks` | BGE IDs are absent from both exact block sets. A BGE receiver is not selected and a BGE brick is not recognized as the inducing material. | Receiver and neighbor block predicates see their canonical materials without changing the Matcha pack. Separate overlay geometry guards still skip many partial receiver quads and require an actual full-collision inducing neighbor. |

This establishes the actual root cause categories:

1. **Confirmed primary cause:** BGE retains canonical material data internally but does not expose canonical visual identity through Fabric block appearance, whose default is the derived state itself.
2. **Confirmed rule-selection consequence:** exact `matchBlocks` and state filters see the derived ID/state. When `matchTiles` is also present, sprite equality does not compensate because both predicates must pass.
3. **Confirmed connection consequence:** `connect=block` and `connect=state` compare derived identities/states; different geometry IDs and the base block are unequal. Overlay block predicates fail for the same reason.
4. **Not causes:** BGE is not bypassing Continuity's quad pipeline, and canonical sprite paths are already preserved. General tag propagation would not repair this format.
5. **Secondary limitation:** even after identity inheritance, Continuity's adjacency test is block/face based and does not prove that two partial quad planes physically meet.

## Preferred scalable architecture

### 1. Canonical appearance in the renderer-neutral family seam

Add one BGE-owned appearance-state resolver backed by the existing `RuntimeBinding`/`DerivedMaterialTraits` relationship. Every eligible derived block should answer Fabric `getAppearance(...)` with the appropriate canonical parent state. Non-BGE blocks and excluded bindings retain their normal self appearance.

This should be implemented once for all captured geometries:

- BGE-owned `BgeLayerBlock` can delegate directly to the resolver.
- CNM-owned Step and Vertical Slab instances need one common mixin on those two CNM base classes that looks up the exact runtime binding. This covers raw fallback instances and their BGE-specialized subclasses without adding overrides to dozens of classes or parsing registry IDs.
- Future geometry bases become eligible by implementing the same appearance contract once and registering the typed binding/policy during capture; registration alone does not change block-method dispatch.

The common layer should depend only on Minecraft/Fabric APIs already used by BGE. Continuity can remain an optional runtime consumer; Matcha-specific assets and IDs do not belong in BGE core.

### 2. Explicit state projection

Return a canonical **state**, not merely a block ID. Start from the canonical default state and copy only explicitly equivalent material properties. Examples include BGE `axis` to canonical `axis`, glazed `pattern_facing` to the canonical horizontal-facing property, and explicitly carried leaves `distance`/`persistent` state. Do not copy geometry `facing`, slab type, `layers`, `double`, or `waterlogged` merely because a property name happens to match.

Profiles whose CTM-relevant state cannot be mapped safely should remain base-only until an explicit projector exists. Focused fixtures should prove default, axis, pattern, and unmappable-property behavior.

### 3. Central geometry eligibility policy

Appearance inheritance should be opt-in by supported geometry/state/visual profile, with one typed policy rather than material-ID lists. Continuity obtains the current appearance once with a `Direction.DOWN` self query before processing the model, so baseline appearance eligibility applies to the whole state/model; it cannot select only exterior planar quads. Begin with full-volume and simple planar geometry states whose whole-model interpretation is clear. Admit multi-element glass-edge, overlay, roots, honey/slime inset, and mixed partial-shape cases only with focused runtime evidence or a processor-level guard.

The neighbor appearance call supplies a queried side plus source state/position, so a later BGE contact policy can return the original derived appearance for an incompatible neighbor query. Continuity passes the already-canonical appearance as `sourceState`; the policy must recover the real source geometry from the render view at `sourcePos`, not assume `sourceState` is the BGE state. It still cannot inspect arbitrary per-quad bounds, and Continuity's self/rule-selection query is distinct from neighbor contact. If runtime testing demonstrates false connections that cannot be avoided through whole-state exclusion or this query policy, add a narrow optional Continuity adapter that filters individual processing/connection decisions using BGE geometry metadata. That is a second phase, not the starting architecture.

### Alternatives considered

- **Shared tags/family tags:** useful as an authoring/debug API, but current Continuity/OptiFine rules do not consume them.
- **Sprite inheritance:** required and already present; insufficient for exact block rules and `connect=block`.
- **Generated rule expansion/synthetic resource pack:** can work, but needlessly reparses or duplicates every active pack's block/state predicates, aliases, priority, extension properties, and connection sets. It is less general than the appearance seam Continuity already consumes.
- **Continuity-internal renderer mixins:** version-coupled and unnecessary for baseline identity inheritance. Reserve an optional hook for proven geometry-contact limitations.
- **Manual rules per geometry:** rejected because every new material and geometry would recreate the compatibility pass this project is intended to eliminate.

## Geometry-specific limits

Continuity can process BGE's actual emitted quads. Its connection lookup nevertheless uses neighboring block positions and the quad's light face, not intersection between voxel/quad surfaces.

- **Vertical Slab:** identical orientation/state and coplanar exterior faces are the safest partial case. The double state is full-volume and is a good cross-geometry control.
- **Step:** the single state is a `16×8×8` quarter-volume; its double state is two diagonal quarter-volumes rather than a full cube. Exterior, recessed, and interior planes can receive a tile selected from a whole-block neighbor relation. This is the highest-risk current geometry.
- **Layer:** 4/8/12/16-pixel cuboids can face any of six directions. Four Layers are full-volume; only regular profiles whose canonical full model is reusable actually reuse that model, while axis, glazed, and inset projections still generate cuboids. Partial layers expose non-boundary planes. Equal facing/thickness is plausible; different thickness, opposite anchoring, and mixed geometry can report a block neighbor without sharing the rendered plane.
- **Future partial geometries:** register a surface/contact policy before enabling inheritance. Registry capture alone must not imply that every face topology is CTM-safe.
- **Orientation:** Continuity's default `orient=TEXTURE` uses actual quad UV orientation, which is favorable for rotated JSON geometry. `STATE_AXIS` and property-filtered rules depend on correct canonical state projection.
- **Overlay methods:** Continuity's overlay processing predicate requires a unit-square target quad, so many partial BGE receiver faces are skipped before material matching. The standard overlay processor also requires the actual inducing neighbor state to have a full collision shape. Canonical appearance does not and should not turn a partial BGE source into a full block. Full-volume/four-Layer cases are appropriate probes; general partial-overlay inheritance needs an explicit exclusion or later specialized integration.
- **Transparent/multi-element models:** glass-edge templates reuse/slice the canonical glass sprite across body and rim elements. CTM can reach those quads, but connected replacement may erase or double-emphasize authored edges. Translucent face suppression happens before CTM can transform a missing face, so BGE translucent-culling behavior remains separate from CTM identity.

Complementary shaders/Iris do not choose CTM families. The current profile has Iris shaders enabled with `ComplementaryUnbound_r5.8.1.zip`, but that is loaded-stack evidence rather than visual validation. Shaders can expose seams, render-layer, alpha, or culling problems after Continuity transforms quads, so the representative transparent cases must be established with shaders off and then repeated with Complementary enabled. Matcha Flavoured remains a texture source; Matcha Overlays remains an independent pack whose unmodified rules should be inherited. No Matcha dependency is needed in the BGE family resolver.

## Likely implementation scope

No model rewrite or per-material CTM asset set is indicated. Likely owned/shared changes are limited to:

- `projects/block-geometry-extensions/src/main/java/dev/aero/cnmterraincompat/NibaruProviderAdapter.java` — canonical appearance lookup and eligibility/state policy;
- `projects/nibaru/common/src/main/java/games/twinhead/moreslabsstairsandwalls/api/material/DerivedMaterialTraits.java` or a BGE-local wrapper — only if a stable appearance-policy field belongs in the neutral public trait;
- `BgeLayerBlock.java`, one common mixin targeting CNM `StepBlock` and `VerticalSlabBlock`, and `cnm_terrain_slabs_compat.mixins.json`;
- focused BGE fixtures/GameTests for family identity and state projection;
- the BGE × CTM project records and later runtime evidence.

A second, Continuity-specific client package/dependency is justified only if the runtime geometry matrix proves the appearance bridge too permissive for selected topologies.

## Focused future runtime procedure

Use the dedicated 26.2 Workbench with Continuity enabled. Establish every result first with shaders off; repeat only the transparent representative row with Complementary Unbound after the non-shader result is understood.

1. **Base control:** verify the active Translucent Glass clear-glass CTM rule on adjacent `minecraft:glass` full blocks.
2. **Identical BGE geometry:** place two glass Vertical Slabs, then two glass Steps, in matching orientations with coplanar exposed faces; record each geometry separately.
3. **Base↔BGE:** join base glass to a full four-Layer glass block, then to one representative partial glass geometry on a physically shared plane.
4. **Cross-geometry/contact:** join a full four-Layer glass block to a double Vertical Slab; then inspect one deliberately coplanar partial Layer↔Vertical Slab arrangement. Add one same-material, intentionally misaligned pair (different Layer depths/opposite anchoring or a non-coplanar Layer↔Vertical pair) and verify it does not falsely connect. Treat visual joining without physical plane contact as failure.
5. **Intentional non-connection:** place clear-glass BGE geometry against white-stained-glass BGE geometry and verify the distinct canonical families do not connect.
6. **Active solid CTM:** repeat the base, identical-BGE, and one base↔BGE arrangement with chiseled sandstone on side faces. This exercises the active `matchTiles` + `matchBlocks` + `connect=block` rule without translucency.
7. **Matcha overlay limits:** place a full bricks block immediately east of a full stone receiver and inspect the stone's upward face along the shared east edge. Repeat with a four-Layer stone receiver, then fixed single-Layer/Vertical receiver states while preserving source direction and observed face. Confirm a partial BGE brick source does not masquerade as Continuity's required full-collision source; test a full four-Layer brick source separately.
8. **Shader/translucency:** repeat the clearest glass base↔BGE and BGE↔BGE cases with Complementary enabled and inspect both sides for seams, missing faces, rim corruption, and alpha/render-layer artifacts.

Record exact blocks, states/orientations, pack order, shader state, pass/fail/inconclusive result, and screenshots. A build, generated resource, unit fixture, or GameTest must remain `RUNTIME_UNTESTED` until these in-game checks are reported.

## Blockers and unknowns

There is no architecture blocker to beginning implementation. The remaining unknowns are bounded proof items:

- which canonical properties beyond axis, glazed pattern, and carried leaves distance/persistent state require projection in the currently admitted catalog;
- whether Step, mixed-thickness Layer, or mixed-geometry adjacency needs a Continuity-specific contact filter or conservative exclusion;
- how glass rim/multi-element quads look after real Continuity processing, and which partial Matcha receiver faces meet Continuity's unit-square predicate;
- the active Matcha Overlay pack's pre-existing `layer=cutout_mipped` parse warning (Continuity defaults the unknown value to cutout), whose visual effect must be separated from BGE inheritance;
- whether any other installed block overrides appearance in a way that requires composition rather than direct canonical projection;
- translucent culling and Complementary behavior, which cannot be inferred from this audit.

Recommended next state: implement the narrow canonical-appearance bridge and focused state/eligibility fixtures, build one retained runtime candidate, then execute the procedure above before broadening geometry coverage.

## Verification

- `python tools/workbench.py validate-repository --root .` passed for all 40 project manifests.
- `python -m unittest discover -s tests -v` passed 99/99 tests.
- `node --test tests/test_sheet_receiver.mjs` passed 29/29 tests.
- `git diff --check` passed.
- Focused scans/decompilation covered the exact installed Continuity and Translucent Glass JARs, active Matcha/Overlays and generated CNM resources, BGE/Nibaru sources, generated models, and current log/config evidence. They establish static architecture and load/parse facts only.

No BGE × CTM implementation was compiled, no artifact was produced or deployed, and no Minecraft runtime visual result was obtained.

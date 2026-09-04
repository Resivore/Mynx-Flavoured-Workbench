# Ribbits × Xaero Entity Icon Compatibility Audit

## Outcome

The primary hypothesis is proven. A Ribbit does not fail inside the accepted
Xaero × EMF Canary 3 adapter. Xaero Minimap 26.4.2 rejects the renderer one
stage earlier:

1. `RadarIconCreator.create(...)` asks
   `EntityRenderTracer.getEntityRendererModel(EntityRenderer)` for a vanilla
   `EntityModel`.
2. That extractor recognizes only `LivingEntityRenderer` and
   `EnderDragonRenderer`. Every other renderer returns `null`.
3. GeckoLib 5.5.1's `GeoEntityRenderer` extends `EntityRenderer`, not
   `LivingEntityRenderer`; `RibbitRenderer` extends `GeoEntityRenderer`.
4. The `null` branch returns the cached `RadarIconManager.FAILED` sentinel
   before `RadarIconModelFormPrerenderer.prerender(...)`,
   `RadarIconModelPrerenderer.renderModel(...)`, or any Canary 3 mixin can run.

This is an intentional scope gap in Canary 3 and, technically, a
Xaero/GeckoLib interoperability gap. It is not an accepted-patch regression, a
Ribbits renderer defect, or an entity-radar configuration defect on the
evidence available. Confidence in this static boundary is **extremely high**:
the result was independently reproduced from the exact Xaero, GeckoLib,
Ribbits, EMF, and Canary 3 binaries and current Workbench source.

Runtime-specific confidence is deliberately lower. No Ribbits or GeckoLib JAR
is present in the dedicated 26.2 Workbench now, and none appears in its
available historical logs. Therefore the exact Ribbits artifact on which the
user noticed the problem, and the exact visible subtype of the fallback, cannot
be bound. The only user-supported symptom is **no correct Ribbit head icon**.
For the current Xaero configuration, the proven static consequence of this
failure would be a colored radar dot plus the Ribbit name, not a blank texture
or malformed head.

## Authority and evidence boundaries

- Starting authoritative `origin/main`:
  `54e226d708cc910361104a6cfacf921bb2d54fc8` (2026-09-04T00:40:56-05:00,
  `Finalize CSR QSN and SAS runtime records`). No equivalent project existed on
  that revision.
- Canonical Ribbits input: project UUID
  `2a650718-ec62-568f-8dff-71258a4d6f3f`, current unaccepted release
  `4.1.6+26.2-mynx-canary6`, artifact
  `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary6.jar`, SHA-256
  `cbeedd06bab0d6809fd528662e4c7e4f11e308f241d60fce0d6ad0f57add0759`,
  source `fe8f08bf128791f1d64f227524b9a0585ef333d5`. Its canonical state is
  `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; it is a static audit input, not a
  substitute for the unknown observed binary.
- Canonical accepted compatibility input: project UUID
  `3f28bc98-058b-57a2-967b-81ad8e35f957`, current/accepted release
  `0.1.0-canary3`, artifact
  `xaero-emf-entity-icon-compat-0.1.0-canary3.jar`, SHA-256
  `4f34d743f5fffd8e938c8f5157c630fd85f3b263ac1ae9f96c432cfe51668df2`,
  source `2478f021982c5f26e51ff0579fa2f5dad85ed200`.
- The dedicated `Matcha Flavoured 26.2 Workbench` was inspected read-only for
  exact files, configuration, resource-pack selection, runtime-manager state,
  and existing logs. Its files and launch state were not changed.
- The protected 26.1.2 gameplay instance was not accessed. `originals/` was not
  changed. No private model content, texture content, upstream class body, or
  ARR asset is reproduced here.

## Exact environment and artifacts

The `Present` rows below are byte identities in the dedicated 26.2 profile at
audit time. `Static only` rows were inspected from the canonical project or
dependency cache and were not present in that profile.

| Input | Exact identity | Bytes | SHA-256 | Presence and use |
|---|---|---:|---|---|
| Minecraft | 26.2 | — | — | Present; version reported by the current log |
| Fabric Loader | 0.19.3 | — | — | Present; version reported by the current log. The profile contains no standalone Loader JAR to hash. |
| Fabric API | `fabric-api-0.157.0+26.2.jar` | 2,533,297 | `acb7dc90a0430519c49548074d3fbf6fd81d13063f08f0af344b2a6b08a42620` | Present |
| Xaero Minimap | `xaerominimap-fabric-26.2-26.4.2.jar` | 2,221,925 | `69284892d2eb853c9aefa85a4c9b74232c322da00207994c67ab8aeed8a64048` | Present and inspected; contains nested XaeroLib 1.7.1 |
| Xaero World Map | `xaeroworldmap-fabric-26.2-1.44.2.jar` | 1,473,719 | `d55ef45c559ae0adcf66d894c022f61d9d921629b0c885d04aa00424546a2389` | Present; relevant to the accepted binary-contract suite, not the first failure |
| EMF | `entity_model_features-3.2.6-26.2-fabric.jar` | 587,342 | `876a3e4ffda021a6266df87208f2d9980322cf86223d4fe1e313ca996631f115` | Present |
| ETF | `entity_texture_features-7.1.1-26.2-fabric.jar` | 762,131 | `f469bc914302a13a5c767296623df60fb0cc3d4e4a02a77c56541a733ad36e3a` | Present |
| Accepted EMF companion | `xaero-emf-entity-icon-compat-0.1.0-canary3.jar`, version `0.1.0-canary3` | 28,351 | `4f34d743f5fffd8e938c8f5157c630fd85f3b263ac1ae9f96c432cfe51668df2` | Present; exact canonical accepted artifact |
| Ribbits | No Ribbits file present | — | — | Not present. All 213 current root mod JAR manifests and all available current/historical profile logs were checked; the observation's exact filename, version, size, and hash remain unproven. |
| Canonical Ribbits C6 | `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary6.jar`, version `4.1.6+26.2-mynx-canary6` | 3,222,195 | `cbeedd06bab0d6809fd528662e4c7e4f11e308f241d60fce0d6ad0f57add0759` | Static only; exact ignored private artifact matching the canonical current record; **not deployed and not treated as observed** |
| GeckoLib | No GeckoLib file present | — | — | Not present and absent from available profile logs |
| GeckoLib audit input | `geckolib-fabric-26.2-5.5.1.jar`, version 5.5.1 | 703,096 | `4bf1c86b4b47aa2c5d84208255695f10d79d609b23d802c995711e64b45cfce0` | Static only; exact cached dependency required by Ribbits C6 |

The current log reports the compatibility mod in Fabric's discovered mod list.
Its mixin plugin is intentionally silent, so the log alone cannot prove an
individual mixin application. Exact version matching plus the reproduced
binary-contract tests prove that Canary 3's activation contract accepts the
current Xaero 26.4.2/EMF 3.2.6 pair; no mismatch exception appears in the
available logs.

### Relevant resource packs

The active `options.txt` pack order was inspected. Every active ZIP and
directory was also checked for an `assets/ribbits/` namespace; none has one, so
no active pack supplies or overrides a Ribbit model, texture, or Xaero icon.
These are the active entity-model packs material to this audit:

| Enabled pack | Bytes | SHA-256 | Relevance |
|---|---:|---|---|
| `FreshAnimations_v1.10.5.zip` | 645,816 | `cf9f17a2977e171b33cb0b598bc4357dd0383e09c10d5f768ff17c12d0a028ee` | Exact EMF pack covered by Canary 3 tests; vanilla entities only |
| `Mizuno x Fresh Animations 4.7 beta3.zip` | 707,399 | `a29b18f63f9157668d6ef8d68017e839249c87e668a59dc3e12bc8520bcf670c` | Vanilla-entity derivative; no Ribbits namespace |
| `Mizuno's Pig Variant x FA 3.2.zip` | 1,056,081 | `eb499b611a26029c44bd32fd71be80076b109ff98ed07fa8c75c06636ca00695` | Vanilla pig only |
| `Mizuno's Cow Variant x FA 3.2.zip` | 609,616 | `f1576e9f64bf2c6e34e5ae87212bd09874b02787877655f0536bb9e0750cbd88` | Vanilla cow only |
| `Mizuno's Chicken Variant x FA 1.3.zip` | 184,093 | `618ea02e64d90437db3611e2c6abd85b61aeb50f9153511f7a1a8d80ae22e00f` | Vanilla chicken only |
| `Enhanced Shulkers x Fresh Animations.zip` | 1,043,188 | `ebd4400917cb4363f1fdd26653cca7cc8ce8abfa9c7f3a8ef4a11e63bb8c9868` | Vanilla shulker only |

`Matcha_Flavoured_1_10.zip` was also active (12,177,511 bytes, SHA-256
`b36faa3c0247c3b96dedcb1131c77989c231c828a8b332f1bbaa16ccf8ef776d`)
and contains no `assets/ribbits/` namespace.

### Xaero entity-radar configuration

- `display_radar=true` in the active `default` minimap profile.
- The active root category hard-includes `anything`; the living and friendly
  branches accept the Ribbit, and only vanilla item-frame types are excluded.
  There is no `ribbits:ribbit` exclusion or passive-mob filter.
- `icons:2.0` maps to **Always** and `displayNameWhenIconFails:true`.
  `entityNumber:1000`, `heightLimit:20`, and `iconScale:1.0` do not establish a
  Ribbits-specific rejection.
- `debug_entity_icons=false` and `debug_entity_variant_ids=false`; consequently
  no per-Ribbit icon diagnostic was expected in the retained logs.

The current category configuration proves eligibility, not the historical
observation's exact range/category context. A truly absent marker would require
an earlier visibility/range/category condition; the `FAILED` branch by itself
produces a dot when the entity is otherwise eligible.

## Renderer and icon call graph

```text
Fabric client registration
  EntityRendererRegistry.register(RIBBIT, RibbitRenderer::new)
    -> RibbitRenderer extends GeoEntityRenderer<RibbitEntity, R>
       owns RibbitModel extends GeoModel<RibbitEntity>

Xaero RadarIconManager.get(ribbit, ...)
  -> dispatcher obtains RibbitRenderer
  -> RibbitRenderer.createRenderState(ribbit, 1.0F)
     -> extractRenderState(...)
        -> GeckoLib fillRenderState(...)
           -> capture defaults
           -> RibbitRenderer.addRenderData(...) [five GeckoLib data tickets]
           -> model/layer data and animation/controller extraction
  -> choose icon form/variant and cache key
  -> RadarIconCreator.create(..., RibbitRenderer, populated state, ...)
     -> EntityRenderTracer.trace(...)
     -> EntityRenderTracer.getEntityRendererModel(RibbitRenderer)
        -> LivingEntityRenderer? no
        -> EnderDragonRenderer? no
        -> null
     -> restore/end icon form; return RadarIconManager.FAILED
        [FIRST INCOMPATIBLE BOUNDARY]

Never reached for Ribbits:
  RadarIconModelFormPrerenderer.prerender(... EntityModel ...)
    -> RadarIconModelPrerenderer.renderModel(... Model ...)
       -> vanilla ModelPart render/compile trace
       -> Canary 3 RETURN injection, only after upstream rendered nothing
          -> EmfIconPartResolver(... ModelPart root, trace ...)
```

### Ribbits and GeckoLib path

Current source establishes the renderer registration in
`projects/ribbits-26.2/fabric/src/main/java/com/yungnickyoung/minecraft/ribbits/fabric/client/RibbitsFabricClient.java`
and the renderer/model pair in
`common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/render/RibbitRenderer.java`
and `common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/model/RibbitModel.java`.

Exact GeckoLib 5.5.1 bytecode establishes that `GeoEntityRenderer` submits
through `GeoRenderer.performRenderPass`, `RenderPassInfo`, a
`BakedGeoModel`, `GeoBone`/`CuboidGeoBone`, `GeoCube`/`GeoQuad`, and a
`VertexConsumer`. `GeoModel` is not `net.minecraft.client.model.Model`, the
baked bone tree is not `ModelPart`, and this path never calls
`ModelPart.render` or `ModelPart.compile`. EMF therefore supplies no vanilla
root or retained head to Xaero for this renderer, and Canary 3's EMF compile
trace cannot observe it.

Xaero creates the full renderer state before icon creation. The timing is not
the current defect: `RibbitRenderer.addRenderData` has populated Ribbit data,
instrument-playing, umbrella-falling, rain, and Pride tickets before a future
Geo-aware form would resolve the model and texture. A future bridge must reuse
that same populated snapshot on the render thread; constructing a bare
`GeoRenderState` would omit `DT_RIBBIT_DATA`, which `RibbitModel` dereferences.

## Accepted Canary 3 guard matrix

| Guard or seam | Canary 3 behavior | Exact Ribbit result |
|---|---|---|
| Required mod containers | Plugin declines when Xaero or EMF is absent | Both are present in the current profile |
| Exact versions | Supports only Xaero 26.4.2 and EMF 3.2.6; a present mismatch throws and fails closed | Current pair matches; not the failure |
| Upstream result | `RadarIconModelPrerenderer` RETURN injection acts only when Xaero's destination remains empty | Not evaluated: Ribbit returns `FAILED` before this prerenderer |
| Model contract | Patched signature remains vanilla `Model`/`EntityModel` and `ModelPart` | GeckoLib supplies `GeoModel`/`BakedGeoModel`/`GeoBone` |
| Exact EMF root | Resolver accepts only `traben.entity_model_features.models.parts.EMFModelPartRoot` | No EMF root exists; a forced call would reject it |
| Retained vanilla root | Reflected `vanillaRoot` must be a `ModelPart` | No retained vanilla root exists |
| Canonical identity | Failed main part must be the direct root `head` or `head_parts`; mapped EMF canonical part must be empty while retained vanilla canonical head has direct cubes | No vanilla canonical part exists |
| Trace and geometry | Candidate must have been traced and contain cubes; the permitted subtree must yield exactly one matching geometry cube within tolerance (semantic candidate ordering itself is deterministic) | Gecko bones never enter the `ModelPart` trace |
| Transform safety | Canonical/live frames must be finite and invertible | Not evaluated |
| EMF compile bridge | `EMFModelPart.compile` forwards to Xaero's model-part render detection | Gecko baked rendering never calls `EMFModelPart.compile` |

Canary 3 contains no GeckoLib, `GeoEntityRenderer`, `EntityRenderTracer`, or
`RadarIconCreator` reference. Its three mixins remain confined to EMF model
part compilation and the two Xaero vanilla-model prerender seams. This is the
documented and accepted scope, not a regression.

## Ribbits model and render-state matrix

| State | Model resolution | Texture resolution | Icon implications |
|---|---|---|---|
| Normal adult | Profession model | Profession texture | Nine professions; model and texture identity must be in the cache discriminator |
| Playing a valid instrument, non-Mynx visual profession | Instrument model; highest priority | Profession texture | Transient instrument geometry must not become head geometry; this branch wins over umbrella/rain/Pride |
| Playing instrument, Chef/Farmer/Prospector/Guard | Instrument branch deliberately skipped | Profession texture | Uses the next applicable state or profession model |
| Umbrella-falling or in rain | `umbrella/<profession>/<umbrella variant>` | Profession texture | Wins over Pride; recursive `body` capture would include umbrella/accessory geometry |
| Pride | `pride_ribbit`, after umbrella branch | Profession texture | Applies only when higher-priority branches do not; needs an explicit stable cache/visual policy |
| Baby | Same state-selected model | Same profession texture | `RibbitRenderer.scaleModelForRender` multiplies both dimensions by `ageScale` (0.5 for a baby in MC 26.2); bridge must deliberately normalize or preserve it |

The exact private C6 archive contains 41 GeckoLib model entries: 9 profession,
27 umbrella, 4 instrument, and 1 Pride model. A deterministic manifest of
`path<TAB>entry-SHA-256<LF>` has SHA-256
`0d41371da10e5328a803ed960914de54b6bf7f28e19133e2b0bc5305cfa0060c`.
Minimal
structural inspection found:

- all 41 use top-level `main` with direct child `body`;
- zero of 41 has any case-insensitive `head` bone;
- `body` is the only stable face/core candidate, but a recursive body render
  includes arms, hats, instruments, fishing accessories, or umbrellas in many
  models;
- direct `body` geometry is not uniform: 29 models have 3 direct cubes, the 8
  Chef/Farmer normal-or-umbrella models have 4, three Merchant variants have 9,
  and one Merchant umbrella variant has 10;
- Guard spear/shield parts can be top-level siblings rather than body children;
  a whole-model capture would include them;
- the renderer suppresses a bone literally named `instrument`, while the 41
  current models contain no such bone and instead expose instrument-specific
  names. This is a future icon-contamination warning, not the first Xaero
  failure and not proof of an in-world renderer defect;
- Ribbits registers no Geo render layer on `RibbitRenderer`. The supporter-hat
  layer belongs to the player/avatar renderer, so it cannot contaminate a
  Ribbit icon. Accessories and umbrellas are model bones and can;
- profession textures can be resolved from the already-populated render-state
  snapshot without mutating or replaying the live world renderer. Nitwit,
  Gardener, Sorcerer, Fisherman, and Merchant share the base texture; Chef,
  Farmer, Prospector, and Guard have profession-specific textures.

There is therefore no safe generic `head`-by-name rule for Ribbits. A correct
bridge can still be generic at the renderer/capture level, but Ribbits needs an
explicit, opt-in selector describing the direct face/core geometry and inherited
transform chain. The selector must fail closed when the model resource or
structure is unknown or ambiguous.

## Symptom and cause classification

| Question | Finding |
|---|---|
| Exact observed symptom | Only “no correct head icon” is supported. The historical Ribbits binary and screenshot/log context are unavailable, so absent vs generic fallback is unresolved. |
| Predicted current-config symptom | Eligible Ribbit → cached `FAILED` → colored radar dot; because icons are Always and `displayNameWhenIconFails=true`, also show the Ribbit name when labels are allowed. Not a blank texture or malformed model. |
| Is accepted C3 loading? | Its mod is discovered, the exact versions satisfy its activation contract, and its unmodified binary tests pass. Individual runtime mixin application is not logged, but no mismatch/decline evidence exists. |
| Accepted-patch regression | **No.** Ribbits never reaches the seam C3 repairs. |
| Intentional scope gap | **Yes.** C3 is explicitly an EMF-backed vanilla `ModelPart` adapter. |
| Xaero/GeckoLib interoperability gap | **Yes; primary cause.** Xaero's renderer-model extractor rejects `GeoEntityRenderer`, and the downstream model representations are incompatible. |
| Ribbits renderer/model defect | **No evidence.** Ribbits follows GeckoLib 5.5.1's renderer/model contract. Its lack of a `head` bone is a selector/design constraint after the first failure. |
| Configuration/filter defect | **No evidence in the current profile.** Heads are enabled and categories admit the entity. Historical range/category state is not recoverable. |
| Multiple independent causes | One first cause (Xaero's null vanilla-model extraction), followed by a real second design barrier (`GeoBone` versus `ModelPart` and no canonical head bone). Runtime artifact identity remains an evidence gap, not another cause. |

## Alternatives considered

| Owner/design | Assessment |
|---|---|
| **Distinct, generic Xaero × GeckoLib bridge with opt-in entity selectors** | **Recommended.** Reuses renderer-state and Geo capture plumbing for other GeckoLib mobs while letting Ribbits describe its nonstandard face/core safely. Keeps Xaero ARR and Gecko API coupling out of Ribbits and out of accepted C3. |
| Separate Ribbits-only Xaero adapter | Technically possible and a reasonable fallback if no stable generic provider contract emerges, but duplicates Geo capture plumbing and couples maintenance to one entity/mod. |
| Extend the accepted Xaero × EMF companion | Rejected. GeckoLib is a different model family and fails before the C3 hook. Widening C3 increases regression risk for an accepted artifact without gaining a sound type contract. |
| Xaero static/custom icon definition | Supported by Xaero (`model`, dot, normal/outlined sprite, item, variants, and a custom variant method). A small licensed sprite set is lower runtime risk, but a complete profession/state set requires assets not currently available for redistribution, is Ribbits-specific, and does not solve GeckoLib generically. Default texture identity also cannot distinguish all umbrella/Pride/instrument model states. |
| Change Ribbits itself | Rejected as primary ownership. `RibbitRenderer` is a normal GeckoLib renderer; adding a vanilla model façade or Xaero dependency would pollute normal rendering and couple an entity mod to an ARR minimap implementation. |

## Recommended implementation direction

Keep production ownership in this distinct compatibility project and build a
**generic Geo-aware core plus an explicit Ribbits selector/provider**. Do not
modify the accepted C3 source or the Ribbits world renderer.

1. Hook at or immediately before `RadarIconCreator.create(...)`'s
   `getEntityRendererModel(...) == null` → `FAILED` branch. Activate only for a
   supported `GeoRenderer` and exact audited Xaero/GeckoLib contracts. Preserve
   Xaero's ordinary path unchanged for vanilla, EMF, and all unsupported
   renderers.
2. Consume the `GeoRenderState` already produced by
   `RibbitRenderer.createRenderState/extractRenderState`. From that one
   immutable snapshot, resolve `GeoModel.getModelResource`,
   `getTextureResource`, and the matching `BakedGeoModel`.
3. Render through a dedicated Geo icon form into Xaero's icon framebuffer and
   outline pipeline. Do not fabricate `EntityModel`/`ModelPart`, mutate the
   in-world renderer, or replay world rendering with live animation side
   effects.
4. Require an opt-in selector/provider. For Ribbits it must bind a known model
   identity/structural fingerprint to the direct intended face/core geometry,
   apply the inherited `main` → `body` transform, and exclude descendants and
   siblings unless individually approved. There is no valid generic `head`
   name heuristic for these assets.
5. Start with stable identity semantics: profession model plus profession
   texture. Exclude transient instrument, umbrella/rain, held item, spear,
   shield, and accessory geometry from the head capture. Decide explicitly,
   with runtime evidence, whether Pride is a distinct stable icon identity and
   whether baby scale is normalized (preferred for legibility) or retained.
6. Include renderer/provider version, resolved model ID, texture ID,
   profession, any approved stable visual state, scale policy, and resource
   reload generation in the cache key. Xaero caches `FAILED` as well as valid
   results by entity type/variant/armor, so a resource reload or provider-state
   transition must invalidate prior failure entries. Avoid keying on animation
   time, rain ticks, or other transient data that causes unbounded churn.
7. Fail closed to Xaero's ordinary `FAILED` behavior when the renderer is not a
   supported Geo renderer, a required data ticket is absent, a model/texture is
   missing, a selector is absent or ambiguous, a structural fingerprint drifts,
   a transform is non-finite/singular, or framebuffer rendering fails. Never
   replace that with whole-body capture.

### Coupling and legal constraints

- Xaero's hook is an exact-version, ARR binary contract. Use narrow mixins and
  signatures/behavioral tests; do not copy or redistribute Xaero classes.
- GeckoLib 5.x APIs and baked-model internals are a second independent binary
  contract. Gate versions and fail closed on drift.
- Ribbits' private/donor-derived model and texture assets remain private. Store
  only selector metadata or hashes that are necessary and lawful; do not copy
  model coordinates, textures, donor assets, or private manifests.
- A sprite-based fallback may use only newly created or expressly licensed
  assets. Current private profession textures are not implicitly reusable.
- The bridge must run on the render thread, isolate pose/buffer/framebuffer
  state, and restore state on every exit. It must not change world rendering.

## Focused future runtime matrix

Only run this matrix after a separate task builds a candidate, records exact
artifact identities, and receives explicit test-slot/deployment ownership.

| Case | Required observation |
|---|---|
| Adult Nitwit, normal/idle | Correct face/core and base texture; no body/accessory geometry |
| Adult Chef and Farmer | Private-profession texture and direct-body geometry variants remain centered and unclipped |
| Adult Merchant | Higher direct-body cube count does not capture clothing/body descendants or clip bounds |
| Adult Guard | No spear/shield sibling contamination |
| Baby representative | Deliberate scale policy is consistent and legible; cache cannot reuse a wrong adult transform if policy differs |
| Pride-eligible Nitwit | Explicit chosen Pride policy, stable variant/cache identity, no stale normal icon |
| Playing instrument | Stable head remains clean; no instrument model/bones or cache churn |
| Rain and each umbrella family | Stable head remains clean; no umbrella/accessory capture |
| State transitions | profession/state change, leaving rain, stop playing, resource reload, save/reload, and restart invalidate/reuse exactly the intended keys |
| Fail-closed controls | unknown model, missing provider, ambiguous selector, or changed structural hash produces Xaero dot/name fallback without crash or whole-body icon |
| Regression controls | one vanilla head, one Fresh Animations/EMF head through accepted C3, one unsupported non-Ribbit Geo entity, entity heads off, and a category-excluded entity remain unchanged |

Pass requires correct face-only geometry, intended texture and centering, stable
cache behavior, no accessory/layer contamination, no world-render change, and
no regression in vanilla or accepted EMF icons. Compilation, audit probes,
screenshots outside Minecraft, and the existing C3 suite are not runtime
validation.

## Controlled validation performed

- Re-ran the existing `projects/xaero-entity-icons` suite **unmodified** in a
  disposable temporary copy with Java 25, Gradle 9.5.1, Loom 1.17.19, and the
  exact hashed Xaero 26.4.2, World Map 1.44.2, EMF 3.2.6, ETF 7.1.1, and Fresh
  Animations 1.10.5 inputs. `clean test build --offline --no-daemon` completed
  successfully: 35 tests in 6 suites, zero failures/errors/skips, and the
  client-only packaging verification passed.
- Inspected exact Xaero and GeckoLib bytecode with controlled `javap`/archive
  probes; no decompiled class body was retained or committed.
- Rehashed the tracked C3 artifact, the dedicated-profile inputs, the ignored
  private C6 artifact, and the cached GeckoLib dependency.
- Ran the audit-only structural probe against C6; it confirms the source
  inheritance contracts, exact C3 identity/scope strings, exact C6 identity,
  41 model entries, the stable `main` → `body` hierarchy, and absence of a
  `head` bone without exposing model coordinates.
- Exhaustively inspected current mod manifests, the relevant active config and
  pack order, all active resource packs for a Ribbits namespace, and every
  available dedicated-profile log. This was read-only and is not Minecraft
  runtime validation.

## Exclusions and non-actions

No production fix, successor JAR, build of Ribbits, deployment, Minecraft
launch, test-slot operation, accepted-baseline change, runtime-state change,
profile/config/resource-pack/save mutation, protected-instance access, or
change to either input project occurred. The historical user observation
remains unbound to an exact Ribbits binary; a future runtime task must establish
that identity rather than infer it from C6.

## 2026-09-04 external/manual failure reconciliation addendum

This addendum supersedes earlier present-tense acceptance conclusions without
rewriting their historical evidence. Starting main:
`a4f16ccceb1d64b1056c8556c4c6e8af187011a5`.

The user withdrew EMF C3's aggregate PASS and reported no correct icons for wolf,
bat, axolotl, parrot, frog, allay, sniffer, iron golem, vex, ravager and witch;
Ribbits C1 also had no correct head icon. No exact fallback subtype or unlisted
pass is established. Both projects now have no accepted release or rollback.
Earlier sheep/horse/turtle/creeper observations remain in the unchanged historical
logs and cannot imply broader or individual C3 coverage.

### Read-only external profile and session binding

The user authorized `Matcha 26.2 Player Instance` for read-only diagnostics only.
Both projects' TESTING.md record the complete boundary. It is not a manager slot.
No protected-profile access, game/launcher control or profile write occurred.
Relevant logs were copied to task-local private scratch outside the profile.
Capture metadata records source path, UTC capture time, byte size, SHA-256,
source modification time and whether size/mtime stayed stable during each read.
All reads in the successful capture were stable; latest.log remains labeled live.
Raw logs/resource archives/configs are not committed or published.

The dedicated Workbench's 179 available log files had no Ribbits/GeckoLib/C1
mod-list entry. Its latest launch at 2026-09-04 17:26:26 local time is not the
reported Ribbits launch. The external profile supplies these relevant sessions
(2026-09-04, America/Chicago, UTC = local + 05:00):

| Log | Launch local | Ribbits | C1 | GeckoLib | Stop local |
| --- | --- | --- | --- | --- | --- |
| 2026-09-04-1.log.gz | 15:59:20 | C7 | absent | 5.5.4 | 16:05:33 |
| 2026-09-04-2.log.gz | 16:06:49 | C7 | present | 5.5.4 | 16:24:10 |
| 2026-09-04-3.log.gz | 16:37:16 | C7 | present | 5.5.4 | 16:48:06 |
| 2026-09-04-4.log.gz | 16:48:56 | C8 | present | 5.5.4 | 16:55:36 |
| 2026-09-04-5.log.gz | 17:03:19 | C8 | present | 5.5.4 | 17:06:55 |
| 2026-09-04-6.log.gz | 17:07:15 | C8 | present | 5.5.4 | 17:23:47 |
| 2026-09-04-7.log.gz | 17:37:39 | C8 | present | 5.5.4 | 17:41:47 |
| latest.log captured after 23:02 UTC | 17:54:31 | C9 | present | 5.5.4 | 18:01:43 |

Every listed session discovers Minecraft 26.2, Fabric Loader 0.19.3, Xaero
Minimap 26.4.2, nested XaeroLib 1.7.1, World Map 1.44.2, EMF 3.2.6, ETF 7.1.1,
EMF C3 and retired Diagnostic1. C1 is discovered only where indicated. The user
has not bound a particular visual observation to a particular session. Archive
mod lists prove versions, not historical byte hashes. Current hashes below were
captured separately at 2026-09-04T23:04:47Z and must not be backdated.

### Captured current external artifact identities

| Filename | Version | Bytes | SHA-256 |
| --- | --- | ---: | --- |
| `entity_model_features-3.2.6-26.2-fabric.jar` | `3.2.6` | 587,342 | `876a3e4ffda021a6266df87208f2d9980322cf86223d4fe1e313ca996631f115` |
| `entity_texture_features-7.1.1-26.2-fabric.jar` | `7.1.1` | 762,131 | `f469bc914302a13a5c767296623df60fb0cc3d4e4a02a77c56541a733ad36e3a` |
| `fabric-api-0.157.0+26.2.jar` | `0.157.0+26.2` | 2,533,297 | `acb7dc90a0430519c49548074d3fbf6fd81d13063f08f0af344b2a6b08a42620` |
| `geckolib-fabric-26.2-5.5.4.jar` | `5.5.4` | 1,183,876 | `a5770f9ea0c21db157559fe266874fd84be8c7da689d37aa7bf2b06304a6a65d` |
| `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary9.jar` | `4.1.6+26.2-mynx-canary9` | 3,333,513 | `e433cd048bc362edae91e2e057c8170d92d110cbe7b9917c105c7336be6543de` |
| `ribbits-xaero-entity-icons-compat-0.1.0-canary1.jar` | `0.1.0-canary1` | 52,119 | `dac701cb5bb1ec3d42ed9dc642bb224235b2e1cf1dd84528557ef3ab494f585a` |
| `xaero-emf-entity-icon-compat-0.1.0-canary3.jar` | `0.1.0-canary3` | 28,351 | `4f34d743f5fffd8e938c8f5157c630fd85f3b263ac1ae9f96c432cfe51668df2` |
| `xaero-entity-icon-diagnostic-0.1.0-diagnostic1.jar` | `0.1.0-diagnostic1` | 6,742 | `05c41f987909276a29cf106f8cbe138e83f6dcbd4dde401f193f21cee31ef440` |
| `xaerominimap-fabric-26.2-26.4.2.jar` | `26.4.2` | 2,221,925 | `69284892d2eb853c9aefa85a4c9b74232c322da00207994c67ab8aeed8a64048` |
| `xaeroworldmap-fabric-26.2-1.44.2.jar` | `1.44.2` | 1,473,719 | `d55ef45c559ae0adcf66d894c022f61d9d921629b0c885d04aa00424546a2389` |

Nested XaeroLib in each exact Minimap/World Map archive originates at
`META-INF/jars/xaerolib-fabric-26.2-1.7.1.jar`, 621,485 bytes, SHA-256
`7f4a78dd7e046fea0500fef83b1481d85317c8348d47e947035d7a07efe51065`.
The duplicate identical nested providers do not establish which parent Fabric
selected in a historical launch.
FreshAnimations_v1.10.5.zip is 645,816 bytes, SHA-256
`cf9f17a2977e171b33cb0b598bc4357dd0383e09c10d5f768ff17c12d0a028ee`.
The currently selected later `Frog_Villagers_x_Fresh_Animations_baby_fix_v4.zip`
is 952,955 bytes, SHA-256
`9edb6ad75b82a52214dcb8126d0f4af83f73704b5fdb3076e0e278280cc55aa7` and supplies
an iron_golem.jem override. Its current bytes are not proof of historical bytes.

### First Ribbits rejection and selector evidence

C1 checks whole-origin Xaero, nested XaeroLib, GeckoLib, then Ribbits identities.
Every evidenced C1 session uses GeckoLib 5.5.4, so the 5.5.1 gate rejects before
Ribbits geometry or provider registration can run. Later C8/C9 also mismatch
C1's exact C7 gate, but the C7/C8 mismatch is not the first rejecting condition.
No activation decision appears in normal latest.log; C1 uses System.Logger.
The runtime decision is inferred from exact plugin source and session versions,
not fabricated as an observed log message. No Geo prerender, selector or draw
failure is established. Preserve the Xaero/XaeroLib/GeckoLib gates for the next
controlled 5.5.1 cohort; do not silently treat manual 5.5.4 as supported.

Read-only C7/C8/C9 comparison proves that all 41 ordinary Geo model entries share
manifest SHA-256 `0d41371da10e5328a803ed960914de54b6bf7f28e19133e2b0bc5305cfa0060c`.
Each has one top-level main and direct body with nonempty direct cubes; counts
are 3x29, 4x8, 9x3, 10x1 (156 total). The separate Wandering model is excluded.
RibbitRenderer, RibbitModel and DataTicketModule class bytes also match across
all three releases. Current C9 is 3,333,513 bytes, SHA-256
`e433cd048bc362edae91e2e057c8170d92d110cbe7b9917c105c7336be6543de`, source
`8dc886c01f6d402ccf19b45756383d62185f0d4e`.
The literal main/body direct-cube rule needs no geometry change on this evidence.

Xaero 26.4.2 bytecode has its requiresEntityModel invocation at create offset 84
and prerender invocation at offset 222; the call precedes prerender on the
inspected path. C1's cached Boolean null guard is therefore not a demonstrated
failure on this binary, though the successor must remove that hidden assumption.
The initial javap inspection emitted an archive access error after producing the
method output; it is inspection evidence, not a passed production harness.

### EMF first-stage evidence boundary

No per-entity resolver trace/destination is present in the captured logs. C3's
version gate is eligible in these sessions, but silent plugin/resolver behavior
prevents claiming an observed per-entity first guard. The following are exact
base Fresh Animations structural inputs only, not reconstructed live EMF trees.
Retained vanilla canonical identity, live root class/path, trace admission,
transformed destination and first C3 rejection all remain unproven per row.

| Entity | Base JEM semantic path | Direct cubes | Descendant cubes | Runtime first guard/stage |
| --- | --- | ---: | ---: | --- |
| wolf | body/head2 | 1 | 9 | unresolved |
| bat | body/head2 | 1 | 4 | unresolved |
| axolotl | body/head2 | 1 | 5 | unresolved |
| parrot | body/head2 | 2 | 5 | unresolved |
| frog | body/body2/head2 | 2 | 10 | unresolved |
| allay | body/head2 | 1 | 3 | unresolved |
| sniffer | body/head2 | 2 | 6 | unresolved |
| iron golem | body/head2 (base; current override exists) | 1 | 4 | unresolved |
| vex | body/head2 | 1 | 6 | unresolved |
| ravager | body/head2 | 2 | 10 | unresolved |
| witch | body/head2 | 1 | 8 | unresolved |

C3's actual static guards require a direct root head/head_parts, empty canonical
EMF subtree, retained vanilla direct cubes, a traced semantic candidate, exactly
one dimensional cube match and finite invertible frames. These are candidate
limitations to test, not one inferred common cause for all eleven mobs.
Neither successor is built at this evidence-correction checkpoint. Diagnostics,
exact controlled fixtures and production classloading remain implementation work.

The manager has neither project in A/B. C3's stale accepted-baseline entry is
preserved under the explicitly restricted project-only scope; clearing project
acceptance does not change physical deployment or claim an accepted promotion.

# BGE × Complementary Canary 1 audit

**Implementation baseline:** BGE C79
`4.2.23-bge.canary79.cnm-family-bridge+26.2`, SHA-256
`7e3265c8746547465ede1a48b31c3db06370de84331b8ff9e35a2ce450650a91`; Iris
`1.11.2+mc26.2`, recorded SHA-256
`df0e2ccddaea17b191eda32b21c979e131bc9d4ef4f831113b50b461fc4a3804`.

**Runtime evidence:** none. This records source, bytecode, archive, and build
inspection only; Canary 1 is `RUNTIME_UNTESTED`.

## Decision

The private Workbench-owned bridge is technically viable without changing Iris
or Complementary. It injects at `RETURN` of Iris 1.11.2's static
`BlockMaterialMapping#createBlockStateIdMap(Int2ObjectLinkedOpenHashMap,
Int2ObjectLinkedOpenHashMap)`, after Iris has parsed and precedence-resolved
the active pack's `block.properties` into its mutable
`Object2IntMap<BlockState>`. It fills only absent BGE physical states from the
existing state-specific result for their BGE-projected canonical parent.

Iris's same class separately returns the `layer.*` render-type map from
`createBlockTypeMap(Map)`. Canary 1 also applies a block-wide fallback at that
completed map. It is needed as a defensive compatibility measure: the exact
Complementary archive declares `layer.translucent` for canonical glass, while
BGE's existing Minecraft/Fabric translucent type remains its primary render
layer authority. An explicit physical BGE `layer.*` entry always wins, and an
unmapped parent adds no override.

The hook is exact-version-gated. It does not apply when Iris is missing or its
friendly version is anything other than `1.11.2+mc26.2`, nor unless loaded BGE
is exactly C79. That leaves ordinary BGE rendering untouched outside the
audited stack.

## Exact Iris path

The Iris 26.2 release branch source and the recorded exact 1.11.2 bytecode
contract establish this lifecycle:

1. `IdMap` reads and preprocesses the active shader pack's
   `shaders/block.properties`, parses `block.*` material rules and `layer.*`
   render-type rules separately.
2. `BlockMaterialMapping#createBlockStateIdMap` expands resolved block and tag
   entries to individual `BlockState` keys. Its `putIfAbsent` semantics give
   Iris's completed pack mapping precedence.
3. On the rendering pipeline's first `beginLevelRendering`,
   `IrisRenderingPipeline` calls `createBlockStateIdMap`, then
   `createBlockTypeMap`, and stores both through
   `WorldRenderingSettings#setBlockStateIds` / `setBlockTypeIds`.
4. Those settings mark a reload when changed. A new shader pipeline after pack
   enable/disable, pack switch, shader/resource reload, or relevant level
   lifecycle repeats construction, so Canary 1 holds no independent cached ID.

The bridge touches neither `IdMap`, parser input, `WorldRenderingSettings`, nor
the shader pack. Its `RETURN` fallback is therefore downstream of Iris parsing
and upstream of the maps becoming renderer authority.

## BGE parent authority and Canary 1 eligibility

For every BGE-owned physical state, the bridge asks the current authoritative
`BgeMaterialBindings.Binding` for `canonicalState`. BGE performs the projection:
only real material state such as axis, leaf state, snow, waterlogging, and
glazed pattern is retained as appropriate; slab type, Layer count, facing,
stair shape, wall arms, and other geometry state are not treated as canonical
material state. No physical registry name is parsed or inventoried.

Canary 1 admits only these canonical parents:

- clear glass and all sixteen exact stained-glass colors;
- iron, gold, diamond, and emerald blocks;
- glowstone and sea lantern.

Leaves, foliage, vines, crops, waving plants, fluids, lily-pad/water-like
forms, portals, beacons, and block-entity-specific paths are explicitly
withheld. The policy is a small canonical-parent set, not a BGE-derived-ID
table and not a copy of Complementary material numbers.

## Precedence and diagnostics

`containsKey` is used, never a default material-ID sentinel. Consequently an
explicit physical state mapping—including zero or another special value—wins;
only a physically absent key may inherit. State IDs remain state-specific.
`layer.*` is inherently block-wide in Iris and follows the same explicit-key
precedence.

Each mapping construction emits one aggregate material line with loaded Iris
and BGE versions plus inherited, explicit, missing-parent, ineligible, and
missing-projection counts. A second aggregate line appears only when a
`layer.*` map has relevant inherited or explicit BGE entries. No individual
block is logged.

## Immutable references and historical proposal

Complementary Unbound r5.8.1 at
`originals/shaderpacks/ComplementaryUnbound_r5.8.1.zip` was read only and
retains SHA-256
`bb89b1fc54687d4147a837fb2e3c3f7261a13bee51819761e9b6a91cb7915965`.
No archive bytes, saved shader configuration, Iris JAR, BGE source, or BGE ×
CTM source were modified or packaged. `UPSTREAM_PROPOSAL.md` is retained as
historical scalable-pack research; upstream approval is no longer an
implementation prerequisite for this private, version-coupled bridge.

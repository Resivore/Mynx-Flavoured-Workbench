# BGE × Complementary Canary 4 audit

**BGE API baseline (static validation only):** current authoritative `main` BGE
C80, `4.2.24-bge.canary80.cnm-two-phase+26.2`, SHA-256
`5bc7c23a3724a1e20bc2142459f33d2a45a19295c27d40197c5fc48d6e43b0fc`.
Canary 4's runtime dependency is not this release string: it requires BGE to
be present and expose the authoritative canonical-binding API consumed by the
bridge: `BgeMaterialBindings.all()`, `Binding#physicalBlock()`,
`Binding#canonicalMaterial()`, and `Binding#canonicalState(BlockState)`.
Iris remains `1.11.2+mc26.2`, recorded SHA-256
`df0e2ccddaea17b191eda32b21c979e131bc9d4ef4f831113b50b461fc4a3804`.

**Supplied Canary 3 startup evidence:** Canary 3 reached Mixin preparation but
crashed before client startup with `MixinTargetAlreadyLoadedException` for BGE
× CTM's legitimate `@Mixin(Block.class)` target,
`net.minecraft.world.level.block.Block`. Canary 3's Mixin-config plugin called
the BGE canonical-binding reflection probe, which resolved `Block`/`BlockState`
before BGE × CTM could prepare its Mixin. Sodium's preLaunch entrypoint is only
where that failure surfaced; it is not the cause. This is startup-failure
evidence for the exact Canary 3 bytes, not any shader or material-runtime
result. Canary 4 is `RUNTIME_UNTESTED`.

## Decision

Canary 4 retains one universal rule for every physical block/state represented
by BGE's authoritative `BgeMaterialBindings`:

```
physical BGE state
    -> BGE Binding#canonicalState(physical state)
    -> existing Iris material classification, if present
```

The bridge copies that existing classification only when Iris's completed
state map lacks a physical entry. It does not choose categories, material IDs,
or a geometry-safety policy. BGE alone supplies physical-to-canonical identity
and the state projection; the active shader pack alone supplies the meaning of
the canonical state. A canonical state without an Iris entry produces no new
physical entry.

Iris's `layer.*` map is block-wide. For every BGE binding, Canary 4 uses the
same absent-physical-entry rule with the binding's authoritative canonical
block. A completed Iris layer mapping for that canonical block is copied only
when Iris did not explicitly map the physical BGE block.

## Exact Iris path

The optional mixin injects at `RETURN` from Iris 1.11.2's
static `BlockMaterialMapping#createBlockStateIdMap(Int2ObjectLinkedOpenHashMap,
Int2ObjectLinkedOpenHashMap)` and `#createBlockTypeMap(Map)`. Both maps have
already been parsed and precedence-resolved from the active shader pack before
the bridge sees them.

The Mixin plugin now makes only the exact Iris `1.11.2+mc26.2` hook decision;
its bootstrap bytecode contains no BGE API/probe reference and no Minecraft
`Block` or `BlockState` reference. It therefore does not inspect BGE or resolve
Minecraft block classes during Mixin configuration, `onLoad`, preLaunch, or
static initialization.

The BGE canonical-binding API probe runs only from the completed-map RETURN
hooks. At that late point `BgeLateRuntimeBridge` first verifies the reflected
API contract, then invokes `BgeShaderMaterialBridge`. A missing or changed
future BGE contract leaves the Iris map untouched and emits one controlled
warning. A `LinkageError`, including `NoSuchMethodError`, between successful
probing and the direct bridge call has the same no-op result. BGE × CTM remains
unmodified: its `@Mixin(Block.class)` can prepare normally because Canary 4
does not resolve that target during Mixin bootstrap.

The hook still declines to apply when Iris is absent or not exactly
`1.11.2+mc26.2`; its private descriptors are Iris-version-specific. BGE release
strings never control activation. The bridge does not parse or replace
`block.properties`, modify `IdMap` or `WorldRenderingSettings`, cache shader
material data, read registry-name patterns, or modify shader-pack content.

## Precedence, state, and policy invariants

`containsKey` is used instead of an ID sentinel, so an explicit physical
assignment—including zero—always wins. The material fallback asks BGE for each
exact physical state's projection; it never substitutes a generic default
canonical state. Geometry-only properties remain BGE's concern and do not
invent a parent mapping. `layer.*` uses the same completed-map precedence at
the block level.

There is no parent material-category allowlist or denylist. The shared
algorithm has no eligibility predicate, and focused coverage reads the bridge
source and its public algorithm shape to reject a new category-policy seam.
`minecraft:magma_block` is therefore not a bridge exception: like every other
authoritative canonical material, it inherits exactly when its canonical Iris
entry exists.

Aggregate diagnostics report inherited, explicit, missing-parent, and
missing-projection counts without individual block spam. No build or static
test is Minecraft runtime evidence.

## Focused static evidence

The twelve synthetic tests prove that a prior glass parent still inherits;
Magma Block and another parent outside Canary 1's former set inherit through
the same algorithm; explicit physical material and layer entries win; missing
canonical mappings remain absent; state-specific canonical projections stay
distinct; and reintroducing a parent-category policy fails the suite. They also
prove that Fabric metadata has no C79/C80 pin, the C80 baseline exposes every
consumed BGE API method, and the Mixin-bootstrap plugin contains no BGE or
Minecraft block reference while retaining its exact Iris guard. A focused
late-gate test proves unavailable capability and a simulated `NoSuchMethodError`
are controlled no-ops. The clean `check` task verifies the C80 baseline hash,
BGE binding presence, Fabric metadata, mixin configuration, required bridge
classes, and that the artifact does not bundle Iris, Complementary, or a shader
archive.

## Immutable references and ownership

Complementary Unbound r5.8.1 at
`originals/shaderpacks/ComplementaryUnbound_r5.8.1.zip` was read only and
retains SHA-256
`bb89b1fc54687d4147a837fb2e3c3f7261a13bee51819761e9b6a91cb7915965`.
No Complementary archive, saved shader configuration, Iris JAR, BGE source,
BGE × CTM source, `originals/` content, or Minecraft testing profile was
modified or packaged. `UPSTREAM_PROPOSAL.md` remains historical pack-side
research and is not a prerequisite or input to this client-only bridge.

# Proposal: canonical-parent block tags for BGE material compatibility

**Status:** ready for maintainer discussion; no local implementation is proposed

**Reference baseline:** BGE C79 (`4.2.23-bge.canary79.cnm-family-bridge+26.2`) and
Complementary Unbound r5.8.1 (SHA-256
`bb89b1fc54687d4147a837fb2e3c3f7261a13bee51819761e9b6a91cb7915965`).

## Proposed upstream destination

Start a maintainer discussion in the official [Complementary Discord](https://www.complementary.dev/discord/).
Complementary's official contact page directs suggestions and casual questions there.
The official [ComplementaryDevelopment/ComplementaryReimagined](https://github.com/ComplementaryDevelopment/ComplementaryReimagined)
repository is the code/PR destination, but its issue tracker currently reports that
new issue creation is restricted. If maintainers want a code contribution after
agreeing on the contract, the follow-up should be a narrow PR against that repository,
not a modified redistribution of a release archive.

## Summary

Block Geometry Extensions (BGE) creates alternate physical geometries for a
canonical Minecraft material: for example, a glass Layer, an iron slab, or a
gold stair. Current BGE C79 records the authoritative relationship between each
physical block and its canonical material in `BgeMaterialBindings`. It retains
the typed role, topology, state projection, and canonical `Block` identity; it
does not derive that relationship by parsing a generated block ID.

Iris and Complementary, however, classify the physical registry block seen by
the renderer. Complementary's `shaders/block.properties` consequently gives a
canonical block its material ID but does not know that its BGE-derived geometry
has the same material identity. A BGE block therefore cannot inherit the
canonical parent's Complementary classification unless Complementary lists that
physical ID or recognizes a tag containing it.

The requested interoperability seam is a small, pack-owned Iris tag extension:

```text
BGE canonical binding
  -> BGE-generated block tag for that canonical parent
  -> Complementary %namespace:tag selector beside the canonical mapping
  -> Iris assigns the existing Complementary material ID to the BGE geometry
```

Complementary remains the sole authority for shader material IDs and for which
material classes are eligible. BGE remains the sole authority for the factual
parent relationship. BGE never needs to know a Complementary numerical ID, and
Complementary never needs a maintained inventory of BGE's derived block IDs.

## Minimal contract

1. BGE would generate a stable **block** data-pack tag for each canonical parent
   represented by its bindings. A tag contains only BGE-derived physical blocks
   whose `BgeMaterialBindings.Binding` points to that exact canonical parent;
   it need not contain the parent itself because Complementary already maps the
   parent explicitly.
2. The tag is a neutral statement of canonical-parent identity, not a
   Complementary material registry. BGE would generate it from the binding and
   the canonical parent registry key, without inspecting a derived block's
   registry-name text and without hard-coding shader IDs or shader classes.
3. Complementary may opt into an individual canonical parent simply by appending
   that tag selector to the existing `block.<material-id>` mapping. A new BGE
   geometry in the same binding automatically becomes a member of that tag.
4. Complementary chooses which tags to reference. No tag reference means no
   shader-classification promise for that parent, even if BGE publishes the
   factual parent tag.

This is deliberately a future contract. BGE should not publish the namespace or
add data until its format and eligibility rules are accepted upstream, and this
proposal does not alter Complementary, Iris, or the supplied archive.

## Proposed BGE tag names

The proposed stable data location is:

```text
data/cnm_terrain_slabs_compat/tags/block/canonical_parent/<parent-namespace>/<parent-path>.json
```

The matching Iris selector is:

```text
%cnm_terrain_slabs_compat:canonical_parent/<parent-namespace>/<parent-path>
```

Examples:

```text
minecraft:glass             -> %cnm_terrain_slabs_compat:canonical_parent/minecraft/glass
minecraft:cyan_stained_glass -> %cnm_terrain_slabs_compat:canonical_parent/minecraft/cyan_stained_glass
minecraft:iron_block        -> %cnm_terrain_slabs_compat:canonical_parent/minecraft/iron_block
```

`cnm_terrain_slabs_compat` is BGE's current Fabric/data namespace, so this uses
an already stable public identity rather than introducing an alias. The suffix
is a serialization of the canonical parent's registered identifier for a tag
resource key only. Runtime membership must be selected from
`BgeMaterialBindings.Binding.canonicalMaterial()` and explicit derived-role
membership; it must never be inferred by splitting or matching the generated
block's name.

One tag per canonical parent is the proposed default because Complementary's
current material IDs distinguish parents that may look broadly similar (for
example, every stained-glass colour). It avoids a second BGE-maintained
"shader-material" grouping. A maintainer-approved coarser grouping could be
added later only if it retains that distinction where Complementary requires it.

## Exact r5.8.1 examples

The following are exact existing mappings in the supplied Complementary Unbound
r5.8.1 `shaders/block.properties`. The right-hand form is the proposed
Complementary-side result, shown conceptually; it is not a request to edit the
archived pack.

| Canonical parent | Existing r5.8.1 mapping | Proposed mapping with its exact parent tag |
| --- | --- | --- |
| Clear glass | `block.32008=glass` | `block.32008=glass %cnm_terrain_slabs_compat:canonical_parent/minecraft/glass` |
| Cyan stained glass | `block.31018=cyan_stained_glass` | `block.31018=cyan_stained_glass %cnm_terrain_slabs_compat:canonical_parent/minecraft/cyan_stained_glass` |
| Iron block | `block.10264=iron_block` | `block.10264=iron_block %cnm_terrain_slabs_compat:canonical_parent/minecraft/iron_block` |
| Gold block | `block.10312=gold_block` | `block.10312=gold_block %cnm_terrain_slabs_compat:canonical_parent/minecraft/gold_block` |
| Diamond block | `block.10316=diamond_block` | `block.10316=diamond_block %cnm_terrain_slabs_compat:canonical_parent/minecraft/diamond_block` |
| Glowstone | `block.10412=glowstone light` | `block.10412=glowstone light %cnm_terrain_slabs_compat:canonical_parent/minecraft/glowstone` |
| Sea lantern | `block.10448=sea_lantern` | `block.10448=sea_lantern %cnm_terrain_slabs_compat:canonical_parent/minecraft/sea_lantern` |

All of those canonical parents are present in BGE C79's current catalog,
including clear/stained glass, glowstone, sea lantern, iron, gold, diamond, and
emerald. The examples are intentionally parent-specific: cyan glass must remain
Complementary material `31018`, not be folded into a generic stained-glass tag.

Clear glass also has a render-layer entry in the exact pack:

```properties
layer.translucent=glass glass_pane beacon
```

If Complementary wants the pack to declare the translucent route as well as the
material ID, the corresponding conservative addition is:

```properties
layer.translucent=glass glass_pane beacon %cnm_terrain_slabs_compat:canonical_parent/minecraft/glass
```

That line should be accepted only alongside the existing BGE guarantees for the
derived block's render layer, texture inputs, and face-culling. A matching
material ID cannot correct a bad translucent pass or geometry/culling defect.

## Iris and version behavior

Iris documents `%namespace:tag` selectors in `block.properties` and the
`IRIS_TAG_SUPPORT` macro. Iris 1.8 and later report
`IRIS_TAG_SUPPORT >= 2`; at that level, an explicit block entry takes precedence
over a tag regardless of line order. The proposed additions should therefore be
guarded at that level, retaining the ordinary explicit mapping as the fallback.

Illustrative pattern for a material mapping:

```properties
# Existing fallback for loaders without the required Iris tag semantics.
block.32008=glass

# Iris 1.8+: repeat the complete mapping and add the parent tag.
#if IRIS_TAG_SUPPORT >= 2
block.32008=glass %cnm_terrain_slabs_compat:canonical_parent/minecraft/glass
#endif
```

The same pattern can guard the optional `layer.translucent` addition. It gives
the pack's explicit individual mappings authority over an accidental or future
overlapping BGE tag mapping, while the tag supplies only missing BGE-derived
members. With BGE absent, the tag is empty and all existing canonical mappings
remain unchanged. With an older Iris or another loader that does not satisfy the
guard, the existing explicit mapping remains the fallback. This introduces no
BGE dependency for Complementary users.

## Conservative initial scope

The initial request is only for canonical-parent inheritance where the
material-class relationship is straightforward and the physical BGE geometry
can be validated independently:

- clear glass and each distinct stained-glass parent, subject to the normal
  translucent-layer and face-culling checks;
- static reflective, smooth, or specular solid materials such as iron, gold,
  diamond, and emerald; and
- ordinary static emissive solids such as glowstone or sea lantern.

The tag contract must not be read as a claim that every shader category should
propagate. Leave these out of the first request unless Complementary explicitly
approves a demonstrated parent/topology pair:

- leaves, foliage, vines, crops, and other waving or plant-state-sensitive
  geometry;
- water, fluids, lily-pad-like and other fluid-adjacent classes;
- portals; and
- beacon, block-entity-specific, or other paths that depend on non-generic
  geometry, render passes, or metadata.

For example, a material ID that drives vertex waving or relies on full-block
semantics cannot safely be assigned merely because a partial block reuses the
same texture. BGE's typed topology and surface data can inform later testing,
but they are not an automatic admission rule for these categories.

## Why tags are preferable to BGE-ID enumeration

- Complementary does not need a giant and continually changing list of BGE
  slabs, stairs, walls, Vertical Slabs, Steps, Layers, Corners, and Quarter
  Columns.
- A future BGE geometry that is bound to the same canonical parent inherits the
  mapping automatically.
- Complementary continues to own its numerical IDs and material taxonomy.
- BGE never couples to Complementary's numerical IDs or shader internals.
- Neither project relies on heuristics derived from registry-name spelling.

## Illustrative minimal patch

This pseudodiff shows the intended narrow Complementary-side change for clear
glass. The actual placement and formatting should follow the current upstream
file.

```diff
 # Clear glass material ID
 block.32008=glass
+#if IRIS_TAG_SUPPORT >= 2
+block.32008=glass %cnm_terrain_slabs_compat:canonical_parent/minecraft/glass
+#endif

 # Existing translucent classification
 layer.translucent=glass glass_pane beacon
+#if IRIS_TAG_SUPPORT >= 2
+layer.translucent=glass glass_pane beacon %cnm_terrain_slabs_compat:canonical_parent/minecraft/glass
+#endif
```

Each approved canonical parent would receive the same small, adjacent change to
its existing `block.<id>` line. No generated BGE ID list, shader numerical-ID
table, Iris hook, or shader archive redistribution is involved.

## Questions for Complementary maintainers

1. Is this Iris `%tag`-based canonical-parent mechanism acceptable for
   Complementary material mappings?
2. Is `cnm_terrain_slabs_compat:canonical_parent/<namespace>/<path>` an
   acceptable stable tag namespace and naming convention, or would you prefer a
   different BGE-owned namespace/path?
3. Do you prefer one tag per exact canonical parent, or a different grouping
   that still preserves every material distinction Complementary needs?
4. Which material categories should explicitly not participate in an initial
   static-material rollout?
5. Should clear-glass tags be included in the relevant `layer.translucent`
   mapping, and are there any geometry/render-layer constraints BGE must meet
   before that is acceptable?
6. Are `IRIS_TAG_SUPPORT >= 2` and the explicit-entry fallback the compatibility
   requirements you want BGE/Complementary to follow, or is a different
   minimum-Iris policy required?

## References

- [Iris `block.properties` reference](https://shaders.properties/current/reference/miscellaneous/block_properties/)
- [Iris `IRIS_TAG_SUPPORT` reference](https://shaders.properties/current/reference/macros/iris_tag_support/)
- [Official Complementary contact page](https://www.complementary.dev/contact/)
- [Official Complementary development repository](https://github.com/ComplementaryDevelopment/ComplementaryReimagined)

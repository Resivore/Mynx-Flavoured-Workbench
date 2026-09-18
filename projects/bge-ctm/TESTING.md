# Testing

Canary 3 makes Continuity and the active resource packs the sole authority for material/rule
semantics. BGE supplies typed canonical identity. BGE × CTM only vetoes an otherwise-positive
ordinary or Standard Overlay result when a managed ordinary slab, Layer, or Vertical Slab does
not have compatible real surface contact. Controlled tests prove that control flow and geometry;
they do **not** prove Minecraft-client CTM output.

The exact Canary 2 runtime observations motivating this matrix are historical release evidence,
not Canary 3 results: ordinary slabs behaved inconsistently; full grass appeared to affect a dirt
slab despite the observed geometry; two grass slabs beside full podzol lacked the expected
behavior; and two dirt slabs beside grass did show CTM/overlay behavior. No aggregate Canary 2
PASS or FAIL is inferred.

## Controlled environment

Use the exact current Canary 3 JAR recorded in `WORKBENCH_STATUS.json` with:

- Minecraft Java 26.2 and Fabric Loader 0.19.3 or later;
- Continuity `3.0.1+26.2` exactly;
- a BGE version satisfying the declared compatible range, recording its exact filename and hash;
- Clutter No More `2.0.7+26.2` or a later compatible provider;
- the active Matcha Flavoured and Matcha overlay packs in their normal order;
- connected textures enabled and shaders off for the first pass.

Record the exact JAR hashes, Continuity configuration, enabled pack order, shader state, and
actual registry IDs before placing blocks. Stop as failed or inconclusive on a launch crash,
missing model/texture, resource-reload error, required-mixin rejection, or uncontrolled stack
drift. Do not substitute another Continuity version.

The profile-owned ordinary grass slab is
`more_slabs_stairs_and_walls:grass_block_slab`; the managed grass Layer is
`cnm_terrain_slabs_compat:minecraft/grass_block_layer`; and the save-compatible managed grass
Vertical Slab is `cnm_terrain_slabs_compat:grass_vertical_slab`. Confirm these in the running
stack. The separate `cnm_terrain_slabs_compat:grass_slab` is not BGE's exact effective slab
source and is deliberately outside this canary's ordinary-slab hook.

## How to establish an expectation

For every partial arrangement, first build the corresponding canonical full-block arrangement
with the same receiver/source direction, inspected face, exposed edge, pack order, and shader
state. Record what Continuity actually changes on each visible face. That observation is the
semantic expectation:

- if the canonical full blocks do not connect or overlay, the partial arrangement must not
  invent a relationship;
- if they do, an equivalent coplanar partial face may reproduce it, subject to Continuity's own
  unit-square, full-collision-source, selector, and occlusion requirements;
- a non-coplanar partial arrangement must be vetoed even when its canonical baseline applies.

Overlay relationships can be directional. Record which block is the rendered receiver, which is
the inducing neighbor, and then swap them. Never summarize a result merely as “the pair
connects”; different faces and directions can legitimately differ.

## First runtime pass: terrain regressions

Use adjacent horizontal positions, initially receiver at `(0,0,0)` and neighbor at `(1,0,0)`.
Inspect the receiver's `UP` face along its east edge plus every other visible face the pack
changes. Swap positions/directions where requested.

### 1. Canonical full-block baselines

With only canonical full blocks, record both directions for each pair:

1. `minecraft:grass_block` ↔ `minecraft:dirt`;
2. `minecraft:grass_block` ↔ `minecraft:podzol`;
3. `minecraft:dirt` ↔ `minecraft:podzol`.

For each direction, record the exact receiver, inducing neighbor, face/edge, and overlay or CTM
change. These six directional observations are the references for the partial rows below; do not
assume any unobserved grass/dirt/podzol relationship.

### 2. Grass slab rule selection and same-geometry contact

Place two profile-owned grass slabs with `type=top`, `waterlogged=false`, and the same snowy
condition. Inspect their coplanar `UP` faces at world `y=1`. Compare against two full grass
blocks. The slab appearance should now reach canonical `minecraft:grass_block`, and any native
same-material rule should be preserved on that face.

Repeat once with both slabs `type=bottom`, inspecting the corresponding coplanar surface rather
than assuming the top-face result applies everywhere.

### 3. Grass slab beside full podzol

Put a grass `type=top` slab beside full `minecraft:podzol`; inspect both blocks' relevant visible
faces and swap receiver/source direction. The partial result on the coplanar `UP` face should
mirror the recorded full grass↔podzol direction only if that canonical direction applied.

### 4. Grass slab beside full dirt

Repeat row 3 with full `minecraft:dirt`, again recording both directions and comparing each to
the corresponding full grass↔dirt baseline.

### 5. Dirt slab beside full grass

Use the profile-owned `more_slabs_stairs_and_walls:dirt_slab` with `type=top` beside full grass.
This reproduces the Canary 2 category that appeared to overlay unexpectedly. Record the exact
receiver/source direction and face. A result may remain only where its canonical full-block
direction applies and the participating surfaces are coplanar.

### 6. Two dirt slabs beside grass

Place two coplanar top dirt slabs along one edge of a full grass block and inspect each affected
face separately. Compare each observation to the equivalent all-full-block layout. Do not infer
that both slabs should behave alike unless their receiver/source direction and visible face are
the same.

### 7. Deliberately non-coplanar controls

Repeat a terrain relationship that was positive in the canonical baseline using:

- grass or dirt `type=top` slab beside `type=bottom` slab, inspecting `UP`;
- a `type=bottom` slab beside a full block while inspecting the mismatched upper surface;
- a bottom-anchored one-Layer carrier (`facing=up,layers=1`) beside a full block, inspecting
  `UP` at the recessed `y=.25` plane;
- the matching top-boundary control (`facing=down,layers=1`) beside a full block at `y=1`.

The first three non-coplanar cases must be vetoed. The top-boundary control may reproduce the
canonical relationship if Continuity's native overlay requirements also permit it.

### 8. Layers and Vertical Slabs

Repeat representative positive and negative canonical terrain relationships with:

- a grass top-boundary Layer (`facing=down,layers=1`) beside full dirt or podzol;
- two same-plane grass or dirt Layers;
- a recessed Layer negative;
- an aligned single grass Vertical Slab and a compatible full-block face;
- the same Vertical Slab oriented away from the evaluated boundary as a negative;
- one valid cross-geometry slab↔Layer or Layer↔Vertical pair.

For Vertical Slabs, record the exact `facing`, `double`, evaluated face, and occupied half. Native
Standard Overlay still rejects a non-unit-square receiver quad and a non-full-collision inducing
source before the C3 contact veto runs; record such a row as a native Continuity limitation, not
as a C3 geometry failure.

## Secondary regression pass

Only after the terrain relationships are understood:

1. Recheck full clear glass, two top clear-glass slabs, top slab↔full glass, top↔bottom negative,
   top-boundary Layer↔full, recessed Layer negative, aligned/misaligned Vertical Slabs, and one
   cross-geometry pair.
2. Repeat representative positive and negative rows with chiseled sandstone to separate solid
   CTM behavior from translucency.
3. Confirm distinct canonical states remain distinct wherever Continuity's active rule requires
   that distinction; C3's geometry resolver itself must not impose profile equality.
4. Repeat the clearest positive and negative glass cases with Complementary enabled and inspect
   seams, rims, alpha, culling, and shader artifacts. Translucent Glass defects outside general
   CTM semantic/contact behavior remain out of scope.

Steps, Corners, Quarter Columns, paths, roots, honey/slime inset families, unrelated cutout or
custom-model families, and unknown topology remain excluded. Do not use this matrix to broaden
them.

## Result record

Record one result per specific face with:

- exact block IDs and complete states;
- positions and receiver/inducing-source direction;
- inspected face and edge, expected world plane, and occupied surface region;
- exact canonical full-block baseline observation;
- expected partial behavior derived from that baseline;
- actual visual result and PASS/FAIL/INCONCLUSIVE classification;
- pack order, Continuity settings, shader state, and screenshots.

The shortest useful shader-off sequence is the six directional full-block baselines, two top
grass slabs, grass top slab↔full podzol, grass top slab↔full dirt, dirt top slab↔full grass, two
dirt top slabs beside grass, one top↔bottom negative, one recessed-Layer negative, and one
top-boundary-Layer positive/control. That directly revisits every supplied Canary 2 terrain
category before expanding to Vertical Slabs, glass, sandstone, or shaders.

Canary 3 remains `ACTIVE / RUNTIME_UNTESTED` until actual client observations are supplied.
Builds, JUnit, GameTests, controlled provider hashes, artifact inspection, and dedicated-server
execution are not Minecraft-client visual CTM evidence and cannot establish acceptance.

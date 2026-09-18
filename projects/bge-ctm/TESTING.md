# Testing

Canary 2 targets face-specific CTM contact for ordinary horizontal slabs, BGE Layers, and
single or double Vertical Slabs. The controlled GameTests prove typed material resolution,
canonical state projection, exact state/quad surface descriptors, and contact decisions. They
do **not** prove that a Minecraft client rendered a Continuity texture correctly. Every visual
row below remains untested until an owner records an actual client observation.

## Canary 2 contract

Rule selection and neighbor contact are separate:

- Exact BGE material/profile identity supplies the canonical parent used by rule selection.
- Ordinary slabs participate only when the block is both a `SlabBlock` and that profile's exact
  `effectiveSlabSource`; a broad slab hook is inert for every other slab.
- Geometry form (`top`/`bottom`/`double`, Layer facing/depth, Vertical facing/double, and
  waterlogging) never enters canonical material state.
- Continuity 3.0.1+26.2's exact connection seam then retains a positive connection only when
  the rendered source quad and the other state's surface have the same normal, world plane,
  canonical profile, and real boundary contact.
- Step, Corner, Quarter Column, unknown topology, missing quad context, and incompatible
  Continuity seams fail closed. Standard Overlay retains Continuity's separate inverse
  predicate semantics.

The inherited visual-profile allowlist remains the Canary 1 set: uniform,
top/side/bottom, pillar, leaves, glass edge, translucent uniform, and glazed-oriented profiles
with a known canonical state projection. Grass/path/root, honey/slime inset, cutout-uniform,
custom-model, and unmappable material states are not broadened by this canary. Native glazed
ordinary slabs remain fail-closed because their material-facing carrier differs from BGE's
explicit `pattern_facing` projector.

## Controlled environment

Use Minecraft Java 26.2 Fabric with all of the following exact inputs:

- BGE × CTM `0.2.0-canary2`;
- Continuity `3.0.1+26.2` (the contact hook is deliberately exact-version bounded);
- Block Geometry Extensions `4.2.14-bge.canary70.stone-native-slab+26.2`;
- Clutter No More `2.0.7+26.2` and the declared Fabric dependencies;
- the current Matcha Flavoured and `Overlays_Matcha_STRICT_darkoak-bugfix` packs in their normal
  order;
- connected textures enabled and shaders **off** for the first pass.

Before testing, record exact JAR hashes, Continuity configuration, enabled pack order, shader
state, and every actual block ID. Stop as failed or inconclusive on a crash, missing model or
texture, resource-reload error, mixin rejection, or unrelated stack drift. Do not substitute a
different Continuity version.

## Face-specific runtime matrix

Unless a row says otherwise, put the source at `(0,0,0)` and its neighbor at `(1,0,0)` (east).
The clear-glass ordinary slab is the profile-owned effective source
`more_slabs_stairs_and_walls:glass_slab`; the Layer is
`cnm_terrain_slabs_compat:minecraft/glass_layer`; the Vertical Slab is
`clutternomore:more_slabs_stairs_and_walls/vertical_glass_slab`. Confirm these actual IDs in the
running stack rather than relying on a similar registry name.

| # | Arrangement and exact state | Face to inspect | Expected Canary 2 result |
|---|---|---|---|
| 1 | Two `minecraft:glass` full blocks | Both `UP` faces across the east edge | Existing clear-glass CTM connects unchanged. |
| 2 | Two clear-glass slabs, both `type=top` | `UP` at world `y=1` | Connect; the two full 16×16 top quads are coplanar. |
| 3 | Clear-glass slab `type=top` beside full `minecraft:glass` | Both `UP` faces at world `y=1` | Connect despite different block classes/states. |
| 4 | Clear-glass slab `type=bottom` beside full glass | First `DOWN`, then `UP` | `DOWN` at `y=0` may connect; slab `UP` at `y=.5` must not connect to full `UP` at `y=1`. Record the two faces separately. |
| 5 | Clear-glass `type=top` slab beside `type=bottom` slab | `UP` | No connection: planes are `y=1` and `y=.5`. Do not generalize this result to every face/placement. |
| 6 | Glass Layer `facing=down,layers=1` beside full glass | `UP` | Connect at the top block boundary. |
| 7 | Clear-glass `type=top` slab beside glass Layer `facing=down,layers=1` | `UP` | Cross-geometry connection at `y=1`. |
| 8 | Glass Layer `facing=up,layers=1` beside full glass | `UP` | No connection: recessed Layer plane `y=.25` versus full plane `y=1`. |
| 9 | Two partial glass Layers `facing=down`, one `layers=1`, one `layers=2` | `UP` | Connect: unequal thickness is allowed because both top boundary surfaces are the same plane/region. |
| 10 | Two single glass Vertical Slabs across the east boundary: west source `facing=east`, east neighbor `facing=west` | `UP` | Connect; the two occupied top rectangles meet at the evaluated boundary. |
| 11 | Single Vertical `facing=east` beside full glass to its east | `UP` | Connect; the Vertical's boundary-aligned top rectangle reaches the full block. |
| 12 | Single Vertical `facing=west` beside full glass to its east | `UP` | No connection; the Vertical top rectangle is recessed from that east boundary. |
| 13 | At `(0,0,0)`, test in turn a glass Layer `facing=up,layers=4`, ordinary glass slab `type=double`, and glass Vertical Slab `facing=north,double=true`, each beside full glass at `(1,0,0)`; then test full Layer→double slab and double slab→double Vertical across the same east boundary | Both `UP` faces at world `y=1`, along their shared east edge | Every listed full-volume pair connects on the exact full top surface. |
| 14 | Clear-glass top slab beside white-stained-glass top slab | `UP` | No connection despite perfect geometry; canonical material profiles differ. |
| 15 | Repeat rows 1–9 with the profile-owned chiseled-sandstone slab/Layer and `minecraft:chiseled_sandstone` | Same named faces | Same contact outcomes, separating material/contact behavior from translucency. |
| 16 | Put the receiver at `(0,0,0)` and source at `(1,0,0)`: first full `minecraft:stone` receiving full `minecraft:bricks`; then a stone Layer `facing=up,layers=4`; then a stone Layer `facing=down,layers=1`. Keep full bricks as the source for those three. Separately replace the source with a brick Layer `facing=down,layers=1`, then with `facing=up,layers=4`. Finally use a single stone Vertical Slab `facing=east,double=false` as receiver with full bricks east. | Receiver `UP`, specifically its east edge; for the single Vertical receiver also record that its `UP` quad is only 8×16 | Full stone is the baseline. Full and top-anchored Layer receivers have unit-square `UP` quads and may inherit the overlay with a full-collision bricks source. The partial brick Layer must not induce it; the full four-Layer brick source may. The 8×16 Vertical receiver must retain Continuity's unit-square rejection. Record each face separately. |
| 17 | Repeat the clearest positive and negative glass rows with Complementary Unbound | Same exact faces | Positive contact remains connected, negative stays disconnected, and no new seam/rim/alpha/culling defect appears. Run only after the shader-off result is known. |

For split glass side faces, inspect the body and rim separately. Canary 2 captures each source
quad's exact in-plane bounds; one split quad is not allowed to borrow boundary contact from
another quad merely because their combined state cuboid reaches the neighbor.

## Short first pass

The shortest useful shader-off sequence is rows 1, 2, 3, the two face observations in row 4,
rows 5–8, rows 10–12, row 14, then the corresponding chiseled-sandstone positive and recessed
negative from row 15. This establishes base behavior, slab priority, partial-to-full,
cross-geometry, deliberate non-coplanarity, Vertical alignment, material separation, and a
non-translucent control before overlays or shaders add variables.

## Recording and acceptance boundary

Record one result per **specific face**, with exact block IDs/states, positions, expected plane
and region, actual CTM result, pack order, shader state, PASS/FAIL/INCONCLUSIVE, and screenshots.
Different faces on one block pair may legitimately differ.

The canary remains `ACTIVE / RUNTIME_UNTESTED` until actual client evidence is supplied. A clean
build, exact dependency hash, artifact inspection, server GameTest, fixture, or source audit must
not be relabeled as Minecraft-client visual CTM validation or project acceptance.

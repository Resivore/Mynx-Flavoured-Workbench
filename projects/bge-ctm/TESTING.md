# BGE × CTM Canary 12 manual verification

Current candidate: `bge-ctm-0.12.0-canary12.jar`

- SHA-256: `a2b44e09dd333e8bb6cca0ded9501878e37dafaf9c1557a28644c58c0acd3b75`
- Source checkpoint: `8f059adcd967696c2b2b58b3ac6c09acd6afa974`
- Use Minecraft Java 26.2, Fabric Loader 0.19.3+, exact Continuity `3.0.1+26.2`, and BGE `>=4.2.22-bge.canary78.stair-wall-surface-authority+26.2` (exact `BGE C78.jar`, SHA-256 `ed2f5592b699532174bc63672f69c3574f01eb752175126bbc702e9cc86a07f0`, is the controlled baseline).
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`.

Diagnostics are off by default. Enable bounded diagnostics only after a failed row with `-Dbge_ctm.diagnostics=true`; `-Dbge_ctm.diagnostics.disable=true` remains an explicit override. Unit tests and dedicated-server GameTests do not execute or visually validate the real client-only Continuity render path, depth buffer, or shader stack.

## Supplied Canary 11 observations

The owner supplied exactly these observations for retained Canary 11:

1. C11 substantially improved the intended footprint clipping.
2. In the tested Layer/slab terrain interaction, the clipped overlay now generally stops at the intended inducing geometry extent.
3. The clipped/projected overlay still visibly z-fights.
4. The z-fighting was also present in the preceding canary, so contribution-provenance duplication was not the only cause.
5. Some overlay/CTM visuals are stable while others z-fight.
6. Stairs currently do not exhibit the desired geometry-wide CTM behavior because prior BGE surface authority explicitly excluded Stair/Wall topologies. BGE C78 now removes that provider limitation.

These are individual C11 observations, not an aggregate PASS/FAIL. They do not establish any C78 + Canary 12 Minecraft-runtime result.

## Canary 12 architecture and render diagnosis

Every canonically bound BGE geometry whose authoritative C78 surface model is supported participates through the same resolver. Continuity owns semantic rule selection; BGE owns canonical material identity, canonical face meaning, physical surface patches, and the typed terrain-height relation. Geometry can veto an invalid physical relationship, but it cannot manufacture a relationship after a negative Continuity semantic result.

Exact Continuity `3.0.1+26.2` source and bytecode inspection found that native Standard Overlay emission sets square geometry, sprite UV, tint color, block atlas, animation state, chunk layer, item render type, ambient occlusion, and then emits. Neither that helper nor Fabric Renderer API `14.1.3` exposes an additional `RenderMaterial`, decal, blend, depth-bias, or polygon-offset property. Canary 11 already preserved the complete non-geometric attribute set. The remaining exact path difference was tessellation: a native full-face overlay and its receiver use identical triangle bounds, while a clipped overlay used smaller coplanar triangles over one larger receiver quad. That can produce depth interpolation differences even though both surfaces use the same mathematical plane.

Canary 12 delays managed Standard Overlay output until the receiver quad finishes processing, partitions the retained receiver and every represented logical overlay at the same authoritative edges, and emits exact shared cells. It adds no epsilon and does not move ordinary physical surfaces. Typed terrain overlays retain their established nominal presentation plane and share a common overlay-only grid there. Regular CTM does not use this overlay emission path; native full-face overlays also retain Continuity's original helper. Those path differences, plus texture alpha/content, explain why some C11 visuals could remain stable while clipped Standard Overlay cases fought.

Automated tests prove shared geometry cells, exact Continuity attribute calls, UV orientation/crop, logical-contribution isolation, duplicate elimination, and absence of a hidden material/depth API in the controlled versions. They cannot prove actual depth stability in Minecraft; the matrix below is required owner-runtime evidence.

## Shader off

Use a Continuity rule known to be semantically positive for the chosen canonical material. Record resource-pack order and exact physical states.

1. Clipped Layer → taller slab side: use a 4/16 Layer contribution on an 8/16 slab receiver side. Confirm the overlay ends at 4/16 and does not shimmer or z-fight.
2. Clipped slab → full side: test bottom and top slab contributors independently. Confirm lower/upper 8/16 crops, canonical UV alignment, and no z-fighting.
3. Full-face overlay control: repeat a full source/full receiver Standard Overlay case. Confirm native full-face appearance remains unchanged and stable.
4. Terrain inset control: test full grass 16/16 ↔ vanilla farmland/path 15/16 and grass bottom slab 8/16 ↔ derived terrain slab 7/16. Confirm physical contact uses the inset plane, presentation remains on 16/16 or 8/16 respectively, the footprint stays clipped, and no fighting appears.
5. Stair tread/riser CTM: test a coplanar tread positive, a noncoplanar tread negative, and a compatible vertical riser positive. Include both TOP and BOTTOM halves and more than one facing.
6. Inner/outer Stair: test STRAIGHT, INNER_LEFT, INNER_RIGHT, OUTER_LEFT, and OUTER_RIGHT across representative rotations. Confirm outside sides/undersides behave by exposed patches and removed internal member faces do not reappear.
7. Wall LOW/TALL arm: compare matching LOW and TALL arrangements. Confirm their top/side extents remain visibly distinct and are never treated as one full-block volume.
8. Wall/full and Wall/partial: test post-only or minimal post, post + one arm, multiple arms, and post + multiple arms across rotations. Include Wall ↔ full, Wall ↔ Stair/Step/slab/Layer, one coplanar positive, one plane/height mismatch negative, and one arm/post-clipped Standard Overlay.
9. Contribution provenance control: repeat differently sized simultaneous sides, side + corner, multiple sprites, a null-sprite gap, combined-side selection, and duplicate-region input. Each logical sprite must retain only its represented probe footprint; distinct sprites may overlap only where Continuity selected genuinely overlapping contributions.
10. Check every positive Standard Overlay row specifically for z-fighting while moving the camera slowly at near and far distances.

## Complementary enabled

With the supported Complementary stack enabled, repeat the minimal depth-sensitive set:

1. 4/16 Layer → 8/16 slab side.
2. Bottom slab → full side.
3. Full-face overlay control.
4. Full grass → farmland/path terrain inset control.
5. One Stair tread and one riser positive.
6. One LOW and one TALL Wall arm, including one Wall/partial interaction.

Confirm the shared-tessellation overlay remains stable and that the shader does not expose a material/layer regression. A shader-only failure and a shader-off failure are distinct observations; record them separately.

## Negative and semantic controls

1. Use a canonical material relationship that Continuity rejects; BGE contact must not create CTM or overlay.
2. Full top 16/16 beside bottom-slab top 8/16 must remain noncoplanar, while overlapping side patches may connect independently.
3. An arbitrary non-terrain 8/16 ↔ 7/16 pair must remain invalid; only surfaces explicitly marked `TERRAIN_HEIGHT_INSET` may use the one-sixteenth relation.
4. Repeat representative Layer, Vertical Slab, Step, Corner, Quarter Column, Stair, and Wall cases. Disjoint authoritative patches must remain independent, and canonical `TOP`/`SIDE`/`BOTTOM`, axis, glazed/directional, patterned, glass/translucent, and terrain semantics must follow BGE's `canonicalFace()` rather than the physical carrier class.
5. Confirm an unbound registry-name decoy and a risky excluded visual profile remain unchanged.

## Failure evidence

After a failure, retain the relevant bounded `APPEARANCE`, `RULE_SELECTION`, `REGULAR`, `OVERLAY`, and `OVERLAY_EMIT` lines with exact physical states, canonical states, native/canonical semantic decisions, contact decision, positions, rendered and canonical face, contribution reason, resource-pack order, and shader state. `RULE_SELECTION reason=NO_PROCESSOR`, `CANONICAL_SEMANTIC_REJECT`, `CARRIER_REJECTION_PROMOTED`, BGE geometry reasons, and `SHARED_TESSELLATION` distinguish the principal paths.

Record only observed rows against the exact Canary 12/BGE C78 identities. Manual testing does not accept the project automatically.

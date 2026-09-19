# BGE Glass Face Culling — Canary 3 runtime verification

Use the exact Canary 3 artifact recorded in `WORKBENCH_STATUS.json` with BGE C78
(`BGE C78.jar`, SHA-256
`ed2f5592b699532174bc63672f69c3574f01eb752175126bbc702e9cc86a07f0`). Keep the camera close
enough to inspect the shared plane from several angles. Do not infer a PASS from a clean launch or
from the controlled build/GameTests.

## Owner-supplied Canary 2 observations

The owner observed that exact Canary 2 opens the previously affected world without the Canary 1
null-`Direction` chunk-meshing crash. Partial BGE glass geometry ↔ partial BGE glass geometry also
appears correct. The same C2 observation found a visual failure for full glass ↔ partial BGE glass:
when the full block touches only part of the partial geometry, C2 removes the full block's entire
touching face. These are C2 observations only; they do not establish any C3 runtime result.

## First C3 gate — open and recheck the reported failure

Enable C3 in that same relevant client stack, open the world successfully, and allow nearby chunks
to build and rebuild meshes. Inspect the previously failing full-glass ↔ partial-BGE contacts. The
full face must no longer disappear wholesale: only the actual compatible coplanar overlap may be
absent. Recheck representative partial ↔ partial arrangements before proceeding to the full matrix.
Do not report this as passed until it has actually been observed.

For every case, check all four invariants:

1. only the real compatible coplanar overlap is absent;
2. every noncontacting part of the original glass surface remains visible;
3. surviving texture regions are cropped, not stretched, and retain tint/material appearance;
4. no duplicate coplanar surface, hole outside the overlap, or new translucency-sorting artifact is
   visible.

## Shader off

Use ordinary clear glass for compatible pairs unless a row says otherwise.

### A. Full glass ↔ full glass

| # | Arrangement | Required observation |
|---:|---|---|
| 1 | full glass ↔ full glass | The complete shared 16×16 interface is absent. |

### B. Full glass ↔ partial BGE glass geometry

| # | Arrangement | Required observation |
|---:|---|---|
| 2 | full glass ↔ bottom Slab | Only the lower 8/16 contact is absent; the upper half remains. |
| 3 | full glass ↔ top Slab | Only the upper 8/16 contact is absent; the lower half remains. |
| 4 | full glass ↔ Vertical Slab | Only the actual half-depth/width contact is absent. |
| 5 | full glass ↔ Step | Only the contacting quarter/half patch is absent. |
| 6 | full glass ↔ one-Layer (4/16) | Exactly 4/16 is absent; the other 12/16 remains. |
| 7 | full glass ↔ Corner | Each contacting patch follows the Corner footprint; exposed arms remain visible. |
| 8 | full glass ↔ Quarter Column | Only the contacting quarter footprint is absent. |
| 9 | full glass ↔ straight Stair | Only the resolved boundary tread/riser footprint is absent. |
| 10 | full glass ↔ both inner and both outer Stair variants | Each resolved shape and rotation clips only its own boundary patches; repeat TOP and BOTTOM. |
| 11 | full glass ↔ Wall LOW arm | The six-wide arm contact ends at 14/16 height. |
| 12 | full glass ↔ Wall TALL arm | The same arm reaches 16/16 height; it must visibly differ from LOW. |

### C. Partial BGE glass geometry ↔ partial BGE glass geometry

| # | Arrangement | Required observation |
|---:|---|---|
| 13 | partial glass ↔ partial glass with partial intersection | Only their exact intersection is absent on both surfaces. |
| 14 | clear glass ↔ stained glass (or two distinct stained families) | The interface remains; canonical material compatibility is not broadened. |
| 15 | adjacent but noncontacting partial geometries | Both exposed surfaces remain; block-cell adjacency alone removes nothing. |

Also sample a Wall post from above, a multi-arm Wall, rotated Stairs/Walls, a three-Layer block, and
two partial geometries whose overlap consists of more than one rectangle. Confirm no provider-removed
internal Stair or Wall member face appears.

## Supported client stack composition

Enable the normal supported client stack, including Continuity when it is normally active. Repeat
rows 1, 2, 6, 9, 11, 13, and 14. Confirm that connected or otherwise transformed face appearance is
unchanged on every surviving fragment: Canary 3 owns only geometry subtraction and must not change
which CTM rule connects, choose a sprite, or create an overlay.

## Complementary enabled

Enable the supported Complementary shader stack and repeat rows 1, 2, 6, 9, 11, and 13. Confirm
that physically removed regions remain absent and surviving fragments render normally. Record any
new sorting, tint, lighting, or seam artifact separately; do not treat a shader workaround as a
substitute for removed geometry.

## Reporting

Report the exact Canary filename and SHA-256, BGE filename and SHA-256, client stack, shader state,
and observations for each tested row. A partial report binds only to the rows actually observed.
No lifecycle transition is implied by testing; acceptance remains an explicit owner decision.

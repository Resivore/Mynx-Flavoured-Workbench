# BGE × CTM Canary 8 manual verification

Current candidate: `bge-ctm-0.8.0-canary8.jar`

- SHA-256: `cb3124ba2324898c17c6ce9774c88dee83633e219f45d6850815dd3f70c872ae`
- Source checkpoint: `ce936549fc8e0a1e0f16146a68988bc0f56d54b8`
- Use Minecraft Java 26.2, Fabric Loader 0.19.3+, exact Continuity `3.0.1+26.2`, and BGE `>=4.2.19-bge.canary75.surface-semantics+26.2` (exact C75 is the controlled baseline).
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`.

Diagnostics are off by default. Enable bounded diagnostics only after a failed row with `-Dbge_ctm.diagnostics=true`; `-Dbge_ctm.diagnostics.disable=true` remains an explicit override. The 21/21 headless GameTests and 12/12 unit tests do not execute or visually validate the real client-only Continuity processor/model path.

## Supplied Canary 7 observations

The owner reported only these C7 runtime observations: placement lag is resolved; grass/Farmland Slab overlay still does not work; and CTM/overlay behavior was seen on slab top faces but not the corresponding side faces. These are not an aggregate C7 result and do not transfer to C8.

## A. Placement responsiveness

With diagnostics off, place ordinary and BGE-derived blocks repeatedly. Confirm C8 preserves C7's observed responsive placement. If responsiveness regresses, note the exact block/state and whether removing BGE × CTM changes it; do not infer diagnostics as the cause without evidence.

## B. Six-face ordinary CTM identities

Use one Continuity rule known to connect the chosen canonical material faces. Evaluate each face independently (UP, DOWN, NORTH, SOUTH, EAST, WEST):

1. Full beside full on the same rendered plane: positive baseline.
2. Full top at 16/16 beside bottom slab top at 8/16: negative.
3. Full side beside the corresponding bottom-slab side: positive where the upstream Continuity decision is positive.
4. Top slab top at 16/16 beside full top at 16/16: positive.
5. Bottom slab top at 8/16 beside top/full top at 16/16: negative.
6. Repeat representative plane/contact positives and negatives with one Layer and one Vertical Slab orientation.

For a positive result, confirm the texture orientation corresponds to the same canonical material face rather than merely appearing somewhere on the block.

## C. Grass × farmland typed terrain inset

First establish the actual positive full Grass Block ↔ full Farmland Continuity direction. C8 may retain that positive semantic result but may never manufacture one.

1. Test full grass 16/16 ↔ full farmland 15/16.
2. Test grass bottom slab 8/16 ↔ Farmland Slab bottom 7/16.
3. Test grass top slab 16/16 ↔ Farmland Slab top 15/16.
4. Test full grass ↔ double Farmland Slab 15/16.
5. For each applicable pair, inspect both the top and corresponding side faces. The side must present the canonical side-face semantics while retaining its real partial footprint.
6. Add one ordinary non-terrain pair offset by 1/16; it must remain negative.

## D. Standard Overlay and receiver surface

Repeat the positive relationships above under Standard Overlay. Overlay uses the same compatible surface-plane model as ordinary CTM: mere cuboid adjacency at a common block boundary must not connect substantially different top planes. Every emitted overlay must remain on the receiver's actual patch (for example, Farmland Slab at 7/16 or 15/16), never a full-block plane or the inducer's plane. A negative upstream Continuity result must remain negative.

## E. Compound and fail-closed controls

1. Exercise representative Step, Corner, and Quarter Column states, including multi-patch faces. Record each face/patch result separately; one positive patch must not flatten or connect a disjoint patch.
2. Native Stairs and Walls intentionally remain fail-closed in this canary because their rendered inner/outer and post/arm topology is contextual. Record them only as limitation controls.
3. Confirm an unbound registry-name decoy and risky excluded visual profile remain unchanged.

After any failure, retain the relevant bounded `APPEARANCE`, `RULE_SELECTION`, `REGULAR`, `OVERLAY`, and `OVERLAY_EMIT` lines together with exact physical states, positions, evaluated face, resource-pack order, and shader state. Record only observed rows and the exact C8/BGE C75 hashes; manual testing does not accept the project automatically.

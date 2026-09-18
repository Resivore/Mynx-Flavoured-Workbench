# BGE × CTM Canary 9 manual verification

Current candidate: `bge-ctm-0.9.0-canary9.jar`

- SHA-256: `85b8fad6b75b1fe6f95d261514f6b397f503ba27d5a95da9b81ddd7fed05ddeb`
- Source checkpoint: `112a90f5b5f07b6bab26d22a7182d78433453ad1`
- Use Minecraft Java 26.2, Fabric Loader 0.19.3+, exact Continuity `3.0.1+26.2`, and BGE `>=4.2.19-bge.canary75.surface-semantics+26.2` (exact C75 is the controlled baseline).
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`.

Diagnostics are off by default. Enable bounded diagnostics only after a failed row with `-Dbge_ctm.diagnostics=true`; `-Dbge_ctm.diagnostics.disable=true` remains an explicit override. The 21/21 headless GameTests and 14/14 unit tests do not execute or visually validate the real client-only Continuity processor/model path.

## Supplied Canary 8 observations

The owner reported only these observations from the exact current C8/BGE/Continuity runtime stack:

- Full-height grass overlay over vanilla farmland behaves correctly: farmland is physically 15/16 high, while the grass overlay remains suspended on the normal 16/16 visual plane.
- The equivalent bottom-slab relationship is wrong: grass beside Farmland Slab connects/overlays, but the grass overlay is emitted directly on the Farmland Slab's physical 7/16 top instead of remaining on the corresponding normal slab plane at 8/16.
- Ordinary slab side faces still do not receive the expected CTM/overlay relationship between each other even though their corresponding side surfaces are coplanar.

These observations are exact C8 evidence only. They are not an aggregate C8 result and do not transfer to C9.

## C9 invariant

Continuity and the active resource pack still own rule selection, pack priority, face and state predicates, `connect=...`, sprites, tiles, tint, layer and AO. C9 supplies BGE's canonical material state to Continuity's connection predicate, then requires BGE's authoritative surface patches to prove physical contact. For a typed `TERRAIN_HEIGHT_INSET`, contact uses the physical inset while overlay presentation uses the corresponding nominal exact plane and keeps the receiver patch's original in-plane crop.

## A. Placement responsiveness

With diagnostics off, place ordinary and BGE-derived blocks repeatedly. Confirm C9 preserves C8's observed responsive placement. If responsiveness regresses, record the exact block/state, resource-pack stack, and whether removing BGE × CTM changes it.

## B. Regular CTM and slab sides

Use a Continuity rule known to be semantically positive for the chosen canonical material. Evaluate each rendered face orientation independently: UP, DOWN, NORTH, SOUTH, EAST, and WEST.

1. Two bottom slabs with corresponding coplanar side patches must connect.
2. A bottom-slab side beside an overlapping full-block side must connect.
3. A bottom-slab top at 8/16 beside a full-block top at 16/16 must not connect.
4. A top-slab top at 16/16 beside a full-block top at 16/16 may connect.
5. Repeat one positive with different BGE carrier forms of the same canonical material; a derived carrier identity must not force a negative result.
6. Repeat one geometrically valid pair whose canonical material rule is negative; geometry must not manufacture a relationship.

Confirm that TOP/SIDE/BOTTOM material meaning follows the canonical face reported by BGE rather than geometry form or carrier identity.

## C. Standard Overlay and terrain height

First establish the active pack's real positive Grass Block/Farmland relationship; C9 may preserve it but may not create it.

1. Full grass 16/16 beside vanilla farmland 15/16: compatibility may be positive, and the grass overlay must remain at the nominal 16/16 presentation plane.
2. Grass bottom slab 8/16 beside Farmland Slab bottom 7/16: compatibility may be positive, and the overlay must present at 8/16.
3. Grass top slab 16/16 beside Farmland Slab top 15/16: compatibility may be positive, and the overlay must present at 16/16.
4. For each typed terrain case, confirm the emitted overlay keeps the receiver patch's real in-plane footprint and UV crop; it must not expand to a full-block footprint.
5. Test an unrelated exact 8/16 surface beside an ordinary exact 7/16 surface; it must remain non-coplanar.
6. Exercise one semantically negative overlay relationship on valid physical contact; it must remain negative.

## D. General BGE surface coverage

Repeat representative positive and negative patch relationships for a Layer, Vertical Slab, Step, Corner, and Quarter Column. Treat each compound patch independently and confirm one positive patch does not flatten, stretch, or connect a disjoint patch. Native contextual Stairs and Walls remain intentional fail-closed controls because BGE C75 does not expose their contextual topology.

Also confirm an unbound registry-name decoy and a risky excluded visual profile remain unchanged.

## E. Failure evidence

After any failure, retain the relevant bounded `APPEARANCE`, `RULE_SELECTION`, `REGULAR`, `OVERLAY`, and `OVERLAY_EMIT` lines with exact physical states, canonical states, native/canonical semantic decisions, contact decision, positions, rendered and canonical face, resource-pack order, and shader state. `RULE_SELECTION reason=NO_PROCESSOR`, `CANONICAL_SEMANTIC_REJECT`, `CARRIER_REJECTION_PROMOTED`, and BGE geometry reasons distinguish the required failure classes.

Record only observed rows and the exact C9/BGE C75 hashes. Manual testing does not accept the project automatically.

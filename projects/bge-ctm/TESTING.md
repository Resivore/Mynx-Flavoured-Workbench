# BGE × CTM Canary 11 manual verification

Current candidate: `bge-ctm-0.11.0-canary11.jar`

- SHA-256: `e6de6b74f8d7c6c865521c0470c006a125227998a78dc4cda866ebccc0164835`
- Source checkpoint: `50a912e72af457f94cf7ab4034010d8c33d50315`
- Use Minecraft Java 26.2, Fabric Loader 0.19.3+, exact Continuity `3.0.1+26.2`, and BGE `>=4.2.19-bge.canary75.surface-semantics+26.2` (exact C75 is the controlled baseline).
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`.

Diagnostics are off by default. Enable bounded diagnostics only after a failed row with `-Dbge_ctm.diagnostics=true`; `-Dbge_ctm.diagnostics.disable=true` remains an explicit override. The controlled headless GameTests and unit tests do not execute or visually validate the real client-only Continuity processor/model path.

## Supplied Canary 10 observations

The owner supplied only these exact Minecraft-runtime observations for retained Canary 10:

- The intended short-source footprint clipping is not correct.
- In the tested Layer → neighboring terrain-face case, the overlay still extends beyond the inducing geometry's intended height.
- The same area also shows visible coplanar fighting/artifacting consistent with multiple overlay quads over the same region.

No aggregate Canary 10 PASS/FAIL is inferred, and the artifact is not classified as a geometry-model failure. Source and bytecode inspection established instead that C10 appended every positive side/corner probe footprint to one receiver-quad collection, while Continuity assembled one or more logical sprites afterward and emitted every sprite against that same aggregate collection. These observations bind only to exact retained Canary 10 and do not transfer to Canary 11.

## Supplied Canary 9 observations

The owner reported only that Layer and slab interactions behaved as expected in the cases they tested. No exact state/face matrix was supplied, so no individual row or aggregate Canary 9 PASS/FAIL is inferred. These observations bind to exact retained Canary 9 only and do not transfer to Canary 10 or Canary 11.

## Supplied Canary 8 observations

The owner reported only these observations from the exact current C8/BGE/Continuity runtime stack:

- Full-height grass overlay over vanilla farmland behaves correctly: farmland is physically 15/16 high, while the grass overlay remains suspended on the normal 16/16 visual plane.
- The equivalent bottom-slab relationship is wrong: grass beside Farmland Slab connects/overlays, but the grass overlay is emitted directly on the Farmland Slab's physical 7/16 top instead of remaining on the corresponding normal slab plane at 8/16.
- Ordinary slab side faces still do not receive the expected CTM/overlay relationship between each other even though their corresponding side surfaces are coplanar.

These observations are exact C8 evidence only. They are not an aggregate C8 result and do not transfer to later canaries.

## C11 invariant

Continuity and the active resource pack still own rule selection, pack priority, face and state predicates, `connect=...`, sprites, tiles, tint, layer and AO. C11 preserves every successful side or diagonal corner probe under its exact inducing position, then attaches only the represented probe set to each non-null entry that Continuity adds to its `SpriteCollector`. The final emission loop consumes one aligned contribution per sprite. Combined sprites receive the union of only the side probes represented by their Continuity sprite index; corner sprites receive only their successful diagonal probe. Each logical contribution is independently partitioned into non-overlapping BGE receiver-local regions, with cropped canonical UVs and Continuity-equivalent tint/layer/AO/item-render state. For typed `TERRAIN_HEIGHT_INSET`, contact remains on the physical inset and only that contribution's footprint moves to the nominal presentation plane.

## A. Placement responsiveness

With diagnostics off, place ordinary and BGE-derived blocks repeatedly. Confirm C11 preserves the current responsive placement. If responsiveness regresses, record the exact block/state, resource-pack stack, and whether removing BGE × CTM changes it.

## B. Regular CTM and slab sides

Use a Continuity rule known to be semantically positive for the chosen canonical material. Evaluate each rendered face orientation independently: UP, DOWN, NORTH, SOUTH, EAST, and WEST.

1. Two bottom slabs with corresponding coplanar side patches must connect.
2. A bottom-slab side beside an overlapping full-block side must connect.
3. A bottom-slab top at 8/16 beside a full-block top at 16/16 must not connect.
4. A top-slab top at 16/16 beside a full-block top at 16/16 may connect.
5. Repeat one positive with different BGE carrier forms of the same canonical material; a derived carrier identity must not force a negative result.
6. Repeat one geometrically valid pair whose canonical material rule is negative; geometry must not manufacture a relationship.

Confirm that TOP/SIDE/BOTTOM material meaning follows the canonical face reported by BGE rather than geometry form or carrier identity.

## C. Per-contribution Standard Overlay footprint

First establish that the active pack's canonical Standard Overlay relationship is positive; C11 may preserve it but may not create it.

1. A 4/16 UP-facing Layer inducing onto an 8/16 neighboring slab side must cover only the corresponding 4/16 receiver band.
2. Arrange two successful side probes with different heights on one receiver. Each selected logical sprite must retain only its own height; the short and tall contributions must not expand one another.
3. Arrange a successful side plus a successful diagonal corner. The corner sprite must be clipped only by the diagonal probe that selected it, while the side sprite retains only its side probe.
4. Exercise a Continuity decision that places multiple non-null sprites in one collector. Each emitted sprite must retain a distinct footprint matching its represented side or corner probe set.
5. Repeat overlapping or duplicate patch inputs for one logical contribution. C11 must emit each exact logical contribution/region once; no duplicate projected quads, coplanar shimmer, or depth fighting may be introduced.
6. A bottom slab inducing onto a full-block side must cover only the lower 8/16; a top slab must cover only the upper 8/16. A full-height source inducing onto a shorter receiver must stop at the receiver's real surface extent.
7. Exercise Quarter Column, Corner, and Step contributors where the active pack is semantically positive. Each overlay must stay inside the reflected/intersected authoritative patch or patches, including a quarter-width and a diagonal corner case.
8. Repeat an ordinary full-face source/full-face receiver case. It must remain the original full receiver overlay with the canonical uncropped UV domain and native-equivalent Continuity render behavior.

## D. Terrain presentation and negative controls

1. Full grass 16/16 beside vanilla farmland 15/16: compatibility may be positive, and the grass overlay must remain at the nominal 16/16 presentation plane while respecting the inducing footprint.
2. Grass bottom slab 8/16 beside Farmland Slab bottom 7/16: compatibility may be positive, and the overlay must present at 8/16 with its contributor crop.
3. Grass top slab 16/16 beside Farmland Slab top 15/16: compatibility may be positive, and the overlay must present at 16/16 with its contributor crop.
4. Test an unrelated exact 8/16 surface beside an ordinary exact 7/16 surface; it must remain non-coplanar.
5. Exercise one semantically negative overlay relationship on valid physical contact; it must remain negative and emit no footprint.

## E. General BGE surface coverage

Repeat representative positive and negative patch relationships for a Layer, Vertical Slab, Step, Corner, and Quarter Column. Treat each compound patch independently and confirm one positive patch does not flatten, stretch, or connect a disjoint patch. Native contextual Stairs and Walls remain intentional fail-closed controls because BGE C75 does not expose their contextual topology.

Also confirm an unbound registry-name decoy and a risky excluded visual profile remain unchanged.

## F. Failure evidence

After any failure, retain the relevant bounded `APPEARANCE`, `RULE_SELECTION`, `REGULAR`, `OVERLAY`, and `OVERLAY_EMIT` lines with exact physical states, canonical states, native/canonical semantic decisions, contact decision, positions, rendered and canonical face, resource-pack order, and shader state. `RULE_SELECTION reason=NO_PROCESSOR`, `CANONICAL_SEMANTIC_REJECT`, `CARRIER_REJECTION_PROMOTED`, and BGE geometry reasons distinguish the required failure classes.

Record only observed rows and the exact C11/BGE C75 hashes. Manual testing does not accept the project automatically.

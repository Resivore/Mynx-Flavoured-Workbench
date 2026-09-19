# Testing — BGE × Complementary

## Current gate

**PLANNED — NO RUNTIME CANDIDATE**

This rescope/audit created no code, artifact, deployment, Minecraft launch, or
runtime observation. The supplied Complementary archive/configuration was read
only. Do not infer shader parity from the canonical binding, generated tags,
static source inspection, a resource reload, a screenshot outside Minecraft, a
build, or a GameTest.

The exact r5.8.1 archive has no BGE tag mapping, so current BGE-derived blocks
are not expected to acquire a canonical Complementary material ID merely because
BGE × CTM projects a canonical Fabric appearance for Continuity.

## Prerequisites for a future candidate

Record all of the following before any result is attached to this UUID:

- exact BGE candidate filename, SHA-256, embedded version, and source commit;
- exact BGE × CTM, Iris, Fabric API, Sodium, Clutter No More, and relevant
  provider identities;
- Complementary Unbound r5.8.1 SHA-256
  bb89b1fc54687d4147a837fb2e3c3f7261a13bee51819761e9b6a91cb7915965;
- saved configuration SHA-256
  931dd0fc5aa4cea6f0f8fb1fa8b6c056492930258bcb52a586f5439a622a130b
  and confirmation that FANCY_GLASS=true;
- enabled resource packs, exact low-to-high pack order, shader settings, world,
  coordinates, time/weather, screenshots, and relevant client log lines;
- the approved Complementary/Iris tag or other documented supported integration
  identity. A local modified shader ZIP is not a valid candidate.

Run every row first with shaders disabled. That establishes BGE rendering,
physical faces, and BGE × CTM behavior separately from shader material parity.
Then repeat only the eligible rows with the exact Complementary configuration.

## Runtime matrix after a supported mapping exists

| Row | Arrangement | Observe | Ownership if it fails |
| --- | --- | --- | --- |
| Clear glass | Canonical clear glass against every available BGE clear-glass role: slab, stair, wall, Vertical Slab, Step, Layer, Corner, and Quarter Column | Same Fancy Glass opacity floor, smoothness/highlight/reflection behavior where the face is visible; no opaque fallback, lost exterior face, or invalid full-cube treatment | Missing parent material class: this project / approved pack contract. Layer or face/culling defect: BGE or BGE Glass Face Culling. |
| Stained glass | Canonical white and one non-white stained parent against matching BGE forms; place unlike colors together as a control | Each derived form follows its own parent color class; no accidental clear-glass or other-color class; unlike colors do not become one material family | This project / pack mapping. |
| Reflective/smooth solid | One mapped metal or gem parent, such as iron, gold, diamond, or emerald, beside a representative derived form | Parent and derived use equivalent material response while retaining correct canonical texture/specular inputs and physical shape | This project / pack mapping, or BGE model texture generation. |
| Emissive material | One mapped glowstone, sea-lantern, shroomlight, or froglight parent where BGE exposes an eligible form | Parent-class glow/emission behavior remains visible without a fake light value or broken geometry | This project / pack mapping; BGE if model/layer/texture input differs. |
| Clear-glass contacts | Same-material base↔derived, derived↔derived, and deliberately partial/misaligned contacts viewed from both sides | Only physically shared area is suppressed; exterior partial faces remain. Repeat with Translucent Glass active as the clear-glass texture/CTM control | BGE / BGE Glass Face Culling for physical faces; BGE × CTM for CTM contact/connection. |
| Continuity control | One known BGE × CTM canonical-appearance case and one unlike-parent negative connection | CTM uses the intended canonical material only where BGE × CTM's contact policy permits it; do not diagnose this as an Iris-ID fix | BGE × CTM. |
| Foliage/waving probe | Only after a pack mapping explicitly admits a leaf/foliage/vine class; test an approved geometry role beside the canonical parent through wind, rain, snow, and multiple views | No detached, overextended, clipped, or wrongly classified waving. A failure keeps that material-class/topology pair withheld; it must not be fixed by suppressing other parent parity. | This project / pack mapping policy; BGE only if the physical model input is wrong. |
| Reload stability | For every passing eligible representative: resource reload, world leave/rejoin, then full client restart | Mapping, pass, texture inputs, and geometry remain stable; preserve logs for any load/reload error | Route by the observed failed layer; do not generalize from a successful reload. |

Record each row independently as pass, fail, or inconclusive. An observation
binds only to the exact release/hash and exact loaded stack. A runtime pass for
clear glass does not establish metal, emissive, foliage, water-like, or other
material classes.

## Geometry-sensitive exclusions

Do not run a category as an expected parity success merely because it has a
canonical parent mapping. Keep foliage, vines, crops, upper/lower plant forms,
lily-pad/water-like forms, fluids, portals, beacons, and block-entity-specific
paths withheld until the integration contract and targeted topology policy
explicitly admit them. In particular, shader vertex waving and render-layer
semantics can depend on physical geometry rather than material identity alone.

Stop on a crash, missing mapping, wrong render pass, visible internal face,
depth/halo artifact, or movement/clipping defect. Preserve the exact
arrangement, F3/debug state if useful, screenshots from both sides, and logs;
do not change the protected Minecraft profiles or infer a general result from a
single placement.

## Acceptance boundary

This project owns Complementary/Iris shader material parity only. It does not
take over Continuity connectivity, BGE geometry/culling, or shader-pack
licensing. Runtime testing remains owner-directed evidence and does not change
the lifecycle from PLANNED or ACCEPTED without an explicit owner decision.

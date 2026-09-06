# Testing

Current `xaero-emf-entity-icon-compat-0.1.0-canary8.jar` is **ACTIVE /
STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED**. It is unaccepted and has no
rollback. Identity: 42,659 bytes, SHA-256
`337adff555afae9fbcedf1c51320595654bfe4f1ab120f8c0dfb7d03bad3aa7c`;
source checkpoint is `8c45f2f9ccdcf12f4403ba3380a32f81f975e077`.

Two independent Java 25 / Gradle 9.5.1 / Loom 1.17.19 clean offline `check
jar` builds produced byte-identical C8 files. All 47 tests in nine suites
passed. The client-only allowlist passed; the 28-entry archive has no duplicate
or nested-JAR entries, and every ZIP entry was read successfully for the CRC
scan. No Minecraft game was launched. These are static results, not runtime
observations.

## C7 external runtime evidence reconciled for C8

The authorized Player Instance contains exact C7 SHA-256
`b70d04024ee22440d573c8b6682c8097e2eda34f5f6e15b171c401db09943ee`.
The user reports that C7 works for axolotl, sniffer, iron golem, wolf, bat,
parrot, witch, ravager, farmer villager, and cleric villager. Butcher villager,
mason villager, frog, allay, and vex instead show Xaero's yellow generic entity
square plus name.

Read-only C7 diagnostics show butcher/mason take Xaero's failed creation route
under their own profession variants. Their effective `Ribbit Villagers v1.zip`
models use transformed empty hat containers with nested cube owners; farmer and
cleric use direct cube owners and succeeded. C8 retains the full named
head-local chain, renders copied EMF traversal containers without copying their
`skipDraw` suppression, and retains actual `visible` state. It never copies
body or arm branches.

Frog, allay, and vex share a second confirmed cause. C7 reaches creator under
each normal texture variant and performs its bounded retry, then fails at
`MISSING_RETAINED_VANILLA_GEOMETRY` before adapter drawing. EMF has cleared the
mapped vanilla canonical-head cubes for these non-attached JEM models. C8 uses
only the already uniquely traced semantic `head2` and its direct cube as the
reference frame in that exact absent-retained-geometry case. It does not add a
retry or broaden model search.

## C8 manual runtime matrix

Under separately authorized Test Instance Manager ownership only, install the
exact C8 hash above, clear/reload relevant Xaero resources, and record effective
resource-pack order and model identities. For every owned target, **PASS** is a
correctly textured and framed minimap head icon. Xaero's yellow generic entity
square plus name is **FAIL**. Do not promote from this procedure without actual
observations.

| Cases | Required observation |
| --- | --- |
| Preserve: axolotl; sniffer; iron golem; wolf; bat; parrot; witch; ravager; farmer villager; cleric villager | Still display correctly framed head icons without body, limb, wing, held-item, or unwanted headwear contamination. |
| Fix: butcher villager; mason villager | Display correct head icons and their complete visible transformed nested headwear chain; no body/arms. |
| Fix: frog; allay; vex | Replace yellow fallback with correctly textured/framed head icons. Confirm the existing one-retry-per-key lifecycle remains bounded. |
| Controls: normal villager; sheep; horse; sea turtle; creeper; vanilla non-EMF entity | Preserve ordinary Xaero behavior. |
| World rendering and reload | Fresh Animations/EMF world rendering remains intact. Reload clears relevant cache state with no persistent retry loop. |

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for yellow fallback,
blank/invisible/wrongly framed or textured icons, included body/limb/wing/held
items, ordinary-icon regressions, EMF/Fresh Animations world-rendering
regressions, Mixin errors, or cache behavior outside the bounded lifecycle.

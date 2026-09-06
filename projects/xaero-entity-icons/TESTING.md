# Testing

Current `xaero-emf-entity-icon-compat-0.1.0-canary6.jar` is **ACTIVE /
STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED**. It is unaccepted and has no
rollback. Identity: 40,659 bytes, SHA-256
`f4e989342fd767b219c1b1c617845562d5db0966fe0e508db62f5b4cd06a89f3`; source
checkpoint is `03c88d9d57981dbf99e46e080fb495009e31df76`.

Two independent Java 25 / Gradle 9.5.1 / Loom 1.17.19 clean offline `check jar`
builds produced byte-identical C6 files. All 42 tests in eight suites passed,
as did the client-only allowlist, CRC and duplicate-entry scan (27 entries),
and the combined 80-mod production Knot/Mixin application with C6, Xaero 26.4.2,
EMF 3.2.6, ETF 7.1.1, Ribbits C12, GeckoLib 5.5.4 and the recorded companion
cohort. No Minecraft game was launched. These are static/classloading results,
not runtime observations.

## C5 external runtime evidence retained for C6

The authorized Player Instance contains the exact C5 artifact (SHA-256
`75dfaec7b34a5284ab9cc626fd7ae403c4c9f103dd12a2b9941de33f79769087`). The
user reports these C5 visible results: axolotl and sniffer remained correct;
iron golem and farmer were fixed; wolf, bat, parrot, frog, ravager, witch,
butcher and cleric were invisible; mason was invisible and is specifically a
regression from the known working C4 hatted-villager control; and allay/vex kept
the ordinary Xaero entity-icon/name-tag fallback instead of the intended head
icon. These remain user-reported external evidence, not a C6 result.

Read-only archived logs preserve C4 bounded diagnostics but no C5
`RESOLVED_CANONICAL_GEOMETRY` draw/result sequence. C6 therefore does not claim
unobserved C5 per-entity allocation, vertex, cache or final-display stages. Its
static diagnosis is bounded: C5 copied EMF's traversal-only `skipDraw` flag to
the detached plain head, which can make Xaero's accepted adapter submit no
canonical-head geometry; farmer's direct hat differs from butcher/mason's
transformed nested hat-cube containers, while cleric again owns direct hat
geometry. Separately, C5 recognized `EMFModelPartVanilla` in the resolver but
only recorded the wrapper root before cache lookup, allowing a prior FAILED
entry to block allay/vex from retrying. C6 forces only the traced detached head
to render, preserves named descendant transforms/visibility, and retries one
FAILED cache value only after the current LivingEntity renderer is structurally
verified as either supported EMF root family.

## C6 manual runtime matrix

Under separately authorized Test Instance Manager ownership only, install the
exact C6 hash above, clear/reload the relevant Xaero resources, and record the
actual effective resource-pack order and model identities. Do not reuse C4/C5
cache results. Every row must visibly have the intended textured head icon,
finite framing, no body/limb/wing/held-item contamination, and unchanged
in-world EMF/Fresh Animations rendering.

| Cases | Required observation |
| --- | --- |
| Axolotl; sniffer | Preserve the confirmed C4/C5 head icon. |
| Iron golem; farmer | Preserve C5's successful icon and headwear result. |
| Mason; butcher; cleric | Restore the hatted-villager head icon; mason is the regression control. |
| Wolf; bat; parrot; frog; ravager; witch | Visible, correctly framed EMF head icon. |
| Allay; vex | The C6 result replaces the ordinary fallback with the EMF head icon; confirm one stale-failure retry at most, then final successful cache/display behavior. |
| Sheep; horse; sea turtle; creeper; normal villager; vanilla non-EMF | Existing normal/Xaero behavior remains unchanged. |
| Resource reload | Relevant successful and FAILED entries are evicted; only structurally observed EMF types may retry one stale FAILED value, with no persistent retry loop. |

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for an absent/fallback/blank,
body-contaminated, wrongly framed or stale icon; a C6 Mixin error; an EMF/Fresh
Animations world-rendering regression; a normal Xaero icon regression; or any
cache/reload result other than the bounded behavior above. A build, allocated
destination, submitted vertices, diagnostic line, or cache insertion is not a
visible-icon pass. Do not mark C6 accepted without exact-artifact runtime
observations.

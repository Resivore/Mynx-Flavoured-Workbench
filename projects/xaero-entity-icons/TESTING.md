# Testing

Exact current release `xaero-emf-entity-icon-compat-0.1.0-canary9.jar` (version `0.1.0-canary9`, SHA-256 `184ca0da6d6c055c0ba9b2d0e9fb3bccc3c30da9ea7f700a64792d4d1513551a`, source `6c1b4e0ede3ada8470bc7b4ba54ea8b51f9d2e93`) is accepted in Workbench Stack v27 after the user's 2026-09-06 user-reported external aggregate PASS. No Test Slot deployment or slot history was created; Slot A remained empty and Slot B was preserved. No row-level observations beyond the reported PASS are inferred.

## Future regression procedure

The retained procedure below is for future regressions of this exact accepted identity or an explicitly identified successor. Any former pre-promotion candidate wording is historical and superseded by this accepted result.

# Testing

Current `xaero-emf-entity-icon-compat-0.1.0-canary9.jar` is **ACTIVE /
STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED**. It is unaccepted and has no
rollback. Identity: 42,828 bytes, SHA-256
`184ca0da6d6c055c0ba9b2d0e9fb3bccc3c30da9ea7f700a64792d4d1513551a`;
implementation source checkpoint is `6c1b4e0ede3ada8470bc7b4ba54ea8b51f9d2e93`.

Two independent Java 25 / Gradle 9.5.1 / Loom 1.17.19 clean offline `check
jar` builds produced byte-identical C9 files. All 49 tests in nine suites
passed. The client-only allowlist passed; the 28-entry archive has no duplicate
or nested-JAR entries, and every ZIP entry was read successfully for the CRC
scan. No Minecraft game was launched. These are static results, not runtime
observations.

## C8 external runtime evidence reconciled for C9

Exact C8 is `xaero-emf-entity-icon-compat-0.1.0-canary8.jar`, 42,659 bytes,
SHA-256 `337adff555afae9fbcedf1c51320595654bfe4f1ab120f8c0dfb7d03bad3aa7c`,
source checkpoint `8c45f2f9ccdcf12f4403ba3380a32f81f975e077`. The user reports
that C8 fixed the remaining hatted villagers: farmer, cleric, butcher, and
mason are all good. No other unreported C8 row is inferred.

Frog, allay, and vex still show Xaero's yellow generic entity marker plus name
with no rendered head icon. For allay and vex the canonical identity is
`root/head`; for frog it is `root/body/head`. Each diagnostics sequence reaches
`MANAGER_RETURNED_NULL`, `MISSING_RETAINED_VANILLA_GEOMETRY`, downstream
failure, cached failure, and the bounded C7 prerender retry; vex records one
deferred retry before that prerender retry. Crucially none emitted
`TRACED_HEAD_FRAME_FALLBACK`.

C8 rejected `followPath(vanillaRoot, canonicalPath) == null` before collecting
or selecting a traced candidate. Its fallback therefore handled only a retained
canonical path whose geometry owner was absent, not the observed missing-path
state. C9 records whether the retained canonical path is found or absent, keeps
the C8 found-path behavior, and permits the same fallback only after exactly one
traced semantic head passes existing direct-cube and finite/invertible transform
checks. It never selects body, wing, arm, leg, sibling, ambiguous, empty, or
untraced geometry; the C7 cache policy and C8 transformed hatted-villager code
are unchanged.

## C9 manual runtime matrix

Under separately authorized Test Instance Manager ownership only, install the
exact C9 hash above, clear/reload relevant Xaero resources, and record effective
resource-pack order and model identities. **PASS** is only a correctly textured
and framed minimap head icon. Xaero's yellow generic marker plus name, a blank
icon, wrong framing or texture, body/wing contamination, or a repeated failed
retry loop is **FAIL**. Do not promote from this procedure without actual
observations.

| Cases | Required observation |
| --- | --- |
| Fix: frog | `RETAINED_CANONICAL_PATH_ABSENT`, selected traced head, `TRACED_HEAD_NO_RETAINED_PATH_FALLBACK`, nonempty draw, downstream acceptance, cached success, and final manager icon; visually correct head only. |
| Fix: allay | Same sequence and correct textured/framed head only for canonical `root/head`. |
| Fix: vex | Same sequence and correct textured/framed head only for canonical `root/head`; retain the existing single bounded retry lifecycle. |
| Preserve: axolotl; sniffer; iron golem; wolf; bat; parrot; witch; ravager; farmer; cleric; butcher; mason | Existing correctly framed head icons remain correct; hatted villagers retain complete visible transformed nested headwear without body or arms. |
| Controls: normal villager; sheep; horse; sea turtle; creeper; vanilla non-EMF entity | Preserve ordinary Xaero behavior. |
| World rendering and reload | Fresh Animations/EMF world rendering remains intact. Reload clears relevant cache state with no persistent retry loop. |

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for any fail condition,
ordinary-icon regression, EMF/Fresh Animations world-rendering regression,
Mixin error, or cache behavior outside the bounded lifecycle.

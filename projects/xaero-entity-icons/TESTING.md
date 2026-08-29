# Testing

## Current gate

Exact Canary 3 is the accepted Xaero Minimap × EMF Entity Icon Compatibility
release. The user supplied one aggregate focused runtime **PASS** for the exact
retained artifact; the historical checklist rows were not reported separately
and must not be retroactively classified one by one.

This migration did not deploy an artifact, launch Minecraft, change either Test
Slot or the accepted baseline, modify map data, or access the protected gameplay
instance. The retained deployment and runtime classifications are prior evidence
for the exact binary, not new validation performed during migration.

- Current/accepted: `xaero-emf-entity-icon-compat-0.1.0-canary3.jar`,
  28,351 bytes, SHA-256
  `4F34D743F5FFFD8E938C8F5157C630FD85F3B263AC1AE9F96C432CFE51668DF2`,
  from legacy source `2478f021982c5f26e51ff0579fa2f5dad85ed200`.
  No prior canary is a valid known-good rollback; C1 and C2 remain failed or
  incomplete provenance only.
- Migration verification reproduced the Java 25 / Gradle 9.5.1 controlled
  build against the exact Xaero 26.4.2, World Map 1.44.2, EMF 3.2.6, ETF 7.1.1,
  and Fresh Animations 1.10.5 inputs: 35/35 tests passed with zero failures,
  errors, or skips, and client-only packaging verification passed. The fresh
  build was not substituted for the retained runtime-tested JAR.
- The immutable Canary 3 source and embedded metadata still carry their
  pre-runtime generated/not-deployed classification. Preserve those bytes;
  current acceptance is recorded only in the canonical status and evidence.

## Retained unsuccessful and incomplete evidence

- Diagnostic1, `xaero-entity-icon-diagnostic-0.1.0-diagnostic1.jar`, 6,742
  bytes, SHA-256
  `05C41F987909276A29CF106F8CBE138E83F6DCBD4DDE401F193F21CEE31EF440`,
  from `a92f62c0c51286d8a8844b0707b813a1eb14c7cb`, is retired diagnostic
  provenance. Adult sheep and creeper had empty traces, returned `FAILED`, and
  remained absent; baby sheep had a non-empty trace and atlas icon at `0,64`
  but still had no final minimap head.
- Canary 1, `xaero-emf-entity-icon-compat-0.1.0-canary1.jar`, 7,555 bytes,
  SHA-256
  `A9F21BAADDDCDFA1AE937653B11E5274C6CA9EEF954A303CCC98EE7469A33239`,
  from `0da32ff62bf191165a7989f8aff227502d7d05d0`, is **FAIL / NOT
  ACCEPTED**: with Fresh Animations enabled, sheep and horse heads remained
  absent. No all-mob failure is inferred.
- Canary 2, `xaero-emf-entity-icon-compat-0.1.0-canary2.jar`, 16,848 bytes,
  SHA-256
  `B796A2CC55AE50B8D927EA576D3AF1F2C9E4FAAB4446460B2F3681CE1411BA73`,
  from `647d4646c0fa7ec2982e8ef18f49f61b579db57e`, is **PARTIAL RUNTIME
  PASS / NOT ACCEPTED**: sheep rendered at giant/bad scale, horse remained
  absent, and sea-turtle geometry was malformed. It must not be restored or
  promoted as a fix.

## Current useful runtime checks

Run these only in a separately authorized runtime task when a specific issue
requires confirmation or a successor candidate exists. Use the exact candidate
hash, fully restart the client, enable Fresh Animations, and configure Xaero to
show entity heads.

1. Confirm an adult sheep head renders at normal Xaero icon scale and framing,
   not the Canary 2 full-body or giant result.
2. Confirm a horse head/neck icon renders with normal framing and orientation.
3. Confirm a sea turtle shows only the intended head without body, shell, or
   rectangle-like cube geometry.
4. Recheck a creeper or another actually established working Fresh Animations
   mob for no regression, duplicate icon, or changed framing.
5. Observe the same entities in-world and confirm normal Fresh Animations
   geometry, animation, orientation, and scaling remain unchanged.
6. Record baby-sheep and any other unreported mob/state result separately; the
   prior baby-sheep downstream display failure and broader entity coverage were
   not proven fixed by the aggregate Canary 3 report.

Stop and record **FAIL** for a crash, dependency or mixin failure, missing or
malformed sheep/horse/turtle icon, control regression, duplicate rendering, or
in-world Fresh Animations change. Record **INCONCLUSIVE** when the exact hash,
resource/mod versions, Xaero head configuration, full restart, representative
entity, or visual observation cannot be confirmed. Do not turn an unreported
mob/state observation into a Canary 3 pass, change another project's files, or
promote a successor from static/build evidence alone.

# Testing

Canary 3 is **ACTIVE / CONTROLLED_VALIDATION_PASS / DEPLOYED / RUNTIME_FAIL**.
The user withdrew its aggregate pass and reported no correct head icon for all
11 entities listed below. Project acceptance and rollback are empty. Its exact
28,351-byte artifact, `xaero-emf-entity-icon-compat-0.1.0-canary3.jar`, SHA-256
`4f34d743f5fffd8e938c8f5157c630fd85f3b263ac1ae9f96c432cfe51668df2`, source
`2478f021982c5f26e51ff0579fa2f5dad85ed200`, remains unchanged.

DEPLOYED describes the exact file found in the dedicated profile and the existing
manager baseline entry. It does not claim a fresh readiness check or project
acceptance. This project-only correction does not edit that manager entry or
assign a slot. A separate manager owner must reconcile the withdrawn acceptance.
Earlier Diagnostic1/C1/C2 observations remain in CODEX_LOG.md; no individual C3
control pass is inferred from the withdrawn aggregate report.

## Profile boundaries

- Dedicated managed test target: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha Flavoured 26.2 Workbench`. Deployment requires separate serialized manager ownership.
- External/manual evidence only: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha 26.2 Player Instance`. Read-only logs, archived logs, crash reports, config and installed-mod metadata/hash inspection are authorized. It may be in active use. Never modify files, deploy, launch/stop/restart/control the game or launcher, acquire exclusive locks, or enroll it in the manager. Capture only necessary evidence outside the profile with source/time and live/stability labels; skip or retry locked/changing files without interrupting gameplay. Never commit/publish raw private evidence.
- Protected `Matcha Flavoured 26.1.2` gameplay instance: permanently off-limits, including reads.

## Future combined runtime procedure

The intended pair is Ribbits compatibility `0.1.0-canary2` and EMF compatibility
`0.1.0-canary4`. Neither successor has been built or validated by this correction.
Do not deploy the failed current pair as a replacement test candidate.

1. Finish the successors and their exact dependency/structural gates, independent
   clean deterministic builds, artifact scans, and production Knot/Mixin checks.
   Use the external session/version evidence and its limits in the audit addendum.
2. In a separately authorized serialized Test Instance Manager task, assign the
   two independently identified successor members as one atomic cohort. Record
   each filename, version, bytes, SHA-256 and source checkpoint, all dependency
   identities, complete resource-pack order and effective model resources. Preserve
   the other slot. No assignment, deployment or Minecraft launch is authorized here.
3. Fully restart; retain the launch log and bounded activation/stage diagnostics.
   Enable Xaero entity heads and put each representative entity within an admitted
   category and range. Compare against heads disabled, category excluded and out
   of range controls. Restore the positive configuration before grading icons.
4. Check Ribbit active textures and every direct cube at exact `main/body` with
   inherited main/body transforms; exclude all children, descendants and siblings.
   Cover Nitwit, Chef, Farmer, Merchant and Guard, adult/baby, Pride, rain/umbrella
   and instrument variants. Keep Wandering Ribbit outside this provider.
5. Grade wolf, bat, axolotl, parrot, frog, allay, sniffer, iron golem, vex, ravager
   and witch separately. Check intended head-local geometry, framing and texture;
   exclude torso, wings, legs, held items and unrelated sibling geometry.
6. Retain adult sheep, horse, sea turtle and creeper controls; also use a vanilla
   entity without an EMF replacement and an unsupported non-Ribbit GeckoLib entity.
   Compare normal in-world Fresh Animations and Ribbits rendering throughout.
7. Exercise a valid cache entry and a deliberately failed one in the controlled
   cohort, reload resources, and confirm both are invalidated and regenerated.
   Repeat after a full restart. Capture the first rejecting stage and final
   displayed result; a successful static fixture or atlas allocation is insufficient.
8. Record independent results for the two projects and individual observations.
   Never promote either member from the other member's result or from static tests.

- **PASS:** all applicable owned cases and controls visibly show the intended
  correct head icon, correct texture/framing, reload recovery and unchanged world
  rendering. Colored dots or names are not head-icon passes.
- **FAIL:** an eligible tested entity lacks a correct head icon, shows wrong or
  contaminated geometry/texture/framing, retains a stale failed cache, crashes, or
  regresses a control. Describe only the fallback actually observed.
- **INCONCLUSIVE:** release/cohort/resource identity, category/range, cache/reload
  sequence, or visual evidence is insufficient. Do not infer unreported passes.

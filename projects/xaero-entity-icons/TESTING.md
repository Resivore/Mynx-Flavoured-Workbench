# Testing

Current `xaero-emf-entity-icon-compat-0.1.0-canary4.jar` is **ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED**.
It is unaccepted and has no rollback. Identity: 37,884 bytes, SHA-256
`03444601251c6edad68d5a19648bd37e4d925b508f0d7b8a91b352883b151639`; source checkpoint is in WORKBENCH_STATUS.json.
Two independent clean builds produced identical bytes; 36 tests in 7 suites and the
combined 80-mod production Knot/Mixin harness passed. These are not Minecraft
runtime observations. Prior C1/C3 failures remain in CODEX_LOG.md and AUDIT.md.

C4 repairs seven reproduced structural rejections: recursive canonical paths for
axolotl, frog, allay, sniffer, vex and ravager, plus wolf's transform-only head
parent. Bat, parrot, base iron golem and witch already resolve in the controlled
C3 fixtures; their external runtime failure stages remain unproven. C4 adds
bounded diagnostics for these cases, not a claim that their visible failures
are fixed. The current external iron-golem override is outside the base fixture.

## Exact future controlled inputs

Use Java 25 (build baseline Temurin 25.0.4.1+1), Minecraft 26.2, Loader 0.19.3,
Fabric API 0.157.0+26.2, Xaero Minimap 26.4.2 / nested XaeroLib 1.7.1 / World Map
1.44.2, EMF 3.2.6, ETF 7.1.1, and FreshAnimations_v1.10.5.zip. Exact hashes remain
in the manifests and audit addenda. The Ribbits input is C9, 3,333,513 bytes,
SHA-256 `e433cd048bc362edae91e2e057c8170d92d110cbe7b9917c105c7336be6543de`,
source `8dc886c01f6d402ccf19b45756383d62185f0d4e`; GeckoLib 5.5.1 is 703,096 bytes,
SHA-256 `4bf1c86b4b47aa2c5d84208255695f10d79d609b23d802c995711e64b45cfce0`.
Use the base Fresh Animations pack first, then explicitly test resource overrides
as separately identified cases. Do not infer historical pack contents from the
currently installed files.

## Profile boundaries

- Dedicated managed test target: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha Flavoured 26.2 Workbench`. Deployment requires separate serialized manager ownership.
- External/manual evidence only: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha 26.2 Player Instance`. Read-only logs, archived logs, crash reports, config and installed-mod metadata/hash inspection are authorized. It may be in active use. Never modify files, deploy, launch/stop/restart/control the game or launcher, acquire exclusive locks, or enroll it in the manager. Capture only necessary evidence outside the profile with source/time and live/stability labels; skip or retry locked/changing files without interrupting gameplay. Never commit/publish raw private evidence.
- Protected `Matcha Flavoured 26.1.2` gameplay instance: permanently off-limits, including reads.

## Future combined runtime procedure

The pair is Ribbits compatibility `0.1.0-canary2` and EMF compatibility
`0.1.0-canary4`. Both are static candidates, not runtime passes. Keep failed
Ribbits C1, EMF C3 and Diagnostic1 out of the future controlled cohort.

1. Verify the exact hashes from both current manifests and the tested dependency
   inputs below. Static gates, deterministic builds and the combined production
   Knot/Mixin harness have passed; runtime remains untested. Keep external
   session-version evidence distinct from current byte hashes.
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

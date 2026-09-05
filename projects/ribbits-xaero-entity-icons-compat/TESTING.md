# Testing

Current `ribbits-xaero-entity-icons-compat-0.1.0-canary4.jar` is **ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED**.
It is unaccepted and has no rollback. Identity: 61,889 bytes, SHA-256
`cc4a690712a37e3fa58e4bff7fce6164285e4a91b8e83ca665ded60c2b0cdc2d`; source checkpoint is in WORKBENCH_STATUS.json.
Clean builds against actual Fabric 26.2 GeckoLib 5.5.4 and 5.5.1 produced identical
bytes. Each passed 71 tests in 16 suites and the combined production Knot/Mixin
classloading harness without launching Minecraft. These are not runtime observations. Prior C1/C3 failures remain in CODEX_LOG.md and AUDIT.md.

C4 preserves every direct cube at `main/body` and excludes all children/siblings.
Exact Ribbits C9, Minimap 26.4.2 and nested XaeroLib 1.7.1 gates remain unchanged.
GeckoLib must be present and expose the required renderer/model/state/direct-cube
APIs. Its version, archive size, hash and origin layout are not activation allowlists.
Missing requirements produce a named `incompatible GeckoLib API` or missing-mod
diagnostic and disable the patch. The API probe reads signatures without loading
game classes during mixin setup.
Sizing/orientation contract: Xaero captures an ordinary 8/16-model-unit head with
32 pixels per model unit, giving a 16-pixel span. C4 normalizes selected bounds to
that span, retaining Xaero's capture `min(1, Parameters.scale)` and downstream
display magnification. It no longer fills the 58-pixel clipping window. Creator
resets incoming pose/model-view to identity; no vanilla model-form rotation has
run at this seam. Gecko's baked +Y up/-Z face becomes screen -Y/+Z toward camera,
with static bind/cube transforms intact. Real-model tests verify composed bounds
and face normals; they do not establish visible pixels or runtime success.

The latest user-reported external visual FAIL described enormous icons, top-of-head
rather than face views, and absent Wandering support. No observation timestamp,
installed compatibility JAR hash/version, session or resource-pack identity was
supplied with that report. Do not bind it to an exact prior release or to C4.

A nonempty vertex submission or allocated atlas is not proof of visible pixels.

## Exact future controlled inputs

Use Java 25 (build baseline Temurin 25.0.4.1+1), Minecraft 26.2, Loader 0.19.3,
Fabric API 0.157.0+26.2, Xaero Minimap 26.4.2 / nested XaeroLib 1.7.1 / World Map
1.44.2, EMF 3.2.6, ETF 7.1.1, and FreshAnimations_v1.10.5.zip. Exact hashes remain
in the manifests and audit addenda. The Ribbits input is C9, 3,333,513 bytes,
SHA-256 `e433cd048bc362edae91e2e057c8170d92d110cbe7b9917c105c7336be6543de`,
source `8dc886c01f6d402ccf19b45756383d62185f0d4e`. Primary GeckoLib validation input:
`geckolib-fabric-26.2-5.5.4.jar`, 1,183,876 bytes, SHA-256
`a5770f9ea0c21db157559fe266874fd84be8c7da689d37aa7bf2b06304a6a65d`.
Regression input: `geckolib-fabric-26.2-5.5.1.jar`, 703,096 bytes, SHA-256
`4bf1c86b4b47aa2c5d84208255695f10d79d609b23d802c995711e64b45cfce0`.
These are exact validation records, not runtime version restrictions. Record the
actual GeckoLib binary used for future observations; do not downgrade any instance
to match a validation baseline. Fixture metadata changes are not binary evidence.
Use the base Fresh Animations pack first, then explicitly test resource overrides
as separately identified cases. Do not infer historical pack contents from the
currently installed files.

## Profile boundaries

- Dedicated managed test target: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha Flavoured 26.2 Workbench`. Deployment requires separate serialized manager ownership.
- External/manual evidence only: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha 26.2 Player Instance`. Read-only logs, archived logs, crash reports, config and installed-mod metadata/hash inspection are authorized. It may be in active use. Never modify files, deploy, launch/stop/restart/control the game or launcher, acquire exclusive locks, or enroll it in the manager. Capture only necessary evidence outside the profile with source/time and live/stability labels; skip or retry locked/changing files without interrupting gameplay. Never commit/publish raw private evidence.
- Protected `Matcha Flavoured 26.1.2` gameplay instance: permanently off-limits, including reads.

## Future combined runtime procedure

The pair is Ribbits compatibility `0.1.0-canary4` and EMF compatibility
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
4. At identical minimap zoom and icon-size settings, compare each Ribbit head
   against an ordinary vanilla head. Exercise smaller, default and larger icon-size
   settings: sizes should track the setting together. Confirm eyes above face,
   upright front view, no top/back view, no clipping and no mirrored active texture.
   Check Ribbit active textures and every direct cube at exact `main/body` with
   inherited main/body transforms; exclude all children, descendants and siblings.
   Cover Nitwit, Chef, Farmer, Merchant and Guard, adult/baby, Pride, rain/umbrella
   and instrument variants. Separately check `ribbits:wandering_ribbit`: upright
   face core and paired eyes with its active texture; exclude clothes, backpack,
   leaf, arms and legs. Its exact renderer requires living Geo state, not ordinary
   profession/instrument/umbrella/Pride tickets. Changed face layout fails closed.
5. Grade wolf, bat, axolotl, parrot, frog, allay, sniffer, iron golem, vex, ravager
   and witch separately. Check intended head-local geometry, framing and texture;
   exclude torso, wings, legs, held items and unrelated sibling geometry.
6. Retain adult sheep, horse, sea turtle and creeper controls; also use a vanilla
   entity without an EMF replacement and an unsupported non-Ribbit GeckoLib entity.
   Compare normal in-world Fresh Animations and Ribbits rendering throughout.
7. Exercise a valid cache entry and a deliberately failed one in the controlled
   cohort, reload resources, and confirm both are invalidated and regenerated.
   Verify both ordinary and Wandering success/FAILED caches regenerate independently.
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

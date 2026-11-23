# Codex Log

## 2026-11-23T07:19:03.151172Z — Croak Soldier Piglins Canary 1

- Revision: 1
- Source checkpoint: `db298f859c11c2da95bdb3ea9e482174e0606887`
- Changes: Added adult piglin renderer selection, a reload-scoped asset view, and two GeckoLib controllers. The adapter normalizes legacy easing names in memory.
- Build/static: Built with Java 25 and Gradle 9.5.1 against GeckoLib 5.5.1 and 5.5.7. Verified the six-bone model, five clips and their bone-track counts, idle sine easing, texture resolution, and code-only archive.
- Runtime: RUNTIME_UNTESTED. No client launch or testing profile access.
- Artifact: `croak-soldier-piglins-0.1.0+26.2-canary1.jar`, SHA-256 `0c0ae5382fa1fb8e7952f45fa38836b756d90ec7b9f46978a1a6b5ab5e113a32`, finalized 2026-11-23T07:17:33.145103Z.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED.
- Next state: Observe the adult and control entities with EMF/ETF using TESTING.md before equipment integration.

## 2026-11-23T16:23:37.987227Z — Correct Croak Soldier Piglins activation in Canary 2

- Revision: 2
- Source checkpoint: `eb49b73f8145639cf801239fe5e822e5eb21747c`
- Changes: Bound the replacement to the registered piglin renderer during state extraction and submission. Initialization checks visible resources and the actual GeckoLib bake on adult use, independent of pack class. Added stage diagnostics and a private local build carrying three Soldier assets, license notice, and credits.
- Build/static: Traced C1 through Fabric's production loader and resource reload off-game. The supplied Workbench archive passed all three hashes, the model source was SoldierPack, GeckoLib baked the model, and normal dispatcher lookup returned SoldierRenderer. Calling the registered renderer directly reproduced vanilla selection. C2 routes that direct call to Soldier extraction. With Fabric 26.2, EMF 3.2.6, ETF 7.1.1 and GeckoLib 5.5.1, the private build resolved through ModNioPackResources with the property cleared. Verified entry into the Gecko render pass, age/type exclusions, the authored clips and idle easing. Also checked the external route with GeckoLib 5.5.7. No world or frame was rendered.
- Runtime: Owner-reported C1 RUNTIME_FAIL, bound to `croak-soldier-piglins-0.1.0+26.2-canary1.jar`, SHA-256 `0c0ae5382fa1fb8e7952f45fa38836b756d90ec7b9f46978a1a6b5ab5e113a32`. Ordinary adult piglins, including naturally encountered bastion piglins, appeared vanilla with the documented JVM argument supplied. No Soldier was visible. The observation does not identify the failing stage. C2 RUNTIME_UNTESTED.
- Artifact: `croak-soldier-piglins-0.1.0+26.2-canary2-private.jar`, 30,284 bytes, SHA-256 `e1623d84ea22863fab114eff2bdb8b5086c3f4cd39633eaf86746eca72d48f76`, finalized 2026-11-23T16:19:46.768233Z. Canonical C2 is the local private test artifact.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED for C2.
- Next state: Owner visual check of ordinary adults, movement and controls with private C2 and the documented stage logs.

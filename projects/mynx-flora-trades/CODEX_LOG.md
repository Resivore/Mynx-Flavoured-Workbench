# Codex Log

## 2026-09-07T02:51:15Z — Implement Mynx Flora Trades Canary 1
- Revision: 1
- Source checkpoint: `18ec89dcfbc627a6adc003dc9d735788cdd2013a`
- Changes: Added the isolated Fabric integration. Florist is a one-level data-driven profession with eight simultaneous fixed `minecraft:emerald` (Matcha display: Obol) to eight-MRU-flora offers, zero XP, ordinary stock, and a FlowerPotBlock-safe POI seam. Added persistent two-choice Farmer buybacks and an isolated ordinary Wandering provider with two persisted flora groups.
- Build/static: Java 25 / Gradle 9.5.1 compile and JAR packaging passed.
- Runtime: RUNTIME_UNTESTED; no client, deployment, slot, or protected profile was touched.
- Artifact: `mynx-flora-trades-0.1.0+26.2-canary1.jar`; SHA-256 `5a7ef239c3be0c4e2e7b14d30c474c245bff7727122da0e7c9acc6aec2224606`.
- Result: ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED.
- Next state: Retain the exact artifact, publish the integrated revision from main through the gated Sheet workflow, then conduct the focused dedicated-Workbench runtime procedure.

## 2026-09-07T18:12:21Z — Repair Mynx Flora Trades Canary 2
- Revision: 2
- Source checkpoint: `3d3678f1a79ddfbbac8c63bdff7ef8b824350f1f`
- Changes: Recorded Canary 1 (`mynx-flora-trades-0.1.0+26.2-canary1.jar`, SHA-256 `5a7ef239c3be0c4e2e7b14d30c474c245bff7727122da0e7c9acc6aec2224606`) as user-reported external runtime FAIL for reversed Farmer/Wandering provider trade direction and flower pots not yielding Florist acquisition. Canary 2 makes Glowcap the exact input and MRU flora the output for all provider-owned Farmer/Wandering offers, retaining Glowleaf’s authoritative `dropleaf` ID, selection/persistence semantics, provider isolation, and ordinary Wandering restocking. It keeps Florist Obol-to-eight-flora trades unchanged and adds the missing PoiTypes `hasPoi` FlowerPotBlock scan hook alongside `forState`, allowing the real PoiManager section scan to create Florist records.
- Build/static: Java 25 / Gradle 9.5.1 clean `check build` passed; focused Farmer/Wandering direction, Florist economy, MRU FlowerPotBlock-type, and production Minecraft 26.2 packaged-mixin POI lookup coverage passed.
- Runtime: Canary 1 is RUNTIME_FAIL from the supplied external/user report only; Canary 2 is RUNTIME_UNTESTED and was not deployed. No Ribbits Canary 17 failure is attributed because the defect was provider-owned.
- Artifact: `mynx-flora-trades-0.1.0+26.2-canary2.jar`; 20,413 bytes; SHA-256 `5a611b1cfc3fba7411f8757d48cef0fd44c25bfebe29876ccc9cc2f21d136b4d`.
- Result: ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED.
- Next state: Retain Canary 2 locally, publish revision 2 from integrated authoritative main through the gated Sheet workflow, then run the focused dedicated-Workbench retest without changing either existing test cohort.

## 2026-09-07T20:20:37Z — Repair Mynx Flora Trades Canary 3
- Revision: 3
- Source checkpoint: `d9ec458f878913e78517ef672c64b07100b5c880`
- Changes: Recorded Canary 2 as user-reported external RUNTIME_FAIL: Farmer/Wandering direction improved, but Farmer Flora offers granted no merchant XP, Florist did not acquire flower pots, and unemployed villagers did not become `mynx_flora_trades:florist`. Traced Minecraft 26.2's real path: `VillagerProfession.NONE` uses `ALL_ACQUIRABLE_JOBS`, which tests `PoiTypeTags.ACQUIRABLE_JOB_SITE`, before `AcquirePoi` reserves a candidate. FlowerPotBlock recognition/indexing worked; Florist was absent from that tag and was filtered before reservation. Added the non-replacing `data/minecraft/tags/point_of_interest_type/acquirable_job_site.json` extension for Florist while retaining the narrow FlowerPotBlock mixins needed for dynamic pot-state recognition. Farmer offers now use their native tier as merchant XP: tier 1 = 1 and tier 2 = 2; Wandering grants none and the one-level Florist remains zero-XP.
- Build/static: Two independent Java 25 / Gradle 9.5.1 clean builds were byte-identical. JUnit economy coverage passed; three controlled Fabric GameTests passed, including empty-pot and potted-poppy real `AcquirePoi` candidate/reservation, vanilla assignment, and release after pot break. Test-only Ribbits fixtures isolate the real Florist registry path while the accepted MRU Canary 3 JAR supplies the actual flora registry; no fixture ships in the production JAR. Packaged Minecraft 26.2 POI mixin lookup coverage passed.
- Runtime: Canary 2 is external/user-reported RUNTIME_FAIL only. Canary 3 is RUNTIME_UNTESTED, NOT_DEPLOYED, and occupies no Test Slot; no profile was touched.
- Artifact: `mynx-flora-trades-0.1.0+26.2-canary3.jar`; 21,222 bytes; SHA-256 `fe08a7da4ef0e24780432f9094ed2aa4b55fd4c7808b6811e8ba4be9e9ebf484`.
- Result: ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED.
- Next state: Retain this exact artifact, publish revision 3 from integrated authoritative main through the gated Sheet workflow, then perform the focused dedicated-Workbench runtime procedure without changing either test cohort.

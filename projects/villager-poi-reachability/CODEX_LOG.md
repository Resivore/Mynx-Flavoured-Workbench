# Codex Log

## 2026-09-07T22:46:39Z — Implement Villager POI Reachability Canary 1
- Revision: 1
- Source checkpoint: `e534b3d77417dcc58c54ccee775a70697eeaac86`
- Changes: Traced Minecraft 26.2 `AcquirePoi.findPathToPois`: it submits real candidate POI block positions to `PathNavigation.createPath` and accepts only `Path.canReach()`. A raised workstation above a solid support therefore fails at the direct real-block target even though POI discovery/indexing is valid. Added an `AcquirePoi` return hook limited to Villagers and `acquirable_job_site` candidates. Only after vanilla fails it uses real navigation to an exact adjacent position at POI Y-1, requires a clear interaction ray, then returns a path whose target remains the original real POI. Reservation, memories, and profession assignment remain vanilla-owned.
- Build/static: Java 25 / Gradle 9.5.1 unit build and GameTest-source compilation passed. Headless Fabric GameTests passed 7/8: raised Brewing Stand/Cleric, raised Composter/Farmer, normal-height Cleric, two-block raised negative, sealed negative, single-capacity reservation, and real-POI break release. The only unavailable fixture was Florist, because the ignored C4/Ribbits/MRU test JARs are absent; it was not treated as a pass. No client gameplay runtime was observed.
- Runtime: NOT_DEPLOYED / RUNTIME_UNTESTED; no Test Slot or protected profile was touched.
- Artifact: `villager-poi-reachability-0.1.0+26.2-canary1.jar`; 5,356 bytes; SHA-256 `b7342e3c0833d08139482263ea454575e94ba24338aa8b23c330db348dbb6848`.
- Result: ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED.
- Next state: Retain exact bytes, publish the integrated revision through the gated Sheet workflow, restore the controlled Florist fixture for its GameTest, then run the dedicated-Workbench matrix.

## 2026-09-14T08:09:29Z — Owner-directed lifecycle acceptance

- Revision: 2
- Source checkpoint: `f511f435c623883c5ee1474e9227acb73fb2df68`
- Changes: Changed lifecycle from ACTIVE to ACCEPTED and bound the unchanged exact current release as accepted by project-owner direction; historical testing records remain intact.
- Build/static: Preserved the existing STATIC_PASS classification; no build or artifact substitution was performed.
- Runtime: No Minecraft runtime testing was performed by Codex for this administrative transition. The project owner's external/gameplay testing authorized acceptance, but no new PASS or other runtime result was recorded or inferred; the existing RUNTIME_UNTESTED classification is unchanged.
- Artifact: Current release identity is unchanged: version 0.1.0+26.2-canary1, filename villager-poi-reachability-0.1.0+26.2-canary1.jar, SHA-256 b7342e3c0833d08139482263ea454575e94ba24338aa8b23c330db348dbb6848, source e534b3d77417dcc58c54ccee775a70697eeaac86.
- Result: ACCEPTED — owner-directed acceptance without fabricated runtime evidence.
- Next state: Preserve the accepted release and historical evidence. A future successor may use TESTING while awaiting or receiving user runtime validation; no Workbench slot or profile allocation is required.

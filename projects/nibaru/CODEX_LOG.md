# Codex Log

## 2026-08-29T17:14:33Z — Migrate frozen Nibaru project into Mynx Flavoured Workbench
- Revision: 5
- Source checkpoint: `6e9b04ec3947f2514f90775bfeefb599242a4596`
- Changes: Imported the maintainable C46 source, build, focused fixtures, licensing, and retained artifacts from `Resivore/Minecraft-26.2-Workbench` checkpoint `1b4c070d7ea5b0699f518f91d0bec35e9a0679a0`; normalized `projects/nibaru-26.2` to `projects/nibaru` without adding a BGE dependency.
- Build/static: Java 25 clean build and migrated fixtures/resource-semantic checks passed; all exact retained artifact hashes were verified, and the paired C46/BGE C53 controlled suite passed 66/66 required GameTests.
- Runtime: No Minecraft deployment or runtime test was performed.
- Artifact: Current C46 `more-slabs-stairs-and-walls-4.2.0+26.2-port-canary46-bge-layer-contract.jar` SHA-256 `3281d110f062db62e721d838a35ce915ca73dd41af098a013b52952561ceae7d`; accepted/rollback C45 (legacy Canary 43) `more-slabs-stairs-and-walls-4.2.0+26.2-port-canary43-native-directional-material-axis.jar` SHA-256 `0a979a75101076e987a35807f8eb293631fe5e664b252e5db0aa263d4eedf07f`.
- Result: ACTIVE — GENERATED / CONTROLLED VALIDATION PASS; NOT DEPLOYED; RUNTIME UNTESTED. C46 remains unaccepted and C45 remains accepted/rollback.
- Next state: Deploy exact C46 and BGE C53 as a controlled pair and complete their runtime procedures before either promotion.

## 2026-08-31T16:42:32Z — Park Nibaru provenance after source absorption into unified BGE
- Revision: 6
- Source checkpoint: `f907bdab139fd2ec68f8741f5a449b9ee0c973f4`
- Changes: Preserved immutable project identity and exact C45/C46 provenance while absorbing exact C46 native blocks, material profiles, specialized semantics, resources, build support, and LGPL-covered source into BGE; removed the active standalone source/build tree and future release boundary, retained the canonical controls, exact historical artifacts, license, and attribution, and created no Nibaru successor artifact.
- Build/static: The exact C46 predecessor clean Java 25 build and migrated fixtures passed before movement; unified BGE then compiled the absorbed source directly, passed all migrated Nibaru checks, generated-resource audits, artifact-union inspection, and 85/85 controlled GameTests. These checks do not constitute an independent standalone Nibaru runtime pass.
- Runtime: Exact C46 participated in the user-tested C56/C46 migration baseline, whose covered behaviors passed while C56 was overall `FAIL` solely for the deferred small glass-Corner visual issue; no independent C46 pass, promotion, or successor result is claimed. The standalone C46 candidate left Slot A when unified C57 provisionally deployed, while accepted C45 remains disabled rollback provenance.
- Artifact: Retained current C46 is `more-slabs-stairs-and-walls-4.2.0+26.2-port-canary46-bge-layer-contract.jar`, 5,652,769 bytes, SHA-256 `3281d110f062db62e721d838a35ce915ca73dd41af098a013b52952561ceae7d`, source `6e9b04ec3947f2514f90775bfeefb599242a4596`; retained accepted/rollback C45 is `more-slabs-stairs-and-walls-4.2.0+26.2-port-canary43-native-directional-material-axis.jar`, SHA-256 `0a979a75101076e987a35807f8eb293631fe5e664b252e5db0aa263d4eedf07f`.
- Result: Proposed `PARKED` — Nibaru remains historical provenance with LGPL licensing and retained artifacts; future native-material and extended-geometry ownership belongs to unified BGE, no artifact was invalidated, and no independent testing plan remains.
- Next state: Keep the parked state pending separate integration into `main` and later Sheet publication; use retained C45/C46 only for explicitly authorized rollback or reproduction, with all future validation routed through unified BGE.

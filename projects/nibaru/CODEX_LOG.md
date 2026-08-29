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

# Codex Log

## 2026-08-29T17:57:56Z — Migrate frozen Horse Immunities into Mynx Flavoured Workbench
- Revision: 6
- Source checkpoint: `f0b8b66cc657bfd6303475fab75f385f06748782`
- Changes: Imported the maintainable source, build files, five focused test suites, and exact C2/C3 artifacts from frozen legacy checkpoint `78bb33251b9fa8c2f9373be75c1bd23f7ca845b2`; normalized `projects/sweet-berry-horse-immunity` to `projects/horse-immunities` while preserving the legacy mod, package, archive, and alias identities. The imported C3 implementation originated at legacy source commit `e5fa00468e821c8f2dbfee1c34df5fa4a762b4e0`.
- Build/static: Temurin Java 25.0.4+7 and Gradle 9.5.1 `clean test build --no-daemon` passed 21/21 focused tests in five suites with zero failures, errors, or skips; both exact retained artifacts were independently SHA-256 verified. The fresh verification build did not replace either retained JAR.
- Runtime: No deployment or Minecraft runtime check was performed; neither runtime slot, Minecraft instance, accepted runtime baseline, nor Test Instance Manager state was touched.
- Artifact: Current/unaccepted C3 `sweet-berry-horse-immunity-0.1.0-canary3.jar` SHA-256 `ab947253c5a6bc1910f05367ca9cc5ed7b5388f1d343b4889605d7b15a10060b`; accepted/rollback C2 `sweet-berry-horse-immunity-0.1.0-canary2.jar` SHA-256 `caddc47f9db0d374d44da4beb21605ad5bcb28ecf162136b774733e9dfe37697`, retained from accepted Phase 1 checkpoint `5a5a4b239772023345e15ed22b4a6f0ed0b2237b`.
- Result: ACTIVE — C3 is GENERATED / STATIC-TESTED, NOT DEPLOYED, RUNTIME UNTESTED, and not accepted; C2 remains accepted and is the rollback.
- Next state: Perform a controlled runtime handoff of exact C3 and complete the focused Horse Immunities matrix before any promotion.

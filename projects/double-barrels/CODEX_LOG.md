# Codex Log

## 2026-08-29T20:59:56Z — Migrate frozen Double Barrels into Mynx Flavoured Workbench
- Revision: 1
- Source checkpoint: `d86d4983ce93b37bb10b3fcf6ccfb6d83c0460f0`
- Changes: Imported all 45 maintainable build, Java, and resource blobs byte-for-byte plus the exact accepted C2 artifact from legacy `Resivore/Minecraft-26.2-Workbench` checkpoint `f0101f38c446dddaa4226450386402c63597ba7c`; normalized `projects/double-barrels-26.2` to `projects/double-barrels`, added project-local generated-output ignores, and omitted legacy narratives, manifests, generated output, originals, hard-coded verifiers, and failed C1 bytes.
- Build/static: All 45 imported legacy Git blobs matched exactly; Temurin Java 25.0.4+7 and Gradle 9.5.1 `clean build --no-daemon` passed with Loom 1.17.20 and no test sources; the retained 35,682-byte C2 JAR independently matched its frozen SHA-256 and embedded identity, and the fresh build did not replace it.
- Runtime: No deployment or Minecraft runtime test was performed, and neither Test Slot nor either Minecraft instance was touched. Frozen evidence retains exact C2's scope-level core-gameplay initial pass and `READY_TO_TEST_VERIFIED` promotion state without inferring individual matrix rows; the later normal-quit watchdog remains attributed to a pre-existing Stack to Nearby Chests executor leak with no Double Barrels frame.
- Artifact: Current, accepted, and rollback are the same exact `1.0.1+26.2-canary2` artifact `doublebarrels-fabric-26.2-1.0.1+26.2-canary2.jar`, SHA-256 `b2119e05c508880c7977551091515f0076dd3cdc2733e51340ac813925b553c0`, from legacy source commit `3f4d9b53c0c38f20717f1b6b1bd83ac543bd6864`; failed C1 is historical evidence, not rollback, and remains only in the legacy repository.
- Result: ACCEPTED — STATIC_PASS; READY_TO_TEST_VERIFIED; PARTIAL_RUNTIME_PASS; CURRENT_IS_ACCEPTED; ACCEPTED_IS_ROLLBACK; no Double Barrels blocker.
- Next state: Remain on exact accepted C2 unless a separately authorized change or regression run produces new evidence; any future runtime work should execute the current focused matrix without rebuilding or substituting the accepted JAR.

# BGE C91 private local-only Beam catalog manual verification

Current candidate: `BGE C91.jar` — **PRIVATE_LOCAL_ONLY; no redistribution permission.**

- Embedded version: `4.2.35-bge.canary91.private-local-beam-catalog+26.2`
- SHA-256: `c59294526858627cc20a496fd14842b4920b12c05ff66d520a024168153d2478`
- Finalized: `2026-09-21T02:09:03.6322657Z`
- Source checkpoint: `7b4619643f972222a7c8bfc189968b3c6395578c`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Private provenance: this exact artifact includes six local-only palette derivatives generated from the locally supplied BBB dev.6 Beam artwork (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`) and Enderscape 3.0.2+mc26.2 palettes (SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`). Do not copy, attach, upload, publish, release, redistribute, or move this JAR or its Beam textures into a tracked/public path.
- Immediate predecessor and supplied runtime-failure evidence: exact C89 `BGE C89.jar`, SHA-256 `cc1368f48fffcde03997c4d3f1d0e8907236c584d7ae107f35eb4e06e98667f2`. Its reported `BGE canonical binding validation failed` world-load abort applies only to those exact C89 bytes.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior observed for these exact C91
bytes. It does not authorize inspecting, creating, selecting, populating, or modifying
any protected or retired Minecraft profile.

## Catalog and selectors

1. In an ordinary permitted environment with the exact C91 candidate and its required providers, open the corresponding CNM selection families for Veiled Planks, Celestial Planks, and Murublight Planks. Each family must contain its ordinary plank forms and exactly one matching Beam group, with the Beam group named `Veiled Beam`, `Celestial Beam`, or `Murublight Beam` rather than an internal/deferred translation key.
2. For each Beam group, verify the selector order is exactly root Beam, then `Slab`, `Stairs`, `Wall`, `Vertical Slab`, `Step`, `Corner`, `Quarter Column`, and `Layer`. Confirm that no redundant effective selector role, geometry-from-geometry family, duplicate Beam root, `*_slab_wall`, or `*_wall_slab` entry appears.
3. Confirm Block of Raw Shadoline appears as one complete ordinary nine-role BGE family with the same deterministic selector ordering and human-facing names.

## Private Beam visual and geometry matrix

1. For Veiled, Celestial, and Murublight, compare the Beam side and end grain against the locally authorized BBB Beam reference. The pixel pattern, shading, alpha, topology, and directional grain intent must match; only the palette should differ to the matching Enderscape plank family. Do not export screenshots, texture files, models, or the candidate JAR.
2. For each Beam, verify axial behavior for the root Beam and every role where the established BBB Beam role is axial: placing along X, Y, and Z must retain the matching directed grain and expected collision/outline. The Wall must remain non-axis.
3. Confirm Beam Walls and matching plank Walls use the thin wooden/plank-wall post-and-arm geometry, not standard stone-wall geometry. Check straight, end, corner, T, and isolated Wall states; there must be no axis selector/state on either Wall.
4. Confirm the Step is an actual shallow Step topology rather than a Stair outline, and verify Corner orientation at all four horizontal corners. Confirm Quarter Column and Layer placement remain aligned with their selector roles.

## Terrain, Path, and retained catalog regressions

1. Inspect the affected Enderscape terrain families. Their generated roles must use their actual terrain texture sources with no Crimson Nylium/reference texture leakage.
2. For affected paths, confirm side culling is not incorrectly applied where exposed, and that Layer top/side UVs stay aligned without stretching or mirrored seams across the expected facing/orientation states.
3. Across first load and one ordinary legitimate ShapeMap rebuild, confirm normal direct material-family completion produces every supported role exactly once, preserves canonical binding validation, and does not reintroduce the C89 `BGE canonical binding validation failed` condition.

## Evidence and local retention

1. For every observation, record this candidate SHA-256, all mod/provider versions, datapack configuration, repeatable sequence, expected result, and observed result. Controlled builds, static checks, and GameTests do not establish gameplay acceptance.
2. Retain `BGE C91.jar` only in its ignored local artifact storage by the exact SHA-256. Its six private Beam derivative textures must remain only in ignored build output and this retained private JAR. Preserve C89 as failed predecessor provenance, C72 as rollback provenance, and C70 as accepted provenance; do not overwrite retained artifacts.

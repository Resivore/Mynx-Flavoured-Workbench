# Codex Log

## 2026-08-29T22:36:32Z — Create official BGE × CTM planned project
- Revision: 1
- Source checkpoint: `8c0243ebee7a9daad9b72eb66fe92c4bfc8434da`
- Changes: Created `projects/bge-ctm` with UUID `5747a309-1bd5-4280-b25c-98669e149928`, the canonical name and aliases, planned goals, scope, ownership boundaries, Google Sheet participation, a project dependency on BGE UUID `4b2342fc-7bdf-5ba6-9f37-d551109d214c`, and Continuity as the target CTM runtime dependency; no compatibility implementation was added.
- Build/static: Repository control, schema, and revision protocol validation passed; no implementation build or static code test was performed.
- Runtime: No deployment or Minecraft runtime check was performed, and no Minecraft instance was accessed.
- Artifact: None; no current, accepted, or rollback release exists.
- Result: Official PLANNED project created at revision 1 with no blocker.
- Next state: Prioritize a bounded architecture and Continuity integration audit before selecting or implementing the compatibility approach.

## 2026-08-30T20:50:07Z — Audit BGE × CTM inheritance architecture
- Revision: 2
- Source checkpoint: `777d9ece7f38bc6c323f57d12ff83d7cd31ebc59`
- Changes: Audited the current BGE/Nibaru material and generated-geometry architecture, installed Continuity 3.0.1+26.2 implementation, active Translucent Glass/Continuity/Matcha rules, generated models/tags, and representative clear-glass, chiseled-sandstone, and Matcha overlay paths. Established that BGE already has an exact derived-to-canonical family registry and canonical sprites, but does not expose canonical appearance through Fabric; documented a centralized canonical appearance-state bridge with explicit property/geometry policy as the scalable approach and replaced the generic future checklist with a focused inheritance matrix. No compatibility code or diagnostic fixture was added.
- Build/static: No project build was applicable. Repository validation passed for all 40 manifests, the Python suite passed 99/99, the Sheet receiver suite passed 29/29, and `git diff --check` passed; focused installed-artifact/resource and decompiled-class assertions supported the audit findings.
- Runtime: Inspected only the configured dedicated Workbench files and installed artifacts. Minecraft was not launched, nothing was deployed, and no source, generated-resource, fixture, or GameTest observation is classified as runtime CTM evidence.
- Artifact: None; no implementation or deployable BGE × CTM artifact exists, and accepted/rollback state is unchanged.
- Result: `PLANNED / AUDITED / RUNTIME_UNTESTED` — root cause and implementation seam are established with no architecture blocker. Partial-geometry contact, state projection, overlay unit-square/full-collision behavior, multi-element glass, translucent culling, and Complementary output remain bounded implementation/runtime proof items.
- Next state: Implement the narrow BGE canonical-appearance bridge and focused state/eligibility fixtures, produce one controlled runtime candidate, and execute the focused matrix before broadening geometry coverage or adding any Continuity-specific filter.

## 2026-09-17T18:58:45.2512715Z — Implement BGE × CTM Canary 1
- Revision: 3
- Source checkpoint: `d07552ee22f4663b50bfb8d84b9a6756df8a3c33`
- Changes: Added a standalone Fabric compatibility mod that implements one centralized appearance resolver on BGE Layer, Clutter No More Vertical Slab, and Step bases. It consumes BGE C70's typed runtime binding, admits only full four-Layers and double Vertical Slabs, explicitly projects canonical axis, glazed-pattern facing, and leaf distance/persistence, rejects risky visuals and every partial/unknown geometry fail-closed, and changes no BGE, Continuity, Matcha, or CTM asset source.
- Build/static: The exact controlled BGE and Clutter No More inputs passed SHA-256 checks; a clean Gradle build, release-boundary inspection, and artifact staging passed. The headless GameTest runner passed 8/8 total required tests (7 authored focused state/dispatch tests plus 1 harness/upstream test), and the repository Python suite completed 52 tests with one platform-limited symlink test skipped. Repository validation and `git diff --check` passed.
- Runtime: No Minecraft client, gameplay profile, or testing profile was launched, inspected, or modified. The dedicated-server GameTests prove state mapping and dispatch only; they are not visual Continuity, connected-texture, overlay, translucency, culling, or shader evidence.
- Artifact: Current `0.1.0-canary1` is `bge-ctm-0.1.0-canary1.jar`, SHA-256 `936d8c3d59e02bd76d649f5eab5cce7d0472a5c0548b65b93f2fc6c2bce0f5b6`, finalized at `2026-09-17T18:57:53.6287474Z` from source checkpoint `d07552ee22f4663b50bfb8d84b9a6756df8a3c33`; accepted and rollback releases remain absent.
- Result: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED` — the first bounded canary exists with no lifecycle acceptance or fabricated runtime result.
- Next state: Run the preserved manual matrix beginning with base clear glass, eligible four-Layer/double-Vertical clear glass and chiseled sandstone, distinct stained-glass separation, then the deliberately excluded partial/Step controls, Matcha overlay limits, and Complementary translucency before broadening eligibility or adding a Continuity-specific contact hook.

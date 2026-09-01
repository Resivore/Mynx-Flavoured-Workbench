# Mynx Flavoured Workbench operating policy

## Authority and completion

- This repository targets Minecraft Java 26.2 on Fabric.
- `main` is the sole durable authority. Project branches and worktrees are temporary task surfaces, never alternate project authorities.
- Start ordinary work from reasonably current `main`, reconcile with newer `main` when necessary, and integrate coherent checkpoints frequently.
- A project task is complete only after its checkpoint is integrated into `main`, its `WORKBENCH_STATUS.json` revision and `CODEX_LOG.md` entry are updated, and the main-only Sheet publisher has handled the new revision. While Sheet cutover is deliberately gated, the revision and publication event must still be produced for delivery after authorization.
- Commit and push durable progress. Current `main` must be reconstructable without conversation history, stale task branches, or local-only state.

## Project contract

Every official project, including a `PLANNED` project, has one directory under `projects/` or `resourcepacks/`, one immutable UUID, and exactly these control records:

- `WORKBENCH_STATUS.json` — canonical structured identity, definition, current state, provenance, and synchronization metadata;
- `TESTING.md` — only the current useful runtime procedure, expected behavior, representative cases, and stopping conditions;
- `CODEX_LOG.md` — compact append-only task history.

A planned project needs no placeholder source, build, or artifact. When implementation exists, keep maintainable source/assets and only necessary current retained artifacts alongside the controls. Do not create duplicate status readmes, narrative canary files, migration reports, or provenance documents as a routine template.

Every project task advances the manifest revision exactly once, updates synchronization metadata, and appends one log entry containing timestamp, task summary, source checkpoint, meaningful changes, static/build evidence, actual runtime evidence (if any), artifact identity changes, result/blocker, and next state. Preserve exact Canary/version, filename, SHA-256, and source checkpoint identity.

`source_commit` is the preceding coherent implementation/artifact checkpoint described by the status and log; it cannot be the commit that contains its own hash. Finish project content first, then make the administrative status/log checkpoint that references it. A status-only project initialization may reference the current authoritative base. The validator's compact log fields and one-entry-per-revision rule are mandatory.

## Concurrency and ownership

- Concurrent tasks on different projects are expected. Each task owns only its declared project paths and controls.
- Do not edit another project's source or status without a material dependency reason and explicit integration ownership.
- Same-project parallel work requires explicitly disjoint ownership or one integration owner.
- Do not rebuild or re-audit unrelated projects merely because `main` advanced.
- Shared infrastructure and runtime deployment state require an explicitly designated owner.
- Retire temporary branches/worktrees after their checkpoint lands when appropriate; do not maintain permanent project branches.

## Evidence and proportionality

- Compilation, static analysis, fixtures, generated assets, GameTests, and JAR production are not Minecraft runtime validation.
- Record only runtime behavior actually observed, using the schema's independent build, deployment, and runtime classifications.
- Preserve original behavior unless a change is requested. Prefer narrow compatibility fixes to broad rewrites.
- Match investigation, documentation, testing, and build effort to the task's risk. Tiny follow-ups do not justify unrelated repository-wide audits.

## Runtime model

The physical runtime profile is an accepted baseline plus exactly two independent experimental slots, A and B. Each occupied slot has its own project UUID, version, artifact identity, source checkpoint, deployment state, and independent `UNTESTED`, `PASS`, `FAIL`, or `INCONCLUSIVE` result. Never collapse two slot results into one aggregate result.

### External user runtime evidence

- An explicit user-reported `PASS`, `FAIL`, or `INCONCLUSIVE` may bind to a current canonical candidate without a Test Instance Manager deployment only when current authoritative `main` uniquely identifies the exact release version, filename, SHA-256, and source checkpoint. Ambiguous or drifted identity fails closed.
- Record such evidence in the project's `WORKBENCH_STATUS.json` and `CODEX_LOG.md` explicitly as user-reported/external runtime evidence. Record only the classification and observations the user actually supplied; never invent row-level observations. Keep deployment truthful (`NOT_DEPLOYED` when no managed deployment exists), and do not use lifecycle `TESTING` unless the project UUID currently occupies a canonical test slot.
- Recording external evidence alone makes no Test Instance Manager transition; `FAIL` and `INCONCLUSIVE` therefore never consume a slot. To promote an externally tested `PASS`, use `PROMOTE_USER_PASSED_BATCH`; it preserves both slots and may either add the project or replace its accepted predecessor only through that predecessor's exact `replaces_accepted_deployment_id`.
- When a passing successor replaces an accepted release, preserve the predecessor as rollback/provenance in the project controls where applicable. This does not change the manager's target-local `retained_rollbacks` semantics: target-local retention remains explicit and independently validated.
- The existing managed-slot assignment, readiness, result-recording, and promotion workflow remains unchanged.

### `TESTING` lifecycle invariant

- `TESTING` means one thing only: the project's immutable UUID currently occupies Test Slot A or Test Slot B in canonical `tools/test_instance_manager/runtime-state.json` after a verified serialized Test Instance Manager deployment transition.
- A build or static pass, retained Canary, useful `TESTING.md`, readiness for runtime testing, queued next step, or wait for a free slot never implies `TESTING`. A ready candidate outside both slots normally remains `ACTIVE` unless another lifecycle is independently appropriate.
- Slot runtime result is independent of lifecycle. An occupied project remains `TESTING` while its result is `UNTESTED`, `PASS`, `FAIL`, or `INCONCLUSIVE`.
- Assigning a project UUID to either slot must set its lifecycle to `TESTING`. Removing it from its last occupied slot must set the appropriate resulting lifecycle, normally `ACTIVE` for an unaccepted development candidate; promotion/removal must derive lifecycle from the remaining current state.
- Accepted-release provenance is independent of current candidate occupancy. A project may have an accepted release and lifecycle `TESTING` when a newer/current candidate for the same UUID occupies a test slot.

Only one Test Instance Manager operation may mutate the dedicated Workbench at a time. Transitions must be serialized and atomic; promoting/removing one slot must preserve the other. Concurrent development, builds, and static validation remain allowed.

- Dedicated test instance: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha Flavoured 26.2 Workbench`
- Protected gameplay instance: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha Flavoured 26.1.2`

The protected gameplay instance is permanently off-limits. A runtime task must explicitly own deployment before touching the dedicated instance.

## Sheet synchronization

Mynx uses a separate Google Sheet as a human-facing mirror keyed by project UUID; `main` remains authoritative. No legacy-row reconciliation or manual revision seeding is required: an unknown UUID is inserted with its full canonical record at any positive authoritative repository revision; an existing UUID rejects lower revisions as stale, acknowledges an identical or semantically idempotent same-revision replay, rejects conflicting same-revision content, and accepts any higher authoritative revision without requiring the Sheet to have observed intermediate revisions. `Notes` is the sole human-owned Sheet field and must be preserved; `Priority` is not part of the contract. Feature branches may validate or preview but may not publish authoritative updates. Live publication to the Mynx Sheet is enabled and remains fail-closed behind the tracked publication gate, exact repository/ref authority, authorized cutover variable, production environment, receiver URL/HMAC secret, receiver write gate, signature validation, and UUID/revision rules. The separate legacy writer may continue targeting its different legacy Sheet. A normal completed project task must publish its distinct revision from `main`, even when only hidden synchronization metadata changed; a missed delivery is repaired by reconciling current authoritative `main`, never by manufacturing project revisions.

## Safety and licensing

- `originals/` is an optional local, Git-ignored reference library. It may contain any useful pristine/reference material, not only dependencies currently required by a project, and the user may populate it manually as needed.
- Once a reference file is placed in `originals/`, treat it as immutable. Never edit or overwrite it in place; if another upstream or version is needed, add it as a distinct file.
- Never commit or otherwise track any `originals/` content merely because it exists locally.
- Do not import or redistribute private, closed-source, or ARR code/assets without permission. Such materials remain subject to redistribution restrictions; retain only lawful, necessary artifacts and provenance outside this local library.
- Projects may inspect these local references for porting, compatibility, provenance, or verification.
- Do not import legacy Git history, deployment history, snapshots, or stale task state.
- Do not infer source/material equivalence: preserve meaningful variants and exact ownership mappings.
- Never discard, overwrite, rebase away, or force-push unrelated user or concurrent work.

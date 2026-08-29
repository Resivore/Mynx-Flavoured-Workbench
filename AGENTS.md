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

Only one Test Instance Manager operation may mutate the dedicated Workbench at a time. Transitions must be serialized and atomic; promoting/removing one slot must preserve the other. Concurrent development, builds, and static validation remain allowed.

- Dedicated test instance: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha Flavoured 26.2 Workbench`
- Protected gameplay instance: `C:\Users\resiv\AppData\Roaming\ModrinthApp\profiles\Matcha Flavoured 26.1.2`

The protected gameplay instance is permanently off-limits. A runtime task must explicitly own deployment before touching the dedicated instance.

## Sheet synchronization

Mynx uses a separate Google Sheet as a human-facing mirror keyed by project UUID; `main` remains authoritative. No legacy-row reconciliation or manual revision seeding is required: an unknown UUID is inserted with its full canonical record at any positive authoritative repository revision, while an existing UUID accepts only an idempotent replay or the exact next revision and rejects conflicts, stale revisions, and gaps. `Notes` is the sole human-owned Sheet field and must be preserved; `Priority` is not part of the contract. Feature branches may validate or preview but may not publish authoritative updates. Live publication to the Mynx Sheet is enabled and remains fail-closed behind the tracked publication gate, exact repository/ref authority, authorized cutover variable, production environment, receiver URL/HMAC secret, receiver write gate, signature validation, and UUID/revision rules. The separate legacy writer may continue targeting its different legacy Sheet. A normal completed project task must publish its distinct revision from `main`, even when only hidden synchronization metadata changed.

## Safety and licensing

- Never modify `originals/`; treat any present contents as intentional pristine/reference inputs.
- Do not import or redistribute closed-source or ARR code/assets without permission. Retain only lawful, necessary artifacts and provenance.
- Do not import legacy Git history, deployment history, snapshots, or stale task state.
- Do not infer source/material equivalence: preserve meaningful variants and exact ownership mappings.
- Never discard, overwrite, rebase away, or force-push unrelated user or concurrent work.

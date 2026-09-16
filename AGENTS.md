# Mynx Flavoured Workbench operating policy

## Authority and completion

- This repository targets Minecraft Java 26.2 on Fabric.
- `main` is the sole durable authority. Project branches and worktrees are temporary task surfaces, never alternate project authorities.
- Start ordinary work from reasonably current `main`, reconcile with newer `main` when necessary, and integrate coherent checkpoints frequently.
- Before integrating a candidate into `main`, run `python tools/workbench.py validate-repository --root .` against that candidate; do not integrate when it fails. This is the same repository gate used by `Validate Workbench` and does not replace verifying the resulting GitHub Actions run.
- A project task is complete only after its checkpoint is integrated into `main`, its `WORKBENCH_STATUS.json` revision and `CODEX_LOG.md` entry are updated, and the main-only Sheet publisher has handled the new revision. Live publication is delegated after the push to the GitHub Actions `Reconcile Project Status Sheet` workflow; local development/Codex/Work hosts never require, receive, or inspect its production credentials. When GitHub Actions can be inspected, report a successful publish step as Sheet publication success, a running workflow as pending verification, and a failed or skipped workflow with its actual GitHub Actions reason. Report publication as unverifiable only when GitHub Actions itself cannot be inspected. Missing local receiver URL/HMAC/cutover values are neither a publication failure nor a project blocker.
- When a project task finalizes a current artifact that will remain ignored and untracked, completion also requires `python -B tools/artifact_retention.py --root . projects/<project>` (or the corresponding `resourcepacks/<project>` path) to verify the manifest filename and SHA-256, atomically retain the exact bytes at the same artifact path in the primary local checkout derived from Git's common directory, refuse to overwrite a conflicting copy, and verify the destination hash and complete bytes without tracking or redistributing it. Any retention failure fails the task.
- Commit and push durable progress. Current `main` must be reconstructable without conversation history, stale task branches, or local-only state.
- The local `WORKBENCH_DASHBOARD.html` is a generated convenience view, never an authority or project-completion gate. Dashboard generation or browser opening must not block a project commit, integration, or completion; ordinary project tasks do not wait for it; and regeneration never creates or advances a project revision. The separate `WORKBENCH_SERVER_STATE.json` is human-owned real-server state keyed by project UUID and is not testing/deployment evidence or Sheet synchronization metadata.

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
- Retire temporary branches/worktrees after their checkpoint lands when appropriate; never retire a project task worktree before required canonical local artifact retention succeeds, and do not maintain permanent project branches.

## Evidence and proportionality

- Compilation, static analysis, fixtures, generated assets, GameTests, and JAR production are not Minecraft runtime validation.
- Record only runtime behavior actually observed, using the schema's independent build, deployment, and runtime classifications.
- Preserve original behavior unless a change is requested. Prefer narrow compatibility fixes to broad rewrites.
- Match investigation, documentation, testing, and build effort to the task's risk. Tiny follow-ups do not justify unrelated repository-wide audits.

## Runtime dependency policy

- Preserve release and evidence identity exactly: version, filename, SHA-256, source commit, deployment UUID, and accepted-predecessor identity are independent of dependency eligibility.
- Runtime metadata normally depends on a stable Fabric mod ID, `provides` alias, or other capability without pinning the exact version used during development. Exact Gradle, build, fixture, and test inputs may remain pinned for reproducibility, but record them separately as validation baselines in the project log or structured validation metadata; they are not runtime requirements by default.
- Minecraft may remain exactly targeted to `26.2`. Fabric Loader, Fabric API, Java, and other runtime providers may declare a genuine minimum floor. Do not place exact Canary versions, semantic build metadata, arbitrary version-family ceilings, or speculative upper bounds in `depends`, `recommends`, or `suggests` merely because those versions were tested.
- An exact pin or upper bound outside the Minecraft exception is allowed only for a demonstrated incompatibility. The current release's `runtime_dependency_policy.exceptions` must bind the exact consumer ID, relationship, dependency ID, and predicate, and must record both the reason and concrete regression evidence.
- Every new or changed current artifact must carry the release-scoped `runtime_dependency_policy` attestation. An unchanged release without the field is grandfathered; promotion or evidence updates do not require repackaging, and the next real successor must adopt the policy.
- Stable provider aliases are compatibility contracts. A compatible future provider must not be rejected only because its version is newer, and replacing a standalone provider with a unified JAR remains valid when the unified JAR declares the stable alias in `provides`.

## Runtime model

Runtime validation is user-directed and independent for every project. Codex must not inspect, track, reserve, populate, clear, or swap any Minecraft testing profile as part of normal project work. A deployment value in a historical status record is evidence only; it is not an active profile-control requirement.

### External user runtime evidence

- An explicit user-reported `PASS`, `FAIL`, or `INCONCLUSIVE` may bind to a current canonical candidate when authoritative `main` uniquely identifies the exact release version, filename, SHA-256, and source checkpoint. Ambiguous or drifted identity fails closed.
- Record such evidence in the project's `WORKBENCH_STATUS.json` and `CODEX_LOG.md` explicitly as user-reported/external runtime evidence. Record only the classification and observations the user actually supplied; never invent row-level observations. Keep historical deployment values truthful, but do not use them as a lifecycle prerequisite.
- When a passing successor replaces an accepted release, preserve the predecessor as rollback/provenance in the project controls where applicable.

### `TESTING` lifecycle invariant

- `TESTING` means the project is awaiting, undergoing, or receiving user runtime validation. It does not imply a profile, slot, deployment, capacity reservation, or any physical filesystem action.
- A build or static pass, retained Canary, useful `TESTING.md`, readiness for runtime testing, or a runtime result does not by itself require a lifecycle change; record the state that accurately reflects the owner's validation process.
- Runtime results remain independent of lifecycle. An accepted release may have historical untested or partial evidence, and a successor may remain `TESTING` while its validation is pending.

The protected Matcha Flavoured 26.1.2 gameplay instance remains permanently off-limits. The retired Matcha Flavoured 26.2 Workbench profile must not be inspected or manipulated by Codex.

## Sheet synchronization

Mynx uses a separate Google Sheet as a human-facing mirror keyed by project UUID; `main` remains authoritative. No legacy-row reconciliation or manual revision seeding is required: an unknown UUID is inserted with its full canonical record at any positive authoritative repository revision; an existing UUID rejects lower revisions as stale, acknowledges an identical or semantically idempotent same-revision replay, rejects conflicting same-revision content, and accepts any higher authoritative revision without requiring the Sheet to have observed intermediate revisions. `Notes` is the sole human-owned Sheet field and must be preserved; `Priority` is not part of the contract. Feature branches may validate or preview but may not publish authoritative updates. Live publication runs only in the GitHub Actions `Reconcile Project Status Sheet` workflow, fail-closed behind the tracked publication gate, exact repository/ref authority, authorized cutover variable, `sheet-production` environment, receiver URL/HMAC secret, receiver write gate, signature validation, and UUID/revision rules. Those production values belong only to that environment, never to local development hosts or repository files. The separate legacy writer may continue targeting its different legacy Sheet. A normal completed project task delegates its distinct `main` revision to that workflow, even when only hidden synchronization metadata changed; a missed delivery is repaired by reconciling current authoritative `main`, never by manufacturing project revisions.

## Safety and licensing

- `originals/` is an optional local, Git-ignored reference library. It may contain any useful pristine/reference material, not only dependencies currently required by a project, and the user may populate it manually as needed.
- Once a reference file is placed in `originals/`, treat it as immutable. Never edit or overwrite it in place; if another upstream or version is needed, add it as a distinct file.
- Never commit or otherwise track any `originals/` content merely because it exists locally.
- Do not import or redistribute private, closed-source, or ARR code/assets without permission. Such materials remain subject to redistribution restrictions; retain only lawful, necessary artifacts and provenance outside this local library.
- Projects may inspect these local references for porting, compatibility, provenance, or verification.
- Do not import legacy Git history, deployment history, snapshots, or stale task state.
- Do not infer source/material equivalence: preserve meaningful variants and exact ownership mappings.
- Never discard, overwrite, rebase away, or force-push unrelated user or concurrent work.

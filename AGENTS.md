# Mynx Flavoured Workbench operating policy

## Authority and completion

- This repository targets Minecraft Java 26.2 on Fabric. `main` is the sole durable authority; branches and worktrees are temporary task surfaces.
- Start ordinary work from reasonably current `main`, preserve concurrent and unrelated work, run `python tools/workbench.py validate-repository --root .` before integrating, and commit/push coherent checkpoints to `main`.
- A project task is complete when canonical work is committed, validated, integrated and pushed to `main`, and required local artifact retention succeeds. Update `WORKBENCH_STATUS.json` and append `CODEX_LOG.md` for real project work; do not manufacture project revisions or log entries for shared protocol/display migrations.
- Current ignored artifacts require `python -B tools/artifact_retention.py --root . projects/<project>` (or the resource-pack equivalent). Preserve exact filename and SHA-256; retention failure fails the task.
- `WORKBENCH_DASHBOARD.html` is the human-facing generated convenience view, not an authority or completion gate. When a task finalizes a new current artifact and canonical status, regenerate it as a best-effort final step. Generation/opening failure never blocks completion or advances a revision; `--open` regenerates from local `main` first. The public GitHub Pages dashboard is an automatically generated convenience view of authoritative `main`, not a project completion gate: ordinary work never waits for, polls, retries, or reconciles Pages. A failed/outage deployment leaves `main` correct; the next `main` push or an explicit manual workflow rerun is sufficient recovery.
- `WORKBENCH_SERVER_STATE.json` is the separate human-owned UUID-keyed record of actual real-server release identity. It is not runtime-test evidence and is never altered just because a new Canary is produced.

## Project contract

Every official project or resource pack has one immutable UUID and exactly these controls:

- `WORKBENCH_STATUS.json` — canonical identity, scope, state, evidence, and artifact provenance;
- `TESTING.md` — current useful manual/runtime verification procedure;
- `CODEX_LOG.md` — compact append-only task history.

For real project work, advance the manifest revision exactly once, update status metadata, append one log entry, and preserve exact Canary/version, filename, SHA-256, finalized `built_at` timestamp, and source-checkpoint identity. New current artifacts need a concise one-sentence release summary for the dashboard. Record `built_at` from the exact finalized, SHA-256-verified bytes, never a task-start approximation. `source_commit` is the preceding coherent implementation/artifact checkpoint, never the commit containing its own hash.

## Concurrency, evidence, and runtime

- Each task owns its declared project paths. Do not edit another project without a material dependency and explicit integration ownership. Prefer narrow compatibility fixes to rewrites.
- Build, static analysis, fixtures, GameTests, and JAR production are not Minecraft runtime validation. Record only runtime behavior actually observed.
- Lifecycle values are `PLANNED`, `ACTIVE`, `BLOCKED`, `ACCEPTED`, and `PARKED`. Runtime testing is user-directed evidence, not a lifecycle transition: an ACTIVE project may await, pass, fail, or partially complete runtime testing while it remains ACTIVE. Acceptance is an explicit owner decision.
- `TESTING.md` is lifecycle-neutral; its existence, completeness, or use never changes lifecycle.
- User-reported runtime evidence binds only when authoritative `main` uniquely identifies the exact release identity. Record only the observations actually supplied and preserve rollback/provenance when appropriate.
- Do not inspect, track, reserve, populate, clear, or swap any Minecraft testing profile. The protected Matcha Flavoured 26.1.2 gameplay instance and retired Matcha Flavoured 26.2 Workbench profile are permanently off-limits.

## Dependencies, artifacts, and safety

- Preserve the runtime dependency policy: depend on stable Fabric capabilities/providers by default; exact pins or upper bounds need concrete incompatibility evidence.
- Keep release identity, evidence identity, and dependency eligibility independent. Every changed current artifact retains its runtime-dependency-policy attestation; unchanged historical bytes are grandfathered until a real successor.
- `originals/` is Git-ignored, immutable once placed, and never committed. Do not import or redistribute private, closed-source, or ARR material without permission.
- Do not import legacy Git/deployment history, infer material equivalence, or create routine duplicate status/provenance documents.
- Do not discard, overwrite, rebase away, force-push, or otherwise disturb unrelated user/concurrent work.
- Other than the generated GitHub Pages convenience view, no external project-status publication, reconciliation, recovery, credentials, or completion condition exists in this workflow.

# Mynx Flavoured Workbench

Mynx Flavoured Workbench is the authoritative Minecraft Java 26.2 Fabric workspace for Mynx-flavoured modding, compatibility, resource-pack integration, and private validation.

`main` is the only durable source of truth. Bounded work happens on temporary branches or worktrees, then a coherent checkpoint is reconciled and integrated into `main`. Different projects may be developed and statically validated concurrently without sharing project ownership.

After a successful integration, reconcile the normal primary checkout as part of completion: if it is clean, fast-forward it to current `origin/main`; if it is dirty, first classify and preserve any legitimate work in an appropriate branch, worktree, or local recovery location. Do not leave the primary checkout on a task branch or silently behind `main`; confirm it is clean, current, and contains any newly tracked artifacts before reporting reconciliation complete.

Every official project—including one whose lifecycle is only `PLANNED`—has an immutable UUID and a directory under `projects/` or `resourcepacks/`. Its canonical controls are:

- `WORKBENCH_STATUS.json` for structured identity, scope, current state, evidence, artifact provenance, and synchronization;
- `TESTING.md` for the current useful runtime procedure;
- `CODEX_LOG.md` for concise append-only task history.

`TESTING` is a supported lifecycle for projects awaiting, undergoing, or receiving user runtime validation. It is independent of any Minecraft profile, testing slot, deployment, or capacity reservation; Codex does not inspect or manipulate a dedicated testing Workbench.

## Local dashboard

Generate the self-contained local project view with `python tools/workbench_dashboard.py`, or regenerate it and open it in the default browser with `python tools/workbench_dashboard.py --open`. The ignored output is `WORKBENCH_DASHBOARD.html`; it embeds the current canonical data and needs no server or network connection after generation.

Real-server implementation state is a separate human-owned deployed-release record stored by immutable project UUID in `WORKBENCH_SERVER_STATE.json`. A missing record means `NOT DEPLOYED`; a record matching the canonical current release and artifact is `CURRENT`; any differing recorded release/artifact is automatically `OUTDATED`. Accepted projects are treated as current from their canonical accepted release without redundant records. Set a project to its exact current release with `python tools/workbench_dashboard.py server <project-id-or-uuid> current` (the compatibility alias `yes` also works), or clear its deployment record with `no`; either command regenerates the dashboard. Project IDs, names, and aliases are lookup conveniences only—the durable key remains the UUID. This record is independent of Workbench validation/deployment evidence and Google Sheet synchronization metadata.

`main` and the canonical project controls remain authoritative. Dashboard generation is only a convenience/view operation: a generation or browser-opening problem must not prevent a project from being committed, integrated, or completed; normal project tasks do not wait for the dashboard; and regenerating it never creates or advances a project revision. The existing Google Sheet protocol remains in place unless it is retired by a separate explicit decision.

This repository begins with clean history. [Minecraft-26.2-Workbench](https://github.com/Resivore/Minecraft-26.2-Workbench) remains the historical archive and frozen import source; its Git and deployment history are intentionally not reproduced here. See `MIGRATION_FREEZE.md` for exact source checkpoints.

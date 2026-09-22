# Mynx Flavoured Workbench

Mynx Flavoured Workbench is the authoritative Minecraft Java 26.2 Fabric workspace for Mynx-flavoured modding, compatibility, resource-pack integration, and private validation.

`main` is the only durable source of truth. Bounded work happens on temporary branches or worktrees, then a coherent checkpoint is reconciled and integrated into `main`. Different projects may be developed and statically validated concurrently without sharing project ownership.

After a successful integration, reconcile the normal primary checkout as part of completion: if it is clean, fast-forward it to current `origin/main`; if it is dirty, first classify and preserve any legitimate work in an appropriate branch, worktree, or local recovery location. Do not leave the primary checkout on a task branch or silently behind `main`; confirm it is clean, current, and contains any newly tracked artifacts before reporting reconciliation complete.

Every official project—including one whose lifecycle is only `PLANNED`—has an immutable UUID and a directory under `projects/` or `resourcepacks/`. Its canonical controls are:

- `WORKBENCH_STATUS.json` for structured identity, scope, current state, evidence, and artifact provenance;
- `TESTING.md` for the current useful runtime procedure;
- `CODEX_LOG.md` for concise append-only task history.

The lifecycle vocabulary is `PLANNED`, `ACTIVE`, `BLOCKED`, `ACCEPTED`, and `PARKED`. Runtime testing is user-directed evidence, not a lifecycle transition: an ACTIVE project may await, pass, fail, or partially complete runtime testing while it remains ACTIVE. `TESTING.md` is lifecycle-neutral. Codex never inspects or manipulates the retired 26.2 Workbench testing profile.

The protected Matcha Flavoured 26.1.2 gameplay instance is permanently off-limits.

## Dashboard

Generate the self-contained local project view with `python tools/workbench_dashboard.py`, or regenerate it and open it in the default browser with `python tools/workbench_dashboard.py --open`. The ignored output is `WORKBENCH_DASHBOARD.html`; it embeds the current canonical data and needs no server or network connection after generation.

Every finalized current release records its public Workbench Canary as an explicit positive integer in `WORKBENCH_STATUS.json`. Dashboard version labels come only from that field and are always rendered as compact `C<number>` values. Internal versions, embedded mod versions, filenames, hashes, project revisions, and development suffixes remain exact provenance data but never determine the displayed Canary. A genuinely new current release advances exactly once; unchanged bytes and lifecycle, acceptance, evidence, or administrative updates retain the existing ordinal.

Real-server implementation state is a separate human-owned deployed-release record stored by immutable project UUID in `WORKBENCH_SERVER_STATE.json`. A missing record means `NOT DEPLOYED`; a record matching the canonical current release and artifact is `CURRENT`; any differing recorded release/artifact is automatically `OUTDATED`. Set a project to its exact current release with `python tools/workbench_dashboard.py server <project-id-or-uuid> current` (the compatibility alias `yes` also works), or clear its deployment record with `no`; either command regenerates the dashboard. Project IDs, names, and aliases are lookup conveniences only—the durable key remains the UUID.

Each push to authoritative `main` also generates and publishes only the self-contained dashboard HTML through GitHub Pages. The repository remains private; the generated Pages site is intentionally public. Its subtle masthead source SHA identifies the exact deployed `main` commit, so a failed deployment is visible as a temporarily older dashboard rather than a different authority.

`main` and the canonical project controls remain authoritative. Both local and Pages dashboard generation are only convenience/view operations: when artifact work finalizes a new current release, regenerate it when practical; a generation or browser-opening problem must not prevent the project from being committed, integrated, or completed; and regenerating it never creates or advances a project revision. Pages is not a completion gate: ordinary tasks do not wait for, poll, retry, or reconcile deployments. If deployment N fails, `main` remains correct and the public site may show N-1 until the next successful `main` deployment or an explicit manual rerun replaces the site wholesale. No reconciliation database or special recovery state exists.

For a new or changed current JAR, record `built_at` as the RFC 3339 UTC timestamp from the exact finalized and SHA-256-verified bytes. It is durable dashboard/provenance metadata, not release/artifact identity or server deployment state; historical unchanged artifacts may omit it until exact timestamp evidence is available.

This repository begins with clean history. [Minecraft-26.2-Workbench](https://github.com/Resivore/Minecraft-26.2-Workbench) remains the historical archive and frozen import source; its Git and deployment history are intentionally not reproduced here. See `MIGRATION_FREEZE.md` for exact source checkpoints.

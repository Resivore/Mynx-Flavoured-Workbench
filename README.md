# Mynx Flavoured Workbench

Mynx Flavoured Workbench is the authoritative Minecraft Java 26.2 Fabric workspace for Mynx-flavoured modding, compatibility, resource-pack integration, and private validation.

`main` is the only durable source of truth. Bounded work happens on temporary branches or worktrees, then a coherent checkpoint is reconciled and integrated into `main`. Different projects may be developed and statically validated concurrently without sharing project ownership.

Every official project—including one whose lifecycle is only `PLANNED`—has an immutable UUID and a directory under `projects/` or `resourcepacks/`. Its canonical controls are:

- `WORKBENCH_STATUS.json` for structured identity, scope, current state, evidence, artifact provenance, and synchronization;
- `TESTING.md` for the current useful runtime procedure;
- `CODEX_LOG.md` for concise append-only task history.

Runtime testing uses one accepted baseline plus two independent experimental slots, A and B. Physical deployment to the one dedicated Workbench is serialized, while each slot retains its own project identity and result.

This repository begins with clean history. [Minecraft-26.2-Workbench](https://github.com/Resivore/Minecraft-26.2-Workbench) remains the historical archive and frozen import source; its Git and deployment history are intentionally not reproduced here. See `MIGRATION_FREEZE.md` for exact source checkpoints.

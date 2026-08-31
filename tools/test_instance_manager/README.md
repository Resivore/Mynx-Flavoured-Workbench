# Test Instance Manager V2

The V2 manager is the physical adapter for `runtime-state.json`. It owns only
the configured dedicated Minecraft 26.2 Workbench `mods` directory, its
target-local V2 lock/ledger, and the repository state commit paired with a
verified physical transition. The protected Minecraft 26.1.2 profile is
permanently rejected before access.

The tracked runtime contract contains the exact frozen accepted baseline (23
project units / 26 artifacts), fixed independent Slots A and B, and exact
per-slot deployment/runtime evidence. The current accepted composition is
human-readable **Stack v1**: `accepted_baseline.revision` is the sole canonical
Stack number, so no parallel stack list or hand-maintained title value exists.
Those JSON values are expected-state metadata, not physical evidence and not
Minecraft runtime validation.

A genuine `PROMOTE_SLOT` addition or artifact replacement and an intentional
`REMOVE_ACCEPTED` each increment the Stack number exactly once. A promotion of
a byte-identical rebuild is a reconciliation: it clears the test slot while
preserving the exact accepted member/artifact identity and Stack number.
Assignments, candidate replacements, slot clearing, readiness/result recording,
verification, and display refreshes never increment the Stack number.

`PROMOTE_UNTESTED_CANDIDATE` is the narrow exception for an explicitly
user-approved successor that must enter the accepted baseline without claiming
runtime evidence or occupying a test slot. It requires the exact authorization
token `USER_APPROVED_UNTESTED_PROMOTION`, can only replace the same already
accepted project, clears an existing same-project slot only while its result is
still `UNTESTED`, and increments the Stack number for changed bytes. The
project's status and append-only log remain responsible for preserving the
user authorization and the distinction from a runtime pass.
Because that authorization is not derivable from a desired-state document,
this exception must use the operation-based `transition --operation` path;
prevalidated `desired_state` callers cannot synthesize the authorization.

`PROMOTE_USER_PASSED_BATCH` is the serialized path for one or more exact
candidates whose runtime pass was reported directly by the user. It requires
the exact `USER_REPORTED_EXACT_RUNTIME_PASS` authorization, preserves both test
slots byte-for-byte, adds every member in one runtime-state revision, and
increments the Stack number once per newly accepted project. The production
CLI binds every candidate's project UUID/ID, version, filename, SHA-256, and
source checkpoint to that project's current manifest release. Candidate bytes
must come from the canonical tracked `projects/<project>/artifacts/<filename>`
(or equivalent canonical project directory) path. If a batch member carries a
retained rollback, it must match that manifest's rollback release exactly; a
project with no manifest rollback cannot declare one.

A retained rollback is target-local history, not an active baseline artifact.
Its unit uses the same project UUID/ID, a `FROZEN_LEGACY` identity, and an exact
`ADOPTED_TARGET mods/<filename>` source. The manager stages and verifies those
enabled predecessor bytes, includes their removal in the same locked rollback
transaction, and writes the exact bytes to `mods/<filename>.disabled`. All
later verification requires that disabled file and rejects any reappearance of
its enabled sibling. Verification receipts expose retained rollback paths,
hashes, disabled dispositions, inventory digest, and the enabled-sibling
absence result.

## Migration closure

The initial physical V2 migration completed at checkpoint
`d0479458c9ac3c49db825be7a25746f554976922` and was reverified on authoritative
`main` at `a5b29a421c503c959d820ecb05d92126c7ddad13`. That historical adoption is not
proof of a later slot revision. A tracked `runtime-state.json`, its target-local
ledger, a manager plan, a receipt from another revision, or a bootstrap test may
describe intended state but may not be reported as current physical proof.

Only a successful live manager scan against the configured dedicated Workbench
may emit `PHYSICAL_STATE_VERIFIED`: the normal `verify` command, or the same
mandatory scan embedded before and after legacy-marker retirement. It
independently checks every managed physical path, active/disabled disposition,
SHA-256, Fabric ownership, and absence of superseded enabled artifacts. Its
receipt names the exact slot projects, versions, physical artifact paths,
hashes and dispositions, plus accepted-baseline counts and deterministic
managed-inventory digests.

## Canonical title projection

The existing client-only Workbench Test Marker is manager-owned infrastructure,
not an accepted-baseline member. Version `0.2.0` reads only
`.mynx-runtime-v2-title.json`. Every applied manager transition atomically
regenerates that projection from the same V2 runtime state and canonical
manifest names, installs/verifies the pinned marker JAR, and records both in the
target-local V3 manager ledger. The projection contains exactly these visible
identity lines:

```text
Baseline: Stack vN
Slot A: <canonical project name> - Canary <number>  (or Slot A: Empty)
Slot B: <canonical project name> - Canary <number>  (or Slot B: Empty)
```

Canary numbers derive from each slot unit's canonical `version`, never an
artifact filename. Empty slots never render `UNKNOWN`. A legacy V2 ledger plus
the exact pinned `workbench-test-marker-0.1.1.jar` is accepted only as the
one-step migration preimage; the next successful manager transition replaces
it with the pinned `0.2.0` JAR, writes the projection, and advances the
target-local ledger to V3 without changing accepted membership or Stack number.

Google Sheet mirroring is not an activation or physical-verification authority.
Its pending R2 publication failure is recorded separately in the Sheet
synchronization infrastructure record and does not change this migration's
successful lifecycle or physical state.

## Normal commands

Run from the repository root:

```powershell
python -B tools/test_instance_manager/manager.py verify
python -B tools/test_instance_manager/manager.py retire-legacy-marker
python -B tools/test_instance_manager/manager.py retire-legacy-marker --apply
python -B tools/test_instance_manager/manager.py transition --operation <operation.json> --expected-revision <revision> --at <RFC3339-UTC>
python -B tools/test_instance_manager/manager.py transition --operation <operation.json> --expected-revision <revision> --at <RFC3339-UTC> --apply
```

`transition` is a dry-run unless `--apply` is present. Inspect the complete
write/removal plan first. An apply recomputes the plan while holding the
target-local exclusive lock, stages and hashes every addition, verifies the
result, commits the target ledger and repository state together, and restores
the preimage if any step fails.

The V1 `.workbench-instance-manager.json` file is legacy display metadata, not
V2 state authority. The 0.2.0 title marker never reads either its active or
retired filename. Normal verification and transitions fail closed while that
active filename exists. `retire-legacy-marker` is dry-run by default: under the
same target lock it first verifies the current repository, V2 ledger, and
physical managed inventory, then reports the exact marker hash. `--apply`
atomically renames it to `.workbench-instance-manager.v1-retired.json`, or
removes the active duplicate only when an existing retired copy is byte-exact.
Differing retired bytes are never overwritten. Marker retirement does not
change `mods`, `runtime-state.json`, or the V2 ledger.

`adopt` was the one-time migration path from populated `GATED` state. It
required a zero-operation physical plan and exact verification before writing
the V2 ledger and activating state; it now fails closed because the ledger
already exists.

## Invariants

- Artifacts use exact filenames, SHA-256 values, Fabric `mod:<id>` ownership,
  and repository or explicitly adopted-target sources.
- Accepted artifacts replaced by a slot remain present as `.jar.disabled`.
- User-passed batch promotion preserves both slots and atomically converts each
  declared adopted predecessor into an exact manager-owned `.jar.disabled`
  retained rollback; a wrong or missing predecessor fails before mutation.
- A slot may declare `dependency_overrides` for exact accepted dependency
  deployments that it temporarily supersedes. The slot artifact set must cover
  every ownership key of each overridden dependency; removing the slot restores
  those accepted bytes, and a normal slot promotion cannot absorb the temporary
  dependency override into the accepted project.
- Slots are independent: updating, clearing, or promoting one cannot move or
  rewrite the other.
- Unmanaged enabled JARs that provide managed Fabric IDs are rejected;
  intentional legacy-disabled fallbacks are preserved.
- Symlinks, junctions/reparse points, path escapes, stale revisions/digests,
  foreign locks, missing adopted-only bytes, and incomplete transactions fail
  closed.
- Any crash residue named `.mynx-runtime-v2-transaction-*` blocks verification
  and mutation until its backups are explicitly inspected and recovered; the
  manager never guesses that a partial transaction is safe to discard.
- A live V1 display marker blocks normal V2 verification and transition until
  its exact bytes are retired through the serialized retirement command.
- Private/non-redistributable adopted artifacts remain target-local; the
  repository stores only their exact identity and path descriptors.

# Test Instance Manager V2

The V2 manager is the physical adapter for `runtime-state.json`. It owns only
the configured dedicated Minecraft 26.2 Workbench `mods` directory, its
target-local V2 lock/ledger, and the repository state commit paired with a
verified physical transition. The protected Minecraft 26.1.2 profile is
permanently rejected before access.

The tracked runtime contract contains the exact frozen accepted baseline (23
project units / 26 artifacts), fixed independent Slots A and B, and exact
shared slot deployment evidence plus independent per-member runtime evidence.
An occupied slot is a cohort of one or more current-manifest project units; a
one-member cohort is the lossless successor to the former one-project slot.
The current accepted composition is
human-readable **Stack v1**: `accepted_baseline.revision` is the sole canonical
Stack number, so no parallel stack list or hand-maintained title value exists.
Those JSON values are expected-state metadata, not physical evidence and not
Minecraft runtime validation.

A genuine `PROMOTE_SLOT` addition or artifact replacement and an intentional
`REMOVE_ACCEPTED` each increment the Stack number exactly once. A managed-slot
promotion of a byte-identical rebuild is a reconciliation: it clears the test
slot while preserving the exact accepted member/artifact identity and Stack
number.
Assignments, candidate replacements, slot clearing, readiness/result recording,
verification, and display refreshes never increment the Stack number.

## Atomic slot cohorts and Fabric dependencies

Schema V2 represents an occupied slot as `members[]` plus one shared
`deployment` record. Every member retains its project/deployment/artifact UUIDs,
version, source checkpoint, accepted-predecessor replacement, optional accepted
dependency overrides, and independent runtime result. Slot assignment,
replacement, clearing, staging, filesystem writes, rollback, ledger/state
commit, and title projection are atomic for the full cohort. Slot A and Slot B
remain independent.

`DEPLOY_PROFILE` is the physical-manager-only whole-profile operation. Its
`slots` object declares both A and B, with each occupied slot supplying either a
legacy `candidate` or a nonempty `members` array. It deterministically migrates
legacy schema V1 state, plans one next revision through `SET_PROFILE`, stages
every cohort artifact, verifies the proposed dependency graph, applies the
entire filesystem transaction, and verifies hashes and the physical enabled
graph. Only then does the in-process physical-manager authority stamp every new
occupied slot `READY_TO_TEST_VERIFIED` in that same next revision. No public
serialized state operation can manufacture that readiness evidence. Any
failure restores every cohort member, both slot states, the title projection,
ledger, and repository state to the exact preimage.

Every `DEPLOY_PROFILE` member is bound before staging to its authoritative
current manifest UUID/ID, source checkpoint, artifact filename, and SHA-256.
Because the operation admits exactly one current-release artifact per member,
the member's canonical runtime `version` must also equal that JAR's root
`fabric.mod.json` version. Preflight rejects a mismatch before mutation, and
post-deployment verification repeats the same expected/embedded comparison
before readiness. A schema-V2 ready slot cannot silently use a multi-artifact
member whose distinct embedded versions cannot be represented by one member
version.
Repository sources must stay inside that project's directory, including the
established ignored private-build subtree. The manager reloads those manifests
after physical verification and before state/ledger commit, so concurrent
candidate drift triggers full rollback.

`RECORD_RESULT` requires `project_uuid` for a multi-member cohort and updates
only that member. Shared deployment readiness never copies a `PASS`, `FAIL`, or
`INCONCLUSIVE` classification between members. `PROMOTE_SLOT` requires an
independent explicit `PASS` for every member, rejects temporary dependency
overrides, and promotes all members atomically; removal likewise clears the
whole cohort and restores its accepted predecessors/dependencies together.

Preflight and verification read exactly one root `fabric.mod.json` from every
enabled Fabric JAR. They record primary ID, `provides` aliases, embedded
version, and hard `depends` predicates. Provider ownership must be unique.
Fabric predicate evaluation follows Loader semantics: predicate-array entries
are OR alternatives, space-delimited terms inside one predicate are ANDed, and
semantic/prerelease/wildcard ordering follows Fabric Loader. A present provider
must satisfy its predicate. A missing ID known to manager ownership fails and
names the companion that must be supplied in the same cohort operation.
Only Fabric's virtual platform IDs (`java`, `minecraft`, `fabricloader`) are
accepted without an enabled root provider JAR and are explicitly classified in
the receipt. This JAR-graph check does not claim an exact version attestation
for those launcher/runtime virtual providers; its exact-version guarantee
applies to root descriptors from enabled managed and unmanaged JARs. A
physically present unmanaged provider is accepted and its exact
path/version is recorded, but a missing external or managed hard dependency
fails closed. Post-deployment verification repeats the graph from physical
enabled bytes; readiness is never committed unless both hash inventory and
graph report `VERIFIED`.

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

An explicit user-reported `PASS`, `FAIL`, or `INCONCLUSIVE` may be recorded for
a candidate tested outside Slots A and B. Current authoritative `main` must
uniquely bind the report to the manifest's current release version, filename,
SHA-256, and source checkpoint; ambiguity or drift fails closed. The project
`WORKBENCH_STATUS.json` and `CODEX_LOG.md` record it explicitly as
user-reported/external evidence, without invented row-level observations.
Deployment remains truthful (`NOT_DEPLOYED` when no manager deployment exists),
and lifecycle remains outside `TESTING` unless the project UUID actually
occupies a slot. Recording external evidence alone makes no manager
transition; `FAIL` and `INCONCLUSIVE` therefore never consume a slot.
Managed-slot result recording and promotion are unchanged.

`PROMOTE_USER_PASSED_BATCH` is the serialized accepted-stack path for one or
more exact candidates whose `PASS` was reported directly by the user. It
requires the exact `USER_REPORTED_EXACT_RUNTIME_PASS` authorization, preserves
both test slots byte-for-byte, and adds or replaces the declared members in one
runtime-state revision. The Stack number increments once for each new member or
byte-changed replacement. A replacement is allowed only when the member's exact
`replaces_accepted_deployment_id` identifies the sole accepted predecessor for
the same project; omitting it or setting it to `null` permits only a new
accepted project. A byte-identical external successor still replaces the exact
accepted version/source/deployment/artifact identity and refreshes
`accepted_at`, but does not increment the composition-based Stack number. The
production CLI binds every candidate's project UUID/ID, version, filename,
SHA-256, and source checkpoint to that project's current manifest release.
Candidate bytes must come from the canonical tracked
`projects/<project>/artifacts/<filename>` (or equivalent canonical project
directory) path. The project controls preserve a replaced accepted release as
rollback/provenance where applicable. If a batch member separately requests a
target-local retained rollback, it must match that manifest's rollback release
exactly; a project with no manifest rollback cannot declare one. A manifest
rollback does not by itself require target-local retention;
`retained_rollbacks` remains optional for ordinary repository-backed rollback
history. The manager reloads the affected current manifests immediately before
committing the ledger and runtime state. It aborts with physical rollback if a
bound current release has drifted; the runtime-state revision CAS independently
rejects accepted-predecessor or slot drift.

A retained rollback is target-local history, not an active baseline artifact.
Its unit uses the same project UUID/ID, a `FROZEN_LEGACY` identity, and an exact
`ADOPTED_TARGET mods/<filename>` source. The manager stages and verifies those
enabled predecessor bytes, snapshots both paths for failure recovery, and uses
a same-filesystem atomic rename to `mods/<filename>.disabled` inside the locked
transaction. All
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
receipt enumerates every slot cohort member with project name/UUID, deployment
UUID, canonical version, source checkpoint, independent runtime result,
artifact UUID/path/hash/disposition, and embedded Fabric version. It also
reports dependency-provider resolution and one explicit release comparison per
member: `CURRENT_RELEASE_DEPLOYED`, `OLDER_RELEASE_DEPLOYED`, or
`CURRENT_RELEASE_NOT_DEPLOYED`. Accepted-baseline counts and deterministic
managed-inventory digests remain present.

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
Slot A: <name> - Canary <number> + <name> - Canary <number>  (or Slot A: Empty)
Slot B: <canonical project name> - Canary <number>  (or Slot B: Empty)
```

Canary numbers derive from every cohort member's canonical `version`, never an
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
write/removal, ownership, dependency-resolution, slot/cohort, lifecycle, and
title plan first. The manager checks Windows Java/JVM process command lines and
refuses mutation when a Minecraft JVM names the dedicated profile; it never
terminates that process. Operator shells and inspection tools that merely quote
the profile path are not treated as a running Minecraft instance. An apply
recomputes the plan while holding the target-local exclusive
lock, stages and hashes every addition, verifies the result, commits the target
ledger and repository state together, and restores the preimage if any step
fails.

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
- User-passed batch promotion preserves both slots. It may add a new accepted
  project or replace the same project's sole predecessor only by exact
  `replaces_accepted_deployment_id`; a wrong, missing, or drifted predecessor
  fails before mutation.
- A separately declared target-local retained rollback is atomically converted
  into the exact manager-owned `.jar.disabled` form; this remains optional for
  ordinary repository-backed rollback provenance.
- A slot may declare `dependency_overrides` for exact accepted dependency
  deployments that it temporarily supersedes. The slot artifact set must cover
  every ownership key of each overridden dependency; removing the slot restores
  those accepted bytes, and a normal slot promotion cannot absorb the temporary
  dependency override into the accepted project.
- Every project UUID in a cohort occupies that one slot for lifecycle purposes;
  member runtime results are never aggregated or copied between members.
- Slots are independent: updating, clearing, or promoting one cannot move or
  rewrite the other.
- A provider replacement is accepted only when every remaining member's exact
  staged Fabric dependency is satisfied. An incompatible retained companion
  fails before any filesystem write; a compatible bounded predicate permits an
  atomic provider-only member replacement.
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

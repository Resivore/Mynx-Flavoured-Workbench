# Test Instance Manager V2

The V2 manager is the physical adapter for `runtime-state.json`. It owns only
the configured dedicated Minecraft 26.2 Workbench `mods` directory, its
target-local V2 lock/ledger, and the repository state commit paired with a
verified physical transition. The protected Minecraft 26.1.2 profile is
permanently rejected before access.

The adopted state contains the exact frozen accepted baseline (23 project
units / 26 artifacts), fixed independent Slots A and B, and exact per-slot
deployment/runtime evidence. Accepted Heart C5 remains managed but disabled;
Heart C7 and Mossy C2 remain enabled failed candidates. Deployment verification
does not imply Minecraft runtime validation.

## Migration closure

The physical V2 migration completed at checkpoint
`d0479458c9ac3c49db825be7a25746f554976922` and was reverified on authoritative
`main` at `a5b29a421c503c959d820ecb05d92126c7ddad13`. Runtime state is `ACTIVE` at
revision 2, accepted-baseline revision 1 is `ADOPTED`, and adoption required no
artifact writes or removals. The exact Heart C7 and Mossy C2 slots remain
independent `FAIL` candidates; neither was promoted and no C8 or C3 was created.

Google Sheet mirroring is not an activation or physical-verification authority.
Its pending R2 publication failure is recorded separately in the Sheet
synchronization infrastructure record and does not change this migration's
successful lifecycle or physical state.

## Normal commands

Run from the repository root:

```powershell
python -B tools/test_instance_manager/manager.py verify
python -B tools/test_instance_manager/manager.py transition --operation <operation.json> --expected-revision <revision> --at <RFC3339-UTC>
python -B tools/test_instance_manager/manager.py transition --operation <operation.json> --expected-revision <revision> --at <RFC3339-UTC> --apply
```

`transition` is a dry-run unless `--apply` is present. Inspect the complete
write/removal plan first. An apply recomputes the plan while holding the
target-local exclusive lock, stages and hashes every addition, verifies the
result, commits the target ledger and repository state together, and restores
the preimage if any step fails.

`adopt` was the one-time migration path from populated `GATED` state. It
required a zero-operation physical plan and exact verification before writing
the V2 ledger and activating state; it now fails closed because the ledger
already exists.

## Invariants

- Artifacts use exact filenames, SHA-256 values, Fabric `mod:<id>` ownership,
  and repository or explicitly adopted-target sources.
- Accepted artifacts replaced by a slot remain present as `.jar.disabled`.
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
- Private/non-redistributable adopted artifacts remain target-local; the
  repository stores only their exact identity and path descriptors.

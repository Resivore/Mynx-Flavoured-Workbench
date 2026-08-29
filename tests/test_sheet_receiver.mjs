import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";
import vm from "node:vm";

const source = fs.readFileSync(new URL("../tools/sheet_sync/receiver/Core.gs", import.meta.url), "utf8");
const context = {};
vm.createContext(context);
vm.runInContext(source, context);
const core = context.MynxSheetSync;

function envelope(revision = 1, eventId = "a".repeat(64)) {
  return {
    contract_version: 1,
    event_id: eventId,
    operation: "project_status_upsert",
    source: {
      repository: "Resivore/Mynx-Flavoured-Workbench",
      ref: "refs/heads/main",
      publication_commit: "b".repeat(40),
      manifest_path: "projects/mossy-stone/WORKBENCH_STATUS.json",
    },
    record: {
      project_uuid: "f81d4fae-7dec-4f7d-a3bb-b2b3f3396812",
      project_name: "Mossy Stone",
      project_id: "mossy-stone",
      aliases: [], legacy_names: [], legacy_ids: [],
      lifecycle: "PLANNED", goals: ["Define it"], scope: ["Future work"],
      boundaries: { owned_paths: ["projects/mossy-stone"], exclusions: [] }, dependencies: [],
      milestone: "Initialized", current_version: null, current_artifact_filename: null,
      current_artifact_sha256: null, current_source_commit: null, accepted_version: null, accepted_artifact_filename: null,
      accepted_artifact_sha256: null, accepted_source_commit: null, rollback_version: null, rollback_artifact_filename: null,
      rollback_artifact_sha256: null, rollback_source_commit: null, accepted_current: "NO_ACCEPTED", accepted_rollback: "NO_ROLLBACK",
      build_validation: "NOT_RUN", deployment_validation: "NOT_DEPLOYED", runtime_validation: "RUNTIME_UNTESTED",
      blocker: null, revision, activity_at: "2026-08-29T12:00:00Z", updated_at: "2026-08-29T12:00:00Z",
      last_codex_at: "2026-08-29T12:00:00Z", source_commit: "c".repeat(40), sheet_participates: true,
      sheet_exclusion_reason: null, publication_commit: "b".repeat(40),
    },
    ownership: { sheet_preserves: ["Priority", "Notes"] },
  };
}

function headers() {
  return [...Object.values(core.columnMap()), ...Object.values(core.eventColumnMap()), ...core.humanFields()];
}

test("receiver inserts by UUID and leaves human-owned fields blank", () => {
  const result = core.applyToRows(headers(), [], envelope());
  assert.equal(result.changed, true);
  assert.equal(result.rows.length, 1);
  assert.equal(result.rows[0][headers().indexOf("Priority")], "");
  assert.equal(result.rows[0][headers().indexOf("Notes")], "");
});

test("metadata-only next revision preserves Priority and Notes", () => {
  const first = core.applyToRows(headers(), [], envelope());
  first.rows[0][headers().indexOf("Priority")] = "High";
  first.rows[0][headers().indexOf("Notes")] = "Human text";
  const next = envelope(2, "d".repeat(64));
  next.record.last_codex_at = "2026-08-29T12:01:00Z";
  next.record.updated_at = "2026-08-29T12:01:00Z";
  const result = core.applyToRows(headers(), first.rows, next);
  assert.equal(result.changed, true);
  assert.equal(result.rows[0][headers().indexOf("Priority")], "High");
  assert.equal(result.rows[0][headers().indexOf("Notes")], "Human text");
});

test("idempotent replay is unchanged and same-revision conflict fails", () => {
  const firstEnvelope = envelope();
  const first = core.applyToRows(headers(), [], firstEnvelope);
  assert.equal(core.applyToRows(headers(), first.rows, firstEnvelope).changed, false);
  assert.throws(() => core.applyToRows(headers(), first.rows, envelope(1, "e".repeat(64))), /same-revision/);
});

test("same-revision partial row with no event marker is reconciled", () => {
  const firstEnvelope = envelope();
  const first = core.applyToRows(headers(), [], firstEnvelope);
  first.rows[0][headers().indexOf("Event ID")] = "";
  first.rows[0][headers().indexOf("Milestone")] = "partial";
  const repaired = core.applyToRows(headers(), first.rows, firstEnvelope);
  assert.equal(repaired.changed, true);
  assert.equal(repaired.rows[0][headers().indexOf("Milestone")], "Initialized");
  assert.equal(repaired.rows[0][headers().indexOf("Event ID")], firstEnvelope.event_id);
});

test("duplicate UUID rows and feature refs fail closed", () => {
  const first = core.applyToRows(headers(), [], envelope());
  assert.throws(() => core.applyToRows(headers(), [first.rows[0], first.rows[0]], envelope(2, "f".repeat(64))), /duplicate Sheet rows/);
  const feature = envelope();
  feature.source.ref = "refs/heads/codex/feature";
  assert.throws(() => core.validateEnvelope(feature), /not authoritative main/);
});

test("unexpected human-owned values are rejected", () => {
  const bad = envelope();
  bad.record.Notes = "must not cross the contract";
  assert.throws(() => core.validateEnvelope(bad), /unknown or missing keys/);
});

test("formula-leading repository text is written as a literal", () => {
  const formula = envelope();
  formula.record.milestone = "=IMPORTDATA(\"https://example.invalid\")";
  const result = core.applyToRows(headers(), [], formula);
  assert.equal(result.rows[0][headers().indexOf("Milestone")], "'=IMPORTDATA(\"https://example.invalid\")");
});

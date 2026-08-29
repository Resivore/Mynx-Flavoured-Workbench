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
    ownership: { sheet_preserves: ["Notes"] },
  };
}

function headers() {
  return [...Object.values(core.columnMap()), ...Object.values(core.eventColumnMap()), ...core.humanFields()];
}

function assertUnknownInsert(revision, eventId) {
  const incoming = envelope(revision, eventId);
  const sheetHeaders = headers();
  const result = core.applyToRows(sheetHeaders, [], incoming);
  assert.equal(result.changed, true);
  assert.equal(result.rows.length, 1);
  assert.equal(result.rows[0][sheetHeaders.indexOf("Project UUID")], incoming.record.project_uuid);
  assert.equal(result.rows[0][sheetHeaders.indexOf("Project")], incoming.record.project_name);
  assert.equal(result.rows[0][sheetHeaders.indexOf("Revision")], revision);
  assert.equal(result.rows[0][sheetHeaders.indexOf("Event ID")], eventId);
  assert.equal(result.rows[0][sheetHeaders.indexOf("Manifest Path")], incoming.source.manifest_path);
  Object.values(core.columnMap()).forEach((header) => {
    assert.equal(result.rows[0][sheetHeaders.indexOf(header)], result.values[header]);
  });
  assert.equal(result.rows[0][sheetHeaders.indexOf("Notes")], "");
}

test("unknown UUID at R1 inserts the full canonical record at R1", () => {
  assertUnknownInsert(1, "a".repeat(64));
});

test("unknown UUID at R5 inserts the full canonical record at R5", () => {
  assertUnknownInsert(5, "b".repeat(64));
});

test("unknown UUID at a higher positive revision inserts at that revision", () => {
  assertUnknownInsert(12, "c".repeat(64));
});

test("versioned dotted project IDs and manifest paths are accepted", () => {
  const dotted = envelope();
  dotted.source.manifest_path = "projects/ribbits-26.2/WORKBENCH_STATUS.json";
  dotted.record.project_id = "ribbits-26.2";
  dotted.record.boundaries.owned_paths = ["projects/ribbits-26.2"];
  assert.doesNotThrow(() => core.validateEnvelope(dotted));
});

test("unknown UUID rejects revision zero", () => {
  assert.throws(
    () => core.applyToRows(headers(), [], envelope(0, "0".repeat(64))),
    /revision must be a positive integer/,
  );
});

test("existing R5 to R6 updates", () => {
  const first = core.applyToRows(headers(), [], envelope(5, "d".repeat(64)));
  const next = envelope(6, "e".repeat(64));
  const result = core.applyToRows(headers(), first.rows, next);
  assert.equal(result.changed, true);
  assert.equal(result.rows[0][headers().indexOf("Revision")], 6);
  assert.equal(result.rows[0][headers().indexOf("Event ID")], next.event_id);
});

test("existing R5 to R7 rejects a skipped revision", () => {
  const first = core.applyToRows(headers(), [], envelope(5, "f".repeat(64)));
  assert.throws(
    () => core.applyToRows(headers(), first.rows, envelope(7, "1".repeat(64))),
    /stale or skipped Sheet revision/,
  );
});

test("existing R5 to R4 rejects a stale revision", () => {
  const first = core.applyToRows(headers(), [], envelope(5, "2".repeat(64)));
  assert.throws(
    () => core.applyToRows(headers(), first.rows, envelope(4, "3".repeat(64))),
    /stale or skipped Sheet revision/,
  );
});

test("same revision and same Event ID is an idempotent no-op", () => {
  const firstEnvelope = envelope(5, "4".repeat(64));
  const first = core.applyToRows(headers(), [], firstEnvelope);
  assert.equal(core.applyToRows(headers(), first.rows, firstEnvelope).changed, false);
});

test("same revision and a different Event ID rejects", () => {
  const first = core.applyToRows(headers(), [], envelope(5, "5".repeat(64)));
  assert.throws(
    () => core.applyToRows(headers(), first.rows, envelope(5, "6".repeat(64))),
    /same-revision Sheet conflict/,
  );
});

test("existing updates preserve Notes", () => {
  const first = core.applyToRows(headers(), [], envelope(5, "7".repeat(64)));
  first.rows[0][headers().indexOf("Notes")] = "Human text";
  const next = envelope(6, "8".repeat(64));
  next.record.last_codex_at = "2026-08-29T12:01:00Z";
  next.record.updated_at = "2026-08-29T12:01:00Z";
  const result = core.applyToRows(headers(), first.rows, next);
  assert.equal(result.changed, true);
  assert.equal(result.rows[0][headers().indexOf("Notes")], "Human text");
});

test("Priority remains absent from the contract", () => {
  assert.deepEqual(Array.from(core.humanFields()), ["Notes"]);
  assert.equal(headers().includes("Priority"), false);
  const bad = envelope();
  bad.record.priority = "High";
  assert.throws(() => core.validateEnvelope(bad), /record has unknown or missing keys/);
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

test("duplicate UUID rows and non-authoritative sources fail closed", () => {
  const first = core.applyToRows(headers(), [], envelope());
  assert.throws(() => core.applyToRows(headers(), [first.rows[0], first.rows[0]], envelope(2, "f".repeat(64))), /duplicate Sheet rows/);
  const feature = envelope();
  feature.source.ref = "refs/heads/codex/feature";
  assert.throws(() => core.validateEnvelope(feature), /not authoritative main/);
  const fork = envelope();
  fork.source.repository = "Elsewhere/Mynx-Flavoured-Workbench";
  assert.throws(() => core.validateEnvelope(fork), /not authoritative main/);
});

test("repository events cannot overwrite Notes", () => {
  const bad = envelope();
  bad.record.Notes = "must not cross the contract";
  assert.throws(() => core.validateEnvelope(bad), /record contains a human-owned field/);
});

test("duplicate and missing required headers fail normally", () => {
  assert.throws(
    () => core.applyToRows([...headers(), "Project UUID"], [], envelope()),
    /duplicate Sheet header: Project UUID/,
  );
  assert.throws(
    () => core.applyToRows(headers().filter((header) => header !== "Milestone"), [], envelope()),
    /missing automation Sheet column: Milestone/,
  );
  assert.throws(
    () => core.applyToRows(headers().filter((header) => header !== "Notes"), [], envelope()),
    /missing human-owned Sheet column: Notes/,
  );
});

test("formula-leading repository text is written as a literal", () => {
  const formula = envelope();
  formula.record.milestone = "=IMPORTDATA(\"https://example.invalid\")";
  const result = core.applyToRows(headers(), [], formula);
  assert.equal(result.rows[0][headers().indexOf("Milestone")], "'=IMPORTDATA(\"https://example.invalid\")");
});

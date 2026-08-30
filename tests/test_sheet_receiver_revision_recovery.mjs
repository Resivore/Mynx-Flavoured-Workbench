import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";
import vm from "node:vm";

const source = fs.readFileSync(new URL("../tools/sheet_sync/receiver/Core.gs", import.meta.url), "utf8");
const context = {};
vm.createContext(context);
vm.runInContext(source, context);
const core = context.MynxSheetSync;

function envelope(revision, eventId, milestone = "Initialized") {
  return {
    contract_version: 1,
    event_id: eventId,
    operation: "project_status_upsert",
    source: {
      repository: "Resivore/Mynx-Flavoured-Workbench",
      ref: "refs/heads/main",
      publication_commit: "b".repeat(40),
      manifest_path: "projects/naturalist/WORKBENCH_STATUS.json",
    },
    record: {
      project_uuid: "c92ad4fe-c210-46c4-ba1d-59828d2bcbcd",
      project_name: "Naturalist 26.2 Port",
      project_id: "naturalist",
      aliases: ["Naturalist"],
      legacy_names: [],
      legacy_ids: ["naturalist"],
      lifecycle: "ACTIVE",
      goals: ["Port Naturalist"],
      scope: ["Preserve behavior"],
      boundaries: { owned_paths: ["projects/naturalist"], exclusions: [] },
      dependencies: [],
      milestone,
      current_version: "2.0.3+26.2-port-canary1",
      current_artifact_filename: "naturalist-2.0.3+26.2-port-canary1.jar",
      current_artifact_sha256: "a".repeat(64),
      current_source_commit: "c".repeat(40),
      accepted_version: null,
      accepted_artifact_filename: null,
      accepted_artifact_sha256: null,
      accepted_source_commit: null,
      rollback_version: null,
      rollback_artifact_filename: null,
      rollback_artifact_sha256: null,
      rollback_source_commit: null,
      accepted_current: "NO_ACCEPTED",
      accepted_rollback: "NO_ROLLBACK",
      build_validation: "STATIC_PASS",
      deployment_validation: "NOT_DEPLOYED",
      runtime_validation: "RUNTIME_UNTESTED",
      blocker: null,
      revision,
      activity_at: "2026-08-30T01:38:14Z",
      updated_at: "2026-08-30T01:38:14Z",
      last_codex_at: "2026-08-30T01:38:14Z",
      source_commit: "c".repeat(40),
      sheet_participates: true,
      sheet_exclusion_reason: null,
      publication_commit: "b".repeat(40),
    },
    ownership: { sheet_preserves: ["Notes"] },
  };
}

function headers() {
  return [...Object.values(core.columnMap()), ...Object.values(core.eventColumnMap()), ...core.humanFields()];
}

test("malformed stored revision is healed by authoritative main while Notes is preserved", () => {
  const sheetHeaders = headers();
  const initial = core.applyToRows(sheetHeaders, [], envelope(4, "4".repeat(64), "R4 state"));
  initial.rows[0][sheetHeaders.indexOf("Revision")] = "R4";
  initial.rows[0][sheetHeaders.indexOf("Notes")] = "Human-owned note";

  const result = core.applyToRows(sheetHeaders, initial.rows, envelope(5, "5".repeat(64), "R5 state"));

  assert.equal(result.changed, true);
  assert.equal(result.rows[0][sheetHeaders.indexOf("Revision")], 5);
  assert.equal(result.rows[0][sheetHeaders.indexOf("Milestone")], "R5 state");
  assert.equal(result.rows[0][sheetHeaders.indexOf("Notes")], "Human-owned note");
});

test("valid stored revisions still reject stale authoritative events", () => {
  const sheetHeaders = headers();
  const current = core.applyToRows(sheetHeaders, [], envelope(6, "6".repeat(64), "R6 state"));
  assert.throws(
    () => core.applyToRows(sheetHeaders, current.rows, envelope(5, "5".repeat(64), "R5 state")),
    /stale Sheet revision/,
  );
});

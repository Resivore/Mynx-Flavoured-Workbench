import assert from "node:assert/strict";
import crypto from "node:crypto";
import fs from "node:fs";
import test from "node:test";
import vm from "node:vm";

const source = fs.readFileSync(new URL("../tools/sheet_sync/receiver/Core.gs", import.meta.url), "utf8");
const receiverSource = fs.readFileSync(new URL("../tools/sheet_sync/receiver/Code.gs", import.meta.url), "utf8");
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

function signedRequest(value, secret = "s".repeat(32)) {
  const payload = Buffer.from(JSON.stringify(value), "utf8").toString("base64url");
  const signature = crypto.createHmac("sha256", secret).update(payload, "ascii").digest("hex");
  return { postData: { contents: JSON.stringify({ payload, signature: `sha256=${signature}` }) } };
}

function createReceiverHarness({
  inputRows = [],
  lockSucceeds = true,
  acceptWrites = true,
  sortThrows = false,
} = {}) {
  const secret = "s".repeat(32);
  const sheetHeaders = headers();
  const data = [sheetHeaders.slice(), ...inputRows.map((row) => row.slice())];
  const state = {
    hiddenColumns: [],
    lockTimeouts: [],
    operations: [],
    releaseCount: 0,
    openCount: 0,
    flushCount: 0,
  };

  function ensureRow(rowIndex) {
    while (data.length <= rowIndex) data.push(new Array(sheetHeaders.length).fill(""));
    while (data[rowIndex].length < sheetHeaders.length) data[rowIndex].push("");
  }

  const sheet = {
    getLastColumn() {
      return data.reduce((lastColumn, row) => {
        for (let column = row.length - 1; column >= 0; column -= 1) {
          if (row[column] !== "") return Math.max(lastColumn, column + 1);
        }
        return lastColumn;
      }, 0);
    },
    getLastRow: () => data.length,
    insertRowAfter(afterRow) {
      state.operations.push({ type: "insertRowAfter", afterRow });
      data.splice(afterRow, 0, new Array(sheetHeaders.length).fill(""));
    },
    hideColumns(column) {
      state.operations.push({ type: "hideColumns", column });
      state.hiddenColumns.push(column);
    },
    getRange(row, column, rowCount = 1, columnCount = 1) {
      return {
        getValues() {
          const values = [];
          for (let rowOffset = 0; rowOffset < rowCount; rowOffset += 1) {
            ensureRow(row - 1 + rowOffset);
            values.push(data[row - 1 + rowOffset].slice(column - 1, column - 1 + columnCount));
          }
          return values;
        },
        setValue(value) {
          ensureRow(row - 1);
          data[row - 1][column - 1] = value;
          state.operations.push({ type: "setValue", row, column, value });
          return this;
        },
        setValues(values) {
          assert.equal(values.length, rowCount);
          values.forEach((valuesRow, rowOffset) => {
            assert.equal(valuesRow.length, columnCount);
            ensureRow(row - 1 + rowOffset);
            valuesRow.forEach((value, columnOffset) => {
              data[row - 1 + rowOffset][column - 1 + columnOffset] = value;
            });
          });
          state.operations.push({ type: "setValues", row, column, rowCount, columnCount });
          return this;
        },
        sort(specification) {
          const specifications = Array.isArray(specification) ? specification : [specification];
          const sortedRows = data.slice(row - 1, row - 1 + rowCount).map((dataRow) => (
            dataRow.slice(column - 1, column - 1 + columnCount)
          ));
          sortedRows.sort((left, right) => {
            for (const item of specifications) {
              const sortColumn = typeof item === "number" ? item : item.column;
              const ascending = typeof item === "number" || item.ascending !== false;
              const leftValue = left[sortColumn - column];
              const rightValue = right[sortColumn - column];
              if (leftValue < rightValue) return ascending ? -1 : 1;
              if (leftValue > rightValue) return ascending ? 1 : -1;
            }
            return 0;
          });
          sortedRows.forEach((sortedRow, rowOffset) => {
            sortedRow.forEach((value, columnOffset) => {
              data[row - 1 + rowOffset][column - 1 + columnOffset] = value;
            });
          });
          state.operations.push({ type: "sort", row, column, rowCount, columnCount, specification });
          return this;
        },
      };
    },
  };

  const receiverContext = {
    PropertiesService: {
      getScriptProperties: () => ({
        getProperty(name) {
          return {
            MYNX_STATUS_ACCEPT_WRITES: acceptWrites ? "true" : "false",
            MYNX_STATUS_HMAC_SECRET: secret,
            MYNX_STATUS_SPREADSHEET_ID: "sheet-id",
            MYNX_STATUS_SHEET_NAME: "Projects",
          }[name] ?? null;
        },
      }),
    },
    Utilities: {
      computeHmacSha256Signature(payload, key) {
        return Array.from(crypto.createHmac("sha256", key).update(payload, "ascii").digest(), (byte) => (
          byte > 127 ? byte - 256 : byte
        ));
      },
      base64DecodeWebSafe(payload) {
        return Array.from(Buffer.from(payload, "base64url"));
      },
      newBlob(bytes) {
        return { getDataAsString: () => Buffer.from(bytes).toString("utf8") };
      },
    },
    LockService: {
      getScriptLock: () => ({
        tryLock(timeout) {
          state.lockTimeouts.push(timeout);
          return lockSucceeds;
        },
        releaseLock() {
          state.releaseCount += 1;
          state.operations.push({ type: "releaseLock" });
        },
      }),
    },
    SpreadsheetApp: {
      openById() {
        state.openCount += 1;
        return { getSheetByName: () => sheet };
      },
      getActiveSpreadsheet() {
        return { getSheetByName: () => sheet };
      },
      flush() {
        state.flushCount += 1;
        state.operations.push({ type: "flush" });
      },
    },
    ContentService: {
      MimeType: { JSON: "application/json" },
      createTextOutput(text) {
        return {
          text,
          setMimeType() { return this; },
        };
      },
    },
  };
  vm.createContext(receiverContext);
  vm.runInContext(source, receiverContext);
  vm.runInContext(receiverSource, receiverContext);
  const sortProjects = receiverContext.sortProjects_;
  receiverContext.sortProjects_ = (targetSheet) => {
    state.operations.push({ type: "sortProjects" });
    if (sortThrows) throw new Error("injected sorting failure");
    return sortProjects(targetSheet);
  };
  return {
    data,
    secret,
    sheet,
    state,
    post: (request) => JSON.parse(receiverContext.doPost(request).text),
    sortProjects: () => sortProjects(sheet),
    sortNow: () => receiverContext.sortProjectsNow(),
  };
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

function sortableRow(project, lifecycle, activityAt) {
  const sheetHeaders = headers();
  const row = sheetHeaders.map((header) => `${project}:${header}`);
  row[sheetHeaders.indexOf("Project")] = project;
  row[sheetHeaders.indexOf("Lifecycle")] = lifecycle;
  row[sheetHeaders.indexOf("Activity At")] = activityAt;
  row[sheetHeaders.indexOf("Notes")] = `${project}:human note`;
  return row;
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

test("normal next revision R2 to R3 updates", () => {
  const first = core.applyToRows(headers(), [], envelope(2, "d".repeat(64)));
  const next = envelope(3, "e".repeat(64));
  const result = core.applyToRows(headers(), first.rows, next);
  assert.equal(result.changed, true);
  assert.equal(result.rows[0][headers().indexOf("Revision")], 3);
  assert.equal(result.rows[0][headers().indexOf("Event ID")], next.event_id);
});

test("skipped revision recovery accepts R1 to R3", () => {
  const first = core.applyToRows(headers(), [], envelope(1, "f".repeat(64)));
  const result = core.applyToRows(headers(), first.rows, envelope(3, "1".repeat(64)));
  assert.equal(result.changed, true);
  assert.equal(result.rows[0][headers().indexOf("Revision")], 3);
});

test("a larger legitimate forward gap converges directly", () => {
  const first = core.applyToRows(headers(), [], envelope(1, "2".repeat(64)));
  const result = core.applyToRows(headers(), first.rows, envelope(12, "3".repeat(64)));
  assert.equal(result.changed, true);
  assert.equal(result.rows[0][headers().indexOf("Revision")], 12);
});

test("blank stored Revision converges as uncommitted revision zero", () => {
  const first = core.applyToRows(headers(), [], envelope(1, "4".repeat(64)));
  first.rows[0][headers().indexOf("Revision")] = "";
  const result = core.applyToRows(headers(), first.rows, envelope(3, "5".repeat(64)));
  assert.equal(result.changed, true);
  assert.equal(result.rows[0][headers().indexOf("Revision")], 3);
});

test("existing R3 rejects stale R2 with a distinct error", () => {
  const first = core.applyToRows(headers(), [], envelope(3, "6".repeat(64)));
  assert.throws(
    () => core.applyToRows(headers(), first.rows, envelope(2, "7".repeat(64))),
    /stale Sheet revision/,
  );
});

test("same revision and same Event ID is an idempotent no-op", () => {
  const firstEnvelope = envelope(5, "4".repeat(64));
  const first = core.applyToRows(headers(), [], firstEnvelope);
  assert.equal(core.applyToRows(headers(), first.rows, firstEnvelope).changed, false);
});

test("same revision and Event ID cannot mask conflicting machine content", () => {
  const incoming = envelope(5, "4".repeat(64));
  const first = core.applyToRows(headers(), [], incoming);
  first.rows[0][headers().indexOf("Milestone")] = "Conflicting stored state";
  assert.throws(
    () => core.applyToRows(headers(), first.rows, incoming),
    /same-revision Sheet conflict/,
  );
});

test("same revision with identical machine content is a semantic no-op across publication events", () => {
  const first = core.applyToRows(headers(), [], envelope(5, "5".repeat(64)));
  first.rows[0][headers().indexOf("Notes")] = "Keep this";
  const replay = envelope(5, "6".repeat(64));
  replay.source.publication_commit = "d".repeat(40);
  replay.record.publication_commit = "d".repeat(40);
  const result = core.applyToRows(headers(), first.rows, replay);
  assert.equal(result.changed, false);
  assert.equal(result.rows[0][headers().indexOf("Event ID")], "5".repeat(64));
  assert.equal(result.rows[0][headers().indexOf("Publication Commit")], "b".repeat(40));
  assert.equal(result.rows[0][headers().indexOf("Notes")], "Keep this");
});

test("same revision with conflicting authoritative content rejects", () => {
  const first = core.applyToRows(headers(), [], envelope(5, "7".repeat(64)));
  const conflicting = envelope(5, "8".repeat(64));
  conflicting.record.milestone = "Conflicting state";
  assert.throws(
    () => core.applyToRows(headers(), first.rows, conflicting),
    /same-revision Sheet conflict/,
  );
});

test("existing updates preserve Notes", () => {
  const first = core.applyToRows(headers(), [], envelope(1, "9".repeat(64)));
  first.rows[0][headers().indexOf("Notes")] = "Human text";
  const next = envelope(7, "a".repeat(64));
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

test("same-revision matching row with no event marker completes its commit marker", () => {
  const firstEnvelope = envelope();
  const first = core.applyToRows(headers(), [], firstEnvelope);
  first.rows[0][headers().indexOf("Event ID")] = "";
  const repaired = core.applyToRows(headers(), first.rows, firstEnvelope);
  assert.equal(repaired.changed, true);
  assert.equal(repaired.rows[0][headers().indexOf("Milestone")], "Initialized");
  assert.equal(repaired.rows[0][headers().indexOf("Event ID")], firstEnvelope.event_id);
});

test("same-revision conflicting row with no event marker is rejected", () => {
  const firstEnvelope = envelope();
  const first = core.applyToRows(headers(), [], firstEnvelope);
  first.rows[0][headers().indexOf("Event ID")] = "";
  first.rows[0][headers().indexOf("Milestone")] = "Conflicting stored state";
  assert.throws(
    () => core.applyToRows(headers(), first.rows, firstEnvelope),
    /same-revision Sheet conflict/,
  );
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

test("manual project sorter preserves the exact lifecycle and Activity At order with complete rows", () => {
  const sheetHeaders = headers();
  const inputRows = [
    sortableRow("unknown", "SOMEDAY", "2026-08-30T00:00:00Z"),
    sortableRow("testing-old", "TESTING", "2026-08-01T00:00:00Z"),
    sortableRow("parked", "PARKED", "2026-08-29T00:00:00Z"),
    sortableRow("testing-missing", "TESTING", ""),
    sortableRow("accepted", "ACCEPTED", "2026-08-29T00:00:00Z"),
    sortableRow("testing-new", "TESTING", "2026-08-29T00:00:00Z"),
    sortableRow("blocked", "BLOCKED", "2026-08-29T00:00:00Z"),
    sortableRow("planned", "PLANNED", "2026-08-29T00:00:00Z"),
    sortableRow("active", "ACTIVE", "2026-08-29T00:00:00Z"),
  ];
  const originalRows = new Map(inputRows.map((row) => [
    row[sheetHeaders.indexOf("Project")],
    row.slice(),
  ]));
  const receiver = createReceiverHarness({ inputRows });

  receiver.sortNow();

  assert.deepEqual(receiver.data[0], [...sheetHeaders, "__Sort Key"]);
  assert.deepEqual(receiver.state.hiddenColumns, [sheetHeaders.length + 1]);
  const expectedProjects = [
    "testing-missing",
    "testing-new",
    "testing-old",
    "active",
    "planned",
    "blocked",
    "accepted",
    "parked",
    "unknown",
  ];
  assert.deepEqual(
    receiver.data.slice(1).map((row) => row[sheetHeaders.indexOf("Project")]),
    expectedProjects,
  );
  receiver.data.slice(1).forEach((row) => {
    const project = row[sheetHeaders.indexOf("Project")];
    assert.deepEqual(row.slice(0, sheetHeaders.length), originalRows.get(project));
  });
  const sortOperation = receiver.state.operations.find((operation) => operation.type === "sort");
  assert.deepEqual(
    {
      row: sortOperation.row,
      column: sortOperation.column,
      rowCount: sortOperation.rowCount,
      columnCount: sortOperation.columnCount,
    },
    { row: 2, column: 1, rowCount: inputRows.length, columnCount: sheetHeaders.length + 1 },
  );
  assert.deepEqual(receiver.state.lockTimeouts, [5000]);
  assert.equal(receiver.state.releaseCount, 1);
});

test("doPost authenticates, locks, converges a forward gap, and preserves Notes", () => {
  const first = core.applyToRows(headers(), [], envelope(1, "b".repeat(64)));
  first.rows[0][headers().indexOf("Notes")] = "Human-owned note";
  const incoming = envelope(3, "c".repeat(64));
  incoming.record.milestone = "Current authoritative main";
  const receiver = createReceiverHarness({ inputRows: first.rows });

  assert.deepEqual(receiver.post(signedRequest(incoming, receiver.secret)), {
    ok: true,
    changed: true,
    event_id: incoming.event_id,
  });
  assert.deepEqual(receiver.state.lockTimeouts, [5000]);
  assert.equal(receiver.state.releaseCount, 1);
  assert.equal(receiver.state.openCount, 1);
  assert.equal(receiver.data[1][headers().indexOf("Revision")], 3);
  assert.equal(receiver.data[1][headers().indexOf("Milestone")], "Current authoritative main");
  assert.equal(receiver.data[1][headers().indexOf("Notes")], "Human-owned note");
  const revisionWriteIndex = receiver.state.operations.findLastIndex((operation) => (
    operation.type === "setValue" && operation.column === headers().indexOf("Revision") + 1
  ));
  const sortProjectsIndex = receiver.state.operations.findIndex((operation) => operation.type === "sortProjects");
  assert.equal(receiver.state.operations[revisionWriteIndex + 1].type, "flush");
  assert.equal(sortProjectsIndex, -1);
  assert.equal(receiver.state.operations.at(-1).type, "releaseLock");
});

test("a committed doPost write survives a later presentation-only sorting failure", () => {
  const first = core.applyToRows(headers(), [], envelope(1, "1".repeat(64)));
  first.rows[0][headers().indexOf("Notes")] = "Still human-owned";
  const incoming = envelope(4, "2".repeat(64));
  incoming.record.milestone = "Committed before sorting";
  const receiver = createReceiverHarness({ inputRows: first.rows, sortThrows: true });

  assert.deepEqual(receiver.post(signedRequest(incoming, receiver.secret)), {
    ok: true,
    changed: true,
    event_id: incoming.event_id,
  });
  assert.equal(receiver.data[1][headers().indexOf("Revision")], 4);
  assert.equal(receiver.data[1][headers().indexOf("Milestone")], "Committed before sorting");
  assert.equal(receiver.data[1][headers().indexOf("Notes")], "Still human-owned");
  const revisionWriteIndex = receiver.state.operations.findLastIndex((operation) => (
    operation.type === "setValue" && operation.column === headers().indexOf("Revision") + 1
  ));
  assert.equal(receiver.state.operations[revisionWriteIndex + 1].type, "flush");
  assert.equal(receiver.state.operations.findIndex((operation) => operation.type === "sortProjects"), -1);
  assert.throws(() => receiver.sortNow(), /injected sorting failure/);
  assert.equal(receiver.data[1][headers().indexOf("Revision")], 4);
  assert.equal(receiver.data[1][headers().indexOf("Notes")], "Still human-owned");
  assert.equal(receiver.state.releaseCount, 2);
  assert.equal(receiver.state.operations.at(-1).type, "releaseLock");
});

test("doPost exact event and revision replay is idempotent", () => {
  const receiver = createReceiverHarness();
  const incoming = envelope(2, "3".repeat(64));
  const request = signedRequest(incoming, receiver.secret);

  assert.deepEqual(receiver.post(request), {
    ok: true,
    changed: true,
    event_id: incoming.event_id,
  });
  assert.deepEqual(receiver.post(request), {
    ok: true,
    changed: false,
    event_id: incoming.event_id,
  });
  assert.equal(receiver.data.length, 2);
  assert.equal(receiver.data[1][headers().indexOf("Revision")], 2);
  assert.deepEqual(receiver.state.lockTimeouts, [5000, 5000]);
});

test("doPost rejects an invalid HMAC before acquiring the mutation lock", () => {
  const receiver = createReceiverHarness();
  const request = signedRequest(envelope(), receiver.secret);
  const wrapper = JSON.parse(request.postData.contents);
  wrapper.signature = `sha256=${"0".repeat(64)}`;
  request.postData.contents = JSON.stringify(wrapper);

  assert.deepEqual(receiver.post(request), {
    ok: false,
    code: "rejected",
    error: "request signature is invalid",
  });
  assert.deepEqual(receiver.state.lockTimeouts, []);
  assert.equal(receiver.state.releaseCount, 0);
  assert.equal(receiver.state.openCount, 0);
});

test("doPost write gate remains fail-closed before authentication and locking", () => {
  const receiver = createReceiverHarness({ acceptWrites: false });
  assert.deepEqual(receiver.post(signedRequest(envelope(), receiver.secret)), {
    ok: false,
    code: "rejected",
    error: "receiver write gate is disabled",
  });
  assert.deepEqual(receiver.state.lockTimeouts, []);
  assert.equal(receiver.state.releaseCount, 0);
  assert.equal(receiver.state.openCount, 0);
});

test("doPost reports busy and never releases a lock it did not acquire", () => {
  const receiver = createReceiverHarness({ lockSucceeds: false });
  assert.deepEqual(receiver.post(signedRequest(envelope(), receiver.secret)), {
    ok: false,
    code: "busy",
    error: "receiver mutation lock is busy",
  });
  assert.deepEqual(receiver.state.lockTimeouts, [5000]);
  assert.equal(receiver.state.releaseCount, 0);
  assert.equal(receiver.state.openCount, 0);
});

test("doPost maps stale and same-revision conflict errors distinctly", () => {
  const stored = core.applyToRows(headers(), [], envelope(3, "d".repeat(64)));

  const staleReceiver = createReceiverHarness({ inputRows: stored.rows });
  assert.deepEqual(staleReceiver.post(signedRequest(envelope(2, "e".repeat(64)), staleReceiver.secret)), {
    ok: false,
    code: "stale",
    error: "stale Sheet revision",
  });
  assert.equal(staleReceiver.state.releaseCount, 1);

  const conflicting = envelope(3, "f".repeat(64));
  conflicting.record.milestone = "Conflicting state";
  const conflictReceiver = createReceiverHarness({ inputRows: stored.rows });
  assert.deepEqual(conflictReceiver.post(signedRequest(conflicting, conflictReceiver.secret)), {
    ok: false,
    code: "conflict",
    error: "same-revision Sheet conflict",
  });
  assert.equal(conflictReceiver.state.releaseCount, 1);
});

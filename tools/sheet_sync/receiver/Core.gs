var MynxSheetSync = (function () {
  "use strict";

  var EXPECTED_REPOSITORY = "Resivore/Mynx-Flavoured-Workbench";
  var EXPECTED_REF = "refs/heads/main";
  var HUMAN_FIELDS = ["Notes"];
  var RECORD_KEYS = [
    "project_uuid", "project_name", "project_id", "aliases", "legacy_names", "legacy_ids",
    "lifecycle", "goals", "scope", "boundaries", "dependencies", "milestone",
    "current_version", "current_artifact_filename", "current_artifact_sha256",
    "current_source_commit", "accepted_version", "accepted_artifact_filename", "accepted_artifact_sha256",
    "accepted_source_commit", "rollback_version", "rollback_artifact_filename", "rollback_artifact_sha256",
    "rollback_source_commit",
    "accepted_current", "accepted_rollback", "build_validation", "deployment_validation",
    "runtime_validation", "blocker", "revision", "activity_at", "updated_at", "last_codex_at",
    "source_commit", "sheet_participates", "sheet_exclusion_reason", "publication_commit"
  ];
  var COLUMN_MAP = {
    project_uuid: "Project UUID",
    project_name: "Project",
    project_id: "Project ID",
    aliases: "Aliases",
    legacy_names: "Legacy Names",
    legacy_ids: "Legacy IDs",
    lifecycle: "Lifecycle",
    goals: "Goals",
    scope: "Scope",
    boundaries: "Boundaries",
    dependencies: "Dependencies",
    milestone: "Milestone",
    current_version: "Current Version",
    current_artifact_filename: "Current Artifact",
    current_artifact_sha256: "Current Artifact SHA-256",
    current_source_commit: "Current Source Commit",
    accepted_version: "Accepted Version",
    accepted_artifact_filename: "Accepted Artifact",
    accepted_artifact_sha256: "Accepted Artifact SHA-256",
    accepted_source_commit: "Accepted Source Commit",
    rollback_version: "Rollback Version",
    rollback_artifact_filename: "Rollback Artifact",
    rollback_artifact_sha256: "Rollback Artifact SHA-256",
    rollback_source_commit: "Rollback Source Commit",
    accepted_current: "Accepted Current Relationship",
    accepted_rollback: "Accepted Rollback Relationship",
    build_validation: "Build Validation",
    deployment_validation: "Deployment Validation",
    runtime_validation: "Runtime Validation",
    blocker: "Blocker",
    revision: "Revision",
    activity_at: "Activity At",
    updated_at: "Updated At",
    last_codex_at: "Last Codex At",
    source_commit: "Source Commit",
    sheet_participates: "Sheet Participates",
    sheet_exclusion_reason: "Sheet Exclusion Reason",
    publication_commit: "Publication Commit"
  };
  var EVENT_COLUMN_MAP = { event_id: "Event ID", manifest_path: "Manifest Path" };

  function ownKeys(value) {
    return Object.keys(value).sort();
  }

  function requireExactKeys(value, expected, path) {
    if (!value || Object.prototype.toString.call(value) !== "[object Object]") {
      throw new Error(path + " must be an object");
    }
    var actual = ownKeys(value);
    var wanted = expected.slice().sort();
    if (JSON.stringify(actual) !== JSON.stringify(wanted)) {
      throw new Error(path + " has unknown or missing keys");
    }
  }

  function validateEnvelope(envelope) {
    requireExactKeys(envelope, ["contract_version", "event_id", "operation", "source", "record", "ownership"], "envelope");
    if (envelope.contract_version !== 1 || envelope.operation !== "project_status_upsert") {
      throw new Error("unsupported envelope contract or operation");
    }
    if (!/^[0-9a-f]{64}$/.test(envelope.event_id)) {
      throw new Error("event_id must be lowercase SHA-256");
    }
    requireExactKeys(envelope.source, ["repository", "ref", "publication_commit", "manifest_path"], "source");
    if (envelope.source.repository !== EXPECTED_REPOSITORY || envelope.source.ref !== EXPECTED_REF) {
      throw new Error("source is not authoritative main");
    }
    if (!/^[0-9a-f]{40}$/.test(envelope.source.publication_commit)) {
      throw new Error("publication commit is invalid");
    }
    if (!/^(projects|resourcepacks)\/[a-z0-9]+(?:[.-][a-z0-9]+)*\/WORKBENCH_STATUS\.json$/.test(envelope.source.manifest_path)) {
      throw new Error("manifest path is invalid");
    }
    requireExactKeys(envelope.ownership, ["sheet_preserves"], "ownership");
    if (JSON.stringify(envelope.ownership.sheet_preserves) !== JSON.stringify(HUMAN_FIELDS)) {
      throw new Error("human-owned field preservation contract is invalid");
    }
    ownKeys(envelope.record).forEach(function (key) {
      var lowered = key.toLowerCase();
      if (HUMAN_FIELDS.some(function (field) { return lowered === field.toLowerCase(); })) {
        throw new Error("record contains a human-owned field");
      }
    });
    requireExactKeys(envelope.record, RECORD_KEYS, "record");
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(envelope.record.project_uuid)) {
      throw new Error("project UUID is invalid");
    }
    if (!Number.isInteger(envelope.record.revision) || envelope.record.revision < 1) {
      throw new Error("revision must be a positive integer");
    }
    if (envelope.record.publication_commit !== envelope.source.publication_commit) {
      throw new Error("record/source publication commit mismatch");
    }
    return envelope;
  }

  function sheetValue(value) {
    if (value === null || typeof value === "undefined") {
      return "";
    }
    if (Array.isArray(value) || Object.prototype.toString.call(value) === "[object Object]") {
      return JSON.stringify(value);
    }
    if (typeof value === "string" && /^[=+\-@]/.test(value)) {
      return "'" + value;
    }
    return value;
  }

  function headerIndex(headers) {
    var result = {};
    headers.forEach(function (header, index) {
      if (Object.prototype.hasOwnProperty.call(result, header)) {
        throw new Error("duplicate Sheet header: " + header);
      }
      result[header] = index;
    });
    Object.keys(COLUMN_MAP).forEach(function (key) {
      if (!Object.prototype.hasOwnProperty.call(result, COLUMN_MAP[key])) {
        throw new Error("missing automation Sheet column: " + COLUMN_MAP[key]);
      }
    });
    Object.keys(EVENT_COLUMN_MAP).forEach(function (key) {
      if (!Object.prototype.hasOwnProperty.call(result, EVENT_COLUMN_MAP[key])) {
        throw new Error("missing event Sheet column: " + EVENT_COLUMN_MAP[key]);
      }
    });
    HUMAN_FIELDS.forEach(function (field) {
      if (!Object.prototype.hasOwnProperty.call(result, field)) {
        throw new Error("missing human-owned Sheet column: " + field);
      }
    });
    return result;
  }

  function rowValues(envelope) {
    validateEnvelope(envelope);
    var values = {};
    Object.keys(COLUMN_MAP).forEach(function (key) {
      values[COLUMN_MAP[key]] = sheetValue(envelope.record[key]);
    });
    values[EVENT_COLUMN_MAP.event_id] = envelope.event_id;
    values[EVENT_COLUMN_MAP.manifest_path] = envelope.source.manifest_path;
    return values;
  }

  function sameAuthoritativeValues(row, indexes, values) {
    return Object.keys(values).every(function (header) {
      if (header === EVENT_COLUMN_MAP.event_id || header === COLUMN_MAP.publication_commit) {
        return true;
      }
      return String(row[indexes[header]]) === String(values[header]);
    });
  }

  function applyToRows(headers, inputRows, envelope) {
    var indexes = headerIndex(headers);
    var values = rowValues(envelope);
    var rows = inputRows.map(function (row) {
      var copy = row.slice();
      while (copy.length < headers.length) copy.push("");
      return copy;
    });
    var uuidColumn = indexes[COLUMN_MAP.project_uuid];
    var revisionColumn = indexes[COLUMN_MAP.revision];
    var eventColumn = indexes[EVENT_COLUMN_MAP.event_id];
    var matches = [];
    rows.forEach(function (row, index) {
      if (String(row[uuidColumn]) === envelope.record.project_uuid) matches.push(index);
    });
    if (matches.length > 1) {
      throw new Error("duplicate Sheet rows for project UUID " + envelope.record.project_uuid);
    }
    var rowIndex;
    if (matches.length === 0) {
      rowIndex = rows.length;
      rows.push(new Array(headers.length).fill(""));
    } else {
      rowIndex = matches[0];
      var existingRevision = Number(rows[rowIndex][revisionColumn]);
      var existingEvent = String(rows[rowIndex][eventColumn]);
      if (!Number.isInteger(existingRevision) || existingRevision < 0) {
        throw new Error("stored Sheet revision is invalid");
      }
      if (existingRevision === envelope.record.revision) {
        var sameValues = sameAuthoritativeValues(rows[rowIndex], indexes, values);
        if (existingEvent && sameValues) {
          return { rows: rows, row_index: rowIndex, changed: false, values: values };
        }
        if (!sameValues) throw new Error("same-revision Sheet conflict");
        // Matching content with an empty Event ID is an uncommitted marker and is completed below.
      } else if (envelope.record.revision < existingRevision) {
        throw new Error("stale Sheet revision");
      }
    }
    Object.keys(values).forEach(function (header) {
      rows[rowIndex][indexes[header]] = values[header];
    });
    return { rows: rows, row_index: rowIndex, changed: true, values: values };
  }

  return {
    validateEnvelope: validateEnvelope,
    applyToRows: applyToRows,
    columnMap: function () { return JSON.parse(JSON.stringify(COLUMN_MAP)); },
    eventColumnMap: function () { return JSON.parse(JSON.stringify(EVENT_COLUMN_MAP)); },
    humanFields: function () { return HUMAN_FIELDS.slice(); }
  };
}());

function doPost(e) {
  var lock = null;
  var lockAcquired = false;
  try {
    var properties = PropertiesService.getScriptProperties();
    if (properties.getProperty("MYNX_STATUS_ACCEPT_WRITES") !== "true") {
      throw new Error("receiver write gate is disabled");
    }
    var secret = properties.getProperty("MYNX_STATUS_HMAC_SECRET");
    var spreadsheetId = properties.getProperty("MYNX_STATUS_SPREADSHEET_ID");
    var sheetName = properties.getProperty("MYNX_STATUS_SHEET_NAME") || "Projects";
    if (!secret || !spreadsheetId) throw new Error("receiver configuration is incomplete");
    if (!e || !e.postData || !e.postData.contents) throw new Error("request body is required");
    if (e.postData.contents.length > 1000000) throw new Error("request body is too large");
    if (secret.length < 32) throw new Error("receiver HMAC secret is too short");

    var wrapper = JSON.parse(e.postData.contents);
    if (!wrapper || Object.keys(wrapper).sort().join(",") !== "payload,signature") {
      throw new Error("request wrapper is invalid");
    }
    if (!/^[A-Za-z0-9_-]+$/.test(wrapper.payload)) throw new Error("request payload encoding is invalid");
    var expectedSignature = "sha256=" + hexBytes_(Utilities.computeHmacSha256Signature(wrapper.payload, secret));
    if (!constantTimeEqual_(String(wrapper.signature), expectedSignature)) {
      throw new Error("request signature is invalid");
    }
    var decoded = Utilities.base64DecodeWebSafe(padBase64_(wrapper.payload));
    var envelope = JSON.parse(Utilities.newBlob(decoded).getDataAsString("UTF-8"));
    MynxSheetSync.validateEnvelope(envelope);

    lock = LockService.getScriptLock();
    lockAcquired = lock.tryLock(30000);
    if (!lockAcquired) throw new Error("receiver mutation lock is busy");
    var sheet = SpreadsheetApp.openById(spreadsheetId).getSheetByName(sheetName);
    if (!sheet) throw new Error("configured project sheet does not exist");
    var lastColumn = sheet.getLastColumn();
    if (lastColumn < 1) throw new Error("project sheet has no header row");
    var headers = sheet.getRange(1, 1, 1, lastColumn).getValues()[0];
    var rowCount = Math.max(0, sheet.getLastRow() - 1);
    var rows = rowCount ? sheet.getRange(2, 1, rowCount, lastColumn).getValues() : [];
    var result = MynxSheetSync.applyToRows(headers, rows, envelope);
    if (result.changed) {
      var sheetRow = result.row_index + 2;
      if (sheetRow > sheet.getLastRow()) sheet.insertRowAfter(Math.max(1, sheet.getLastRow()));
      Object.keys(result.values).forEach(function (header) {
        if (header === "Event ID" || header === "Revision") return;
        var column = headers.indexOf(header) + 1;
        sheet.getRange(sheetRow, column).setValue(result.values[header]);
      });
      SpreadsheetApp.flush();
      sheet.getRange(sheetRow, headers.indexOf("Event ID") + 1).setValue(result.values["Event ID"]);
      SpreadsheetApp.flush();
      // Revision is the final commit marker. A retry can safely reconcile any earlier partial write.
      sheet.getRange(sheetRow, headers.indexOf("Revision") + 1).setValue(result.values["Revision"]);
      SpreadsheetApp.flush();
    }
    return jsonResponse_({ ok: true, changed: result.changed, event_id: envelope.event_id });
  } catch (error) {
    var message = String(error && error.message ? error.message : error);
    var code = message.indexOf("stale Sheet revision") >= 0 ? "stale" :
      message.indexOf("same-revision Sheet conflict") >= 0 ? "conflict" :
      message.indexOf("mutation lock is busy") >= 0 ? "busy" : "rejected";
    return jsonResponse_({ ok: false, code: code, error: message });
  } finally {
    if (lockAcquired) lock.releaseLock();
  }
}

function hexBytes_(bytes) {
  return bytes.map(function (value) {
    var normalized = value < 0 ? value + 256 : value;
    return ("0" + normalized.toString(16)).slice(-2);
  }).join("");
}

function padBase64_(value) {
  while (value.length % 4) value += "=";
  return value;
}

function constantTimeEqual_(left, right) {
  if (left.length !== right.length) return false;
  var difference = 0;
  for (var index = 0; index < left.length; index += 1) {
    difference |= left.charCodeAt(index) ^ right.charCodeAt(index);
  }
  return difference === 0;
}

function jsonResponse_(value) {
  return ContentService.createTextOutput(JSON.stringify(value)).setMimeType(ContentService.MimeType.JSON);
}

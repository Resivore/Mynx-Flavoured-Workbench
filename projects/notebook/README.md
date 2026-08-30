# Notebook

Notebook Canary 1 is a client-only Minecraft Java 26.2 Fabric mod for keeping
small personal notes without an item or server component. It presents a
responsive two-page journal: the left page is an explicitly ordered note index
and the right page is either a scrollable reading view or an ordinary multiline
text editor.

## Access and interaction

- The configurable `Open Notebook` key mapping defaults to `N` and opens from
  gameplay.
- Survival and creative player inventory screens receive one compact `N`
  utility button. It uses the live container bounds and searches adjacent
  positions to avoid widgets that already exist when the inventory initializes.
- `+` creates a note, `−` uses a two-click delete confirmation, `R` rescans disk,
  and dragging index rows persists a user-controlled order.
- `Edit` exposes a normal title field and multiline text editor. Reading mode
  wraps only for display; it never writes wrap-created newlines to disk.
- Lines beginning with `[ ]`, `[x]`, or `[X]` render as clickable checkboxes in
  reading mode. A click changes only the marker in the underlying Markdown.
- A leading `# ` is rendered as a small heading. No broader rich-text editor is
  implemented.

## Storage

The canonical data is local to the current Fabric profile:

```text
config/
  notebook/
    notebook.json
    notes/
      Ancient City.md
      Farm Ideas.md
    backups/
      <stable-note-uuid>/
        previous-1.md
        previous-2.md
        previous-3.md
```

Every note body is one ordinary UTF-8 `.md` file with authored line endings and
plain text. `notebook.json` is machine-owned metadata containing stable UUIDs,
display titles, file-name associations, selected note, and explicit order; it
never contains note bodies. Display titles are separate from stable identity,
and portable file names are sanitized and uniquified without changing the
display title.

Notebook rescans on each screen open and through the in-screen `R` control.
External body edits are reloaded, new valid Markdown files are appended, and
deleted files remove stale entries. An external file rename is deliberately
treated as deletion plus a newly discovered note with a new UUID; content-based
rename guessing is not performed. Unsafe or stale metadata entries are ignored,
and malformed metadata is rebuilt from the still-readable note files.

Writes use a same-directory temporary file followed by atomic replacement when
the filesystem supports it. Before body replacement or deletion, the three most
recent prior contents rotate through the note's backup directory. This is a
small recovery aid, not revision history.

## Canary boundary

Stable note UUIDs and separate machine metadata leave room for note shortcuts
and one pinned-note reference without changing the Markdown files. Visual tabs,
tab management, HUD pinning, HUD position/scale settings, and direct HUD editing
are intentionally deferred until the core notebook has runtime evidence.

Canary 1 is build- and static-test verified only. It has not been deployed or
validated inside Minecraft.

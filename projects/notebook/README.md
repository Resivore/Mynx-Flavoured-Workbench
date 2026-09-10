# Notebook

Notebook Canary 6 is a client-only Minecraft Java 26.2 Fabric mod for keeping
small personal notes without an item or server component. It presents a
responsive two-page journal: the left page is an explicitly ordered note index
and the right page is a scrollable reading view that turns directly into its
ordinary multiline text editor when the user clicks title or body text.

The journal background is the supplied Bedrock book artwork, rendered through
Minecraft's GUI textured pipeline as the complete aspect-preserving 640×400
texture, with page/content bounds derived from its actual page and spine
regions. Reading and editing share the live font row height, editor inset, and
scroll grid, so ruled rows stay with their text while notes scroll. The supplied 16×16 right-arrow sprite is
packaged beside it for the Notebook visual asset set, but is intentionally not
rendered: Canary 2 has no existing semantic next-page or next-note control to
which it could be attached without changing the interaction model.

## Access and interaction

- The configurable `Open Notebook` key mapping defaults to `N` and opens from
  gameplay.
- Survival and creative player inventory screens receive one compact `N`
  utility button. It uses the live container bounds and searches adjacent
  positions to avoid widgets that already exist when the inventory initializes.
- `+` creates a note, `−` uses a two-click delete confirmation, `R` rescans disk,
  and dragging index rows persists a user-controlled order. There are no Edit,
  Done, or Save controls: click visible title/body text to edit at that location,
  and Escape closes after a successful flush.
- Reading mode wraps only for display; it never writes wrap-created newlines to
  disk. Checkbox-box clicks remain reading-mode checkbox actions; clicking their
  text enters ordinary editing.
- Lines beginning with `[ ]`, `[x]`, or `[X]` render as compact, ruled-row-aligned
  ballot boxes in reading mode: an opaque light interior and thin dark outline
  in both states, plus a slim pixel-clean green tick when complete. A click
  changes only the marker in the underlying Markdown.
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
the filesystem supports it. Dirty title/body edits autosave after roughly 750 ms
of idle client ticks and always flush before selection, delete, reload, Escape,
or normal screen exit. One continuous edit session retains one pre-edit body
backup, then its subsequent autosaves update only the canonical note; the three
historical backup slots remain useful recovery history rather than a keystroke
log.

## Canary boundary

Stable note UUIDs and separate machine metadata leave room for note shortcuts
and one pinned-note reference without changing the Markdown files. Visual tabs,
tab management, HUD pinning, HUD position/scale settings, and direct HUD editing
are intentionally deferred until the core notebook has runtime evidence.

Canary 1's retained artifact is historical runtime-failure evidence: Fabric
could not load its client entrypoint because its normal screen class shared the
Mixin-owned client package. A user reports that the repaired Canary 2 opens
without that client-init crash. Canary 6 preserves that package repair and the
shared text grid, packages the supplied X-less 640×400 book artwork, removes
the old dim/procedural backing and Notebook text shadows, and refines the
checklist presentation to match the supplied ballot-box reference. Canary 6 is build- and static-test verified only until
an actual Minecraft session tests it.

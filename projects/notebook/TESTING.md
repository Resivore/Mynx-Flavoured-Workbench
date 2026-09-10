# Testing

Current candidate: `0.1.0-canary9`, retained as `notebook-0.1.0-canary9.jar`, SHA-256 `15a6410af8fb9e22188d9ee56a6cd9d61475a51bd8b36f506c0c06012247f947`, implementation checkpoint `253f9c8d0cb0df61fe4f40d92158223ac6c2776c`. It is `STATIC_PASS`, `NOT_DEPLOYED`, and `RUNTIME_UNTESTED`. Both canonical test slots are occupied by unrelated cohorts; do not change slot ownership for this candidate without a separately authorized serialized manager operation.

Historical Canary 1 (`0.1.0-canary1`, retained `notebook-0.1.0-canary1.jar`, SHA-256 `54a601d9693501bbaca8fcf25b07f1aa65671631278d5a3cd9863fcfdf3c82c0`, source `e25729bf2fc2761269746e20c6ecbad1105e5c80`) passed compilation and static validation but received a user-supplied Minecraft client-initialization `RUNTIME_FAIL`. Fabric rejected `NotebookScreen` because C1 declared `dev.resivore.notebook.client` as its Mixin package. Do not retest, relabel, or claim a runtime pass for C1.

The user reports that the repaired Canary 2 opened successfully, which is external evidence that its fatal C1 Mixin/client-initialization failure no longer occurred. The supplied C3 report further says the book artwork and ruled-line alignment looked correct. Those observations are limited and do not establish a full UI/runtime pass or transfer to C9; C9 has not received a managed deployment or direct Minecraft observation.

## Canary 9 runtime acceptance procedure

Use only the dedicated Matcha Flavoured 26.2 Workbench after explicit Test Slot ownership. Never use the protected 26.1.2 gameplay profile. Preserve exact logs and files and stop on any crash, classloading/Mixin error, input lock, note loss or duplication, malformed UTF-8 write, unexpected newline insertion, wrong-file association, stale checkbox, ordering reset, duplicate/overlapping inventory button, or inventory/item mutation.

1. Launch the Minecraft 26.2 client successfully with Notebook installed. Confirm Fabric initializes `dev.resivore.notebook.NotebookClient` without the C1 Mixin package/classloading failure, then open Notebook with default `N` and through both inventory utility buttons. Confirm no Edit or Done button exists; rebind N and confirm typing it in an editor does not reopen or close Notebook.
2. Inspect the newly supplied X-less 640×400 Bedrock artwork at representative GUI scales/window sizes, including a 320×240-equivalent scaled GUI. Confirm it is aspect-preserving with no programmatic X, synthetic large black shadow, or full-world dim backdrop, and that all Notebook-owned text is shadowless.
3. Click ordinary body text and title text in reading mode; each must immediately open its native editor, focus it, and position the caret near the click. Click a rendered checklist box and confirm it toggles without entering body editing; clicking checklist text must enter ordinary body editing. Type, select another note, return, and confirm persistence; type, wait for autosave, restart/reopen, and confirm persistence and useful prior-content backup history.
4. At representative GUI scales/window sizes, inspect unchecked and checked checklist rows in the reading view. The 8×8 ballot box must remain vertically centered with its text row, sit cleanly on the same shared nine-pixel ruled row, leave a natural gap before its text, and remain aligned while scrolling. An unchecked box must be a transparent 8×8 hollow outline in the ordinary Notebook ink color, with no green or fill. A completed item must be only the supplied #43693F 8×8 outline-and-check matrix on transparency: no pale/white or green-filled interior, white tick, dark outline, smoothing, or substitute mark.
5. In body editing, type a change and press Escape once. Confirm the change is saved, editing ends, reading mode returns, and Notebook remains open; press Escape again and confirm it closes normally. Repeat from title editing. If a deliberately induced save error is reported, confirm the first Escape leaves Notebook open with editing and the unsaved text intact.
6. In empty, reading, and editing views, verify that every ruled row crosses the matching text baseline/row. Use a long wrapped note to scroll both the reader and the multiline editor; the rules must move on the same nine-pixel font row grid with no drift.
7. Run the focused survival-inventory cumulative layout checks, then open Notebook from the compact `N` utility button and repeat after resizing/reinitializing the screen. Confirm exactly one collision-aware button, no overlap with existing utility controls, and no Mixin/classloading failure.

   1. Open survival inventory and confirm `N` appears at bottom-right.
   2. Confirm its bottom edge aligns with the inventory GUI bottom edge.
   3. With QSN installed, confirm QSN appears directly above Notebook.
   4. Confirm the vertical gap between QSN and Notebook is exactly 4px.
   5. Confirm both controls share the right-side `leftPos + imageWidth + 4` X anchor.
   6. Confirm both controls remain clickable and both tooltips work.
   7. Resize or reinitialize the inventory and confirm neither control duplicates.
   8. Repeat at representative GUI scales and window sizes.
   9. Repeat with the cumulative Inventory Extended / Inventory Search stack installed; confirm no overlap.
   10. Confirm the visual `N`, its standard 18x18 Minecraft button chrome, and its action are unchanged.
   11. Confirm the survival fallback begins at the lower-right slot and moves upward on the right before using the left.
   12. Open creative inventory and confirm its existing placement is unchanged.
8. Create at least four independently named notes, including an empty note, a Unicode/unusual-character note, and a long note with wrapped and many authored lines. Select, rename, edit, save, reopen, and delete them; confirm ordinary Markdown files retain only authored line endings.
9. Drag notes upward and downward in the left index, including while scrolled. Restart the client and confirm exact manual ordering and selected stable UUID persist rather than sorting alphabetically or by recency.
10. Author `[ ] unfinished task` and `[x] completed task` lines. In reading mode, click each rendered box and confirm only its Markdown marker changes; enter editing mode and confirm ordinary plain-text markers remain editable.
11. With Notebook closed, externally edit an existing Markdown file, create a valid UTF-8 `.md`, and delete another. Reopen and confirm reconciliation; repeat an edit while open and use `R`. Externally rename a file and confirm conservative delete-plus-create identity handling.
12. Inspect `config/notebook/notebook.json` and `notes/*.md`: metadata must not contain note bodies; filenames must remain portable and unique; no temporary writes may remain. After several edits and a delete, only `previous-1.md` through `previous-3.md` may remain per stable-note backup directory.
13. After copying the profile Notebook directory aside, introduce stale note references and malformed `notebook.json` separately. Reopen each time and confirm readable Markdown notes stay usable and metadata repairs without a wrong association or crash. Change dimensions, die/respawn, leave and join worlds, and restart the client; Notebook must remain profile-local and never mutate world, server, item, or player data.

Builds, source/JAR checks, and synthetic classloading/configuration checks are not Minecraft runtime evidence and cannot make Canary 9 pass or be promoted.

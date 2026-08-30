# Testing

Notebook Canary 1 (`0.1.0-canary1`) is implemented, built, and statically
verified but has not been deployed or runtime-tested. Every case below remains
pending until the exact retained Canary is assigned through the normal Workbench
Test Instance Manager workflow.

## Canary 1 runtime acceptance procedure

1. From ordinary gameplay, use the configurable `Open Notebook` key mapping
   (default `N`) to open and close Notebook. Rebind it through Controls, verify
   the new binding, and confirm that typing `N` into a Notebook text field does
   not reopen or close the screen.
2. Open both the survival player inventory and creative inventory. Confirm one
   compact `N` utility button appears beside the live inventory bounds, opens
   Notebook, survives window resize/reinitialization without duplication, and
   does not overlap Quick Stack Nearby, Inventory Search, recipe-book, effects,
   or Inventory Extended controls in the cumulative Workbench stack.
3. Repeat the access and basic navigation checks at representative GUI scales
   and common window sizes, including a small 320x240-equivalent scaled GUI.
   Confirm both pages, footer controls, editors, text, clipping, and scrollbars
   remain usable without leaving the screen.
4. Create at least four independently named notes, including an empty note, a
   Unicode/unusual-character note, and a long note containing long wrapped
   lines and many authored lines. Select, rename, edit, save, reopen, and delete
   them; confirm no wrap-created filesystem newlines, clipping lockup, stale
   editor content, or unintended item/inventory interaction.
5. Drag notes upward and downward in the left index, including while the index
   is scrolled. Restart the client and confirm the exact manual order and last
   selected stable note persist rather than changing to alphabetical or recent
   order.
6. Author `[ ] unfinished task` and `[x] completed task` lines. In reading mode,
   click each rendered box and confirm its state changes immediately. Inspect
   the corresponding Markdown file and confirm only `[ ]`/`[x]` changed; enter
   edit mode and confirm ordinary plain-text markers remain editable.
7. With Notebook closed, edit an existing note file externally, create a new
   valid UTF-8 `.md` file, and delete another. Reopen Notebook and confirm all
   three changes reconcile. Repeat an edit while Notebook is open and use `R`
   to rescan. Externally rename a file and confirm the old entry disappears and
   the new file is conservatively added rather than guessed as the old identity.
8. Inspect `config/notebook/notebook.json` and `notes/*.md`. Confirm note bodies
   are normal UTF-8 text with real authored line breaks, metadata contains no
   body text, file names remain portable/unique, and no temporary write files
   remain. After several edits and a delete, confirm only `previous-1.md`
   through `previous-3.md` remain under the stable note UUID's backup directory.
9. After copying the test profile's Notebook directory aside, introduce stale
   note references and then malformed JSON in `notebook.json`. Reopen Notebook
   after each case and confirm readable Markdown notes remain usable and repaired
   metadata is written without a crash or association with the wrong file.
10. Change dimensions, die/respawn, leave a world, join another world, and fully
    restart the client. Confirm the profile-local notebook remains available and
    unchanged, with no item requirement, server data, cross-player document
    behavior, or world-save mutation.

Stop and preserve the exact files and logs on any crash, input lock, note loss or
duplication, wrong-file association, malformed UTF-8 write, unexpected newline
insertion, stale checkbox state, ordering reset, duplicate/overlapping inventory
button, or inventory/item mutation. Compilation, automated tests, JAR inspection,
and artifact hashing are not Minecraft runtime validation.

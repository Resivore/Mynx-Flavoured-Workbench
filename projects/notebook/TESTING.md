# Testing

Notebook is PLANNED and has no implementation, build, artifact, or runtime candidate. Every case below is a future runtime acceptance target; no test is completed.

## Future runtime acceptance procedure

1. Open and close the notebook through the approved in-game interaction or keybind, confirming that control returns cleanly to normal gameplay.
2. Create, rename, edit, save, reopen, and delete representative notes without stale content or unintended changes.
3. Verify that authored text survives world save/reload and a full client restart, plus a server restart if the eventual architecture participates on the server.
4. Create and navigate multiple notes, pages, or the selected simple organizational equivalent, confirming predictable ordering and selection.
5. Exercise long notes and empty notes at the documented limits without clipping, data loss, interface lockup, or ambiguous save behavior.
6. Exercise Unicode, unusual characters, line breaks, and supported formatting input without crashes, unsafe rendering, or corrupted text.
7. Change dimensions and die/respawn, confirming that notebook data follows the approved ownership and persistence rules without duplication or loss.
8. Validate representative singleplayer behavior and, if the architecture requires server participation, dedicated-server persistence plus separation between players.
9. Confirm that normal notebook use causes no item or inventory loss, crashes, disconnects, or corrupted notebook data.

Stop and preserve the exact state and logs on any crash, data corruption, lost or duplicated note, cross-player data exposure, unintended item loss, or persistence failure. Do not infer a runtime result from design work, static checks, compilation, or artifact creation.

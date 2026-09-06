# Container Slot Reservations Canary 15 runtime procedure

**Candidate: `0.1.0-canary15`; controlled validation and deployment are not a runtime pass. Do not promote.**

Use only the dedicated **Matcha Flavoured 26.2 Workbench**. Never use the protected 26.1.2 gameplay profile. Preserve `logs/latest.log` and stop immediately for a crash, absent or duplicated panel, competing outer tooltip, wrong host binding, click-through, loss, duplication, or unexpected mod/profile change.

Exact candidate: `container-slot-reservations-0.1.0-canary15.jar`, 174491 bytes, SHA-256 `daf24d25732d4e436d37d3e44305a3d63b67a1ab86b586bc8ec7207b98a8ed1b`, source `f33060e874258c46bde48c2aafd015615adf3ec0`. It is verified in dedicated Workbench Slot A as deployment `2f718451-1b67-4ebc-bbfe-ed6a685bf6fb` / artifact `bf64fb59-2421-4743-b6c9-f015ae4e2bd3` at manager revision 106, and remains `RUNTIME_UNTESTED`. Exact C14 Slot A deployment `fa2ee388-d8f7-4cb6-a2fc-1410b0b7e561` / artifact `7c38c5f3-25f9-4d4a-9f16-1d41c88fa314` is historical `FAIL` evidence only for its incorrect hardcoded bottom-bezel source.

## Canary 15 focused gate

### Active-resource-pack bottom bezel and reload

- With the active Matcha / Inventory Extended compatibility texture, hover a count-one shulker and inspect the standalone panel.
- Require the 176x83 panel to retain its unchanged upper 176x77 shulker region and end directly below the third shulker row with a clean six-pixel bottom bezel. The lower strip must not show player-inventory divider lines.
- Reload or change to a resource pack with the vanilla shulker layout, then reopen/hover the host. Require the same runtime texture to supply the upper panel and the clean bottom bezel without restarting Minecraft.
- Reload or return to the taller compatibility texture and require the clean extended-layout bezel to return, again without restarting Minecraft.
- If a replacement texture is malformed or unsupported, require the upper 176x77 panel to remain stable and stop/report if an arbitrary inventory-row fragment appears as the bottom frame.

### Preserved backmost extraction

Put visibly different items into at least two separated shulker slots, including an early/front slot and a later/back slot. Carry the count-one shulker on the cursor and secondary-click an empty writable player slot.

- Require the highest-index/backmost occupied stack to extract by default.
- Require a second complete extraction to take the next occupied stack working backward, including across gaps.
- Confirm an explicitly selected occupied internal cell still extracts that exact stack.
- Repeat with a capacity-limited target: the selected remainder stays in the shulker and counts remain conserved.
- Repeat with a reservation mismatch: neither target nor shulker changes.

Do not infer a runtime pass from this procedure, build, or unit tests. Record only observed results.

# Container Slot Reservations Canary 14 runtime procedure

**Candidate: `0.1.0-canary14`; controlled validation is not a runtime pass. Do not promote.**

Use only the dedicated **Matcha Flavoured 26.2 Workbench**. Never use the protected 26.1.2 gameplay profile. Preserve `logs/latest.log` and stop immediately for a crash, absent or duplicated panel, competing outer tooltip, wrong host binding, click-through, loss, duplication, or unexpected mod/profile change.

Exact candidate: `container-slot-reservations-0.1.0-canary14.jar`, 170465 bytes, SHA-256 `72b602c1fa781a53fecfbdfd58a3174ee85a910a7f0c099dcdd178c339d1f790`, source `4cd247732e39787fdafef9d9fa25a461b9665f0e`. It is verified in dedicated Workbench Slot A as deployment `fa2ee388-d8f7-4cb6-a2fc-1410b0b7e561` / artifact `7c38c5f3-25f9-4d4a-9f16-1d41c88fa314` at manager revision 104 (2026-09-06T06:30:00Z), and remains `RUNTIME_UNTESTED`. Exact C13 Slot A deployment `377b5b52-c3a5-49c6-9c5e-bc722f263cf7` / artifact `3bbd66ef-c703-41ab-8ada-92275c9f6f3b` is historical `UNTESTED` evidence only.

## Canary 14 focused gate

### Bottom bezel

- Hover a count-one shulker and inspect the standalone panel.
- Confirm the panel ends directly below the third shulker row with the matching bottom bezel from the active `minecraft:textures/gui/container/shulker_box.png`.
- Confirm no player-inventory or hotbar slot dividers appear in that strip.
- If practical, change or reload a resource pack that replaces the shulker GUI and confirm the upper panel and bezel change together.

### Backmost extraction

Put visibly different items into at least two separated shulker slots, including an early/front slot and a later/back slot. Carry the count-one shulker on the cursor and secondary-click an empty writable player slot.

- Require the highest-index/backmost occupied stack to extract by default.
- Require a second complete extraction to take the next occupied stack working backward, including across gaps.
- Confirm an explicitly selected occupied internal cell still extracts that exact stack.
- Repeat with a capacity-limited target: the selected remainder stays in the shulker and counts remain conserved.
- Repeat with a reservation mismatch: neither target nor shulker changes.

Do not infer a runtime pass from this procedure, build, or unit tests. Record only observed results.

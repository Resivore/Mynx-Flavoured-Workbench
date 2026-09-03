# Testing

Canary 1 is the exact accepted/current Stack v15 release and no longer occupies a Test Slot. Preserve only `artifacts/container-slot-reservations-0.1.0-canary1.jar`, 63,388 bytes, SHA-256 `4E7F0A470A387BE76189D0A5E1AE8C614D17EC8B24A6774B400838CC234C4532`, from source checkpoint `e0adc6b9302392440f2e5654c1e4031916b0c996`. Its accepted identity remains deployment `c0fdfff1-831a-4081-93a3-e596d7fc928d` and artifact `c3bc9365-5664-4148-9200-3292307f9bce`; manager revision 70 / Stack v15 has Slot A empty and preserves QSN C6 independently in Slot B.

The unchanged retained C1 identity was not rebuilt, repackaged, relabeled, or given successor-only dependency-policy metadata. Its existing verification remains `STATIC_PASS`: Temurin Java 25.0.4.1 and Gradle 9.5.1 `clean test runGameTest build --offline --no-daemon` passed 28/28 JUnit tests and all 10 required Fabric GameTests, and a clean LF export reproduced the exact retained JAR byte-for-byte. The user's aggregate runtime `PASS` is bound only to the exact identity above; no individual checklist rows were supplied or inferred. Minecraft was not launched while recording or promoting that result.

A normal Windows checkout had previously produced a semantically equivalent but byte-distinct verification JAR because three JSON resources used CRLF instead of the retained artifact's LF; all other 50 JAR entries matched. That output was discarded and never replaced the exact retained artifact. Stop if a later canonical revision fails to preserve the exact accepted deployment ID, artifact ID, filename, SHA-256, version, and source checkpoint above.

## Accepted C1 regression reference

Use this historical matrix only for an explicitly authorized C1 rollback/regression check in the dedicated Minecraft 26.2 Workbench. Do not touch the protected gameplay profile, rebuild or substitute a same-version JAR, infer row-level evidence from the aggregate PASS, or mutate an independent Test Slot.

1. In a single chest, reserve an occupied slot and a cursor-derived empty slot, clear both forms, close and reopen, and confirm the exact indices and component-aware templates persist.
2. Repeat the reserve, clear, close, and reopen checks in a trapped chest and a barrel.
3. In a double chest, reserve slots in both physical halves; reopen from either half, break one half, verify the survivor retains only its physical reservations, re-form the double chest, and confirm reservations did not migrate between halves.
4. Reserve placed shulker slots, close and reopen, break and carry the shulker, relog, place it again, and confirm the exact local indices persist.
5. Repeat the shulker round trip with an undyed and a colored shulker carrying physical contents, a custom name, and other meaningful components; confirm color, contents, name, unrelated components, and reservations all remain intact.
6. Confirm an empty reservation shows a faded count-free ghost, project outline, and reservation tooltip at the live slot coordinates; confirm an occupied reserved slot shows only its marker.
7. Manually insert a matching stack into an empty reserved slot, then try an item with the same registry carrier but different meaningful components; accept only the exact component identity without moving, deleting, or duplicating the rejected stack.
8. Shift-click matching and component-distinct items and confirm vanilla destination order is unchanged except that mismatched reserved empty slots are skipped.
9. Insert matching and component-distinct items with a vanilla hopper and confirm vanilla ordering and cooldown remain unchanged; extract manually and by hopper until empty and confirm the reservation remains.
10. Verify reservations across chunk unload/reload, world save/reload, and a clean server restart.
11. With two players viewing one supported container, change reservations from each side and confirm server-authoritative convergence without stale ghosts or unauthorized slot changes.
12. With the intended inventory UI stack enabled, repeat representative display, manual-placement, and QUICK_MOVE checks and confirm player, crafting, result, equipment, trinket, trash, fake, and foreign slots remain ineligible while expanded live coordinates stay aligned.
13. Exercise rapid reserve, replace, clear, insert, and extract actions and confirm no duplication, deletion, detached-copy writeback, or client/server desynchronization.
14. Exit through a clean shutdown and inspect the complete log for project-attributable mixin, codec, component, networking, persistence, or rendering errors.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if the artifact or dependency identities differ, readiness verification fails, an unsupported owner becomes eligible, physical-half ownership changes, a component-distinct item is admitted, insertion order changes beyond the reservation predicate, persistence or synchronization fails, any item or unrelated component is lost, or a project-attributable error occurs.
